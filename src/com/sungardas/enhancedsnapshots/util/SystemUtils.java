package com.sungardas.enhancedsnapshots.util;

import com.amazonaws.util.EC2MetadataUtils;


public class SystemUtils {

    private static final String CLUSTRE_ID = System.getenv("CLUSTER_ID");

    public static String getSystemId() {
        if(CLUSTRE_ID != null){
            return CLUSTRE_ID;
        }
        return EC2MetadataUtils.getInstanceId();
    }

    public static SystemMode getSystemMode() {
        if(CLUSTRE_ID != null){
            return SystemMode.CLUSTER;
        }
        return SystemMode.STANDALONE;
    }


    enum SystemMode{
        STANDALONE, CLUSTER
    }
}
