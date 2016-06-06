package com.sungardas.enhancedsnapshots.service.impl;

import javax.annotation.PostConstruct;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.sungardas.enhancedsnapshots.aws.dynamodb.model.Configuration;
import com.sungardas.enhancedsnapshots.aws.dynamodb.model.User;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

public class CreateAppConfigurationDev {

    @Value("${enhancedsnapshots.default.tempVolumeType}")
    private String tempVolumeType;
    @Value("${enhancedsnapshots.default.tempVolumeIopsPerGb}")
    private int tempVolumeIopsPerGb;
    @Value("${enhancedsnapshots.default.restoreVolumeType}")
    private String restoreVolumeType;
    @Value("${enhancedsnapshots.default.restoreVolumeIopsPerGb}")
    private int restoreVolumeIopsPerGb;
    @Value("${enhancedsnapshots.default.amazon.retry.count}")
    private int amazonRetryCount;
    @Value("${enhancedsnapshots.default.amazon.retry.sleep}")
    private int amazonRetrySleep;
    @Value("${enhancedsnapshots.default.queue.size}")
    private int queueSize;
    @Value("${enhancedsnapshots.default.sdfs.volume.config.path}")
    private String sdfsConfigPath;
    @Value("${enhancedsnapshots.default.sdfs.backup.file.name}")
    private String sdfsStateBackupFileName;
    @Value("${enhancedsnapshots.default.retention.cron}")
    private String defaultRetentionCronExpression;
    @Value("${enhancedsnapshots.default.polling.rate}")
    private int defaultPollingRate;
    @Value("${enhancedsnapshots.default.sdfs.local.cache.size}")
    private int sdfsLocalCacheSize;
    @Value("${enhancedsnapshots.default.wait.time.before.new.sync}")
    private int defaultWaitTimeBeforeNewSyncWithAWS;
    @Value("${enhancedsnapshots.default.max.wait.time.to.detach.volume}")
    private int defaultMaxWaitTimeToDetachVolume;

    @Autowired
    private AmazonDynamoDB amazonDynamoDB;

    @PostConstruct
    private void init() {
        DynamoDBMapper mapper = new DynamoDBMapper(amazonDynamoDB);
        Configuration configuration = getDevConf();
        mapper.save(configuration);

        User user = new User("admin@admin", DigestUtils.sha512Hex("admin"), "admin", "dev", "dev", "DEV");
        user.setId("DEV");
        mapper.save(user);
    }

    private Configuration getDevConf() {
        Configuration configuration = new Configuration();
        configuration.setConfigurationId("DEV");
        configuration.setEc2Region(Regions.EU_WEST_1.getName());
        configuration.setSdfsMountPoint("");
        configuration.setSdfsVolumeName("");
        configuration.setRestoreVolumeIopsPerGb(restoreVolumeIopsPerGb);
        configuration.setRestoreVolumeType(restoreVolumeType);
        configuration.setTempVolumeIopsPerGb(tempVolumeIopsPerGb);
        configuration.setTempVolumeType(tempVolumeType);
        configuration.setSdfsLocalCacheSize(sdfsLocalCacheSize);
        configuration.setAmazonRetryCount(amazonRetryCount);
        configuration.setAmazonRetrySleep(amazonRetrySleep);
        configuration.setMaxQueueSize(queueSize);
        configuration.setSdfsConfigPath(sdfsConfigPath);
        configuration.setSdfsBackupFileName(sdfsStateBackupFileName);
        configuration.setRetentionCronExpression(defaultRetentionCronExpression);
        configuration.setWorkerDispatcherPollingRate(defaultPollingRate);
        configuration.setWaitTimeBeforeNewSyncWithAWS(defaultWaitTimeBeforeNewSyncWithAWS);
        configuration.setMaxWaitTimeToDetachVolume(defaultMaxWaitTimeToDetachVolume);
        configuration.setS3Bucket("com.sungardas.enhancedsnapshots.dev");
        configuration.setSdfsSize(500);
        configuration.setSdfsVolumeName("awspool");
        configuration.setSdfsMountPoint("/mnt/awspool");

        return configuration;
    }
}

