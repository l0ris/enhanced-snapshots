package com.sungardas.enhancedsnapshots.aws;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
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
import com.sungardas.enhancedsnapshots.service.CryptoService;

import org.socialsignin.spring.data.dynamodb.repository.config.EnableDynamoDBRepositories;
import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("dev")
@EnableDynamoDBRepositories(basePackages = "com.sungardas.enhancedsnapshots.aws.dynamodb.repository", dynamoDBMapperConfigRef = "dynamoDBMapperConfig")
public class AmazonConfigProviderDEV {


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


    @Bean(name = "retryInterceptor")
    public RetryInterceptor retryInterceptor() {
        return new RetryInterceptor();
    }

    @Bean
    public AWSCredentials amazonAWSCredentials() {
        String accessKey = cryptoService.decrypt(instanceId, amazonAWSAccessKey);
        String secretKey = cryptoService.decrypt(instanceId, amazonAWSSecretKey);
        return new BasicAWSCredentials(accessKey, secretKey);
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
        builder.withTableNameOverride(DynamoDBMapperConfig.TableNameOverride
                .withTableNamePrefix(AmazonConfigProvider.getDynamoDbPrefix("DEV")));
        return builder.build();
    }

    @Bean
    public ProxyFactoryBean amazonDynamoDbMapperProxy() {
        ProxyFactoryBean proxyFactoryBean = new ProxyFactoryBean();

        proxyFactoryBean.setTarget(dynamoDBMapper());
        proxyFactoryBean.setInterceptorNames("retryInterceptor");

        return proxyFactoryBean;
    }

    private DynamoDBMapper dynamoDBMapper() {
        return new DynamoDBMapper(amazonDynamoDB(), dynamoDBMapperConfig());
    }


    @Bean(name = "dynamoDB")
    public AmazonDynamoDB amazonDynamoDB() {
        AmazonDynamoDB amazonDynamoDB = new AmazonDynamoDBClient(amazonAWSCredentials());
        amazonDynamoDB.setRegion(Region.getRegion(Regions.fromName(region)));
        return amazonDynamoDB;
    }

    private AmazonEC2 amazonEC2() {
        AmazonEC2 amazonEC2 = new AmazonEC2Client(amazonAWSCredentials());
        amazonEC2.setRegion(Region.getRegion(Regions.fromName(region)));
        return amazonEC2;
    }

    private AmazonS3 amazonS3() {
        AmazonS3 amazonS3 = new AmazonS3Client(amazonAWSCredentials());
        Region current = Region.getRegion(Regions.fromName(region));
        if (!current.equals(Region.getRegion(Regions.US_EAST_1))) {
            amazonS3.setRegion(current);
        }
        return amazonS3;
    }
}
