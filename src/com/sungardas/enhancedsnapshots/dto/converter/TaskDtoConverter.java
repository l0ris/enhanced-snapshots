package com.sungardas.enhancedsnapshots.dto.converter;

import com.sungardas.enhancedsnapshots.aws.dynamodb.model.TaskEntry;
import com.sungardas.enhancedsnapshots.dto.TaskDto;
import org.springframework.beans.BeanUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static com.sungardas.enhancedsnapshots.aws.dynamodb.model.TaskEntry.TaskEntryType.SYSTEM_BACKUP;



public class TaskDtoConverter {

	public static TaskDto convert(TaskEntry task) {
		TaskDto taskDto = new TaskDto();
		BeanUtils.copyProperties(task, taskDto);
		taskDto.setSchedulerTime(task.getSchedulerTime());
		taskDto.setPriority(String.valueOf(task.getPriority()));
		taskDto.setVolumes(Arrays.asList(task.getVolume()));
		return taskDto;
	}

	public static List<TaskEntry> convert(TaskDto taskDto) {
		List<TaskEntry> result = new ArrayList<>();
        if(SYSTEM_BACKUP.getType().equals(taskDto.getType())){
            TaskEntry task = copy(taskDto);
            task.setVolume(SYSTEM_BACKUP.getType());
            result.add(task);
        } else {
            for (String volumeId : taskDto.getVolumes()) {
                TaskEntry task = copy(taskDto);
                task.setVolume(volumeId);
				result.add(task);
			}
		}
		return result;
	}

	private static TaskEntry copy(TaskDto taskDto){
		TaskEntry task = new TaskEntry();
		BeanUtils.copyProperties(taskDto, task);
		String options = new StringBuffer().
				append(taskDto.getBackupFileName()).append(", ").append(taskDto.getZone()).toString();
		task.setOptions(options);
		switch (task.getType()) {
			case "delete":
				task.setPriority(1);
				break;
			default:
				task.setPriority(0);
				break;
		}
		return task;
	}

	public static List<TaskDto> convert(Iterable<TaskEntry> taskEntries) {
		List<TaskDto> dtos = new ArrayList<>();
		for (TaskEntry task : taskEntries) {
			dtos.add(convert(task));
		}
		return dtos;
	}

	public static List<TaskDto> convert(Collection<TaskEntry>... taskEntries) {
		return Arrays.stream(taskEntries).flatMap(s -> s.stream()).parallel()
				.map(TaskDtoConverter::convert).collect(Collectors.toList());
	}
}
