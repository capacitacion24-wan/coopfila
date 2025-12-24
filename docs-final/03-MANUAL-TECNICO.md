# Manual Técnico - CoopFila

**Sistema de Gestión de Tickets con Notificaciones en Tiempo Real**  
**Versión:** 1.0  
**Fecha:** Diciembre 2025  
**Audiencia:** Desarrolladores, DevOps y Administradores de Sistema

---

## 1. Arquitectura del Sistema

### 1.1 Stack Tecnológico

```
┌─────────────────────────────────────────────────────────┐
│                    STACK COMPLETO                       │
├─────────────────────────────────────────────────────────┤
│ Frontend:          Terminal Autoservicio (Externo)     │
│ Backend:           Spring Boot 3.2.11 + Java 21        │
│ Base de Datos:     PostgreSQL 15                       │
│ Mensajería:        Telegram Bot API                    │
│ Containerización:  Docker + Docker Compose             │
│ Build Tool:        Maven 3.9+                          │
│ Migraciones:       Flyway                              │
│ Monitoreo:         Spring Actuator                     │
└─────────────────────────────────────────────────────────┘
```

### 1.2 Componentes Principales

#### Backend API (Spring Boot)
- **Puerto:** 8080
- **Contexto:** /api
- **Endpoints:** REST JSON
- **Autenticación:** Básica para endpoints admin

#### Base de Datos (PostgreSQL)
- **Puerto:** 5432
- **Base de datos:** ticketero
- **Usuario:** ticketero_user
- **Esquema:** Gestionado por Flyway

#### Integración Externa (Telegram)
- **API:** https://api.telegram.org/bot
- **Método:** HTTP POST (RestTemplate)
- **Formato:** JSON

### 1.3 Flujo de Datos

```
Terminal → API REST → PostgreSQL
    ↓         ↓           ↓
Cliente   Lógica    Persistencia
          Negocio      
    ↓         ↓           ↓
Telegram ← Scheduler ← Mensajes
```

### 1.4 Patrones de Diseño

- **MVC:** Separación Controller-Service-Repository
- **Repository Pattern:** Abstracción de acceso a datos
- **DTO Pattern:** Transferencia de datos entre capas
- **Scheduler Pattern:** Procesamiento asíncrono
- **Builder Pattern:** Construcción de entidades (Lombok)

---

## 2. Requisitos del Sistema

### 2.1 Hardware

#### Desarrollo Local
- **CPU:** 2 cores mínimo, 4 cores recomendado
- **RAM:** 8GB mínimo, 16GB recomendado
- **Storage:** 20GB disponibles
- **Red:** Conexión a internet para Telegram API

#### Producción (Estimado para 1 sucursal)
- **CPU:** 2 vCPUs
- **RAM:** 4GB
- **Storage:** 50GB SSD
- **Red:** 10 Mbps simétrico
- **Base de datos:** 2 vCPUs, 4GB RAM, 100GB SSD

#### Escalabilidad por Sucursales
| Sucursales | CPU API | RAM API | CPU DB | RAM DB | Storage DB |
|------------|---------|---------|--------|--------|------------|
| 1          | 2 vCPU  | 4GB     | 2 vCPU | 4GB    | 100GB      |
| 5          | 4 vCPU  | 8GB     | 4 vCPU | 8GB    | 200GB      |
| 10         | 8 vCPU  | 16GB    | 8 vCPU | 16GB   | 500GB      |
| 50         | 16 vCPU | 32GB    | 16 vCPU| 32GB   | 1TB        |

### 2.2 Software

#### Requisitos Obligatorios
- **Java:** OpenJDK 21 LTS
- **Maven:** 3.9.0 o superior
- **Docker:** 20.10.0 o superior
- **Docker Compose:** 2.0.0 o superior
- **PostgreSQL:** 15.x (si no usa Docker)

#### Sistemas Operativos Soportados
- **Linux:** Ubuntu 20.04+, CentOS 8+, RHEL 8+
- **Windows:** Windows 10/11, Windows Server 2019+
- **macOS:** macOS 11+ (solo desarrollo)

#### Herramientas de Desarrollo (Opcionales)
- **IDE:** IntelliJ IDEA, Eclipse, VS Code
- **Git:** Para control de versiones
- **Postman:** Para testing de APIs
- **DBeaver:** Para administración de PostgreSQL

---

## 3. Instalación y Configuración

### 3.1 Desarrollo Local

#### Paso 1: Clonar Repositorio
```bash
git clone https://github.com/empresa/coopfila.git
cd coopfila
```

#### Paso 2: Configurar Variables de Entorno
```bash
# Copiar archivo de ejemplo
cp .env.example .env

# Editar variables (usar tu editor preferido)
nano .env
```

**Contenido del archivo .env:**
```bash
# Telegram Bot Configuration
TELEGRAM_BOT_TOKEN=tu_token_aqui

# Database Configuration
DATABASE_URL=jdbc:postgresql://localhost:5432/ticketero
DATABASE_USERNAME=ticketero_user
DATABASE_PASSWORD=ticketero_pass

# Spring Profile
SPRING_PROFILES_ACTIVE=dev
```

#### Paso 3: Levantar Base de Datos
```bash
# Solo PostgreSQL
docker-compose up -d postgres

# Verificar que esté corriendo
docker-compose ps
```

#### Paso 4: Ejecutar Aplicación
```bash
# Opción 1: Con Maven
mvn spring-boot:run

# Opción 2: Con Docker Compose (completo)
docker-compose up --build

# Opción 3: JAR compilado
mvn clean package -DskipTests
java -jar target/ticketero-1.0.0.jar
```

#### Paso 5: Verificar Instalación
```bash
# Health check
curl http://localhost:8080/actuator/health

# Respuesta esperada:
# {"status":"UP","components":{"db":{"status":"UP"}}}
```

### 3.2 Configuración de Telegram

#### Crear Bot con BotFather

1. **Abrir Telegram** y buscar @BotFather
2. **Enviar comando:** `/newbot`
3. **Seguir instrucciones:**
   ```
   BotFather: Alright, a new bot. How are we going to call it?
   Tú: CoopFila Bot
   
   BotFather: Good. Now let's choose a username for your bot.
   Tú: coopfila_bot
   
   BotFather: Done! Congratulations on your new bot.
   Token: 123456789:ABCdefGHIjklMNOpqrsTUVwxyz
   ```

4. **Copiar el token** al archivo .env

#### Configurar Webhook (Opcional)
```bash
# Para recibir mensajes del bot (no necesario para este proyecto)
curl -X POST "https://api.telegram.org/bot<TOKEN>/setWebhook" \
     -H "Content-Type: application/json" \
     -d '{"url": "https://tu-dominio.com/webhook"}'
```

#### Probar Bot
```bash
# Enviar mensaje de prueba
curl -X POST "https://api.telegram.org/bot<TOKEN>/sendMessage" \
     -H "Content-Type: application/json" \
     -d '{
       "chat_id": "TU_CHAT_ID",
       "text": "¡Hola desde CoopFila!"
     }'
```

### 3.3 Base de Datos

#### Configuración PostgreSQL Manual

Si no usas Docker, instala PostgreSQL manualmente:

```sql
-- Crear base de datos
CREATE DATABASE ticketero;

-- Crear usuario
CREATE USER ticketero_user WITH PASSWORD 'ticketero_pass';

-- Otorgar permisos
GRANT ALL PRIVILEGES ON DATABASE ticketero TO ticketero_user;
GRANT ALL ON SCHEMA public TO ticketero_user;
```

#### Migraciones Flyway

Las migraciones se ejecutan automáticamente al iniciar la aplicación:

```
src/main/resources/db/migration/
├── V1__create_ticket_table.sql
├── V2__create_mensaje_table.sql
├── V3__create_advisor_table.sql
├── V4__create_cliente_table.sql
└── V5__create_audit_log_table.sql
```

**Verificar migraciones:**
```sql
-- Conectar a PostgreSQL
psql -h localhost -U ticketero_user -d ticketero

-- Ver historial de migraciones
SELECT * FROM flyway_schema_history;

-- Ver tablas creadas
\dt
```

#### Datos Iniciales

El sistema incluye datos de ejemplo para desarrollo:

```sql
-- 5 asesores de ejemplo
INSERT INTO advisor (name, email, status, module_number) VALUES
('Juan Pérez', 'juan.perez@empresa.com', 'AVAILABLE', 1),
('Ana García', 'ana.garcia@empresa.com', 'AVAILABLE', 2),
('Luis Martínez', 'luis.martinez@empresa.com', 'AVAILABLE', 3),
('María López', 'maria.lopez@empresa.com', 'AVAILABLE', 4),
('Carlos Silva', 'carlos.silva@empresa.com', 'AVAILABLE', 5);
```

#### Backup y Restore

**Crear backup:**
```bash
# Backup completo
docker exec ticketero-postgres pg_dump -U ticketero_user ticketero > backup_$(date +%Y%m%d).sql

# Backup solo datos
docker exec ticketero-postgres pg_dump -U ticketero_user --data-only ticketero > data_backup.sql
```

**Restaurar backup:**
```bash
# Restaurar desde archivo
docker exec -i ticketero-postgres psql -U ticketero_user ticketero < backup_20251223.sql
```

---

## 4. Configuración por Ambientes

### 4.1 Desarrollo (application-dev.yml)

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/ticketero
    username: ticketero_user
    password: ticketero_pass
  
  jpa:
    show-sql: true
    hibernate:
      ddl-auto: validate
  
  flyway:
    enabled: true
    clean-disabled: false  # Permite limpiar BD en dev

logging:
  level:
    com.example.ticketero: DEBUG
    org.hibernate.SQL: DEBUG
    org.springframework.web: DEBUG

telegram:
  bot-token: ${TELEGRAM_BOT_TOKEN}
  api-url: https://api.telegram.org/bot
```

### 4.2 Producción (application-prod.yml)

```yaml
spring:
  datasource:
    url: ${DATABASE_URL}
    username: ${DATABASE_USERNAME}
    password: ${DATABASE_PASSWORD}
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
  
  jpa:
    show-sql: false
    hibernate:
      ddl-auto: validate
  
  flyway:
    enabled: true
    clean-disabled: true  # Protección en producción

logging:
  level:
    com.example.ticketero: INFO
    org.springframework: WARN
    org.hibernate: WARN
  file:
    name: /var/log/coopfila/application.log

telegram:
  bot-token: ${TELEGRAM_BOT_TOKEN}
  api-url: https://api.telegram.org/bot

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: when-authorized
```

### 4.3 Testing (application-test.yml)

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
    username: sa
    password: 
  
  jpa:
    hibernate:
      ddl-auto: create-drop
    database-platform: org.hibernate.dialect.H2Dialect
  
  flyway:
    enabled: false  # H2 usa create-drop

logging:
  level:
    com.example.ticketero: DEBUG
    org.springframework.test: DEBUG

telegram:
  bot-token: test-token
  api-url: http://localhost:8089/mock  # WireMock
```

---

## 5. Monitoreo y Logging

### 5.1 Logs de Aplicación

#### Niveles de Log
- **ERROR:** Errores críticos que requieren atención inmediata
- **WARN:** Situaciones anómalas que no interrumpen el funcionamiento
- **INFO:** Eventos importantes del negocio (creación tickets, asignaciones)
- **DEBUG:** Información detallada para desarrollo y troubleshooting

#### Formato de Logs
```
2025-01-15 14:30:25 [INFO ] [http-nio-8080-exec-1] c.e.t.service.TicketService - Creating ticket for cliente: 123
2025-01-15 14:30:25 [DEBUG] [http-nio-8080-exec-1] c.e.t.service.TicketService - Calculating position for queue: CAJA
2025-01-15 14:30:26 [INFO ] [http-nio-8080-exec-1] c.e.t.service.TicketService - Ticket created: C05
```

#### Configuración de Rotación
```yaml
logging:
  file:
    name: /var/log/coopfila/application.log
  logback:
    rollingpolicy:
      max-file-size: 100MB
      max-history: 30
      total-size-cap: 3GB
```

#### Logs por Componente
```yaml
logging:
  level:
    # Aplicación principal
    com.example.ticketero: INFO
    
    # Controllers (requests HTTP)
    com.example.ticketero.controller: INFO
    
    # Services (lógica de negocio)
    com.example.ticketero.service: INFO
    
    # Schedulers (tareas asíncronas)
    com.example.ticketero.scheduler: INFO
    
    # Telegram (integraciones)
    com.example.ticketero.service.TelegramService: DEBUG
    
    # Base de datos
    org.hibernate.SQL: WARN
    org.springframework.jdbc: WARN
```

### 5.2 Métricas (Spring Actuator)

#### Endpoints Disponibles
```bash
# Health check básico
curl http://localhost:8080/actuator/health

# Health check detallado
curl http://localhost:8080/actuator/health/db
curl http://localhost:8080/actuator/health/diskSpace

# Información de la aplicación
curl http://localhost:8080/actuator/info

# Métricas generales
curl http://localhost:8080/actuator/metrics

# Métricas específicas
curl http://localhost:8080/actuator/metrics/jvm.memory.used
curl http://localhost:8080/actuator/metrics/http.server.requests
```

#### Métricas de Negocio Personalizadas

El sistema expone métricas específicas del dominio:

```bash
# Tickets por estado
curl http://localhost:8080/actuator/metrics/tickets.by.status

# Asesores por estado
curl http://localhost:8080/actuator/metrics/advisors.by.status

# Mensajes Telegram
curl http://localhost:8080/actuator/metrics/telegram.messages.sent
curl http://localhost:8080/actuator/metrics/telegram.messages.failed
```

#### Configuración Prometheus (Opcional)
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

### 5.3 Health Checks

#### Health Check Personalizado
```java
@Component
public class TelegramHealthIndicator implements HealthIndicator {
    
    @Override
    public Health health() {
        try {
            // Verificar conectividad con Telegram
            telegramService.checkConnection();
            return Health.up()
                .withDetail("telegram", "Connected")
                .build();
        } catch (Exception e) {
            return Health.down()
                .withDetail("telegram", "Disconnected")
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}
```

#### Monitoreo Externo
```bash
# Script de monitoreo simple
#!/bin/bash
HEALTH_URL="http://localhost:8080/actuator/health"
RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" $HEALTH_URL)

if [ $RESPONSE -eq 200 ]; then
    echo "✅ CoopFila is healthy"
else
    echo "❌ CoopFila is down (HTTP $RESPONSE)"
    # Enviar alerta (email, Slack, etc.)
fi
```

---

## 6. Mantenimiento

### 6.1 Tareas Regulares

#### Diarias
```bash
# Verificar logs de errores
tail -f /var/log/coopfila/application.log | grep ERROR

# Verificar espacio en disco
df -h

# Verificar estado de contenedores
docker-compose ps
```

#### Semanales
```bash
# Backup de base de datos
./scripts/backup-database.sh

# Limpiar logs antiguos (si no hay rotación automática)
find /var/log/coopfila -name "*.log.*" -mtime +30 -delete

# Verificar métricas de performance
curl http://localhost:8080/actuator/metrics/jvm.memory.used
```

#### Mensuales
```bash
# Actualizar dependencias (revisar vulnerabilidades)
mvn versions:display-dependency-updates

# Revisar y optimizar queries de base de datos
# Analizar logs de queries lentas

# Verificar certificados SSL (si aplica)
openssl x509 -in cert.pem -text -noout
```

### 6.2 Troubleshooting

#### Problemas Comunes

**1. Aplicación no inicia**
```bash
# Verificar logs
docker-compose logs app

# Verificar conectividad a BD
docker exec ticketero-postgres pg_isready -U ticketero_user

# Verificar variables de entorno
docker exec ticketero-app env | grep -E "(DATABASE|TELEGRAM)"
```

**2. Base de datos no conecta**
```bash
# Verificar que PostgreSQL esté corriendo
docker-compose ps postgres

# Probar conexión manual
docker exec -it ticketero-postgres psql -U ticketero_user -d ticketero

# Verificar logs de PostgreSQL
docker-compose logs postgres
```

**3. Telegram no envía mensajes**
```bash
# Verificar token en logs (censurado)
grep "telegram" /var/log/coopfila/application.log

# Probar API manualmente
curl -X POST "https://api.telegram.org/bot<TOKEN>/getMe"

# Verificar conectividad
ping api.telegram.org
```

**4. Performance lenta**
```bash
# Verificar uso de CPU y memoria
docker stats

# Verificar queries lentas en PostgreSQL
docker exec ticketero-postgres psql -U ticketero_user -d ticketero -c "
SELECT query, mean_time, calls 
FROM pg_stat_statements 
ORDER BY mean_time DESC 
LIMIT 10;"

# Verificar métricas de la aplicación
curl http://localhost:8080/actuator/metrics/http.server.requests
```

#### Comandos Útiles

**Docker:**
```bash
# Ver logs en tiempo real
docker-compose logs -f app

# Reiniciar solo la aplicación
docker-compose restart app

# Reconstruir imagen
docker-compose up --build app

# Limpiar contenedores y volúmenes
docker-compose down -v
docker system prune -f
```

**Base de Datos:**
```bash
# Conectar a PostgreSQL
docker exec -it ticketero-postgres psql -U ticketero_user -d ticketero

# Ver tablas y tamaños
\dt+

# Ver conexiones activas
SELECT * FROM pg_stat_activity WHERE datname='ticketero';

# Ver queries lentas
SELECT * FROM pg_stat_statements ORDER BY mean_time DESC LIMIT 5;
```

**Aplicación:**
```bash
# Ver threads de Java
docker exec ticketero-app jstack 1

# Ver memoria de JVM
docker exec ticketero-app jstat -gc 1

# Ver variables de entorno
docker exec ticketero-app env
```

---

## 7. Seguridad

### 7.1 Configuración de Seguridad

#### Variables de Entorno
```bash
# ✅ CORRECTO: Variables en .env (no en código)
TELEGRAM_BOT_TOKEN=123456789:ABCdefGHIjklMNOpqrsTUVwxyz
DATABASE_PASSWORD=super_secure_password_2025

# ❌ INCORRECTO: Hardcoded en código
String token = "123456789:ABCdefGHIjklMNOpqrsTUVwxyz";
```

#### Secrets Management
```yaml
# Docker Compose con secrets
version: '3.8'
services:
  app:
    environment:
      - TELEGRAM_BOT_TOKEN_FILE=/run/secrets/telegram_token
    secrets:
      - telegram_token

secrets:
  telegram_token:
    file: ./secrets/telegram_token.txt
```

#### Network Security
```yaml
# Docker Compose con redes privadas
networks:
  backend:
    driver: bridge
    internal: true  # Sin acceso a internet
  frontend:
    driver: bridge

services:
  app:
    networks:
      - frontend
      - backend
  
  postgres:
    networks:
      - backend  # Solo acceso interno
```

### 7.2 Validación de Inputs

#### Bean Validation
```java
public record TicketRequest(
    @NotBlank(message = "RUT es obligatorio")
    @Pattern(regexp = "^[0-9]{7,8}-[0-9Kk]$", message = "Formato RUT inválido")
    String nationalId,
    
    @Pattern(regexp = "^\\+56[0-9]{9}$", message = "Formato teléfono inválido")
    String telefono
) {}
```

#### SQL Injection Prevention
```java
// ✅ CORRECTO: JPA/JPQL con parámetros
@Query("SELECT t FROM Ticket t WHERE t.nationalId = :nationalId")
Optional<Ticket> findByNationalId(@Param("nationalId") String nationalId);

// ❌ INCORRECTO: Concatenación de strings
String sql = "SELECT * FROM ticket WHERE national_id = '" + nationalId + "'";
```

### 7.3 Logging de Seguridad

#### Eventos de Seguridad
```java
@Component
public class SecurityAuditService {
    
    public void logSecurityEvent(String event, String user, String details) {
        log.warn("SECURITY_EVENT: {} by {} - {}", event, user, details);
        // Enviar a SIEM si existe
    }
}
```

#### Sanitización de Logs
```java
// ✅ CORRECTO: Sin datos sensibles
log.info("Ticket created for client: {}", clientId);

// ❌ INCORRECTO: Datos sensibles en logs
log.info("Ticket created for client: {} with phone: {}", clientId, phoneNumber);
```

---

## 8. Performance y Optimización

### 8.1 Configuración JVM

#### Parámetros Recomendados
```bash
# Desarrollo
JAVA_OPTS="-Xms512m -Xmx1g -XX:+UseG1GC"

# Producción
JAVA_OPTS="-Xms2g -Xmx4g -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+HeapDumpOnOutOfMemoryError"
```

#### Dockerfile Optimizado
```dockerfile
FROM eclipse-temurin:21-jre-alpine

# Configuración JVM
ENV JAVA_OPTS="-Xms512m -Xmx1g -XX:+UseG1GC"

# Usuario no-root
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

USER appuser

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

### 8.2 Configuración PostgreSQL

#### postgresql.conf Optimizado
```ini
# Memoria
shared_buffers = 256MB
effective_cache_size = 1GB
work_mem = 4MB

# Conexiones
max_connections = 100
shared_preload_libraries = 'pg_stat_statements'

# Logging
log_min_duration_statement = 1000  # Queries > 1s
log_statement = 'mod'  # INSERT, UPDATE, DELETE
```

#### Índices Optimizados
```sql
-- Índices existentes (ya creados por migraciones)
CREATE INDEX idx_ticket_status ON ticket(status);
CREATE INDEX idx_ticket_queue_type ON ticket(queue_type);
CREATE INDEX idx_ticket_created_at ON ticket(created_at DESC);

-- Índices compuestos para queries frecuentes
CREATE INDEX idx_ticket_status_queue ON ticket(status, queue_type);
CREATE INDEX idx_mensaje_estado_fecha ON mensaje(estado_envio, fecha_programada);
```

### 8.3 Connection Pooling

#### HikariCP Configuration
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
      leak-detection-threshold: 60000
```

---

## 9. Backup y Recovery

### 9.1 Estrategia de Backup

#### Backup Automático
```bash
#!/bin/bash
# backup-database.sh

DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="/var/backups/coopfila"
BACKUP_FILE="$BACKUP_DIR/ticketero_$DATE.sql"

# Crear directorio si no existe
mkdir -p $BACKUP_DIR

# Backup completo
docker exec ticketero-postgres pg_dump -U ticketero_user ticketero > $BACKUP_FILE

# Comprimir
gzip $BACKUP_FILE

# Limpiar backups antiguos (mantener 30 días)
find $BACKUP_DIR -name "*.sql.gz" -mtime +30 -delete

echo "Backup completed: $BACKUP_FILE.gz"
```

#### Cron Job para Backups
```bash
# Editar crontab
crontab -e

# Backup diario a las 2:00 AM
0 2 * * * /opt/coopfila/scripts/backup-database.sh

# Backup semanal completo los domingos
0 1 * * 0 /opt/coopfila/scripts/full-backup.sh
```

### 9.2 Disaster Recovery

#### RTO/RPO Objectives
- **RTO (Recovery Time Objective):** 4 horas
- **RPO (Recovery Point Objective):** 24 horas
- **Backup Frequency:** Diario
- **Retention:** 30 días local, 90 días remoto

#### Procedimiento de Recovery
```bash
# 1. Detener aplicación
docker-compose down

# 2. Restaurar base de datos
docker-compose up -d postgres
sleep 30

# 3. Restaurar desde backup
gunzip -c /var/backups/coopfila/ticketero_20251223_020000.sql.gz | \
docker exec -i ticketero-postgres psql -U ticketero_user ticketero

# 4. Verificar integridad
docker exec ticketero-postgres psql -U ticketero_user -d ticketero -c "
SELECT COUNT(*) FROM ticket;
SELECT COUNT(*) FROM advisor;
"

# 5. Reiniciar aplicación
docker-compose up -d app
```

---

## 10. Checklist de Configuración

### 10.1 Desarrollo Local
- [ ] Java 21 instalado
- [ ] Maven 3.9+ instalado
- [ ] Docker y Docker Compose instalados
- [ ] Archivo .env configurado con token de Telegram
- [ ] PostgreSQL corriendo (docker-compose up -d postgres)
- [ ] Aplicación inicia sin errores (mvn spring-boot:run)
- [ ] Health check responde OK (curl localhost:8080/actuator/health)
- [ ] Bot de Telegram responde a mensajes de prueba

### 10.2 Producción
- [ ] Servidor con recursos suficientes (CPU, RAM, Storage)
- [ ] Java 21 instalado
- [ ] Docker y Docker Compose instalados
- [ ] Variables de entorno configuradas en secrets
- [ ] Base de datos PostgreSQL configurada
- [ ] Backup automático configurado
- [ ] Monitoreo configurado (logs, métricas)
- [ ] Firewall configurado (solo puertos necesarios)
- [ ] SSL/TLS configurado (si aplica)
- [ ] Procedimientos de recovery documentados

### 10.3 Seguridad
- [ ] Secrets no están en código fuente
- [ ] Variables de entorno configuradas correctamente
- [ ] Usuario no-root para contenedores
- [ ] Red interna para base de datos
- [ ] Logs no contienen información sensible
- [ ] Validación de inputs implementada
- [ ] Auditoría de eventos de seguridad

---

## 11. Contacto y Soporte

### 11.1 Equipo Técnico
- **Tech Lead:** [nombre@empresa.com]
- **DevOps:** [devops@empresa.com]
- **DBA:** [dba@empresa.com]

### 11.2 Recursos
- **Repositorio:** https://github.com/empresa/coopfila
- **Documentación:** https://docs.coopfila.empresa.com
- **Monitoreo:** https://monitoring.coopfila.empresa.com
- **Logs:** https://logs.coopfila.empresa.com

### 11.3 Escalamiento
1. **Nivel 1:** Desarrollador del equipo
2. **Nivel 2:** Tech Lead
3. **Nivel 3:** Arquitecto de Software
4. **Nivel 4:** CTO

---

**Manual Técnico CoopFila v1.0**  
**Última actualización:** Diciembre 2025  
**Próxima revisión:** Marzo 2026