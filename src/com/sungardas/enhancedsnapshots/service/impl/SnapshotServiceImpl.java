package com.sungardas.enhancedsnapshots.service.impl;

import com.sungardas.enhancedsnapshots.aws.dynamodb.model.SnapshotEntry;
import com.sungardas.enhancedsnapshots.aws.dynamodb.repository.SnapshotRepository;
import com.sungardas.enhancedsnapshots.service.SnapshotService;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class SnapshotServiceImpl implements SnapshotService {
    private static final Logger LOG = LogManager.getLogger(SnapshotServiceImpl.class);

    @Autowired
    private SnapshotRepository snapshotRepository;

    @Override
    public String getSnapshotId(String volumeId) {
        LOG.info("Get snapshot id for volume {}", volumeId);
        try {
            return snapshotRepository.findOne(volumeId).getSnapshotId();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void saveSnapshot(String volumeId, String snapshotId) {
        snapshotRepository.save(new SnapshotEntry(snapshotId, volumeId));
    }
}
