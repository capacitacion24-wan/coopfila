package com.coopfila.infrastructure;

import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StackProps;

public class InfrastructureApp {
    public static void main(final String[] args) {
        App app = new App();

        // Get environment from context
        String environment = (String) app.getNode().tryGetContext("environment");
        if (environment == null) {
            environment = "dev";
        }

        // Get AWS account and region from context or environment variables
        String account = (String) app.getNode().tryGetContext("account");
        if (account == null) {
            account = System.getenv("CDK_DEFAULT_ACCOUNT");
        }

        String region = (String) app.getNode().tryGetContext("region");
        if (region == null) {
            region = System.getenv("CDK_DEFAULT_REGION");
            if (region == null) {
                region = "us-east-1"; // Default region
            }
        }

        // Create stack props with environment
        StackProps stackProps = null;
        if (account != null && region != null) {
            stackProps = StackProps.builder()
                    .env(Environment.builder()
                            .account(account)
                            .region(region)
                            .build())
                    .build();
        }

        // Create the stack
        new CoopFilaStack(app, "CoopFila-" + environment, stackProps);

        app.synth();
    }
}