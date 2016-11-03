package com.sungardas.enhancedsnapshots.aws.dynamodb.repository;

import com.sungardas.enhancedsnapshots.aws.dynamodb.model.TaskEntry;
import com.sungardas.enhancedsnapshots.enumeration.TaskProgress;
import org.socialsignin.spring.data.dynamodb.repository.EnableScan;
import org.socialsignin.spring.data.dynamodb.repository.EnableScanCount;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

@EnableScan
@EnableScanCount
public interface TaskRepository extends CrudRepository<TaskEntry, String> {
    List<TaskEntry> findByStatusAndRegular(String status, String regular);

    List<TaskEntry> findByStatusAndRegularAndWorker(String status, String regular, String worker);

    List<TaskEntry> findByStatusNotAndRegular(String status, String regular);

    List<TaskEntry> findByRegularAndVolume(String regular, String volumeId);

    List<TaskEntry> findByRegularAndEnabled(String regular, String enabled);

    List<TaskEntry> findByVolumeAndTypeAndOptions(String volumeId, String type, String options);

    List<TaskEntry> findByRegularAndCompleteTimeGreaterThanEqual(String regular, long completeTime);

    List<TaskEntry> findByRegular(String regular);

    Long countByRegularAndTypeAndStatus(String regular, String type, String status);

    List<TaskEntry> findByWorkerAndProgressNot(String worker, TaskProgress progress);

    List<TaskEntry> findByWorkerIsNull();

    default void save(List<TaskEntry> tasks) {
        tasks.forEach(this::save);
    }
}
