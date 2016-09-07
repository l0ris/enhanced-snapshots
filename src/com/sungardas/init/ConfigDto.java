package com.sungardas.init;

import com.sungardas.enhancedsnapshots.aws.dynamodb.model.User;
import com.sungardas.enhancedsnapshots.dto.MailConfigurationDto;

class ConfigDto {
    private User user;
    private String bucketName;
    private int volumeSize;
    private boolean ssoMode;
    private String spEntityId;
    private MailConfigurationDto mailConfiguration;

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

    public MailConfigurationDto getMailConfiguration() {
        return mailConfiguration;
    }

    public void setMailConfiguration(MailConfigurationDto mailConfiguration) {
        this.mailConfiguration = mailConfiguration;
    }
}
