package com.sungardas.enhancedsnapshots.service.impl;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.*;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.sungardas.enhancedsnapshots.components.ConfigurationMediator;
import com.sungardas.enhancedsnapshots.exception.EnhancedSnapshotsException;
import com.sungardas.enhancedsnapshots.service.AWSCommunicationService;
import com.sungardas.enhancedsnapshots.util.SystemUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.lang.String.format;

@Service
@Profile("prod")
public class AWSCommunicationServiceImpl implements AWSCommunicationService {

    private static final Logger LOG = LogManager.getLogger(AWSCommunicationServiceImpl.class);
    private static final String AVAILABLE_STATE = VolumeState.Available.toString();
    private final static int MIN_SIZE_OF_OI1_VOLUME = 4;
    private final static int MIN_IOPS_VALUE = 100;
    private final static int MAX_IOPS_VALUE = 20_000;

    @Autowired
    private AmazonS3 amazonS3;

    @Autowired
    private AmazonEC2 ec2client;

    @Autowired
    private ConfigurationMediator configurationMediator;

   @Override
   public List<AvailabilityZone> describeAvailabilityZonesForCurrentRegion() {
       return ec2client.describeAvailabilityZones().getAvailabilityZones();
   }

    @Override
    public String getCurrentAvailabilityZone() {
        return getInstance(SystemUtils.getInstanceId()).getPlacement()
                .getAvailabilityZone();    }

    @Override
    public void createTemporaryTag(String resourceId, String description) {
        CreateTagsRequest tagsRequest = new CreateTagsRequest().withResources(resourceId).withTags(
                new Tag().withKey("ESTempVolume").withValue(description));
        ec2client.createTags(tagsRequest);
    }

    @Override
    public void deleteTemporaryTag(String resourceId) {
        List<Volume> volumes= ec2client.describeVolumes(new DescribeVolumesRequest().withVolumeIds(resourceId)).getVolumes();
        if(volumes.size()>0) {
            Volume volume = volumes.get(0);
            List<Tag> tags = volume.getTags();
            boolean tagWasDeleted = false;
            for(Tag tag: tags) {
                if (tag.getKey().equals("ESTempVolume")) {
                    DeleteTagsRequest tagsRequest = new DeleteTagsRequest().withResources(resourceId).withTags();
                    ec2client.deleteTags(tagsRequest);
                    tagWasDeleted = true;
                    break;
                }
            }
            if(! tagWasDeleted) {
                LOG.info("No temporary tag associated with volume {}",resourceId);
            }
        } else {
            LOG.info("Volume with id {} does not exist ",resourceId);
        }

    }


    private Volume createVolume(int size, int iiops, VolumeType type) {
        String availabilityZone = getInstance(SystemUtils.getInstanceId()).getPlacement()
                .getAvailabilityZone();

        CreateVolumeRequest createVolumeRequest = new CreateVolumeRequest()
                .withSize(size).withVolumeType(type)
                .withAvailabilityZone(availabilityZone);
        if (iiops > 0) {
            createVolumeRequest = createVolumeRequest.withIops(iiops);
        }
        Volume result = ec2client.createVolume(createVolumeRequest).getVolume();
        return result;
    }

    @Override
    public Volume createVolume(int size, VolumeType type) {
        return createVolume(size, 0, type);
    }

    @Override
    public Volume createIO1Volume(int size, int iopsPerGb) {
        // io1 volume size can not be less than 4 Gb
        size = size < MIN_SIZE_OF_OI1_VOLUME ? MIN_SIZE_OF_OI1_VOLUME : size;
        return createVolume(size < MIN_SIZE_OF_OI1_VOLUME ? MIN_SIZE_OF_OI1_VOLUME : size, getIops(iopsPerGb, size), VolumeType.Io1);
    }

    @Override
    public Snapshot createSnapshot(Volume volume) {
        SimpleDateFormat formatter = new SimpleDateFormat(
                "yyyy.MM.dd'_T'hh:mm:ss");

        String volumeId = volume.getVolumeId();
        LOG.info(format("Starting creating snapshot for %s", volumeId));
        CreateSnapshotRequest snapshotRequest = new CreateSnapshotRequest(
                volumeId,
                volumeId
                        + "__"
                        + formatter.format(new Date(System.currentTimeMillis())));
        CreateSnapshotResult crSnapshotResult = ec2client
                .createSnapshot(snapshotRequest);
        Snapshot snapshot = crSnapshotResult.getSnapshot();
        return snapshot;
    }

    @Override
    public void deleteSnapshot(String snapshotId) {
        LOG.info(format("Deleting snapshot: %s", snapshotId));
        DeleteSnapshotRequest deleteSnapshotRequest = new DeleteSnapshotRequest();
        deleteSnapshotRequest.setSnapshotId(snapshotId);
        try {
            ec2client.deleteSnapshot(deleteSnapshotRequest);
        } catch (Throwable e) {
            LOG.info("Snapshot with id {} does not exist ", snapshotId);
        }
    }

    @Override
    public void cleanupSnapshots(String volumeId, String snapshotIdToLeave) {
        deleteSnapshot(snapshotIdToLeave);
    }

    @Override
    public Snapshot waitForCompleteState(Snapshot snapshot) {
        Snapshot syncSnapshot;
        do {
            sleepTillNextSync();
            syncSnapshot = syncSnapshot(snapshot.getSnapshotId());
            LOG.debug("Snapshot state: {}, progress: {}", syncSnapshot.getState(), syncSnapshot.getProgress());
            if (syncSnapshot.getState().equals(SnapshotState.Error)) {
                new AWSCommunicationServiceException("Snapshot " + snapshot.getSnapshotId() + " is in error state");
            }
        } while (syncSnapshot.getState().equals(SnapshotState.Pending) || !syncSnapshot.getProgress().equals("100%"));
        return syncSnapshot;
    }

    @Override
    public Snapshot syncSnapshot(String snapshotId) {
        Snapshot syncSnapshot = getSnapshot(snapshotId);
        if (syncSnapshot != null) {
            return syncSnapshot;
        }
        LOG.error("Failed to sync snapshot {}. Snapshot does not exist.", snapshotId);
        throw new AWSCommunicationServiceException("Can not sync snapshot. Snapshot " + snapshotId + " does not exist.");
    }

    @Override
    public Volume waitForVolumeState(Volume volume, VolumeState expectedState) {
        String state;
        Volume result;
        do {
            sleepTillNextSync();
            result = syncVolume(volume);
            state = result.getState();
            LOG.debug("waitForAvailableState.current state: " + state);
            if (state.equals(VolumeState.Error.toString())) {
                throw new EnhancedSnapshotsException("Volume state error, volumeID: " + volume.getVolumeId());
            }
        } while (!state.equals(expectedState.toString())
                && !state.equals(VolumeState.Deleted.toString()));
        return result;
    }

    @Override
    public Volume getVolume(String volumeId) {
        DescribeVolumesRequest describeVolumesRequest = new DescribeVolumesRequest().withVolumeIds(volumeId);
        DescribeVolumesResult result = ec2client.describeVolumes(describeVolumesRequest);
        if (result.getVolumes().size() == 1) {
            return result.getVolumes().get(0);
        }
        return null;
    }

    @Override
    public Instance getInstance(String instanceId) {
        DescribeInstancesRequest describeInstancesRequest = new DescribeInstancesRequest()
                .withInstanceIds(instanceId);
        DescribeInstancesResult describeInstancesResult = ec2client
                .describeInstances(describeInstancesRequest);
        List<Reservation> reservations = describeInstancesResult
                .getReservations();
        for (Reservation res : reservations) {
            if (res.getInstances().size() > 0) {
                return res.getInstances().get(0);
            }
        }
        return null;
    }

    @Override
    public void detachVolume(Volume volume) {
        DetachVolumeRequest detachVolumeRequest = new DetachVolumeRequest(volume.getVolumeId());
        ec2client.detachVolume(detachVolumeRequest);
        waitVolumeToDetach(volume);
    }

    public Volume createVolumeFromSnapshot(String snapshotId, String availabilityZoneName, VolumeType type, int iopsPerGb) {
        CreateVolumeRequest crVolumeRequest = new CreateVolumeRequest(snapshotId, availabilityZoneName);
        crVolumeRequest.setVolumeType(type);

        if (type.equals(VolumeType.Io1)) {
            Snapshot snapshot = getSnapshot(snapshotId);
            // io1 volume size can not be less than 4 Gb
            int size = snapshot.getVolumeSize() < MIN_SIZE_OF_OI1_VOLUME ? MIN_SIZE_OF_OI1_VOLUME : snapshot.getVolumeSize();
            crVolumeRequest.setSize(size);
            // setting iops
            if (iopsPerGb != 0) {
                crVolumeRequest.setIops(getIops(iopsPerGb,  size));
            }
        }
        return ec2client.createVolume(crVolumeRequest).getVolume();
    }

    @Override
    public Volume syncVolume(Volume volume) {
        Volume syncVolume = getVolume(volume.getVolumeId());
        if (syncVolume != null) {
            return syncVolume;
        }
        LOG.error("Failed to sync volume {}. Volume does not exist.", volume.getVolumeId());
        throw new AWSCommunicationServiceException("Can not sync volume. Volume " + volume.getVolumeId() + " does not exist.");
    }

    @Override
    public void deleteVolume(Volume volume) {
        DeleteVolumeRequest deleteVolumeRequest = new DeleteVolumeRequest(volume.getVolumeId());
        ec2client.deleteVolume(deleteVolumeRequest);
        LOG.info(format("Volume %s deleted", volume.getVolumeId()));
    }

    @Override
    public synchronized void attachVolume(Instance instance, Volume volume) {
        String deviceName = getNextAvailableDeviceName(instance);
        AttachVolumeRequest attachVolumeRequest = new AttachVolumeRequest(volume.getVolumeId(),
                instance.getInstanceId(), deviceName);
        ec2client.attachVolume(attachVolumeRequest);
        waitForVolumeState(volume, VolumeState.InUse);

        LOG.info(format("\nVolume attached. check instance data\n %s", instance.toString()));
    }

    private String getNextAvailableDeviceName(Instance instance) {
        String deviceName = "/dev/sd";
        List<String> reservedDeviceNames = new ArrayList<>(11);
        for (char c = 'f'; c <= 'p'; c++) {
            reservedDeviceNames.add(deviceName + c);
        }

        List<InstanceBlockDeviceMapping> devList = instance
                .getBlockDeviceMappings();
        Set<String> devicesInUsage = devList.stream().map(d -> d.getDeviceName()).collect(Collectors.toSet());

        String newDeviceName = reservedDeviceNames.stream().filter(r -> !devicesInUsage.contains(r) && !Files.exists(Paths.get(r))).findFirst().orElseThrow(() -> new EnhancedSnapshotsException("There are no available mount point, please detach some volume"));
        return newDeviceName;
    }

    @Override
    public void setResourceName(String resourceId, String value) {
        addTag(resourceId, "Name", value);
    }

    @Override
    public void addTag(String resourceId, String name, String value) {
        CreateTagsRequest r = new CreateTagsRequest().withResources(resourceId)
                .withTags(new Tag().withKey(name).withValue(value));
        ec2client.createTags(r);
    }

    @Override
    public boolean snapshotExists(String snapshotId) {
        Snapshot snapshot = getSnapshot(snapshotId);
        if (snapshot != null) {
            return true;
        }
        return false;
    }

    public Snapshot getSnapshot(String snapshotId) {
        DescribeSnapshotsResult describeSnapshotsResult = ec2client.describeSnapshots();
        List<Snapshot> snapshots = describeSnapshotsResult.getSnapshots();
        for(Snapshot snapshot: snapshots){
            if(snapshot.getSnapshotId().equals(snapshotId)){
                return snapshot;
            }
        }
        return null;
    }

    @Override
    public void dropS3Bucket(String bucketName) {
        LOG.info("Removing bucket {}.", bucketName);
        ObjectListing objectListing = amazonS3.listObjects(bucketName);

        while (true) {
            for (Iterator<?> iterator = objectListing.getObjectSummaries().iterator(); iterator.hasNext(); ) {
                S3ObjectSummary objectSummary = (S3ObjectSummary) iterator.next();
                amazonS3.deleteObject(bucketName, objectSummary.getKey());
            }

            if (objectListing.isTruncated()) {
                objectListing = amazonS3.listNextBatchOfObjects(objectListing);
            } else {
                break;
            }
        }
        VersionListing list = amazonS3.listVersions(new ListVersionsRequest().withBucketName(bucketName));
        for (Iterator<?> iterator = list.getVersionSummaries().iterator(); iterator.hasNext(); ) {
            S3VersionSummary s = (S3VersionSummary) iterator.next();
            amazonS3.deleteVersion(bucketName, s.getKey(), s.getVersionId());
        }
        amazonS3.deleteBucket(bucketName);
    }

    @Override
    public void restartAWSLogService() {
        Process p;
        try {
            p = Runtime.getRuntime().exec("service awslogs restart");
            p.waitFor();
            switch (p.exitValue()) {
                case 0:
                    LOG.info("awslogs restarted successfully");
                    break;
                default:
                    throw new AWSCommunicationServiceException("Failed to restart awslogs");
            }
        } catch (Exception e) {
            LOG.error("Failed to restart awslogs");
            throw new AWSCommunicationServiceException("Failed to restart awslogs");
        }

    }

    @Override
    public String getDNSName(String instanceId) {
        return getInstance(instanceId).getPrivateDnsName();
    }


    @Override
    public boolean volumeExists(String volumeId) {
        if (getVolume(volumeId) != null) {
            return true;
        }
        return false;
    }

    private void waitVolumeToDetach(Volume volume) {
        int waitTime = 0;
        LOG.debug("Volume {} is attached to {}", volume.getVolumeId(), volume.getAttachments().get(0).getInstanceId());
        LOG.debug("Max wait time for volume detaching: {} seconds", configurationMediator.getMaxWaitTimeToDetachVolume());
        while (!volume.getState().equals(AVAILABLE_STATE) && waitTime < configurationMediator.getMaxWaitTimeToDetachVolume()) {
            sleepTillNextSync();
            volume = syncVolume(volume);
            waitTime += configurationMediator.getWaitTimeBeforeNewSyncWithAWS();
        }
        if (syncVolume(volume).getState().equals(AVAILABLE_STATE)) {
            LOG.debug("Volume {} detached.", volume.getVolumeId());
            return;
        }
        LOG.error("Failed to detach volume {}.", volume.getVolumeId());
        throw new AWSCommunicationServiceException("Failed to detach volume " + volume.getVolumeId());
    }

    public static class AWSCommunicationServiceException extends EnhancedSnapshotsException {
        public AWSCommunicationServiceException(String message) {
            super(message);
        }
    }

    // iops can not be less than 100 and more than 20 000
    private int getIops(int iopsPerGb, int volumeSize) {
        int iops = volumeSize * iopsPerGb;
        if (iops < MIN_IOPS_VALUE) {
            return MIN_IOPS_VALUE;
        }
        if (iops > MAX_IOPS_VALUE) {
            return MAX_IOPS_VALUE;
        }
        return iops;
    }

    private void sleepTillNextSync(){
        try {
            TimeUnit.SECONDS.sleep(configurationMediator.getWaitTimeBeforeNewSyncWithAWS());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

