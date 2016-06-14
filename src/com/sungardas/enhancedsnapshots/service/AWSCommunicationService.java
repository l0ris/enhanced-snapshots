package com.sungardas.enhancedsnapshots.service;

import java.util.List;

import com.amazonaws.services.ec2.model.AvailabilityZone;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Snapshot;
import com.amazonaws.services.ec2.model.Volume;
import com.amazonaws.services.ec2.model.VolumeType;

public interface AWSCommunicationService {

    Snapshot createSnapshot(Volume volume);

    void deleteSnapshot(String snapshotId);

    void cleanupSnapshots(String volumeId, String snapshotIdToLeave);

    Snapshot waitForCompleteState(Snapshot snapshot);

    Snapshot syncSnapshot(String snapshotId);

    Volume waitForAvailableState(Volume volume);

    Volume syncVolume(Volume volume);

    /**
     * iopsPerGb paramenter is only required for io1 volume type, for other volume types it will be skipped
     */
    Volume createVolumeFromSnapshot(String snapshotId, String availabilityZoneName, VolumeType type, int iopsPerGb);

    void deleteVolume(Volume volume);

    void attachVolume(Instance instance, Volume volume);

	Volume getVolume(String volumeId);

    List<AvailabilityZone> describeAvailabilityZonesForCurrentRegion();

    String getCurrentAvailabilityZone();

    void createTemporaryTag(String resourceId, String description);

    void deleteTemporaryTag(String resourceId);

    Volume createVolume(int size, VolumeType type);

    Volume createIO1Volume(int size, int iopsPerGb);

    Instance getInstance(String instanceId);

    void detachVolume(Volume volume);

    void setResourceName(String resourceid, String value);

    void addTag(String resourceId, String name, String value);

    boolean snapshotExists(String snapshotId);

    boolean volumeExists(String volumeId);

    Snapshot getSnapshot(String snapshotId);

    void dropS3Bucket(String bucketName);

    //TODO: awslog restarted after each sdfs remount
    //TODO: need to clarify whether we still need restart it
    //TODO: in case we need, it should be triggered by SystemService while restarting sdfs, not by SDFSStateService
    void restartAWSLogService();
}