package com.sungardas.enhancedsnapshots.enumeration;

public enum BackupProgress {
    STARTED,
    CREATING_SNAPSHOT,
    CREATING_TEMP_VOLUME,
    ATTACHING_VOLUME,
    COPYING,
    DETACHING_TEMP_VOLUME,
    DELETING_TEMP_VOLUME,
    CLEANING_TEMP_RESOURCES,
    FAIL_CLEANING
}
