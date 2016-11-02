package com.sungardas.enhancedsnapshots.dto;

import com.sungardas.enhancedsnapshots.aws.dynamodb.model.TaskEntry.TaskEntryStatus;

public class TaskProgressDto implements Dto {
    private String taskId;

    private String message;

    private double progress;

    private String status;

    public TaskProgressDto() {
    }

    public TaskProgressDto(String taskId, String message, double progress, TaskEntryStatus status) {
        this.taskId = taskId;
        this.message = message;
        this.progress = progress;
        this.status = status.getStatus();
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public double getProgress() {
        return progress;
    }

    public void setProgress(double progress) {
        this.progress = progress;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(TaskEntryStatus status) {
        this.status = status.getStatus();
    }

    public void addProgress(double progress) {
        this.progress += progress;
        if (this.progress > 100) {
            this.progress = 100;
        }
    }
}
