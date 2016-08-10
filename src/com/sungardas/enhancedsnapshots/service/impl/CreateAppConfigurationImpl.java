package com.sungardas.enhancedsnapshots.service.impl;


import com.amazonaws.services.s3.AmazonS3;
import com.sungardas.enhancedsnapshots.components.ConfigurationMediator;
import com.sungardas.enhancedsnapshots.service.AWSCommunicationService;
import com.sungardas.enhancedsnapshots.service.SDFSStateService;
import com.sungardas.enhancedsnapshots.service.SystemService;
import com.sungardas.enhancedsnapshots.util.EnhancedSnapshotSystemMetadataUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;


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
            if (EnhancedSnapshotSystemMetadataUtil.isBucketExits(configurationMediator.getS3Bucket(), amazonS3)) {
                isBucketContainsSDFSMetadata = EnhancedSnapshotSystemMetadataUtil.containsSdfsMetadata(configurationMediator.getS3Bucket(),
                        configurationMediator.getSdfsBackupFileName(), amazonS3);
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
}
