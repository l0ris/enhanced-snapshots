package com.sungardas.enhancedsnapshots.tasks;

import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.amazonaws.auth.AWSCredentials;
import com.sungardas.enhancedsnapshots.aws.dynamodb.model.TaskEntry;
import com.sungardas.enhancedsnapshots.aws.dynamodb.repository.BackupRepository;
import com.sungardas.enhancedsnapshots.aws.dynamodb.repository.TaskRepository;
import com.sungardas.enhancedsnapshots.service.AWSCommunicationService;

@Component
@Scope("prototype")
@Profile("dev")
public class RestoreFakeTask implements RestoreTask {
	private static final Logger LOG = LogManager.getLogger(RestoreFakeTask.class);

	@Autowired
	private TaskRepository taskRepository;
	@Autowired
	private BackupRepository backupRepository;

	@Autowired
	private AWSCredentials amazonAWSCredentials;

	@Autowired
	private AWSCommunicationService awsCommunication;

	private TaskEntry taskEntry;

	@Override
	public void setTaskEntry(TaskEntry taskEntry) {
		this.taskEntry = taskEntry;

	}

	@Override
	public void execute() {
		LOG.info("Task " + taskEntry.getId() + ": Change task state to 'inprogress'");
		taskEntry.setStatus("running");
		taskRepository.save(taskEntry);
		String[] options = taskEntry.getOptions().split(", ");
		String targetZone = options[1];

		String sourceFile = options[0];
		LOG.info("restore from: {}; restore to az: {}", sourceFile, targetZone);
		String instanceId = taskEntry.getInstanceId();

		try {
			TimeUnit.SECONDS.sleep(10);
		} catch (InterruptedException ignored) {
		}

		LOG.info("Task " + taskEntry.getId() + ": Delete completed task:" + taskEntry.getId());
		taskRepository.delete(taskEntry);
		LOG.info("Task completed.");

	}

}
