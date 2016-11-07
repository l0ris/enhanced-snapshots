package com.sungardas.enhancedsnapshots.rest;


import com.sungardas.enhancedsnapshots.cluster.ClusterEventPublisher;
import com.sungardas.enhancedsnapshots.components.ConfigurationMediator;
import com.sungardas.enhancedsnapshots.components.logwatcher.LogsWatcherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

import javax.annotation.security.RolesAllowed;

@Controller
public class LogsController {

    @Autowired
    private LogsWatcherService logsWatcherService;
    @Autowired
    private ClusterEventPublisher clusterEventPublisher;
    @Autowired
    private ConfigurationMediator configurationMediator;

    @RolesAllowed("ROLE_ADMIN")
    @SubscribeMapping("/logs")
    public void subscriptionHandler() {
        if (configurationMediator.isClusterMode()) {
            clusterEventPublisher.logWatcherStarted();
        }
        logsWatcherService.start();
        logsWatcherService.sendLatestLogs();
    }
}
