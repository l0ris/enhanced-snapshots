package com.sungardas.enhancedsnapshots.tasks.executors;

import java.util.concurrent.TimeUnit;

import com.sungardas.enhancedsnapshots.aws.dynamodb.model.BackupEntry;
import com.sungardas.enhancedsnapshots.aws.dynamodb.model.BackupState;
import com.sungardas.enhancedsnapshots.aws.dynamodb.model.TaskEntry;
import com.sungardas.enhancedsnapshots.aws.dynamodb.repository.BackupRepository;
import com.sungardas.enhancedsnapshots.aws.dynamodb.repository.TaskRepository;
import com.sungardas.enhancedsnapshots.dto.CopyingTaskProgressDto;
import com.sungardas.enhancedsnapshots.exception.EnhancedSnapshotsInterruptedException;
import com.sungardas.enhancedsnapshots.service.NotificationService;
import com.sungardas.enhancedsnapshots.service.RetentionService;
import com.sungardas.enhancedsnapshots.service.TaskService;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

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

    @Override
    public void execute(TaskEntry taskEntry) {
        LOG.info("Task " + taskEntry.getId() + ": Change task state to 'inprogress'");
        notificationService.notifyAboutTaskProgress(taskEntry.getId(), "Starting delete task", 0);

        taskEntry.setStatus(RUNNING.getStatus());
        taskRepository.save(taskEntry);

        notificationService.notifyAboutTaskProgress(taskEntry.getId(), "Finding source file", 30);
        LOG.info(taskEntry.toString());
        String timestamp = Long.toString(System.currentTimeMillis());
        String volumeId = taskEntry.getVolume();
        String filename = volumeId + "." + timestamp + ".backup";
        notificationService.notifyAboutTaskProgress(taskEntry.getId(), "Checking volume", 60);
        BackupEntry backup = new BackupEntry(taskEntry.getVolume(), filename, timestamp, "123456789", BackupState.COMPLETED,
                "snap-00100110","gp2","3000", "10");
        LOG.info("Task " + taskEntry.getId() + ":put backup info'");
        backupRepository.save(backup);

        CopyingTaskProgressDto dto = new CopyingTaskProgressDto(taskEntry.getId(), 60, 100, Long.parseLong(backup.getSizeGiB()));
        for (int i = 0; i <= 10; i++) {
            if (Thread.interrupted()) {
                throw new EnhancedSnapshotsInterruptedException("Task interrupted");
            }
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException ignored) {
            }
            dto.setCopyingProgress(i * 100000000);
            notificationService.notifyAboutTaskProgress(dto);
        }

        notificationService.notifyAboutTaskProgress(taskEntry.getId(), "Task complete", 100);
        LOG.info("Task " + taskEntry.getId() + ": Delete completed task:" + taskEntry.getId());
        taskService.complete(taskEntry);
        LOG.info("Task completed.");
        retentionService.apply();
    }

}
