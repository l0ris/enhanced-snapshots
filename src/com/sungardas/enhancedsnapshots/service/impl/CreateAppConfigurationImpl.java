package com.sungardas.enhancedsnapshots.service.impl;


import com.amazonaws.services.s3.AmazonS3;
import com.sungardas.enhancedsnapshots.aws.dynamodb.model.BackupEntry;
import com.sungardas.enhancedsnapshots.aws.dynamodb.repository.BackupRepository;
import com.sungardas.enhancedsnapshots.components.ConfigurationMediator;
import com.sungardas.enhancedsnapshots.service.AWSCommunicationService;
import com.sungardas.enhancedsnapshots.service.SDFSStateService;
import com.sungardas.enhancedsnapshots.util.EnhancedSnapshotSystemMetadataUtil;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;


import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;


@Service("CreateAppConfiguration")
@Profile("prod")
class CreateAppConfigurationImpl {
    private static final Logger LOG = LogManager.getLogger(CreateAppConfigurationImpl.class);

    @Autowired
    private SDFSStateService sdfsService;

    @Autowired
    private ConfigurationMediator configurationMediator;

    @Autowired
    private SDFSStateService sdfsStateService;

    @Autowired
    private AmazonS3 amazonS3;

    @Autowired
    private BackupRepository backupRepository;

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
                LOG.info("Restoring SDFS state");
                sdfsService.restoreSDFS();
                syncBackupsInDBWithExistingOnes();
            } else {
                LOG.info("Starting SDFS");
                sdfsService.startSDFS();
            }
            LOG.info("Initialization finished");
        }
    }


    /**
     * There can be situations when user removed/added backups after system backup and than decided
     * to migrate to another instance, in this case backups will not be in consistent state
     */
    private void syncBackupsInDBWithExistingOnes() {
        List<BackupEntry> realBackups = sdfsStateService.getBackupsFromSDFSMountPoint();
        List<BackupEntry> backupEntries = backupRepository.findAll();

        // removing non existing backups from DB
        List<BackupEntry> toRemove =  new ArrayList<>();
        backupEntries.stream().filter(b -> !realBackups.contains(b))
                .peek(b -> LOG.info("Backup {} of volume {} does not exist any more. Removing related data from DB", b.getFileName(), b.getVolumeId()))
                .forEach(toRemove::add);
        backupRepository.delete(toRemove);

        // adding backups which are not stored in DB
        List<BackupEntry> toAdd =  new ArrayList<>();
        realBackups.stream().filter(rb -> !backupEntries.contains(rb))
                .peek(rb -> LOG.info("Adding backup {} info to DB", rb.getFileName())).forEach(toAdd::add);
        backupRepository.save(toAdd);
    }
}
