package com.sungardas.enhancedsnapshots.service.dev;

import com.amazonaws.services.ec2.model.*;
import com.sungardas.enhancedsnapshots.service.impl.AWSCommunicationServiceImpl;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;



@Service
@Profile("dev")
public class AWSCommunicationServiceDev extends AWSCommunicationServiceImpl {

    @Override
    public void createTemporaryTag(final String resourceId, final String description) {

    }

    @Override
    public void deleteTemporaryTag(final String resourceId) {

    }

    @Override
    public Volume createVolume(final int size, final VolumeType type) {
        return getVolume();
    }

    @Override
    public Volume createIO1Volume(final int size, final int iopsPerGb) {
        return getVolume();
    }

    @Override
    public Snapshot createSnapshot(final Volume volume) {
        return getSnapshot();
    }

    @Override
    public void deleteSnapshot(final String snapshotId) {
    }

    @Override
    public void cleanupSnapshots(final String volumeId, final String snapshotIdToLeave) {
    }

    @Override
    public Snapshot waitForCompleteState(final Snapshot snapshot) {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return snapshot;
    }

    @Override
    public Snapshot syncSnapshot(final String snapshotId) {
        return new Snapshot();
    }

    @Override
    public Volume waitForVolumeState(final Volume volume, final VolumeState expectedState) {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return volume;
    }

    @Override
    public Instance getInstance(String instanceId) {
        Instance instance = new Instance();
        instance.setPlacement(new Placement());
        return instance;
    }

    @Override
    public void detachVolume(final Volume volume) {
    }

    @Override
    public Volume createVolumeFromSnapshot(final String snapshotId, final String availabilityZoneName, final VolumeType type, final int iopsPerGb) {
        return getVolume();
    }

    @Override
    public Volume syncVolume(final Volume volume) {
        return volume;
    }

    @Override
    public void deleteVolume(final Volume volume) {

    }

    @Override
    public synchronized void attachVolume(final Instance instance, final Volume volume) {

    }

    @Override
    public void setResourceName(final String resourceId, final String value) {

    }

    @Override
    public void addTag(final String resourceId, final String name, final String value) {

    }

    @Override
    public boolean snapshotExists(final String snapshotId) {
        return true;
    }

    @Override
    public Snapshot getSnapshot(final String snapshotId) {
        return getSnapshot();
    }

    @Override
    public void restartAWSLogService() {

    }

    @Override
    public String getCurrentAvailabilityZone() {
        return describeAvailabilityZonesForCurrentRegion().get(0).getZoneName();
    }

    @Override
    public void dropS3Bucket(String bucketName) {
        return;
    }

    @Override
    public Volume getVolume(String volumeId) {
        return getVolume();
    }

    private Snapshot getSnapshot() {
        return new Snapshot().withSnapshotId("DEV_SNAPSHOT_ID");
    }

    private Volume getVolume() {
        return new Volume().withSize(100).withVolumeType("gp2").withVolumeId("DEV_VOLUME_ID").withAvailabilityZone("");
    }

}
