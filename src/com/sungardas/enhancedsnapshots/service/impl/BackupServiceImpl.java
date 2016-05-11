package com.sungardas.enhancedsnapshots.service.impl;

import com.sungardas.enhancedsnapshots.aws.dynamodb.model.BackupEntry;
import com.sungardas.enhancedsnapshots.aws.dynamodb.model.TaskEntry;
import com.sungardas.enhancedsnapshots.aws.dynamodb.repository.BackupRepository;
import com.sungardas.enhancedsnapshots.aws.dynamodb.repository.TaskRepository;
import com.sungardas.enhancedsnapshots.exception.DataException;
import com.sungardas.enhancedsnapshots.service.BackupService;
import com.sungardas.enhancedsnapshots.service.ConfigurationService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static com.sungardas.enhancedsnapshots.aws.dynamodb.model.TaskEntry.TaskEntryType.DELETE;

@Service
public class BackupServiceImpl implements BackupService {

    private static final Logger LOG = LogManager.getLogger(BackupServiceImpl.class);

    private static final String BACKUP_FILE_EXT = ".backup";

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private BackupRepository backupRepository;

    @Autowired
    private TaskRepository taskRepository;

    private String instanceId;

    @PostConstruct
    private void init() {
        instanceId = configurationService.getConfiguration().getConfigurationId();
    }

    @Override
    public void deleteBackup(String backupName, String user) {
        TaskEntry taskEntry = getDeleteTask(backupName + BACKUP_FILE_EXT, user, true);
        if (taskRepository.findByVolumeAndTypeAndInstanceIdAndOptions(taskEntry.getVolume(),
                taskEntry.getType(), instanceId, taskEntry.getOptions()).isEmpty()) {
            taskRepository.save(taskEntry);
        } else {
            LOG.error("Task already exist: {}", taskEntry);
            throw new DataException("Task already exist");
        }
    }

    @Override
    public List<BackupEntry> getBackupList(String volumeId) {
        return backupRepository.get(volumeId, instanceId);
    }

    @Override
    public void deleteBackup(Collection<BackupEntry> backupEntries, String user) {
        LOG.debug("Removing backups: {}", backupEntries);
        List<TaskEntry> tasks = new ArrayList<>();

        for (BackupEntry entry : backupEntries) {
            TaskEntry taskEntry = getDeleteTask(entry.getFileName(), user, false);
            if (taskRepository.findByVolumeAndTypeAndInstanceIdAndOptions(taskEntry.getVolume(),
                    taskEntry.getType(), instanceId, taskEntry.getOptions()).isEmpty()) {

                tasks.add(getDeleteTask(entry.getFileName(), user, false));
            } else {
                LOG.debug("Task ignored: {}", taskEntry);
            }
        }

        taskRepository.save(tasks);
    }

    private String getVolumeId(String backupName) {
        return backupName.substring(0, 12);
    }

    private TaskEntry getDeleteTask(String backupFile, String user, boolean schedulerManual) {
        String volumeId = getVolumeId(backupFile);

        TaskEntry taskEntry = new TaskEntry();

        taskEntry.setId(UUID.randomUUID().toString());
        taskEntry.setVolume(volumeId);
        taskEntry.setType(DELETE.getType());
        taskEntry.setInstanceId(instanceId);
        taskEntry.setStatus(TaskEntry.TaskEntryStatus.QUEUED.getStatus());
        taskEntry.setOptions(backupFile);
        taskEntry.setSchedulerName(user);
        taskEntry.setSchedulerTime(String.valueOf(DateTime.now().getMillis()));
        taskEntry.setPriority(1);

        //TODO Remove hardcode
        taskEntry.setWorker(taskEntry.getInstanceId());
        taskEntry.setSchedulerManual(schedulerManual);
        taskEntry.setRegular(false);

        return taskEntry;
    }
}
