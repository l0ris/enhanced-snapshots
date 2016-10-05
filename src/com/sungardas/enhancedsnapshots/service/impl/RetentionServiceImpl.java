package com.sungardas.enhancedsnapshots.service.impl;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.annotation.PostConstruct;

import com.amazonaws.AmazonClientException;
import com.sungardas.enhancedsnapshots.aws.dynamodb.model.BackupEntry;
import com.sungardas.enhancedsnapshots.aws.dynamodb.model.RetentionEntry;
import com.sungardas.enhancedsnapshots.aws.dynamodb.repository.BackupRepository;
import com.sungardas.enhancedsnapshots.aws.dynamodb.repository.RetentionRepository;
import com.sungardas.enhancedsnapshots.components.ConfigurationMediator;
import com.sungardas.enhancedsnapshots.dto.RetentionDto;
import com.sungardas.enhancedsnapshots.exception.DataAccessException;
import com.sungardas.enhancedsnapshots.service.BackupService;
import com.sungardas.enhancedsnapshots.service.RetentionService;
import com.sungardas.enhancedsnapshots.service.SchedulerService;
import com.sungardas.enhancedsnapshots.service.Task;
import com.sungardas.enhancedsnapshots.service.VolumeService;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static com.sungardas.enhancedsnapshots.dto.converter.RetentionConverter.toDto;
import static com.sungardas.enhancedsnapshots.dto.converter.RetentionConverter.toEntry;

@Service
public class RetentionServiceImpl implements RetentionService {

    public static final String RETENTION_USER = "RETENTION POLICY";

    private static final long BYTES_IN_GB = 1073741824;

    private static final Logger LOG = LogManager.getLogger(RetentionServiceImpl.class);

    @Autowired
    private RetentionRepository retentionRepository;

    @Autowired
    private BackupRepository backupRepository;

    @Autowired
    private BackupService backupService;

    @Autowired
    private VolumeService volumeService;

    @Autowired
    private SchedulerService schedulerService;

    @Autowired
    private ConfigurationMediator configurationMediator;

    @PostConstruct
    private void init() {
        schedulerService.addTask(getJob(this), configurationMediator.getRetentionCronExpression());
        try {
            apply();
        } catch (AmazonClientException e) {
            LOG.error(e);
        }
    }

    @Override
    public void putRetention(RetentionDto retentionDto) {
        if (volumeService.volumeExists(retentionDto.getVolumeId())) {
            retentionRepository.save(toEntry(retentionDto));
            apply();
        } else {
            LOG.error("Volume with id: {} not found", retentionDto.getVolumeId());
            throw new DataAccessException("Volume with id:" + retentionDto.getVolumeId() + " not found");
        }
    }

    @Override
    public RetentionDto getRetentionDto(String volumeId) {
        try {
            return toDto(retentionRepository.findOne(volumeId));
        } catch (Exception e) {
            if (volumeService.volumeExists(volumeId)) {
                return new RetentionDto(volumeId);
            } else {
                LOG.error("Volume with id: {} not found", volumeId);
                throw new DataAccessException("Volume with id:" + volumeId + " not found");
            }
        }
    }

    @Override
    public void apply() {
        LOG.debug("Retention started");
        Map<String, Set<BackupEntry>> backups = getBackups();
        Map<String, RetentionEntry> retentions = getRetentions();
        Set<BackupEntry> backupsToRemove = new LinkedHashSet<>();

        for (Map.Entry<String, Set<BackupEntry>> entry : backups.entrySet()) {
            RetentionEntry retentionEntry = retentions.get(entry.getKey());
            if (retentionEntry != null) {
                if (isEmpty(retentionEntry)) {
                    retentionRepository.delete(retentionEntry.getVolumeId());
                } else {
                    BackupEntry[] values = entry.getValue().toArray(new BackupEntry[entry.getValue().size()]);
                    applySizeRetention(backupsToRemove, values, retentionEntry);
                    applyCountRetention(backupsToRemove, values, retentionEntry);
                    applyDayRetention(backupsToRemove, values, retentionEntry);
                }
            }
        }

        for (Map.Entry<String, RetentionEntry> entry : retentions.entrySet()) {
            if (!backups.containsKey(entry.getKey())) {
                retentionRepository.delete(entry.getValue().getVolumeId());
            }
        }
        if (!backupsToRemove.isEmpty()) {
            LOG.debug("Found backup to remove: {}", backupsToRemove);
            backupService.deleteBackup(backupsToRemove, RETENTION_USER);
            LOG.debug("Backups successfully removed");
        }
        LOG.debug("Finished");
    }

    private boolean isEmpty(RetentionEntry retentionEntry) {
        return retentionEntry.getCount() < 1 && retentionEntry.getDays() < 1 && retentionEntry.getSize() < 1;
    }

    private void applySizeRetention(Set<BackupEntry> backupsToRemove, BackupEntry[] backups, RetentionEntry retention) {
        if (retention.getSize() > 0 && backups.length > 0) {
            long retentionSize = retention.getSize() * BYTES_IN_GB;
            long size = 0;
            int i;
            for (i = 0; i < backups.length && size <= retentionSize; i++) {
                size += parseLong(backups[i].getSize());
            }
            if (size > retentionSize) {
                i--;
                for (; i < backups.length; i++) {
                    backupsToRemove.add(backups[i]);
                }
            }
        }
    }

    private void applyCountRetention(Set<BackupEntry> backupsToRemove, BackupEntry[] backups, RetentionEntry retention) {
        if (retention.getCount() > 0) {
            for (int i = retention.getCount(); i < backups.length; i++) {
                backupsToRemove.add(backups[i]);
            }
        }
    }

    private void applyDayRetention(Set<BackupEntry> backupsToRemove, BackupEntry[] backups, RetentionEntry retention) {
        if (retention.getDays() > 0) {
            DateTime currentDateTime = new DateTime();
            for (int i = 0; i < backups.length; i++) {
                DateTime creationDate = new DateTime(parseLong(backups[i].getTimeCreated()));
                DateTime expireTime = creationDate.plusDays(retention.getDays());
                if (currentDateTime.isAfter(expireTime)) {
                    backupsToRemove.add(backups[i]);
                }
            }
        }
    }

    private Map<String, Set<BackupEntry>> getBackups() {
        Map<String, Set<BackupEntry>> backupsSortedByCreationTime = new HashMap<>();

        for (BackupEntry backupEntry : backupRepository.findAll()) {
            Set<BackupEntry> backups = backupsSortedByCreationTime.get(backupEntry.getVolumeId());
            if (backups == null) {
                backups = new TreeSet<>(backupComparatorByCreationTime);
                backupsSortedByCreationTime.put(backupEntry.getVolumeId(), backups);
            }
            backups.add(backupEntry);
        }

        return backupsSortedByCreationTime;
    }

    private Map<String, RetentionEntry> getRetentions() {
        Map<String, RetentionEntry> retentionEntryMap = new HashMap<>();

        for (RetentionEntry entry : retentionRepository.findAll()) {
            retentionEntryMap.put(entry.getVolumeId(), entry);
        }

        return retentionEntryMap;
    }

    private long parseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            LOG.debug(e);
            return 0;
        }
    }

    private static final Comparator<BackupEntry> backupComparatorByCreationTime = new Comparator<BackupEntry>() {
        @Override
        public int compare(BackupEntry o1, BackupEntry o2) {
            return o2.getTimeCreated().compareTo(o1.getTimeCreated());
        }
    };

    public Task getJob(final RetentionService retentionService) {
        return new Task() {
            @Override
            public void run() {
                retentionService.apply();
            }

            @Override
            public String getId() {
                return retentionService.getClass().toString();
            }
        };
    }
}
