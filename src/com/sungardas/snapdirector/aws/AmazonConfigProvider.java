package com.sungardas.snapdirector.aws;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.sungardas.snapdirector.service.CryptoService;
import org.socialsignin.spring.data.dynamodb.repository.config.EnableDynamoDBRepositories;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.GeneralSecurityException;

@Configuration
@EnableDynamoDBRepositories(basePackages = "com.sungardas.snapdirector.aws.dynamodb.repository")
public class AmazonConfigProvider {
    @Value("${amazon.aws.accesskey}")
    private String amazonAWSAccessKey;

    @Value("${amazon.aws.secretkey}")
    private String amazonAWSSecretKey;

    @Value("${sungardas.worker.configuration}")
    private String instanceId;
    
    @Value("${amazon.aws.region}")
    private String region;

    @Autowired
    private CryptoService cryptoService;


    @Bean
    public AmazonDynamoDB amazonDynamoDB() {
        AmazonDynamoDB amazonDynamoDB = new AmazonDynamoDBClient(amazonAWSCredentials());
        amazonDynamoDB.setRegion(Region.getRegion(Regions.fromName(region)));
        return amazonDynamoDB;
    }

    @Bean
    public AWSCredentials amazonAWSCredentials() {
        String accessKey = cryptoService.decrypt(instanceId, amazonAWSAccessKey);
        String secretKey = cryptoService.decrypt(instanceId, amazonAWSSecretKey);
        return new BasicAWSCredentials(accessKey, secretKey);
    }
    
    @Bean
    public AmazonEC2 amazonEC2() {
    	AmazonEC2 amazonEC2 = new AmazonEC2Client(amazonAWSCredentials());
    	amazonEC2.setRegion(Region.getRegion(Regions.fromName(region)));
        return amazonEC2;
    }
    
    @Bean
    public AmazonSQS amazonSQS() {
    	AmazonSQS amazonSQS = new AmazonSQSClient(amazonAWSCredentials());
    	amazonSQS.setRegion(Region.getRegion(Regions.fromName(region)));
        return amazonSQS;
    }

    @Bean
    public AmazonS3 amazonS3() {
        AmazonS3 amazonS3  = new AmazonS3Client(amazonAWSCredentials());
        if(!Regions.fromName(region).equals(Regions.US_EAST_1)) {
            amazonS3.setRegion(Region.getRegion(Regions.fromName(region)));
        }
        return amazonS3;
    }

}