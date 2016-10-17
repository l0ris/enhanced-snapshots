package com.sungardas.enhancedsnapshots.cluster;

import com.sungardas.enhancedsnapshots.aws.dynamodb.model.EventEntry;
import com.sungardas.enhancedsnapshots.aws.dynamodb.model.NodeEntry;
import com.sungardas.enhancedsnapshots.aws.dynamodb.repository.EventsRepository;
import com.sungardas.enhancedsnapshots.aws.dynamodb.repository.NodeRepository;
import com.sungardas.enhancedsnapshots.components.ConfigurationMediator;
import com.sungardas.enhancedsnapshots.service.SystemService;
import com.sungardas.enhancedsnapshots.util.SystemUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.StreamSupport;

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
    @Autowired
    private ConfigurationMediator configurationMediator;


    public void run() {
        while (configurationMediator.isClusterMode()) {
            List<EventEntry> events = eventsRepository.findByTimeGreaterThan(lastCheckTime);
            try {
                for (EventEntry eventEntry : events) {
                    ClusterEvents event = ClusterEvents.valueOf(eventEntry.getEvent());
                    switch (event) {
                        case NODE_LAUNCHED: {
                            NodeEntry nodeEntry = nodeRepository.findOne(eventEntry.getInstanceId());
                            LOG.info("node launched event: {}", eventEntry.toString());
                            //TODO: update copycat
                        }
                        case NODE_TERMINATED: {
                            NodeEntry terminatedNode = nodeRepository.findOne(eventEntry.getInstanceId());
                            // check whether terminated node was master one and current node should become a new master
                            if (terminatedNode.isMaster() && StreamSupport.stream(nodeRepository.findAll().spliterator(), false)
                                    .sorted(Comparator.comparing(node -> node.getNodeId()))
                                    .findFirst().get().getNodeId().toLowerCase().equals(SystemUtils.getInstanceId().toLowerCase())) {
                                NodeEntry currentNode = nodeRepository.findOne(SystemUtils.getInstanceId());
                                currentNode.setMaster(true);
                                nodeRepository.save(currentNode);
                            }
                            //TODO: update copycat
                            LOG.info("Node terminated event: {}", eventEntry.toString());
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
            } catch (Exception e) {
                LOG.error(e);
            }
            sleep();
        }
    }


    @PostConstruct
    public void startListener() {
        if (configurationMediator.isClusterMode()) {
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
