package com.coopfila.infrastructure.constructs;

import software.amazon.awscdk.Duration;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.rds.*;
import software.amazon.awscdk.services.secretsmanager.Secret;
import software.amazon.awscdk.services.secretsmanager.SecretStringGenerator;
import software.constructs.Construct;

import java.util.Map;

/**
 * DatabaseConstruct - Defines RDS PostgreSQL instance for CoopFila
 */
public class DatabaseConstruct extends Construct {

    private final DatabaseInstance database;
    private final Secret databaseSecret;

    public DatabaseConstruct(Construct scope, String id, DatabaseProps props) {
        super(scope, id);

        // Create database credentials secret
        this.databaseSecret = Secret.Builder.create(this, "DatabaseSecret")
                .description("CoopFila database credentials")
                .generateSecretString(SecretStringGenerator.builder()
                        .secretStringTemplate("{\"username\": \"ticketero_user\"}")
                        .generateStringKey("password")
                        .excludeCharacters(" %+~`#$&*()|[]{}:;<>?!'/\"\\")
                        .passwordLength(32)
                        .build())
                .build();

        // Create subnet group for database
        SubnetGroup subnetGroup = SubnetGroup.Builder.create(this, "DatabaseSubnetGroup")
                .description("Subnet group for CoopFila database")
                .vpc(props.getVpc())
                .vpcSubnets(SubnetSelection.builder()
                        .subnetType(SubnetType.PRIVATE_ISOLATED)
                        .build())
                .build();

        // Create parameter group for PostgreSQL optimization
        ParameterGroup parameterGroup = ParameterGroup.Builder.create(this, "DatabaseParameterGroup")
                .engine(DatabaseInstanceEngine.postgres(PostgresInstanceEngineProps.builder()
                        .version(PostgresEngineVersion.VER_15_4)
                        .build()))
                .description("Parameter group for CoopFila PostgreSQL")
                .parameters(Map.of(
                        "shared_preload_libraries", "pg_stat_statements",
                        "log_statement", "all",
                        "log_min_duration_statement", "1000",
                        "max_connections", props.isProduction() ? "200" : "100"
                ))
                .build();

        // Create RDS instance
        this.database = DatabaseInstance.Builder.create(this, "Database")
                .engine(DatabaseInstanceEngine.postgres(PostgresInstanceEngineProps.builder()
                        .version(PostgresEngineVersion.VER_15_4)
                        .build()))
                .instanceType(props.isProduction() 
                        ? InstanceType.of(InstanceClass.T3, InstanceSize.MEDIUM)
                        : InstanceType.of(InstanceClass.T3, InstanceSize.MICRO))
                .credentials(Credentials.fromSecret(databaseSecret))
                .databaseName("ticketero")
                .vpc(props.getVpc())
                .vpcSubnets(SubnetSelection.builder()
                        .subnetType(SubnetType.PRIVATE_ISOLATED)
                        .build())
                .securityGroups(java.util.List.of(props.getDbSecurityGroup()))
                .subnetGroup(subnetGroup)
                .parameterGroup(parameterGroup)
                .allocatedStorage(props.isProduction() ? 100 : 20)
                .maxAllocatedStorage(props.isProduction() ? 1000 : 100)
                .storageType(StorageType.GP3)
                .storageEncrypted(true)
                .multiAz(props.isProduction())
                .backupRetention(Duration.days(props.isProduction() ? 7 : 1))
                .deletionProtection(props.isProduction())
                .removalPolicy(props.isProduction() ? RemovalPolicy.RETAIN : RemovalPolicy.DESTROY)
                .enablePerformanceInsights(props.isProduction())
                .performanceInsightRetention(props.isProduction() 
                        ? PerformanceInsightRetention.MONTHS_1 
                        : PerformanceInsightRetention.DEFAULT)
                .monitoringInterval(props.isProduction() ? Duration.seconds(60) : Duration.seconds(0))
                .build();

        // Add tags
        Map<String, String> tags = Map.of(
                "Project", "CoopFila",
                "Environment", props.getEnvironment(),
                "Component", "Database"
        );

        tags.forEach((key, value) -> {
            software.amazon.awscdk.Tags.of(database).add(key, value);
            software.amazon.awscdk.Tags.of(databaseSecret).add(key, value);
        });
    }

    public DatabaseInstance getDatabase() {
        return database;
    }

    public Secret getDatabaseSecret() {
        return databaseSecret;
    }

    public String getConnectionString() {
        return String.format("jdbc:postgresql://%s:5432/ticketero",
                database.getInstanceEndpoint().getHostname());
    }

    /**
     * Properties for DatabaseConstruct
     */
    public static class DatabaseProps {
        private final Vpc vpc;
        private final SecurityGroup dbSecurityGroup;
        private final String environment;
        private final boolean production;

        public DatabaseProps(Vpc vpc, SecurityGroup dbSecurityGroup, String environment, boolean production) {
            this.vpc = vpc;
            this.dbSecurityGroup = dbSecurityGroup;
            this.environment = environment;
            this.production = production;
        }

        public Vpc getVpc() {
            return vpc;
        }

        public SecurityGroup getDbSecurityGroup() {
            return dbSecurityGroup;
        }

        public String getEnvironment() {
            return environment;
        }

        public boolean isProduction() {
            return production;
        }
    }
}