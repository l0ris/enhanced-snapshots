package com.sungardas.enhancedsnapshots.tasks.executors;

import com.sungardas.enhancedsnapshots.aws.dynamodb.model.TaskEntry;


public interface TaskExecutor {
	void execute(TaskEntry taskEntry);
}
