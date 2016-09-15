package com.sungardas.enhancedsnapshots.rest;


import com.sungardas.enhancedsnapshots.components.logwatcher.LogsWatcherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

@Controller
public class LogsController {

    @Autowired
    private LogsWatcherService logsWatcherService;

    @SubscribeMapping({"/logs"})
    public void handleLogsSubscription() {
        logsWatcherService.start();
//        logsWatcherService.getLatestLogs();
    }


}
