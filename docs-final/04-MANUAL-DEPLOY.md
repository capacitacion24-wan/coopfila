# Manual de Despliegue - CoopFila

**Sistema de Gestión de Tickets con Notificaciones en Tiempo Real**  
**Versión:** 1.0  
**Fecha:** Diciembre 2025  
**Audiencia:** DevOps Engineers y Administradores de Infraestructura

---

## 1. Introducción

### 1.1 Objetivos del Manual

Este manual proporciona instrucciones detalladas para desplegar CoopFila en diferentes ambientes:
- **Desarrollo Local:** Docker Compose para desarrollo
- **AWS Desarrollo:** Infraestructura básica en AWS
- **AWS Producción:** Infraestructura completa con alta disponibilidad

### 1.2 Arquitecturas de Despliegue

#### Local (Desarrollo)
```
Docker Host
├── PostgreSQL Container (puerto 5432)
├── CoopFila API Container (puerto 8080)
└── Volúmenes persistentes
```

#### AWS (Desarrollo/Producción)
```
Internet → Route 53 → ALB → ECS Fargate → RDS PostgreSQL
                              ↓
                        Secrets Manager
                              ↓
                        CloudWatch Logs
```

### 1.3 Estimación de Costos AWS

| Ambiente | Recursos | Costo Mensual | Descripción |
|----------|----------|---------------|-------------|
| **Desarrollo** | t3.micro RDS, 1 Fargate task | ~$80 USD | Ambiente básico |
| **Producción** | t3.small RDS Multi-AZ, 2+ Fargate tasks | ~$150 USD | Alta disponibilidad |
| **Escalado** | t3.medium+ RDS, Auto-scaling | $300+ USD | Múltiples sucursales |

---

## 2. Prerrequisitos

### 2.1 Herramientas Requeridas

#### Para Despliegue Local
```bash
# Verificar versiones
docker --version          # >= 20.10.0
docker-compose --version  # >= 2.0.0
java --version            # >= 21
mvn --version             # >= 3.9.0
```

#### Para Despliegue AWS
```bash
# AWS CLI
aws --version             # >= 2.0.0
aws configure list        # Verificar credenciales

# CDK CLI
npm install -g aws-cdk
cdk --version             # >= 2.100.0

# Permisos AWS requeridos
aws sts get-caller-identity
```

### 2.2 Configuración AWS

#### Credenciales AWS
```bash
# Opción 1: AWS CLI
aws configure
# AWS Access Key ID: AKIA...
# AWS Secret Access Key: ...
# Default region: us-east-1
# Default output format: json

# Opción 2: Variables de entorno
export AWS_ACCESS_KEY_ID=AKIA...
export AWS_SECRET_ACCESS_KEY=...
export AWS_DEFAULT_REGION=us-east-1

# Opción 3: IAM Role (recomendado para EC2/ECS)
# Configurar IAM Role con permisos necesarios
```

#### Permisos IAM Necesarios
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "cloudformation:*",
        "ec2:*",
        "ecs:*",
        "ecr:*",
        "rds:*",
        "secretsmanager:*",
        "elasticloadbalancing:*",
        "logs:*",
        "iam:CreateRole",
        "iam:AttachRolePolicy",
        "iam:PassRole"
      ],
      "Resource": "*"
    }
  ]
}
```

### 2.3 Configuración de Telegram Bot

#### Crear Bot con BotFather
```bash
# 1. Abrir Telegram y buscar @BotFather
# 2. Enviar: /newbot
# 3. Seguir instrucciones:
#    - Nombre: CoopFila Bot
#    - Username: coopfila_bot
# 4. Copiar token: 123456789:ABCdefGHIjklMNOpqrsTUVwxyz
```

#### Probar Bot
```bash
# Verificar que el bot funciona
curl "https://api.telegram.org/bot<TOKEN>/getMe"

# Respuesta esperada:
# {"ok":true,"result":{"id":123456789,"is_bot":true,"first_name":"CoopFila Bot"}}
```

---

## 3. Despliegue Local (Desarrollo)

### 3.1 Preparación del Entorno

#### Paso 1: Clonar Repositorio
```bash
git clone https://github.com/empresa/coopfila.git
cd coopfila
```

#### Paso 2: Configurar Variables de Entorno
```bash
# Copiar archivo de ejemplo
cp .env.example .env

# Editar con tu token real
nano .env
```

**Contenido del .env:**
```bash
# Telegram Bot Configuration
TELEGRAM_BOT_TOKEN=123456789:ABCdefGHIjklMNOpqrsTUVwxyz

# Database Configuration
DATABASE_URL=jdbc:postgresql://postgres:5432/ticketero
DATABASE_USERNAME=ticketero_user
DATABASE_PASSWORD=ticketero_pass

# Spring Profile
SPRING_PROFILES_ACTIVE=dev
```

### 3.2 Despliegue con Docker Compose

#### Opción 1: Despliegue Completo
```bash
# Levantar todo (PostgreSQL + API)
docker-compose up --build

# En background
docker-compose up -d --build
```

#### Opción 2: Solo Base de Datos
```bash
# Solo PostgreSQL (para desarrollo con IDE)
docker-compose up -d postgres

# Ejecutar API desde IDE o Maven
mvn spring-boot:run
```

#### Opción 3: Desarrollo con Hot Reload
```bash
# PostgreSQL en Docker, API en local
docker-compose up -d postgres

# API con hot reload
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Dspring.profiles.active=dev"
```

### 3.3 Verificación del Despliegue Local

#### Health Checks
```bash
# Verificar que PostgreSQL esté corriendo
docker-compose ps postgres

# Verificar que la API responda
curl http://localhost:8080/actuator/health

# Respuesta esperada:
# {"status":"UP","components":{"db":{"status":"UP"}}}
```

#### Pruebas Funcionales
```bash
# Crear un ticket de prueba
curl -X POST http://localhost:8080/api/tickets \
  -H "Content-Type: application/json" \
  -d '{
    "nationalId": "12345678-9",
    "telefono": "+56912345678",
    "branchOffice": "Sucursal Test",
    "queueType": "CAJA"
  }'

# Verificar respuesta HTTP 201 con JSON del ticket
```

#### Logs y Debugging
```bash
# Ver logs en tiempo real
docker-compose logs -f app

# Ver logs de PostgreSQL
docker-compose logs postgres

# Conectar a PostgreSQL para debugging
docker exec -it ticketero-postgres psql -U ticketero_user -d ticketero
```

---

## 4. Despliegue en AWS

### 4.1 Arquitectura AWS

#### Componentes Principales
```
┌─────────────────────────────────────────────────────────┐
│                        AWS CLOUD                        │
├─────────────────────────────────────────────────────────┤
│  Internet Gateway                                       │
│       ↓                                                 │
│  Application Load Balancer (ALB)                        │
│       ↓                                                 │
│  ECS Fargate Cluster                                    │
│  ├── Task Definition (CoopFila API)                     │
│  ├── Service (Auto Scaling 1-4 tasks)                  │
│  └── Container (512 CPU, 1024 MB)                      │
│       ↓                                                 │
│  RDS PostgreSQL                                         │
│  ├── t3.micro (dev) / t3.small (prod)                  │
│  ├── Multi-AZ (solo prod)                              │
│  └── Automated Backups                                 │
│       ↓                                                 │
│  Secrets Manager                                        │
│  └── Telegram Bot Token                                │
│       ↓                                                 │
│  CloudWatch                                             │
│  ├── Logs (/ecs/ticketero-api)                         │
│  ├── Metrics (CPU, Memory, HTTP)                       │
│  └── Alarms (CPU > 80%)                                │
└─────────────────────────────────────────────────────────┘
```

#### Networking
```
VPC (10.0.0.0/16)
├── Public Subnets (ALB)
│   ├── 10.0.1.0/24 (AZ-a)
│   └── 10.0.2.0/24 (AZ-b)
├── Private Subnets (ECS, RDS)
│   ├── 10.0.11.0/24 (AZ-a)
│   └── 10.0.12.0/24 (AZ-b)
└── NAT Gateways
    ├── 1 NAT (dev)
    └── 2 NAT (prod, HA)
```

### 4.2 Despliegue de Infraestructura (CDK)

#### Paso 1: Preparar CDK
```bash
cd ticketero-infra

# Compilar proyecto CDK
mvn clean compile

# Verificar sintaxis
cdk synth ticketero-dev
```

#### Paso 2: Bootstrap CDK (Primera vez)
```bash
# Configurar cuenta y región
export CDK_DEFAULT_ACCOUNT=$(aws sts get-caller-identity --query Account --output text)
export CDK_DEFAULT_REGION=us-east-1

# Bootstrap CDK
cdk bootstrap

# Verificar bootstrap
aws cloudformation describe-stacks --stack-name CDKToolkit
```

#### Paso 3: Deploy Infraestructura
```bash
# Desarrollo
./deploy-dev.sh

# O manualmente:
cdk deploy ticketero-dev --require-approval never

# Producción
cdk deploy ticketero-prod --require-approval never
```

#### Paso 4: Verificar Infraestructura
```bash
# Ver recursos creados
aws cloudformation describe-stacks --stack-name ticketero-dev

# Obtener outputs importantes
ECR_URI=$(aws cloudformation describe-stacks --stack-name ticketero-dev \
  --query 'Stacks[0].Outputs[?OutputKey==`EcrRepositoryUri`].OutputValue' --output text)

ALB_DNS=$(aws cloudformation describe-stacks --stack-name ticketero-dev \
  --query 'Stacks[0].Outputs[?OutputKey==`LoadBalancerDNS`].OutputValue' --output text)

echo "ECR: $ECR_URI"
echo "ALB: $ALB_DNS"
```

### 4.3 Despliegue de Aplicación

#### Paso 1: Build y Push Docker Image
```bash
# Obtener ECR URI
ECR_URI=$(aws cloudformation describe-stacks --stack-name ticketero-dev \
  --query 'Stacks[0].Outputs[?OutputKey==`EcrRepositoryUri`].OutputValue' --output text)

# Login a ECR
aws ecr get-login-password | docker login --username AWS --password-stdin $ECR_URI

# Build imagen
docker build -t $ECR_URI:latest .

# Push a ECR
docker push $ECR_URI:latest
```

#### Paso 2: Configurar Secrets
```bash
# Actualizar token de Telegram
aws secretsmanager put-secret-value \
  --secret-id ticketero-dev-telegram-token \
  --secret-string '{"token":"123456789:ABCdefGHIjklMNOpqrsTUVwxyz"}'

# Verificar secret
aws secretsmanager get-secret-value --secret-id ticketero-dev-telegram-token
```

#### Paso 3: Deploy Aplicación
```bash
# Forzar nuevo deployment de ECS
aws ecs update-service \
  --cluster ticketero-dev-cluster \
  --service ticketero-dev-service \
  --force-new-deployment

# Esperar que se estabilice
aws ecs wait services-stable \
  --cluster ticketero-dev-cluster \
  --services ticketero-dev-service
```

#### Paso 4: Script Automatizado
```bash
# Usar script completo
./build-and-deploy.sh ticketero-dev

# El script hace todo automáticamente:
# 1. Build Docker image
# 2. Push to ECR
# 3. Update ECS service
# 4. Wait for deployment
# 5. Test health check
```

### 4.4 Verificación del Despliegue AWS

#### Health Checks
```bash
# Obtener URL del ALB
ALB_DNS=$(aws cloudformation describe-stacks --stack-name ticketero-dev \
  --query 'Stacks[0].Outputs[?OutputKey==`LoadBalancerDNS`].OutputValue' --output text)

# Health check
curl http://$ALB_DNS/actuator/health

# Respuesta esperada:
# {"status":"UP","components":{"db":{"status":"UP"}}}
```

#### Pruebas Funcionales
```bash
# Crear ticket de prueba
curl -X POST http://$ALB_DNS/api/tickets \
  -H "Content-Type: application/json" \
  -d '{
    "nationalId": "12345678-9",
    "telefono": "+56912345678",
    "branchOffice": "Sucursal AWS",
    "queueType": "CAJA"
  }'

# Dashboard admin
curl http://$ALB_DNS/api/admin/dashboard
```

#### Monitoreo
```bash
# Ver logs de ECS
aws logs tail /ecs/ticketero-dev-api --follow

# Ver métricas de ECS
aws ecs describe-services \
  --cluster ticketero-dev-cluster \
  --services ticketero-dev-service

# Ver estado de RDS
aws rds describe-db-instances \
  --db-instance-identifier ticketero-dev-database
```

---

## 5. Configuración Post-Despliegue

### 5.1 Configuración de DNS (Opcional)

#### Route 53 Setup
```bash
# Crear hosted zone
aws route53 create-hosted-zone \
  --name coopfila.empresa.com \
  --caller-reference $(date +%s)

# Crear record A hacia ALB
aws route53 change-resource-record-sets \
  --hosted-zone-id Z123456789 \
  --change-batch '{
    "Changes": [{
      "Action": "CREATE",
      "ResourceRecordSet": {
        "Name": "api.coopfila.empresa.com",
        "Type": "A",
        "AliasTarget": {
          "DNSName": "'$ALB_DNS'",
          "EvaluateTargetHealth": false,
          "HostedZoneId": "Z35SXDOTRQ7X7K"
        }
      }
    }]
  }'
```

### 5.2 Configuración SSL/TLS (Opcional)

#### Certificate Manager
```bash
# Solicitar certificado SSL
aws acm request-certificate \
  --domain-name api.coopfila.empresa.com \
  --validation-method DNS

# Configurar ALB para usar HTTPS
# (Requiere modificar CDK stack)
```

### 5.3 Configuración de Monitoreo

#### CloudWatch Alarms
```bash
# Alarm para CPU alto
aws cloudwatch put-metric-alarm \
  --alarm-name "ticketero-dev-high-cpu" \
  --alarm-description "ECS CPU > 80%" \
  --metric-name CPUUtilization \
  --namespace AWS/ECS \
  --statistic Average \
  --period 300 \
  --threshold 80 \
  --comparison-operator GreaterThanThreshold \
  --evaluation-periods 2

# Alarm para errores HTTP
aws cloudwatch put-metric-alarm \
  --alarm-name "ticketero-dev-http-errors" \
  --alarm-description "ALB HTTP 5xx errors" \
  --metric-name HTTPCode_Target_5XX_Count \
  --namespace AWS/ApplicationELB \
  --statistic Sum \
  --period 300 \
  --threshold 10 \
  --comparison-operator GreaterThanThreshold \
  --evaluation-periods 1
```

---

## 6. Gestión de Ambientes

### 6.1 Estrategia de Ambientes

#### Ambientes Recomendados
```
Development (ticketero-dev)
├── Propósito: Desarrollo y testing
├── Recursos: Mínimos (t3.micro)
├── Datos: Sintéticos
└── Acceso: Equipo de desarrollo

Staging (ticketero-staging)
├── Propósito: Testing pre-producción
├── Recursos: Similares a producción
├── Datos: Copia de producción (anonimizada)
└── Acceso: QA y stakeholders

Production (ticketero-prod)
├── Propósito: Usuarios finales
├── Recursos: Alta disponibilidad
├── Datos: Reales
└── Acceso: Solo operaciones
```

### 6.2 Promoción entre Ambientes

#### Pipeline de Deployment
```bash
# 1. Desarrollo → Staging
git tag v1.2.3
./deploy-staging.sh v1.2.3

# 2. Staging → Producción (después de testing)
./deploy-production.sh v1.2.3

# 3. Rollback si es necesario
./rollback-production.sh v1.2.2
```

#### Blue/Green Deployment (Avanzado)
```bash
# Crear ambiente Green
cdk deploy ticketero-prod-green

# Cambiar tráfico gradualmente
# 10% → 50% → 100%

# Eliminar ambiente Blue
cdk destroy ticketero-prod-blue
```

### 6.3 Configuración por Ambiente

#### Variables por Ambiente
```yaml
# ticketero-dev
spring:
  profiles:
    active: dev
  datasource:
    url: jdbc:postgresql://ticketero-dev-db:5432/ticketero

# ticketero-prod
spring:
  profiles:
    active: prod
  datasource:
    url: jdbc:postgresql://ticketero-prod-db:5432/ticketero
```

---

## 7. Backup y Recovery

### 7.1 Estrategia de Backup

#### RDS Automated Backups
```bash
# Configurar backup automático (ya incluido en CDK)
aws rds modify-db-instance \
  --db-instance-identifier ticketero-prod-database \
  --backup-retention-period 7 \
  --preferred-backup-window "03:00-04:00"

# Crear snapshot manual
aws rds create-db-snapshot \
  --db-instance-identifier ticketero-prod-database \
  --db-snapshot-identifier ticketero-prod-$(date +%Y%m%d)
```

#### Backup de Secrets
```bash
# Exportar secrets (para disaster recovery)
aws secretsmanager get-secret-value \
  --secret-id ticketero-prod-telegram-token \
  --query SecretString --output text > telegram-token-backup.json
```

### 7.2 Disaster Recovery

#### RTO/RPO Objectives
- **RTO (Recovery Time Objective):** 2 horas
- **RPO (Recovery Point Objective):** 1 hora
- **Backup Frequency:** Continuo (RDS)
- **Cross-Region:** Opcional para prod

#### Procedimiento de Recovery
```bash
# 1. Crear nueva instancia desde snapshot
aws rds restore-db-instance-from-db-snapshot \
  --db-instance-identifier ticketero-prod-recovery \
  --db-snapshot-identifier ticketero-prod-20251223

# 2. Actualizar CDK para apuntar a nueva DB
# (Modificar DatabaseConstruct.java)

# 3. Deploy infraestructura actualizada
cdk deploy ticketero-prod

# 4. Verificar funcionamiento
curl http://$ALB_DNS/actuator/health
```

---

## 8. Monitoreo y Alertas

### 8.1 Métricas Clave

#### Métricas de Aplicación
```bash
# CPU y Memoria ECS
aws cloudwatch get-metric-statistics \
  --namespace AWS/ECS \
  --metric-name CPUUtilization \
  --start-time 2025-01-15T00:00:00Z \
  --end-time 2025-01-15T23:59:59Z \
  --period 3600 \
  --statistics Average

# Requests HTTP ALB
aws cloudwatch get-metric-statistics \
  --namespace AWS/ApplicationELB \
  --metric-name RequestCount \
  --start-time 2025-01-15T00:00:00Z \
  --end-time 2025-01-15T23:59:59Z \
  --period 3600 \
  --statistics Sum
```

#### Métricas de Base de Datos
```bash
# Conexiones RDS
aws cloudwatch get-metric-statistics \
  --namespace AWS/RDS \
  --metric-name DatabaseConnections \
  --dimensions Name=DBInstanceIdentifier,Value=ticketero-prod-database \
  --start-time 2025-01-15T00:00:00Z \
  --end-time 2025-01-15T23:59:59Z \
  --period 3600 \
  --statistics Average
```

### 8.2 Dashboard CloudWatch

#### Crear Dashboard
```bash
aws cloudwatch put-dashboard \
  --dashboard-name "CoopFila-Production" \
  --dashboard-body '{
    "widgets": [
      {
        "type": "metric",
        "properties": {
          "metrics": [
            ["AWS/ECS", "CPUUtilization", "ServiceName", "ticketero-prod-service"],
            ["AWS/ECS", "MemoryUtilization", "ServiceName", "ticketero-prod-service"]
          ],
          "period": 300,
          "stat": "Average",
          "region": "us-east-1",
          "title": "ECS Metrics"
        }
      }
    ]
  }'
```

### 8.3 Alertas SNS

#### Configurar Notificaciones
```bash
# Crear tópico SNS
aws sns create-topic --name coopfila-alerts

# Suscribir email
aws sns subscribe \
  --topic-arn arn:aws:sns:us-east-1:123456789:coopfila-alerts \
  --protocol email \
  --notification-endpoint devops@empresa.com

# Configurar alarm para enviar a SNS
aws cloudwatch put-metric-alarm \
  --alarm-name "ticketero-prod-critical-error" \
  --alarm-actions arn:aws:sns:us-east-1:123456789:coopfila-alerts
```

---

## 9. Troubleshooting

### 9.1 Problemas Comunes de Despliegue

#### Error: "Stack does not exist"
```bash
# Verificar que el stack existe
aws cloudformation describe-stacks --stack-name ticketero-dev

# Si no existe, hacer deploy inicial
cdk deploy ticketero-dev
```

#### Error: "No space left on device" (Docker)
```bash
# Limpiar imágenes Docker
docker system prune -a -f

# Limpiar volúmenes
docker volume prune -f

# Verificar espacio
df -h
```

#### Error: "Service failed to stabilize"
```bash
# Ver logs del servicio ECS
aws logs tail /ecs/ticketero-dev-api --follow

# Ver eventos del servicio
aws ecs describe-services \
  --cluster ticketero-dev-cluster \
  --services ticketero-dev-service \
  --query 'services[0].events'

# Verificar task definition
aws ecs describe-task-definition \
  --task-definition ticketero-dev-task
```

### 9.2 Problemas de Conectividad

#### RDS Connection Issues
```bash
# Verificar security groups
aws ec2 describe-security-groups \
  --group-names ticketero-dev-rds-sg

# Test conectividad desde ECS task
aws ecs execute-command \
  --cluster ticketero-dev-cluster \
  --task TASK_ID \
  --container ticketero-api \
  --interactive \
  --command "/bin/bash"

# Dentro del container:
nc -zv ticketero-dev-database.region.rds.amazonaws.com 5432
```

#### Telegram API Issues
```bash
# Verificar conectividad a Telegram
curl -v https://api.telegram.org/bot123456789:TOKEN/getMe

# Verificar secret en AWS
aws secretsmanager get-secret-value \
  --secret-id ticketero-dev-telegram-token

# Ver logs de la aplicación
aws logs filter-log-events \
  --log-group-name /ecs/ticketero-dev-api \
  --filter-pattern "telegram"
```

### 9.3 Performance Issues

#### High CPU/Memory
```bash
# Ver métricas en tiempo real
aws cloudwatch get-metric-statistics \
  --namespace AWS/ECS \
  --metric-name CPUUtilization \
  --dimensions Name=ServiceName,Value=ticketero-prod-service \
  --start-time $(date -u -d '1 hour ago' +%Y-%m-%dT%H:%M:%S) \
  --end-time $(date -u +%Y-%m-%dT%H:%M:%S) \
  --period 300 \
  --statistics Average,Maximum

# Escalar servicio temporalmente
aws ecs update-service \
  --cluster ticketero-prod-cluster \
  --service ticketero-prod-service \
  --desired-count 4
```

#### Database Performance
```bash
# Ver queries lentas en RDS
aws rds describe-db-log-files \
  --db-instance-identifier ticketero-prod-database

# Habilitar Performance Insights
aws rds modify-db-instance \
  --db-instance-identifier ticketero-prod-database \
  --enable-performance-insights
```

---

## 10. Rollback Procedures

### 10.1 Rollback de Aplicación

#### Rollback ECS Service
```bash
# Ver historial de task definitions
aws ecs list-task-definitions \
  --family-prefix ticketero-prod-task \
  --sort DESC

# Rollback a versión anterior
PREVIOUS_TASK_DEF="ticketero-prod-task:5"
aws ecs update-service \
  --cluster ticketero-prod-cluster \
  --service ticketero-prod-service \
  --task-definition $PREVIOUS_TASK_DEF

# Esperar estabilización
aws ecs wait services-stable \
  --cluster ticketero-prod-cluster \
  --services ticketero-prod-service
```

#### Rollback Docker Image
```bash
# Ver tags en ECR
aws ecr describe-images \
  --repository-name ticketero-prod \
  --query 'imageDetails[*].imageTags' \
  --output table

# Hacer rollback a tag específico
PREVIOUS_TAG="v1.2.2"
ECR_URI=$(aws cloudformation describe-stacks --stack-name ticketero-prod \
  --query 'Stacks[0].Outputs[?OutputKey==`EcrRepositoryUri`].OutputValue' --output text)

# Update task definition con imagen anterior
# (Requiere modificar CDK y re-deploy)
```

### 10.2 Rollback de Infraestructura

#### CDK Rollback
```bash
# Ver historial de deployments
aws cloudformation describe-stack-events \
  --stack-name ticketero-prod \
  --query 'StackEvents[?ResourceStatus==`UPDATE_COMPLETE`]'

# Rollback automático de CloudFormation
aws cloudformation cancel-update-stack \
  --stack-name ticketero-prod

# O rollback manual con CDK
git checkout v1.2.2
cdk deploy ticketero-prod
```

---

## 11. Checklist de Despliegue

### 11.1 Pre-Despliegue
- [ ] Código testeado y aprobado
- [ ] Variables de entorno configuradas
- [ ] Token de Telegram válido
- [ ] Credenciales AWS configuradas
- [ ] Backup de producción realizado (si aplica)
- [ ] Ventana de mantenimiento comunicada

### 11.2 Durante el Despliegue
- [ ] Infraestructura desplegada exitosamente
- [ ] Imagen Docker construida y subida a ECR
- [ ] Secrets actualizados en AWS Secrets Manager
- [ ] Servicio ECS desplegado y estable
- [ ] Health checks pasando
- [ ] Pruebas funcionales básicas ejecutadas

### 11.3 Post-Despliegue
- [ ] Monitoreo configurado y funcionando
- [ ] Alertas configuradas
- [ ] Logs accesibles y sin errores críticos
- [ ] Performance dentro de parámetros normales
- [ ] Stakeholders notificados del despliegue exitoso
- [ ] Documentación actualizada

---

## 12. Contacto y Escalamiento

### 12.1 Equipo de DevOps
- **DevOps Lead:** [devops-lead@empresa.com]
- **Cloud Architect:** [cloud-architect@empresa.com]
- **On-Call Engineer:** [oncall@empresa.com]

### 12.2 Recursos de Soporte
- **Runbooks:** https://wiki.empresa.com/coopfila/runbooks
- **Monitoring:** https://monitoring.empresa.com/coopfila
- **Incident Management:** https://incident.empresa.com

### 12.3 Escalamiento de Incidentes
1. **Nivel 1:** DevOps Engineer (15 min)
2. **Nivel 2:** DevOps Lead (30 min)
3. **Nivel 3:** Cloud Architect (1 hora)
4. **Nivel 4:** CTO (2 horas)

---

**Manual de Despliegue CoopFila v1.0**  
**Última actualización:** Diciembre 2025  
**Próxima revisión:** Marzo 2026