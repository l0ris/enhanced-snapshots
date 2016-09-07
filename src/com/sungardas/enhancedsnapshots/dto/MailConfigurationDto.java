package com.sungardas.enhancedsnapshots.dto;

import com.sungardas.enhancedsnapshots.enumeration.MailConnectionType;
import com.sungardas.enhancedsnapshots.enumeration.MailNotificationEvent;

import java.util.Set;

public class MailConfigurationDto {

    private String fromMailAddress;

    private Set<String> recipients;

    private String userName;

    private String password;

    private String mailSMTPHost;

    private int mailSMTPPort;

    private MailConnectionType connectionType;

    private Set<MailNotificationEvent> events;

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

    public Set<MailNotificationEvent> getEvents() {
        return events;
    }

    public void setEvents(Set<MailNotificationEvent> events) {
        this.events = events;
    }
}
