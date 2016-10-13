package com.sungardas.enhancedsnapshots.aws;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.ClasspathPropertiesFileCredentialsProvider;
import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.autoscaling.AmazonAutoScaling;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;
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
import com.sungardas.enhancedsnapshots.components.RetryInterceptor;

import com.sungardas.enhancedsnapshots.util.SystemUtils;
import org.socialsignin.spring.data.dynamodb.repository.config.EnableDynamoDBRepositories;
import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("prod")
@EnableDynamoDBRepositories(basePackages = "com.sungardas.enhancedsnapshots.aws.dynamodb.repository", dynamoDBMapperConfigRef = "dynamoDBMapperConfig")
public class AmazonConfigProvider {
    private AWSCredentials awsCredentials;

    @Bean(name = "retryInterceptor")
    public RetryInterceptor retryInterceptor() {
        return new RetryInterceptor();
    }

    public AWSCredentials awsCredentials() {
        if (awsCredentials == null) {
            awsCredentials = new InstanceProfileCredentialsProvider().getCredentials();
        }
        return awsCredentials;
    }

    @Bean(name = "amazonDynamoDB")
    public ProxyFactoryBean amazonDynamoDbProxy() {
        ProxyFactoryBean proxyFactoryBean = new ProxyFactoryBean();

        proxyFactoryBean.setTarget(amazonDynamoDB());
        proxyFactoryBean.setInterceptorNames("retryInterceptor");
        return proxyFactoryBean;
    }

    @Bean
    public ProxyFactoryBean amazonEC2Proxy() {
        ProxyFactoryBean proxyFactoryBean = new ProxyFactoryBean();

        proxyFactoryBean.setTarget(amazonEC2());
        proxyFactoryBean.setInterceptorNames("retryInterceptor");

        return proxyFactoryBean;
    }

    @Bean
    public ProxyFactoryBean amazonS3Proxy() {
        ProxyFactoryBean proxyFactoryBean = new ProxyFactoryBean();

        proxyFactoryBean.setTarget(amazonS3());
        proxyFactoryBean.setInterceptorNames("retryInterceptor");

        return proxyFactoryBean;
    }

    @Bean
    public DynamoDBMapperConfig dynamoDBMapperConfig() {
        DynamoDBMapperConfig.Builder builder = new DynamoDBMapperConfig.Builder();
        builder.withTableNameOverride(DynamoDBMapperConfig.TableNameOverride.
                withTableNamePrefix(getDynamoDbPrefix()));
        return builder.build();
    }

    @Bean
    public ProxyFactoryBean amazonDynamoDbMapperProxy() {
        ProxyFactoryBean proxyFactoryBean = new ProxyFactoryBean();

        proxyFactoryBean.setTarget(dynamoDBMapper());
        proxyFactoryBean.setInterceptorNames("retryInterceptor");

        return proxyFactoryBean;
    }

    @Bean(name = "dynamoDB")
    public AmazonDynamoDB amazonDynamoDB() {
        AmazonDynamoDB amazonDynamoDB = new AmazonDynamoDBClient(awsCredentials());
        amazonDynamoDB.setRegion(getRegion());
        return amazonDynamoDB;
    }

    private DynamoDBMapper dynamoDBMapper() {
        return new DynamoDBMapper(amazonDynamoDB(), dynamoDBMapperConfig());
    }

    private AmazonEC2 amazonEC2() {
        AmazonEC2 amazonEC2 = new AmazonEC2Client(awsCredentials());
        amazonEC2.setRegion(getRegion());
        return amazonEC2;
    }

    private AmazonS3 amazonS3() {
        AmazonS3 amazonS3 = new AmazonS3Client(awsCredentials());
        Region current = getRegion();
        if (!current.equals(Region.getRegion(Regions.US_EAST_1))) {
            amazonS3.setRegion(current);
        }
        return amazonS3;
    }

    @Bean
    public ProxyFactoryBean amazonAutoScalingProxy() {
        ProxyFactoryBean proxyFactoryBean = new ProxyFactoryBean();
        proxyFactoryBean.setTarget(autoScalingClient());
        proxyFactoryBean.setInterceptorNames("retryInterceptor");
        return proxyFactoryBean;
    }

    @Bean
    public ProxyFactoryBean amazonELBProxy() {
        ProxyFactoryBean proxyFactoryBean = new ProxyFactoryBean();
        proxyFactoryBean.setTarget(elasticLoadBalancingClient());
        proxyFactoryBean.setInterceptorNames("retryInterceptor");
        return proxyFactoryBean;
    }

    @Bean
    public ProxyFactoryBean amazonCloudWatch() {
        ProxyFactoryBean proxyFactoryBean = new ProxyFactoryBean();
        proxyFactoryBean.setTarget(cloudWatchClient());
        proxyFactoryBean.setInterceptorNames("retryInterceptor");
        return proxyFactoryBean;
    }

    @Bean
    public ProxyFactoryBean amazonSNS() {
        ProxyFactoryBean proxyFactoryBean = new ProxyFactoryBean();
        proxyFactoryBean.setTarget(amazonSNSClient());
        proxyFactoryBean.setInterceptorNames("retryInterceptor");
        return proxyFactoryBean;
    }

    @Bean
    public ProxyFactoryBean amazonSQS() {
        ProxyFactoryBean proxyFactoryBean = new ProxyFactoryBean();
        proxyFactoryBean.setTarget(amazonSQSClient());
        proxyFactoryBean.setInterceptorNames("retryInterceptor");
        return proxyFactoryBean;
    }

    private AmazonSNS amazonSNSClient() {
        AmazonSNSClient snsClient = new AmazonSNSClient(awsCredentials());
        snsClient.setRegion(getRegion());
        return snsClient;
    }

    private AmazonSQS amazonSQSClient() {
        AmazonSQSClient sqsClient = new AmazonSQSClient(awsCredentials());
        sqsClient.setRegion(getRegion());
        return sqsClient;
    }
    private AmazonAutoScaling autoScalingClient() {
        AmazonAutoScalingClient autoScalingClient = new AmazonAutoScalingClient(awsCredentials());
        autoScalingClient.setRegion(getRegion());
        return autoScalingClient;
    }

    private AmazonElasticLoadBalancing elasticLoadBalancingClient() {
        AmazonElasticLoadBalancingClient elasticLoadBalancingClient = new AmazonElasticLoadBalancingClient(awsCredentials());
        elasticLoadBalancingClient.setRegion(getRegion());
        return elasticLoadBalancingClient;
    }

    private AmazonCloudWatch cloudWatchClient() {
        AmazonCloudWatchClient cloudWatchClient = new AmazonCloudWatchClient(awsCredentials());
        cloudWatchClient.setRegion(getRegion());
        return cloudWatchClient;
    }

    public static String getDynamoDbPrefix() {
        return getDynamoDbPrefix(SystemUtils.getSystemId());
    }

    public static String getDynamoDbPrefix(String systemId) {
        return "ENHANCEDSNAPSHOTS_" + systemId + "_";
    }

    protected Region getRegion (){
        return Regions.getCurrentRegion();
    }
}