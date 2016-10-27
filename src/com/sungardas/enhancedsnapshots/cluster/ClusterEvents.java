package com.sungardas.enhancedsnapshots.cluster;


public enum ClusterEvents {
    NODE_LAUNCHED("nodeLaunched"), NODE_TERMINATED("nodeTerminated"), SETTINGS_UPDATED("settingsUpdated"),
    LOGS_WATCHER_STARTED("logsWatcherStarted"),  LOGS_WATCHER_STOPPED("logsWatcherStopped");

    public String getEvent() {
        return event;
    }

    private final String event;

    ClusterEvents(String event) {
        this.event = event;
    }
}