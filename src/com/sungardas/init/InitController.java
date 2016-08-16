package com.sungardas.init;

import com.amazonaws.AmazonClientException;
import com.sungardas.enhancedsnapshots.aws.dynamodb.model.User;
import com.sungardas.enhancedsnapshots.aws.dynamodb.repository.UserRepository;
import com.sungardas.enhancedsnapshots.dto.InitConfigurationDto;
import com.sungardas.enhancedsnapshots.dto.converter.BucketNameValidationDTO;
import com.sungardas.enhancedsnapshots.exception.ConfigurationException;
import com.sungardas.enhancedsnapshots.exception.EnhancedSnapshotsException;
import com.sungardas.enhancedsnapshots.rest.RestAuthenticationFilter;
import com.sungardas.enhancedsnapshots.rest.filters.FilterProxy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.support.XmlWebApplicationContext;

import javax.annotation.PostConstruct;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;


@RestController
class InitController implements ApplicationContextAware {

    private static final Logger LOG = LogManager.getLogger(InitController.class);

    @Autowired
    private FilterProxy filterProxy;

    @Autowired
    private InitConfigurationService initConfigurationService;
    private InitConfigurationDto configurationDto;

    @Autowired
    private XmlWebApplicationContext applicationContext;

    private boolean CONTEXT_REFRESH_IN_PROCESS = false;

    @PostConstruct
    private void init() {
        // check that aws credentials are provided
        // try to authenticate as real admin user
        if (initConfigurationService.propertyFileExists()) {
            LOG.info("System is already configured.");
            initConfigurationService.syncSettingsInDbAndConfigFile();
            refreshContext();
        } else {
            initConfigurationService.configureAWSLogAgent();
        }
    }

    @ExceptionHandler(value = {EnhancedSnapshotsException.class, ConfigurationException.class})
    @ResponseBody
    @ResponseStatus(INTERNAL_SERVER_ERROR)
    private Exception internalServerError(Exception exception) {
        LOG.error(exception);
        return exception;
    }

    //TODO: This should be removed, for dev mode only
    @RequestMapping(value = "/configuration/awscreds", method = RequestMethod.GET)
    public ResponseEntity<String> getAwsCredentialsInfo() {
        return new ResponseEntity<>("{\"contains\": true}", HttpStatus.OK);
    }

    @ExceptionHandler(value = {Exception.class, AmazonClientException.class})
    @ResponseBody
    @ResponseStatus(INTERNAL_SERVER_ERROR)
    private Exception amazonException(Exception exception) {
        LOG.error(exception);
        return new EnhancedSnapshotsException("Internal server error", exception);
    }

    @RequestMapping(method = RequestMethod.POST, value = "/session")
    public ResponseEntity<String> init(@RequestBody User user) {
        if (CONTEXT_REFRESH_IN_PROCESS) {
            return new ResponseEntity<>("", HttpStatus.NOT_FOUND);
        }
        // no aws credentials are provided
        // try to authenticate as default user admin@enhancedsnapshots:<instance-id>
        else if (initConfigurationService.checkDefaultUser(user.getEmail(), user.getPassword())) {
            return new ResponseEntity<>("{ \"role\":\"configurator\" }", HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
    }

    @RequestMapping(value = "/configuration/current", method = RequestMethod.GET)
    public ResponseEntity<InitConfigurationDto> getConfiguration() {
        return new ResponseEntity<>(getInitConfigurationDTO(), HttpStatus.OK);
    }


    @RequestMapping(value = "/configuration/current", method = RequestMethod.POST)
    public ResponseEntity<String> setConfiguration(@RequestBody ConfigDto config) {
        InitConfigurationDto initConfigurationDto = getInitConfigurationDTO();
        if (!initConfigurationDto.getDb().isValid()) {
            initConfigurationService.setUser(config.getUser());
        }
        if (config.getUser() != null) {
            initConfigurationService.setUser(config.getUser());
        }
        initConfigurationService.validateVolumeSize(config.getVolumeSize());
        try {
            // we need to ensure before context refresh that provided bucket name is valid
            initConfigurationService.createBucket(config.getBucketName());
        } catch (IllegalArgumentException e) {
            LOG.warn("Failed to create bucket {}", config.getBucketName());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.UNPROCESSABLE_ENTITY);
        }
        initConfigurationService.storePropertiesEditableFromConfigFile();
        initConfigurationService.createDBAndStoreSettings(config);
        try {
            refreshContext();
        } catch (Exception e) {
            initConfigurationService.removeProperties();
            throw e;
        }
        return new ResponseEntity<>("", HttpStatus.OK);
    }

    @RequestMapping(value = "/configuration/bucket/{name:.+}", method = RequestMethod.GET)
    public ResponseEntity<BucketNameValidationDTO> validateBucketName(@PathVariable("name") String bucketName) {
        return new ResponseEntity<>(initConfigurationService.validateBucketName(bucketName), HttpStatus.OK);
    }

    @RequestMapping(value = "/configuration/bucket/{name:.+}/metadata", method = RequestMethod.GET)
    public ResponseEntity<BucketNameValidationDTO> containsMetadata(@PathVariable("name") String bucketName) {
        return new ResponseEntity(initConfigurationService.containsMetadata(bucketName), HttpStatus.OK);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = (XmlWebApplicationContext) applicationContext;
    }

    private void refreshContext() {
        LOG.info("Context refresh process started.");
        CONTEXT_REFRESH_IN_PROCESS = true;
        applicationContext.setConfigLocation("/WEB-INF/spring-web-config.xml");
        applicationContext.refresh();

        // enabling auth filter
        RestAuthenticationFilter filter = applicationContext.getBean(RestAuthenticationFilter.class);
        filter.setUserRepository(applicationContext.getBean(UserRepository.class));
        filter.setInstanceId(initConfigurationService.getInstanceId());
        filterProxy.setFilter(filter);

        LOG.info("Context refreshed successfully.");
        CONTEXT_REFRESH_IN_PROCESS = false;
    }

    static class ConfigDto {
        private User user;
        private String bucketName;
        private int volumeSize;

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
    }

    private InitConfigurationDto getInitConfigurationDTO() {
        if (configurationDto == null) {
            configurationDto = initConfigurationService.getInitConfigurationDto();
        }
        return configurationDto;
    }
}
