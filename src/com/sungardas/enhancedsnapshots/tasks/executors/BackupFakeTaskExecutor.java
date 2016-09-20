package com.sungardas.enhancedsnapshots.tasks.executors;

import com.sungardas.enhancedsnapshots.aws.dynamodb.model.BackupEntry;
import com.sungardas.enhancedsnapshots.aws.dynamodb.model.BackupState;
import com.sungardas.enhancedsnapshots.aws.dynamodb.model.TaskEntry;
import com.sungardas.enhancedsnapshots.aws.dynamodb.repository.BackupRepository;
import com.sungardas.enhancedsnapshots.aws.dynamodb.repository.TaskRepository;
import com.sungardas.enhancedsnapshots.dto.CopyingTaskProgressDto;
import com.sungardas.enhancedsnapshots.exception.EnhancedSnapshotsInterruptedException;
import com.sungardas.enhancedsnapshots.exception.EnhancedSnapshotsTaskInterruptedException;
import com.sungardas.enhancedsnapshots.service.MailService;
import com.sungardas.enhancedsnapshots.service.NotificationService;
import com.sungardas.enhancedsnapshots.service.RetentionService;
import com.sungardas.enhancedsnapshots.service.TaskService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

import static com.sungardas.enhancedsnapshots.aws.dynamodb.model.TaskEntry.TaskEntryStatus.CANCELED;
import static com.sungardas.enhancedsnapshots.aws.dynamodb.model.TaskEntry.TaskEntryStatus.RUNNING;

@Service("awsBackupVolumeTaskExecutor")
@Profile("dev")
public class BackupFakeTaskExecutor implements TaskExecutor {
	private static final Logger LOG = LogManager.getLogger(BackupFakeTaskExecutor.class);
    
    @Autowired
	private TaskRepository taskRepository;
    @Autowired
	private BackupRepository backupRepository;
    
    @Autowired
    private RetentionService retentionService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private MailService mailService;

    @Override
    public void execute(TaskEntry taskEntry) {
        try {
            LOG.info("Task " + taskEntry.getId() + ": Change task state to 'inprogress'");
            notificationService.notifyAboutRunningTaskProgress(taskEntry.getId(), "Starting delete task", 0);

            taskEntry.setStatus(RUNNING.getStatus());
            taskRepository.save(taskEntry);

            checkThreadInterruption(taskEntry);

            notificationService.notifyAboutRunningTaskProgress(taskEntry.getId(), "Finding source file", 30);
            LOG.info(taskEntry.toString());
            String timestamp = Long.toString(System.currentTimeMillis());
            String volumeId = taskEntry.getVolume();
            String filename = volumeId + "." + timestamp + ".backup";
            notificationService.notifyAboutRunningTaskProgress(taskEntry.getId(), "Checking volume", 60);
            BackupEntry backup = new BackupEntry(taskEntry.getVolume(), filename, timestamp, "123456789", BackupState.COMPLETED,
                    "snap-00100110", "gp2", "3000", "10");
            LOG.info("Task " + taskEntry.getId() + ":put backup info'");
            backupRepository.save(backup);

            CopyingTaskProgressDto dto = new CopyingTaskProgressDto(taskEntry.getId(), 60, 100, Long.parseLong(backup.getSizeGiB()));
            for (int i = 0; i <= 10; i++) {
                checkThreadInterruption(taskEntry);
                sleep();
                dto.setCopyingProgress(i * 100000000);
                notificationService.notifyAboutTaskProgress(dto);
            }
            checkThreadInterruption(taskEntry);
            notificationService.notifyAboutRunningTaskProgress(taskEntry.getId(), "Task complete", 100);
            LOG.info("Task " + taskEntry.getId() + ": Delete completed task:" + taskEntry.getId());
            taskService.complete(taskEntry);
            LOG.info("Task completed.");
            mailService.notifyAboutSuccess(taskEntry);
            retentionService.apply();
        } catch (Exception e) {
            notificationService.notifyAboutTaskProgress(taskEntry.getId(), "Delete temp resources", 40, CANCELED);
            sleep();
            notificationService.notifyAboutTaskProgress(taskEntry.getId(), "Detach volume", 80, CANCELED);
            sleep();
            notificationService.notifyAboutTaskProgress(taskEntry.getId(), "Successfully canceled", 100, CANCELED);
            taskRepository.delete(taskEntry);
        }
    }


    protected void checkThreadInterruption(TaskEntry taskEntry) {
        if (Thread.interrupted()) {
            LOG.info("Task {} was interrupted.", taskEntry.getId());
            throw new EnhancedSnapshotsInterruptedException("Task interrupted");
        }
        if (taskService.isCanceled(taskEntry.getId())) {
            LOG.info("Task {} was canceled.", taskEntry.getId());
            throw new EnhancedSnapshotsTaskInterruptedException("Task canceled");
        }
    }

    private void sleep() {
        try {
            TimeUnit.SECONDS.sleep(3);
        } catch (InterruptedException ignored) {
        }
    }
}
