package com.sungardas.enhancedsnapshots.service.impl;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;



@Service
@Profile("dev")
public class AWSCommunicationServiceDev extends AWSCommunicationServiceImpl {


    @Override
    public String getCurrentAvailabilityZone() {
        return describeAvailabilityZonesForCurrentRegion().get(0).getZoneName();
    }

    @Override
    public void dropS3Bucket(String bucketName) {
        return;
    }

}
