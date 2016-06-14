package com.sungardas.enhancedsnapshots.aws.dynamodb.model;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;


@DynamoDBTable(tableName = "Configurations")
public class Configuration {

    // enhancedsnapshot settings
    private String configurationId;
    private int amazonRetryCount;
    private int amazonRetrySleep;
    private int maxQueueSize;
    private String retentionCronExpression;
    private int workerDispatcherPollingRate;
    private String tempVolumeType;
    private int tempVolumeIopsPerGb;
    private String restoreVolumeType;
    private int restoreVolumeIopsPerGb;
    private int waitTimeBeforeNewSyncWithAWS;
    private int maxWaitTimeToDetachVolume;

    // sdfs settings
    private String sdfsVolumeName;
    private String sdfsMountPoint;
    private int sdfsLocalCacheSize;
    private int sdfsSize;
    private String sdfsConfigPath;
    private String sdfsBackupFileName;

    // amazon settings
    private String region;
    private String s3Bucket;

    // Nginx
    private String nginxCertPath;
    private String nginxKeyPath;

    public String getSdfsVolumeName() {
        return sdfsVolumeName;
    }

    public void setSdfsVolumeName(String sdfsVolumeName) {
        this.sdfsVolumeName = sdfsVolumeName;
    }


    public String getSdfsMountPoint() {
        return sdfsMountPoint;
    }

    public void setSdfsMountPoint(String sdfsMountPoint) {
        this.sdfsMountPoint = sdfsMountPoint;
    }

    @DynamoDBAttribute(attributeName = "region")
    public String getEc2Region() {
        return region;
    }

    public void setEc2Region(String ec2Region) {
        this.region = ec2Region;
    }

    @DynamoDBHashKey()
    public String getConfigurationId() {
        return configurationId;
    }

    public void setConfigurationId(String configurationId) {
        this.configurationId = configurationId;
    }

    public String getS3Bucket() {
        return s3Bucket;
    }

    public void setS3Bucket(String taskS3Bucket) {
        this.s3Bucket = taskS3Bucket;
    }

    public String getTempVolumeType() {
        return tempVolumeType;
    }

    public void setTempVolumeType(String tempVolumeType) {
        this.tempVolumeType = tempVolumeType;
    }

    public int getTempVolumeIopsPerGb() {
        return tempVolumeIopsPerGb;
    }

    public void setTempVolumeIopsPerGb(int tempVolumeIopsPerGb) {
        this.tempVolumeIopsPerGb = tempVolumeIopsPerGb;
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

    public int getSdfsLocalCacheSize() {
        return sdfsLocalCacheSize;
    }

    public void setSdfsLocalCacheSize(int sdfsLocalCacheSize) {
        this.sdfsLocalCacheSize = sdfsLocalCacheSize;
    }

    public int getSdfsSize() {
        return sdfsSize;
    }

    public void setSdfsSize(int sdfsSize) {
        this.sdfsSize = sdfsSize;
    }

    public int getAmazonRetryCount() {
        return amazonRetryCount;
    }

    public void setAmazonRetryCount(int amazonRetryCount) {
        this.amazonRetryCount = amazonRetryCount;
    }

    public int getAmazonRetrySleep() {
        return amazonRetrySleep;
    }

    public void setAmazonRetrySleep(int amazonRetrySleep) {
        this.amazonRetrySleep = amazonRetrySleep;
    }

    public int getMaxQueueSize() {
        return maxQueueSize;
    }

    public void setMaxQueueSize(int maxQueueSize) {
        this.maxQueueSize = maxQueueSize;
    }

    public String getSdfsConfigPath() {
        return sdfsConfigPath;
    }

    public void setSdfsConfigPath(String sdfsConfigPath) {
        this.sdfsConfigPath = sdfsConfigPath;
    }

    public String getSdfsBackupFileName() {
        return sdfsBackupFileName;
    }

    public void setSdfsBackupFileName(String sdfsBackupFileName) {
        this.sdfsBackupFileName = sdfsBackupFileName;
    }

    public String getRetentionCronExpression() {
        return retentionCronExpression;
    }

    public void setRetentionCronExpression(String retentionCronExpression) {
        this.retentionCronExpression = retentionCronExpression;
    }

    public int getWorkerDispatcherPollingRate() {
        return workerDispatcherPollingRate;
    }

    public void setWorkerDispatcherPollingRate(int workerDispatcherPollingRate) {
        this.workerDispatcherPollingRate = workerDispatcherPollingRate;
    }

    public int getWaitTimeBeforeNewSyncWithAWS() {
        return waitTimeBeforeNewSyncWithAWS;
    }

    public void setWaitTimeBeforeNewSyncWithAWS(int waitTimeBeforeNewSyncWithAWS) {
        this.waitTimeBeforeNewSyncWithAWS = waitTimeBeforeNewSyncWithAWS;
    }

    public int getMaxWaitTimeToDetachVolume() {
        return maxWaitTimeToDetachVolume;
    }

    public void setMaxWaitTimeToDetachVolume(int maxWaitTimeToDetachVolume) {
        this.maxWaitTimeToDetachVolume = maxWaitTimeToDetachVolume;
    }

    public String getNginxCertPath() {
        return nginxCertPath;
    }

    public void setNginxCertPath(final String nginxCertPath) {
        this.nginxCertPath = nginxCertPath;
    }

    public String getNginxKeyPath() {
        return nginxKeyPath;
    }

    public void setNginxKeyPath(final String nginxKeyPath) {
        this.nginxKeyPath = nginxKeyPath;
    }
}
