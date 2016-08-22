package com.sungardas.enhancedsnapshots.tasks.executors;

import com.amazonaws.services.ec2.model.Volume;
import com.sungardas.enhancedsnapshots.aws.dynamodb.model.TaskEntry;
import com.sungardas.enhancedsnapshots.dto.TaskProgressDto;
import com.sungardas.enhancedsnapshots.exception.EnhancedSnapshotsInterruptedException;
import com.sungardas.enhancedsnapshots.exception.EnhancedSnapshotsTaskInterruptedException;
import com.sungardas.enhancedsnapshots.service.AWSCommunicationService;
import com.sungardas.enhancedsnapshots.service.NotificationService;
import com.sungardas.enhancedsnapshots.service.TaskService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Abstract class for amazon AWS task executors
 */
public abstract class AbstractAWSVolumeTaskExecutor implements TaskExecutor {

    private static final Logger LOG = LogManager.getLogger(AbstractAWSVolumeTaskExecutor.class);

    @Autowired
    private AWSCommunicationService awsCommunication;

    @Autowired
    private TaskService taskService;

    @Autowired
    private NotificationService notificationService;

    /**
     * Detach and delete temp volume
     *
     * @param tempVolume
     * @param dto notification transfer object
     */
    protected void deleteTempVolume(Volume tempVolume, TaskProgressDto dto) {
        if (tempVolume != null && awsCommunication.volumeExists(tempVolume.getVolumeId())) {
            tempVolume = awsCommunication.syncVolume(tempVolume);
            if (tempVolume.getAttachments().size() != 0) {
                dto.setMessage("Detaching temp volume");
                dto.addProgress(10);
                notificationService.notifyAboutTaskProgress(dto);
                awsCommunication.detachVolume(tempVolume);
            }
            awsCommunication.deleteSnapshot(tempVolume.getSnapshotId());
            dto.setMessage("Deleting temp volume");
            dto.addProgress(10);
            notificationService.notifyAboutTaskProgress(dto);
            awsCommunication.deleteVolume(tempVolume);
        }
    }

    /**
     * Check if task was interrupted
     *
     * @param taskEntry task
     * @throws EnhancedSnapshotsInterruptedException     if context is closing
     * @throws EnhancedSnapshotsTaskInterruptedException if task has been canceled
     */
    protected void checkThreadInterruption(TaskEntry taskEntry) {
        if (Thread.interrupted()) {
            LOG.info("Task {} was interrupted.", taskEntry.getId());
            throw new EnhancedSnapshotsInterruptedException("Task interrupted");
        }
        if (taskService.isCanceled(taskEntry.getId())) {
            LOG.info("Task {} was canceled.", taskEntry.getId());
            throw new EnhancedSnapshotsTaskInterruptedException("Task canceled");
        }
    }
}
