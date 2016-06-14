package com.sungardas.enhancedsnapshots.service.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import com.amazonaws.services.ec2.model.Volume;
import com.sungardas.enhancedsnapshots.components.ConfigurationMediator;
import com.sungardas.enhancedsnapshots.dto.CopyingTaskProgressDto;
import com.sungardas.enhancedsnapshots.exception.EnhancedSnapshotsInterruptedException;
import com.sungardas.enhancedsnapshots.exception.SDFSException;
import com.sungardas.enhancedsnapshots.service.NotificationService;
import com.sungardas.enhancedsnapshots.service.StorageService;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import static java.lang.String.format;

@Service
@DependsOn("SystemService")
@Profile("prod")
public class StorageServiceImpl implements StorageService {

    public static final Logger LOG = LogManager.getLogger(StorageServiceImpl.class);

    private String mountPoint;

    @Autowired
    private ConfigurationMediator configurationMediator;

    @Autowired
    private NotificationService notificationService;

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

    @Override
    public void javaBinaryCopy(String source, String destination, CopyingTaskProgressDto dto) throws IOException {
        try {
            LOG.info("Copying from {} to {} started", source, destination);
            File dest = new File(destination);
            ProcessBuilder builder = new ProcessBuilder("cp", source, destination);
            builder.redirectErrorStream(true);
            Process process = builder.start();
            while (!process.waitFor(5, TimeUnit.SECONDS)) {
                if (Thread.interrupted()) {
                    process.destroy();
                    throw new EnhancedSnapshotsInterruptedException("Task interrupted");
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
                    BufferedReader input = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                    StringBuilder errorMessage = new StringBuilder();
                    for (String line; (line = input.readLine()) != null; ) {
                        errorMessage.append(line);
                    }
                    LOG.error(errorMessage.toString());
                    throw new EnhancedSnapshotsInterruptedException(errorMessage.toString());
                }
            }
        } catch (InterruptedException e) {
            throw new EnhancedSnapshotsInterruptedException(e);
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

            devname = "/dev/xvd" + devname.substring(devname.length() - 1);
            LOG.info(format("New source path : %s", devname));
        }
        return devname;
    }

}
