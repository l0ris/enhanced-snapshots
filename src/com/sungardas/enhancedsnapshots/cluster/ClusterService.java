package com.sungardas.enhancedsnapshots.cluster;




public interface ClusterService {

    /**
     * Removes all the additional infrastructure created to support multi-node mode
     */
    void removeClusterInfrastructure ();

}
