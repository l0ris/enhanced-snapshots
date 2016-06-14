package com.sungardas.enhancedsnapshots.aws.dynamodb.repository;

import java.util.List;

import com.sungardas.enhancedsnapshots.aws.dynamodb.model.TaskEntry;

import org.socialsignin.spring.data.dynamodb.repository.EnableScan;
import org.socialsignin.spring.data.dynamodb.repository.EnableScanCount;
import org.springframework.data.repository.CrudRepository;

@EnableScan
@EnableScanCount
public interface TaskRepository extends CrudRepository<TaskEntry, String> {
    List<TaskEntry> findByStatusAndRegular(String status, String regular);

    List<TaskEntry> findByRegularAndVolume(String regular, String volumeId);

    List<TaskEntry> findByRegularAndEnabled(String regular, String enabled);

    List<TaskEntry> findByVolumeAndTypeAndOptions(String volumeId, String type, String options);

    List<TaskEntry> findByExpirationDateLessThanEqual(String expirationDate);

    List<TaskEntry> findByRegular(String regular);

    Long countByRegularAndTypeAndStatus(String regular, String type, String status);
}
