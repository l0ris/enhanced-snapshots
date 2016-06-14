package com.sungardas.enhancedsnapshots.components;

/**
 * Mediator pattern used for simplifying immediate update system properties in all dependant services
 */
public interface ConfigurationMediator {
    String getRegion();

    String getS3Bucket();

    String getConfigurationId();

    int getAmazonRetryCount();

    int getAmazonRetrySleep();

    int getMaxQueueSize();

    String getRetentionCronExpression();

    int getWorkerDispatcherPollingRate();

    String getTempVolumeType();

    int getTempVolumeIopsPerGb();

    String getRestoreVolumeType();

    int getRestoreVolumeIopsPerGb();

    String getSdfsVolumeName();

    String getSdfsMountPoint();

    String getSdfsLocalCacheSize();

    String getSdfsVolumeSize();

    int getSdfsVolumeSizeWithoutMeasureUnit();

    int getSdfsLocalCacheSizeWithoutMeasureUnit();

    String getSdfsConfigPath();

    String getSdfsBackupFileName();

    int getWaitTimeBeforeNewSyncWithAWS();

    int getMaxWaitTimeToDetachVolume();

    String getVolumeSizeUnit();
}
