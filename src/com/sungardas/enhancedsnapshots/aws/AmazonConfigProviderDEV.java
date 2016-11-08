package com.sungardas.enhancedsnapshots.aws;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.autoscaling.AmazonAutoScaling;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancing;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.sungardas.enhancedsnapshots.service.CryptoService;
import com.sungardas.enhancedsnapshots.service.impl.CryptoServiceImpl;
import org.socialsignin.spring.data.dynamodb.repository.config.EnableDynamoDBRepositories;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("dev")
@EnableDynamoDBRepositories(basePackages = "com.sungardas.enhancedsnapshots.aws.dynamodb.repository", dynamoDBMapperConfigRef = "dynamoDBMapperConfig")
public class AmazonConfigProviderDEV extends AmazonConfigProvider {


    @Value("${amazon.aws.accesskey}")
    private String amazonAWSAccessKey;

    @Value("${amazon.aws.secretkey}")
    private String amazonAWSSecretKey;

    @Value("${sungardas.worker.configuration}")
    private String configurationId;

    @Value("${amazon.aws.region}")
    private String region;

    private CryptoService cryptoService = new CryptoServiceImpl();
    private AWSCredentials awsCredentials;


    @Bean
    public AWSCredentials awsCredentials() {
        if(awsCredentials == null) {
            String accessKey = cryptoService.decrypt(configurationId, amazonAWSAccessKey);
            String secretKey = cryptoService.decrypt(configurationId, amazonAWSSecretKey);
            awsCredentials = new BasicAWSCredentials(accessKey, secretKey);
        }
        return awsCredentials;
    }

    @Bean(name = "dynamoDB")
    @Override
    public AmazonDynamoDB amazonDynamoDB() {
        AmazonDynamoDB amazonDynamoDB = new AmazonDynamoDBClient(awsCredentials());
        amazonDynamoDB.setRegion(getRegion());
        return amazonDynamoDB;
    }

    @Override
    protected AmazonEC2 amazonEC2() {
        AmazonEC2 amazonEC2 = new AmazonEC2Client(awsCredentials());
        amazonEC2.setRegion(getRegion());
        return amazonEC2;
    }

    @Override
    protected AmazonS3 amazonS3() {
        AmazonS3 amazonS3 = new AmazonS3Client(awsCredentials());
        Region current = getRegion();
        if (!current.equals(Region.getRegion(Regions.US_EAST_1))) {
            amazonS3.setRegion(current);
        }
        return amazonS3;
    }

    @Override
    protected AmazonSNS amazonSNSClient() {
        AmazonSNSClient snsClient = new AmazonSNSClient(awsCredentials());
        snsClient.setRegion(getRegion());
        return snsClient;
    }

    @Override
    protected AmazonSQS amazonSQSClient() {
        AmazonSQSClient sqsClient = new AmazonSQSClient(awsCredentials());
        sqsClient.setRegion(getRegion());
        return sqsClient;
    }

    @Override
    protected AmazonAutoScaling autoScalingClient() {
        AmazonAutoScalingClient autoScalingClient = new AmazonAutoScalingClient(awsCredentials());
        autoScalingClient.setRegion(getRegion());
        return autoScalingClient;
    }

    @Override
    protected AmazonElasticLoadBalancing elasticLoadBalancingClient() {
        AmazonElasticLoadBalancingClient elasticLoadBalancingClient = new AmazonElasticLoadBalancingClient(awsCredentials());
        elasticLoadBalancingClient.setRegion(getRegion());
        return elasticLoadBalancingClient;
    }

    @Override
    protected AmazonCloudFormation amazonCloudFormationClient() {
        AmazonCloudFormation amazonCloudFormation = new AmazonCloudFormationClient(awsCredentials());
        amazonCloudFormation.setRegion(getRegion());
        return amazonCloudFormation;
    }

    protected AmazonCloudWatch cloudWatchClient() {
        AmazonCloudWatchClient cloudWatchClient = new AmazonCloudWatchClient(awsCredentials());
        cloudWatchClient.setRegion(getRegion());
        return cloudWatchClient;
    }

    @Override
    protected Region getRegion (){
        return Region.getRegion(Regions.fromName(region));
    }

    public static String getDynamoDbPrefix(String systemId) {
        return "ENHANCEDSNAPSHOTS_" + systemId + "_";
    }

}
