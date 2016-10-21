package com.sungardas.enhancedsnapshots.service;

import com.sun.management.OperatingSystemMXBean;
import com.sungardas.enhancedsnapshots.aws.dynamodb.model.BackupEntry;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.UUID;

public interface SDFSStateService {

    // constant value
    long BYTES_IN_GB = 1_073_741_824;


    /**
     * Returns max sdfs volume size for current system in GB
     *
     * @param systemReservedRam    in bytes
     * @param volumeSizePerGbOfRam in GB
     * @param sdfsReservedRam      in bytes
     * @return
     */
    static int getMaxVolumeSize(int systemReservedRam, int volumeSizePerGbOfRam, int sdfsReservedRam) {
        OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        //Total RAM - RAM available for Tomcat - reserved
        long totalRAM = osBean.getTotalPhysicalMemorySize() - Runtime.getRuntime().maxMemory() - systemReservedRam - sdfsReservedRam;
        int maxVolumeSize = (int) (totalRAM / BYTES_IN_GB) * volumeSizePerGbOfRam;
        return maxVolumeSize;
    }


    /**
     * Returns count of GB which can be used to increase sdfs local cache
     *
     * @param systemReservedStorage reserved storage in bytes
     * @return
     */
    static int getFreeStorageSpace(int systemReservedStorage) {
        File file = new File("/");
        int maxLocalCacheInGb = (int) ((file.getFreeSpace() - systemReservedStorage) / BYTES_IN_GB);
        return maxLocalCacheInGb;
    }

    Long getBackupTime();

    /**
     * Reconfigure SDFS and restart
     */
    void reconfigureAndRestartSDFS();

    /**
     * Restore SDFS from S3 bucket
     */
    void restoreSDFS();

    /**
     * Start SDFS if it is not running
     */
    void startSDFS();

    /**
     * Stop SDFS if it is not running
     */
    void stopSDFS();

    /**
     * Return true if SDFS is currently runnings, false otherwise
     */
    boolean sdfsIsAvailable();

    /**
     * Expand sdfs volume
     */
    void expandSdfsVolume(String newVolumeSize);

    /**
     * Sync local SDFS metadata with cloud
     */
    void cloudSync();

    /**
     * Returns list of real existing backups from SDFS mount point
     *
     * @return
     */
    List<BackupEntry> getBackupsFromSDFSMountPoint();

    /**
     * Returns sdfs volume id
     *
     * @return
     */

    String getSDFSVolumeId();

    static String generateChunkStoreEncryptionKey(){
        return UUID.randomUUID().toString().replace("-", "");
    }

    static String generateChunkStoreIV(){
        return UUID.randomUUID().toString().replace("-", "").substring(0, 14);
    }

}
