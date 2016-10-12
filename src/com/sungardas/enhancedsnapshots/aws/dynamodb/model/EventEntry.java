package com.sungardas.enhancedsnapshots.aws.dynamodb.model;



import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;

@DynamoDBTable(tableName = "Events")
public class EventEntry {


    @DynamoDBHashKey()
    private String id;
    @DynamoDBAttribute
    private String event;

    // for node added/removed events
    @DynamoDBAttribute
    private String instanceId;
    @DynamoDBAttribute
    private String volumeId;

    public EventEntry(String id, String event, String instanceId, String volumeId) {
        this.id = id;
        this.event = event;
        this.instanceId = instanceId;
        this.volumeId = volumeId;
    }
}
