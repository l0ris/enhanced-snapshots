package com.sungardas.enhancedsnapshots.service;

public interface SnapshotService {

    String getSnapshotId(String volumeId);

    void saveSnapshot(String volumeId, String snapshotId);
}
