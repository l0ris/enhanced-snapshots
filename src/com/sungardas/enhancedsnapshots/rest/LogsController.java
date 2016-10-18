package com.sungardas.enhancedsnapshots.rest;


import com.sungardas.enhancedsnapshots.components.logwatcher.LogsWatcherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

import javax.annotation.security.RolesAllowed;

@Controller
public class LogsController {

    @Autowired
    private LogsWatcherService logsWatcherService;

    @RolesAllowed("ROLE_ADMIN")
    @SubscribeMapping("/logs")
    public void subscriptionHandler() {
        logsWatcherService.start();
        logsWatcherService.sendLatestLogs();
    }
}
