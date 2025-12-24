package com.example.infra.constructs;

import com.example.infra.config.EnvironmentConfig;
import software.amazon.awscdk.services.secretsmanager.*;
import software.constructs.Construct;

/**
 * Secrets Manager para tokens y credenciales externas.
 */
public class SecretsConstruct extends Construct {
    
    private final Secret telegramSecret;
    
    public SecretsConstruct(final Construct scope, final String id,
                            final EnvironmentConfig config) {
        super(scope, id);
        
        // Secret Telegram (placeholder - debe actualizarse post-deploy)
        this.telegramSecret = Secret.Builder.create(this, "TelegramSecret")
            .secretName(config.resourceName("telegram-token"))
            .description("Telegram Bot Token - UPDATE AFTER DEPLOY")
            .generateSecretString(SecretStringGenerator.builder()
                .secretStringTemplate("{\"token\": \"PLACEHOLDER_UPDATE_AFTER_DEPLOY\"}")
                .generateStringKey("dummy")
                .build())
            .build();
    }
    
    public Secret getTelegramSecret() { return telegramSecret; }
}