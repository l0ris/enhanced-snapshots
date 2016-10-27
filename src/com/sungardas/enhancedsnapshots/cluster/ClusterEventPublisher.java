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
    void nodeLaunched(String nodeId, long volumeId, String msgId);

    /**
     * Publish node terminated event
     * @param nodeId id of terminated node
     * @param msgId SQS message id, null in case event was not received from SQS
     */
    void nodeTerminated(String nodeId, String msgId);

    /**
     * Logs watcher started event
     */
    void logWatcherStarted();

    /**
     * Logs watcher stopped event
     */
    void logWatcherStopped();
}
