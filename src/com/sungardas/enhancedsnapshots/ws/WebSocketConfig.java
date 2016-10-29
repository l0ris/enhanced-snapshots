package com.sungardas.enhancedsnapshots.ws;

import com.sungardas.enhancedsnapshots.aws.dynamodb.repository.NodeRepository;
import com.sungardas.enhancedsnapshots.components.ConfigurationMediator;
import com.sungardas.enhancedsnapshots.service.AWSCommunicationService;
import org.apache.activemq.broker.BrokerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.AbstractWebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;



@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig extends AbstractWebSocketMessageBrokerConfigurer {

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


    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        if (configurationMediator.isClusterMode()) {
            String masterId = nodeRepository.findByMaster(true).get(0).getNodeId();
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
}
