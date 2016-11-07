package enhancedsnapshots.service.impl;

import com.sungardas.enhancedsnapshots.aws.dynamodb.model.NodeEntry;
import com.sungardas.enhancedsnapshots.aws.dynamodb.model.TaskEntry;
import com.sungardas.enhancedsnapshots.aws.dynamodb.repository.NodeRepository;
import com.sungardas.enhancedsnapshots.aws.dynamodb.repository.TaskRepository;
import com.sungardas.enhancedsnapshots.cluster.ClusterConfigurationService;
import com.sungardas.enhancedsnapshots.components.ConfigurationMediator;
import com.sungardas.enhancedsnapshots.service.SchedulerService;
import com.sungardas.enhancedsnapshots.service.impl.MasterServiceImpl;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

//TODO fix
@Ignore
@RunWith(MockitoJUnitRunner.class)
public class MasterServiceImplTest {

    @Mock
    private ConfigurationMediator configurationMediator;

    @Mock
    private SchedulerService schedulerService;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private NodeRepository nodeRepository;

    @Mock
    private ClusterConfigurationService clusterConfigurationService;

    @InjectMocks
    private MasterServiceImpl masterService;


    @Test
    public void basicTaskDistributionTest() {
        List<TaskEntry> unassignedTasks = Arrays.asList(new TaskEntry[]{
                getTaskEntry(TaskEntry.TaskEntryType.BACKUP),
                getTaskEntry(TaskEntry.TaskEntryType.BACKUP),
                getTaskEntry(TaskEntry.TaskEntryType.RESTORE),
                getTaskEntry(TaskEntry.TaskEntryType.RESTORE),
                getTaskEntry(TaskEntry.TaskEntryType.RESTORE),
                getTaskEntry(TaskEntry.TaskEntryType.DELETE),
                getTaskEntry(TaskEntry.TaskEntryType.SYSTEM_BACKUP)
        });

        List<NodeEntry> nodes = Arrays.asList(new NodeEntry[]{
                getNode("id1", 0, 5),
                getNode("id2", 5, 0)
        });

        when(taskRepository.findByWorkerIsNull()).thenReturn(unassignedTasks);
        when(nodeRepository.findAll()).thenReturn(nodes);

        masterService.taskDistribution();

        verify(taskRepository).save(unassignedTasks);

        for (TaskEntry task : unassignedTasks) {
            assertNotNull(task.getWorker());
        }
    }

    @Test
    public void TaskDistributionWithLimitedResourcesTest() {
        List<TaskEntry> unassignedTasks = Arrays.asList(new TaskEntry[]{
                getTaskEntry(TaskEntry.TaskEntryType.BACKUP),
                getTaskEntry(TaskEntry.TaskEntryType.BACKUP),
                getTaskEntry(TaskEntry.TaskEntryType.RESTORE),
                getTaskEntry(TaskEntry.TaskEntryType.RESTORE),
                getTaskEntry(TaskEntry.TaskEntryType.RESTORE),
                getTaskEntry(TaskEntry.TaskEntryType.DELETE),
                getTaskEntry(TaskEntry.TaskEntryType.SYSTEM_BACKUP)
        });

        List<NodeEntry> nodes = Arrays.asList(new NodeEntry[]{
                getNode("id1", 0, 2),
                getNode("id2", 5, 0)
        });

        when(taskRepository.findByWorkerIsNull()).thenReturn(unassignedTasks);
        when(nodeRepository.findAll()).thenReturn(nodes);

        masterService.taskDistribution();

        verify(taskRepository).save(unassignedTasks);

        assertNotNull(unassignedTasks.get(0).getWorker());
        assertNotNull(unassignedTasks.get(1).getWorker());
        assertNotNull(unassignedTasks.get(2).getWorker());
        assertNotNull(unassignedTasks.get(3).getWorker());
        assertNull(unassignedTasks.get(4).getWorker());
        assertNotNull(unassignedTasks.get(5).getWorker());
        assertNull(unassignedTasks.get(6).getWorker());
    }

    @Test
    public void TaskDistributionWithLimitedResourcesTest2() {
        List<TaskEntry> unassignedTasks = Arrays.asList(new TaskEntry[]{
                getTaskEntry(TaskEntry.TaskEntryType.BACKUP),
                getTaskEntry(TaskEntry.TaskEntryType.BACKUP),
                getTaskEntry(TaskEntry.TaskEntryType.RESTORE),
                getTaskEntry(TaskEntry.TaskEntryType.RESTORE),
                getTaskEntry(TaskEntry.TaskEntryType.RESTORE),
                getTaskEntry(TaskEntry.TaskEntryType.DELETE),
                getTaskEntry(TaskEntry.TaskEntryType.SYSTEM_BACKUP)
        });

        List<NodeEntry> nodes = Arrays.asList(new NodeEntry[]{
                getNode("id1", 0, 5),
                getNode("id2", 1, 0)
        });

        when(taskRepository.findByWorkerIsNull()).thenReturn(unassignedTasks);
        when(nodeRepository.findAll()).thenReturn(nodes);

        masterService.taskDistribution();

        verify(taskRepository).save(unassignedTasks);

        assertNotNull(unassignedTasks.get(0).getWorker());
        assertNull(unassignedTasks.get(1).getWorker());
        assertNotNull(unassignedTasks.get(2).getWorker());
        assertNotNull(unassignedTasks.get(3).getWorker());
        assertNotNull(unassignedTasks.get(4).getWorker());
        assertNull(unassignedTasks.get(5).getWorker());
        assertNotNull(unassignedTasks.get(6).getWorker());
    }

    @Test
    public void TaskDistributionResourcesTest() {
        List<TaskEntry> unassignedTasks = Arrays.asList(new TaskEntry[]{
                getTaskEntry(TaskEntry.TaskEntryType.BACKUP),
                getTaskEntry(TaskEntry.TaskEntryType.BACKUP),
                getTaskEntry(TaskEntry.TaskEntryType.BACKUP),
                getTaskEntry(TaskEntry.TaskEntryType.RESTORE),
                getTaskEntry(TaskEntry.TaskEntryType.RESTORE),
                getTaskEntry(TaskEntry.TaskEntryType.RESTORE),
                getTaskEntry(TaskEntry.TaskEntryType.DELETE),
                getTaskEntry(TaskEntry.TaskEntryType.SYSTEM_BACKUP)
        });

        List<NodeEntry> nodes = Arrays.asList(new NodeEntry[]{
                getNode("id1", 1, 1),
                getNode("id2", 2, 1),
                getNode("id3", 1, 2)
        });

        when(taskRepository.findByWorkerIsNull()).thenReturn(unassignedTasks);
        when(nodeRepository.findAll()).thenReturn(nodes);

        masterService.taskDistribution();

        verify(taskRepository).save(unassignedTasks);

        int id1 = 0, id2 = 0, id3 = 0;

        for (TaskEntry task : unassignedTasks) {
            assertNotNull(task.getWorker());
            switch (task.getWorker()) {
                case "id1":
                    id1++;
                    break;
                case "id2":
                    id2++;
                    break;
                case "id3":
                    id3++;
                    break;

            }
        }
        assertEquals(2, id1);
        assertEquals(3, id2);
        assertEquals(3, id3);
    }


    private TaskEntry getTaskEntry(TaskEntry.TaskEntryType type) {
        TaskEntry taskEntry = new TaskEntry();

        taskEntry.setType(type.getType());

        return taskEntry;
    }

    private NodeEntry getNode(String id, int backup, int restore) {
        NodeEntry nodeEntry = new NodeEntry();

        nodeEntry.setNodeId(id);
        nodeEntry.setFreeBackupWorkers(backup);
        nodeEntry.setFreeRestoreWorkers(restore);

        return nodeEntry;
    }
}
