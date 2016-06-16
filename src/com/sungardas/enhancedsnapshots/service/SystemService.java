package com.sungardas.enhancedsnapshots.service;

import com.sungardas.enhancedsnapshots.dto.SystemConfiguration;

/**
 * Enhancedsnapshots system service interface main responsibilities: System backup & restore and configuration
 */
public interface SystemService {

    /**
     * Backup current system state to S3 bucket Backup data are: -DynamoDB tables -Property file -nginx certificates
     */
    void backup(String taskId);

    /**
     * Restore system state from backup {@link #backup(String)}
     */
    void restore();

    /**
     * Get current system configuration from DB
     *
     * @return system configuration {@link SystemConfiguration}
     */
    SystemConfiguration getSystemConfiguration();

    /**
     * set new system configuration
     * @param systemConfiguration new system configuration {@link SystemConfiguration}
     */
    void setSystemConfiguration(SystemConfiguration systemConfiguration);

    /**
     * Uninstall system. Removes all system infrastructure: DB tables, instance with application
     *
     * @param removeS3Bucket in case true S3 bucket will be removed as well
     */
    void systemUninstall(boolean removeS3Bucket);

    String VOLUME_SIZE_UNIT = "GB";
}
