package com.sungardas.enhancedsnapshots.dto;

public class MailConfigurationTestDto {
    private MailConfigurationDto mailConfiguration;
    private String testEmail;
    private String domain;

    public MailConfigurationDto getMailConfiguration() {
        return mailConfiguration;
    }

    public void setMailConfiguration(MailConfigurationDto mailConfiguration) {
        this.mailConfiguration = mailConfiguration;
    }

    public String getTestEmail() {
        return testEmail;
    }

    public void setTestEmail(String testEmail) {
        this.testEmail = testEmail;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }
}
