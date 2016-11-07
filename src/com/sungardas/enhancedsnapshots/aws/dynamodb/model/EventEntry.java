package com.sungardas.enhancedsnapshots.aws.dynamodb.model;


import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMarshalling;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.sungardas.enhancedsnapshots.aws.dynamodb.Marshaller.ClusterEventMarshaller;
import com.sungardas.enhancedsnapshots.cluster.ClusterEvents;

@DynamoDBTable(tableName = "Events")
public class EventEntry {

    @DynamoDBHashKey
    private String id;

    @DynamoDBAttribute
    @DynamoDBMarshalling(marshallerClass = ClusterEventMarshaller.class)
    private ClusterEvents event;
    // for node added/removed events
    @DynamoDBAttribute
    private String instanceId;

    @DynamoDBAttribute
    private long volumeId;

    @DynamoDBAttribute
    private long time;

    public EventEntry() {
    }

    public EventEntry(String id, long time, ClusterEvents event, String instanceId, long volumeId) {
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

    public ClusterEvents getEvent() {
        return event;
    }

    public void setEvent(ClusterEvents event) {
        this.event = event;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public long getVolumeId() {
        return volumeId;
    }

    public void setVolumeId(long volumeId) {
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
