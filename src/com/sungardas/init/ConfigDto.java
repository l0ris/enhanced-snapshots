package com.sungardas.init;

import com.sungardas.enhancedsnapshots.aws.dynamodb.model.User;
import com.sungardas.enhancedsnapshots.dto.Cluster;
import com.sungardas.enhancedsnapshots.dto.MailConfigurationDto;

class ConfigDto {
    private User user;
    private String bucketName;
    private int volumeSize;
    private boolean ssoMode;
    private boolean sungardasSSO;
    private String spEntityId;
    private String domain;
    private MailConfigurationDto mailConfiguration;

    private Cluster cluster;

    public int getVolumeSize() {
        return volumeSize;
    }

    public void setVolumeSize(final int volumeSize) {
        this.volumeSize = volumeSize;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public boolean isSsoMode() {
        return ssoMode;
    }

    public void setSsoMode(boolean ssoMode) {
        this.ssoMode = ssoMode;
    }

    public String getSpEntityId() {
        return spEntityId;
    }

    public void setSpEntityId(String spEntityId) {
        this.spEntityId = spEntityId;
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

    public Cluster getCluster() {
        return cluster;
    }

    public void setCluster(Cluster cluster) {
        this.cluster = cluster;
    }


    public boolean isSungardasSSO() {
        return sungardasSSO;
    }

    public void setSungardasSSO(boolean sungardasSSO) {
        this.sungardasSSO = sungardasSSO;
    }
}
