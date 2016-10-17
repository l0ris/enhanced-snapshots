package com.sungardas.enhancedsnapshots.cluster;


import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.sungardas.enhancedsnapshots.aws.dynamodb.model.NodeEntry;
import com.sungardas.enhancedsnapshots.aws.dynamodb.repository.EventsRepository;
import com.sungardas.enhancedsnapshots.aws.dynamodb.repository.NodeRepository;
import com.sungardas.enhancedsnapshots.components.ConfigurationMediator;
import com.sungardas.enhancedsnapshots.util.SystemUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
@DependsOn("clusterConfigurationServiceImpl")
public class AutoScalingEventListener implements Runnable {

    private static final Logger LOG = LogManager.getLogger(AutoScalingEventListener.class);

    @Autowired
    private AmazonSQS amazonSQS;
    @Autowired
    private ClusterEventPublisher clusterEventPublisher;
    @Autowired
    private NodeRepository nodeRepository;
    @Autowired
    private ConfigurationMediator configurationMediator;
    @Autowired
    private EventsRepository eventsRepository;


    private static final String ESS_QUEUE_NAME = "ESS-" + SystemUtils.getInstanceId() + "-queue";
    private boolean receiveMessages = false;
    private ExecutorService executor;
    @Value("${enhancedsnapshots.default.polling.rate}")
    private int pollingRate;


    public void run() {
        while (receiveMessages) {
            try {
                ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(getQueueUrl());
                List<Message> messages = amazonSQS.receiveMessage(receiveMessageRequest).getMessages();
                for (Message message : messages) {
                    JSONObject obj = new JSONObject(message.getBody());
                    String msg = obj.get("Message").toString();
                    AutoScalingEvents event = AutoScalingEvents.valueOf((String) new JSONObject(msg).get("Event"));
                    switch (event) {
                        case EC2_INSTANCE_TERMINATE: {
                            if (eventsRepository.findOne(message.getMessageId()) == null) {
                                clusterEventPublisher.nodeTerminated((String) new JSONObject(msg).get("EC2InstanceId"), message.getMessageId());
                                amazonSQS.deleteMessage(new DeleteMessageRequest()
                                        .withQueueUrl(getQueueUrl()).withReceiptHandle(message.getReceiptHandle()));
                            }
                        }
                        default: {
                            LOG.warn("New AutoScaling event: {}", message.toString());
                            amazonSQS.deleteMessage(new DeleteMessageRequest()
                                    .withQueueUrl(getQueueUrl()).withReceiptHandle(message.getReceiptHandle()));
                        }
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
            executor = Executors.newSingleThreadExecutor();
            receiveMessages = true;
            executor.execute(this);
            LOG.info("Listener for queue: {} started.", ESS_QUEUE_NAME);
        }
    }

    @PreDestroy
    public void stopListener() {
        if (executor != null) {
            receiveMessages = false;
            executor.shutdownNow();
            LOG.info("Listener for queue: {} stoped.", ESS_QUEUE_NAME);
        }
    }

    private void sleep() {
        try {
            TimeUnit.MILLISECONDS.sleep(pollingRate);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private String getQueueUrl() {
        return amazonSQS.getQueueUrl(ESS_QUEUE_NAME).getQueueUrl();
    }


    public enum AutoScalingEvents {

        EC2_INSTANCE_LAUNCH("autoscaling:EC2_INSTANCE_LAUNCH"), EC2_INSTANCE_LAUNCH_ERROR("autoscaling:EC2_INSTANCE_LAUNCH_ERROR"),
        EC2_INSTANCE_TERMINATE("autoscaling:EC2_INSTANCE_TERMINATE"), EC2_INSTANCE_TERMINATE_ERROR("autoscaling:EC2_INSTANCE_TERMINATE_ERROR");

        public String getAutoScalingEvent() {
            return autoScalingEvent;
        }

        private final String autoScalingEvent;

        AutoScalingEvents(String event) {
            this.autoScalingEvent = event;
        }
    }

    private boolean isMasterNode(String instanceId) {
        NodeEntry node = nodeRepository.findOne(instanceId);
        return node != null && node.isMaster();
    }

}
