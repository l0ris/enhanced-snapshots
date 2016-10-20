package com.sungardas.enhancedsnapshots.aws.dynamodb.model;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;

@DynamoDBTable(tableName = "Nodes")
public class NodeEntry {

    @DynamoDBHashKey()
    private String nodeId;
    @DynamoDBAttribute
    private boolean isMaster;
    @DynamoDBAttribute
    private int freeRestoreWorkers;
    @DynamoDBAttribute
    private int freeBackupWorkers;
    @DynamoDBAttribute
    private long sdfsVolumeId;

    public NodeEntry() {
    }

    public NodeEntry(String nodeId, boolean isMaster, int freeRestoreWorkers, int freeBackupWorkers, long sdfsVolumeId) {
        this.nodeId = nodeId;
        this.isMaster = isMaster;
        this.freeRestoreWorkers = freeRestoreWorkers;
        this.freeBackupWorkers = freeBackupWorkers;
        this.sdfsVolumeId = sdfsVolumeId;
    }

    public boolean isMaster() {
        return isMaster;
    }

    public void setMaster(boolean master) {
        isMaster = master;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public int getFreeRestoreWorkers() {
        return freeRestoreWorkers;
    }

    public void setFreeRestoreWorkers(int freeRestoreWorkers) {
        this.freeRestoreWorkers = freeRestoreWorkers;
    }

    public int getFreeBackupWorkers() {
        return freeBackupWorkers;
    }

    public void setFreeBackupWorkers(int freeBackupWorkers) {
        this.freeBackupWorkers = freeBackupWorkers;
    }

    public long getSdfsVolumeId() {
        return sdfsVolumeId;
    }

    public void setSdfsVolumeId(long sdfsVolumeId) {
        this.sdfsVolumeId = sdfsVolumeId;
    }
}
