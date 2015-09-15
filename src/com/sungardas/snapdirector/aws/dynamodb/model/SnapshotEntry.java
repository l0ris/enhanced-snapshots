package com.sungardas.snapdirector.aws.dynamodb.model;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;

@DynamoDBTable(tableName = "Snapshots")
public class SnapshotEntry {

    @DynamoDBAttribute
    private String snapshotId;

    @DynamoDBHashKey
    private String volumeId;

    @DynamoDBAttribute
    private String instanceId;

    public SnapshotEntry() {
        this(null, null,null);
    }

    public SnapshotEntry(String snapshotId, String volumeId) {
        this(snapshotId, volumeId, null);
    }

    public SnapshotEntry(String snapshotId, String volumeId, String instanceId) {
        this.snapshotId = snapshotId;
        this.volumeId = volumeId;
        this.instanceId = instanceId;
    }

    public String getSnapshotId() {
        return snapshotId;
    }

    public void setSnapshotId(String snapshotId) {
        this.snapshotId = snapshotId;
    }


    public String getVolumeId() {
        return volumeId;
    }

    public void setVolumeId(String volumeId) {
        this.volumeId = volumeId;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }
}