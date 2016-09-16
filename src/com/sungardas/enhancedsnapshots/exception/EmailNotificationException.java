package com.sungardas.enhancedsnapshots.exception;

public class EmailNotificationException extends EnhancedSnapshotsException {
    public EmailNotificationException() {
    }

    public EmailNotificationException(String message) {
        super(message);
    }

    public EmailNotificationException(String message, Throwable cause) {
        super(message, cause);
    }

    public EmailNotificationException(Throwable cause) {
        super(cause);
    }

    public EmailNotificationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
