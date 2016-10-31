package com.sungardas.destroy;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.sungardas.enhancedsnapshots.aws.AmazonConfigProvider;
import com.sungardas.enhancedsnapshots.aws.dynamodb.model.Configuration;
import com.sungardas.enhancedsnapshots.aws.dynamodb.repository.ConfigurationRepository;
import com.sungardas.enhancedsnapshots.aws.dynamodb.repository.NodeRepository;
import com.sungardas.enhancedsnapshots.cluster.ClusterConfigurationService;
import com.sungardas.enhancedsnapshots.service.AWSCommunicationService;
import com.sungardas.enhancedsnapshots.util.SystemUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.PostConstruct;
import java.util.stream.StreamSupport;

public class RemoveAppConfiguration {

    private static final Logger LOG = LogManager.getLogger(RemoveAppConfiguration.class);

    @Value("${enhancedsnapshots.db.tables}")
    private String[] tables;

    @Value("${removeS3Bucket}")
    private boolean removeS3Bucket;

    @Autowired
    @Qualifier("amazonDynamoDB")
    private AmazonDynamoDB db;

    @Autowired
    private AmazonEC2 ec2;

    @Autowired
    private ConfigurationRepository configurationRepository;

    @Autowired
    private AWSCommunicationService awsCommunicationService;

    @Autowired
    private ClusterConfigurationService clusterConfigurationService;

    @Autowired
    private NodeRepository nodeRepository;

    private DynamoDB dynamoDB;

    private String configurationId;

    @PostConstruct
    private void init() {
        configurationId = SystemUtils.getSystemId();
        dynamoDB = new DynamoDB(db);
        switch (SystemUtils.getSystemMode()) {
            case CLUSTER:
                dropConfiguration(removeS3Bucket);
                clusterConfigurationService.removeClusterInfrastructure();
                break;
            case STANDALONE:
                dropConfiguration(removeS3Bucket);
                LOG.info("Terminating instance");
                terminateInstance();
                break;
        }
    }

    private void dropConfiguration(boolean withS3Bucket) {
        if (withS3Bucket) {
            LOG.info("Dropping S3 bucket");
            awsCommunicationService.dropS3Bucket(getConfiguration().getS3Bucket());
        }
        LOG.info("Dropping DB data");
        dropDbData();
    }



    private void terminateInstance() {
        terminateInstance(SystemUtils.getInstanceId());
    }

    private void terminateInstance(String... instanceIds) {
        ec2.terminateInstances(new TerminateInstancesRequest().withInstanceIds(instanceIds));
    }

    private void dropDbData() {
        for (String tableToDrop : tables) {
            dropTable(tableToDrop);
        }
    }

    private void dropTable(String tableName) {
        Table tableToDelete = dynamoDB.getTable(AmazonConfigProvider.getDynamoDbPrefix() + tableName);
        tableToDelete.delete();
        try {
            tableToDelete.waitForDelete();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private Configuration getConfiguration(){
        return StreamSupport.stream(configurationRepository.findAll().spliterator(), false).findFirst().get();
    }
}
