package com.sungardas.enhancedsnapshots.ws;

import com.sungardas.enhancedsnapshots.aws.dynamodb.model.NodeEntry;
import com.sungardas.enhancedsnapshots.aws.dynamodb.repository.NodeRepository;
import com.sungardas.enhancedsnapshots.components.ConfigurationMediator;
import com.sungardas.enhancedsnapshots.service.AWSCommunicationService;
import com.sungardas.enhancedsnapshots.util.SystemUtils;
import org.apache.activemq.broker.BrokerService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompBrokerRelayMessageHandler;
import org.springframework.web.socket.config.annotation.AbstractWebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;

import java.util.List;


@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig extends AbstractWebSocketMessageBrokerConfigurer implements ApplicationContextAware {

    private static final Logger LOG = LogManager.getLogger(WebSocketConfig.class);
    public static final String TASK_PROGRESS_DESTINATION = "/task";
    public static final String ERROR_DESTINATION = "/error";
    public static final String LOGS_DESTINATION = "/logs";
    @Value("${enhancedsnapshots.logs.broker.port}")
    private int brokerPort;
    @Autowired
    private AWSCommunicationService awsCommunicationService;
    @Autowired
    private NodeRepository nodeRepository;
    @Autowired
    private ConfigurationMediator configurationMediator;
    private ApplicationContext applicationContext;
    private StompBrokerRelayMessageHandler stompBrokerRelayMessageHandler;


    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        if (configurationMediator.isClusterMode()) {
            List<NodeEntry> nodeEntries = nodeRepository.findByMaster(true);
            String masterId;
            if (nodeEntries.isEmpty()) {
                masterId = SystemUtils.getInstanceId();
            } else {
                masterId = nodeEntries.get(0).getNodeId();
            }
            config.enableStompBrokerRelay(ERROR_DESTINATION, TASK_PROGRESS_DESTINATION, LOGS_DESTINATION)
                    .setRelayHost(awsCommunicationService.getDNSName(masterId)).setRelayPort(brokerPort);
        } else {
            config.enableSimpleBroker(ERROR_DESTINATION, TASK_PROGRESS_DESTINATION, LOGS_DESTINATION);
        }
    }

    @Bean
    public BrokerService broker() throws Exception {
        BrokerService broker = new BrokerService();
        broker.addConnector("stomp://0.0.0.0:" + brokerPort);
        broker.setPersistent(false);
        return broker;
    }


    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOrigins("*").withSockJS();
    }

    public void updateWebSocketConfiguration() {
        try {
            LOG.info("Updating websocket configuration");
            if (stompBrokerRelayMessageHandler == null) {
                stompBrokerRelayMessageHandler = (StompBrokerRelayMessageHandler) applicationContext.getBean("stompBrokerRelayMessageHandler");
            }
            stompBrokerRelayMessageHandler.stop();
            stompBrokerRelayMessageHandler.setTcpClient(null);
            stompBrokerRelayMessageHandler.setRelayHost(awsCommunicationService.getDNSName(nodeRepository.findByMaster(true).get(0).getNodeId()));
            stompBrokerRelayMessageHandler.start();
        } catch (Exception e) {
            LOG.error("Failed to update websocket configuration", e);
        }
    }


    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
