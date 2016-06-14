package com.sungardas.enhancedsnapshots.service;


public interface SnapshotService {

    /**
     * Returns snapshot info by volumeId
     * @param volumeId
     * @return
     */
    String getSnapshotIdByVolumeId(String volumeId);

    /**
     * Saves snapshot info to DB
     * @param volumeId
     * @param snapshotId
     */
    void saveSnapshot(String volumeId, String snapshotId);

    /**
     * Removes snapshot from DB and from AWS in case it exists
     *
     * @param snapshotId
     */
    void deleteSnapshot(String snapshotId);

}
