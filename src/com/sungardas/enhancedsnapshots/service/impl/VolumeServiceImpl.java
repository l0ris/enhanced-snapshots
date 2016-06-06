package com.sungardas.enhancedsnapshots.service.impl;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import javax.annotation.PostConstruct;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.AmazonEC2;
import com.sungardas.enhancedsnapshots.aws.dynamodb.model.BackupEntry;
import com.sungardas.enhancedsnapshots.aws.dynamodb.repository.BackupRepository;
import com.sungardas.enhancedsnapshots.components.ConfigurationMediator;
import com.sungardas.enhancedsnapshots.dto.VolumeDto;
import com.sungardas.enhancedsnapshots.dto.converter.VolumeDtoConverter;
import com.sungardas.enhancedsnapshots.exception.DataAccessException;
import com.sungardas.enhancedsnapshots.service.SchedulerService;
import com.sungardas.enhancedsnapshots.service.Task;
import com.sungardas.enhancedsnapshots.service.VolumeService;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class VolumeServiceImpl implements VolumeService {

    private static final Logger LOG = LogManager.getLogger(VolumeServiceImpl.class);
    private static final String REMOVED_VOLUME_STATE = "removed";
    private static final String EMPTY = "";
    private static final VolumeDtoComparator volumeDtoComparator = new VolumeDtoComparator();
    @Autowired
    private AmazonEC2 amazonEC2;
    @Autowired
    private BackupRepository backupRepository;
    @Autowired
    private SchedulerService schedulerService;
    @Autowired
    private ConfigurationMediator configurationMediator;
    private Set<VolumeDto> cache;

    @PostConstruct
    private void init() {
        schedulerService.addTask(new Task() {
            @Override
            public String getId() {
                return getClass().getName();
            }

            @Override
            public void run() {
                expireCache();
            }
        }, "* * * * *");
    }

    @Override
    public Set<VolumeDto> getVolumes() {
        LOG.debug("Getting volume list ...");
        amazonEC2.setRegion(Region.getRegion(Regions.fromName(configurationMediator.getRegion())));
        return getVolumes(amazonEC2);
    }

    private Set<VolumeDto> getVolumes(AmazonEC2 amazonEC2) {
        if (cache != null) {
            return cache;
        } else {
            try {
                Set<VolumeDto> result = new TreeSet<>(volumeDtoComparator);
                result.addAll(VolumeDtoConverter.convert(amazonEC2.describeVolumes().getVolumes()));
                result.addAll(getHistoryVolumes());
                result = setSchedules(result);
                LOG.debug("Volume list: [{}]", result);
                cache = result;
                return result;
            } catch (RuntimeException e) {
                LOG.error("Failed to get volume list.", e);
                throw new DataAccessException("Failed to get volume list.", e);
            }
        }
    }

    @Override
    public void expireCache() {
        cache = null;
    }

    @Override
    public Set<VolumeDto> getVolumesByRegion(Region region) {
        LOG.debug("Getting volume list for region [{}]", region);
        amazonEC2.setRegion(region);
        Set<VolumeDto> volumes = getVolumes(amazonEC2);
        return volumes;
    }

    @Override
    public boolean volumeExists(String volumeId) {
        for (VolumeDto dto : getVolumes()) {
            if (dto.getVolumeId().equals(volumeId)) {
                return true;
            }
        }
        return false;
    }

    private Set<VolumeDto> setSchedules(Set<VolumeDto> result) {
        Set<String> volumeWithSchedule = schedulerService.getVolumeIdsWithSchedule();
        for (VolumeDto volumeDto : result) {
            if (volumeWithSchedule.contains(volumeDto.getVolumeId())) {
                volumeDto.setScheduled(true);
            }
        }
        return result;
    }

    private Set<VolumeDto> getHistoryVolumes() {
        return convert(backupRepository.findAll());
    }

    private Set<VolumeDto> convert(Iterable<BackupEntry> entries) {
        Set<VolumeDto> dtos = new HashSet<>();

        for (BackupEntry entry : entries) {
            VolumeDto dto = new VolumeDto();

            dto.setVolumeId(entry.getVolumeId());
            dto.setSize(0);

            dto.setInstanceID(EMPTY);
            dto.setVolumeName(EMPTY);
            dto.setTags(Collections.EMPTY_LIST);
            dto.setAvailabilityZone(EMPTY);
            dto.setSnapshotId(EMPTY);
            dto.setState(REMOVED_VOLUME_STATE);

            dtos.add(dto);
        }

        return dtos;
    }

    private static final class VolumeDtoComparator implements Comparator<VolumeDto> {
        @Override
        public int compare(VolumeDto o1, VolumeDto o2) {
            return o1.getVolumeId().compareTo(o2.getVolumeId());
        }
    }


}
