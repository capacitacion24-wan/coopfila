package com.coopfila.infrastructure;

import com.coopfila.infrastructure.constructs.*;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.ecs.Cluster;
import software.amazon.awscdk.services.secretsmanager.Secret;
import software.amazon.awscdk.services.secretsmanager.SecretStringGenerator;
import software.amazon.awscdk.services.sns.Topic;
import software.constructs.Construct;

import java.util.Map;

public class CoopFilaStack extends Stack {
    
    private final NetworkingConstruct networking;
    private final DatabaseConstruct database;
    private final ApplicationConstruct application;
    private final MonitoringConstruct monitoring;
    
    public CoopFilaStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public CoopFilaStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        // Get environment from context or default to 'dev'
        String environment = (String) this.getNode().tryGetContext("environment");
        if (environment == null) {
            environment = "dev";
        }
        boolean isProduction = "prod".equals(environment);
        
        // Get image URI from context
        String imageUri = (String) this.getNode().tryGetContext("imageUri");
        if (imageUri == null) {
            imageUri = "coopfila:latest"; // Default for local development
        }

        // 1. Create networking infrastructure
        this.networking = new NetworkingConstruct(this, "Networking",
                new NetworkingConstruct.NetworkingProps(environment, isProduction));

        // 2. Create database
        this.database = new DatabaseConstruct(this, "Database",
                new DatabaseConstruct.DatabaseProps(
                        networking.getVpc(),
                        networking.getDbSecurityGroup(),
                        environment,
                        isProduction
                ));

        // 3. Create Telegram bot secret
        Secret telegramSecret = Secret.Builder.create(this, "TelegramSecret")
                .description("Telegram bot token for CoopFila")
                .generateSecretString(SecretStringGenerator.builder()
                        .secretStringTemplate("{\"token\": \"REPLACE_WITH_ACTUAL_TOKEN\"}")
                        .generateStringKey("dummy")
                        .build())
                .build();

        // 4. Create ECS cluster
        Cluster cluster = Cluster.Builder.create(this, "Cluster")
                .clusterName("coopfila-" + environment)
                .vpc(networking.getVpc())
                .containerInsights(isProduction)
                .build();

        // 5. Create application
        this.application = new ApplicationConstruct(this, "Application",
                new ApplicationConstruct.ApplicationProps(
                        networking.getVpc(),
                        cluster,
                        networking.getAppSecurityGroup(),
                        database.getDatabaseSecret(),
                        telegramSecret,
                        imageUri,
                        environment,
                        isProduction
                ));

        // 6. Create SNS topic for alarms
        Topic alarmTopic = Topic.Builder.create(this, "AlarmTopic")
                .topicName("coopfila-" + environment + "-alarms")
                .displayName("CoopFila Alarms - " + environment)
                .build();

        // 7. Create monitoring
        this.monitoring = new MonitoringConstruct(this, "Monitoring",
                new MonitoringConstruct.MonitoringProps(
                        application.getFargateService().getService(),
                        application.getFargateService().getLoadBalancer(),
                        application.getFargateService().getTargetGroup(),
                        database.getDatabase(),
                        alarmTopic,
                        environment,
                        isProduction
                ));

        // Add stack-level tags
        Map<String, String> stackTags = Map.of(
                "Project", "CoopFila",
                "Environment", environment,
                "ManagedBy", "CDK"
        );
        
        stackTags.forEach((key, value) -> 
                software.amazon.awscdk.Tags.of(this).add(key, value));
    }
    
    // Getters for accessing constructs from tests or other stacks
    public NetworkingConstruct getNetworking() {
        return networking;
    }
    
    public DatabaseConstruct getDatabase() {
        return database;
    }
    
    public ApplicationConstruct getApplication() {
        return application;
    }
    
    public MonitoringConstruct getMonitoring() {
        return monitoring;
    }
}