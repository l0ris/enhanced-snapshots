package com.sungardas.enhancedsnapshots.aws.dynamodb.repository;

import com.sungardas.enhancedsnapshots.aws.dynamodb.model.SnapshotEntry;

import org.socialsignin.spring.data.dynamodb.repository.EnableScan;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

@EnableScan
public interface SnapshotRepository extends CrudRepository<SnapshotEntry, String> {
    List<SnapshotEntry> findByVolumeId(String volumeId);
}
