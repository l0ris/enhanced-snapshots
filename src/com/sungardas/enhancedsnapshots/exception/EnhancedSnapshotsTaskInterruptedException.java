package com.sungardas.enhancedsnapshots.exception;

/**
 * {@code EnhancedSnapshotsTaskInterruptedException} is exception that can be thrown during the task interruption
 */
public class EnhancedSnapshotsTaskInterruptedException extends EnhancedSnapshotsInterruptedException {
    public EnhancedSnapshotsTaskInterruptedException() {
    }

    public EnhancedSnapshotsTaskInterruptedException(final String message) {
        super(message);
    }

    public EnhancedSnapshotsTaskInterruptedException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public EnhancedSnapshotsTaskInterruptedException(final Throwable cause) {
        super(cause);
    }

    public EnhancedSnapshotsTaskInterruptedException(final String message, final Throwable cause, final boolean enableSuppression, final boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
