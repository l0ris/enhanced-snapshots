package com.sungardas.enhancedsnapshots.aws.dynamodb.model;


import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;

@DynamoDBTable(tableName = "Events")
public class EventEntry {

    @DynamoDBHashKey
    private String id;

    @DynamoDBAttribute
    private String event;
    // for node added/removed events
    @DynamoDBAttribute
    private String instanceId;

    @DynamoDBAttribute
    private String volumeId;

    @DynamoDBAttribute
    private long time;

    public EventEntry() {
    }

    public EventEntry(String id, long time, String event, String instanceId, String volumeId) {
        this.id = id;
        this.time = time;
        this.event = event;
        this.instanceId = instanceId;
        this.volumeId = volumeId;
    }

    public void setId(String id) {
        this.id = id;
    }
    public String getId() {
        return id;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long id) {
        this.time = id;
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getVolumeId() {
        return volumeId;
    }

    public void setVolumeId(String volumeId) {
        this.volumeId = volumeId;
    }

    @Override
    public String toString() {
        return "EventEntry{" +
                "id='" + id + '\'' +
                ", event='" + event + '\'' +
                ", instanceId='" + instanceId + '\'' +
                ", volumeId='" + volumeId + '\'' +
                ", time=" + time +
                '}';
    }
}
