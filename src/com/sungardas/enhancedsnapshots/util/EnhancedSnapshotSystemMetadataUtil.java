package com.sungardas.enhancedsnapshots.util;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.sungardas.enhancedsnapshots.exception.EnhancedSnapshotsException;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class EnhancedSnapshotSystemMetadataUtil {

    private static final Logger LOG = LogManager.getLogger(EnhancedSnapshotSystemMetadataUtil.class);

    private static final String TEMP_DIRECTORY_PREFIX = "systemBackupFiles";
    private static final String INFO_FILE_NAME = "info";
    private static final String VERSION_KEY = "version";
    private static final String TEMP_FILE_SUFFIX = "ZIP";

    private static final ObjectMapper objectMapper = new ObjectMapper();


    private EnhancedSnapshotSystemMetadataUtil() {
    }

    /**
     * Return true when S3 bucket contains SDFS backup, false otherwise
     */
    public static boolean containsSdfsMetadata(String sBucket, String sdfsBackupFileName, AmazonS3 amazonS3) {
        if(!isBucketExits(sBucket, amazonS3)){
            return false;
        }
        ListObjectsRequest request = new ListObjectsRequest().withBucketName(sBucket).withPrefix(sdfsBackupFileName);
        return amazonS3.listObjects(request).getObjectSummaries().size() > 0;
    }

    public static boolean isBucketExits(String s3Bucket, AmazonS3 amazonS3) {
        try {
            String location = amazonS3.getBucketLocation(s3Bucket);
            return location != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Method for defining application version, which created system backup
     *
     * @param tempDirectory directory to which was unzipped system backup
     * @return application version
     */
    public static String getBackupVersion(final Path tempDirectory) {
        Path infoFile = Paths.get(tempDirectory.toString(), INFO_FILE_NAME);
        if (infoFile.toFile().exists()) {
            try (FileInputStream fileInputStream = new FileInputStream(infoFile.toFile())) {
                HashMap<String, String> info = objectMapper.readValue(fileInputStream, HashMap.class);
                if (info.containsKey(VERSION_KEY)) {
                    return info.get(VERSION_KEY);
                } else {
                    LOG.error("Invalid info file formant");
                    throw new EnhancedSnapshotsException("Invalid info file formant");
                }
            } catch (IOException e) {
                LOG.error("Failed to parse info file");
                LOG.error(e);
                throw new EnhancedSnapshotsException(e);
            }
        }
        return "0.0.1";
    }

    /**
     * Method for defining application version, which created system backup
     *
     * @param bucketName
     * @param sdfsBackupFileName
     * @param amazonS3
     * @return application version
     */
    public static String getBackupVersion(final String bucketName, String sdfsBackupFileName, AmazonS3 amazonS3) {
        Path tempDirectory = null;
        String version = null;
        if (containsSdfsMetadata(bucketName, sdfsBackupFileName, amazonS3)) {
            try {
                tempDirectory = Files.createTempDirectory(TEMP_DIRECTORY_PREFIX);
                EnhancedSnapshotSystemMetadataUtil.downloadFromS3(tempDirectory,
                        bucketName, sdfsBackupFileName, amazonS3);

                version = getBackupVersion(tempDirectory);
            } catch (IOException e) {
                LOG.error(e);
            } finally {
                if (tempDirectory != null) {
                    try {
                        FileUtils.deleteDirectory(tempDirectory.toFile());
                    } catch (IOException e) {

                    }
                }
            }
        }
        return version;
    }

    public static void downloadFromS3(Path tempDirectory, String s3Bucket, String sdfsBackupFileName, AmazonS3 amazonS3) throws IOException {
        // download
        LOG.info("-Download");
        GetObjectRequest getObjectRequest = new GetObjectRequest(s3Bucket, sdfsBackupFileName);
        S3Object s3object = amazonS3.getObject(getObjectRequest);

        Path tempFile = Files.createTempFile(TEMP_DIRECTORY_PREFIX, TEMP_FILE_SUFFIX);
        Files.copy(s3object.getObjectContent(), tempFile, StandardCopyOption.REPLACE_EXISTING);

        LOG.info("  -Unzip");
        //unzip
        try (FileInputStream fileInputStream = new FileInputStream(tempFile.toFile());
             ZipInputStream zipInputStream = new ZipInputStream(fileInputStream)) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                // in case entry is directory system backup relates to version 0.0.1
                // copy /etc/sdfs/awspool-volume-cfg.xml to temp dir
                if (entry.isDirectory()) {
                    zipInputStream.getNextEntry();
                    zipInputStream.getNextEntry();
                    Path dest = Paths.get(tempDirectory.toString(), "awspool-volume-cfg.xml");
                    Files.copy(zipInputStream, dest, StandardCopyOption.REPLACE_EXISTING);
                    break;
                }
                Path dest = Paths.get(tempDirectory.toString(), entry.getName());
                Files.copy(zipInputStream, dest, StandardCopyOption.REPLACE_EXISTING);
            }
        }

        //cleanup
        tempFile.toFile().delete();
    }
}
