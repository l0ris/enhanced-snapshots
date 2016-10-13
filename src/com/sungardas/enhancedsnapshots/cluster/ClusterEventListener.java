package com.sungardas.enhancedsnapshots.cluster;

import com.sungardas.enhancedsnapshots.aws.dynamodb.model.EventEntry;
import com.sungardas.enhancedsnapshots.aws.dynamodb.model.NodeEntry;
import com.sungardas.enhancedsnapshots.aws.dynamodb.repository.EventsRepository;
import com.sungardas.enhancedsnapshots.aws.dynamodb.repository.NodeRepository;
import com.sungardas.enhancedsnapshots.service.SystemService;
import com.sungardas.enhancedsnapshots.util.SystemUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
public class ClusterEventListener implements Runnable {


    private static final Logger LOG = LogManager.getLogger(ClusterEventListener.class);

    @Autowired
    private EventsRepository eventsRepository;
    @Autowired
    private NodeRepository nodeRepository;
    @Autowired
    private SystemService systemService;
    private ExecutorService executor;
    @Value("${enhancedsnapshots.default.polling.rate}")
    private int pollingRate;
    private long lastCheckTime;


    public void run() {
        while (SystemUtils.clusterMode()) {
            List<EventEntry> events = eventsRepository.findByTimeGreaterThan(lastCheckTime);
            try {
                for (EventEntry eventEntry : events) {
                    ClusterEvents event = ClusterEvents.valueOf(eventEntry.getEvent());
                    switch (event) {
                        case NODE_LAUNCHED: {
                            NodeEntry nodeEntry = nodeRepository.findOne(eventEntry.getInstanceId());
                            //TODO: update copycat
                        }
                        case NODE_TERMINATED: {
                            //TODO: update copycat
                        }
                        case SETTINGS_UPDATED: {
                            systemService.refreshSystemConfiguration();
                            LOG.info("System settings synchronized with DB");
                        }
                        default: {
                            LOG.warn("Unknown event type: {}", event);
                        }
                    }
                    if (eventEntry.getTime() > lastCheckTime) {
                        lastCheckTime = eventEntry.getTime();
                    }
                }
                //TODO: checkMasterNodeIsAlive();
            } catch (Exception e) {
                LOG.error(e);
            }
            sleep();
        }
    }


    @PostConstruct
    public void startListener() {
        if (SystemUtils.clusterMode()) {
            lastCheckTime = System.currentTimeMillis();
            executor = Executors.newSingleThreadExecutor();
            executor.execute(this);
            LOG.info("Cluster events listener started.");
        }
    }

    @PreDestroy
    public void stopListener() {
        if (executor != null) {
            executor.shutdownNow();
            LOG.info("Cluster events listener stopped.");
        }
    }

    private void sleep() {
        try {
            TimeUnit.MILLISECONDS.sleep(pollingRate);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
