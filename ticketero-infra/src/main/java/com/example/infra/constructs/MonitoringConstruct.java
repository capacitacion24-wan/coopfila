package com.example.infra.constructs;

import com.example.infra.config.EnvironmentConfig;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.services.cloudwatch.*;
import software.amazon.awscdk.services.logs.*;
import software.constructs.Construct;
import java.util.List;

/**
 * CloudWatch Logs, Alarms y Dashboard.
 */
public class MonitoringConstruct extends Construct {
    
    public MonitoringConstruct(final Construct scope, final String id,
                                final EnvironmentConfig config,
                                final ContainerConstruct container,
                                final DatabaseConstruct database) {
        super(scope, id);
        
        // Log Group
        LogGroup.Builder.create(this, "LogGroup")
            .logGroupName("/ecs/" + config.resourceName("api"))
            .retention(RetentionDays.of(config.logRetentionDays()))
            .build();
        
        if (!config.enableAlarms()) {
            return;
        }
        
        var service = container.getService();
        
        // Alarm: CPU > 80%
        Alarm.Builder.create(this, "CpuAlarm")
            .alarmName(config.resourceName("high-cpu"))
            .alarmDescription("CPU > 80%")
            .metric(service.getService().metricCpuUtilization(
                MetricOptions.builder().period(Duration.minutes(5)).build()))
            .threshold(80)
            .evaluationPeriods(1)
            .comparisonOperator(ComparisonOperator.GREATER_THAN_THRESHOLD)
            .build();
        
        // Alarm: Memory > 80%
        Alarm.Builder.create(this, "MemoryAlarm")
            .alarmName(config.resourceName("high-memory"))
            .metric(service.getService().metricMemoryUtilization(
                MetricOptions.builder().period(Duration.minutes(5)).build()))
            .threshold(80)
            .evaluationPeriods(1)
            .build();
        
        // Alarm: HTTP 5xx
        Alarm.Builder.create(this, "Http5xxAlarm")
            .alarmName(config.resourceName("http-5xx"))
            .metric(service.getLoadBalancer().metricHttpCodeTarget(
                HttpCodeTarget.TARGET_5XX_COUNT,
                MetricOptions.builder().period(Duration.minutes(5)).build()))
            .threshold(10)
            .evaluationPeriods(1)
            .treatMissingData(TreatMissingData.NOT_BREACHING)
            .build();
        
        // Dashboard
        Dashboard dashboard = Dashboard.Builder.create(this, "Dashboard")
            .dashboardName(config.resourceName("dashboard"))
            .build();
        
        dashboard.addWidgets(
            GraphWidget.Builder.create()
                .title("ECS CPU & Memory")
                .left(List.of(
                    service.getService().metricCpuUtilization(),
                    service.getService().metricMemoryUtilization()))
                .width(12).build(),
            GraphWidget.Builder.create()
                .title("ALB Requests")
                .left(List.of(service.getLoadBalancer().metricRequestCount()))
                .width(12).build()
        );
    }
}