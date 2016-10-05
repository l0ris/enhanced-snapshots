package com.sungardas.enhancedsnapshots.service.impl;

import com.sungardas.enhancedsnapshots.aws.dynamodb.model.Configuration;
import com.sungardas.enhancedsnapshots.util.SystemUtils;
import org.springframework.context.annotation.DependsOn;

@DependsOn("CreateAppConfiguration")
public class SystemServiceDev extends SystemServiceImpl {

    @Override
    public void backup(final String taskId) {

    }

    @Override
    protected String getSystemId() {
        return "DEV";
    }
}
