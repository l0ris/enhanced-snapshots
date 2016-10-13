package com.sungardas.enhancedsnapshots.service.impl;

import java.util.List;

import com.sungardas.enhancedsnapshots.aws.dynamodb.model.SnapshotEntry;
import com.sungardas.enhancedsnapshots.aws.dynamodb.repository.SnapshotRepository;
import com.sungardas.enhancedsnapshots.service.AWSCommunicationService;
import com.sungardas.enhancedsnapshots.service.SnapshotService;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;


@Service
@Profile("prod")
public class SnapshotServiceImpl implements SnapshotService {
    private static final Logger LOG = LogManager.getLogger(SnapshotServiceImpl.class);

    @Autowired
    private SnapshotRepository snapshotRepository;
    @Autowired
    private AWSCommunicationService awsCommunicationService;

    @Override
    public String getSnapshotIdByVolumeId(String volumeId) {
        LOG.info("Get snapshot id for volume {}", volumeId);
        try {
            // currently we suppose that there is always only one snapshot for volume
            List<SnapshotEntry> snapshotEntryList = snapshotRepository.findByVolumeId(volumeId);
            if (snapshotEntryList.size() != 1) {
                LOG.warn("There is more than one snapshot for volume {}. First one will be returned.", volumeId);
            }
            return snapshotEntryList.get(0).getSnapshotId();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void saveSnapshot(String volumeId, String snapshotId) {
        snapshotRepository.save(new SnapshotEntry(snapshotId, volumeId));
    }

    @Override
    public void deleteSnapshot(String snapshotId) {
        if (snapshotId != null && snapshotRepository.findOne(snapshotId) != null) {
            if (awsCommunicationService.snapshotExists(snapshotId)) {
                awsCommunicationService.deleteSnapshot(snapshotId);
            }
            snapshotRepository.delete(snapshotId);
            LOG.info("Snapshot {} removed successfully", snapshotId);
        }
    }
}
