package com.sungardas.enhancedsnapshots.service;

import com.sungardas.enhancedsnapshots.aws.dynamodb.model.TaskEntry;
import com.sungardas.enhancedsnapshots.dto.MailConfigurationDto;

public interface MailService {

    boolean reconnect();

    void disconnect();

    void notifyAboutSuccess(TaskEntry taskEntry);

    void notifyAboutError(TaskEntry taskEntry, Exception e);

    void notifyAboutSystemStatus(String message);

    void testConfiguration(MailConfigurationDto config, String testEmail, String domain);
}
