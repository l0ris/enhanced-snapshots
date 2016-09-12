package com.sungardas.enhancedsnapshots.service;

import com.sungardas.enhancedsnapshots.aws.dynamodb.model.TaskEntry;
import com.sungardas.enhancedsnapshots.dto.MailConfigurationDto;

/**
 * Mail service interface
 */
public interface MailService {

    /**
     * reconnect to mail server
     *
     * @return true if connect succeeded
     */
    boolean reconnect();

    /**
     * disconnect from mail server
     */
    void disconnect();

    /**
     * Notify users about completed task
     * @param taskEntry task
     */
    void notifyAboutSuccess(TaskEntry taskEntry);


    /**
     * Notify about error
     * @param taskEntry task
     * @param e exception
     */
    void notifyAboutError(TaskEntry taskEntry, Exception e);


    /**
     * Notify about system status
     * @param message system message
     */
    void notifyAboutSystemStatus(String message);

    /**
     * Test mail configuration (send test mail)
     * @param config mail configuration
     * @param testEmail test mail address
     * @param domain instance domain
     */
    void testConfiguration(MailConfigurationDto config, String testEmail, String domain);
}
