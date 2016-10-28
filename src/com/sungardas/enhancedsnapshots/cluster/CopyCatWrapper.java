package com.sungardas.enhancedsnapshots.cluster;

import com.datish.copycat.Server;
import com.sungardas.enhancedsnapshots.aws.dynamodb.model.EventEntry;
import com.sungardas.enhancedsnapshots.aws.dynamodb.model.NodeEntry;
import com.sungardas.enhancedsnapshots.aws.dynamodb.repository.NodeRepository;
import com.sungardas.enhancedsnapshots.components.ConfigurationMediator;
import com.sungardas.enhancedsnapshots.service.AWSCommunicationService;
import com.sungardas.enhancedsnapshots.util.SystemUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CopyCatWrapper implements ClusterEventListener {

    private static final Logger LOG = LogManager.getLogger(CopyCatWrapper.class);

    private static String COPYCAT_SYNC_CMD = "sdfscli --sync-remote-cloud-volume={VOLUME_ID} --password={PASSWORD}";

    @Autowired
    private ConfigurationMediator configurationMediator;

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private AWSCommunicationService awsCommunicationService;

    @Value("${enhancedsnapshots.copycat.persist.path}")
    private String persistPath;

    @Value("${enhancedsnapshots.copycat.port}")
    private int port;

    private ConcurrentHashMap<String, Server> serverMap = new ConcurrentHashMap<>();

    @PostConstruct
    private void init() {
        Server.PERSISTENCE_PATH = persistPath;
        if (configurationMediator.isClusterMode()) {
            for (NodeEntry node : nodeRepository.findAll()) {
                try {
                    String hostName = getHostName(node.getNodeId());
                    LOG.info("CopyCat server, volumeId={}, hostName={}, port={} started", node.getSdfsVolumeId(), hostName, port);
                    deleteCopyCatTempData(node.getSdfsVolumeId());
                    Server server = new Server(node.getSdfsVolumeId(), hostName, port, configurationMediator.getConfigurationId(), true, true);
                    serverMap.put(node.getNodeId(), server);
                    synchronizeSdfsVolumes(node);
                } catch (Exception e) {
                    LOG.error("CopyCat server start failed", e);
                }
            }
        }

    }

    private void synchronizeSdfsVolumes(NodeEntry node) {
        try {
            LOG.info("Synchronization with volume {} started", node.getSdfsVolumeId());
            Process process = Runtime.getRuntime().exec(COPYCAT_SYNC_CMD
                    .replace("{VOLUME_ID}", node.getSdfsVolumeId()+"")
                    .replace("{PASSWORD}", SystemUtils.getSystemId()));
            process.waitFor();
            LOG.info("Synchronization with volume {} finished", node.getSdfsVolumeId());
        } catch (Exception e) {
            LOG.error("Synchronization with volume failed", e);
        }
    }

    private void deleteCopyCatTempData(long volumeId) {
        try {
            new File(Server.PERSISTENCE_PATH + File.separator + volumeId + ".db").delete();
        } catch (Exception e) {
            //skip
        }
    }

    @Override
    public void launched(EventEntry eventEntry) {
        try {
            String hostName = getHostName(eventEntry.getInstanceId());
            deleteCopyCatTempData(eventEntry.getVolumeId());
            LOG.info("CopyCat server, volumeId={}, hostName={}, port={} started", eventEntry.getVolumeId(), hostName, port);
            Server server = new Server(eventEntry.getVolumeId(), hostName, port, configurationMediator.getConfigurationId(), true, true);
            serverMap.put(eventEntry.getInstanceId(), server);
        } catch (Exception e) {
            LOG.error("CopyCat server start failed", e);
        }
    }


    @Override
    public void terminated(EventEntry eventEntry) {
        try {
            serverMap.remove(eventEntry.getInstanceId()).close();
            deleteCopyCatTempData(eventEntry.getVolumeId());
        } catch (Exception e) {
            LOG.error("CopyCat server stop failed", e);
        }
    }

    private String getHostName(String nodeId) {
        return awsCommunicationService.getDNSName(nodeId);
    }
}
