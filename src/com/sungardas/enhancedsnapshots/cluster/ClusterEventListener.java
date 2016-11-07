package com.sungardas.enhancedsnapshots.cluster;

import com.sungardas.enhancedsnapshots.aws.dynamodb.model.EventEntry;

public interface ClusterEventListener {
    void launched(EventEntry eventEntry);

    void terminated(EventEntry eventEntry);
}
