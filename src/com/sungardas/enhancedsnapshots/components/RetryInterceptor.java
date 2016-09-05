package com.sungardas.enhancedsnapshots.components;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

public class RetryInterceptor implements MethodInterceptor {

    @Autowired
    private AmazonS3 amazonS3;

    @Autowired
    private ConfigurationMediator configurationMediator;

    @Value("${amazon.s3.default.region}")
    private String defaultS3Region;

    private static final Logger LOG = LogManager.getLogger(RetryInterceptor.class);

    @Override
    public Object invoke(MethodInvocation methodInvocation) throws Throwable {
        Throwable throwable = null;
        for (int i = 0; i < configurationMediator.getAmazonRetryCount(); i++) {
            try {
                return methodInvocation.proceed();
            } catch (AmazonS3Exception e) {
                try {
                    // some requests can be executed only with endpoint in us-east-1 region
                    amazonS3.setRegion(com.amazonaws.regions.Region.getRegion(Regions.fromName(defaultS3Region)));
                    methodInvocation.proceed();
                } finally {
                    // setting back correct region
                    amazonS3.setRegion(Region.getRegion(Regions.fromName(configurationMediator.getRegion())));
                }
            } catch (AmazonServiceException e) {
                if (e.getErrorType() == AmazonServiceException.ErrorType.Client) {
                    throw e;
                }
                LOG.debug("Exception while method invocation: ", methodInvocation.getMethod());
                LOG.debug("Amazon service exception, try: " + i, e);
                throwable = e;
                Thread.sleep(configurationMediator.getAmazonRetrySleep());
            }
        }
        throw throwable;
    }
}
