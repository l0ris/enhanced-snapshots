package com.sungardas.enhancedsnapshots.service.impl;


import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.IDynamoDBMapper;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.VolumeType;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.util.EC2MetadataUtils;
import com.sungardas.enhancedsnapshots.aws.dynamodb.model.*;
import com.sungardas.enhancedsnapshots.aws.dynamodb.repository.ConfigurationRepository;
import com.sungardas.enhancedsnapshots.components.ConfigurationMediatorConfigurator;
import com.sungardas.enhancedsnapshots.components.WorkersDispatcher;
import com.sungardas.enhancedsnapshots.dto.MailConfigurationDto;
import com.sungardas.enhancedsnapshots.dto.SystemConfiguration;
import com.sungardas.enhancedsnapshots.exception.ConfigurationException;
import com.sungardas.enhancedsnapshots.exception.EnhancedSnapshotsException;
import com.sungardas.enhancedsnapshots.service.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.env.PropertySource;
import org.springframework.web.context.support.XmlWebApplicationContext;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


/**
 * Implementation for {@link SystemService}
 */
@DependsOn("CreateAppConfiguration")
public class SystemServiceImpl implements SystemService {
    private static final Logger LOG = LogManager.getLogger(SystemServiceImpl.class);

    private static final String CURRENT_VERSION = "0.0.2";
    private static final String LATEST_VERSION = "latest-version";
    private static final String INFO_URL = "http://com.sungardas.releases.s3.amazonaws.com/info";

    private static final String TEMP_DIRECTORY_PREFIX = "systemBackupFiles";
    private static final String INFO_FILE_NAME = "info";
    private static final String VERSION_KEY = "version";
    private static final String TEMP_FILE_SUFFIX = "ZIP";

    private static final String[] VOLUME_TYPE_OPTIONS = new String[]{VolumeType.Gp2.toString(), VolumeType.Io1.toString(), VolumeType.Standard.toString()};

    @Value("${enhancedsnapshots.system.reserved.memory}")
    private int systemReservedRam;
    @Value("${enhancedsnapshots.sdfs.volume.size.per.1gb.ram}")
    private int volumeSizePerGbOfRam;
    @Value("${enhancedsnapshots.sdfs.reserved.ram}")
    private int sdfsReservedRam;
    @Value("${enhancedsnapshots.system.reserved.storage}")
    private int systemReservedStorage;
    @Value("${enhancedsnapshots.saml.sp.cert.jks}")
    private String samlCertJks;
    @Value("${enhancedsnapshots.saml.idp.metadata}")
    private String samlIdpMetadata;

    @Autowired
    private IDynamoDBMapper dynamoDBMapper;

    @Autowired
    private AmazonS3 amazonS3;

    @Autowired
    private ConfigurationRepository configurationRepository;

    @Autowired
    private ConfigurationMediatorConfigurator configurationMediator;


    @Autowired
    private SDFSStateService sdfsStateService;

    @Autowired
    private NotificationService notificationService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Configuration currentConfiguration;

    @Autowired
    private XmlWebApplicationContext applicationContext;

    @Autowired
    private CryptoService cryptoService;

    @Autowired
    private MailService mailService;

    @Autowired
    private AmazonEC2 ec2client;


    @PostConstruct
    private void init() {
        currentConfiguration = dynamoDBMapper.load(Configuration.class, getInstanceId());
        configurationMediator.setCurrentConfiguration(currentConfiguration);
        mailService.reconnect();
    }

    @Override
    public void backup(final String taskId) {
        try {
            LOG.info("System backup started");
            notificationService.notifyAboutRunningTaskProgress(taskId, "System backup started", 0);
            Path tempDirectory = Files.createTempDirectory(TEMP_DIRECTORY_PREFIX);
            LOG.info("Add info file");
            notificationService.notifyAboutRunningTaskProgress(taskId, "System information backup", 5);
            addInfo(tempDirectory);
            LOG.info("Backup SDFS state");
            notificationService.notifyAboutRunningTaskProgress(taskId, "Backup SDFS state", 10);
            backupSDFS(tempDirectory, taskId);
            notificationService.notifyAboutRunningTaskProgress(taskId, "Backup system files", 55);
            LOG.info("Backup files");
            storeFiles(tempDirectory);
            LOG.info("Backup db");
            notificationService.notifyAboutRunningTaskProgress(taskId, "Backup DB", 60);
            backupDB(tempDirectory, taskId);
            if (configurationMediator.isSsoLoginMode()){
                LOG.info("Backup up saml certificate and ipd metadata", 90);
                backupSamlFiles(tempDirectory);
            }
            LOG.info("Upload to S3");
            notificationService.notifyAboutRunningTaskProgress(taskId, "Upload to S3", 95);
            uploadToS3(tempDirectory);
            tempDirectory.toFile().delete();
            LOG.info("System backup finished");
        } catch (IOException e) {
            LOG.error("System backup failed");
            LOG.error(e);
            throw new EnhancedSnapshotsException(e);
        }
    }

    /**
     * Adding metainfo to system backup
     * @param tempDirectory directory where system backup is stored
     * @throws IOException checked file system exception
     */
    private void addInfo(final Path tempDirectory) throws IOException {
        File dest = Paths.get(tempDirectory.toString(), INFO_FILE_NAME).toFile();
        Map<String, String> info = new HashMap<>();
        info.put(VERSION_KEY, CURRENT_VERSION);
        objectMapper.writeValue(dest, info);
    }

    private void backupDB(final Path tempDirectory, final String taskId) throws IOException {
        notificationService.notifyAboutRunningTaskProgress(taskId, "Backup DB", 65);
        storeTable(BackupEntry.class, tempDirectory);
        notificationService.notifyAboutRunningTaskProgress(taskId, "Backup DB", 70);
        storeTable(Configuration.class, tempDirectory);
        notificationService.notifyAboutRunningTaskProgress(taskId, "Backup DB", 75);
        storeTable(RetentionEntry.class, tempDirectory);
        notificationService.notifyAboutRunningTaskProgress(taskId, "Backup DB", 80);
        storeTable(SnapshotEntry.class, tempDirectory);
        notificationService.notifyAboutRunningTaskProgress(taskId, "Backup DB", 85);
        storeTable(User.class, tempDirectory);
        notificationService.notifyAboutRunningTaskProgress(taskId, "Backup DB", 90);
    }

    private void backupSDFS(final Path tempDirectory, final String taskId) throws IOException {
        notificationService.notifyAboutRunningTaskProgress(taskId, "Backup SDFS state", 15);
        copyToDirectory(Paths.get(currentConfiguration.getSdfsConfigPath()), tempDirectory);
        notificationService.notifyAboutRunningTaskProgress(taskId, "Backup SDFS state", 20);
        sdfsStateService.cloudSync();
        notificationService.notifyAboutRunningTaskProgress(taskId, "Backup SDFS state", 45);
    }

    private void backupSamlFiles(final Path tempDirectory) throws IOException {
        copyToDirectory(Paths.get(System.getProperty("catalina.home"), samlCertJks), tempDirectory);
        copyToDirectory(Paths.get(System.getProperty("catalina.home"), samlIdpMetadata), tempDirectory);
    }

    private void storeFiles(Path tempDirectory) {
        //nginx certificates
        try {
            copyToDirectory(Paths.get(currentConfiguration.getNginxCertPath()), tempDirectory);
            copyToDirectory(Paths.get(currentConfiguration.getNginxKeyPath()), tempDirectory);
        } catch (IOException e) {
            LOG.warn("Nginx certificate not found");
        }
    }

    private void uploadToS3(final Path tempDirectory) throws IOException {
        // Compress to zip
        LOG.info("  -Compress files");
        File[] files = tempDirectory.toFile().listFiles();
        Path zipFile = Files.createTempFile(TEMP_DIRECTORY_PREFIX, TEMP_FILE_SUFFIX);
        try (FileOutputStream fos = new FileOutputStream(zipFile.toFile());
             ZipOutputStream zos = new ZipOutputStream(fos)) {
            for (File file : files) {
                zos.putNextEntry(new ZipEntry(file.getName()));
                Files.copy(file.toPath(), zos);
                zos.closeEntry();
            }
        }

        //Upload
        LOG.info("  -Upload");
        PutObjectRequest putObjectRequest = new PutObjectRequest(configurationMediator.getS3Bucket(),
                configurationMediator.getSdfsBackupFileName(), zipFile.toFile());
        amazonS3.putObject(putObjectRequest);

        zipFile.toFile().delete();
    }

    private void storeTable(Class tableClass, Path tempDirectory) throws IOException {
        LOG.info("Backup DB table: {}", tableClass.getSimpleName());
        File dest = Paths.get(tempDirectory.toString(), tableClass.getName()).toFile();
        List result = dynamoDBMapper.scan(tableClass, new DynamoDBScanExpression());
        objectMapper.writeValue(dest, result);
    }

    @Override
    public SystemConfiguration getSystemConfiguration() {
        SystemConfiguration configuration = new SystemConfiguration();

        configuration.setS3(new SystemConfiguration.S3());
        configuration.getS3().setBucketName(configurationMediator.getS3Bucket());

        configuration.setSdfs(new SystemConfiguration.SDFS());
        configuration.getSdfs().setMountPoint(configurationMediator.getSdfsMountPoint());
        configuration.getSdfs().setVolumeName(configurationMediator.getSdfsVolumeName());
        configuration.getSdfs().setVolumeSize(currentConfiguration.getSdfsSize());
        // user can only expand volume size
        configuration.getSdfs().setMinVolumeSize(currentConfiguration.getSdfsSize());
        configuration.getSdfs().setMaxVolumeSize(SDFSStateService.getMaxVolumeSize(systemReservedRam, volumeSizePerGbOfRam,  sdfsReservedRam));

        configuration.getSdfs().setSdfsLocalCacheSize(currentConfiguration.getSdfsLocalCacheSize());
        configuration.getSdfs().setMaxSdfsLocalCacheSize(SDFSStateService.getFreeStorageSpace(systemReservedStorage) + configurationMediator.getSdfsLocalCacheSizeWithoutMeasureUnit());
        configuration.getSdfs().setMinSdfsLocalCacheSize(configurationMediator.getSdfsLocalCacheSizeWithoutMeasureUnit());

        configuration.setEc2Instance(new SystemConfiguration.EC2Instance());
        configuration.getEc2Instance().setInstanceID(getInstanceId());

        configuration.setLastBackup(getBackupTime());
        configuration.setCurrentVersion(CURRENT_VERSION);
        configuration.setLatestVersion(getLatestVersion());

        SystemConfiguration.SystemProperties systemProperties = new SystemConfiguration.SystemProperties();
        systemProperties.setRestoreVolumeIopsPerGb(configurationMediator.getRestoreVolumeIopsPerGb());
        systemProperties.setRestoreVolumeType(configurationMediator.getRestoreVolumeType().toString());
        systemProperties.setTempVolumeIopsPerGb(configurationMediator.getTempVolumeIopsPerGb());
        systemProperties.setTempVolumeType(configurationMediator.getTempVolumeType().toString());
        systemProperties.setVolumeTypeOptions(VOLUME_TYPE_OPTIONS);
        systemProperties.setAmazonRetryCount(configurationMediator.getAmazonRetryCount());
        systemProperties.setAmazonRetrySleep(configurationMediator.getAmazonRetrySleep());
        systemProperties.setMaxQueueSize(configurationMediator.getMaxQueueSize());
        systemProperties.setStoreSnapshots(configurationMediator.isStoreSnapshot());
        systemProperties.setTaskHistoryTTS(configurationMediator.getTaskHistoryTTS());
        configuration.setSystemProperties(systemProperties);
        configuration.setSsoMode(configurationMediator.isSsoLoginMode());
        configuration.setDomain(configurationMediator.getDomain());
        if (configurationMediator.getMailConfiguration() != null) {
            MailConfigurationDto mailConfigurationDto = new MailConfigurationDto();
            BeanUtils.copyProperties(configurationMediator.getMailConfiguration(), mailConfigurationDto);
            configuration.setMailConfiguration(mailConfigurationDto);
        }
        return configuration;
    }

    @Override
    public void setSystemConfiguration(SystemConfiguration configuration) {
        LOG.info("Updating system properties.");
        // mail configuration
        boolean mailReconnect = false;
        if (configuration.getMailConfiguration() == null) {
            currentConfiguration.setMailConfigurationDocument(null);
            mailService.disconnect();
        } else {
            MailConfigurationDocument configurationDocument = new MailConfigurationDocument();
            BeanUtils.copyProperties(configuration.getMailConfiguration(), configurationDocument);
            if (configuration.getMailConfiguration().getEvents() != null) {
                BeanUtils.copyProperties(configuration.getMailConfiguration().getEvents(), configurationDocument.getEvents());
            }
            if (configurationDocument.getPassword() != null) {
                configurationDocument.setPassword(cryptoService.encrypt(currentConfiguration.getConfigurationId(), configurationDocument.getPassword()));
            } else {
                configurationDocument.setPassword(currentConfiguration.getMailConfigurationDocument().getPassword());
            }
            if (!mailService.checkConfiguration(configurationDocument)) {
                throw new ConfigurationException("Mail configuration is incorrect");
            } else {
                currentConfiguration.setMailConfigurationDocument(configurationDocument);
                mailReconnect = true;
            }
        }
        // update system properties
        currentConfiguration.setRestoreVolumeIopsPerGb(configuration.getSystemProperties().getRestoreVolumeIopsPerGb());
        currentConfiguration.setRestoreVolumeType(configuration.getSystemProperties().getRestoreVolumeType());
        currentConfiguration.setTempVolumeIopsPerGb(configuration.getSystemProperties().getTempVolumeIopsPerGb());
        currentConfiguration.setTempVolumeType(configuration.getSystemProperties().getTempVolumeType());
        currentConfiguration.setAmazonRetryCount(configuration.getSystemProperties().getAmazonRetryCount());
        currentConfiguration.setAmazonRetrySleep(configuration.getSystemProperties().getAmazonRetrySleep());
        currentConfiguration.setMaxQueueSize(configuration.getSystemProperties().getMaxQueueSize());
        currentConfiguration.setStoreSnapshot(configuration.getSystemProperties().isStoreSnapshots());
        currentConfiguration.setTaskHistoryTTS(configuration.getSystemProperties().getTaskHistoryTTS());
        if (configuration.getDomain() == null) {
            currentConfiguration.setDomain("");
        }
        currentConfiguration.setDomain(configuration.getDomain());

        // update sdfs setting
        currentConfiguration.setSdfsSize(configuration.getSdfs().getVolumeSize());
        currentConfiguration.setSdfsLocalCacheSize(configuration.getSdfs().getSdfsLocalCacheSize());

        // update bucket
        currentConfiguration.setS3Bucket(configuration.getS3().getBucketName());

        configurationRepository.save(currentConfiguration);

        configurationMediator.setCurrentConfiguration(currentConfiguration);
        if (mailReconnect) {
            mailService.reconnect();
        }
    }

    @Override
    public void systemUninstall(boolean removeS3Bucket) {
        LOG.info("Uninstaling system. S3 bucket will be removed: {}", removeS3Bucket);
        applicationContext.setConfigLocation("/WEB-INF/destroy-spring-web-config.xml");
        applicationContext.getAutowireCapableBeanFactory().destroyBean(WorkersDispatcher.class);
        new Thread() {
            @Override
            public void run() {
                applicationContext.getEnvironment().getPropertySources().addLast(
                        new DestroyContextPropertySource(removeS3Bucket));
                LOG.info("Context refresh started");
                applicationContext.refresh();
            }
        }.start();
    }

    /**
     * Returns the latest official version of application
     * Current version taken from {@link SystemServiceImpl::INFO_URL}
     * @return latest version
     */
    private String getLatestVersion() {
        try {
            URL infoURL = new URL(INFO_URL);
            Properties properties = new Properties();
            properties.load(infoURL.openStream());
            String latestVersion = properties.getProperty(LATEST_VERSION);
            if (latestVersion != null) {
                return latestVersion;
            }
        } catch (Exception e) {
        }
        return CURRENT_VERSION;
    }


    protected String getInstanceId() {
        return EC2MetadataUtils.getInstanceId();
    }

    //TODO: this should be stored in DB
    private Long getBackupTime() {
        ListObjectsRequest request = new ListObjectsRequest()
                .withBucketName(configurationMediator.getS3Bucket()).withPrefix(configurationMediator.getSdfsBackupFileName());
        List<S3ObjectSummary> list = amazonS3.listObjects(request).getObjectSummaries();
        if (list.size() > 0) {
            return list.get(0).getLastModified().getTime();
        } else {
            return null;
        }
    }

    private void copyToDirectory(Path src, Path dest) throws IOException {
        Path srcFileName = src.getFileName();
        Files.copy(src, Paths.get(dest.toString(), srcFileName.toString()), StandardCopyOption.REPLACE_EXISTING);
    }

    // Passing removeS3Bucket property to context
    private static class DestroyContextPropertySource extends PropertySource<String> {
        private boolean removeS3Bucket;

        public DestroyContextPropertySource(boolean removeS3Bucket) {
            super("custom");
            this.removeS3Bucket = removeS3Bucket;
        }

        @Override
        public String getProperty(String name) {
            if (name.equals("removeS3Bucket")) {
                return Boolean.toString(removeS3Bucket);
            }
            return null;
        }
    }
}
