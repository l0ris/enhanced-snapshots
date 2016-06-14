package com.sungardas.destroy;

import javax.annotation.PostConstruct;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.amazonaws.util.EC2MetadataUtils;
import com.sungardas.enhancedsnapshots.aws.AmazonConfigProvider;
import com.sungardas.enhancedsnapshots.aws.dynamodb.model.Configuration;
import com.sungardas.enhancedsnapshots.aws.dynamodb.repository.ConfigurationRepository;
import com.sungardas.enhancedsnapshots.service.AWSCommunicationService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;

public class RemoveAppConfiguration {

    @Value("${enhancedsnapshots.db.tables}")
    private String[] tables;

    @Autowired
    @Qualifier("amazonDynamoDB")
    private AmazonDynamoDB db;

    @Autowired
    private AmazonEC2 ec2;

    @Autowired
    private ConfigurationRepository configurationRepository;

    @Autowired
    private AWSCommunicationService awsCommunicationService;

    private DynamoDB dynamoDB;

    private String configurationId;

    @PostConstruct
    private void init() {
        configurationId = EC2MetadataUtils.getInstanceId();
        dynamoDB = new DynamoDB(db);
        dropConfiguration();
    }

    private void dropConfiguration() {
        awsCommunicationService.dropS3Bucket(getConfiguration().getS3Bucket());
        dropDbData();
        terminateInstance();
    }



    private void terminateInstance() {
        ec2.terminateInstances(new TerminateInstancesRequest().withInstanceIds(configurationId));
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
        return configurationRepository.findOne(configurationId);
    }
}
