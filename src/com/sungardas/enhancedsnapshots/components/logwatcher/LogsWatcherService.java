package com.sungardas.enhancedsnapshots.components.logwatcher;

import com.sungardas.enhancedsnapshots.components.ConfigurationMediator;
import org.apache.commons.io.input.ReversedLinesFileReader;
import org.apache.commons.io.input.Tailer;

import org.apache.commons.io.input.TailerListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.DependsOn;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.util.Collections.*;


@Service
@DependsOn("SystemService")
public class LogsWatcherService implements TailerListener, ApplicationListener<SessionUnsubscribeEvent> {

    private static final Logger LOG = LogManager.getLogger(LogsWatcherService.class);
    private static final String LOGS_DESTINATION = "/logs";
    private File logFile;

    private Tailer tailer;

    @Autowired
    private ConfigurationMediator configurationMediator;
    @Autowired
    private SimpMessagingTemplate template;
    @Value("${catalina.home}")
    private String catalinaHome;

    @PreDestroy
    public void destroy() {
        stop();
    }

    public void stop() {
        if (tailer != null) {
            tailer.stop();
            LOG.info("Logs watcher stopped.");
        }
    }
    @PostConstruct
    public void start() {
        if (tailer != null) {
            tailer.stop();
        }
        tailer = Tailer.create(getLogsFile(), this, 500L, true);
        LOG.info("Logs watcher started. File {} will be tracked for changes.", configurationMediator.getLogFileName());
    }

    private File getLogsFile() {
        if (logFile == null) {
            logFile = Paths.get(catalinaHome, configurationMediator.getLogFileName()).toFile();
        }
        return logFile;
    }

    @Override
    public void init(Tailer tailer) {}

    @Override
    public void fileNotFound() {
        LOG.warn("Log file {} was not found", configurationMediator.getLogFileName());
    }

    @Override
    public void fileRotated() {}

    @Override
    public synchronized void handle(String line) {
        template.convertAndSend(LOGS_DESTINATION, Arrays.asList(line));
    }

    @Override
    public void handle(Exception ex) {
        LOG.warn("Failed to read log file {}", configurationMediator.getLogFileName(), ex);
    }

    public synchronized List<String> getLatestLogs() {
        List<String> list = new ArrayList<>();
        try {
            ReversedLinesFileReader reader = new ReversedLinesFileReader(getLogsFile());
            while (list.size() < configurationMediator.getLogsBufferSize()) {
                list.add(reader.readLine());
            }
            reverse(list);
            return list;
        } catch (IOException e) {
            LOG.warn("Failed to read logs {}", e);
            reverse(list);
            return list;
        }
    }

    @Override
    public void onApplicationEvent(SessionUnsubscribeEvent event) {
        stop();
    }
}
