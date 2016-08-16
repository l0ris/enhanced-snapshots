package com.sungardas.enhancedsnapshots.service.impl;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("dev")
public class SDFSStateServiceDev extends SDFSStateServiceImpl {
    private static final org.apache.log4j.Logger LOG = org.apache.log4j.LogManager.getLogger(SDFSStateServiceDev.class);

    public void expandSdfsVolume(String newVolumeSize) {
        System.out.println("Volume expanded to " + newVolumeSize);
    }

    @Override
    public Long getBackupTime() {
        return System.currentTimeMillis();
    }

    @Override
    public void reconfigureAndRestartSDFS() {

    }

    @Override
    public void restoreSDFS() {

    }

    @Override
    public void startSDFS() {

    }

    @Override
    public void stopSDFS() {

    }

    @Override
    public boolean sdfsIsAvailable() {
        return true;
    }

}
