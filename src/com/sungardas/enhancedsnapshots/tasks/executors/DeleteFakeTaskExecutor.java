package com.sungardas.enhancedsnapshots.tasks.executors;

import com.sungardas.enhancedsnapshots.aws.dynamodb.model.BackupEntry;
import com.sungardas.enhancedsnapshots.aws.dynamodb.model.TaskEntry;
import com.sungardas.enhancedsnapshots.aws.dynamodb.repository.BackupRepository;
import com.sungardas.enhancedsnapshots.aws.dynamodb.repository.TaskRepository;
import com.sungardas.enhancedsnapshots.exception.DataAccessException;
import com.sungardas.enhancedsnapshots.service.NotificationService;
import com.sungardas.enhancedsnapshots.service.TaskService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import static com.sungardas.enhancedsnapshots.aws.dynamodb.model.TaskEntry.TaskEntryStatus.ERROR;
import static com.sungardas.enhancedsnapshots.aws.dynamodb.model.TaskEntry.TaskEntryStatus.RUNNING;

@Service("awsDeleteTaskExecutor")
@Profile("dev")
public class DeleteFakeTaskExecutor implements TaskExecutor {

    private static final Logger LOG = LogManager.getLogger(DeleteFakeTaskExecutor.class);

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private BackupRepository backupRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private TaskService taskService;


    @Override
    public void execute(TaskEntry taskEntry) {
        LOG.info("Task " + taskEntry.getId() + ": Change task state to 'running'");
        notificationService.notifyAboutRunningTaskProgress(taskEntry.getId(), "Delete task started", 0);
        taskEntry.setStatus(RUNNING.getStatus());
        taskRepository.save(taskEntry);

        BackupEntry backupEntry = new BackupEntry();
        backupEntry.setVolumeId(taskEntry.getVolume());
        backupEntry.setFileName(taskEntry.getOptions());

        try {
            notificationService.notifyAboutRunningTaskProgress(taskEntry.getId(), "Deleting", 50);
            backupRepository.delete(backupEntry);
            taskService.complete(taskEntry);
            notificationService.notifyAboutRunningTaskProgress(taskEntry.getId(), "Delete complete", 100);
            LOG.info("Task " + taskEntry.getId() + ": Change task state to 'complete'");
        } catch (DataAccessException e){
            LOG.error(e);
            taskEntry.setStatus(ERROR.getStatus());
            taskRepository.save(taskEntry);
        }
    }
}
