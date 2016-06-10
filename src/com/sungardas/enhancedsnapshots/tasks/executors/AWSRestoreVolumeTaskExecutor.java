package com.sungardas.enhancedsnapshots.tasks.executors;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Snapshot;
import com.amazonaws.services.ec2.model.Volume;
import com.amazonaws.services.ec2.model.VolumeType;
import com.sungardas.enhancedsnapshots.aws.dynamodb.model.BackupEntry;
import com.sungardas.enhancedsnapshots.aws.dynamodb.model.TaskEntry;
import com.sungardas.enhancedsnapshots.aws.dynamodb.repository.BackupRepository;
import com.sungardas.enhancedsnapshots.aws.dynamodb.repository.TaskRepository;
import com.sungardas.enhancedsnapshots.components.ConfigurationMediator;
import com.sungardas.enhancedsnapshots.dto.CopyingTaskProgressDto;
import com.sungardas.enhancedsnapshots.exception.DataAccessException;
import com.sungardas.enhancedsnapshots.exception.EnhancedSnapshotsInterruptedException;
import com.sungardas.enhancedsnapshots.service.AWSCommunicationService;
import com.sungardas.enhancedsnapshots.service.NotificationService;
import com.sungardas.enhancedsnapshots.service.SnapshotService;
import com.sungardas.enhancedsnapshots.service.StorageService;
import com.sungardas.enhancedsnapshots.service.TaskService;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;


@Service("awsRestoreVolumeTaskExecutor")
@Profile("prod")
public class AWSRestoreVolumeTaskExecutor implements TaskExecutor {
    public static final String RESTORED_NAME_PREFIX = "Restore of ";
    private static final Logger LOG = LogManager.getLogger(AWSRestoreVolumeTaskExecutor.class);
    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private BackupRepository backupRepository;

    @Autowired
    private SnapshotService snapshotService;

    @Autowired
    private AWSCommunicationService awsCommunication;

    @Autowired
    private StorageService storageService;

    @Autowired
    private ConfigurationMediator configurationMediator;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private TaskService taskService;

    @Override
    public void execute(TaskEntry taskEntry) {
        LOG.info("Executing restore task:\n" + taskEntry.toString());
        if (Thread.interrupted()) {
            throw new EnhancedSnapshotsInterruptedException("Task interrupted");
        }
        notificationService.notifyAboutTaskProgress(taskEntry.getId(), "Starting restore", 0);
        String sourceFile = taskEntry.getSourceFileName();
        changeTaskStatusToRunning(taskEntry);
        try {
            if (sourceFile == null || sourceFile.isEmpty()) {
                LOG.info("Task was defined as restore from snapshot.");
                restoreFromSnapshot(taskEntry);
            } else {
                LOG.info("Task was defined as restore from history.");
                restoreFromBackupFile(taskEntry);
            }
            completeTask(taskEntry);
        } catch (Exception e) {
            LOG.error("Failed to execute {} task {}. Changing task status to '{}'", taskEntry.getType(), taskEntry.getId(), TaskEntry.TaskEntryStatus.ERROR);
            LOG.error(e);
            taskEntry.setStatus(TaskEntry.TaskEntryStatus.ERROR.getStatus());
            taskRepository.save(taskEntry);
        }
    }

    private void changeTaskStatusToRunning(TaskEntry taskEntry) {
        LOG.info("Status of {} task {} was changed to '{}'", taskEntry.getType(), taskEntry.getId(), TaskEntry.TaskEntryStatus.RUNNING);
        taskEntry.setStatus(TaskEntry.TaskEntryStatus.RUNNING.getStatus());
        taskRepository.save(taskEntry);
    }

    private void completeTask(TaskEntry taskEntry) {
        notificationService.notifyAboutTaskProgress(taskEntry.getId(), "Restore complete", 100);
        taskService.complete(taskEntry);
        LOG.info("{} task {} was completed", taskEntry.getType(), taskEntry.getId());
    }

    private void restoreFromSnapshot(TaskEntry taskEntry) {
        notificationService.notifyAboutTaskProgress(taskEntry.getId(), "Restore from snapshot", 20);
        String targetZone = taskEntry.getAvailabilityZone();

        String volumeId = taskEntry.getVolume();
        String snapshotId = snapshotService.getSnapshotIdByVolumeId(volumeId);
        // check that snapshot exists
        if (snapshotId == null || !awsCommunication.snapshotExists(snapshotId)) {
            LOG.error("Failed to find snapshot for volume {} ", volumeId);
            throw new DataAccessException("Backup for volume: " + volumeId + " was not found");
        }

        checkThreadInterruption(taskEntry);
        notificationService.notifyAboutTaskProgress(taskEntry.getId(), "Creating volume from snapshot", 50);

        Volume volume = awsCommunication.createVolumeFromSnapshot(snapshotId, targetZone, VolumeType.fromValue(taskEntry.getRestoreVolumeType()),
                taskEntry.getRestoreVolumeIopsPerGb());
        awsCommunication.setResourceName(volume.getVolumeId(), RESTORED_NAME_PREFIX + taskEntry.getVolume());
        awsCommunication.addTag(volume.getVolumeId(), "Created by", "Enhanced Snapshots");
    }

    //TODO: in case availability zone is the same we do not need temp volume
    // add logic to handle this situation
    private void restoreFromBackupFile(TaskEntry taskEntry) {
        Volume tempVolume = null;
        Snapshot tempSnapshot = null;
        try {
            checkThreadInterruption(taskEntry);
            notificationService.notifyAboutTaskProgress(taskEntry.getId(), "Restore from file", 10);

            BackupEntry backupentry = backupRepository.findOne(taskEntry.getSourceFileName());
            LOG.info("Used backup record: {}", backupentry.getFileName());
            Instance instance = awsCommunication.getInstance(configurationMediator.getConfigurationId());
            int size = Integer.parseInt(backupentry.getSizeGiB());
            checkThreadInterruption(taskEntry);
            notificationService.notifyAboutTaskProgress(taskEntry.getId(), "Creating volume...", 15);
            // creating temporary volume
            if (taskEntry.getTempVolumeType().equals(VolumeType.Io1.toString())) {
                tempVolume = awsCommunication.createIO1Volume(size, taskEntry.getTempVolumeIopsPerGb());
            } else {
                tempVolume = awsCommunication.createVolume(size, VolumeType.fromValue(taskEntry.getTempVolumeType()));
            }
            LOG.info("Created {} volume:{}", taskEntry.getTempVolumeType(), tempVolume.toString());
            checkThreadInterruption(taskEntry);
            notificationService.notifyAboutTaskProgress(taskEntry.getId(), "Attaching volume...", 20);
            awsCommunication.createTemporaryTag(tempVolume.getVolumeId(), backupentry.getFileName());
            awsCommunication.waitForAvailableState(tempVolume);
            awsCommunication.attachVolume(instance, tempVolume);

            try {
                TimeUnit.MINUTES.sleep(1);
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
            LOG.info("Trying to attach volume to instance {}", instance.getInstanceId());
            checkThreadInterruption(taskEntry);
            notificationService.notifyAboutTaskProgress(taskEntry.getId(), "Synchronizing volume...", 25);

            //wait for attached state
            while (tempVolume.getAttachments().size() == 0) {
                sleep();
                tempVolume = awsCommunication.syncVolume(tempVolume);
            }

            String attachedDeviceName = storageService.detectFsDevName(tempVolume);
            LOG.info("Volume was attached as device: " + attachedDeviceName);
            try {
                CopyingTaskProgressDto dto = new CopyingTaskProgressDto(taskEntry.getId(), 25, 80, Long.parseLong(backupentry.getSizeGiB()));
                storageService.javaBinaryCopy(configurationMediator.getSdfsMountPoint() + backupentry.getFileName(), attachedDeviceName, dto);
            } catch (IOException | InterruptedException e) {
                LOG.fatal("Restore of volume {} failed", tempVolume);
                taskEntry.setStatus("error");
                e.printStackTrace();
            }

            checkThreadInterruption(taskEntry);
            notificationService.notifyAboutTaskProgress(taskEntry.getId(), "Detaching volume...", 85);

            awsCommunication.detachVolume(tempVolume);
            LOG.info("Detaching volume after restoring data: " + tempVolume.toString());
            checkThreadInterruption(taskEntry);
            notificationService.notifyAboutTaskProgress(taskEntry.getId(), "Moving into target zone...", 90);
            tempSnapshot = awsCommunication.waitForCompleteState(awsCommunication.createSnapshot(tempVolume));

            checkThreadInterruption(taskEntry);
            notificationService.notifyAboutTaskProgress(taskEntry.getId(), "Moving into target zone...", 95);

            Volume volumeToRestore = awsCommunication.createVolumeFromSnapshot(tempSnapshot.getSnapshotId(), taskEntry.getAvailabilityZone(),
                    VolumeType.fromValue(taskEntry.getRestoreVolumeType()), taskEntry.getRestoreVolumeIopsPerGb());
            checkThreadInterruption(taskEntry);

            awsCommunication.setResourceName(volumeToRestore.getVolumeId(), RESTORED_NAME_PREFIX + backupentry.getFileName());
            awsCommunication.deleteVolume(tempVolume);
            awsCommunication.deleteSnapshot(tempSnapshot.getSnapshotId());
            awsCommunication.addTag(volumeToRestore.getVolumeId(), "Created by", "Enhanced Snapshots");
        } catch (Exception e) {
            // TODO: add user notification about task failure
            LOG.error("Failed to restore backup.", e);
            // clean up
            if (tempVolume != null && awsCommunication.volumeExists(tempVolume.getVolumeId())) {
                tempVolume = awsCommunication.syncVolume(tempVolume);
                if (tempVolume.getAttachments().size() != 0) {
                    awsCommunication.detachVolume(tempVolume);
                }
                awsCommunication.deleteVolume(tempVolume);
            }
            if (tempSnapshot != null && awsCommunication.snapshotExists(tempSnapshot.getSnapshotId())) {
                awsCommunication.deleteSnapshot(tempSnapshot.getSnapshotId());
            }
            throw e;
        }
    }

    private void sleep() {
        try {
            TimeUnit.SECONDS.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void checkThreadInterruption(TaskEntry taskEntry) {
        if (Thread.interrupted()) {
            LOG.info("Restore task {} was interrupted.", taskEntry.getId());
            throw new EnhancedSnapshotsInterruptedException("Task interrupted");
        }
    }
}
