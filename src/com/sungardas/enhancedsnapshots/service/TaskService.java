package com.sungardas.enhancedsnapshots.service;

import com.sungardas.enhancedsnapshots.aws.dynamodb.model.TaskEntry;
import com.sungardas.enhancedsnapshots.dto.TaskDto;

import java.util.List;
import java.util.Map;

public interface TaskService {
    Map<String, String> createTask(TaskDto taskDto);

    List<TaskDto> getAllTasks();

    List<TaskDto> getAllRegularTasks(String volumeId);

    void removeTask(String Id);

    boolean exists(String id);

    void updateTask(TaskDto taskInfo);

    List<TaskDto> getAllTasks(String volumeId);

    void complete(TaskEntry taskEntry);

    boolean isQueueFull();

    boolean isCanceled(String taskId);
}
