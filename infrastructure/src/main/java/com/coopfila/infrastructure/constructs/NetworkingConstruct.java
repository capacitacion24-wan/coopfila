package com.coopfila.infrastructure.constructs;

import software.amazon.awscdk.services.ec2.*;
import software.constructs.Construct;

import java.util.List;
import java.util.Map;

/**
 * NetworkingConstruct - Defines VPC, subnets, and security groups for CoopFila
 */
public class NetworkingConstruct extends Construct {

    private final Vpc vpc;
    private final SecurityGroup appSecurityGroup;
    private final SecurityGroup dbSecurityGroup;
    private final SecurityGroup albSecurityGroup;

    public NetworkingConstruct(Construct scope, String id, NetworkingProps props) {
        super(scope, id);

        // Create VPC with public and private subnets
        this.vpc = Vpc.Builder.create(this, "CoopFilaVpc")
                .ipAddresses(IpAddresses.cidr("10.0.0.0/16"))
                .maxAzs(2)
                .subnetConfiguration(List.of(
                        SubnetConfiguration.builder()
                                .name("PublicSubnet")
                                .subnetType(SubnetType.PUBLIC)
                                .cidrMask(24)
                                .build(),
                        SubnetConfiguration.builder()
                                .name("PrivateSubnet")
                                .subnetType(SubnetType.PRIVATE_WITH_EGRESS)
                                .cidrMask(24)
                                .build(),
                        SubnetConfiguration.builder()
                                .name("DatabaseSubnet")
                                .subnetType(SubnetType.PRIVATE_ISOLATED)
                                .cidrMask(24)
                                .build()
                ))
                .natGateways(props.isProduction() ? 2 : 1)
                .enableDnsHostnames(true)
                .enableDnsSupport(true)
                .build();

        // ALB Security Group - Internet facing
        this.albSecurityGroup = SecurityGroup.Builder.create(this, "AlbSecurityGroup")
                .vpc(vpc)
                .description("Security group for Application Load Balancer")
                .allowAllOutbound(true)
                .build();

        albSecurityGroup.addIngressRule(
                Peer.anyIpv4(),
                Port.tcp(80),
                "Allow HTTP traffic from internet"
        );

        albSecurityGroup.addIngressRule(
                Peer.anyIpv4(),
                Port.tcp(443),
                "Allow HTTPS traffic from internet"
        );

        // Application Security Group - ECS tasks
        this.appSecurityGroup = SecurityGroup.Builder.create(this, "AppSecurityGroup")
                .vpc(vpc)
                .description("Security group for CoopFila application")
                .allowAllOutbound(true)
                .build();

        appSecurityGroup.addIngressRule(
                albSecurityGroup,
                Port.tcp(8080),
                "Allow traffic from ALB to application"
        );

        // Database Security Group - RDS
        this.dbSecurityGroup = SecurityGroup.Builder.create(this, "DbSecurityGroup")
                .vpc(vpc)
                .description("Security group for PostgreSQL database")
                .allowAllOutbound(false)
                .build();

        dbSecurityGroup.addIngressRule(
                appSecurityGroup,
                Port.tcp(5432),
                "Allow PostgreSQL access from application"
        );

        // Add tags
        Map<String, String> tags = Map.of(
                "Project", "CoopFila",
                "Environment", props.getEnvironment(),
                "Component", "Networking"
        );

        tags.forEach((key, value) -> {
            software.amazon.awscdk.Tags.of(vpc).add(key, value);
            software.amazon.awscdk.Tags.of(appSecurityGroup).add(key, value);
            software.amazon.awscdk.Tags.of(dbSecurityGroup).add(key, value);
            software.amazon.awscdk.Tags.of(albSecurityGroup).add(key, value);
        });
    }

    public Vpc getVpc() {
        return vpc;
    }

    public SecurityGroup getAppSecurityGroup() {
        return appSecurityGroup;
    }

    public SecurityGroup getDbSecurityGroup() {
        return dbSecurityGroup;
    }

    public SecurityGroup getAlbSecurityGroup() {
        return albSecurityGroup;
    }

    public List<ISubnet> getPublicSubnets() {
        return vpc.getPublicSubnets();
    }

    public List<ISubnet> getPrivateSubnets() {
        return vpc.getPrivateSubnets();
    }

    public List<ISubnet> getIsolatedSubnets() {
        return vpc.getIsolatedSubnets();
    }

    /**
     * Properties for NetworkingConstruct
     */
    public static class NetworkingProps {
        private final String environment;
        private final boolean production;

        public NetworkingProps(String environment, boolean production) {
            this.environment = environment;
            this.production = production;
        }

        public String getEnvironment() {
            return environment;
        }

        public boolean isProduction() {
            return production;
        }
    }
}