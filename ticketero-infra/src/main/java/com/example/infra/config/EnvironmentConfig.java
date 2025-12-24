package com.example.infra.config;

import software.amazon.awscdk.services.ec2.InstanceType;
import software.amazon.awscdk.services.rds.InstanceClass;
import software.amazon.awscdk.services.rds.InstanceSize;

/**
 * Configuración por ambiente para Ticketero
 * 
 * @param environment Nombre del ambiente (dev, prod)
 * @param vpcCidr CIDR block para VPC
 * @param rdsInstanceType Tipo de instancia RDS
 * @param ecsTaskCpu CPU para task ECS (en unidades CDK)
 * @param ecsTaskMemory Memoria para task ECS (en MB)
 * @param minCapacity Capacidad mínima auto-scaling
 * @param maxCapacity Capacidad máxima auto-scaling
 * @param multiAz Habilitar Multi-AZ para RDS
 * @param natGateways Número de NAT Gateways
 * @param logRetentionDays Días de retención CloudWatch Logs
 */
public record EnvironmentConfig(
    String environment,
    String vpcCidr,
    InstanceType rdsInstanceType,
    int ecsTaskCpu,
    int ecsTaskMemory,
    int minCapacity,
    int maxCapacity,
    boolean multiAz,
    int natGateways,
    int logRetentionDays
) {
    
    /**
     * Configuración para ambiente de desarrollo
     */
    public static EnvironmentConfig development() {
        return new EnvironmentConfig(
            "dev",
            "10.0.0.0/16",
            InstanceType.of(InstanceClass.T3, InstanceSize.MICRO),
            512,   // 0.5 vCPU
            1024,  // 1 GB RAM
            1,     // Min instances
            2,     // Max instances
            false, // Single-AZ
            1,     // 1 NAT Gateway
            7      // 7 days log retention
        );
    }
    
    /**
     * Configuración para ambiente de producción
     */
    public static EnvironmentConfig production() {
        return new EnvironmentConfig(
            "prod",
            "10.0.0.0/16",
            InstanceType.of(InstanceClass.T3, InstanceSize.SMALL),
            1024,  // 1 vCPU
            2048,  // 2 GB RAM
            2,     // Min instances
            4,     // Max instances
            true,  // Multi-AZ
            2,     // 2 NAT Gateways
            14     // 14 days log retention
        );
    }
    
    /**
     * Obtiene configuración basada en el nombre del ambiente
     */
    public static EnvironmentConfig forEnvironment(String env) {
        return switch (env.toLowerCase()) {
            case "dev", "development" -> development();
            case "prod", "production" -> production();
            default -> throw new IllegalArgumentException("Unknown environment: " + env);
        };
    }
    
    /**
     * Genera tags comunes para todos los recursos
     */
    public java.util.Map<String, String> getCommonTags() {
        return java.util.Map.of(
            "Environment", environment,
            "Project", "Ticketero",
            "ManagedBy", "CDK"
        );
    }
}