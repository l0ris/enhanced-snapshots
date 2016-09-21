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
    private final static String UNSUBSCRIBTIONS_MSG = "unsubscribed";

    @Autowired
    private LogsWatcherService logsWatcherService;

    @MessageMapping({"/logs"})
    public Collection<String> msgHandler(String msg) {
        if (msg.equals(SUBSCRIBTIONS_MSG)) {
            logsWatcherService.start();
            return logsWatcherService.getLatestLogs();
        }
        if (msg.equals(UNSUBSCRIBTIONS_MSG)) {
            logsWatcherService.stop();
        }
        return null;
    }

    @SubscribeMapping("/logs")
    public Collection<String> subscriptionHandler() {
        logsWatcherService.start();
        return logsWatcherService.getLatestLogs();
    }


}
