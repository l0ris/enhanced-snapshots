package com.sungardas.enhancedsnapshots.rest;

import com.sungardas.enhancedsnapshots.components.ConfigurationMediator;
import com.sungardas.enhancedsnapshots.dto.SystemConfiguration;
import com.sungardas.enhancedsnapshots.rest.filters.FilterProxy;
import com.sungardas.enhancedsnapshots.rest.utils.Constants;
import com.sungardas.enhancedsnapshots.service.SDFSStateService;
import com.sungardas.enhancedsnapshots.service.SystemService;
import com.sungardas.enhancedsnapshots.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import java.util.Map;


@RestController
@RequestMapping("/system")
public class SystemController {
    @Autowired
    private FilterProxy filterProxy;

    @Autowired
    private HttpServletRequest servletRequest;

    @Autowired
    private ServletContext context;

    @Autowired
    private SDFSStateService sdfsStateService;

    @Autowired
    private SystemService systemService;

    @Autowired
    private UserService userService;

    @Autowired
    private ConfigurationMediator configurationMediator;

    @RequestMapping(value = "/delete", method = RequestMethod.POST)
    public ResponseEntity<String> deleteService(@RequestBody RemoveAppDTO removeAppDTO) {
        String session = servletRequest.getSession().getId();
        String currentUser = ((Map<String, String>) context.getAttribute(Constants.CONTEXT_ALLOWED_SESSIONS_ATR_NAME)).get(session);
        if (!userService.isAdmin(currentUser)) {
            return new ResponseEntity<>("{\"msg\":\"Only admin can delete service\"}", HttpStatus.FORBIDDEN);
        }
        if (!configurationMediator.getConfigurationId().equals(removeAppDTO.getInstanceId())) {
            return new ResponseEntity<>("{\"msg\":\"Provided instance ID is incorrect\"}", HttpStatus.FORBIDDEN);
        }
        filterProxy.setFilter(null);
        systemService.systemUninstall(removeAppDTO.removeS3Bucket);
        return new ResponseEntity<>("", HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<SystemConfiguration> getSystem() {
        return new ResponseEntity<>(systemService.getSystemConfiguration(), HttpStatus.OK);
    }

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
        if (newConfiguration.getSystemProperties().getTaskHistoryTTL() <= 0) {
            return new ResponseEntity<>("Task history TTL should be greater than 0", HttpStatus.BAD_REQUEST);
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

    @RequestMapping(value = "/backup", method = RequestMethod.GET)
    public ResponseEntity<SystemBackupDto> getConfiguration() {
        return new ResponseEntity<>(new SystemBackupDto(sdfsStateService.getBackupTime()), HttpStatus.OK);
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

        private String instanceId;
        private boolean removeS3Bucket;

        public boolean isRemoveS3Bucket() {
            return removeS3Bucket;
        }

        public void setRemoveS3Bucket(boolean removeS3Bucket) {
            this.removeS3Bucket = removeS3Bucket;
        }

        public String getInstanceId() {
            return instanceId;
        }

        public void setInstanceId(String instanceId) {
            this.instanceId = instanceId;
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
