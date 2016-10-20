package com.sungardas.enhancedsnapshots.cluster;




public interface ClusterConfigurationService {

    /**
     * Removes all the additional infrastructure created to support multi-node mode
     */
    void removeClusterInfrastructure ();

    /**
     * Configures additional infrastructure to support multi-node mode
     */
    void configureClusterInfrastructure ();

    void updateCloudWatchMetric();
}
