package com.sungardas.init;

import com.amazonaws.AmazonClientException;
import com.sungardas.enhancedsnapshots.aws.dynamodb.model.User;
import com.sungardas.enhancedsnapshots.dto.InitConfigurationDto;
import com.sungardas.enhancedsnapshots.dto.MailConfigurationTestDto;
import com.sungardas.enhancedsnapshots.dto.converter.BucketNameValidationDTO;
import com.sungardas.enhancedsnapshots.exception.ConfigurationException;
import com.sungardas.enhancedsnapshots.exception.EnhancedSnapshotsException;
import com.sungardas.enhancedsnapshots.service.MailService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;


@RestController
class InitController {

    private static final Logger LOG = LogManager.getLogger(InitController.class);

    private String idpMetadata = "idp_metadata.xml";
    private String samlCertPem = "saml_sp_cert.pem";
    @Autowired
    private InitConfigurationService initConfigurationService;
    @Autowired
    private ContextManager contextManager;
    private InitConfigurationDto configurationDto;

    @Autowired
    private MailService mailService;

    @PostConstruct
    private void init() {
        // check that aws credentials are provided
        // try to authenticate as real admin user
        if (initConfigurationService.systemIsConfigured()) {
            LOG.info("System is already configured.");
            initConfigurationService.configureSystem(null);
        }
    }

    @ExceptionHandler(value = {EnhancedSnapshotsException.class, ConfigurationException.class})
    @ResponseBody
    @ResponseStatus(INTERNAL_SERVER_ERROR)
    private Exception internalServerError(Exception exception) {
        LOG.error(exception);
        return exception;
    }

    @ExceptionHandler(value = IllegalArgumentException.class)
    @ResponseBody
    @ResponseStatus(UNPROCESSABLE_ENTITY)
    private Exception illegalArg(IllegalArgumentException exception) {
        LOG.error(exception);
        return exception;
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
        if (contextManager.contextRefreshInProcess()) {
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
        initConfigurationService.configureSystem(config);
        return new ResponseEntity<>("", HttpStatus.OK);
    }


    @RequestMapping(value = "/system/mail/configuration/test", method = RequestMethod.POST)
    public ResponseEntity mailConfigurationTest(@RequestBody MailConfigurationTestDto dto) {
        mailService.testConfiguration(dto.getMailConfiguration(), dto.getTestEmail(), dto.getDomain());
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * Upload idp metadata & saml sp certificate
     */
    @RequestMapping(value = "/configuration/uploadFiles", method = RequestMethod.POST)
    public
    @ResponseBody
    ResponseEntity<String> uploadSSOFiles(@RequestParam("name") String name[],
                                          @RequestParam("file") MultipartFile[] file) throws Exception {
        if (name.length != 2 && file.length != name.length) {
            return new ResponseEntity<>("Failed to upload files. Saml certificate and IDP metadata should be provided", HttpStatus.BAD_REQUEST);
        }
        if(name[0].equals(idpMetadata) && name[1].equals(samlCertPem)){
            initConfigurationService.saveAndProcessSAMLFiles(file[1], file[0]);
        } else if (name[0].equals(samlCertPem) && name[0].equals(idpMetadata)) {
            initConfigurationService.saveAndProcessSAMLFiles(file[0], file[1]);
        } else{
            return new ResponseEntity<>("Failed to upload SAML files. Saml certificate and IDP metadata should be provided", HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>("File uploaded successfully", HttpStatus.OK);
    }

    @RequestMapping(value = "/configuration/bucket/{name:.+}", method = RequestMethod.GET)
    public ResponseEntity<BucketNameValidationDTO> validateBucketName(@PathVariable("name") String bucketName) {
        return new ResponseEntity<>(initConfigurationService.validateBucketName(bucketName), HttpStatus.OK);
    }

    @RequestMapping(value = "/configuration/bucket/{name:.+}/metadata", method = RequestMethod.GET)
    public ResponseEntity<BucketNameValidationDTO> containsMetadata(@PathVariable("name") String bucketName) {
        return new ResponseEntity(initConfigurationService.containsMetadata(bucketName), HttpStatus.OK);
    }

    private InitConfigurationDto getInitConfigurationDTO() {
        if (configurationDto == null) {
            configurationDto = initConfigurationService.getInitConfigurationDto();
        }
        return configurationDto;
    }
}
