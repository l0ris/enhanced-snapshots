package com.sungardas.enhancedsnapshots.components.logwatcher;


import com.sungardas.enhancedsnapshots.components.ConfigurationMediator;
import org.apache.commons.io.input.Tailer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.nio.file.Paths;
import java.util.Collection;


@Service
public class LogsWatcherService {

    private static final Logger LOG = LogManager.getLogger(LogsWatcherService.class);
    private static final String catalinaHomeEnvPropName = "catalina.home";

    private Tailer tailer;
    @Autowired
    private LogsTailerListener logsTailerListener;
    @Autowired
    private ConfigurationMediator configurationMediator;

    @PreDestroy
    public void destroy() {
        tailer.stop();
        LOG.info("Logs watcher stoped");
    }

    public Collection getLatestLogs(){
        return logsTailerListener.getLatestLogs();
    }

    public void stop(){
        tailer.stop();
        LOG.info("Logs watcher stoped.");
    }

    @PostConstruct
    public void start() {
        if (tailer != null) {
            tailer.stop();
        }
        System.out.println("Catalina_home: "+System.getProperty(catalinaHomeEnvPropName));
        tailer = Tailer.create(Paths.get(System.getProperty(catalinaHomeEnvPropName), configurationMediator.getLogFileName()).toFile(), logsTailerListener, 500L, true);
        LOG.info("Logs watcher started. File {} will be tracked for changes.", configurationMediator.getLogFileName());
    }
}
