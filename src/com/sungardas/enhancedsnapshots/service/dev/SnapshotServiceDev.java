package com.sungardas.enhancedsnapshots.service.dev;

import com.sungardas.enhancedsnapshots.service.SnapshotService;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("dev")
public class SnapshotServiceDev implements SnapshotService {
    @Override
    public String getSnapshotIdByVolumeId(final String volumeId) {
        return null;
    }

    @Override
    public void saveSnapshot(final String volumeId, final String snapshotId) {

    }

    @Override
    public void deleteSnapshot(final String snapshotId) {

    }
}
