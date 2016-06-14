package com.sungardas.enhancedsnapshots.service.impl;

import org.springframework.context.annotation.DependsOn;

@DependsOn("CreateAppConfiguration")
public class SystemServiceDev extends SystemServiceImpl {

    @Override
    public void backup(final String taskId) {

    }

    @Override
    public void restore() {

    }

    @Override
    protected String getInstanceId() {
        return "DEV";
    }
}
