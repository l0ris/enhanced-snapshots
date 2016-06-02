package com.sungardas.enhancedsnapshots.service.upgrade;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import com.amazonaws.util.EC2MetadataUtils;
import com.sungardas.enhancedsnapshots.aws.dynamodb.model.BackupEntry;
import com.sungardas.enhancedsnapshots.aws.dynamodb.model.BackupState;
import com.sungardas.enhancedsnapshots.components.impl.ConfigurationMediatorImpl;
import com.sungardas.enhancedsnapshots.service.SDFSStateService;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class UpgradeSystemTo002 implements SystemUpgrade {

    private static final Logger LOG = LogManager.getLogger(UpgradeSystemTo002.class);
    @Value("${enhancedsnapshots.default.sdfs.mount.point}")
    private String mountPoint;
    private static final String upgradeVersion = "0.0.2";

    // do not move to property file
    private static final String sdfsSystemBackupArchive = "sdfsstate.zip";

    private final ObjectMapper objectMapper = new ObjectMapper();
    @Autowired
    private SDFSStateService sdfsStateService;
    @Autowired
    private ConfigurationMediatorImpl configurationMediator;


    @Override
    public void upgrade(Path tempFolder, String initVersion) {
        try {
            if (stringVersionToInt(initVersion) >= stringVersionToInt(upgradeVersion)) {
                LOG.info("No need to upgrade to {}", upgradeVersion);
                return;
            }
            LOG.info("Upgrading system to version {}", upgradeVersion);
            File destForBackups = Paths.get(tempFolder.toString(), BackupEntry.class.getName()).toFile();
            restoreFile(tempFolder, Paths.get(configurationMediator.getSdfsConfigPath()));
            sdfsStateService.restoreSDFS();
            objectMapper.writeValue(destForBackups, sdfsStateService.getBackupsFromSDFSMountPoint());
        } catch (Exception e) {
            LOG.error("Failed to upgrade system: ", e);
        }
        finally {
            sdfsStateService.stopSDFS();
        }
    }


    protected int stringVersionToInt(String version){
       return Integer.parseInt(version.replace(".", ""));
    }



    protected String getInstanceId() {
        return EC2MetadataUtils.getInstanceId();
    }

    private void restoreFile(Path tempDirectory, Path destPath) throws IOException {
        Path fileName = destPath.getFileName();
        Files.copy(Paths.get(tempDirectory.toString(), fileName.toString()), destPath, StandardCopyOption.REPLACE_EXISTING);
    }

}
