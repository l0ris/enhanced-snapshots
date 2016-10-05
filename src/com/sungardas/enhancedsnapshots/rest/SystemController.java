package com.sungardas.enhancedsnapshots.rest;

import com.sungardas.enhancedsnapshots.components.ConfigurationMediator;
import com.sungardas.enhancedsnapshots.dto.MailConfigurationTestDto;
import com.sungardas.enhancedsnapshots.dto.SystemConfiguration;
import com.sungardas.enhancedsnapshots.service.MailService;
import com.sungardas.enhancedsnapshots.service.SDFSStateService;
import com.sungardas.enhancedsnapshots.service.SystemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.security.RolesAllowed;


@RestController
@RequestMapping("/system")
public class SystemController {

    @Autowired
    private SDFSStateService sdfsStateService;

    @Autowired
    private SystemService systemService;

    @Autowired
    private MailService mailService;

    @Autowired
    private ConfigurationMediator configurationMediator;

    @RolesAllowed("ROLE_ADMIN")
    @RequestMapping(value = "/delete", method = RequestMethod.POST)
    public ResponseEntity<String> deleteService(@RequestBody RemoveAppDTO removeAppDTO) {
        if (!configurationMediator.getConfigurationId().equals(removeAppDTO.getSystemId())) {
            return new ResponseEntity<>("{\"msg\":\"Provided instance ID is incorrect\"}", HttpStatus.FORBIDDEN);
        }
        systemService.systemUninstall(removeAppDTO.removeS3Bucket);
        return new ResponseEntity<>("", HttpStatus.OK);
    }

    @RolesAllowed({"ROLE_ADMIN", "ROLE_USER"})
    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<SystemConfiguration> getSystem() {
        return new ResponseEntity<>(systemService.getSystemConfiguration(), HttpStatus.OK);
    }

    @RolesAllowed({"ROLE_ADMIN", "ROLE_USER"})
    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<String> updateSystemProperties(@RequestBody SystemConfiguration newConfiguration) {
        SystemConfiguration currentConfiguration = systemService.getSystemConfiguration();
        if (!checkIopsAreValid(newConfiguration.getSystemProperties())) {
            return new ResponseEntity<>("iops per GB can not be less than 1 and more than 30", HttpStatus.BAD_REQUEST);
        }
        if (newConfiguration.getSdfs().getVolumeSize() > currentConfiguration.getSdfs().getMaxVolumeSize()) {
            return new ResponseEntity<>("Volume size can not be more than " + currentConfiguration.getSdfs().getMaxVolumeSize(), HttpStatus.BAD_REQUEST);
        }
        if (newConfiguration.getSdfs().getSdfsLocalCacheSize() > currentConfiguration.getSdfs().getMaxSdfsLocalCacheSize()) {
            return new ResponseEntity<>("Local cache size can not be more than " + currentConfiguration.getSdfs().getMaxSdfsLocalCacheSize(), HttpStatus.BAD_REQUEST);
        }
        if (newConfiguration.getSystemProperties().getTaskHistoryTTS() <= 0) {
            return new ResponseEntity<>("Task history TTS should be greater than 0", HttpStatus.BAD_REQUEST);
        }
        boolean needToReconfigureSdfs = false;

        if (configurationMediator.getSdfsVolumeSizeWithoutMeasureUnit() != newConfiguration.getSdfs().getVolumeSize()
                && newConfiguration.getSdfs().getVolumeSize() > 0) {
            sdfsStateService.expandSdfsVolume(newConfiguration.getSdfs().getVolumeSize() + configurationMediator.getVolumeSizeUnit());
        }
        if (configurationMediator.getSdfsLocalCacheSizeWithoutMeasureUnit() != newConfiguration.getSdfs().getSdfsLocalCacheSize()
                && newConfiguration.getSdfs().getSdfsLocalCacheSize() > 0) {
            needToReconfigureSdfs = true;
        }
        systemService.setSystemConfiguration(newConfiguration);
        if (needToReconfigureSdfs) {
            sdfsStateService.reconfigureAndRestartSDFS();
        }
        return new ResponseEntity<>("", HttpStatus.OK);
    }

    @RolesAllowed({"ROLE_ADMIN", "ROLE_USER"})
    @RequestMapping(value = "/backup", method = RequestMethod.GET)
    public ResponseEntity<SystemBackupDto> getConfiguration() {
        return new ResponseEntity<>(new SystemBackupDto(sdfsStateService.getBackupTime()), HttpStatus.OK);
    }

    @RolesAllowed({"ROLE_ADMIN", "ROLE_USER"})
    @RequestMapping(value = "/mail/configuration/test", method = RequestMethod.POST)
    public ResponseEntity mailConfigurationTest(@RequestBody MailConfigurationTestDto dto) {
        mailService.testConfiguration(dto.getMailConfiguration(), dto.getTestEmail(), dto.getDomain());
        return new ResponseEntity<>(HttpStatus.OK);
    }

    private static class SystemBackupDto {
        private Long lastBackup;

        public SystemBackupDto(Long lastBackup) {
            this.lastBackup = lastBackup;
        }

        public Long getLastBackup() {
            return lastBackup;
        }

        public void setLastBackup(Long lastBackup) {
            this.lastBackup = lastBackup;
        }
    }

    private static class RemoveAppDTO {

        private String systemId;
        private boolean removeS3Bucket;

        public boolean isRemoveS3Bucket() {
            return removeS3Bucket;
        }

        public void setRemoveS3Bucket(boolean removeS3Bucket) {
            this.removeS3Bucket = removeS3Bucket;
        }

        public String getSystemId() {
            return systemId;
        }

        public void setSystemId(String instanceId) {
            this.systemId = instanceId;
        }
    }

    // iops per GB can not be less than 1 and more than 30
    private boolean checkIopsAreValid(SystemConfiguration.SystemProperties systemProperties) {
        boolean result = true;
        if (systemProperties.getRestoreVolumeIopsPerGb() > 30 || systemProperties.getTempVolumeIopsPerGb() > 30) {
            result = false;
        }
        if (systemProperties.getRestoreVolumeIopsPerGb() < 1 || systemProperties.getTempVolumeIopsPerGb() < 1) {
            result = false;
        }
        return result;
    }
}
