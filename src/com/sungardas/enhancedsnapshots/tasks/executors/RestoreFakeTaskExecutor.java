package com.sungardas.enhancedsnapshots.tasks.executors;

import com.sungardas.enhancedsnapshots.aws.dynamodb.model.TaskEntry;
import com.sungardas.enhancedsnapshots.aws.dynamodb.repository.TaskRepository;
import com.sungardas.enhancedsnapshots.service.NotificationService;
import com.sungardas.enhancedsnapshots.service.TaskService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service ("awsRestoreVolumeTaskExecutor")
@Profile("dev")
public class RestoreFakeTaskExecutor implements TaskExecutor {
	private static final Logger LOG = LogManager.getLogger(RestoreFakeTaskExecutor.class);

	@Autowired
	private TaskRepository taskRepository;

	@Autowired
	private NotificationService notificationService;

	@Autowired
	private TaskService taskService;


	@Override
	public void execute(TaskEntry taskEntry) {
		notificationService.notifyAboutRunningTaskProgress(taskEntry.getId(), "Starting restore...", 0);
		LOG.info("Task " + taskEntry.getId() + ": Change task state to 'in progress'");
		taskEntry.setStatus("running");
		taskRepository.save(taskEntry);
		String[] options = taskEntry.getOptions().split(", ");
		String targetZone = options[1];

		String sourceFile = options[0];
		LOG.info("restore from: {}; restore to az: {}", sourceFile, targetZone);
		notificationService.notifyAboutRunningTaskProgress(taskEntry.getId(), "Restoring...", 50);

		try {
			TimeUnit.SECONDS.sleep(10);
		} catch (InterruptedException ignored) {
		}

		LOG.info("Task " + taskEntry.getId() + ": Delete completed task:" + taskEntry.getId());
		taskService.complete(taskEntry);
		LOG.info("Task completed.");
		notificationService.notifyAboutRunningTaskProgress(taskEntry.getId(), "Task complete", 100);

	}

}
