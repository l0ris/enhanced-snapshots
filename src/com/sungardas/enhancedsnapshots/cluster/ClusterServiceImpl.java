package com.sungardas.enhancedsnapshots.cluster;

import com.amazonaws.services.autoscaling.AmazonAutoScaling;
import com.amazonaws.services.autoscaling.model.*;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.model.*;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancing;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.model.SubscribeRequest;
import com.amazonaws.services.sns.model.Topic;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.CreateQueueResult;
import com.sungardas.enhancedsnapshots.aws.dynamodb.model.NodeEntry;
import com.sungardas.enhancedsnapshots.aws.dynamodb.repository.NodeRepository;
import com.sungardas.enhancedsnapshots.components.ConfigurationMediator;
import com.sungardas.enhancedsnapshots.exception.ConfigurationException;
import com.sungardas.enhancedsnapshots.util.SystemUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class ClusterServiceImpl implements ClusterService {

    private static final Logger LOG = LogManager.getLogger(ClusterServiceImpl.class);
    private static final String SCALE_UP_POLICY = "ESS-ScaleUpPolicy-" + SystemUtils.getInstanceId();
    private static final String SCALE_DOWN_POLICY = "ESS-ScaleDownPolicy-" + SystemUtils.getInstanceId();
    private static final String METRIC_DATA_NAME = "ESS-Load-Metric-" + SystemUtils.getInstanceId();
    private static final String ESS_OVERLOAD_ALARM = "ESS-Overload-Alarm-" + SystemUtils.getInstanceId();
    private static final String ESS_IDLE_ALARM = "ESS-Idle-Alarm-" + SystemUtils.getInstanceId();
    private static final String ESS_TOPIC_NAME = "ESS-" + SystemUtils.getInstanceId() + "-topic";
    private static final String ESS_QUEUE_NAME = "ESS-" + SystemUtils.getInstanceId() + "-queue";

    @Autowired
    private AmazonSNS amazonSNS;
    @Autowired
    private AmazonSQS amazonSQS;
    @Autowired
    private AmazonCloudWatch cloudWatch;
    @Autowired
    private AmazonAutoScaling autoScaling;
    @Autowired
    private NodeRepository nodeRepository;
    @Autowired
    private ConfigurationMediator configurationMediator;
    @Autowired
    private ClusterEventListener listener;
    @Value("${enhancedsnapshots.default.backup.threadPool.size}")
    private int backupThreadPoolSize;
    @Value("${enhancedsnapshots.default.restore.threadPool.size}")
    private int restoreThreadPoolSize;
    private AutoScalingGroup autoScalingGroup;


    @PostConstruct
    private void init() {
        if (SystemUtils.clusterMode() && !clusterIsConfigured()) {
            configureClusterInfrastructure();
            nodeRepository.save(getMasterNodeInfo());
        }
    }

    protected NodeEntry getMasterNodeInfo() {
        return new NodeEntry(SystemUtils.getInstanceId(), true,
                restoreThreadPoolSize, backupThreadPoolSize);
    }

    private void configureClusterInfrastructure() {
        LOG.info("Configuration of cluster infrastructure started");

        // update AutoScalingGroup with min and max node number
        autoScaling.updateAutoScalingGroup(new UpdateAutoScalingGroupRequest()
                .withAutoScalingGroupName(getAutoScalingGroup().getAutoScalingGroupName())
                .withMaxSize(configurationMediator.getMaxNodeNumberInCluster())
                .withMinSize(configurationMediator.getMinNodeNumberInCluster())
                .withDesiredCapacity(configurationMediator.getMaxNodeNumberInCluster()));
        LOG.info("AutoScalingGroup {} updated: {}", autoScalingGroup.getAutoScalingGroupName(), autoScalingGroup.toString());

        // we create this infrustructure from JAVA since currently we can not get arn when we create policy from CFT
        //create AutoScaling Policies
        String scaleUpPolicyARN = autoScaling.putScalingPolicy(new PutScalingPolicyRequest().withAutoScalingGroupName(getAutoScalingGroup().getAutoScalingGroupName())
                .withPolicyName(SCALE_UP_POLICY)
                .withAutoScalingGroupName(getAutoScalingGroup().getAutoScalingGroupName())
                //Increase or decrease the current capacity of the group by the specified number of instances.
                .withAdjustmentType("ChangeInCapacity")
                .withPolicyType("SimpleScaling")
                .withScalingAdjustment(1)).getPolicyARN();
        LOG.info("Scale up policy created: {}", SCALE_UP_POLICY);

        String scaleDownPolicyARN = autoScaling.putScalingPolicy(new PutScalingPolicyRequest().withAutoScalingGroupName(getAutoScalingGroup().getAutoScalingGroupName())
                .withPolicyName(SCALE_DOWN_POLICY)
                .withAutoScalingGroupName(getAutoScalingGroup().getAutoScalingGroupName())
                //Increase or decrease the current capacity of the group by the specified number of instances.
                .withAdjustmentType("ChangeInCapacity")
                .withPolicyType("SimpleScaling")
                .withScalingAdjustment(-1)).getPolicyARN();
        LOG.info("Scale down policy created: {}", SCALE_DOWN_POLICY);

        // create custom metric
        MetricDatum metricDatum = new MetricDatum();
        metricDatum.setValue(0.0);
        metricDatum.setUnit(StandardUnit.Count);
        metricDatum.setTimestamp(new Date());
        metricDatum.setMetricName(METRIC_DATA_NAME);
        cloudWatch.putMetricData(new PutMetricDataRequest()
                .withNamespace("ESS/Tasks").withMetricData(metricDatum));
        LOG.info("Custom metric added: {}", metricDatum.toString());

        // create custom alarm
        cloudWatch.putMetricAlarm(new PutMetricAlarmRequest()
                .withAlarmName(ESS_OVERLOAD_ALARM)
                .withMetricName(METRIC_DATA_NAME)
                .withComparisonOperator(ComparisonOperator.GreaterThanOrEqualToThreshold)
                .withThreshold(80.0)
                .withPeriod(300)
                .withEvaluationPeriods(2)
                .withStatistic(Statistic.Average)
                .withNamespace("ESS/Tasks")
                .withDimensions(new Dimension()
                        .withName("AutoScalingGroupName")
                        .withValue(getAutoScalingGroup()
                                .getAutoScalingGroupName()))
                .withAlarmActions(scaleUpPolicyARN));
        LOG.info("Load alarm added: ", cloudWatch.describeAlarms().getMetricAlarms()
                .stream().filter(alarm->alarm.getAlarmName().equals(ESS_OVERLOAD_ALARM)).findFirst().get().toString());

        // create custom alarm
        cloudWatch.putMetricAlarm(new PutMetricAlarmRequest()
                .withAlarmName(ESS_IDLE_ALARM)
                .withMetricName(METRIC_DATA_NAME)
                .withComparisonOperator(ComparisonOperator.GreaterThanOrEqualToThreshold)
                .withThreshold(40.0)
                .withPeriod(300)
                .withEvaluationPeriods(2)
                .withStatistic(Statistic.Average)
                .withNamespace("ESS/Tasks")
                .withDimensions(new Dimension()
                        .withName("AutoScalingGroupName")
                        .withValue(getAutoScalingGroup()
                                .getAutoScalingGroupName()))
                .withAlarmActions(scaleDownPolicyARN));

        // subscribe to topic
        CreateQueueRequest request = new CreateQueueRequest()
                .withQueueName(ESS_QUEUE_NAME);
        amazonSQS.createQueue(request);
        Topic ess_topic = amazonSNS.listTopics().getTopics()
                .stream().filter(topic -> topic.getTopicArn().endsWith(ESS_TOPIC_NAME)).findFirst()
                .orElseThrow(() -> new ConfigurationException("Topic " + ESS_TOPIC_NAME + " does not exist."));
        amazonSNS.subscribe(new SubscribeRequest(ess_topic.getTopicArn(), "sqs", ESS_QUEUE_NAME));


        LOG.info("Alarm for idle resources added: ", cloudWatch.describeAlarms().getMetricAlarms()
                .stream().filter(alarm->alarm.getAlarmName().equals(ESS_IDLE_ALARM)).findFirst().get().toString());
        LOG.info("Cluster infrastructure successfully configured.");
    }

    /**
     * Returns % of system load level
     *
     * @return
     */
    private double getSystemLoadLevel() {
        List<NodeEntry> nodes = new ArrayList<>();
        nodeRepository.findAll().forEach(nodes::add);
        double backupLoad = nodes.size() * backupThreadPoolSize - nodes.stream().reduce(0, (sum, node) -> sum + node.getFreeBackupWorkers(),
                (sum1, sum2) -> sum1 + sum2) / nodes.size() * backupThreadPoolSize;
        double restoreLoad = nodes.size() * backupThreadPoolSize - nodes.stream().reduce(0, (sum, node) -> sum + node.getFreeRestoreWorkers(),
                (sum1, sum2) -> sum1 + sum2) / nodes.size() * restoreThreadPoolSize;
        return backupLoad > restoreLoad ? backupLoad : restoreLoad;
    }

    private AutoScalingGroup getAutoScalingGroup() {
        //there is no possibility to set custom name for AutoScalingGroup from CFT
        //that's why we have to determine created group name in code base on stack name
        //CloudFormation service uses next schema for AutoScalingGroup name
        //$CUSTOM_STACK_NAME-AutoScalingGroup-<some random string>

        Optional<AutoScalingGroup> asg = null;
        if (autoScalingGroup == null) {
            asg = autoScaling.describeAutoScalingGroups().getAutoScalingGroups().stream()
                    .filter(autoScalingGroup -> autoScalingGroup.getAutoScalingGroupName()
                            .startsWith(SystemUtils.getCloudFormationStackName() + "-AutoScalingGroup-")).findFirst();
            autoScalingGroup = asg.orElseThrow(() -> new ConfigurationException("No appropriate AutoScalingGroup was found"));
        }
        return autoScalingGroup;
    }

    // we assume that system is configured if configuration exists
    private boolean clusterIsConfigured() {
        return (nodeRepository.count() != 0) ? true : false;
    }

    @Override
    public void removeClusterInfrastructure() {
        autoScaling.deletePolicy(new DeletePolicyRequest().withPolicyName(SCALE_UP_POLICY));
        autoScaling.deletePolicy(new DeletePolicyRequest().withPolicyName(SCALE_DOWN_POLICY));
        cloudWatch.deleteAlarms(new DeleteAlarmsRequest().withAlarmNames(ESS_OVERLOAD_ALARM, ESS_IDLE_ALARM));
        // CloudWatch metrics are stored for two weeks. Old data will be removed automatically.
    }
}
