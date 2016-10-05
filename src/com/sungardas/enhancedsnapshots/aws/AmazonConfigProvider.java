package com.sungardas.enhancedsnapshots.aws;

import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;

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
    private InstanceProfileCredentialsProvider  credentialsProvider;

    @Bean(name = "retryInterceptor")
    public RetryInterceptor retryInterceptor() {
        return new RetryInterceptor();
    }

    @Bean
    public InstanceProfileCredentialsProvider amazonCredentialsProvider() {
        if(credentialsProvider==null) {
            credentialsProvider = new InstanceProfileCredentialsProvider();
        }
        return credentialsProvider;
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
        AmazonDynamoDB amazonDynamoDB = new AmazonDynamoDBClient(amazonCredentialsProvider());
        amazonDynamoDB.setRegion(Regions.getCurrentRegion());
        return amazonDynamoDB;
    }

    private DynamoDBMapper dynamoDBMapper() {
        return new DynamoDBMapper(amazonDynamoDB(), dynamoDBMapperConfig());
    }

    private AmazonEC2 amazonEC2() {
        AmazonEC2 amazonEC2 = new AmazonEC2Client(amazonCredentialsProvider());
        amazonEC2.setRegion(Regions.getCurrentRegion());
        return amazonEC2;
    }

    private AmazonS3 amazonS3() {
        AmazonS3 amazonS3 = new AmazonS3Client(amazonCredentialsProvider());
        Region current = Regions.getCurrentRegion();
        if (!current.equals(Region.getRegion(Regions.US_EAST_1))) {
            amazonS3.setRegion(current);
        }
        return amazonS3;
    }

    public static String getDynamoDbPrefix() {
        return getDynamoDbPrefix(SystemUtils.getSystemId());
    }

    public static String getDynamoDbPrefix(String systemId) {
        return "ENHANCEDSNAPSHOTS_" + systemId + "_";
    }
}