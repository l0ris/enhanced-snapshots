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

    /**
     * Updates nodes number in cluster
     * @param minNodeNumber
     * @param maxNodeNumber
     */
    void updateAutoScalingSettings(int minNodeNumber, int maxNodeNumber);
}
