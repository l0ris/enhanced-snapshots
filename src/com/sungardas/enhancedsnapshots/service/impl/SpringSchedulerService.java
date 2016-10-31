package com.sungardas.enhancedsnapshots.service.impl;

import com.amazonaws.AmazonClientException;
import com.sungardas.enhancedsnapshots.aws.dynamodb.model.TaskEntry;
import com.sungardas.enhancedsnapshots.aws.dynamodb.repository.TaskRepository;
import com.sungardas.enhancedsnapshots.dto.ExceptionDto;
import com.sungardas.enhancedsnapshots.exception.EnhancedSnapshotsException;
import com.sungardas.enhancedsnapshots.service.*;
import com.sungardas.enhancedsnapshots.util.SystemUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.DependsOn;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;

@Service
@DependsOn("ConfigurationMediator")
public class SpringSchedulerService implements SchedulerService, MasterInitialization {

    private static final Logger LOG = LogManager.getLogger(SpringSchedulerService.class);

    @Qualifier("taskScheduler")
    @Autowired
    private TaskScheduler scheduler;

    @Autowired
    private TaskRepository taskRepository;

    private Map<String, ScheduledFuture> jobs = new HashMap<>();

    @Autowired
    private VolumeService volumeService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private TaskService taskService;

    @Override
    public void init() {
        if (!SystemUtils.clusterMode()) {
            try {
                List<TaskEntry> tasks = taskRepository.findByRegular(Boolean.TRUE.toString());
                for (TaskEntry taskEntry : tasks) {
                    try {
                        addTask(taskEntry);
                    } catch (EnhancedSnapshotsException e) {
                        LOG.error(e);
                    }
                }
            } catch (AmazonClientException e) {
                LOG.error(e);
            }
        }
    }

    @Override
    public void addTask(TaskEntry taskEntry) {
        if (TaskEntry.TaskEntryType.BACKUP.getType().equals(taskEntry.getType()) && taskEntry.getCron() != null && !taskEntry.getCron().isEmpty()) {
            if (Boolean.valueOf(taskEntry.getEnabled())) {
                addTask(new TaskImpl(taskEntry), taskEntry.getCron());
            }
            volumeService.expireCache();
        } else {
            throw new EnhancedSnapshotsException("Invalid task: " + taskEntry);
        }
    }

    @Override
    public void addTask(Task task, String cronExpression) {
        addTask(task, new CronTrigger("0 " + cronExpression));
    }

    @Override
    public void addTask(Task task, CronTrigger cron) {
        ScheduledFuture<?> future = scheduler.schedule(task, cron);
        jobs.put(task.getId(), future);
    }

    @Override
    public void removeTask(String id) {
        ScheduledFuture future = jobs.remove(id);
        if (future != null) {
            future.cancel(false);
            volumeService.expireCache();
        } else {
            LOG.debug("Task with id: {} not found", id);
        }
    }

    @Override
    public Set<String> getVolumeIdsWithSchedule() {
        Set<String> result = taskRepository.findByRegularAndEnabled(Boolean.TRUE.toString(), Boolean.TRUE.toString()).stream().map(TaskEntry::getVolume).collect(Collectors.toSet());
        return result;
    }

    private class TaskImpl implements Task {

        private TaskEntry taskEntry;

        public TaskImpl(TaskEntry taskEntry) {
            this.taskEntry = taskEntry;
        }

        @Override
        public void run() {
            if (!volumeService.volumeExists(taskEntry.getVolume())) {
                LOG.info("Volume {} does not exist any more. Removing scheduler [{}] [] for backups.",
                        taskEntry.getVolume(), taskEntry.getSchedulerName(), taskEntry.getCron());
                taskRepository.delete(taskEntry.getId());
                removeTask(taskEntry.getId());
                return;
            }
            if (taskService.isQueueFull()) {
                notificationService.notifyAboutError(new ExceptionDto("Task creation error", "Task queue is full"));
            } else {
                taskEntry.setId(UUID.randomUUID().toString());
                taskEntry.setSchedulerManual(false);
                taskEntry.setRegular(false);
                taskEntry.setSchedulerTime(String.valueOf(DateTime.now().getMillis()));
                taskRepository.save(taskEntry);
            }
        }

        @Override
        public String getId() {
            return taskEntry.getId();
        }

    }
}
