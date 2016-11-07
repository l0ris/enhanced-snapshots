package com.sungardas.enhancedsnapshots.service.impl;

import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Service("SystemService")
@DependsOn("CreateAppConfiguration")
@Profile("dev")
public class SystemServiceDev extends SystemServiceImpl {

    @Override
    public void backup(final String taskId) {

    }

    @Override
    protected String getSystemId() {
        return "DEV";
    }
}
