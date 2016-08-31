package com.sungardas.enhancedsnapshots.service;

import com.amazonaws.services.ec2.model.Volume;
import com.sungardas.enhancedsnapshots.dto.CopyingTaskProgressDto;

import java.io.IOException;

public interface StorageService {

    void deleteFile(String fileName);

    long getSize(String filename);

    long getBackupCreationTime(String filename);

    String detectFsDevName(Volume volume);

    void copyData(String source, String destination, CopyingTaskProgressDto dto, String taskId) throws IOException, InterruptedException;
}
