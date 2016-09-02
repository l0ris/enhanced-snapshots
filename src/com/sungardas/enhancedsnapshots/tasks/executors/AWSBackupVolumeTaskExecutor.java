package com.sungardas.enhancedsnapshots.tasks.executors;

import com.amazonaws.services.ec2.model.*;
import com.sungardas.enhancedsnapshots.aws.dynamodb.model.BackupEntry;
import com.sungardas.enhancedsnapshots.aws.dynamodb.model.BackupState;
import com.sungardas.enhancedsnapshots.aws.dynamodb.model.TaskEntry;
import com.sungardas.enhancedsnapshots.aws.dynamodb.repository.BackupRepository;
import com.sungardas.enhancedsnapshots.aws.dynamodb.repository.TaskRepository;
import com.sungardas.enhancedsnapshots.components.ConfigurationMediator;
import com.sungardas.enhancedsnapshots.dto.CopyingTaskProgressDto;
import com.sungardas.enhancedsnapshots.dto.TaskProgressDto;
import com.sungardas.enhancedsnapshots.exception.EnhancedSnapshotsException;
import com.sungardas.enhancedsnapshots.exception.EnhancedSnapshotsTaskInterruptedException;
import com.sungardas.enhancedsnapshots.service.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.util.concurrent.TimeUnit;

import static com.sungardas.enhancedsnapshots.aws.dynamodb.model.TaskEntry.TaskEntryStatus.*;
import static java.lang.String.format;

@Service("awsBackupVolumeTaskExecutor")
@Profile("prod")
public class AWSBackupVolumeTaskExecutor extends AbstractAWSVolumeTaskExecutor {
    private static final Logger LOG = LogManager.getLogger(AWSBackupVolumeTaskExecutor.class);

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private StorageService storageService;

    @Autowired
    private BackupRepository backupRepository;

    @Autowired
    private SnapshotService snapshotService;

    @Autowired
    private AWSCommunicationService awsCommunication;

    @Autowired
    private ConfigurationMediator configurationMediator;

    @Autowired
    private RetentionService retentionService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private VolumeService volumeService;

    public void execute(TaskEntry taskEntry) {
        String volumeId = taskEntry.getVolume();
        Volume tempVolume = null;
        String backupFileName = null;
        try {
            // check volume exists
            if(!volumeService.volumeExists(taskEntry.getVolume())){
                LOG.info("Volume [{}] does not exist. Removing backup task [{}]");
                taskRepository.delete(taskEntry);
                return;
            }
            checkThreadInterruption(taskEntry);
            notificationService.notifyAboutRunningTaskProgress(taskEntry.getId(), "Starting backup task", 0);

            LOG.info("Starting backup process for volume {}", volumeId);
            LOG.info("{} task state was changed to 'in progress'", taskEntry.getId());

            // change task status
            taskEntry.setStatus(RUNNING.getStatus());
            taskRepository.save(taskEntry);

            checkThreadInterruption(taskEntry);

            notificationService.notifyAboutRunningTaskProgress(taskEntry.getId(), "Preparing temp volume", 5);

            // creating temp volume from original volume
            tempVolume = createAndAttachBackupVolume(volumeId, configurationMediator.getConfigurationId(), taskEntry);
            try {
                TimeUnit.MINUTES.sleep(1);
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
            // storage blocks on volumes that were restored from snapshots must be initialized (pulled down from Amazon S3 and written to the volume) before they can be accesed.
            // This preliminary action takes time and can cause a significant increase in the latency of an I/O operation the first time each block is accessed.
            // To avoid this performance hit in a production environment all blocks on volumes can be read before volume usage; this process is called initialization (formerly known as pre-warming).
            initializeVolume(tempVolume);
            checkThreadInterruption(taskEntry);
            notificationService.notifyAboutRunningTaskProgress(taskEntry.getId(), "Checking volume", 10);
            String attachedDeviceName = storageService.detectFsDevName(tempVolume);

            String backupDate = String.valueOf(System.currentTimeMillis());

            Volume volumeToBackup = awsCommunication.getVolume(volumeId);
            String snapshotId = tempVolume.getSnapshotId();
            String volumeType = volumeToBackup.getVolumeType();
            String iops = (volumeToBackup.getIops() != null) ? volumeToBackup
                    .getIops().toString() : "";
            String sizeGib = tempVolume.getSize().toString();
            if (volumeType.equals("")) volumeType = "gp2";
            if (volumeType.equals("standard")) volumeType = "gp2";
            backupFileName = volumeId + "." + backupDate + "." + volumeType + "." + iops + ".backup";

            BackupEntry backup = new BackupEntry(volumeId, backupFileName, backupDate, "", BackupState.INPROGRESS, snapshotId, volumeType, iops, sizeGib);
            checkThreadInterruption(taskEntry);
            notificationService.notifyAboutRunningTaskProgress(taskEntry.getId(), "Copying...", 15);
            String source = attachedDeviceName;
            LOG.info("Starting copying: " + source + " to:" + backupFileName);
            CopyingTaskProgressDto dto = new CopyingTaskProgressDto(taskEntry.getId(), 15, 80, Long.parseLong(backup.getSizeGiB()));
            storageService.copyData(source, configurationMediator.getSdfsMountPoint() + backupFileName, dto, taskEntry.getId());

            checkThreadInterruption(taskEntry);
            notificationService.notifyAboutRunningTaskProgress(taskEntry.getId(), "Detaching temp volume", 80);
            // kill initialization Process if it's alive, should be killed before volume detaching
            killInitializationVolumeProcessIfAlive(tempVolume);
            LOG.info("Detaching volume: {}", tempVolume.getVolumeId());
            awsCommunication.detachVolume(tempVolume);
            checkThreadInterruption(taskEntry);
            notificationService.notifyAboutRunningTaskProgress(taskEntry.getId(), "Deleting temp volume", 85);
            LOG.info("Deleting temporary volume: {}", tempVolume.getVolumeId());
            awsCommunication.deleteVolume(tempVolume);
            checkThreadInterruption(taskEntry);
            long backupSize = storageService.getSize(configurationMediator.getSdfsMountPoint() + backupFileName);
            long backupCreationtime = storageService.getBackupCreationTime(configurationMediator.getSdfsMountPoint() + backupFileName);
            LOG.info("Backup creation time: {}", backupCreationtime);
            LOG.info("Backup size: {}", backupSize);

            checkThreadInterruption(taskEntry);
            LOG.info("Put backup entry to the Backup List: {}", backup.getFileName());
            notificationService.notifyAboutRunningTaskProgress(taskEntry.getId(), "Backup complete", 90);
            backup.setState(BackupState.COMPLETED.getState());
            backup.setSize(String.valueOf(backupSize));
            backupRepository.save(backup);

            LOG.info(format("Backup process for volume %s finished successfully ", volumeId));
            LOG.info("Task " + taskEntry.getId() + ": Delete completed task:" + taskEntry.getId());
            LOG.info("Cleaning up previously created snapshots");
            LOG.info("Storing snapshot data: [{},{},{}]", volumeId, snapshotId, configurationMediator.getConfigurationId());

            String previousSnapshot = snapshotService.getSnapshotIdByVolumeId(volumeId);
            if (previousSnapshot != null) {
                checkThreadInterruption(taskEntry);
                notificationService.notifyAboutRunningTaskProgress(taskEntry.getId(), "Deleting previous snapshot", 95);
                LOG.info("Deleting previous snapshot {}", previousSnapshot);
                snapshotService.deleteSnapshot(previousSnapshot);
            }
            if (configurationMediator.isStoreSnapshot()) {
                snapshotService.saveSnapshot(volumeId, snapshotId);
            } else {
                awsCommunication.deleteSnapshot(snapshotId);
            }
            taskService.complete(taskEntry);
            LOG.info("Task completed.");
            checkThreadInterruption(taskEntry);
            notificationService.notifyAboutRunningTaskProgress(taskEntry.getId(), "Task complete", 100);
            retentionService.apply();
        } catch (EnhancedSnapshotsTaskInterruptedException e) {
            LOG.info("Backup process for volume {} canceled ", volumeId);
            TaskProgressDto dto = new TaskProgressDto(taskEntry.getId(), "Kill initialization process", 20, CANCELED);
            notificationService.notifyAboutTaskProgress(dto);
            // kill initialization Process if it's alive, should be killed before volume detaching
            killInitializationVolumeProcessIfAlive(tempVolume);
            deleteTempResources(tempVolume, backupFileName, dto);

            taskRepository.delete(taskEntry);
            dto.setMessage("Done");
            dto.setProgress(100);
            notificationService.notifyAboutTaskProgress(dto);
        } catch (Exception e) {
            LOG.error("Backup process for volume {} failed ", volumeId, e);
            taskEntry.setStatus(ERROR.toString());
            taskRepository.save(taskEntry);

            TaskProgressDto dto = new TaskProgressDto(taskEntry.getId(), "Killing initialization process", 20, ERROR);
            notificationService.notifyAboutTaskProgress(dto);

            // kill initialization Process if it's alive, should be killed before volume detaching
            killInitializationVolumeProcessIfAlive(tempVolume);
            deleteTempResources(tempVolume, backupFileName, dto);

            dto.setMessage("Done");
            dto.setProgress(100);
            notificationService.notifyAboutTaskProgress(dto);
        }
    }

    /**
     * Clean up resources if exception appeared
     *
     * @param tempVolume
     * @param backupFileName
     * @param dto notification transfer object
     */
    private void deleteTempResources(Volume tempVolume, String backupFileName, TaskProgressDto dto) {
        deleteTempVolume(tempVolume, dto);
        try {
            dto.setMessage("Delete temp file");
            dto.addProgress(10);
            notificationService.notifyAboutTaskProgress(dto);
            storageService.deleteFile(backupFileName);
        } catch (Exception ex) {
            //do nothing if file not found
        }
    }

    private void initializeVolume(Volume tempVolume) {
        String fileNameParam = "--filename=" + storageService.detectFsDevName(tempVolume);
        ProcessBuilder builder = new ProcessBuilder("fio", fileNameParam, "--rw=randread", "--bs=128k", "--iodepth=32", "--ioengine=libaio", "--direct=1", "--name=volume-initialize");
        try {
            LOG.info("Starting volume {} initialization", tempVolume.getVolumeId());
            Process process = builder.start();
            if (process.isAlive()) {
                LOG.debug("Volume {} initialization started successfully", tempVolume.getVolumeId());
            } else {
                LOG.info("Volume {} initialization failed to start", tempVolume.getVolumeId());
                try (BufferedReader input = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    StringBuilder errorMessage = new StringBuilder();
                    for (String line; (line = input.readLine()) != null; ) {
                        errorMessage.append(line);
                    }
                    LOG.warn(errorMessage);
                }
            }
        } catch (Exception e) {
            LOG.warn("Failed to initialize volume {}", tempVolume.getVolumeId());
            LOG.warn(e);
        }
    }

    private void killInitializationVolumeProcessIfAlive(Volume volume) {
        String fioProcNamePrefix = "fio --filename=" + storageService.detectFsDevName(volume);
        try {
            // check fio processes are alive
            Process checkFioProcessAlive = new ProcessBuilder("pgrep", "-f", fioProcNamePrefix).start();
            checkFioProcessAlive.waitFor();
            switch (checkFioProcessAlive.exitValue()) {
                case 0:
                    // fio processes are alive
                    LOG.info("Fio processes for volume {} are alive", volume.getVolumeId());
                    Process process = new ProcessBuilder("pkill", "-f", fioProcNamePrefix).start();
                    process.waitFor();
                    switch (process.exitValue()) {
                        case 0:
                            LOG.info("Fio processes for volume {} terminated", volume.getVolumeId());
                            break;
                        default: {
                            LOG.warn("Failed to terminate fio processes for volume {}", volume.getVolumeId());
                        }
                    }
                    break;
                default: {
                    // no need to terminate, fio processes are already terminated
                    LOG.info("Fio processes for volume {} already terminated, no need to stop forcibly", volume.getVolumeId());
                }
            }
        } catch (IOException | InterruptedException e) {
            LOG.warn("Exception while termination of fio processes", e);
        }
    }

    private Volume createAndAttachBackupVolume(String volumeId, String instanceId, TaskEntry taskEntry) {
        Instance instance = awsCommunication.getInstance(instanceId);
        if (instance == null) {
            LOG.error("Can't get access to {} instance" + instanceId);
            throw new AWSBackupVolumeException(MessageFormat.format("Can't get access to {} instance", instanceId));
        }

        // create snapshot for AMI
        Volume volumeSrc = awsCommunication.getVolume(volumeId);
        if (volumeSrc == null) {
            LOG.error("Can't get access to {} volume", volumeId);
            throw new AWSBackupVolumeException(MessageFormat.format("Can't get access to {} volume", volumeId));
        }

        Snapshot snapshot = awsCommunication.waitForCompleteState(awsCommunication.createSnapshot(volumeSrc));
        LOG.info("SnapshotEntry created: {}", snapshot.toString());

        // create volume
        String instanceAvailabilityZone = instance.getPlacement().getAvailabilityZone();
        Volume volumeDest = awsCommunication.waitForVolumeState(awsCommunication.createVolumeFromSnapshot(snapshot.getSnapshotId(),
                instanceAvailabilityZone, VolumeType.fromValue(taskEntry.getTempVolumeType()), taskEntry.getTempVolumeIopsPerGb()),
                VolumeState.Available);
        LOG.info("Volume created: {}", volumeDest.toString());

        // create temporary tag
        awsCommunication.createTemporaryTag(volumeDest.getVolumeId(),volumeSrc.getVolumeId());

        // mount AMI volume
        awsCommunication.attachVolume(instance, volumeDest);

        return awsCommunication.syncVolume(volumeDest);
    }

    public class AWSBackupVolumeException extends EnhancedSnapshotsException {
        public AWSBackupVolumeException(String message) {
            super(message);
        }
    }
}
