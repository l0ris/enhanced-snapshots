package com.sungardas.snapdirector.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sungardas.snapdirector.aws.dynamodb.model.Snapshot;
import com.sungardas.snapdirector.aws.dynamodb.repository.SnapshotRepository;
import com.sungardas.snapdirector.dto.SnapshotDto;
import com.sungardas.snapdirector.dto.converter.SnapshotDtoConverter;
import com.sungardas.snapdirector.service.SnapshotService;

@Service
public class SnapshotServiceImpl implements SnapshotService {

	@Autowired
	private SnapshotRepository snapshotRepository;
	
	@Override
	public void addSnapshot(SnapshotDto newSnapshot) {
		if(newSnapshot == null){
			throw new IllegalArgumentException("Provided argument is null");
		}
		
		snapshotRepository.save(SnapshotDtoConverter.convert(newSnapshot));

	}

	@Override
	public void removeSnapshot(String snapshotId) {
		if (snapshotId == null || snapshotId.length() != 13){
			throw new IllegalArgumentException("Incorrect SnapshotID");
		}
		
		Snapshot snapToDelete = snapshotRepository.deleteBySnapshotId(snapshotId).get(0);
		if (snapToDelete != null) {
			snapshotRepository.delete(snapToDelete.getId());
		}

	}

	@Override
	public List<SnapshotDto> getSnapshotsToDelete(String volumeId,
			String snapshotToLeave) {
		
		return SnapshotDtoConverter.convertToSnapshotDtoList(snapshotRepository.findByVolumeIdAndSnapshotIdNot(volumeId, snapshotToLeave));
	}

}
