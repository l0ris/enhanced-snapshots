package com.sungardas.enhancedsnapshots.aws.dynamodb.model;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;

@DynamoDBTable(tableName = "Nodes")
public class NodeEntry {

    @DynamoDBHashKey()
    private String nodeId;
    @DynamoDBAttribute
    private boolean master;
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
        this.master = isMaster;
        this.freeRestoreWorkers = freeRestoreWorkers;
        this.freeBackupWorkers = freeBackupWorkers;
        this.sdfsVolumeId = sdfsVolumeId;
    }

    public boolean isMaster() {
        return master;
    }

    public void setMaster(boolean master) {
        this.master = master;
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

    @Override
    public String toString() {
        return "NodeEntry{" +
                "nodeId='" + nodeId + '\'' +
                ", master=" + master +
                ", freeRestoreWorkers=" + freeRestoreWorkers +
                ", freeBackupWorkers=" + freeBackupWorkers +
                ", sdfsVolumeId=" + sdfsVolumeId +
                '}';
    }
}
