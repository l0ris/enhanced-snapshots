package com.sungardas.enhancedsnapshots.aws.dynamodb.repository;

import com.sungardas.enhancedsnapshots.aws.dynamodb.model.EventEntry;
import org.socialsignin.spring.data.dynamodb.repository.EnableScan;
import org.socialsignin.spring.data.dynamodb.repository.EnableScanCount;
import org.springframework.data.repository.CrudRepository;

import java.util.List;


@EnableScan
@EnableScanCount
public interface EventsRepository extends CrudRepository<EventEntry, String> {

    List<EventEntry> findByTimeGreaterThan(long time);

}
