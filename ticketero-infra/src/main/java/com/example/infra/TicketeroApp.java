package com.example.infra;

import com.example.infra.config.EnvironmentConfig;
import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StackProps;

/**
 * Entry point para la aplicación CDK de Ticketero
 * 
 * Uso:
 * - Desarrollo: cdk deploy --context environment=dev
 * - Producción: cdk deploy --context environment=prod
 */
public class TicketeroApp {
    
    public static void main(final String[] args) {
        App app = new App();
        
        // Obtener ambiente desde contexto CDK
        String environmentName = app.getNode().tryGetContext("environment");
        if (environmentName == null) {
            environmentName = "dev"; // Default a desarrollo
        }
        
        // Cargar configuración del ambiente
        EnvironmentConfig config = EnvironmentConfig.forEnvironment(environmentName);
        
        // Obtener account y region desde variables de entorno o contexto
        String account = System.getenv("CDK_DEFAULT_ACCOUNT");
        String region = System.getenv("CDK_DEFAULT_REGION");
        
        if (account == null) {
            account = app.getNode().tryGetContext("account");
        }
        if (region == null) {
            region = app.getNode().tryGetContext("region");
            if (region == null) {
                region = "us-east-1"; // Default region
            }
        }
        
        // Crear environment para CDK
        Environment env = Environment.builder()
            .account(account)
            .region(region)
            .build();
        
        // Crear stack principal
        new TicketeroStack(app, "TicketeroStack-" + config.environment(), 
            StackProps.builder()
                .env(env)
                .description("Ticketero Infrastructure - " + config.environment().toUpperCase())
                .build(),
            config);
        
        // Sintetizar la aplicación
        app.synth();
    }
}