package com.sungardas.enhancedsnapshots.rest;


import com.sungardas.enhancedsnapshots.components.logwatcher.LogsWatcherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

import java.util.Collection;

@Controller
public class LogsController {

    private final static String SUBSCRIBTIONS_MSG = "subscribed";

    @Autowired
    private LogsWatcherService logsWatcherService;

    @SubscribeMapping("/logs")
    public void subscriptionHandler() {
        logsWatcherService.start();
        logsWatcherService.sendLatestLogs();
    }
}
