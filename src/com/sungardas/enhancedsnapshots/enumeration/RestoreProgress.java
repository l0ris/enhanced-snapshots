package com.sungardas.enhancedsnapshots.enumeration;

public enum RestoreProgress {
    STARTED,
    RESTORE_FROM_SNAPSHOT,
    RESTORE_FROM_FILE,
    CREATING_VOLUME,
    ATTACHING_VOLUME,
    COPYING,
    DETACHING_VOLUME,
    MOVE_TO_TARGET_ZONE
}
