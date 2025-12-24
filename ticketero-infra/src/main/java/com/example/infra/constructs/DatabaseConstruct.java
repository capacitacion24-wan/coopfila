package com.example.infra.constructs;

import com.example.infra.config.EnvironmentConfig;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.rds.*;
import software.amazon.awscdk.services.secretsmanager.*;
import software.constructs.Construct;
import java.util.List;

/**
 * RDS PostgreSQL con credenciales auto-generadas.
 */
public class DatabaseConstruct extends Construct {
    
    private final DatabaseInstance database;
    private final Secret credentials;
    
    public DatabaseConstruct(final Construct scope, final String id,
                              final EnvironmentConfig config,
                              final NetworkingConstruct networking) {
        super(scope, id);
        
        // Credenciales auto-generadas
        this.credentials = Secret.Builder.create(this, "DbCredentials")
            .secretName(config.resourceName("db-credentials"))
            .description("RDS PostgreSQL Credentials")
            .generateSecretString(SecretStringGenerator.builder()
                .secretStringTemplate("{\"username\": \"ticketero_user\"}")
                .generateStringKey("password")
                .excludePunctuation(true)
                .passwordLength(32)
                .build())
            .build();
        
        // RDS PostgreSQL
        this.database = DatabaseInstance.Builder.create(this, "Postgres")
            .instanceIdentifier(config.resourceName("db"))
            .engine(DatabaseInstanceEngine.postgres(
                PostgresInstanceEngineProps.builder()
                    .version(PostgresEngineVersion.VER_16)
                    .build()))
            .instanceType(InstanceType.of(
                config.dbInstanceClass(), config.dbInstanceSize()))
            .vpc(networking.getVpc())
            .vpcSubnets(SubnetSelection.builder()
                .subnetType(SubnetType.PRIVATE_WITH_EGRESS)
                .build())
            .securityGroups(List.of(networking.getRdsSg()))
            .credentials(Credentials.fromSecret(credentials))
            .databaseName("ticketero")
            .allocatedStorage(config.dbAllocatedStorage())
            .multiAz(config.multiAz())
            .backupRetention(Duration.days(7))
            .deletionProtection(config.isProd())
            .removalPolicy(config.isProd() ? RemovalPolicy.RETAIN : RemovalPolicy.DESTROY)
            .build();
    }
    
    public String getEndpoint() {
        return database.getDbInstanceEndpointAddress();
    }
    
    public Secret getCredentials() { return credentials; }
    public DatabaseInstance getDatabase() { return database; }
}