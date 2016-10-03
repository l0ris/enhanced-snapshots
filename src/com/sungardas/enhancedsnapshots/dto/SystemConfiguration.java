package com.sungardas.enhancedsnapshots.dto;

public class SystemConfiguration {

    private S3 s3;
    private SDFS sdfs;
    private Long lastBackup;
    private EC2Instance ec2Instance;
    private SystemProperties systemProperties;
    private String currentVersion;
    private String latestVersion;
    private boolean ssoMode;
    private String domain;
    private MailConfigurationDto mailConfiguration;


    public String getCurrentVersion() {
        return currentVersion;
    }

    public SystemProperties getSystemProperties() {
        return systemProperties;
    }

    public void setSystemProperties(SystemProperties systemProperties) {
        this.systemProperties = systemProperties;
    }

    public void setCurrentVersion(final String currentVersion) {
        this.currentVersion = currentVersion;
    }

    public String getLatestVersion() {
        return latestVersion;
    }

    public void setLatestVersion(final String latestVersion) {
        this.latestVersion = latestVersion;
    }

    public EC2Instance getEc2Instance() {
        return ec2Instance;
    }

    public void setEc2Instance(EC2Instance ec2Instance) {
        this.ec2Instance = ec2Instance;
    }

    public SDFS getSdfs() {
        return sdfs;
    }

    public void setSdfs(SDFS sdfs) {
        this.sdfs = sdfs;
    }

    public S3 getS3() {
        return s3;
    }

    public void setS3(S3 s3) {
        this.s3 = s3;
    }


    public Long getLastBackup() {
        return lastBackup;
    }

    public void setLastBackup(Long lastBackup) {
        this.lastBackup = lastBackup;
    }

    public boolean isSsoMode() {
        return ssoMode;
    }

    public void setSsoMode(boolean ssoMode) {
        this.ssoMode = ssoMode;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public MailConfigurationDto getMailConfiguration() {
        return mailConfiguration;
    }

    public void setMailConfiguration(MailConfigurationDto mailConfiguration) {
        this.mailConfiguration = mailConfiguration;
    }

    public static class S3 {
        private String bucketName;

        public String getBucketName() {
            return bucketName;
        }

        public void setBucketName(String bucketName) {
            this.bucketName = bucketName;
        }
    }

    public static class SDFS {

        private String mountPoint;
        private String volumeName;

        private int volumeSize;
        private int minVolumeSize;
        private int maxVolumeSize;

        private int sdfsLocalCacheSize;
        private int maxSdfsLocalCacheSize;
        private int minSdfsLocalCacheSize;


        public int getSdfsLocalCacheSize() {
            return sdfsLocalCacheSize;
        }

        public void setSdfsLocalCacheSize(int sdfsLocalCacheSize) {
            this.sdfsLocalCacheSize = sdfsLocalCacheSize;
        }

        public int getMaxSdfsLocalCacheSize() {
            return maxSdfsLocalCacheSize;
        }

        public void setMaxSdfsLocalCacheSize(int maxSdfsLocalCacheSize) {
            this.maxSdfsLocalCacheSize = maxSdfsLocalCacheSize;
        }

        public int getMinSdfsLocalCacheSize() {
            return minSdfsLocalCacheSize;
        }

        public void setMinSdfsLocalCacheSize(int minSdfsLocalCacheSize) {
            this.minSdfsLocalCacheSize = minSdfsLocalCacheSize;
        }

        public int getMinVolumeSize() {
            return minVolumeSize;
        }

        public void setMinVolumeSize(int minVolumeSize) {
            this.minVolumeSize = minVolumeSize;
        }

        public int getMaxVolumeSize() {
            return maxVolumeSize;
        }

        public void setMaxVolumeSize(int maxVolumeSize) {
            this.maxVolumeSize = maxVolumeSize;
        }

        public String getVolumeName() {
            return volumeName;
        }

        public void setVolumeName(String volumeName) {
            this.volumeName = volumeName;
        }

        public int getVolumeSize() {
            return volumeSize;
        }

        public void setVolumeSize(int volumeSize) {
            this.volumeSize = volumeSize;
        }

        public String getMountPoint() {
            return mountPoint;
        }

        public void setMountPoint(String mountPoint) {
            this.mountPoint = mountPoint;
        }
    }

    public static class EC2Instance {
        private String instanceID;

        public String getInstanceID() {
            return instanceID;
        }

        public void setInstanceID(String instanceID) {
            this.instanceID = instanceID;
        }
    }

    public static class SystemProperties {
        private String tempVolumeType;
        private int tempVolumeIopsPerGb;
        private String restoreVolumeType;
        private int restoreVolumeIopsPerGb;
        private String[] volumeTypeOptions;
        private int amazonRetryCount;
        private int amazonRetrySleep;
        private int maxQueueSize;
        private int taskHistoryTTS;
        private boolean storeSnapshots;
        private int logsBuffer;

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

        public String[] getVolumeTypeOptions() {
            return volumeTypeOptions;
        }

        public void setVolumeTypeOptions(String[] volumeTypeOptions) {
            this.volumeTypeOptions = volumeTypeOptions;
        }

        public int getTaskHistoryTTS() {
            return taskHistoryTTS;
        }

        public void setTaskHistoryTTS(int taskHistoryTTS) {
            this.taskHistoryTTS = taskHistoryTTS;
        }

        public boolean isStoreSnapshots() {
            return storeSnapshots;
        }

        public void setStoreSnapshots(boolean storeSnapshots) {
            this.storeSnapshots = storeSnapshots;
        }

        public int getLogsBuffer() {
            return logsBuffer;
        }

        public void setLogsBuffer(int logsBuffer) {
            this.logsBuffer = logsBuffer;
        }
    }
}
