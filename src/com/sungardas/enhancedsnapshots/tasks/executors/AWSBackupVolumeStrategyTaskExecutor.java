package com.sungardas.enhancedsnapshots.tasks.executors;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.util.concurrent.TimeUnit;

import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Snapshot;
import com.amazonaws.services.ec2.model.Volume;
import com.amazonaws.services.ec2.model.VolumeState;
import com.amazonaws.services.ec2.model.VolumeType;
import com.sungardas.enhancedsnapshots.aws.dynamodb.model.BackupEntry;
import com.sungardas.enhancedsnapshots.aws.dynamodb.model.BackupState;
import com.sungardas.enhancedsnapshots.aws.dynamodb.model.TaskEntry;
import com.sungardas.enhancedsnapshots.aws.dynamodb.repository.BackupRepository;
import com.sungardas.enhancedsnapshots.aws.dynamodb.repository.TaskRepository;
import com.sungardas.enhancedsnapshots.components.ConfigurationMediator;
import com.sungardas.enhancedsnapshots.dto.CopyingTaskProgressDto;
import com.sungardas.enhancedsnapshots.dto.TaskProgressDto;
import com.sungardas.enhancedsnapshots.dto.converter.VolumeDtoConverter;
import com.sungardas.enhancedsnapshots.enumeration.TaskProgress;
import com.sungardas.enhancedsnapshots.exception.EnhancedSnapshotsException;
import com.sungardas.enhancedsnapshots.exception.EnhancedSnapshotsTaskInterruptedException;
import com.sungardas.enhancedsnapshots.service.AWSCommunicationService;
import com.sungardas.enhancedsnapshots.service.MailService;
import com.sungardas.enhancedsnapshots.service.NotificationService;
import com.sungardas.enhancedsnapshots.service.RetentionService;
import com.sungardas.enhancedsnapshots.service.SnapshotService;
import com.sungardas.enhancedsnapshots.service.StorageService;
import com.sungardas.enhancedsnapshots.service.TaskService;
import com.sungardas.enhancedsnapshots.service.VolumeService;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static com.sungardas.enhancedsnapshots.aws.dynamodb.model.TaskEntry.TaskEntryStatus.CANCELED;
import static com.sungardas.enhancedsnapshots.aws.dynamodb.model.TaskEntry.TaskEntryStatus.ERROR;
import static com.sungardas.enhancedsnapshots.aws.dynamodb.model.TaskEntry.TaskEntryStatus.RUNNING;
import static java.lang.String.format;

@Service("awsBackupVolumeTaskExecutor")
public class AWSBackupVolumeStrategyTaskExecutor extends AbstractAWSVolumeTaskExecutor {

    private static final Logger LOG = LogManager.getLogger(AWSBackupVolumeStrategyTaskExecutor.class);

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

    @Autowired
    private MailService mailService;


    @Override
    public void execute(final TaskEntry taskEntry) {
        Volume tempVolume = taskEntry.getTempVolumeId() != null ? awsCommunication.getVolume(taskEntry.getTempVolumeId()) : null;
        try {
            notificationService.notifyAboutRunningTaskProgress(taskEntry.getId(), "Starting backup task", 0);

            LOG.info("Starting backup process for volume {}", taskEntry.getVolume());
            LOG.info("{} task state was changed to 'in progress'", taskEntry.getId());
            // change task status
            taskEntry.setStatus(RUNNING.getStatus());
            taskRepository.save(taskEntry);

            if (taskEntry.getProgress() != TaskProgress.NONE) {
                switch (taskEntry.getProgress()) {
                    case ATTACHING_VOLUME:
                    case CREATING_TEMP_VOLUME:
                    case WAITING_TEMP_VOLUME:
                    case COPYING: {
                        try {
                            detachingTempVolumeStep(taskEntry, tempVolume);
                        } catch (Exception e) {
                            //skip
                        }
                        try {
                            deletingTempVolumeStep(taskEntry, tempVolume);
                        } catch (Exception e) {
                            //skip
                        }
                        try {
                            storageService.deleteFile(getBackupName(taskEntry));
                        } catch (Exception e) {
                            //skip
                        }
                        setProgress(taskEntry, TaskProgress.CREATING_TEMP_VOLUME);
                        break;
                    }
                    case CREATING_SNAPSHOT: {
                        if (taskEntry.getTempSnapshotId() != null) {
                            setProgress(taskEntry, TaskProgress.WAITING_SNAPSHOT);
                        }
                        break;
                    }
                    case DETACHING_TEMP_VOLUME: {
                        setProgress(taskEntry, TaskProgress.DELETING_TEMP_VOLUME);
                    }
                }
            }
            switch (taskEntry.getProgress()) {
                case NONE: {
                    // check volume exists
                    if (!volumeService.volumeExists(taskEntry.getVolume())) {
                        LOG.info("Volume [{}] does not exist. Removing backup task [{}]");
                        taskRepository.delete(taskEntry);
                        return;
                    }
                }
                case STARTED: {
                    startedStep(taskEntry);
                }
                case CREATING_SNAPSHOT: {
                    creatingSnapshotStep(taskEntry);
                }
                case WAITING_SNAPSHOT: {
                    waitingSnapshotStep(taskEntry);
                }
                case CREATING_TEMP_VOLUME: {
                    tempVolume = creatingTempVolumeStep(taskEntry);
                }
                case WAITING_TEMP_VOLUME: {
                    waitingTempVolumeStep(taskEntry, tempVolume);
                }
                case ATTACHING_VOLUME: {
                    tempVolume = attachingVolumeStep(taskEntry, tempVolume);
                }
                case COPYING: {
                    copyingStep(taskEntry, tempVolume);
                }
                case DETACHING_TEMP_VOLUME: {
                    detachingTempVolumeStep(taskEntry, tempVolume);
                }
                case DELETING_TEMP_VOLUME: {
                    deletingTempVolumeStep(taskEntry, tempVolume);
                }
                case CLEANING_TEMP_RESOURCES: {
                    cleaningTempResourcesStep(taskEntry);
                }
            }
        } catch (EnhancedSnapshotsTaskInterruptedException e) {
            LOG.info("Backup process for volume {} canceled ", taskEntry.getVolume());
            setProgress(taskEntry, TaskProgress.FAIL_CLEANING);
            TaskProgressDto dto = new TaskProgressDto(taskEntry.getId(), "Kill initialization process", 20, CANCELED);
            notificationService.notifyAboutTaskProgress(dto);
            // kill initialization Process if it's alive, should be killed before volume detaching
            killInitializationVolumeProcessIfAlive(tempVolume);
            deleteTempResources(tempVolume, getBackupName(taskEntry), dto);

            taskRepository.delete(taskEntry);
            dto.setMessage("Done");
            dto.setProgress(100);
            notificationService.notifyAboutTaskProgress(dto);
            mailService.notifyAboutSystemStatus("Backup task for volume with id: " + taskEntry.getVolume() + " was canceled");
        } catch (Exception e) {
            LOG.error("Backup process for volume {} failed ", taskEntry.getVolume(), e);
            setProgress(taskEntry, TaskProgress.FAIL_CLEANING);
            taskEntry.setStatus(ERROR.toString());
            taskRepository.save(taskEntry);

            TaskProgressDto dto = new TaskProgressDto(taskEntry.getId(), "Killing initialization process", 20, ERROR);
            notificationService.notifyAboutTaskProgress(dto);

            // kill initialization Process if it's alive, should be killed before volume detaching
            killInitializationVolumeProcessIfAlive(tempVolume);
            deleteTempResources(tempVolume, getBackupName(taskEntry), dto);

            dto.setMessage("Done");
            dto.setProgress(100);
            notificationService.notifyAboutTaskProgress(dto);
            mailService.notifyAboutError(taskEntry, e);
        }
    }

    private void startedStep(TaskEntry taskEntry) {
        setProgress(taskEntry, TaskProgress.STARTED);
        checkThreadInterruption(taskEntry);
        taskEntry.setStartTime(System.currentTimeMillis());
    }

    private void creatingSnapshotStep(TaskEntry taskEntry) {
        setProgress(taskEntry, TaskProgress.CREATING_SNAPSHOT);
        Volume volumeSrc = awsCommunication.getVolume(taskEntry.getVolume());
        if (volumeSrc == null) {
            LOG.error("Can't get access to {} volume", taskEntry.getVolume());
            throw new AWSBackupVolumeException(MessageFormat.format("Can't get access to {} volume", taskEntry.getVolume()));
        }

        taskEntry.setTempSnapshotId(awsCommunication.createSnapshot(volumeSrc).getSnapshotId());
        taskRepository.save(taskEntry);
    }

    private void waitingSnapshotStep(TaskEntry taskEntry) {
        setProgress(taskEntry, TaskProgress.WAITING_SNAPSHOT);
        Snapshot snapshot = awsCommunication.waitForCompleteState(awsCommunication.getSnapshot(taskEntry.getTempSnapshotId()));
        LOG.info("SnapshotEntry created: {}", snapshot.toString());
    }

    private Volume creatingTempVolumeStep(TaskEntry taskEntry) {
        // create volume
        checkThreadInterruption(taskEntry);
        setProgress(taskEntry, TaskProgress.CREATING_TEMP_VOLUME);
        Instance instance = awsCommunication.getInstance(configurationMediator.getConfigurationId());
        if (instance == null) {
            LOG.error("Can't get access to {} instance" + configurationMediator.getConfigurationId());
            throw new AWSBackupVolumeException(MessageFormat.format("Can't get access to {} instance", configurationMediator.getConfigurationId()));
        }
        String instanceAvailabilityZone = instance.getPlacement().getAvailabilityZone();
        Volume tempVolume = awsCommunication.createVolumeFromSnapshot(taskEntry.getTempSnapshotId(), instanceAvailabilityZone,
                VolumeType.fromValue(taskEntry.getTempVolumeType()), taskEntry.getTempVolumeIopsPerGb());
        taskEntry.setTempVolumeId(tempVolume.getVolumeId());
        taskRepository.save(taskEntry);

        // create temporary tag
        awsCommunication.createTemporaryTag(taskEntry.getTempVolumeId(), taskEntry.getVolume());
        return tempVolume;
    }

    private void waitingTempVolumeStep(TaskEntry taskEntry, Volume tempVolume) {
        checkThreadInterruption(taskEntry);
        setProgress(taskEntry, TaskProgress.WAITING_TEMP_VOLUME);
        Volume volumeDest = awsCommunication.waitForVolumeState(tempVolume, VolumeState.Available);
        LOG.info("Volume created: {}", volumeDest.toString());
    }

    private Volume attachingVolumeStep(TaskEntry taskEntry, Volume tempVolume) {
        setProgress(taskEntry, TaskProgress.ATTACHING_VOLUME);
        // mount volume
        awsCommunication.attachVolume(awsCommunication.getInstance(configurationMediator.getConfigurationId()), tempVolume);

        tempVolume = awsCommunication.syncVolume(tempVolume);

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
        return tempVolume;
    }

    private void copyingStep(TaskEntry taskEntry, Volume tempVolume) throws IOException, InterruptedException {
        setProgress(taskEntry, TaskProgress.COPYING);
        checkThreadInterruption(taskEntry);

        Volume volumeToBackup = awsCommunication.getVolume(taskEntry.getVolume());
        String volumeType = volumeToBackup.getVolumeType();
        String iops = (volumeToBackup.getIops() != null) ? volumeToBackup
                .getIops().toString() : "";
        String sizeGib = tempVolume.getSize().toString();
        if (volumeType.equals("")) {
            volumeType = "gp2";
        }
        if (volumeType.equals("standard")) {
            volumeType = "gp2";
        }
        String backupFileName = getBackupName(taskEntry);
        BackupEntry backup = new BackupEntry(taskEntry.getVolume(),
                VolumeDtoConverter.convert(awsCommunication.getVolume(taskEntry.getVolume())).getVolumeName(),
                backupFileName, taskEntry.getStartTime() + "", "", BackupState.INPROGRESS, taskEntry.getTempSnapshotId(), volumeType, iops, sizeGib);
        checkThreadInterruption(taskEntry);
        notificationService.notifyAboutRunningTaskProgress(taskEntry.getId(), "Copying...", 15);
        String source = storageService.detectFsDevName(tempVolume);
        LOG.info("Starting copying: " + source + " to:" + backupFileName);
        CopyingTaskProgressDto dto = new CopyingTaskProgressDto(taskEntry.getId(), 15, 80, Long.parseLong(backup.getSizeGiB()));
        storageService.copyData(source, configurationMediator.getSdfsMountPoint() + backupFileName, dto, taskEntry.getId());

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

        LOG.info(format("Backup process for volume %s finished successfully ", taskEntry.getVolume()));
        LOG.info("Task " + taskEntry.getId() + ": Delete completed task:" + taskEntry.getId());
        LOG.info("Cleaning up previously created snapshots");
        LOG.info("Storing snapshot data: [{},{},{}]", taskEntry.getVolume(), taskEntry.getTempSnapshotId(), configurationMediator.getConfigurationId());
    }

    private void detachingTempVolumeStep(TaskEntry taskEntry, Volume tempVolume) {
        setProgress(taskEntry, TaskProgress.DETACHING_TEMP_VOLUME);
        checkThreadInterruption(taskEntry);
        notificationService.notifyAboutRunningTaskProgress(taskEntry.getId(), "Detaching temp volume", 80);
        // kill initialization Process if it's alive, should be killed before volume detaching
        killInitializationVolumeProcessIfAlive(tempVolume);
        LOG.info("Detaching volume: {}", tempVolume.getVolumeId());
        awsCommunication.detachVolume(tempVolume);
    }

    private void deletingTempVolumeStep(TaskEntry taskEntry, Volume tempVolume) {
        setProgress(taskEntry, TaskProgress.DELETING_TEMP_VOLUME);
        notificationService.notifyAboutRunningTaskProgress(taskEntry.getId(), "Deleting temp volume", 85);
        LOG.info("Deleting temporary volume: {}", tempVolume.getVolumeId());
        awsCommunication.deleteVolume(tempVolume);
    }

    private void cleaningTempResourcesStep(TaskEntry taskEntry) {
        setProgress(taskEntry, TaskProgress.CLEANING_TEMP_RESOURCES);
        String previousSnapshot = snapshotService.getSnapshotIdByVolumeId(taskEntry.getVolume());
        if (previousSnapshot != null) {
            checkThreadInterruption(taskEntry);
            notificationService.notifyAboutRunningTaskProgress(taskEntry.getId(), "Deleting previous snapshot", 95);
            LOG.info("Deleting previous snapshot {}", previousSnapshot);
            snapshotService.deleteSnapshot(previousSnapshot);
        }
        if (configurationMediator.isStoreSnapshot()) {
            snapshotService.saveSnapshot(taskEntry.getVolume(), taskEntry.getTempSnapshotId());
        } else {
            awsCommunication.deleteSnapshot(taskEntry.getTempSnapshotId());
        }
        taskService.complete(taskEntry);
        LOG.info("Task completed.");
        checkThreadInterruption(taskEntry);
        notificationService.notifyAboutRunningTaskProgress(taskEntry.getId(), "Task complete", 100);
        retentionService.apply();
        mailService.notifyAboutSuccess(taskEntry);
    }


    public class AWSBackupVolumeException extends EnhancedSnapshotsException {
        public AWSBackupVolumeException(String message) {
            super(message);
        }
    }

    /**
     * Clean up resources if exception appeared
     *
     * @param tempVolume
     * @param backupFileName
     * @param dto            notification transfer object
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

    private String getBackupName(TaskEntry taskEntry) {
        Volume volumeToBackup = awsCommunication.getVolume(taskEntry.getVolume());
        String volumeType = volumeToBackup.getVolumeType();
        String iops = (volumeToBackup.getIops() != null) ? volumeToBackup
                .getIops().toString() : "";
        if (volumeType.equals("")) {
            volumeType = "gp2";
        }
        if (volumeType.equals("standard")) {
            volumeType = "gp2";
        }
        return taskEntry.getVolume() + "." + taskEntry.getStartTime() + "." + volumeType + "." + iops + ".backup";
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
}
