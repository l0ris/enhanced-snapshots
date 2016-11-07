package com.sungardas.enhancedsnapshots.tasks.executors;

import com.sungardas.enhancedsnapshots.aws.dynamodb.model.BackupEntry;
import com.sungardas.enhancedsnapshots.aws.dynamodb.model.TaskEntry;
import com.sungardas.enhancedsnapshots.aws.dynamodb.repository.BackupRepository;
import com.sungardas.enhancedsnapshots.aws.dynamodb.repository.TaskRepository;
import com.sungardas.enhancedsnapshots.dto.ExceptionDto;
import com.sungardas.enhancedsnapshots.enumeration.TaskProgress;
import com.sungardas.enhancedsnapshots.exception.EnhancedSnapshotsException;
import com.sungardas.enhancedsnapshots.service.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import static com.sungardas.enhancedsnapshots.aws.dynamodb.model.TaskEntry.TaskEntryStatus.ERROR;
import static com.sungardas.enhancedsnapshots.aws.dynamodb.model.TaskEntry.TaskEntryStatus.RUNNING;

@Service("awsDeleteTaskExecutor")
@Profile("prod")
public class AWSDeleteTaskExecutor extends AbstractAWSVolumeTaskExecutor {

    private static final Logger LOG = LogManager.getLogger(AWSDeleteTaskExecutor.class);

    @Autowired
    private NotificationService notificationService;
    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private BackupRepository backupRepository;
    @Autowired
    private StorageService storageService;
    @Autowired
    private TaskService taskService;
    @Autowired
    private SnapshotService snapshotService;

    @Autowired
    private MailService mailService;
    @Override
    public void execute(TaskEntry taskEntry) {
        LOG.info("Task " + taskEntry.getId() + ": Change task state to 'running'");
        notificationService.notifyAboutRunningTaskProgress(taskEntry.getId(), "Starting delete task", 0);

        taskEntry.setStatus(RUNNING.getStatus());
        taskRepository.save(taskEntry);

        notificationService.notifyAboutRunningTaskProgress(taskEntry.getId(), "Finding source file", 30);
        BackupEntry backupEntry = backupRepository.findOne(taskEntry.getSourceFileName());

        try {
            notificationService.notifyAboutRunningTaskProgress(taskEntry.getId(), "Deleting file...", 60);
            try {
                storageService.deleteFile(backupEntry.getFileName());
            } catch (Exception e) {
                // skip
            }
            try {
                snapshotService.deleteSnapshot(backupEntry.getSnapshotId());
            } catch (Exception e) {
                // skip
            }
            try {
                backupRepository.delete(backupEntry);
            } catch (Exception e) {
                // skip
            }
            taskService.complete(taskEntry);
            LOG.info("Task " + taskEntry.getId() + ": Change task state to 'complete'");
            notificationService.notifyAboutRunningTaskProgress(taskEntry.getId(), "Task complete", 100);
            mailService.notifyAboutSuccess(taskEntry);
            setProgress(taskEntry, TaskProgress.DONE);
        } catch (EnhancedSnapshotsException e){
            LOG.error(e);
            notificationService.notifyAboutError(new ExceptionDto("Delete task has failed", e.getLocalizedMessage()));
            taskEntry.setStatus(ERROR.getStatus());
            taskRepository.save(taskEntry);
            mailService.notifyAboutError(taskEntry, e);
            setProgress(taskEntry, TaskProgress.DONE);
        }
    }
}
