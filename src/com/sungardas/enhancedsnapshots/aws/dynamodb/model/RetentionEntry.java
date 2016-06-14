package com.sungardas.enhancedsnapshots.aws.dynamodb.model;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;

@DynamoDBTable(tableName = "Retention")
public class RetentionEntry {

    @DynamoDBHashKey
    private String volumeId;

    @DynamoDBAttribute
    private int size;

    @DynamoDBAttribute
    private int count;

    @DynamoDBAttribute
    private int days;

    public RetentionEntry() {
    }

    @Deprecated
    public RetentionEntry(String volumeId, int size, int count, int days) {
        this.volumeId = volumeId;
        this.size = size;
        this.count = count;
        this.days = days;
    }

    public String getVolumeId() {
        return volumeId;
    }

    public void setVolumeId(String volumeId) {
        this.volumeId = volumeId;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public int getDays() {
        return days;
    }

    public void setDays(int days) {
        this.days = days;
    }
}
