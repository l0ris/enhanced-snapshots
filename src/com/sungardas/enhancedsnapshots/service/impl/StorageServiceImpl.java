package com.sungardas.enhancedsnapshots.service.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;

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
    public static final int BYTES_IN_MEGABYTE = 1000000;

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
        FileInputStream fis = null;
        FileOutputStream fos = null;

        try {
            fis = new FileInputStream(source);
            fos = new FileOutputStream(destination);

            byte[] buffer = new byte[BYTES_IN_MEGABYTE];
            int noOfBytes;

            long total = 0;

            LOG.info("Copying from {} to {} started", source, destination);

            while ((noOfBytes = fis.read(buffer)) != -1) {
                if (Thread.interrupted()) {
                    throw new EnhancedSnapshotsInterruptedException("Task interrupted");
                }
                fos.write(buffer, 0, noOfBytes);
                total += noOfBytes;
                dto.setCopyingProgress(total);
                notificationService.notifyAboutTaskProgress(dto);
            }

            LOG.info("Copying from {} to {} finished: {}", source, destination, total);
        } finally {
            if (fis != null) {
                fis.close();
            }
            if (fos != null) {
                fos.close();
            }

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
