package com.sungardas.enhancedsnapshots.components.logwatcher;

import com.sungardas.enhancedsnapshots.components.ConfigurationMediator;
import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Collection;

@Service
public class LogsTailerListener implements TailerListener {

    private static final Logger LOG = LogManager.getLogger(LogsTailerListener.class);
    public static final String LOGS_DESTINATION = "/logs";
    @Autowired
    private ConfigurationMediator configurationMediator;
    @Autowired
    private SimpMessagingTemplate template;

    private CircularFifoQueue<String> logs;

    @Override
    public void init(Tailer tailer) {
        logs = new CircularFifoQueue<>(configurationMediator.getLogsBufferSize());
    }

    @Override
    public void fileNotFound() {
        LOG.warn("Log file {} was not found", configurationMediator.getLogFileName());
    }

    @Override
    public void fileRotated() {

    }

    @Override
    public synchronized void handle(String line) {
        if (configurationMediator.getLogsBufferSize() != logs.maxSize()) {
            expandBufferSize(configurationMediator.getLogsBufferSize());
        }
        logs.add(line);
        template.convertAndSend(LOGS_DESTINATION, line);
    }

    @Override
    public void handle(Exception ex) {
        LOG.warn("Failed to read log file {}", configurationMediator.getLogFileName(), ex);
    }

    private void expandBufferSize(int newSize){
        CircularFifoQueue expandedQueue = new CircularFifoQueue<String>(newSize);
        expandedQueue.addAll(logs);
        logs = expandedQueue;
        LOG.info("Logs buffer size was expanded to {} lines", newSize);
    }

    public Collection<String> getLatestLogs () {
        return logs;
    }
}
