package com.sungardas.enhancedsnapshots.service.impl;

import com.sungardas.enhancedsnapshots.aws.dynamodb.model.NodeEntry;
import com.sungardas.enhancedsnapshots.aws.dynamodb.model.TaskEntry;
import com.sungardas.enhancedsnapshots.aws.dynamodb.repository.NodeRepository;
import com.sungardas.enhancedsnapshots.aws.dynamodb.repository.TaskRepository;
import com.sungardas.enhancedsnapshots.cluster.ClusterConfigurationService;
import com.sungardas.enhancedsnapshots.components.ConfigurationMediator;
import com.sungardas.enhancedsnapshots.service.AWSCommunicationService;
import com.sungardas.enhancedsnapshots.service.MasterService;
import com.sungardas.enhancedsnapshots.service.SchedulerService;
import com.sungardas.enhancedsnapshots.service.Task;
import com.sungardas.enhancedsnapshots.util.SystemUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@DependsOn({"ClusterConfigurationService", "SystemService"})
public class MasterServiceImpl implements MasterService {

    private static final String TASK_DISTRIBUTION_ID = "taskDistribution";
    private static final String METRIC_UPDATE_ID = "metricUpdate";

    @Autowired
    private ConfigurationMediator configurationMediator;

    @Autowired
    private SchedulerService schedulerService;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private ClusterConfigurationService clusterConfigurationService;

    @Autowired
    private AWSCommunicationService awsCommunicationService;


    @Override
    public void init() {
        if (configurationMediator.isClusterMode() && nodeRepository.findOne(SystemUtils.getInstanceId()).isMaster()) {
            schedulerService.addTask(new Task() {
                @Override
                public void run() {
                    taskDistribution();
                }

                @Override
                public String getId() {
                    return TASK_DISTRIBUTION_ID;
                }
            }, new CronTrigger("*/5 * * * * *"));

            schedulerService.addTask(new Task() {
                @Override
                public String getId() {
                    return METRIC_UPDATE_ID;
                }

                @Override
                public void run() {
                    clusterConfigurationService.updateCloudWatchMetric();
                }
            }, "*/5 * * * *");
        }
    }

    @Override
    public String getMasterId() {
        List<NodeEntry> nodes = nodeRepository.findByMaster(true);
        if (nodes.size() > 0) {
            return nodes.get(0).getNodeId();
        }
        return null;
    }

    @Override
    public String getNodeHostname(String instanceId) {
        return awsCommunicationService.getInstance(instanceId).getPrivateDnsName();
    }

    @Override
    public String getMasterHostname() {
        return getNodeHostname(getMasterId());
    }

    @Override
    public boolean isClusterMode() {
        return configurationMediator.isClusterMode();
    }


    public void taskDistribution() {
        List<TaskEntry> unassignedTasks = taskRepository.findByWorkerIsNull();
        List<NodeEntry> nodes = new ArrayList<>();
        nodes.addAll(nodeRepository.findAll());
        for (TaskEntry t : unassignedTasks) {
            if (TaskEntry.TaskEntryType.RESTORE.getType().equals(t.getType())) {
                NodeEntry node = getNodeWithMaxAvailableRestoreWorkers(nodes);
                if (node.getFreeRestoreWorkers() == 0) {
                    continue;
                }
                node.setFreeRestoreWorkers(node.getFreeRestoreWorkers() - 1);
                t.setWorker(node.getNodeId());
            } else if (TaskEntry.TaskEntryType.BACKUP.getType().equals(t.getType())) {
                NodeEntry node = getNodeWithMaxAvailableBackupWorkers(nodes);
                if (node.getFreeBackupWorkers() == 0) {
                    continue;
                }
                node.setFreeBackupWorkers(node.getFreeBackupWorkers() - 1);
                t.setWorker(node.getNodeId());
            } else if (TaskEntry.TaskEntryType.DELETE.getType().equals(t.getType())) {
                NodeEntry node = getNodeWithMaxAvailableBackupWorkers(nodes);
                if (node.getFreeBackupWorkers() == 0) {
                    continue;
                }
                t.setWorker(node.getNodeId());
            } else if (TaskEntry.TaskEntryType.SYSTEM_BACKUP.getType().equals(t.getType())) {
                NodeEntry node = getNodeWithMaxAvailableRestoreWorkers(nodes);
                if (node.getFreeRestoreWorkers() == 0) {
                    continue;
                }
                t.setWorker(node.getNodeId());
            }
        }
        taskRepository.save(unassignedTasks);
    }

    private NodeEntry getNodeWithMaxAvailableBackupWorkers(List<NodeEntry> nodes) {
        return nodes.stream().sorted((n1, n2) -> n2.getFreeBackupWorkers() - n1.getFreeBackupWorkers()).findFirst().get();
    }

    private NodeEntry getNodeWithMaxAvailableRestoreWorkers(List<NodeEntry> nodes) {
        return nodes.stream().sorted((n1, n2) -> n2.getFreeRestoreWorkers() - n1.getFreeRestoreWorkers()).findFirst().get();
    }

}
