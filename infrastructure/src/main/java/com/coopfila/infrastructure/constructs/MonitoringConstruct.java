package com.coopfila.infrastructure.constructs;

import software.amazon.awscdk.Duration;
import software.amazon.awscdk.services.cloudwatch.*;
import software.amazon.awscdk.services.ecs.IService;
import software.amazon.awscdk.services.elasticloadbalancingv2.IApplicationLoadBalancer;
import software.amazon.awscdk.services.elasticloadbalancingv2.IApplicationTargetGroup;
import software.amazon.awscdk.services.rds.IDatabaseInstance;
import software.amazon.awscdk.services.sns.ITopic;
import software.constructs.Construct;

import java.util.List;
import java.util.Map;

/**
 * MonitoringConstruct - Defines CloudWatch monitoring, dashboards and alarms for CoopFila
 */
public class MonitoringConstruct extends Construct {

    private final Dashboard dashboard;
    private final List<Alarm> alarms;

    public MonitoringConstruct(Construct scope, String id, MonitoringProps props) {
        super(scope, id);

        // Create CloudWatch Dashboard
        this.dashboard = Dashboard.Builder.create(this, "CoopFilaDashboard")
                .dashboardName("CoopFila-" + props.getEnvironment())
                .build();

        // Application metrics widgets
        var appMetricsWidget = GraphWidget.Builder.create()
                .title("Application Metrics")
                .left(List.of(
                        createEcsMetric(props.getEcsService(), "CPUUtilization"),
                        createEcsMetric(props.getEcsService(), "MemoryUtilization")
                ))
                .right(List.of(
                        createAlbMetric(props.getLoadBalancer(), "RequestCount"),
                        createAlbMetric(props.getLoadBalancer(), "TargetResponseTime")
                ))
                .period(Duration.minutes(5))
                .build();

        // Database metrics widget
        var dbMetricsWidget = GraphWidget.Builder.create()
                .title("Database Metrics")
                .left(List.of(
                        createRdsMetric(props.getDatabase(), "CPUUtilization"),
                        createRdsMetric(props.getDatabase(), "DatabaseConnections")
                ))
                .right(List.of(
                        createRdsMetric(props.getDatabase(), "ReadLatency"),
                        createRdsMetric(props.getDatabase(), "WriteLatency")
                ))
                .period(Duration.minutes(5))
                .build();

        // Error metrics widget
        var errorMetricsWidget = GraphWidget.Builder.create()
                .title("Error Metrics")
                .left(List.of(
                        createAlbMetric(props.getLoadBalancer(), "HTTPCode_Target_4XX_Count"),
                        createAlbMetric(props.getLoadBalancer(), "HTTPCode_Target_5XX_Count")
                ))
                .right(List.of(
                        createTargetGroupMetric(props.getTargetGroup(), "UnHealthyHostCount"),
                        createTargetGroupMetric(props.getTargetGroup(), "HealthyHostCount")
                ))
                .period(Duration.minutes(5))
                .build();

        // Add widgets to dashboard
        dashboard.addWidgets(appMetricsWidget);
        dashboard.addWidgets(dbMetricsWidget);
        dashboard.addWidgets(errorMetricsWidget);

        // Create alarms
        this.alarms = createAlarms(props);

        // Add tags
        Map<String, String> tags = Map.of(
                "Project", "CoopFila",
                "Environment", props.getEnvironment(),
                "Component", "Monitoring"
        );

        tags.forEach((key, value) -> {
            software.amazon.awscdk.Tags.of(dashboard).add(key, value);
            alarms.forEach(alarm -> software.amazon.awscdk.Tags.of(alarm).add(key, value));
        });
    }

    private List<Alarm> createAlarms(MonitoringProps props) {
        return List.of(
                // High CPU alarm
                Alarm.Builder.create(this, "HighCpuAlarm")
                        .alarmName("CoopFila-" + props.getEnvironment() + "-HighCPU")
                        .alarmDescription("High CPU utilization on ECS service")
                        .metric(createEcsMetric(props.getEcsService(), "CPUUtilization"))
                        .threshold(80.0)
                        .evaluationPeriods(2)
                        .comparisonOperator(ComparisonOperator.GREATER_THAN_THRESHOLD)
                        .treatMissingData(TreatMissingData.NOT_BREACHING)
                        .build(),

                // High memory alarm
                Alarm.Builder.create(this, "HighMemoryAlarm")
                        .alarmName("CoopFila-" + props.getEnvironment() + "-HighMemory")
                        .alarmDescription("High memory utilization on ECS service")
                        .metric(createEcsMetric(props.getEcsService(), "MemoryUtilization"))
                        .threshold(85.0)
                        .evaluationPeriods(2)
                        .comparisonOperator(ComparisonOperator.GREATER_THAN_THRESHOLD)
                        .treatMissingData(TreatMissingData.NOT_BREACHING)
                        .build(),

                // High response time alarm
                Alarm.Builder.create(this, "HighResponseTimeAlarm")
                        .alarmName("CoopFila-" + props.getEnvironment() + "-HighResponseTime")
                        .alarmDescription("High response time on load balancer")
                        .metric(createAlbMetric(props.getLoadBalancer(), "TargetResponseTime"))
                        .threshold(2.0)
                        .evaluationPeriods(3)
                        .comparisonOperator(ComparisonOperator.GREATER_THAN_THRESHOLD)
                        .treatMissingData(TreatMissingData.NOT_BREACHING)
                        .build(),

                // High error rate alarm
                Alarm.Builder.create(this, "HighErrorRateAlarm")
                        .alarmName("CoopFila-" + props.getEnvironment() + "-HighErrorRate")
                        .alarmDescription("High 5XX error rate")
                        .metric(createAlbMetric(props.getLoadBalancer(), "HTTPCode_Target_5XX_Count"))
                        .threshold(10.0)
                        .evaluationPeriods(2)
                        .comparisonOperator(ComparisonOperator.GREATER_THAN_THRESHOLD)
                        .treatMissingData(TreatMissingData.NOT_BREACHING)
                        .build(),

                // Database CPU alarm
                Alarm.Builder.create(this, "DatabaseHighCpuAlarm")
                        .alarmName("CoopFila-" + props.getEnvironment() + "-DB-HighCPU")
                        .alarmDescription("High CPU utilization on RDS instance")
                        .metric(createRdsMetric(props.getDatabase(), "CPUUtilization"))
                        .threshold(75.0)
                        .evaluationPeriods(2)
                        .comparisonOperator(ComparisonOperator.GREATER_THAN_THRESHOLD)
                        .treatMissingData(TreatMissingData.NOT_BREACHING)
                        .build(),

                // Database connections alarm
                Alarm.Builder.create(this, "DatabaseHighConnectionsAlarm")
                        .alarmName("CoopFila-" + props.getEnvironment() + "-DB-HighConnections")
                        .alarmDescription("High number of database connections")
                        .metric(createRdsMetric(props.getDatabase(), "DatabaseConnections"))
                        .threshold(props.isProduction() ? 150.0 : 75.0)
                        .evaluationPeriods(2)
                        .comparisonOperator(ComparisonOperator.GREATER_THAN_THRESHOLD)
                        .treatMissingData(TreatMissingData.NOT_BREACHING)
                        .build()
        );
    }

    private Metric createEcsMetric(IService service, String metricName) {
        return Metric.Builder.create()
                .namespace("AWS/ECS")
                .metricName(metricName)
                .dimensionsMap(Map.of(
                        "ServiceName", service.getServiceName(),
                        "ClusterName", service.getCluster().getClusterName()
                ))
                .statistic("Average")
                .period(Duration.minutes(5))
                .build();
    }

    private Metric createAlbMetric(IApplicationLoadBalancer loadBalancer, String metricName) {
        return Metric.Builder.create()
                .namespace("AWS/ApplicationELB")
                .metricName(metricName)
                .dimensionsMap(Map.of(
                        "LoadBalancer", loadBalancer.getLoadBalancerFullName()
                ))
                .statistic("Average")
                .period(Duration.minutes(5))
                .build();
    }

    private Metric createTargetGroupMetric(IApplicationTargetGroup targetGroup, String metricName) {
        return Metric.Builder.create()
                .namespace("AWS/ApplicationELB")
                .metricName(metricName)
                .dimensionsMap(Map.of(
                        "TargetGroup", targetGroup.getTargetGroupFullName()
                ))
                .statistic("Average")
                .period(Duration.minutes(5))
                .build();
    }

    private Metric createRdsMetric(IDatabaseInstance database, String metricName) {
        return Metric.Builder.create()
                .namespace("AWS/RDS")
                .metricName(metricName)
                .dimensionsMap(Map.of(
                        "DBInstanceIdentifier", database.getInstanceIdentifier()
                ))
                .statistic("Average")
                .period(Duration.minutes(5))
                .build();
    }

    public Dashboard getDashboard() {
        return dashboard;
    }

    public List<Alarm> getAlarms() {
        return alarms;
    }

    /**
     * Properties for MonitoringConstruct
     */
    public static class MonitoringProps {
        private final IService ecsService;
        private final IApplicationLoadBalancer loadBalancer;
        private final IApplicationTargetGroup targetGroup;
        private final IDatabaseInstance database;
        private final ITopic alarmTopic;
        private final String environment;
        private final boolean production;

        public MonitoringProps(IService ecsService, IApplicationLoadBalancer loadBalancer,
                             IApplicationTargetGroup targetGroup, IDatabaseInstance database,
                             ITopic alarmTopic, String environment, boolean production) {
            this.ecsService = ecsService;
            this.loadBalancer = loadBalancer;
            this.targetGroup = targetGroup;
            this.database = database;
            this.alarmTopic = alarmTopic;
            this.environment = environment;
            this.production = production;
        }

        public IService getEcsService() { return ecsService; }
        public IApplicationLoadBalancer getLoadBalancer() { return loadBalancer; }
        public IApplicationTargetGroup getTargetGroup() { return targetGroup; }
        public IDatabaseInstance getDatabase() { return database; }
        public ITopic getAlarmTopic() { return alarmTopic; }
        public String getEnvironment() { return environment; }
        public boolean isProduction() { return production; }
    }
}