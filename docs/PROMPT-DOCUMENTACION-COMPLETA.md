# PROMPT: DOCUMENTACIÓN COMPLETA DEL PROYECTO COOPFILA

## Contexto

Eres un Technical Writer Senior especializado en documentación de software. Necesitas crear documentación completa y profesional para el proyecto **"CoopFila"** - Sistema de Gestión de Tickets con Notificaciones en Tiempo Real.

**Características del proyecto:**
- API REST con Spring Boot 3.2, Java 21
- Base de datos PostgreSQL 15
- Notificaciones vía Telegram Bot API
- Docker + Docker Compose
- Suite completa de tests (unitarios, integración, E2E, NFR)
- Infraestructura AWS con CDK

**IMPORTANTE:** Después de completar CADA documento, debes DETENERTE y solicitar una revisión exhaustiva antes de continuar.

---

## Documentos de Entrada

Lee y analiza estos archivos del proyecto:
- `docs/ARQUITECTURA.md` - Arquitectura del sistema
- `docs/MODELO-DATOS.md` - Modelo de base de datos
- `docs/project-requirements.md` - Requerimientos del proyecto
- `docs/PLAN-IMPLEMENTACION.md` - Plan de implementación
- `docs/NFR-TEST-RESULTS.md` - Resultados de pruebas
- `src/main/java/` - Código fuente completo
- `target/surefire-reports/` - Reportes de tests
- `scripts/` - Scripts de testing y deployment
- `docker-compose.yml` - Configuración de contenedores
- `pom.xml` - Configuración Maven

---

## Metodología de Trabajo

**Principio:**
"Analizar → Documentar → Validar → Confirmar → Continuar"

**Después de CADA documento:**
- ✅ Analiza el código y archivos relacionados
- ✅ Crea documentación completa y profesional
- ✅ Valida que la información sea precisa
- ⏸️ **DETENTE** y solicita revisión
- ✅ Espera confirmación antes de continuar

**Formato de Solicitud de Revisión:**
```
✅ DOCUMENTO X COMPLETADO

Documento creado:
- [Nombre del documento]

Contenido incluido:
- [Lista de secciones principales]

Archivos analizados:
- [Lista de archivos fuente consultados]

🔍 SOLICITO REVISIÓN:
- ¿El contenido es preciso y completo?
- ¿El formato es profesional?
- ¿Puedo continuar con el siguiente documento?

⏸️ ESPERANDO CONFIRMACIÓN...
```

---

## Estructura de Documentación a Crear

```
docs-final/
├── 01-DOCUMENTACION-CODIGO.md          # Documentación técnica del código
├── 02-MANUAL-USUARIO.md                # Manual para usuarios finales
├── 03-MANUAL-TECNICO.md                # Manual para desarrolladores
├── 04-MANUAL-DEPLOY.md                 # Manual de despliegue
├── 05-INFORME-PRUEBAS.md               # Informe completo de testing
├── 06-INTEGRACIONES-ENDPOINTS.md       # Documentación de APIs
├── 07-MODELO-DATOS.md                  # Copia del modelo de datos existente
├── 08-RESUMEN-EJECUTIVO.md             # Resumen para stakeholders
└── README.md                           # Índice general de documentación
```

---

## Tu Tarea: 8 Documentos + README

| Paso | Documento | Descripción | Audiencia |
|------|-----------|-------------|-----------|
| **PASO 1** | Documentación Código | Arquitectura, clases, métodos | Desarrolladores |
| **PASO 2** | Manual Usuario | Guía para usuarios finales | Usuarios/Supervisores |
| **PASO 3** | Manual Técnico | Setup, configuración, troubleshooting | DevOps/Sysadmin |
| **PASO 4** | Manual Deploy | Despliegue local y AWS | DevOps |
| **PASO 5** | Informe Pruebas | Cobertura, resultados, métricas | QA/Management |
| **PASO 6** | Integraciones/Endpoints | APIs, contratos, ejemplos | Integradores |
| **PASO 7** | Modelo Datos | Copia del existente + validación | DBAs/Arquitectos |
| **PASO 8** | Resumen Ejecutivo + README | Overview del proyecto | Stakeholders |

---

## PASO 1: Documentación del Código

**Objetivo:** Crear documentación técnica completa del código fuente.

### Estructura del Documento:

```markdown
# Documentación Técnica del Código - CoopFila

## 1. Arquitectura General
- Diagrama de capas
- Patrón de diseño utilizado
- Principios SOLID aplicados

## 2. Estructura del Proyecto
- Organización de packages
- Convenciones de naming
- Dependencias principales

## 3. Capa de Controladores
### 3.1 TicketController
- Endpoints expuestos
- Validaciones aplicadas
- Manejo de errores
- Ejemplos de uso

### 3.2 AdminController
- Endpoints administrativos
- Seguridad implementada
- Métricas expuestas

## 4. Capa de Servicios
### 4.1 TicketService
- Lógica de negocio principal
- Reglas implementadas (RN-001 a RN-013)
- Transacciones
- Métodos públicos y privados

### 4.2 TelegramService
- Integración con Telegram API
- Manejo de errores
- Rate limiting
- Formato de mensajes

### 4.3 Otros Services
- QueueManagementService
- AdvisorService
- NotificationService
- AuditService

## 5. Capa de Datos
### 5.1 Entities
- Mapeo JPA
- Relaciones entre entidades
- Validaciones
- Lifecycle callbacks

### 5.2 Repositories
- Queries personalizadas
- Métodos derivados
- Performance considerations

## 6. DTOs y Validaciones
- Request DTOs
- Response DTOs
- Bean Validation
- Mappers

## 7. Schedulers
- MessageScheduler
- QueueProcessorScheduler
- Configuración de timing
- Manejo de concurrencia

## 8. Configuración
- Application properties
- Profiles (dev/prod)
- Bean configuration
- Security settings

## 9. Manejo de Excepciones
- Exception hierarchy
- GlobalExceptionHandler
- Error responses
- Logging strategy

## 10. Testing
- Estructura de tests
- Mocks utilizados
- Test data builders
- Coverage reports
```

**Archivos a analizar:**
- Todo el contenido de `src/main/java/`
- `src/test/java/` para entender la estrategia de testing
- `pom.xml` para dependencias
- `application.yml` para configuración

---

## PASO 2: Manual de Usuario

**Objetivo:** Crear guía completa para usuarios finales del sistema.

### Estructura del Documento:

```markdown
# Manual de Usuario - CoopFila

## 1. Introducción
- ¿Qué es CoopFila?
- Beneficios para el cliente
- Requisitos previos

## 2. Guía Rápida
- Proceso completo en 5 pasos
- Tiempo estimado
- Qué necesitas

## 3. Obtener un Ticket
### 3.1 En el Terminal
- Paso a paso con capturas
- Campos obligatorios
- Tipos de atención disponibles
- Qué hacer si hay errores

### 3.2 Información del Ticket
- Número de ticket
- Posición en cola
- Tiempo estimado
- Código de referencia

## 4. Notificaciones Telegram
### 4.1 Configuración Inicial
- Cómo configurar Telegram
- Agregar número de teléfono
- Verificar notificaciones

### 4.2 Tipos de Mensajes
- Confirmación de ticket
- Pre-aviso (3 personas adelante)
- Turno activo (ir al módulo)

## 5. Durante la Espera
- ¿Puedo salir de la sucursal?
- Cómo consultar mi posición
- Qué hacer si cambia mi situación

## 6. Atención en Módulo
- Cómo identificar mi módulo
- Información del asesor
- Qué llevar a la atención

## 7. Preguntas Frecuentes
- ¿Qué pasa si no tengo Telegram?
- ¿Puedo cancelar mi ticket?
- ¿Qué pasa si llego tarde?
- ¿Puedo tener múltiples tickets?

## 8. Solución de Problemas
- No recibo notificaciones
- Error al crear ticket
- Perdí mi número de ticket
- Contacto para soporte

## 9. Panel de Supervisor
### 9.1 Acceso al Dashboard
- URL de acceso
- Información mostrada
- Actualización automática

### 9.2 Gestión de Asesores
- Cambiar estado de asesor
- Ver estadísticas
- Alertas del sistema

## 10. Consejos y Mejores Prácticas
- Horarios de menor afluencia
- Preparación para la atención
- Uso eficiente del sistema
```

**Información a incluir:**
- Screenshots simulados del proceso
- Ejemplos reales de mensajes Telegram
- Casos de uso comunes
- Troubleshooting básico

---

## PASO 3: Manual Técnico

**Objetivo:** Guía completa para desarrolladores y administradores del sistema.

### Estructura del Documento:

```markdown
# Manual Técnico - CoopFila

## 1. Arquitectura del Sistema
- Stack tecnológico
- Componentes principales
- Flujo de datos
- Patrones de diseño

## 2. Requisitos del Sistema
### 2.1 Hardware
- CPU, RAM, Storage
- Estimaciones por ambiente
- Escalabilidad

### 2.2 Software
- Java 21
- PostgreSQL 15
- Docker & Docker Compose
- Maven 3.9+

## 3. Instalación y Configuración
### 3.1 Desarrollo Local
- Clonar repositorio
- Configurar variables de entorno
- Levantar base de datos
- Ejecutar aplicación

### 3.2 Configuración de Telegram
- Crear bot con BotFather
- Obtener token
- Configurar webhook (opcional)

### 3.3 Base de Datos
- Configuración PostgreSQL
- Migraciones Flyway
- Datos iniciales
- Backup y restore

## 4. Configuración por Ambientes
### 4.1 Desarrollo
- application-dev.yml
- Docker Compose local
- Variables de entorno

### 4.2 Producción
- application-prod.yml
- Configuración AWS
- Secrets management
- Monitoring

## 5. Monitoreo y Logging
### 5.1 Logs de Aplicación
- Niveles de log
- Formato de logs
- Rotación de archivos

### 5.2 Métricas
- Actuator endpoints
- Métricas de negocio
- Health checks

### 5.3 Alertas
- Condiciones de alerta
- Canales de notificación
- Escalamiento

## 6. Mantenimiento
### 6.1 Tareas Regulares
- Limpieza de logs
- Backup de BD
- Actualización de dependencias

### 6.2 Troubleshooting
- Problemas comunes
- Logs a revisar
- Comandos útiles

## 7. Seguridad
### 7.1 Configuración
- Variables de entorno
- Secrets management
- Network security

### 7.2 Auditoría
- Logs de auditoría
- Trazabilidad
- Compliance

## 8. Performance
### 8.1 Optimización
- Connection pooling
- Query optimization
- Caching strategy

### 8.2 Tuning
- JVM parameters
- PostgreSQL tuning
- Docker resources

## 9. Backup y Recovery
### 9.1 Estrategia de Backup
- Frecuencia
- Retención
- Verificación

### 9.2 Disaster Recovery
- RTO/RPO objectives
- Procedimientos
- Testing

## 10. Actualizaciones
### 10.1 Proceso de Deploy
- CI/CD pipeline
- Blue/Green deployment
- Rollback procedures

### 10.2 Migraciones
- Schema changes
- Data migrations
- Compatibility
```

**Archivos a analizar:**
- `docker-compose.yml`
- `application.yml` y profiles
- Scripts de `scripts/`
- Dockerfile
- Configuración de logging

---

## PASO 4: Manual de Deploy

**Objetivo:** Guía paso a paso para desplegar el sistema en diferentes ambientes.

### Estructura del Documento:

```markdown
# Manual de Despliegue - CoopFila

## 1. Preparación del Despliegue
### 1.1 Prerrequisitos
- Herramientas necesarias
- Accesos requeridos
- Validaciones previas

### 1.2 Checklist Pre-Deploy
- Tests pasando
- Variables configuradas
- Backups realizados

## 2. Despliegue Local (Desarrollo)
### 2.1 Con Docker Compose
```bash
# Comandos paso a paso
git clone [repo]
cd coopfila
cp .env.example .env
# Editar .env con valores reales
docker-compose up --build
```

### 2.2 Validación Local
- Health checks
- Tests de smoke
- Verificación de logs

## 3. Despliegue en AWS
### 3.1 Preparación AWS
- Configurar AWS CLI
- Permisos IAM necesarios
- Configurar CDK

### 3.2 Deploy con CDK
```bash
cd ticketero-infra
mvn compile
cdk bootstrap
cdk deploy TicketeroStack-dev
```

### 3.3 Configuración Post-Deploy
- Secrets Manager
- Variables de entorno
- DNS configuration

## 4. Verificación del Despliegue
### 4.1 Health Checks
- Application health
- Database connectivity
- External integrations

### 4.2 Tests de Humo
- Crear ticket
- Enviar notificación
- Dashboard admin

## 5. Rollback Procedures
### 5.1 Identificar Problemas
- Métricas a monitorear
- Logs críticos
- Alertas automáticas

### 5.2 Proceso de Rollback
- Rollback de aplicación
- Rollback de base de datos
- Comunicación a stakeholders

## 6. Monitoreo Post-Deploy
### 6.1 Métricas Clave
- Response time
- Error rate
- Throughput
- Resource utilization

### 6.2 Alertas
- Configuración de alertas
- Canales de notificación
- Escalamiento

## 7. Troubleshooting
### 7.1 Problemas Comunes
- Fallos de conectividad
- Errores de configuración
- Performance issues

### 7.2 Herramientas de Diagnóstico
- Logs centralizados
- Métricas en tiempo real
- Tracing distribuido
```

**Archivos a analizar:**
- `ticketero-infra/` (CDK code)
- `docker-compose.yml`
- Scripts de deployment
- Configuración de CI/CD

---

## PASO 5: Informe de Pruebas

**Objetivo:** Documentar completamente la estrategia de testing y resultados.

### Estructura del Documento:

```markdown
# Informe Completo de Pruebas - CoopFila

## 1. Resumen Ejecutivo
- Cobertura total de tests
- Resultados generales
- Recomendaciones

## 2. Estrategia de Testing
### 2.1 Pirámide de Tests
- Unit tests (70%)
- Integration tests (20%)
- E2E tests (10%)

### 2.2 Tipos de Pruebas
- Funcionales
- No funcionales
- Seguridad
- Performance

## 3. Tests Unitarios
### 3.1 Cobertura por Componente
- Services: X% coverage
- Controllers: Y% coverage
- Repositories: Z% coverage

### 3.2 Herramientas Utilizadas
- JUnit 5
- Mockito
- TestContainers
- AssertJ

### 3.3 Resultados
- Total tests: XXX
- Passed: XXX
- Failed: 0
- Execution time: XX seconds

## 4. Tests de Integración
### 4.1 Componentes Probados
- Database integration
- Telegram API integration
- Scheduler integration

### 4.2 Escenarios Cubiertos
- Happy path scenarios
- Error handling
- Edge cases

## 5. Tests End-to-End
### 5.1 Flujos Probados
- Creación completa de ticket
- Notificaciones Telegram
- Dashboard administrativo

### 5.2 Herramientas
- Spring Boot Test
- TestContainers
- WireMock

## 6. Tests No Funcionales
### 6.1 Performance Testing
- Load testing results
- Stress testing
- Soak testing

### 6.2 Resiliencia
- Failure scenarios
- Recovery testing
- Chaos engineering

## 7. Cobertura de Código
### 7.1 Métricas Generales
- Line coverage: XX%
- Branch coverage: XX%
- Method coverage: XX%

### 7.2 Análisis por Package
- Controllers: XX%
- Services: XX%
- Repositories: XX%

## 8. Análisis de Calidad
### 8.1 SonarQube Results
- Code smells
- Bugs
- Vulnerabilities
- Technical debt

### 8.2 Métricas de Complejidad
- Cyclomatic complexity
- Cognitive complexity
- Maintainability index

## 9. Tests de Seguridad
### 9.1 Vulnerabilidades
- OWASP Top 10
- Dependency scanning
- Static analysis

### 9.2 Penetration Testing
- API security
- Input validation
- Authentication/Authorization

## 10. Recomendaciones
### 10.1 Mejoras Identificadas
- Áreas con baja cobertura
- Tests faltantes
- Optimizaciones

### 10.2 Plan de Acción
- Prioridades
- Timeline
- Responsables
```

**Archivos a analizar:**
- `target/surefire-reports/`
- `target/site/jacoco/`
- `scripts/` (NFR tests)
- Resultados de SonarQube
- `docs/NFR-TEST-RESULTS.md`

---

## PASO 6: Integraciones y Endpoints

**Objetivo:** Documentar completamente las APIs y integraciones del sistema.

### Estructura del Documento:

```markdown
# Documentación de APIs e Integraciones - CoopFila

## 1. Introducción
- Arquitectura de APIs
- Estándares utilizados
- Versionado

## 2. API REST - Endpoints Públicos
### 2.1 Gestión de Tickets

#### POST /api/tickets
**Descripción:** Crear un nuevo ticket
**Request:**
```json
{
  "nationalId": "12345678-9",
  "telefono": "+56912345678",
  "branchOffice": "Sucursal Centro",
  "queueType": "PERSONAL_BANKER"
}
```
**Response (201):**
```json
{
  "id": 123,
  "codigoReferencia": "uuid-here",
  "numero": "P001",
  "positionInQueue": 5,
  "estimatedWaitMinutes": 75,
  "status": "EN_ESPERA"
}
```

#### GET /api/tickets/{uuid}
**Descripción:** Obtener información de un ticket
**Response (200):**
```json
{
  "id": 123,
  "numero": "P001",
  "status": "EN_ESPERA",
  "positionInQueue": 3,
  "estimatedWaitMinutes": 45,
  "assignedAdvisor": null
}
```

## 3. API REST - Endpoints Administrativos
### 3.1 Dashboard

#### GET /api/admin/dashboard
**Descripción:** Dashboard completo del sistema
**Response (200):**
```json
{
  "totalTicketsHoy": 45,
  "ticketsEnEspera": 12,
  "ticketsAtendiendo": 3,
  "asesoresDisponibles": 4,
  "tiempoPromedioAtencion": 18,
  "colas": [...]
}
```

## 4. Integración Telegram Bot API
### 4.1 Configuración
- Bot token management
- Rate limiting
- Error handling

### 4.2 Mensajes Enviados
#### Mensaje 1: Confirmación
```
✅ Ticket P001 creado
📍 Posición: #5
⏰ Tiempo estimado: 75 min
🏢 Sucursal Centro
```

#### Mensaje 2: Pre-aviso
```
⏰ ¡Pronto será tu turno!
🎫 Ticket: P001
👥 Quedan 3 personas adelante
📍 Acércate a la sucursal
```

#### Mensaje 3: Turno activo
```
🔔 ¡ES TU TURNO!
🎫 Ticket: P001
👤 Asesor: María González
🏢 Módulo: 3
```

## 5. Base de Datos - Modelo de Datos
### 5.1 Entidades Principales
- ticket
- mensaje
- advisor
- audit_log

### 5.2 Relaciones
- ticket → mensaje (1:N)
- advisor → ticket (1:N)

## 6. Eventos y Auditoría
### 6.1 Eventos Registrados
- TICKET_CREADO
- TICKET_ASIGNADO
- MENSAJE_ENVIADO
- ADVISOR_STATUS_CHANGED

### 6.2 Formato de Auditoría
```json
{
  "timestamp": "2023-12-23T10:30:00Z",
  "evento": "TICKET_CREADO",
  "actor": "SYSTEM",
  "entityType": "TICKET",
  "entityId": "123",
  "cambiosEstado": {...}
}
```

## 7. Códigos de Error
### 7.1 Errores de Validación (400)
```json
{
  "message": "Validation failed",
  "status": 400,
  "timestamp": "2023-12-23T10:30:00Z",
  "errors": [
    "nationalId: El RUT/ID es obligatorio"
  ]
}
```

### 7.2 Errores de Negocio (409)
```json
{
  "message": "Ya tienes un ticket activo: P001",
  "status": 409,
  "timestamp": "2023-12-23T10:30:00Z"
}
```

## 8. Rate Limiting y Throttling
### 8.1 Límites por Endpoint
- POST /api/tickets: 10 req/min por IP
- GET endpoints: 100 req/min por IP

### 8.2 Headers de Rate Limit
```
X-RateLimit-Limit: 10
X-RateLimit-Remaining: 7
X-RateLimit-Reset: 1640261400
```

## 9. Autenticación y Autorización
### 9.1 Endpoints Públicos
- /api/tickets (crear y consultar)
- /actuator/health

### 9.2 Endpoints Administrativos
- /api/admin/* (requiere autenticación)

## 10. Monitoreo y Observabilidad
### 10.1 Health Checks
- /actuator/health
- /actuator/health/db
- /actuator/health/telegram

### 10.2 Métricas
- /actuator/metrics
- /actuator/prometheus
```

**Archivos a analizar:**
- Controllers en `src/main/java/`
- DTOs de request/response
- Exception handlers
- Configuración de Telegram
- Tests de integración

---

## PASO 7: Modelo de Datos

**Objetivo:** Copiar y validar el modelo de datos existente.

**Tarea:** Copiar el archivo `docs/MODELO-DATOS.md` existente a `docs-final/07-MODELO-DATOS.md` y validar que esté actualizado con el código actual.

**Validaciones a realizar:**
- Verificar que las entidades JPA coincidan con el modelo
- Confirmar que las migraciones Flyway estén alineadas
- Validar que los índices estén implementados
- Verificar que las constraints estén en el código

---

## PASO 8: Resumen Ejecutivo + README

**Objetivo:** Crear documentos de alto nivel para stakeholders y un índice general.

### 8.1 Resumen Ejecutivo

```markdown
# Resumen Ejecutivo - Proyecto CoopFila

## 1. Visión General del Proyecto
- Objetivos alcanzados
- Beneficios entregados
- ROI estimado

## 2. Arquitectura Implementada
- Stack tecnológico
- Decisiones arquitectónicas
- Escalabilidad

## 3. Funcionalidades Entregadas
- Creación de tickets digitales
- Notificaciones automáticas
- Dashboard administrativo
- Asignación automática

## 4. Métricas de Calidad
- Cobertura de tests: XX%
- Performance: XX req/sec
- Disponibilidad: 99.X%
- Tiempo de respuesta: XXXms

## 5. Cumplimiento de Requerimientos
- Funcionales: 100%
- No funcionales: 100%
- Casos de uso: 100%

## 6. Próximos Pasos
- Fase 2: Expansión
- Mejoras identificadas
- Roadmap técnico
```

### 8.2 README Principal

```markdown
# CoopFila - Documentación Completa

## 📋 Índice de Documentación

### 📚 Documentación Técnica
- [01-DOCUMENTACION-CODIGO.md](01-DOCUMENTACION-CODIGO.md) - Arquitectura y código fuente
- [03-MANUAL-TECNICO.md](03-MANUAL-TECNICO.md) - Setup y configuración técnica
- [04-MANUAL-DEPLOY.md](04-MANUAL-DEPLOY.md) - Guías de despliegue
- [07-MODELO-DATOS.md](07-MODELO-DATOS.md) - Modelo de base de datos

### 👥 Documentación de Usuario
- [02-MANUAL-USUARIO.md](02-MANUAL-USUARIO.md) - Guía para usuarios finales

### 🔧 Documentación de Integración
- [06-INTEGRACIONES-ENDPOINTS.md](06-INTEGRACIONES-ENDPOINTS.md) - APIs y contratos

### 📊 Reportes y Análisis
- [05-INFORME-PRUEBAS.md](05-INFORME-PRUEBAS.md) - Cobertura y resultados de testing
- [08-RESUMEN-EJECUTIVO.md](08-RESUMEN-EJECUTIVO.md) - Overview para stakeholders

## 🚀 Quick Start
1. Leer [Manual Técnico](03-MANUAL-TECNICO.md) para setup
2. Seguir [Manual de Deploy](04-MANUAL-DEPLOY.md) para despliegue
3. Consultar [Manual de Usuario](02-MANUAL-USUARIO.md) para uso

## 📞 Contacto
- Equipo de desarrollo: [email]
- Soporte técnico: [email]
- Documentación: [link]
```

---

## 🎯 Criterios de Calidad

### Para Cada Documento:
- ✅ **Precisión:** Información verificada contra código fuente
- ✅ **Completitud:** Cubre todos los aspectos relevantes
- ✅ **Claridad:** Lenguaje apropiado para la audiencia
- ✅ **Formato:** Markdown profesional y consistente
- ✅ **Ejemplos:** Código, comandos y casos de uso reales
- ✅ **Navegación:** Enlaces internos y estructura clara

### Estándares de Formato:
- Headers jerárquicos (H1, H2, H3)
- Code blocks con syntax highlighting
- Tablas para información estructurada
- Listas para pasos y elementos
- Callouts para información importante
- Enlaces a archivos fuente cuando sea relevante

---

## 📋 Checklist Final

### Antes de Entregar:
- [ ] ✅ 8 documentos creados en `docs-final/`
- [ ] ✅ README.md con índice completo
- [ ] ✅ Toda la información verificada contra código
- [ ] ✅ Formato consistente en todos los documentos
- [ ] ✅ Enlaces internos funcionando
- [ ] ✅ Ejemplos de código actualizados
- [ ] ✅ Screenshots/diagramas incluidos donde sea necesario
- [ ] ✅ Información de contacto y soporte

---

**PROMPT DE DOCUMENTACIÓN COMPLETADO**

**Documentos a Generar:** 8 + README  
**Audiencias:** Desarrolladores, Usuarios, DevOps, Stakeholders  
**Formato:** Markdown profesional  
**Fuentes:** Código fuente + documentación existente  

**Estado:** ✅ Listo para ejecución  
**Tiempo Estimado:** 6-8 horas de trabajo técnico  

Este prompt te guiará para crear documentación completa, profesional y actualizada del proyecto CoopFila, cubriendo todas las necesidades de las diferentes audiencias involucradas.