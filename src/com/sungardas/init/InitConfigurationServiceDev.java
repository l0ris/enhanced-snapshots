package com.sungardas.init;

import java.util.ArrayList;
import java.util.List;

import com.amazonaws.auth.AWSCredentialsProvider;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.internal.StaticCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.ListTablesResult;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.internal.BucketNameUtils;
import com.amazonaws.services.s3.model.Bucket;
import com.sungardas.enhancedsnapshots.aws.AmazonConfigProviderDEV;
import com.sungardas.enhancedsnapshots.aws.dynamodb.model.*;
import com.sungardas.enhancedsnapshots.dto.InitConfigurationDto;
import com.sungardas.enhancedsnapshots.dto.converter.BucketNameValidationDTO;
import com.sungardas.enhancedsnapshots.exception.ConfigurationException;
import com.sungardas.enhancedsnapshots.service.impl.CryptoServiceImpl;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.PostConstruct;

class InitConfigurationServiceDev implements InitConfigurationService {


    private static final Logger LOG = LogManager.getLogger(InitConfigurationServiceDev.class);

    @Value("${enhancedsnapshots.bucket.name.prefix.002}")
    private String enhancedSnapshotBucketPrefix;
    @Value("${amazon.aws.accesskey}")
    private String amazonAWSAccessKey;
    @Value("${amazon.aws.secretkey}")
    private String amazonAWSSecretKey;
    @Value("${sungardas.worker.configuration}")
    private String instanceId;
    @Value("${amazon.aws.region}")
    private String region;
    @Value("${enhancedsnapshots.db.tables}")
    private String[] tables;
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


    private AWSCredentialsProvider credentialsProvider;
    private AmazonS3Client amazonS3;

    @Override
    public void removeProperties() {
    }

    @Autowired
    private AmazonDynamoDB amazonDynamoDB;
    private DynamoDBMapper mapper = new DynamoDBMapper(amazonDynamoDB);


    @PostConstruct
    private void init() {
        String accessKey = new CryptoServiceImpl().decrypt(instanceId, amazonAWSAccessKey);
        String secretKey = new CryptoServiceImpl().decrypt(instanceId, amazonAWSSecretKey);
        credentialsProvider = new StaticCredentialsProvider(new BasicAWSCredentials(accessKey, secretKey));
        amazonS3 = new AmazonS3Client(credentialsProvider);
        DynamoDBMapperConfig config = new DynamoDBMapperConfig.Builder().withTableNameOverride(DynamoDBMapperConfig.TableNameOverride.
                withTableNamePrefix(AmazonConfigProviderDEV.getDynamoDbPrefix("DEV"))).build();
        mapper = new DynamoDBMapper(amazonDynamoDB, config);
    }

    @Override
    public InitConfigurationDto getInitConfigurationDto() {
        InitConfigurationDto config = new InitConfigurationDto();
        List<InitConfigurationDto.S3> names = new ArrayList<>();
        names.add(new InitConfigurationDto.S3(enhancedSnapshotBucketPrefix + "s0", false));
        names.add(new InitConfigurationDto.S3(enhancedSnapshotBucketPrefix + "s1", true));
        names.add(new InitConfigurationDto.S3(enhancedSnapshotBucketPrefix + "s2", true));

        InitConfigurationDto.SDFS sdfs = new InitConfigurationDto.SDFS();
        sdfs.setCreated(true);
        sdfs.setMountPoint("/mnt/awspool");
        sdfs.setVolumeName("awspool");
        sdfs.setVolumeSize("40");
        sdfs.setMinVolumeSize("10");
        sdfs.setMaxVolumeSize("2000");
        sdfs.setSdfsLocalCacheSize(1);
        sdfs.setMinSdfsLocalCacheSize(0);
        sdfs.setMaxSdfsLocalCacheSize(3);

        InitConfigurationDto.DB db = new InitConfigurationDto.DB();
        db.setValid(true);
        db.setAdminExist(true);


        config.setS3(names);
        config.setSdfs(sdfs);
        config.setDb(db);
        config.setImmutableBucketNamePrefix(enhancedSnapshotBucketPrefix);

        return config;
    }

    @Override
    public boolean propertyFileExists() {
        return false;
    }

    @Override
    public boolean checkDefaultUser(String login, String password) {
        return true;
    }

    @Override
    public String getInstanceId() {
        return "DEV";
    }

    @Override
    public void configureAWSLogAgent() {
    }

    @Override
    public void validateVolumeSize(final int volumeSize) {
        int min = 10;
        int max = 2000;
        if (volumeSize < min || volumeSize > max) {
            throw new ConfigurationException("Invalid volume size");
        }
    }

    @Override
    public void storePropertiesEditableFromConfigFile() {
    }

    @Override
    public void setUser(User user) {

    }

    @Override
    public void createDBAndStoreSettings(final InitController.ConfigDto config) {
        createDbStructure();
        storeSettings();
    }

    private void storeSettings() {
        Configuration configuration = getDevConf();
        mapper.save(configuration);

        User user = new User("admin@admin", DigestUtils.sha512Hex("admin"), "admin", "dev", "dev");
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

    private void createDbStructure() throws ConfigurationException {
        createTable(BackupEntry.class);
        createTable(Configuration.class);
        createTable(RetentionEntry.class);
        createTable(TaskEntry.class);
        createTable(SnapshotEntry.class);
        createTable(User.class);
    }

    private void createTable(Class tableClass) {
        CreateTableRequest createTableRequest = mapper.generateCreateTableRequest(tableClass);

        createTableRequest.setProvisionedThroughput(new ProvisionedThroughput(50L, 20L));
        if (tableExists(createTableRequest.getTableName())) {
            LOG.info("Table {} already exists", createTableRequest.getTableName());
            return;
        }
        try {
            DynamoDB dynamoDB = new DynamoDB(amazonDynamoDB);
            Table table = dynamoDB.createTable(createTableRequest);
            LOG.info("Creating table {} ... ", createTableRequest.getTableName());
            table.waitForActive();
            LOG.info("Table {} was created successfully.", createTableRequest.getTableName());
        } catch (Exception e) {
            LOG.error("Failed to create table {}. ", createTableRequest.getTableName());
            LOG.error(e);
            throw new ConfigurationException("Failed to create table" + createTableRequest.getTableName(), e);
        }
    }

    private boolean tableExists(String tableName) {
        ListTablesResult listResult = amazonDynamoDB.listTables();
        List<String> tableNames = listResult.getTableNames();
        return tableNames.contains(tableName);
    }


    @Override
    public void syncSettingsInDbAndConfigFile() {

    }

    public BucketNameValidationDTO validateBucketName(String bucketName) {
        if (!bucketName.startsWith(enhancedSnapshotBucketPrefix)) {
            return new BucketNameValidationDTO(false, "Bucket name should start with " + enhancedSnapshotBucketPrefix);
        }
        if (amazonS3.doesBucketExist(bucketName)) {
            // check whether we own this bucket
            List<Bucket> buckets = amazonS3.listBuckets();
            for (Bucket bucket : buckets) {
                if (bucket.getName().equals(bucketName)) {
                    return new BucketNameValidationDTO(true, "");
                }
            }
            return new BucketNameValidationDTO(false, "The requested bucket name is not available.Please select a different name.");
        }
        try {
            BucketNameUtils.validateBucketName(bucketName);
            return new BucketNameValidationDTO(true, "");
        } catch (IllegalArgumentException e) {
            return new BucketNameValidationDTO(false, e.getMessage());
        }
    }

    @Override
    public void createBucket(String bucketName) {
        BucketNameValidationDTO validationDTO = validateBucketName(bucketName);
        if (!validationDTO.isValid()) {
            throw new IllegalArgumentException(validationDTO.getMessage());
        }
        if (!amazonS3.doesBucketExist(bucketName)) {
            LOG.info("Creating bucket {} in {}", bucketName, "us-west-2");
            amazonS3.createBucket(bucketName, "us-west-2");
            // delete created bucket in dev mode, we do not need it
            LOG.info("Removing bucket {} in {}", bucketName, "us-west-2");
            amazonS3.deleteBucket(bucketName);
        }
    }
}
