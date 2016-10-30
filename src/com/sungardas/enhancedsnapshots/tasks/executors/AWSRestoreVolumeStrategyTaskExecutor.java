package com.sungardas.enhancedsnapshots.tasks.executors;


import com.amazonaws.services.ec2.model.Snapshot;
import com.amazonaws.services.ec2.model.Volume;
import com.amazonaws.services.ec2.model.VolumeState;
import com.amazonaws.services.ec2.model.VolumeType;
import com.sungardas.enhancedsnapshots.aws.dynamodb.model.BackupEntry;
import com.sungardas.enhancedsnapshots.aws.dynamodb.model.TaskEntry;
import com.sungardas.enhancedsnapshots.aws.dynamodb.repository.BackupRepository;
import com.sungardas.enhancedsnapshots.aws.dynamodb.repository.TaskRepository;
import com.sungardas.enhancedsnapshots.components.ConfigurationMediator;
import com.sungardas.enhancedsnapshots.dto.CopyingTaskProgressDto;
import com.sungardas.enhancedsnapshots.enumeration.TaskProgress;
import com.sungardas.enhancedsnapshots.exception.DataAccessException;
import com.sungardas.enhancedsnapshots.exception.EnhancedSnapshotsException;
import com.sungardas.enhancedsnapshots.exception.EnhancedSnapshotsInterruptedException;
import com.sungardas.enhancedsnapshots.exception.EnhancedSnapshotsTaskInterruptedException;
import com.sungardas.enhancedsnapshots.service.*;
import com.sungardas.enhancedsnapshots.util.SystemUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.concurrent.TimeUnit;

import static com.sungardas.enhancedsnapshots.aws.dynamodb.model.TaskEntry.TaskEntryStatus.RUNNING;

@Service("awsRestoreVolumeTaskExecutor")
public class AWSRestoreVolumeStrategyTaskExecutor extends AbstractAWSVolumeTaskExecutor {
    public static final String RESTORED_NAME_PREFIX = "Restore of ";
    private static final Logger LOG = LogManager.getLogger(AWSRestoreVolumeStrategyTaskExecutor.class);
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

    @Autowired
    private MailService mailService;

    @Override
    public void execute(TaskEntry taskEntry) {
        switch (taskEntry.getProgress()) {
            case FAIL_CLEANING: {
                failCleaningStep(taskEntry, new EnhancedSnapshotsException("Restore failed"));
                return;
            }
            case INTERRUPTED_CLEANING: {
                interruptedCleaningStep(taskEntry);
                return;
            }
        }
        try {
            LOG.info("Starting restore process for volume {}", taskEntry.getVolume());
            LOG.info("{} task state was changed to 'in progress'", taskEntry.getId());
            taskEntry.setStatus(RUNNING.getStatus());
            taskRepository.save(taskEntry);
            notificationService.notifyAboutRunningTaskProgress(taskEntry.getId(), "Starting restore", 0);

            String sourceFile = taskEntry.getSourceFileName();
            if (snapshotService.getSnapshotIdByVolumeId(taskEntry.getVolume()) != null && (sourceFile == null || sourceFile.isEmpty())) {
                LOG.info("Task was defined as restore from snapshot.");
                notificationService.notifyAboutRunningTaskProgress(taskEntry.getId(), "Restore from Snapshot", 5);
                restoreFromSnapshot(taskEntry);
            } else {
                LOG.info("Task was defined as restore from history.");
                notificationService.notifyAboutRunningTaskProgress(taskEntry.getId(), "Restore from S3", 0);
                restoreFromBackupFile(taskEntry);
            }
            completeTask(taskEntry);
        } catch (EnhancedSnapshotsInterruptedException e) {
            if (!configurationMediator.isClusterMode()) {
                interruptedCleaningStep(taskEntry);
            }
        } catch (Exception e) {
            failCleaningStep(taskEntry, e);
        }
    }

    private void restoreFromSnapshot(TaskEntry taskEntry) {
        try {
            notificationService.notifyAboutRunningTaskProgress(taskEntry.getId(), "Restore from snapshot", 20);
            String targetZone = taskEntry.getAvailabilityZone();

            String volumeId = taskEntry.getVolume();
            String snapshotId = snapshotService.getSnapshotIdByVolumeId(volumeId);
            // check that snapshot exists
            if (snapshotId == null || !awsCommunication.snapshotExists(snapshotId)) {
                LOG.error("Failed to find snapshot for volume {} ", volumeId);
                throw new DataAccessException("Backup for volume: " + volumeId + " was not found");
            }

            checkThreadInterruption(taskEntry);
            notificationService.notifyAboutRunningTaskProgress(taskEntry.getId(), "Creating volume from snapshot", 50);

            Volume volume = awsCommunication.createVolumeFromSnapshot(snapshotId, targetZone, VolumeType.fromValue(taskEntry.getRestoreVolumeType()),
                    taskEntry.getRestoreVolumeIopsPerGb());
            awsCommunication.setResourceName(volume.getVolumeId(), RESTORED_NAME_PREFIX + taskEntry.getVolume());
            awsCommunication.addTag(volume.getVolumeId(), "Created by", "Enhanced Snapshots");
        } catch (EnhancedSnapshotsTaskInterruptedException e) {
            LOG.info("Restore task was canceled");
            taskRepository.delete(taskEntry);
            mailService.notifyAboutSystemStatus("Restore task for volume with id" + taskEntry.getVolume() + " was canceled");
        }
    }

    private void restoreFromBackupFile(TaskEntry taskEntry) throws IOException, InterruptedException {
        Volume tempVolume = taskEntry.getTempVolumeId() != null ? awsCommunication.getVolume(taskEntry.getTempVolumeId()) : null;
        BackupEntry backupEntry;
        if (taskEntry.getSourceFileName() != null && !taskEntry.getSourceFileName().isEmpty()) {
            backupEntry = backupRepository.findOne(taskEntry.getSourceFileName());
        } else {
            backupEntry = backupRepository.findByVolumeId(taskEntry.getVolume())
                    .stream().sorted((e1, e2) -> e2.getTimeCreated().compareTo(e1.getTimeCreated()))
                    .findFirst().get();
        }
        if (taskEntry.getProgress() != TaskProgress.NONE) {
            switch (taskEntry.getProgress()) {
                case ATTACHING_VOLUME:
                case CREATING_TEMP_VOLUME:
                case WAITING_TEMP_VOLUME:
                case COPYING: {
                    try {
                        notificationService.notifyAboutRunningTaskProgress(taskEntry.getId(), "Detaching temp volume", 50);
                        detachingTempVolumeStep(taskEntry);
                    } catch (Exception e) {
                        // skip
                    }
                    try {
                        notificationService.notifyAboutRunningTaskProgress(taskEntry.getId(), "Deleting temp volume", 20);
                        deletingTempVolumeStep(taskEntry);
                    } catch (Exception e) {
                        // skip
                    }
                    setProgress(taskEntry, TaskProgress.STARTED);
                    break;
                }
                case CREATING_SNAPSHOT: {
                    if (taskEntry.getTempSnapshotId() != null) {
                        setProgress(taskEntry, TaskProgress.WAITING_SNAPSHOT);
                    }
                    break;
                }
            }
        }
        switch (taskEntry.getProgress()) {
            case NONE:
            case STARTED:
                setProgress(taskEntry, TaskProgress.STARTED);
            case CREATING_TEMP_VOLUME: {
                notificationService.notifyAboutRunningTaskProgress(taskEntry.getId(), "Creating temp volume", 10);
                creationTempVolumeStep(taskEntry, backupEntry);
            }
            case WAITING_TEMP_VOLUME: {
                notificationService.notifyAboutRunningTaskProgress(taskEntry.getId(), "Waiting temp volume", 15);
                tempVolume = waitingTempVolumeStep(taskEntry);
            }
            case ATTACHING_VOLUME: {
                notificationService.notifyAboutRunningTaskProgress(taskEntry.getId(), "Attaching temp volume", 20);
                tempVolume = attachingVolumeStep(taskEntry, tempVolume);
            }
            case COPYING: {
                copyingStep(taskEntry, tempVolume, backupEntry);
            }
            case DETACHING_TEMP_VOLUME: {
                notificationService.notifyAboutRunningTaskProgress(taskEntry.getId(), "Detaching temp volume", 65);
                detachingTempVolumeStep(taskEntry);
            }
        }
        if (!tempVolume.getAvailabilityZone().equals(taskEntry.getAvailabilityZone())) {
            //move to target availability zone
            switch (taskEntry.getProgress()) {
                case DETACHING_TEMP_VOLUME:
                case CREATING_SNAPSHOT: {
                    notificationService.notifyAboutRunningTaskProgress(taskEntry.getId(), "Creating snapshot", 70);
                    creatingTempSnapshotStep(taskEntry);
                }
                case WAITING_SNAPSHOT: {
                    notificationService.notifyAboutRunningTaskProgress(taskEntry.getId(), "Waiting snapshot", 75);
                    waitingTempSnapshotStep(taskEntry);
                }
                case MOVE_TO_TARGET_ZONE: {
                    notificationService.notifyAboutRunningTaskProgress(taskEntry.getId(), "Moving to target Zone", 80);
                    Volume volumeToRestore = moveToTargetZoneStep(taskEntry);
                    awsCommunication.setResourceName(volumeToRestore.getVolumeId(), RESTORED_NAME_PREFIX + backupEntry.getFileName());
                    awsCommunication.addTag(volumeToRestore.getVolumeId(), "Created by", "Enhanced Snapshots");
                }
                case DELETING_TEMP_VOLUME: {
                    notificationService.notifyAboutRunningTaskProgress(taskEntry.getId(), "Deleting temp volume", 85);
                    deletingTempVolumeStep(taskEntry);
                }
                case DELETING_TEMP_SNAPSHOT: {
                    notificationService.notifyAboutRunningTaskProgress(taskEntry.getId(), "Deleting temp snapshot", 90);
                    deletingTempSnapshotStep(taskEntry);
                }
            }
        } else {
            //in case availability zone is the same we do not need temp volume
            awsCommunication.deleteTemporaryTag(tempVolume.getVolumeId());
            awsCommunication.setResourceName(tempVolume.getVolumeId(), RESTORED_NAME_PREFIX + backupEntry.getFileName());
            awsCommunication.addTag(tempVolume.getVolumeId(), "Created by", "Enhanced Snapshots");
        }

    }

    private Volume creationTempVolumeStep(TaskEntry taskEntry, BackupEntry backupEntry) {
        checkThreadInterruption(taskEntry);
        setProgress(taskEntry, TaskProgress.CREATING_TEMP_VOLUME);
        Volume tempVolume;

        LOG.info("Used backup record: {}", backupEntry.getFileName());
        int size = Integer.parseInt(backupEntry.getSizeGiB());
        checkThreadInterruption(taskEntry);
        // creating temporary volume
        if (taskEntry.getTempVolumeType().equals(VolumeType.Io1.toString())) {
            tempVolume = awsCommunication.createIO1Volume(size, taskEntry.getTempVolumeIopsPerGb());
        } else {
            tempVolume = awsCommunication.createVolume(size, VolumeType.fromValue(taskEntry.getTempVolumeType()));
        }
        LOG.info("Created {} volume:{}", taskEntry.getTempVolumeType(), tempVolume.toString());
        checkThreadInterruption(taskEntry);
        awsCommunication.createTemporaryTag(tempVolume.getVolumeId(), backupEntry.getFileName());
        taskEntry.setTempVolumeId(tempVolume.getVolumeId());
        taskRepository.save(taskEntry);

        return tempVolume;
    }

    private Volume waitingTempVolumeStep(TaskEntry taskEntry) {
        checkThreadInterruption(taskEntry);
        setProgress(taskEntry, TaskProgress.WAITING_TEMP_VOLUME);
        Volume volumeDest = awsCommunication.waitForVolumeState(awsCommunication.getVolume(taskEntry.getTempVolumeId()), VolumeState.Available);
        LOG.info("Volume created: {}", volumeDest.toString());
        return volumeDest;
    }

    private Volume attachingVolumeStep(TaskEntry taskEntry, Volume tempVolume) {
        checkThreadInterruption(taskEntry);
        setProgress(taskEntry, TaskProgress.ATTACHING_VOLUME);
        awsCommunication.attachVolume(awsCommunication.getInstance(SystemUtils.getInstanceId()), tempVolume);

        tempVolume = awsCommunication.syncVolume(tempVolume);

        try {
            TimeUnit.MINUTES.sleep(1);
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }
        checkThreadInterruption(taskEntry);
        return tempVolume;
    }

    private void copyingStep(TaskEntry taskEntry, Volume tempVolume, BackupEntry backupEntry) throws IOException, InterruptedException {
        String attachedDeviceName = storageService.detectFsDevName(tempVolume);
        LOG.info("Volume was attached as device: " + attachedDeviceName);
        CopyingTaskProgressDto dto = new CopyingTaskProgressDto(taskEntry.getId(), 25, 60, Long.parseLong(backupEntry.getSizeGiB()));
        storageService.copyData(configurationMediator.getSdfsMountPoint() + backupEntry.getFileName(), attachedDeviceName, dto, taskEntry.getId());
    }

    private void completeTask(TaskEntry taskEntry) {
        notificationService.notifyAboutRunningTaskProgress(taskEntry.getId(), "Restore complete", 100);
        taskService.complete(taskEntry);
        LOG.info("{} task {} was completed", taskEntry.getType(), taskEntry.getId());
        mailService.notifyAboutSuccess(taskEntry);
    }


    private void detachingTempVolumeStep(TaskEntry taskEntry) {
        setProgress(taskEntry, TaskProgress.DETACHING_TEMP_VOLUME);
        checkThreadInterruption(taskEntry);
        LOG.info("Detaching volume: {}", taskEntry.getTempVolumeId());
        awsCommunication.detachVolume(awsCommunication.getVolume(taskEntry.getTempVolumeId()));
    }

    private void creatingTempSnapshotStep(TaskEntry taskEntry) {
        setProgress(taskEntry, TaskProgress.CREATING_SNAPSHOT);
        Volume volumeSrc = awsCommunication.getVolume(taskEntry.getVolume());
        if (volumeSrc == null) {
            LOG.error("Can't get access to {} volume", taskEntry.getVolume());
            throw new DataAccessException(MessageFormat.format("Can't get access to {} volume", taskEntry.getVolume()));
        }

        taskEntry.setTempSnapshotId(awsCommunication.createSnapshot(volumeSrc).getSnapshotId());
        taskRepository.save(taskEntry);
    }

    private void waitingTempSnapshotStep(TaskEntry taskEntry) {
        setProgress(taskEntry, TaskProgress.WAITING_SNAPSHOT);
        Snapshot snapshot = awsCommunication.waitForCompleteState(awsCommunication.getSnapshot(taskEntry.getTempSnapshotId()));
        LOG.info("SnapshotEntry created: {}", snapshot.toString());
    }

    private Volume moveToTargetZoneStep(TaskEntry taskEntry) {
        checkThreadInterruption(taskEntry);
        notificationService.notifyAboutRunningTaskProgress(taskEntry.getId(), "Moving into target zone...", 95);

        return awsCommunication.createVolumeFromSnapshot(taskEntry.getTempSnapshotId(), taskEntry.getAvailabilityZone(),
                VolumeType.fromValue(taskEntry.getRestoreVolumeType()), taskEntry.getRestoreVolumeIopsPerGb());
    }

    private void deletingTempVolumeStep(TaskEntry taskEntry) {
        setProgress(taskEntry, TaskProgress.DELETING_TEMP_VOLUME);
        LOG.info("Deleting temporary volume: {}", taskEntry.getTempVolumeId());
        awsCommunication.deleteVolume(awsCommunication.getVolume(taskEntry.getTempVolumeId()));
    }

    private void deletingTempSnapshotStep(TaskEntry taskEntry) {
        setProgress(taskEntry, TaskProgress.DELETING_TEMP_SNAPSHOT);
        LOG.info("Deleting temporary snapshot: {}", taskEntry.getTempSnapshotId());
        awsCommunication.deleteSnapshot(taskEntry.getTempSnapshotId());
    }

    private void cleaningStep(TaskEntry taskEntry) {
        try {
            deletingTempSnapshotStep(taskEntry);
        } catch (Exception e) {
            //skip
        }
        try {
            deletingTempVolumeStep(taskEntry);
        } catch (Exception e) {
            //skip
        }

    }

    private void interruptedCleaningStep(TaskEntry taskEntry) {
        setProgress(taskEntry, TaskProgress.INTERRUPTED_CLEANING);
        cleaningStep(taskEntry);
        LOG.info("Restore task was canceled");
        taskRepository.delete(taskEntry);
        mailService.notifyAboutSystemStatus("Restore task for volume with id" + taskEntry.getVolume() + " was canceled");
    }

    private void failCleaningStep(TaskEntry taskEntry, Exception e) {
        setProgress(taskEntry, TaskProgress.FAIL_CLEANING);
        cleaningStep(taskEntry);
        LOG.error("Failed to execute {} task {}. Changing task status to '{}'", taskEntry.getType(), taskEntry.getId(), TaskEntry.TaskEntryStatus.ERROR);
        LOG.error(e);
        taskEntry.setStatus(TaskEntry.TaskEntryStatus.ERROR.getStatus());
        taskRepository.save(taskEntry);
        mailService.notifyAboutError(taskEntry, e);
    }
}
