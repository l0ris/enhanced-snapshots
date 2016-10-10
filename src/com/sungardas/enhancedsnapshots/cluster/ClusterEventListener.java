package com.sungardas.enhancedsnapshots.cluster;


import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.sungardas.enhancedsnapshots.util.SystemUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
public class ClusterEventListener implements Runnable {

    private static final Logger LOG = LogManager.getLogger(ClusterEventListener.class);

    @Autowired
    private AmazonSQS amazonSQS;
    private static final String ESS_QUEUE_NAME = "ESS-" + SystemUtils.getInstanceId() + "-queue";
    private boolean receiveMessages = false;
    private ExecutorService executor;
    @Value("${enhancedsnapshots.default.polling.rate}")
    private int pollingRate;


    @PostConstruct
    public void run() {
        while (receiveMessages) {
            ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(ESS_QUEUE_NAME);
            List<Message> messages = amazonSQS.receiveMessage(receiveMessageRequest).getMessages();
            for (Message message : messages) {
                System.out.println("  Message");
                System.out.println("    MessageId:     " + message.getMessageId());
                System.out.println("    ReceiptHandle: " + message.getReceiptHandle());
                System.out.println("    MD5OfBody:     " + message.getMD5OfBody());
                System.out.println("    Body:          " + message.getBody());
                for (Map.Entry<String, String> entry : message.getAttributes().entrySet()) {
                    System.out.println("  Attribute");
                    System.out.println("    Name:  " + entry.getKey());
                    System.out.println("    Value: " + entry.getValue());
                }
            }
            sleep();
        }
    }

    public void startListener(){
        executor = Executors.newSingleThreadExecutor();
        executor.execute(this);
        receiveMessages = true;
        LOG.info("Listener for queue: {} started.", ESS_QUEUE_NAME);
    }

    public void stopListener(){
        receiveMessages = false;
        executor.shutdownNow();
        LOG.info("Listener for queue: {} stoped.", ESS_QUEUE_NAME);
    }

    private void sleep() {
        try {
            TimeUnit.MILLISECONDS.sleep(pollingRate);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
