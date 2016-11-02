package com.sungardas.enhancedsnapshots.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;

public class InitConfigurationDto {
    private List<S3> s3 = Collections.EMPTY_LIST;
    private SDFS sdfs;
    private DB db;
    private String immutableBucketNamePrefix;
    private boolean clusterMode;
    private String UUID;

    public List<S3> getS3() {
        return s3;
    }

    public void setS3(List<S3> s3) {
        this.s3 = s3;
    }

    public SDFS getSdfs() {
        return sdfs;
    }

    public void setSdfs(SDFS sdfs) {
        this.sdfs = sdfs;
    }

    public DB getDb() {
        return db;
    }

    public void setDb(DB db) {
        this.db = db;
    }

    public boolean isClusterMode() {
        return clusterMode;
    }

    public void setClusterMode(boolean clusterMode) {
        this.clusterMode = clusterMode;
    }


    public String getUUID() {
        return UUID;
    }

    public void setUUID(String UUID) {
        this.UUID = UUID;
    }

    public static class S3 {
        @JsonProperty("isCreated")
        private boolean created;
        private String bucketName;

        public S3(String bucketName, boolean created) {
            this.created = created;
            this.bucketName = bucketName;
        }

        public S3() {

        }

        public boolean isCreated() {
            return created;
        }

        public void setCreated(boolean created) {
            this.created = created;
        }

        public String getBucketName() {
            return bucketName;
        }

        public void setBucketName(String bucketName) {
            this.bucketName = bucketName;
        }
    }

    public String getImmutableBucketNamePrefix() {
        return immutableBucketNamePrefix;
    }

    public void setImmutableBucketNamePrefix(String immutableBucketNamePrefix) {
        this.immutableBucketNamePrefix = immutableBucketNamePrefix;
    }

    public static class SDFS {
        @JsonProperty("isCreated")
        private boolean created;
        private String volumeName;
        private String volumeSize;
        private String mountPoint;
        private String minVolumeSize;
        private String maxVolumeSize;
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


        public boolean isCreated() {
            return created;
        }

        public void setCreated(boolean created) {
            this.created = created;
        }

        public String getVolumeName() {
            return volumeName;
        }

        public void setVolumeName(String volumeName) {
            this.volumeName = volumeName;
        }

        public String getVolumeSize() {
            return volumeSize;
        }

        public void setVolumeSize(String volumeSize) {
            this.volumeSize = volumeSize;
        }

        public String getMountPoint() {
            return mountPoint;
        }

        public void setMountPoint(String mountPoint) {
            this.mountPoint = mountPoint;
        }

        public String getMinVolumeSize() {
            return minVolumeSize;
        }

        public void setMinVolumeSize(final String minVolumeSize) {
            this.minVolumeSize = minVolumeSize;
        }

        public String getMaxVolumeSize() {
            return maxVolumeSize;
        }

        public void setMaxVolumeSize(final String maxVolumeSize) {
            this.maxVolumeSize = maxVolumeSize;
        }
    }

    public static class DB {
        @JsonProperty("isValid")
        private boolean valid;

        @JsonProperty("hasAdmin")
        private boolean adminExist;

        public boolean isAdminExist() {
            return adminExist;
        }

        public void setAdminExist(boolean adminExist) {
            this.adminExist = adminExist;
        }

        public boolean isValid() {
            return valid;
        }

        public void setValid(boolean valid) {
            this.valid = valid;
        }
    }
}
