package com.sungardas.enhancedsnapshots.cluster;


public interface ClusterEventPublisher {

    /**
     * Publish settings updated event
     */
    void settingsUpdated();

    /**
     * Publish node launched event
     * @param nodeId id of terminated node
     * @param volumeId SDFS volume id of launched node
     * @param msgId SQS message id, null in case event was not received from SQS
     */
    void nodeLaunched(String nodeId, String volumeId, String msgId);

    /**
     * Publish node terminated event
     * @param nodeId id of terminated node
     * @param msgId SQS message id, null in case event was not received from SQS
     */
    void nodeTerminated(String nodeId, String msgId);
}
