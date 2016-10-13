package com.sungardas.enhancedsnapshots.cluster;

import com.sungardas.enhancedsnapshots.aws.dynamodb.model.EventEntry;
import com.sungardas.enhancedsnapshots.aws.dynamodb.model.NodeEntry;
import com.sungardas.enhancedsnapshots.aws.dynamodb.repository.EventsRepository;
import com.sungardas.enhancedsnapshots.aws.dynamodb.repository.NodeRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class ClusterEventPublisherImpl implements ClusterEventPublisher {

    private static final Logger LOG = LogManager.getLogger(ClusterEventPublisherImpl.class);
    @Autowired
    private EventsRepository eventsRepository;
    @Autowired
    private NodeRepository nodeRepository;


    public void settingsUpdated() {
        EventEntry eventEntry = new EventEntry(System.currentTimeMillis(), ClusterEvents.SETTINGS_UPDATED.getEvent(), null, null);
        eventsRepository.save(eventEntry);
        LOG.info("Settings update event was published");
    }

    public void nodeLaunched(String nodeId, String volumeId) {
        EventEntry eventEntry = new EventEntry(System.currentTimeMillis(), ClusterEvents.NODE_LAUNCHED.getEvent(), nodeId, volumeId);
        eventsRepository.save(eventEntry);
        LOG.info("Node launched event published");
    }

    public void nodeTerminated(String nodeId) {
        NodeEntry terminatedNode = nodeRepository.findOne(nodeId);
        EventEntry eventEntry = new EventEntry(System.currentTimeMillis(), ClusterEvents.NODE_TERMINATED.getEvent(), nodeId, terminatedNode.getSdfsVolumeId());
        eventsRepository.save(eventEntry);
        nodeRepository.delete(terminatedNode);
        LOG.info("Node terminated event published");
    }
}
