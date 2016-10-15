package com.sungardas.enhancedsnapshots.components;

import com.sungardas.enhancedsnapshots.aws.dynamodb.model.MailConfigurationDocument;

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

    int getTaskHistoryTTS();

    String getVolumeSizeUnit();

    boolean isSsoLoginMode();

    String getSamlEntityId();

    boolean isStoreSnapshot();

    int getLogsBufferSize();

    String getLogFileName();

    String getDomain();

    MailConfigurationDocument getMailConfiguration();

    String getUUID();

    boolean isSungardasSSO();
}
