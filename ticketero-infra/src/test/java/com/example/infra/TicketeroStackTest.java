package com.example.infra;

import com.example.infra.config.EnvironmentConfig;
import org.junit.jupiter.api.Test;
import software.amazon.awscdk.App;
import software.amazon.awscdk.assertions.Template;
import java.util.Map;

class TicketeroStackTest {
    
    @Test
    void devStackCreatesAllResources() {
        App app = new App();
        TicketeroStack stack = new TicketeroStack(app, "Test", null, EnvironmentConfig.dev());
        Template template = Template.fromStack(stack);
        
        template.resourceCountIs("AWS::EC2::VPC", 1);
        template.resourceCountIs("AWS::EC2::Subnet", 4);
        template.resourceCountIs("AWS::EC2::NatGateway", 1);
        template.resourceCountIs("AWS::EC2::SecurityGroup", 3);
        template.resourceCountIs("AWS::RDS::DBInstance", 1);
        template.resourceCountIs("AWS::ECR::Repository", 1);
        template.resourceCountIs("AWS::ECS::Cluster", 1);
        template.resourceCountIs("AWS::ECS::Service", 1);
        template.resourceCountIs("AWS::ElasticLoadBalancingV2::LoadBalancer", 1);
        template.resourceCountIs("AWS::SecretsManager::Secret", 2);
        template.resourceCountIs("AWS::CloudWatch::Alarm", 0);
    }
    
    @Test
    void prodHasHighAvailability() {
        App app = new App();
        TicketeroStack stack = new TicketeroStack(app, "Test", null, EnvironmentConfig.prod());
        Template template = Template.fromStack(stack);
        
        template.resourceCountIs("AWS::EC2::NatGateway", 2);
        template.hasResourceProperties("AWS::RDS::DBInstance", Map.of("MultiAZ", true));
        template.resourceCountIs("AWS::CloudWatch::Alarm", 3);
        template.resourceCountIs("AWS::CloudWatch::Dashboard", 1);
    }
}