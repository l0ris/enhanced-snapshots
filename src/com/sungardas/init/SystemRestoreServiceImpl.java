package com.sungardas.init;

import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;
import com.amazonaws.services.dynamodbv2.datamodeling.IDynamoDBMapper;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.util.EC2MetadataUtils;
import com.sungardas.enhancedsnapshots.aws.AmazonConfigProvider;
import com.sungardas.enhancedsnapshots.aws.dynamodb.model.*;
import com.sungardas.enhancedsnapshots.exception.EnhancedSnapshotsException;
import com.sungardas.enhancedsnapshots.service.upgrade.SystemUpgrade;
import com.sungardas.enhancedsnapshots.service.upgrade.UpgradeSystemTo002;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


public class SystemRestoreServiceImpl implements SystemRestoreService {

    private static final Logger LOG = LogManager.getLogger(SystemRestoreServiceImpl.class);

    private static final String TEMP_DIRECTORY_PREFIX = "systemBackupFiles";
    private static final String INFO_FILE_NAME = "info";
    private static final String VERSION_KEY = "version";
    private static final String TEMP_FILE_SUFFIX = "ZIP";

    @Value("${enhancedsnapshots.saml.sp.cert.jks}")
    private String samlCertJks;
    @Value("${enhancedsnapshots.saml.idp.metadata}")
    private String samlIdpMetadata;
    @Value("${enhancedsnapshots.default.sdfs.backup.file.name}")
    private String backupZipName;

    private Configuration currentConfiguration;

    private SystemUpgrade systemUpgrade;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private IDynamoDBMapper dynamoDBMapper;
    private AmazonS3 amazonS3;

    private void init() {
        InstanceProfileCredentialsProvider credentialsProvider = new InstanceProfileCredentialsProvider();
        AmazonDynamoDB amazonDynamoDB = new AmazonDynamoDBClient(credentialsProvider);
        amazonDynamoDB.setRegion(Regions.getCurrentRegion());
        dynamoDBMapper = new DynamoDBMapper(amazonDynamoDB, dynamoDBMapperConfig());
        amazonS3 = new AmazonS3Client(credentialsProvider);
        Region current = Regions.getCurrentRegion();
        if (!current.equals(Region.getRegion(Regions.US_EAST_1))) {
            amazonS3.setRegion(current);
        }
        systemUpgrade = new UpgradeSystemTo002();
    }

    public DynamoDBMapperConfig dynamoDBMapperConfig() {
        DynamoDBMapperConfig.Builder builder = new DynamoDBMapperConfig.Builder();
        builder.withTableNameOverride(DynamoDBMapperConfig.TableNameOverride.
                withTableNamePrefix(AmazonConfigProvider.getDynamoDbPrefix()));
        return builder.build();
    }

    public void restore(String bucketName) {
        try {
            init();
            LOG.info("System restore started");
            Path tempDirectory = Files.createTempDirectory(TEMP_DIRECTORY_PREFIX);
            LOG.info("Download from S3");
            downloadFromS3(tempDirectory, bucketName);
            systemUpgrade.upgrade(tempDirectory, getBackupVersion(tempDirectory));
            LOG.info("Restore DB");
            restoreDB(tempDirectory);
            LOG.info("Restore files");
            restoreFiles(tempDirectory);
            LOG.info("Restore SDFS state");
            restoreSDFS(tempDirectory);
            // restore SSO files if exist
            if(currentConfiguration.isSsoLoginMode()) {
                LOG.info("Restoring saml certificate and ipd metadata", 90);
                restoreSSOFiles(tempDirectory);
            }
        } catch (IOException e) {
            LOG.error("System restore failed");
            LOG.error(e);
            throw new EnhancedSnapshotsException(e);
        }
    }


    /**
     * Method for defining application version, which created system backup
     *
     * @param tempDirectory directory to which was unzipped system backup
     * @return application version
     */
    private String getBackupVersion(final Path tempDirectory) {
        Path infoFile = Paths.get(tempDirectory.toString(), INFO_FILE_NAME);
        if (infoFile.toFile().exists()) {
            try (FileInputStream fileInputStream = new FileInputStream(infoFile.toFile())) {
                HashMap<String, String> info = objectMapper.readValue(fileInputStream, HashMap.class);
                if (info.containsKey(VERSION_KEY)) {
                    return info.get(VERSION_KEY);
                } else {
                    LOG.error("Invalid info file formant");
                    throw new EnhancedSnapshotsException("Invalid info file formant");
                }
            } catch (IOException e) {
                LOG.error("Failed to parse info file");
                LOG.error(e);
                throw new EnhancedSnapshotsException(e);
            }
        }
        return "0.0.1";
    }

    private void restoreDB(Path tempDirectory) throws IOException {
        restoreConfiguration(tempDirectory);
        restoreTable(BackupEntry.class, tempDirectory);
        restoreTable(RetentionEntry.class, tempDirectory);
        restoreTable(SnapshotEntry.class, tempDirectory);
        restoreTable(User.class, tempDirectory);
        currentConfiguration = dynamoDBMapper.load(Configuration.class, EC2MetadataUtils.getInstanceId());
    }

    private void restoreSDFS(final Path tempDirectory) throws IOException {
        restoreFile(tempDirectory, Paths.get(currentConfiguration.getSdfsConfigPath()));
    }


    private void restoreFile(Path tempDirectory, Path destPath) throws IOException {
        Path fileName = destPath.getFileName();
        Files.copy(Paths.get(tempDirectory.toString(), fileName.toString()), destPath, StandardCopyOption.REPLACE_EXISTING);
    }


    private void restoreFiles(Path tempDirectory) {
        //nginx certificates
        try {
            restoreFile(tempDirectory, Paths.get(currentConfiguration.getNginxCertPath()));
            restoreFile(tempDirectory, Paths.get(currentConfiguration.getNginxKeyPath()));
        } catch (IOException e) {
            LOG.warn("Nginx certificate not found");
        }
    }

    private void restoreSSOFiles(Path tempDirectory) {
        try {
            restoreFile(tempDirectory, Paths.get(System.getProperty("catalina.home"), samlCertJks));
            restoreFile(tempDirectory, Paths.get(System.getProperty("catalina.home"), samlIdpMetadata));
        } catch (IOException e) {
            LOG.warn("Nginx certificate not found");
        }
    }

    private void downloadFromS3(Path tempDirectory, String bucketName) throws IOException {
        // download
        LOG.info("-Download");
        GetObjectRequest getObjectRequest = new GetObjectRequest(bucketName, backupZipName);
        S3Object s3object = amazonS3.getObject(getObjectRequest);

        Path tempFile = Files.createTempFile(TEMP_DIRECTORY_PREFIX, TEMP_FILE_SUFFIX);
        Files.copy(s3object.getObjectContent(), tempFile, StandardCopyOption.REPLACE_EXISTING);

        LOG.info("  -Unzip");
        //unzip
        try (FileInputStream fileInputStream = new FileInputStream(tempFile.toFile());
             ZipInputStream zipInputStream = new ZipInputStream(fileInputStream)) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                // in case entry is directory system backup relates to version 0.0.1
                // copy /etc/sdfs/awspool-volume-cfg.xml to temp dir
                if(entry.isDirectory()){
                    zipInputStream.getNextEntry();
                    zipInputStream.getNextEntry();
                    Path dest = Paths.get(tempDirectory.toString(), "awspool-volume-cfg.xml");
                    Files.copy(zipInputStream, dest, StandardCopyOption.REPLACE_EXISTING);
                    break;
                }
                Path dest = Paths.get(tempDirectory.toString(), entry.getName());
                Files.copy(zipInputStream, dest, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        //cleanup
        tempFile.toFile().delete();
    }

    private void restoreTable(Class tableClass, Path tempDirectory) throws IOException {
        LOG.info("  -Restore table: {}", tableClass.getSimpleName());
        File src = Paths.get(tempDirectory.toString(), tableClass.getName()).toFile();
        try (FileInputStream fileInputStream = new FileInputStream(src)) {
            ArrayList data = objectMapper.readValue(fileInputStream,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, tableClass));
            dynamoDBMapper.batchSave(data);
        } catch (IOException e) {
            LOG.warn("Table restore failed: {}", e.getLocalizedMessage());
        }
    }

    private void restoreConfiguration(final Path tempDirectory) {
        File src = Paths.get(tempDirectory.toString(), Configuration.class.getName()).toFile();
        try (FileInputStream fileInputStream = new FileInputStream(src)) {
            ArrayList<Configuration> data = objectMapper.readValue(fileInputStream,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Configuration.class));
            if (!data.isEmpty()) {
                Configuration configuration = data.get(0);
                configuration.setConfigurationId(EC2MetadataUtils.getInstanceId());
            }
            dynamoDBMapper.batchSave(data);
        } catch (IOException e) {
            LOG.warn("Table restore failed: {}", e.getLocalizedMessage());
        }
    }

}
