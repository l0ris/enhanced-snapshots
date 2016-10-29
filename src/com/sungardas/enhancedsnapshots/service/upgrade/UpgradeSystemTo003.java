package com.sungardas.enhancedsnapshots.service.upgrade;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;

public class UpgradeSystemTo003 extends UpgradeSystemTo002 {


    private static final Logger LOG = LogManager.getLogger(UpgradeSystemTo003.class);
    private static final String upgradeVersion = "0.0.3";


    //TODO: no special logic required any more, consider to remove this class
    @Override
    public void upgrade(Path tempFolder, String initVersion) {
        if (stringVersionToInt(initVersion) >= stringVersionToInt(upgradeVersion)) {
            return;
        }
        LOG.info("Upgrading system to version {}", upgradeVersion);
    }


    protected int stringVersionToInt(String version){
        return Integer.parseInt(version.replace(".", ""));
    }




}
