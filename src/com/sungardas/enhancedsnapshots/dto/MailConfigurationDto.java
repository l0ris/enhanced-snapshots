package com.sungardas.enhancedsnapshots.dto;

import com.sungardas.enhancedsnapshots.enumeration.MailConnectionType;

import java.util.Set;

public class MailConfigurationDto {

    private String fromMailAddress;

    private Set<String> recipients;

    private String userName;

    private String password;

    private String mailSMTPHost;

    private int mailSMTPPort;

    private MailConnectionType connectionType;

    private MailNotificationEvents events;

    public String getFromMailAddress() {
        return fromMailAddress;
    }

    public void setFromMailAddress(String fromMailAddress) {
        this.fromMailAddress = fromMailAddress;
    }

    public Set<String> getRecipients() {
        return recipients;
    }

    public void setRecipients(Set<String> recipients) {
        this.recipients = recipients;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getMailSMTPHost() {
        return mailSMTPHost;
    }

    public void setMailSMTPHost(String mailSMTPHost) {
        this.mailSMTPHost = mailSMTPHost;
    }

    public int getMailSMTPPort() {
        return mailSMTPPort;
    }

    public void setMailSMTPPort(int mailSMTPPort) {
        this.mailSMTPPort = mailSMTPPort;
    }

    public MailConnectionType getConnectionType() {
        return connectionType;
    }

    public void setConnectionType(MailConnectionType connectionType) {
        this.connectionType = connectionType;
    }

    public MailNotificationEvents getEvents() {
        return events;
    }

    public void setEvents(MailNotificationEvents events) {
        this.events = events;
    }

    public static class MailNotificationEvents {
        private boolean success;
        private boolean error;
        private boolean info;

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public boolean isError() {
            return error;
        }

        public void setError(boolean error) {
            this.error = error;
        }

        public boolean isInfo() {
            return info;
        }

        public void setInfo(boolean info) {
            this.info = info;
        }
    }
}
