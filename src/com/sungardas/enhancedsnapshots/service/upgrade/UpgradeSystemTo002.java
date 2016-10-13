package com.sungardas.enhancedsnapshots.service.upgrade;

import java.nio.file.Path;


import com.amazonaws.util.EC2MetadataUtils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class UpgradeSystemTo002 implements SystemUpgrade {

    private static final Logger LOG = LogManager.getLogger(UpgradeSystemTo002.class);
    private static final String upgradeVersion = "0.0.2";


    //TODO: no special logic required any more, consider to remove this class
    @Override
    public void upgrade(Path tempFolder, String initVersion) {
        if (stringVersionToInt(initVersion) >= stringVersionToInt(upgradeVersion)) {
            LOG.info("No need to upgrade to {}", upgradeVersion);
            return;
        }
        LOG.info("Upgrading system to version {}", upgradeVersion);
    }


    protected int stringVersionToInt(String version){
       return Integer.parseInt(version.replace(".", ""));
    }

}
