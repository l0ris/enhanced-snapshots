package com.sungardas.init;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.internal.StaticCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.internal.BucketNameUtils;
import com.amazonaws.services.s3.model.Bucket;
import com.sungardas.enhancedsnapshots.aws.dynamodb.model.User;
import com.sungardas.enhancedsnapshots.dto.InitConfigurationDto;
import com.sungardas.enhancedsnapshots.dto.converter.BucketNameValidationDTO;
import com.sungardas.enhancedsnapshots.exception.ConfigurationException;
import com.sungardas.enhancedsnapshots.service.impl.CryptoServiceImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

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


    private AWSCredentialsProvider credentialsProvider;
    private AmazonS3Client amazonS3;

    @Override
    public void removeProperties() {
    }

    @PostConstruct
    private void init() {
        String accessKey = new CryptoServiceImpl().decrypt(instanceId, amazonAWSAccessKey);
        String secretKey = new CryptoServiceImpl().decrypt(instanceId, amazonAWSSecretKey);
        credentialsProvider = new StaticCredentialsProvider(new BasicAWSCredentials(accessKey, secretKey));
        amazonS3 =  new AmazonS3Client(credentialsProvider);
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
            for(Bucket bucket: buckets){
                if (bucket.getName().equals(bucketName)){
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

    @Override
    public InitConfigurationDto.DB containsMetadata(final String bucketName) {
        InitConfigurationDto.DB db = new InitConfigurationDto.DB();
        db.setValid(true);
        db.setAdminExist(true);
        return db;
    }
}
