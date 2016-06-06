package com.sungardas.enhancedsnapshots.aws.dynamodb.repository;

import com.sungardas.enhancedsnapshots.aws.dynamodb.model.SnapshotEntry;

import org.socialsignin.spring.data.dynamodb.repository.EnableScan;
import org.springframework.data.repository.CrudRepository;

@EnableScan
public interface SnapshotRepository extends CrudRepository<SnapshotEntry, String> {
}
