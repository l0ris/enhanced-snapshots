package com.sungardas.enhancedsnapshots.components.impl;

import com.amazonaws.services.dynamodbv2.datamodeling.IDynamoDBMapper;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.sungardas.enhancedsnapshots.aws.dynamodb.model.Configuration;
import com.sungardas.enhancedsnapshots.aws.dynamodb.model.MailConfigurationDocument;
import com.sungardas.enhancedsnapshots.components.ConfigurationMediatorConfigurator;
import com.sungardas.enhancedsnapshots.util.SystemUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

import static com.sungardas.enhancedsnapshots.service.SystemService.VOLUME_SIZE_UNIT;



/**
 * implementation for {@link ConfigurationMediatorConfigurator}
 */
@Service("ConfigurationMediator")
public class ConfigurationMediatorImpl implements ConfigurationMediatorConfigurator {

    private static final Logger LOG = LogManager.getLogger(ConfigurationMediatorImpl.class);
    @Autowired
    @Qualifier("dbMapperWithoutProxy")
    private IDynamoDBMapper dbMapper;
    private Configuration currentConfiguration;


    @PostConstruct
    private void init() throws Throwable {
        for (int i = 0; i < 3; i++) {
            try {
                currentConfiguration = dbMapper.load(Configuration.class, SystemUtils.getSystemId());
                return;
            } catch (ResourceNotFoundException e) {
                currentConfiguration = new Configuration();
                currentConfiguration.setAmazonRetryCount(30);
                currentConfiguration.setAmazonRetrySleep(15000);
            } catch (Exception e) {
                LOG.warn("Failed to get configuration: ", e);
                Thread.sleep(1000);
            }
        }
    }

    @Override
    public String getRegion() {
        return currentConfiguration.getEc2Region();
    }

    @Override
    public String getS3Bucket() {
        return currentConfiguration.getS3Bucket();
    }

    @Override
    public String getConfigurationId() {
        return currentConfiguration.getConfigurationId();
    }

    @Override
    public int getAmazonRetryCount() {
        return currentConfiguration.getAmazonRetryCount();
    }

    @Override
    public int getAmazonRetrySleep() {
        return currentConfiguration.getAmazonRetrySleep();
    }

    @Override
    public int getMaxQueueSize() {
        return currentConfiguration.getMaxQueueSize();
    }

    @Override
    public String getRetentionCronExpression() {
        return currentConfiguration.getRetentionCronExpression();
    }

    @Override
    public int getWorkerDispatcherPollingRate() {
        return currentConfiguration.getWorkerDispatcherPollingRate();
    }

    @Override
    public String getTempVolumeType() {
        return currentConfiguration.getTempVolumeType();
    }

    @Override
    public int getTempVolumeIopsPerGb() {
        return currentConfiguration.getTempVolumeIopsPerGb();
    }

    @Override
    public String getRestoreVolumeType() {
        return currentConfiguration.getRestoreVolumeType();
    }

    @Override
    public int getRestoreVolumeIopsPerGb() {
        return currentConfiguration.getRestoreVolumeIopsPerGb();
    }

    @Override
    public String getSdfsVolumeName() {
        return currentConfiguration.getSdfsVolumeName();
    }

    @Override
    public String getSdfsMountPoint() {
        return currentConfiguration.getSdfsMountPoint();
    }

    @Override
    public String getSdfsLocalCacheSize() {
        return currentConfiguration.getSdfsLocalCacheSize() + VOLUME_SIZE_UNIT;
    }

    @Override
    public int getSdfsLocalCacheSizeWithoutMeasureUnit() {
        return currentConfiguration.getSdfsLocalCacheSize();
    }

    @Override
    public String getSdfsVolumeSize() {
        return currentConfiguration.getSdfsSize() + VOLUME_SIZE_UNIT;
    }

    @Override
    public int getSdfsVolumeSizeWithoutMeasureUnit() {
        return currentConfiguration.getSdfsSize();
    }

    @Override
    public String getSdfsConfigPath() {
        return currentConfiguration.getSdfsConfigPath();
    }

    @Override
    public String getSdfsBackupFileName() {
        return currentConfiguration.getSdfsBackupFileName();
    }

    @Override
    public int getWaitTimeBeforeNewSyncWithAWS() {
        return currentConfiguration.getWaitTimeBeforeNewSyncWithAWS();
    }

    @Override
    public int getMaxWaitTimeToDetachVolume() {
        return currentConfiguration.getMaxWaitTimeToDetachVolume();
    }

    @Override
    public int getTaskHistoryTTS() {
        return currentConfiguration.getTaskHistoryTTS();
    }

    @Override
    public void setCurrentConfiguration(final Configuration currentConfiguration) {
        this.currentConfiguration = currentConfiguration;
    }

    @Override
    public Configuration getCurrentConfiguration() {
        return currentConfiguration;
    }

    @Override
    public String getVolumeSizeUnit() {
        return VOLUME_SIZE_UNIT;
    }

    public boolean isSsoLoginMode() {
        return this.currentConfiguration.isSsoLoginMode();
    }

    public String getSamlEntityId() {
        return this.currentConfiguration.getEntityId();
    }

    @Override
    public boolean isStoreSnapshot() {
        return currentConfiguration.isStoreSnapshot();
    }

    @Override
    public int getLogsBufferSize() {
        return currentConfiguration.getLogsBufferSize();
    }

    @Override
    public String getLogFileName() {
        return currentConfiguration.getLogFile();
    }

    public String getDomain() {
        return currentConfiguration.getDomain();
    }

    @Override
    public MailConfigurationDocument getMailConfiguration() {
        return currentConfiguration.getMailConfigurationDocument();
    }

    @Override
    public int getMinNodeNumberInCluster() {
        return currentConfiguration.getMinNodeNumber();
    }

    @Override
    public int getMaxNodeNumberInCluster() {
        return currentConfiguration.getMaxNodeNumber();
    }

    @Override
    public boolean isClusterMode() {
        return currentConfiguration.isClusterMode();
    }

    @Override
    public String getChunkStoreEncryptionKey() {
        return currentConfiguration.getChunkStoreEncryptionKey();
    }

    @Override
    public String getChunkStoreIV() {
        return currentConfiguration.getChunkStoreIV();
    }

    @Override
    public String getSdfsCliPsw() {
        return currentConfiguration.getSdfsCliPsw();
    }
}
