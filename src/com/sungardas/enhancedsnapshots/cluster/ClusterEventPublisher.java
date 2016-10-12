package com.sungardas.enhancedsnapshots.cluster;


public interface ClusterEventPublisher {

    void settingsUpdated();

    void nodeLaunched(String nodeId, String volumeId);

    void nodeTerminated(String nodeId);
}
