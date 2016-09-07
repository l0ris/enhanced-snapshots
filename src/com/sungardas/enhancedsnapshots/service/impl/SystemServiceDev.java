package com.sungardas.enhancedsnapshots.service.impl;

import com.sungardas.enhancedsnapshots.dto.SystemConfiguration;
import org.springframework.context.annotation.DependsOn;

@DependsOn("CreateAppConfiguration")
public class SystemServiceDev extends SystemServiceImpl {

    @Override
    public void backup(final String taskId) {

    }

    @Override
    protected String getInstanceId() {
        return "DEV";
    }

    @Override
    public void setSystemConfiguration(SystemConfiguration configuration) {
        if (configuration.getDomain() == null || configuration.getDomain().isEmpty()) {
            configuration.setDomain("http://localhost:8080");
        }
        super.setSystemConfiguration(configuration);
    }
}
