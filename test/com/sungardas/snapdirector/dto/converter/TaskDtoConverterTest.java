package com.sungardas.snapdirector.dto.converter;

import java.util.Arrays;
import java.util.List;

import com.sungardas.enhancedsnapshots.aws.dynamodb.model.TaskEntry;
import com.sungardas.enhancedsnapshots.dto.TaskDto;
import com.sungardas.enhancedsnapshots.dto.converter.TaskDtoConverter;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TaskDtoConverterTest {

    private String id = "Id1";
    private String priority = "2";
    private String status = "queued";
    private String type = "backup";
    private List<String> volumes = Arrays.asList("vol-c343123b", "vol-c34232");
    private String schedulerManual = "false";
    private String schedulerName = "everyYear";
    private String schedulerTime = "00:03:82";
    private String instanceId = "i-6easdads";
    private String backupFileName = "someFileName";
    private String cron = "0 0 1 1 *";
    private String zone = "az";
    private String regular = Boolean.TRUE.toString();
    private String enabled = "true";

    private TaskDto taskDto;

    @Before
    public void setUp(){
        taskDto = new TaskDto();
        taskDto.setId(id);
        taskDto.setPriority(priority);
        taskDto.setType(type);
        taskDto.setStatus(status);
        taskDto.setVolumes(volumes);
        taskDto.setSchedulerManual(schedulerManual);
        taskDto.setSchedulerTime(schedulerTime);
        taskDto.setSchedulerName(schedulerName);
        taskDto.setBackupFileName(backupFileName);
        taskDto.setCron(cron);
        taskDto.setZone(zone);
        taskDto.setRegular(regular);
        taskDto.setEnabled(enabled);
    }

    @Test
    public void shouldConvertFromTaskDtoToTaskEnty(){
        List<TaskEntry> taskEntries = TaskDtoConverter.convert(taskDto);

        // assert there are 2 taskEntries since there are 2 volumes in the volumes list
        Assert.assertTrue(taskEntries.size() == 2);

        // check properties values
        for(TaskEntry taskEntry: taskEntries) {
            Assert.assertTrue(taskEntry.getId().equals(id));
            // backup priority is 0
            Assert.assertTrue(taskEntry.getPriority() == 0);
            Assert.assertTrue(taskEntry.getType().equals(type));
            Assert.assertTrue(taskEntry.getStatus().equals(status));
            Assert.assertTrue(taskEntry.getSchedulerManual().equals(schedulerManual));
            Assert.assertTrue(taskEntry.getSchedulerName().equals(schedulerName));
            //TODO: find out what if instance ids were different while backup of several volumes ???
            Assert.assertTrue(taskEntry.getCron().equals(cron));
            Assert.assertTrue(taskEntry.getAvailabilityZone().equals(zone));
            Assert.assertTrue(taskEntry.getEnabled().equals(enabled));
        }

        // check volume id of first taskEntry
        Assert.assertTrue(taskEntries.get(0).getVolume().equals(volumes.get(0)));

        // check volume id of second taskEntry
        Assert.assertTrue(taskEntries.get(1).getVolume().equals(volumes.get(1)));
    }

    @Test
    public void shouldSetPriority1ForDeleteTasks(){
        taskDto.setType("delete");
        List<TaskEntry> taskEntries = TaskDtoConverter.convert(taskDto);

        // check priority of first taskEntry
        Assert.assertTrue(taskEntries.get(0).getPriority() == 1);

        // check priority of second taskEntry
        Assert.assertTrue(taskEntries.get(1).getPriority() == 1);
    }
}
