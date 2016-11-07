package com.sungardas.enhancedsnapshots.aws;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
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

    protected Region getRegion (){
        return Region.getRegion(Regions.fromName(region));
    }

    public static String getDynamoDbPrefix(String systemId) {
        return "ENHANCEDSNAPSHOTS_" + systemId + "_";
    }

}
