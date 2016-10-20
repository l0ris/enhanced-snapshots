package com.sungardas.enhancedsnapshots.cluster;

import com.datish.copycat.Server;
import com.sungardas.enhancedsnapshots.aws.dynamodb.model.EventEntry;
import com.sungardas.enhancedsnapshots.aws.dynamodb.model.NodeEntry;
import com.sungardas.enhancedsnapshots.aws.dynamodb.repository.NodeRepository;
import com.sungardas.enhancedsnapshots.components.ConfigurationMediator;
import com.sungardas.enhancedsnapshots.service.AWSCommunicationService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CopyCatWrapper implements ClusterEventListener {

    private static final Logger LOG = LogManager.getLogger(CopyCatWrapper.class);

    @Autowired
    private ConfigurationMediator configurationMediator;

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private AWSCommunicationService awsCommunicationService;

    @Value("${enhancedsnapshots.copycat.persist.path}")
    private String persistPath;

    @Value("enhancedsnapshots.copycat.port")
    private int port;

    private ConcurrentHashMap<String, Server> serverMap = new ConcurrentHashMap<>();

    @PostConstruct
    private void init() {
        if (configurationMediator.isClusterMode()) {
            for (NodeEntry node : nodeRepository.findAll()) {
                try {
                    //TODO remove password hardcode
                    Server server = new Server(node.getSdfsVolumeId(), getHostName(node.getNodeId()), port, "PASSWORD", true, true);
                    serverMap.put(node.getNodeId(), server);
                } catch (Exception e) {
                    LOG.error(e);
                }
            }
        }

    }

    @Override
    public void launched(EventEntry eventEntry) {
        try {
            //TODO remove password hardcode
            Server server = new Server(eventEntry.getVolumeId(), getHostName(eventEntry.getInstanceId()), port, "PASSWORD", true, true);
            serverMap.put(eventEntry.getInstanceId(), server);
        } catch (Exception e) {
            LOG.error(e);
        }
    }


    @Override
    public void terminated(EventEntry eventEntry) {
        try {
            serverMap.remove(eventEntry.getInstanceId()).close();
        } catch (Exception e) {
            LOG.error(e);
        }
    }

    private String getHostName(String nodeId) {
        return awsCommunicationService.getInstance(nodeId).getPrivateDnsName();
    }
}
