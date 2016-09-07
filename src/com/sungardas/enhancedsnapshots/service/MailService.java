package com.sungardas.enhancedsnapshots.service;

import com.sungardas.enhancedsnapshots.aws.dynamodb.model.MailConfigurationDocument;
import com.sungardas.enhancedsnapshots.aws.dynamodb.model.TaskEntry;

public interface MailService {

    boolean reconnect();

    void disconnect();

    boolean checkConfiguration(MailConfigurationDocument configurationDocument);

    void notifyAboutSuccess(TaskEntry taskEntry);

    void notifyAboutError(TaskEntry taskEntry, Exception e);

    void notifyAboutSystemStatus(String message);
}
