package com.sungardas.enhancedsnapshots.service.impl;

import com.amazonaws.services.ec2.model.Volume;
import com.sungardas.enhancedsnapshots.components.ConfigurationMediator;
import com.sungardas.enhancedsnapshots.dto.CopyingTaskProgressDto;
import com.sungardas.enhancedsnapshots.exception.EnhancedSnapshotsInterruptedException;
import com.sungardas.enhancedsnapshots.exception.EnhancedSnapshotsTaskInterruptedException;
import com.sungardas.enhancedsnapshots.exception.SDFSException;
import com.sungardas.enhancedsnapshots.service.NotificationService;
import com.sungardas.enhancedsnapshots.service.StorageService;
import com.sungardas.enhancedsnapshots.service.TaskService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;

@Service
@DependsOn("SystemService")
@Profile("prod")
public class StorageServiceImpl implements StorageService {

    public static final Logger LOG = LogManager.getLogger(StorageServiceImpl.class);

    private String mountPoint;
    public static final int BYTES_IN_MEGABYTE = 1000000;
    private static final String devNamePrefix = "/dev/xvd";

    @Autowired
    private ConfigurationMediator configurationMediator;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private TaskService taskService;

    @PostConstruct
    public void init() {
        this.mountPoint = configurationMediator.getSdfsMountPoint();
    }

    @Override
    public void deleteFile(String fileName) {
        Path path = Paths.get(mountPoint, fileName);
        File file = path.toFile();
        if (file.exists()) {
            file.delete();
        } else {
            LOG.error("File not found " + file.getAbsolutePath());
            throw new SDFSException("File not found " + file.getAbsolutePath());
        }
    }

    private void copyWithCpCommand(String source, String destination, CopyingTaskProgressDto dto, final String taskId) throws IOException {
        try {
            LOG.info("Copying from {} to {} started", source, destination);
            File dest = new File(destination);
            ProcessBuilder builder = new ProcessBuilder("cp", source, destination);
            Process process = builder.start();
            while (!process.waitFor(3, TimeUnit.SECONDS)) {
                if (Thread.interrupted()) {
                    process.destroy();
                    throw new EnhancedSnapshotsInterruptedException("Task interrupted");
                }
                if (taskService.isCanceled(taskId)) {
                    process.destroy();
                    throw new EnhancedSnapshotsTaskInterruptedException("Task canceled");
                }
                dto.setCopyingProgress(dest.length());
                notificationService.notifyAboutTaskProgress(dto);
            }
            switch (process.exitValue()) {
                case 0:
                    LOG.info("Copying from {} to {} finished: {}", source, destination, dest.length());
                    break;
                default: {
                    LOG.error("Failed to copy data from {} to {} ", source, destination);
                    try (BufferedReader input = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                        StringBuilder errorMessage = new StringBuilder();
                        for (String line; (line = input.readLine()) != null; ) {
                            errorMessage.append(line);
                        }
                        LOG.error(errorMessage.toString());
                        throw new EnhancedSnapshotsInterruptedException(errorMessage.toString());
                    }
                }
            }
        } catch (InterruptedException e) {
            throw new EnhancedSnapshotsInterruptedException(e);
        }
    }

    @Override
    public void copyData(String source, String destination, CopyingTaskProgressDto dto, String taskId) throws IOException {
        // for restoring we need to use java binary copy approach, so we could determine size of copied data
        // since otherwise progress bar will always show that 0 mb copied till restore is finished
        if (destination.startsWith(devNamePrefix)){
            javaBinaryCopy(source, destination, dto, taskId);
            return;
        }
        // copy with native commands shows best performance
        // so it's better to use this approach for backups
        copyWithCpCommand(source, destination, dto, taskId);
    }

    private void javaBinaryCopy(String source, String destination, CopyingTaskProgressDto dto, final String taskId) throws IOException {
        try (FileInputStream fis = new FileInputStream(source);
             FileOutputStream fos = new FileOutputStream(destination)) {
            byte[] buffer = new byte[BYTES_IN_MEGABYTE];
            int noOfBytes;
            long total = 0;
            LOG.info("Copying from {} to {} started", source, destination);
            while ((noOfBytes = fis.read(buffer)) != -1) {
                if (Thread.interrupted()) {
                    throw new EnhancedSnapshotsInterruptedException("Task interrupted");
                }
                if (taskService.isCanceled(taskId)) {
                    throw new EnhancedSnapshotsTaskInterruptedException("Task canceled");
                }
                fos.write(buffer, 0, noOfBytes);
                total += noOfBytes;
                dto.setCopyingProgress(total);
                notificationService.notifyAboutTaskProgress(dto);
            }
            LOG.info("Copying from {} to {} finished: {}", source, destination, total);
        }
    }

    @Override
    public long getSize(String filename) {
        Path file = Paths.get(filename);
        long size = -1;
        try {
            BasicFileAttributes attrs = Files.readAttributes(file, BasicFileAttributes.class);
            size = attrs.size();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return size;
    }

    @Override
    public long getBackupCreationTime(String filename) {
        Path file = Paths.get(filename);
        long timestamp = -1;
        try {
            BasicFileAttributes attrs = Files.readAttributes(file, BasicFileAttributes.class);
            timestamp = attrs.creationTime().toMillis();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return timestamp;
    }

    @Override
    public String detectFsDevName(Volume volume) {

        String devname = volume.getAttachments().get(0).getDevice();
        File volf = new File(devname);
        if (!volf.exists() || !volf.isFile()) {
            LOG.info(format("Cant find attached source: %s", volume));

            devname = devNamePrefix + devname.substring(devname.length() - 1);
            LOG.info(format("New source path : %s", devname));
        }
        return devname;
    }

}
