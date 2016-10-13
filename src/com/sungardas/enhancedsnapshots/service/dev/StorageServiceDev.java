package com.sungardas.enhancedsnapshots.service.dev;

import java.io.IOException;

import com.amazonaws.services.ec2.model.Volume;
import com.sungardas.enhancedsnapshots.dto.CopyingTaskProgressDto;
import com.sungardas.enhancedsnapshots.service.StorageService;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;


@Service
@Profile("dev")
public class StorageServiceDev implements StorageService {
    @Override
    public void deleteFile(final String fileName) {

    }

    @Override
    public long getSize(final String filename) {
        return 0;
    }

    @Override
    public long getBackupCreationTime(final String filename) {
        return 0;
    }

    @Override
    public String detectFsDevName(final Volume volume) {
        return null;
    }

    @Override
    public void copyData(final String source, final String destination, final CopyingTaskProgressDto dto, final String taskId) throws IOException, InterruptedException {

    }
}
