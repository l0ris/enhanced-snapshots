package com.sungardas.enhancedsnapshots.service.upgrade;


import java.nio.file.Path;

public class UpgradeSystemTo003 extends UpgradeSystemTo002 {

    private static final String currentInitVersion = "0.0.2";

    /**
     * This is an example for future update logic
     */
    @Override
    public void upgrade(Path tempFolder, String initVersion) {
        if (stringVersionToInt(initVersion) < stringVersionToInt(currentInitVersion)) {
            super.upgrade(tempFolder, initVersion);
        }
        // further upgrade logic

    }



}
