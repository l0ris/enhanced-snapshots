package com.sungardas.enhancedsnapshots.service;

import com.sungardas.enhancedsnapshots.dto.SystemConfiguration;

import javax.annotation.security.RolesAllowed;

/**
 * Enhancedsnapshots system service interface main responsibilities: System backup & restore and configuration
 */
public interface SystemService {

    /**
     * Backup current system state to S3 bucket Backup data are: -DynamoDB tables -Property file -nginx certificates
     */
    void backup(String taskId);

    /**
     * Get current system configuration from DB
     *
     * @return system configuration {@link SystemConfiguration}
     */
    SystemConfiguration getSystemConfiguration();

    /**
     * set new system configuration
     *
     * @param systemConfiguration new system configuration {@link SystemConfiguration}
     */
    void setSystemConfiguration(SystemConfiguration systemConfiguration);

    /**
     * Uninstall system. Removes all system infrastructure: DB tables, instance with application
     *
     * @param removeS3Bucket in case true S3 bucket will be removed as well
     */
    @RolesAllowed("ROLE_ADMIN")
    void systemUninstall(boolean removeS3Bucket);

    String VOLUME_SIZE_UNIT = "GB";

    /**
     * synchronize system settings with DB
     */
    void refreshSystemConfiguration();
}
