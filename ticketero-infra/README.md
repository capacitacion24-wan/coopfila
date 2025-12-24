# Ticketero Infrastructure (AWS CDK)

Infraestructura como código para el sistema Ticketero usando AWS CDK con Java 21.

## Arquitectura

```
Internet → ALB → ECS Fargate → RDS PostgreSQL
                     ↓
               Secrets Manager (Telegram Token)
```

## Recursos AWS

### Desarrollo (~$80/mes)
- VPC con 1 NAT Gateway
- RDS PostgreSQL t3.micro (20GB)
- ECS Fargate (1 task, 512 CPU, 1024 MB)
- ALB + ECR + Secrets Manager

### Producción (~$150/mes)
- VPC con 2 NAT Gateways (HA)
- RDS PostgreSQL t3.small (50GB, Multi-AZ)
- ECS Fargate (2 tasks, auto-scaling 2-4)
- CloudWatch Alarms + Dashboard

## Prerequisitos

```bash
# AWS CLI configurado
aws configure

# CDK CLI instalado
npm install -g aws-cdk

# Java 21 + Maven
java --version  # >= 21
mvn --version   # >= 3.9
```

## Comandos

### 1. Compilar y validar
```bash
cd ticketero-infra
mvn clean compile
cdk synth ticketero-dev
```

### 2. Ejecutar tests
```bash
mvn test
```

### 3. Deploy infraestructura
```bash
# Desarrollo
./deploy-dev.sh

# Producción
cdk deploy ticketero-prod
```

### 4. Build y deploy aplicación
```bash
# Completo (infra + app)
./build-and-deploy.sh ticketero-dev

# Solo aplicación (si infra ya existe)
./build-and-deploy.sh ticketero-dev
```

## Configuración Post-Deploy

### 1. Actualizar Token Telegram
```bash
aws secretsmanager put-secret-value \
  --secret-id ticketero-dev-telegram-token \
  --secret-string '{"token":"YOUR_REAL_BOT_TOKEN"}'
```

### 2. Verificar deployment
```bash
# Health check
curl http://ALB_DNS/actuator/health

# Crear ticket de prueba
curl -X POST http://ALB_DNS/api/tickets \
  -H "Content-Type: application/json" \
  -d '{"nationalId":"12345678","telefono":"+56912345678","branchOffice":"Centro","queueType":"CAJA"}'
```

## Estructura del Proyecto

```
src/main/java/com/example/infra/
├── TicketeroApp.java              # Entry point
├── TicketeroStack.java            # Stack principal
├── config/
│   └── EnvironmentConfig.java     # Configuración por ambiente
└── constructs/
    ├── NetworkingConstruct.java   # VPC + Security Groups
    ├── DatabaseConstruct.java     # RDS PostgreSQL
    ├── SecretsConstruct.java      # Secrets Manager
    ├── ContainerConstruct.java    # ECR + ECS + ALB
    └── MonitoringConstruct.java   # CloudWatch
```

## Comandos Útiles

```bash
# CDK
cdk ls                          # Listar stacks
cdk diff ticketero-dev          # Ver cambios
cdk destroy ticketero-dev       # Destruir stack

# AWS
aws logs tail /ecs/ticketero-dev-api --follow
aws ecs describe-services --cluster ticketero-dev-cluster --services ticketero-dev-service

# Docker
docker build -t ticketero .
docker run -p 8080:8080 ticketero
```

## Troubleshooting

### Error: "Stack does not exist"
```bash
# Verificar que el stack existe
aws cloudformation describe-stacks --stack-name ticketero-dev
```

### Error: "No space left on device" (ECR push)
```bash
# Limpiar imágenes Docker locales
docker system prune -a
```

### Error: "Service failed to stabilize"
```bash
# Ver logs del servicio
aws logs tail /ecs/ticketero-dev-api --follow

# Ver eventos del servicio
aws ecs describe-services --cluster ticketero-dev-cluster --services ticketero-dev-service
```

## Costos Estimados

| Recurso | Desarrollo | Producción |
|---------|------------|------------|
| RDS PostgreSQL | $15/mes | $35/mes |
| ECS Fargate | $25/mes | $50/mes |
| NAT Gateway | $32/mes | $64/mes |
| ALB | $18/mes | $18/mes |
| **Total** | **~$80/mes** | **~$150/mes** |

*Precios estimados para us-east-1, pueden variar por región y uso real.*