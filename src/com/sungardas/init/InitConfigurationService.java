package com.sungardas.init;

import com.sungardas.enhancedsnapshots.dto.InitConfigurationDto;
import com.sungardas.enhancedsnapshots.dto.converter.BucketNameValidationDTO;
import org.springframework.web.multipart.MultipartFile;


interface InitConfigurationService {

    InitConfigurationDto getInitConfigurationDto();

    boolean systemIsConfigured();

    boolean checkDefaultUser(String login, String password);

    void configureSystem(ConfigDto configDto);

    BucketNameValidationDTO validateBucketName(String bucketName);

    /**
     * Check if it is possible to restore DB from S3 bucket
     *
     * @param bucketName bucket name
     * @return information about DB
     */
    InitConfigurationDto.DB containsMetadata(String bucketName);

    /**
     * Validate and convert SAML 2.0 related files
     *
     * @param spCertificate Service provider certificate (pem file)
     * @param idpMetadata   Identity provider metadata (xml file)
     */
    void saveAndProcessSAMLFiles(MultipartFile spCertificate, MultipartFile idpMetadata);

}
