package com.sungardas.enhancedsnapshots.service.impl;


import javax.annotation.PostConstruct;

import com.amazonaws.services.s3.AmazonS3;
import com.sungardas.enhancedsnapshots.components.ConfigurationMediator;
import com.sungardas.enhancedsnapshots.service.AWSCommunicationService;
import com.sungardas.enhancedsnapshots.service.SDFSStateService;
import com.sungardas.enhancedsnapshots.service.SystemService;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;


class CreateAppConfigurationImpl {
    private static final Logger LOG = LogManager.getLogger(CreateAppConfigurationImpl.class);

    @Autowired
    private SDFSStateService sdfsService;

    @Autowired
    private ConfigurationMediator configurationMediator;

    @Autowired
    private SystemService systemService;

    @Autowired
    private AmazonS3 amazonS3;

    @Autowired
    private AWSCommunicationService awsCommunicationService;

    private boolean init = false;

    @PostConstruct
    private void init() {
        awsCommunicationService.restartAWSLogService();
        if (!init) {
            LOG.info("Initialization started");
            init = true;
            boolean isBucketContainsSDFSMetadata = false;
            if (isBucketExits(configurationMediator.getS3Bucket())) {
                isBucketContainsSDFSMetadata = sdfsService.containsSdfsMetadata(configurationMediator.getS3Bucket());
            }
            LOG.info("Initialization restore");
            if (isBucketContainsSDFSMetadata) {
                LOG.info("Restoring from backup");
                systemService.restore();
            } else {
                LOG.info("Starting SDFS");
                sdfsService.startSDFS();
            }
            LOG.info("Initialization finished");
        }
    }

    private boolean isBucketExits(String s3Bucket) {
        try {
            String location = amazonS3.getBucketLocation(s3Bucket);
            return location != null;
        } catch (Exception e) {
            return false;
        }
    }
}
