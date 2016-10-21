package com.sungardas.enhancedsnapshots.cluster;

import org.apache.activemq.broker.BrokerService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;

@Service
public class WebSocketNotificationsBroker {

    private static final Logger LOG = LogManager.getLogger(WebSocketNotificationsBroker.class);
    @Value("${enhancedsnapshots.logs.broker.port}")
    private int brokerPort;
    private BrokerService broker;


    public void startBroker() {
        try {
            if (broker == null) {
                broker = new BrokerService();
                broker.addConnector(new URI("tcp", "localhost", Integer.toString(brokerPort)));
            }
            broker.start();
        } catch (Exception e) {
            LOG.error("Failed to start broker", e);
        }
    }

    public void stopBroker() {
        try {
            if (broker != null) {
                broker.stop();
            }
        } catch (Exception e) {
            LOG.error("Failed to stop broker", e);
        }

    }
}
