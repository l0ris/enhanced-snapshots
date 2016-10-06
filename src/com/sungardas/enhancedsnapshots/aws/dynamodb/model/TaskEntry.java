package com.sungardas.enhancedsnapshots.aws.dynamodb.model;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBIgnore;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMarshalling;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.amazonaws.util.json.Jackson;
import com.sungardas.enhancedsnapshots.aws.dynamodb.Marshaller.TaskProgressMarshaller;
import com.sungardas.enhancedsnapshots.enumeration.TaskProgress;


@DynamoDBTable(tableName = "Tasks")
public class TaskEntry {

    @DynamoDBHashKey(attributeName = "id")
    private String id;

    @DynamoDBAttribute(attributeName = "priority")
    private int priority;

    @DynamoDBAttribute(attributeName = "worker")
    private String worker;

    @DynamoDBAttribute(attributeName = "status")
    private String status;

    @DynamoDBAttribute(attributeName = "type")
    private String type;

    @DynamoDBAttribute(attributeName = "volume")
    private String volume;

    @DynamoDBAttribute(attributeName = "schedulerManual")
    private String schedulerManual;

    @DynamoDBAttribute(attributeName = "schedulerName")
    private String schedulerName;

    @DynamoDBAttribute(attributeName = "schedulerTime")
    private String schedulerTime;

    @DynamoDBAttribute(attributeName = "options")
    private String options;

    @DynamoDBAttribute
    private String cron;

    @DynamoDBAttribute
    private String regular = Boolean.FALSE.toString();

    @DynamoDBAttribute
    private String enabled;

    //complete time in milliseconds
    @DynamoDBAttribute
    private long completeTime;

    @DynamoDBAttribute
    private long startTime;

    @DynamoDBAttribute
    private String tempVolumeType;

    @DynamoDBAttribute
    private String restoreVolumeType;

    @DynamoDBAttribute
    private int tempVolumeIopsPerGb;

    @DynamoDBAttribute
    private int restoreVolumeIopsPerGb;

    @DynamoDBAttribute
    @DynamoDBMarshalling(marshallerClass = TaskProgressMarshaller.class)
    private TaskProgress progress;

    @DynamoDBAttribute
    private String tempVolumeId;

    @DynamoDBAttribute
    private String tempSnapshotId;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getPriority() {
        return priority;
    }


    public void setPriority(int priority) {
        this.priority = priority;
    }

    public String getWorker() {
        return worker;
    }

    public void setWorker(String worker) {
        this.worker = worker;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getVolume() {
        return volume;
    }

    public void setVolume(String volume) {
        this.volume = volume;
    }

    public String getSchedulerManual() {
        return String.valueOf(schedulerManual);
    }

    public void setSchedulerManual(boolean schedulerManual) {
        this.schedulerManual = String.valueOf(schedulerManual);
    }

    public void setSchedulerManual(String schedulerManual) {
        this.schedulerManual = schedulerManual;
    }

    public String getSchedulerName() {
        return schedulerName;
    }

    public void setSchedulerName(String schedulerName) {
        this.schedulerName = schedulerName;
    }

    public String getSchedulerTime() {
        return schedulerTime;
    }

    public void setSchedulerTime(String schedulerTime) {
        this.schedulerTime = schedulerTime;
    }

    public String getOptions() {
        return options;
    }

    public void setOptions(String options) {
        this.options = options;
    }

    public String getCron() {
        return cron;
    }

    public void setCron(String cron) {
        this.cron = cron;
    }

    public String getRegular() {
        return regular;
    }

    public void setRegular(String regular) {
        this.regular = regular;
    }

    public void setRegular(boolean regular) {
        this.regular = String.valueOf(regular);
    }

    public String getEnabled() {
        return enabled;
    }

    public void setEnabled(String enabled) {
        this.enabled = enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = String.valueOf(enabled);
    }

    public long getCompleteTime() {
        return completeTime;
    }

    public void setCompleteTime(long completeTime) {
        this.completeTime = completeTime;
    }

    @DynamoDBIgnore
    public String getSourceFileName() {
        return options.split(", ")[0];
    }

    public int getTempVolumeIopsPerGb() {
        return tempVolumeIopsPerGb;
    }

    public void setTempVolumeIopsPerGb(int tempVolumeIopsPerGb) {
        this.tempVolumeIopsPerGb = tempVolumeIopsPerGb;
    }

    public String getTempVolumeType() {
        return tempVolumeType;
    }

    public void setTempVolumeType(String tempVolumeType) {
        this.tempVolumeType = tempVolumeType;
    }

    @DynamoDBIgnore
    public String getAvailabilityZone() {
        return options.split(", ")[1];
    }

    @Deprecated
    @Override
    public String toString() {
        return Jackson.toJsonString(this);
    }

    public String getRestoreVolumeType() {
        return restoreVolumeType;
    }


    public void setRestoreVolumeType(String restoreVolumeType) {
        this.restoreVolumeType = restoreVolumeType;
    }

    public int getRestoreVolumeIopsPerGb() {
        return restoreVolumeIopsPerGb;
    }

    public void setRestoreVolumeIopsPerGb(int restoreVolumeIopsPerGb) {
        this.restoreVolumeIopsPerGb = restoreVolumeIopsPerGb;
    }

    public TaskProgress getProgress() {
        return progress;
    }

    public void setProgress(final TaskProgress progress) {
        this.progress = progress;
    }

    public String getTempVolumeId() {
        return tempVolumeId;
    }


    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(final long startTime) {
        this.startTime = startTime;
    }

    public void setTempVolumeId(final String tempVolumeId) {
        this.tempVolumeId = tempVolumeId;
    }

    public String getTempSnapshotId() {
        return tempSnapshotId;
    }

    public void setTempSnapshotId(final String tempSnapshotId) {
        this.tempSnapshotId = tempSnapshotId;
    }

    public enum TaskEntryType {
        BACKUP("backup"),
        RESTORE("restore"),
        DELETE("delete"),
        SYSTEM_BACKUP("system_backup"),
        UNKNOWN("unknown");

        private String type;

        TaskEntryType(String type) {
            this.type = type;
        }

        public static TaskEntryType getType(String type) {
            try {
                return valueOf(type.toUpperCase());
            } catch (IllegalArgumentException e) {
                return UNKNOWN;
            }
        }

        public String getType() {
            return type;
        }

    }

    public enum TaskEntryStatus {
        WAITING("waiting"),
        RUNNING("running"),
        QUEUED("queued"),
        COMPLETE("complete"),
        CANCELED("canceled"),
        PARTIALLY_FINISHED("partially_finished"),
        ERROR("error");

        private String status;

        TaskEntryStatus(String status) {
            this.status = status;
        }

        @Override
        public String toString() {
            return status;
        }
        public String getStatus() {
            return status;
        }

    }
}
