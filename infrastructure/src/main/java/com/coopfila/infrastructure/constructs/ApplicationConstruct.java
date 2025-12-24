package com.coopfila.infrastructure.constructs;

import software.amazon.awscdk.Duration;
import software.amazon.awscdk.services.applicationautoscaling.EnableScalingProps;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.ecs.*;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedFargateService;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedTaskImageOptions;
import software.amazon.awscdk.services.elasticloadbalancingv2.HealthCheck;
import software.amazon.awscdk.services.elasticloadbalancingv2.Protocol;
import software.amazon.awscdk.services.iam.Effect;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.amazon.awscdk.services.secretsmanager.ISecret;
import software.constructs.Construct;

import java.util.List;
import java.util.Map;

/**
 * ApplicationConstruct - Defines ECS Fargate service for CoopFila application
 */
public class ApplicationConstruct extends Construct {

    private final ApplicationLoadBalancedFargateService fargateService;
    private final LogGroup logGroup;

    public ApplicationConstruct(Construct scope, String id, ApplicationProps props) {
        super(scope, id);

        // Create log group
        this.logGroup = LogGroup.Builder.create(this, "ApplicationLogGroup")
                .logGroupName("/ecs/coopfila-" + props.getEnvironment())
                .retention(props.isProduction() ? RetentionDays.ONE_MONTH : RetentionDays.ONE_WEEK)
                .build();

        // Create Fargate service with ALB
        this.fargateService = ApplicationLoadBalancedFargateService.Builder.create(this, "FargateService")
                .serviceName("coopfila-" + props.getEnvironment())
                .cluster(props.getCluster())
                .cpu(props.isProduction() ? 1024 : 512)
                .memoryLimitMiB(props.isProduction() ? 2048 : 1024)
                .desiredCount(props.isProduction() ? 2 : 1)
                .taskImageOptions(ApplicationLoadBalancedTaskImageOptions.builder()
                        .image(ContainerImage.fromRegistry(props.getImageUri()))
                        .containerPort(8080)
                        .containerName("coopfila-app")
                        .enableLogging(true)
                        .logDriver(LogDriver.awsLogs(AwsLogDriverProps.builder()
                                .logGroup(logGroup)
                                .streamPrefix("app")
                                .build()))
                        .environment(Map.of(
                                "SPRING_PROFILES_ACTIVE", props.getEnvironment(),
                                "SERVER_PORT", "8080",
                                "MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE", "health,info,metrics"
                        ))
                        .secrets(Map.of(
                                "SPRING_DATASOURCE_URL", 
                                EcsSecret.fromSecretsManager(props.getDatabaseSecret(), "url"),
                                "SPRING_DATASOURCE_USERNAME", 
                                EcsSecret.fromSecretsManager(props.getDatabaseSecret(), "username"),
                                "SPRING_DATASOURCE_PASSWORD", 
                                EcsSecret.fromSecretsManager(props.getDatabaseSecret(), "password"),
                                "TELEGRAM_BOT_TOKEN", 
                                EcsSecret.fromSecretsManager(props.getTelegramSecret())
                        ))
                        .build())
                .vpc(props.getVpc())
                .assignPublicIp(false)
                .listenerPort(80)
                .protocol(software.amazon.awscdk.services.elasticloadbalancingv2.ApplicationProtocol.HTTP)
                .publicLoadBalancer(true)
                .build();

        // Configure health check
        fargateService.getTargetGroup().configureHealthCheck(HealthCheck.builder()
                .enabled(true)
                .path("/actuator/health")
                .protocol(Protocol.HTTP)
                .port("8080")
                .healthyHttpCodes("200")
                .interval(Duration.seconds(30))
                .timeout(Duration.seconds(10))
                .healthyThresholdCount(2)
                .unhealthyThresholdCount(3)
                .build());

        // Add security group rules
        fargateService.getService().getConnections().getSecurityGroups().get(0)
                .addIngressRule(props.getAppSecurityGroup(), Port.tcp(8080), "Allow ALB traffic");

        // Add IAM permissions for Secrets Manager
        fargateService.getTaskDefinition().getTaskRole().addToPrincipalPolicy(
                PolicyStatement.Builder.create()
                        .effect(Effect.ALLOW)
                        .actions(List.of(
                                "secretsmanager:GetSecretValue",
                                "secretsmanager:DescribeSecret"
                        ))
                        .resources(List.of(
                                props.getDatabaseSecret().getSecretArn(),
                                props.getTelegramSecret().getSecretArn()
                        ))
                        .build()
        );

        // Configure auto scaling
        if (props.isProduction()) {
            var scalableTarget = fargateService.getService().autoScaleTaskCount(
                    EnableScalingProps.builder()
                            .minCapacity(2)
                            .maxCapacity(10)
                            .build()
            );

            // CPU-based scaling
            scalableTarget.scaleOnCpuUtilization("CpuScaling",
                    CpuUtilizationScalingProps.builder()
                            .targetUtilizationPercent(70)
                            .scaleInCooldown(Duration.minutes(5))
                            .scaleOutCooldown(Duration.minutes(2))
                            .build()
            );

            // Memory-based scaling
            scalableTarget.scaleOnMemoryUtilization("MemoryScaling",
                    MemoryUtilizationScalingProps.builder()
                            .targetUtilizationPercent(80)
                            .scaleInCooldown(Duration.minutes(5))
                            .scaleOutCooldown(Duration.minutes(2))
                            .build()
            );
        }

        // Add tags
        Map<String, String> tags = Map.of(
                "Project", "CoopFila",
                "Environment", props.getEnvironment(),
                "Component", "Application"
        );

        tags.forEach((key, value) -> {
            software.amazon.awscdk.Tags.of(fargateService).add(key, value);
            software.amazon.awscdk.Tags.of(logGroup).add(key, value);
        });
    }

    public ApplicationLoadBalancedFargateService getFargateService() {
        return fargateService;
    }

    public LogGroup getLogGroup() {
        return logGroup;
    }

    public String getLoadBalancerDnsName() {
        return fargateService.getLoadBalancer().getLoadBalancerDnsName();
    }

    /**
     * Properties for ApplicationConstruct
     */
    public static class ApplicationProps {
        private final Vpc vpc;
        private final Cluster cluster;
        private final SecurityGroup appSecurityGroup;
        private final ISecret databaseSecret;
        private final ISecret telegramSecret;
        private final String imageUri;
        private final String environment;
        private final boolean production;

        public ApplicationProps(Vpc vpc, Cluster cluster, SecurityGroup appSecurityGroup,
                              ISecret databaseSecret, ISecret telegramSecret, String imageUri,
                              String environment, boolean production) {
            this.vpc = vpc;
            this.cluster = cluster;
            this.appSecurityGroup = appSecurityGroup;
            this.databaseSecret = databaseSecret;
            this.telegramSecret = telegramSecret;
            this.imageUri = imageUri;
            this.environment = environment;
            this.production = production;
        }

        public Vpc getVpc() { return vpc; }
        public Cluster getCluster() { return cluster; }
        public SecurityGroup getAppSecurityGroup() { return appSecurityGroup; }
        public ISecret getDatabaseSecret() { return databaseSecret; }
        public ISecret getTelegramSecret() { return telegramSecret; }
        public String getImageUri() { return imageUri; }
        public String getEnvironment() { return environment; }
        public boolean isProduction() { return production; }
    }
}