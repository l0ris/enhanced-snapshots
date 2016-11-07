package com.sungardas.enhancedsnapshots.service;

public interface MasterService {
    void init();

    String getMasterId();

    String getNodeHostname(String instanceId);

    String getMasterHostname();

    boolean isClusterMode();
}
