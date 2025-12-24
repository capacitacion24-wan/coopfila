package com.example.infra.constructs;

import software.amazon.awscdk.services.ec2.*;
import software.constructs.Construct;

/**
 * Construct para networking: VPC, Subnets, Security Groups
 */
public class NetworkingConstruct extends Construct {
    
    private final Vpc vpc;
    private final SecurityGroup albSecurityGroup;
    private final SecurityGroup applicationSecurityGroup;
    private final SecurityGroup databaseSecurityGroup;
    
    public NetworkingConstruct(final Construct scope, final String id, final Props props) {
        super(scope, id);
        
        // VPC con subnets públicas y privadas
        this.vpc = Vpc.Builder.create(this, "VPC")
            .ipAddresses(IpAddresses.cidr(props.vpcCidr()))
            .maxAzs(2)
            .natGateways(props.natGateways())
            .subnetConfiguration(java.util.List.of(
                SubnetConfiguration.builder()
                    .name("Public")
                    .subnetType(SubnetType.PUBLIC)
                    .cidrMask(24)
                    .build(),
                SubnetConfiguration.builder()
                    .name("Private")
                    .subnetType(SubnetType.PRIVATE_WITH_EGRESS)
                    .cidrMask(24)
                    .build()
            ))
            .build();
        
        // Security Group para ALB
        this.albSecurityGroup = SecurityGroup.Builder.create(this, "ALBSecurityGroup")
            .vpc(vpc)
            .description("Security Group for Application Load Balancer")
            .allowAllOutbound(false)
            .build();
        
        // Permitir tráfico HTTP desde Internet
        albSecurityGroup.addIngressRule(
            Peer.anyIpv4(),
            Port.tcp(80),
            "Allow HTTP from Internet"
        );
        
        // Security Group para aplicación ECS
        this.applicationSecurityGroup = SecurityGroup.Builder.create(this, "ApplicationSecurityGroup")
            .vpc(vpc)
            .description("Security Group for ECS Application")
            .allowAllOutbound(false)
            .build();
        
        // Permitir tráfico desde ALB a aplicación
        applicationSecurityGroup.addIngressRule(
            Peer.securityGroupId(albSecurityGroup.getSecurityGroupId()),
            Port.tcp(8080),
            "Allow traffic from ALB"
        );
        
        // Permitir tráfico HTTPS saliente (para Telegram API)
        applicationSecurityGroup.addEgressRule(
            Peer.anyIpv4(),
            Port.tcp(443),
            "Allow HTTPS outbound for Telegram API"
        );
        
        // Security Group para base de datos
        this.databaseSecurityGroup = SecurityGroup.Builder.create(this, "DatabaseSecurityGroup")
            .vpc(vpc)
            .description("Security Group for RDS Database")
            .allowAllOutbound(false)
            .build();
        
        // Permitir tráfico desde aplicación a base de datos
        databaseSecurityGroup.addIngressRule(
            Peer.securityGroupId(applicationSecurityGroup.getSecurityGroupId()),
            Port.tcp(5432),
            "Allow PostgreSQL from Application"
        );
        
        // Permitir tráfico saliente desde ALB a aplicación
        albSecurityGroup.addEgressRule(
            Peer.securityGroupId(applicationSecurityGroup.getSecurityGroupId()),
            Port.tcp(8080),
            "Allow traffic to Application"
        );
        
        // Permitir tráfico saliente desde aplicación a base de datos
        applicationSecurityGroup.addEgressRule(
            Peer.securityGroupId(databaseSecurityGroup.getSecurityGroupId()),
            Port.tcp(5432),
            "Allow PostgreSQL to Database"
        );
    }
    
    public Vpc getVpc() {
        return vpc;
    }
    
    public SecurityGroup getAlbSecurityGroup() {
        return albSecurityGroup;
    }
    
    public SecurityGroup getApplicationSecurityGroup() {
        return applicationSecurityGroup;
    }
    
    public SecurityGroup getDatabaseSecurityGroup() {
        return databaseSecurityGroup;
    }
    
    /**
     * Props para NetworkingConstruct
     */
    public static class Props {
        private final String vpcCidr;
        private final int natGateways;
        
        private Props(Builder builder) {
            this.vpcCidr = builder.vpcCidr;
            this.natGateways = builder.natGateways;
        }
        
        public String vpcCidr() {
            return vpcCidr;
        }
        
        public int natGateways() {
            return natGateways;
        }
        
        public static Builder builder() {
            return new Builder();
        }
        
        public static class Builder {
            private String vpcCidr;
            private int natGateways;
            
            public Builder vpcCidr(String vpcCidr) {
                this.vpcCidr = vpcCidr;
                return this;
            }
            
            public Builder natGateways(int natGateways) {
                this.natGateways = natGateways;
                return this;
            }
            
            public Props build() {
                return new Props(this);
            }
        }
    }
}