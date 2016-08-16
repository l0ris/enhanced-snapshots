package com.sungardas.enhancedsnapshots.service;

import com.sungardas.enhancedsnapshots.aws.dynamodb.model.TaskEntry.TaskEntryStatus;
import com.sungardas.enhancedsnapshots.dto.Dto;
import com.sungardas.enhancedsnapshots.dto.ExceptionDto;
import com.sungardas.enhancedsnapshots.dto.TaskProgressDto;

public interface NotificationService {

    /**
     * Send notification to user about running task progress
     *
     * @param taskId   task ID
     * @param message  message to user
     * @param progress progress in range from 0 to 100
     */
    void notifyAboutRunningTaskProgress(String taskId, String message, double progress);

    /**
     * Send notification to user about task progress
     *
     * @param taskId   task ID
     * @param message  message to user
     * @param progress progress in range from 0 to 100
     * @param status   task status
     */
    void notifyAboutTaskProgress(String taskId, String message, double progress, TaskEntryStatus status);

    void notifyAboutTaskProgress(TaskProgressDto dto);

    void notifyAboutError(ExceptionDto exceptionDto);

    void notifyUser(String broker, Dto dto);
}
