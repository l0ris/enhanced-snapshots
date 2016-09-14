package com.sungardas.enhancedsnapshots.aws.dynamodb.model;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBDocument;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMarshalling;
import com.sungardas.enhancedsnapshots.aws.dynamodb.Marshaller.MailConnectionTypeMarshaller;
import com.sungardas.enhancedsnapshots.enumeration.MailConnectionType;

import java.util.Set;

@DynamoDBDocument
public class MailConfigurationDocument {

    @DynamoDBAttribute
    private String fromMailAddress;

    @DynamoDBAttribute
    private Set<String> recipients;

    @DynamoDBAttribute
    private String userName;

    /**
     * Password value should be encrypted using {@link com.sungardas.enhancedsnapshots.service.CryptoService}
     */
    @DynamoDBAttribute
    private String password;

    @DynamoDBAttribute(attributeName = "mail.smtp.host")
    private String mailSMTPHost;

    @DynamoDBAttribute(attributeName = "mail.smtp.port")
    private int mailSMTPPort;

    @DynamoDBAttribute
    @DynamoDBMarshalling(marshallerClass = MailConnectionTypeMarshaller.class)
    private MailConnectionType connectionType;

    @DynamoDBAttribute
    private MailNotificationEvents events = new MailNotificationEvents();

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

    /**
     * Password value should be encrypted using {@link com.sungardas.enhancedsnapshots.service.CryptoService}
     *
     * @param password value
     */
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

}
