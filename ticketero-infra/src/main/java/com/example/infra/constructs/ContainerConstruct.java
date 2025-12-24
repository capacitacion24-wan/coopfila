package com.example.infra.constructs;

import com.example.infra.config.EnvironmentConfig;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.services.ecr.Repository;
import software.amazon.awscdk.services.ecs.*;
import software.amazon.awscdk.services.ecs.patterns.*;
import software.amazon.awscdk.services.elasticloadbalancingv2.HealthCheck;
import software.constructs.Construct;
import java.util.List;
import java.util.Map;

/**
 * ECR, ECS Fargate, ALB con auto-scaling para Ticketero.
 */
public class ContainerConstruct extends Construct {
    
    private final Repository ecr;
    private final Cluster cluster;
    private final ApplicationLoadBalancedFargateService service;
    
    public ContainerConstruct(final Construct scope, final String id,
                               final EnvironmentConfig config,
                               final NetworkingConstruct networking,
                               final DatabaseConstruct database,
                               final SecretsConstruct secrets) {
        super(scope, id);
        
        // ECR
        this.ecr = Repository.Builder.create(this, "Ecr")
            .repositoryName(config.resourceName("api"))
            .imageScanOnPush(true)
            .build();
        
        // ECS Cluster
        this.cluster = Cluster.Builder.create(this, "Cluster")
            .vpc(networking.getVpc())
            .clusterName(config.resourceName("cluster"))
            .containerInsights(config.enableAlarms())
            .build();
        
        // Environment variables
        Map<String, String> environment = Map.of(
            "SPRING_PROFILES_ACTIVE", config.envName(),
            "SPRING_DATASOURCE_URL", "jdbc:postgresql://" + database.getEndpoint() + ":5432/ticketero"
        );
        
        // Secrets
        Map<String, Secret> secretsMap = Map.of(
            "SPRING_DATASOURCE_USERNAME", Secret.fromSecretsManager(database.getCredentials(), "username"),
            "SPRING_DATASOURCE_PASSWORD", Secret.fromSecretsManager(database.getCredentials(), "password"),
            "TELEGRAM_BOT_TOKEN", Secret.fromSecretsManager(secrets.getTelegramSecret(), "token")
        );
        
        // Fargate + ALB
        this.service = ApplicationLoadBalancedFargateService.Builder
            .create(this, "Service")
            .cluster(cluster)
            .serviceName(config.resourceName("service"))
            .desiredCount(config.desiredCount())
            .cpu(config.taskCpu())
            .memoryLimitMiB(config.taskMemory())
            .taskImageOptions(ApplicationLoadBalancedTaskImageOptions.builder()
                .image(ContainerImage.fromEcrRepository(ecr, "latest"))
                .containerPort(8080)
                .environment(environment)
                .secrets(secretsMap)
                .logDriver(LogDrivers.awsLogs(AwsLogDriverProps.builder()
                    .streamPrefix("ticketero")
                    .build()))
                .build())
            .publicLoadBalancer(true)
            .securityGroups(List.of(networking.getEcsSg()))
            .assignPublicIp(false)
            .build();
        
        // ALB Security Group
        service.getLoadBalancer().addSecurityGroup(networking.getAlbSg());
        
        // Health Check
        service.getTargetGroup().configureHealthCheck(HealthCheck.builder()
            .path("/actuator/health")
            .interval(Duration.seconds(30))
            .timeout(Duration.seconds(10))
            .healthyThresholdCount(2)
            .unhealthyThresholdCount(3)
            .build());
        
        // Auto-scaling
        ScalableTaskCount scaling = service.getService().autoScaleTaskCount(
            EnableScalingProps.builder()
                .minCapacity(config.minCapacity())
                .maxCapacity(config.maxCapacity())
                .build());
        
        scaling.scaleOnCpuUtilization("CpuScaling",
            CpuUtilizationScalingProps.builder()
                .targetUtilizationPercent(70)
                .scaleInCooldown(Duration.seconds(300))
                .scaleOutCooldown(Duration.seconds(60))
                .build());
    }
    
    public String getLoadBalancerDNS() {
        return service.getLoadBalancer().getLoadBalancerDnsName();
    }
    
    public String getEcrRepositoryUri() {
        return ecr.getRepositoryUri();
    }
    
    public ApplicationLoadBalancedFargateService getService() { return service; }
    public Repository getEcr() { return ecr; }
}