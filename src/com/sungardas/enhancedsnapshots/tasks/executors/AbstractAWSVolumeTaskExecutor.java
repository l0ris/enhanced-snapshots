package com.sungardas.enhancedsnapshots.tasks.executors;

import com.amazonaws.services.ec2.model.Volume;
import com.sungardas.enhancedsnapshots.service.AWSCommunicationService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Abstract class for amazon AWS task executors
 */
public abstract class AbstractAWSVolumeTaskExecutor implements TaskExecutor {

    @Autowired
    private AWSCommunicationService awsCommunication;

    /**
     * Detach and delete temp volume
     *
     * @param tempVolume
     */
    protected void deleteTempVolume(Volume tempVolume) {
        if (tempVolume != null && awsCommunication.volumeExists(tempVolume.getVolumeId())) {
            tempVolume = awsCommunication.syncVolume(tempVolume);
            if (tempVolume.getAttachments().size() != 0) {
                awsCommunication.detachVolume(tempVolume);
            }
            awsCommunication.deleteVolume(tempVolume);
        }

    }
}
