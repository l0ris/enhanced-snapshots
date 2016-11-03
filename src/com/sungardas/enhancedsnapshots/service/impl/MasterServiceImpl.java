package com.sungardas.enhancedsnapshots.service.impl;

import com.sungardas.enhancedsnapshots.aws.dynamodb.model.NodeEntry;
import com.sungardas.enhancedsnapshots.aws.dynamodb.model.TaskEntry;
import com.sungardas.enhancedsnapshots.aws.dynamodb.repository.NodeRepository;
import com.sungardas.enhancedsnapshots.aws.dynamodb.repository.TaskRepository;
import com.sungardas.enhancedsnapshots.cluster.ClusterConfigurationService;
import com.sungardas.enhancedsnapshots.components.ConfigurationMediator;
import com.sungardas.enhancedsnapshots.enumeration.TaskProgress;
import com.sungardas.enhancedsnapshots.service.*;
import com.sungardas.enhancedsnapshots.util.SystemUtils;
import org.apache.activemq.broker.BrokerService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service("MasterService")
@DependsOn({"ClusterConfigurationService"})
public class MasterServiceImpl implements MasterService {

    private static final Logger LOG = LogManager.getLogger(MasterServiceImpl.class);
    private static final String TASK_DISTRIBUTION_ID = "taskDistribution";
    private static final String METRIC_UPDATE_ID = "metricUpdate";
    private static final String TERMINATED_NODE_TASK_REASSIGN_ID = "terminatedReassign";
    private static final String MASTER_STATUS_CHECK = "masterStatusCheck";

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
    @Autowired
    private BrokerService broker;
    @Autowired
    private List<MasterInitialization> masterInitializations;

    @Value("${enhancedsnapshots.default.backup.threadPool.size}")
    private int backupThreadPoolSize;

    @Value("${enhancedsnapshots.default.restore.threadPool.size}")
    private int restoreThreadPoolSize;


    @Override
    @PostConstruct
    public void init() {
        if (configurationMediator.isClusterMode() && nodeRepository.findOne(SystemUtils.getInstanceId()).isMaster()) {
            LOG.info("Master node initialization");
            startBroker();
            if (!schedulerService.exists(TASK_DISTRIBUTION_ID)) {
                schedulerService.addTask(new Task() {
                    @Override
                    public void run() {
                        taskDistribution();
                    }

                    @Override
                    public String getId() {
                        return TASK_DISTRIBUTION_ID;
                    }
                }, new CronTrigger("*/20 * * * * *"));
            }

            if (!schedulerService.exists(TERMINATED_NODE_TASK_REASSIGN_ID)) {
                schedulerService.addTask(new Task() {
                    @Override
                    public void run() {
                        terminatedNodeTaskReassign();
                    }

                    @Override
                    public String getId() {
                        return TERMINATED_NODE_TASK_REASSIGN_ID;
                    }
                }, new CronTrigger("0 */5 * * * *"));
            }

            if (!schedulerService.exists(METRIC_UPDATE_ID)) {
                schedulerService.addTask(new Task() {
                    @Override
                    public String getId() {
                        return METRIC_UPDATE_ID;
                    }

                    @Override
                    public void run() {
                        clusterConfigurationService.updateCloudWatchMetric();
                    }
                }, "*/1 * * * *");
            }

            masterInitializations.forEach(MasterInitialization::init);

            LOG.info("Master node initialization finished");
        } else if (!configurationMediator.isClusterMode()) {
            masterInitializations.forEach(MasterInitialization::init);
        } else if (SystemUtils.clusterMode()) {
            if (!schedulerService.exists(MASTER_STATUS_CHECK)) {
                schedulerService.addTask(new Task() {
                    @Override
                    public String getId() {
                        return MASTER_STATUS_CHECK;
                    }

                    @Override
                    public void run() {
                        NodeEntry nodeEntry = nodeRepository.findOne(SystemUtils.getInstanceId());
                        if (nodeEntry.isMaster()) {
                            init();
                        }
                    }
                }, "*/2 * * * *");
            }
        }
    }

    @PreDestroy
    private void cleanUp() throws Exception {
       broker.stop();
    }


    private void startBroker() {
        try {
            broker.start();
            LOG.info("Broker successfully started");
        } catch (Exception e) {
            LOG.error("Failed to start broker:", e);
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
        LOG.debug("Master task distribution started");
        List<TaskEntry> unassignedTasks = taskRepository.findByWorkerIsNull();
        List<NodeEntry> nodes = getNodes();
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
        LOG.debug("Master task distribution finished. {} tasks reassigned", unassignedTasks.size());
    }

    private NodeEntry getNodeWithMaxAvailableBackupWorkers(List<NodeEntry> nodes) {
        return nodes.stream().sorted((n1, n2) -> n2.getFreeBackupWorkers() - n1.getFreeBackupWorkers()).findFirst().get();
    }

    private NodeEntry getNodeWithMaxAvailableRestoreWorkers(List<NodeEntry> nodes) {
        return nodes.stream().sorted((n1, n2) -> n2.getFreeRestoreWorkers() - n1.getFreeRestoreWorkers()).findFirst().get();
    }


    private void terminatedNodeTaskReassign() {
        Set<String> nodeIds = nodeRepository.findAll().stream().map(n -> n.getNodeId()).collect(Collectors.toSet());
        List<TaskEntry> taskEntries = taskRepository.findByStatusNotAndRegular(TaskEntry.TaskEntryStatus.COMPLETE.toString(), Boolean.FALSE.toString());
        List<TaskEntry> unassignedTasks = taskEntries.stream()
                .filter(t -> t.getWorker() != null && !nodeIds.contains(t.getWorker()))
                .peek(t -> {
                    t.setWorker(null);
                    t.setStatus(TaskEntry.TaskEntryStatus.PARTIALLY_FINISHED.toString());
                }).collect(Collectors.toList());
        LOG.info("Reassigned {} tasks", unassignedTasks.size());
        taskRepository.save(unassignedTasks);
    }

    public List<NodeEntry> getNodes() {
        List<NodeEntry> nodes = nodeRepository.findAll();
        for (NodeEntry node : nodes) {
            List<TaskEntry> assignedTasks = taskRepository.findByWorkerAndProgressNot(node.getNodeId(), TaskProgress.DONE);
            node.setFreeBackupWorkers((int) (backupThreadPoolSize - assignedTasks.stream()
                    .filter(t -> TaskEntry.TaskEntryType.BACKUP.getType().equals(t.getType()))
                    .count())
            );

            node.setFreeRestoreWorkers((int) (restoreThreadPoolSize - assignedTasks.stream()
                    .filter(t -> TaskEntry.TaskEntryType.RESTORE.getType().equals(t.getType()))
                    .count())
            );
        }


        return nodes;
    }
}
