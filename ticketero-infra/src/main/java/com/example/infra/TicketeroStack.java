package com.example.infra;

import com.example.infra.config.EnvironmentConfig;
import com.example.infra.constructs.*;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.CfnOutput;
import software.constructs.Construct;

/**
 * Stack principal de Ticketero que orquesta todos los componentes
 */
public class TicketeroStack extends Stack {
    
    public TicketeroStack(final Construct scope, final String id, 
                         final StackProps props, final EnvironmentConfig config) {
        super(scope, id, props);
        
        // 1. Networking (VPC, Subnets, Security Groups)
        NetworkingConstruct networking = new NetworkingConstruct(this, "Networking", 
            NetworkingConstruct.Props.builder()
                .vpcCidr(config.vpcCidr())
                .natGateways(config.natGateways())
                .build());
        
        // 2. Secrets (Telegram Bot Token, DB Credentials)
        SecretsConstruct secrets = new SecretsConstruct(this, "Secrets",
            SecretsConstruct.Props.builder()
                .environment(config.environment())
                .build());
        
        // 3. Database (RDS PostgreSQL)
        DatabaseConstruct database = new DatabaseConstruct(this, "Database",
            DatabaseConstruct.Props.builder()
                .vpc(networking.getVpc())
                .databaseSecurityGroup(networking.getDatabaseSecurityGroup())
                .instanceType(config.rdsInstanceType())
                .multiAz(config.multiAz())
                .credentials(secrets.getDatabaseSecret())
                .build());
        
        // 4. Container Platform (ECR, ECS, Fargate, ALB)
        ContainerConstruct container = new ContainerConstruct(this, "Container",
            ContainerConstruct.Props.builder()
                .vpc(networking.getVpc())
                .applicationSecurityGroup(networking.getApplicationSecurityGroup())
                .albSecurityGroup(networking.getAlbSecurityGroup())
                .taskCpu(config.ecsTaskCpu())
                .taskMemory(config.ecsTaskMemory())
                .minCapacity(config.minCapacity())
                .maxCapacity(config.maxCapacity())
                .databaseEndpoint(database.getEndpoint())
                .databaseSecret(secrets.getDatabaseSecret())
                .telegramSecret(secrets.getTelegramSecret())
                .build());
        
        // 5. Monitoring (CloudWatch Logs, Alarms, Dashboard)
        MonitoringConstruct monitoring = new MonitoringConstruct(this, "Monitoring",
            MonitoringConstruct.Props.builder()
                .ecsService(container.getEcsService())
                .applicationLoadBalancer(container.getLoadBalancer())
                .database(database.getDatabaseInstance())
                .logRetentionDays(config.logRetentionDays())
                .environment(config.environment())
                .build());
        
        // Outputs importantes
        CfnOutput.Builder.create(this, "LoadBalancerDNS")
            .description("DNS del Application Load Balancer")
            .value(container.getLoadBalancer().getLoadBalancerDnsName())
            .build();
        
        CfnOutput.Builder.create(this, "DatabaseEndpoint")
            .description("Endpoint de la base de datos RDS")
            .value(database.getEndpoint())
            .build();
        
        CfnOutput.Builder.create(this, "ECRRepository")
            .description("URI del repositorio ECR")
            .value(container.getEcrRepository().getRepositoryUri())
            .build();
        
        CfnOutput.Builder.create(this, "TelegramSecretArn")
            .description("ARN del secret de Telegram Bot Token")
            .value(secrets.getTelegramSecret().getSecretArn())
            .build();
        
        // Aplicar tags comunes a todos los recursos
        config.getCommonTags().forEach((key, value) -> 
            this.getNode().setContext("aws:cdk:enable-path-metadata", true));
    }
}