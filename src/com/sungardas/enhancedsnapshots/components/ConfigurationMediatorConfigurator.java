package com.sungardas.enhancedsnapshots.components;

import com.sungardas.enhancedsnapshots.aws.dynamodb.model.Configuration;

public interface ConfigurationMediatorConfigurator extends ConfigurationMediator {
    void setCurrentConfiguration(final Configuration currentConfiguration);
}
