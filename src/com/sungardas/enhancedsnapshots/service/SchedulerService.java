package com.sungardas.enhancedsnapshots.service;

import com.sungardas.enhancedsnapshots.aws.dynamodb.model.TaskEntry;
import org.springframework.scheduling.support.CronTrigger;

import java.util.Set;

public interface SchedulerService {
    void addTask(TaskEntry taskEntry);

    void addTask(Task task, String cronExpression);

    /**
     * Add task to scheduler
     *
     * @param task task
     * @param cron cron trigger
     */
    void addTask(Task task, CronTrigger cron);

    void removeTask(String id);

    Set<String> getVolumeIdsWithSchedule();
}
