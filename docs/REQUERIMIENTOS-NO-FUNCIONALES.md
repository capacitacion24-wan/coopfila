# Requerimientos No Funcionales - Sistema Ticketero Digital

**Proyecto:** Sistema de Gestión de Tickets con Notificaciones en Tiempo Real  
**Cliente:** Institución Financiera  
**Versión:** 1.0  
**Fecha:** Diciembre 2025  
**Analista:** Analista de Sistemas Senior

---

## 1. Introducción

### 1.1 Propósito

Este documento especifica los requerimientos no funcionales del Sistema Ticketero Digital, definiendo las características de calidad, rendimiento, seguridad y operación que debe cumplir el sistema para garantizar una experiencia óptima y confiable.

### 1.2 Alcance

Este documento cubre:
- ✅ 7 Categorías de RNF (Disponibilidad, Performance, Escalabilidad, Confiabilidad, Seguridad, Usabilidad, Mantenibilidad)
- ✅ Métricas específicas y medibles
- ✅ Criterios de aceptación cuantificables
- ✅ Herramientas de monitoreo y validación

### 1.3 Fases del Proyecto

| Fase | Descripción | Volumen Esperado | Duración |
|------|-------------|------------------|----------|
| **Piloto** | 1 sucursal | 500-800 tickets/día | 3 meses |
| **Expansión** | 5 sucursales | 2,500-3,000 tickets/día | 6 meses |
| **Nacional** | 50+ sucursales | 25,000+ tickets/día | 12+ meses |

---

## 2. Requerimientos No Funcionales

### **RNF-001: Disponibilidad**

**Descripción:** El sistema debe estar disponible durante el horario de atención de sucursales con mínimas interrupciones.

**Métricas:**
- **Uptime:** 99.5% durante horario de atención (8:00-18:00, L-V)
- **Downtime máximo:** 4 horas/mes
- **Recovery automático:** < 5 minutos
- **Mantenimiento programado:** Fuera de horario de atención

**Criterios de Aceptación:**
```gherkin
Given el sistema está en producción
When se mide la disponibilidad durante un mes
Then el uptime debe ser >= 99.5%
And el downtime no planificado debe ser <= 4 horas/mes
And el recovery automático debe ser <= 5 minutos
```

**Herramientas de Monitoreo:**
- Spring Boot Actuator (health checks)
- Prometheus + Grafana (métricas)
- Alertas automáticas vía email/Slack

**Validación:**
- Health check endpoint: GET /actuator/health
- Monitoreo 24/7 con alertas
- SLA reportes mensuales

---

### **RNF-002: Performance**

**Descripción:** El sistema debe responder rápidamente a las operaciones críticas para garantizar una experiencia fluida.

**Métricas por Operación:**

| Operación | Tiempo Máximo | Percentil 95 | Volumen Esperado |
|-----------|---------------|--------------|------------------|
| Creación de ticket | 3 segundos | 2 segundos | 800/día |
| Envío Mensaje 1 | 5 segundos | 3 segundos | 800/día |
| Consulta posición | 1 segundo | 0.5 segundos | 2,000/día |
| Dashboard admin | 2 segundos | 1.5 segundos | 200/día |
| Asignación automática | 10 segundos | 5 segundos | 800/día |

**Criterios de Aceptación:**
```gherkin
Scenario: Performance de creación de ticket
Given el sistema está bajo carga normal
When un cliente crea un ticket
Then la respuesta debe llegar en <= 3 segundos
And el 95% de las requests deben ser <= 2 segundos

Scenario: Performance de consulta de posición
Given existen 100 tickets activos en el sistema
When un cliente consulta su posición
Then la respuesta debe llegar en <= 1 segundo
And el cálculo debe ser en tiempo real
```

**Herramientas de Medición:**
- Micrometer (métricas de Spring Boot)
- JMeter (pruebas de carga)
- New Relic/AppDynamics (APM)

**Validación:**
- Pruebas de carga automatizadas
- Monitoreo continuo de response times
- Alertas cuando percentil 95 > umbral

---

### **RNF-003: Escalabilidad**

**Descripción:** El sistema debe escalar horizontalmente para soportar el crecimiento planificado sin degradación de performance.

**Métricas por Fase:**

| Fase | Sucursales | Tickets/día | Usuarios Concurrentes | Instancias API |
|------|------------|-------------|----------------------|----------------|
| Piloto | 1 | 800 | 10 | 1 |
| Expansión | 5 | 3,000 | 50 | 2 |
| Nacional | 50+ | 25,000+ | 500+ | 5+ |

**Criterios de Aceptación:**
```gherkin
Scenario: Escalabilidad horizontal
Given el sistema maneja 800 tickets/día con 1 instancia
When se agregan 4 sucursales más (3,000 tickets/día)
And se despliegan 2 instancias API con load balancer
Then el performance debe mantenerse dentro de los SLA
And no debe haber degradación > 10%

Scenario: Auto-scaling (Futuro)
Given el sistema detecta carga > 80% CPU por 5 minutos
When se activa auto-scaling
Then debe crear nueva instancia en <= 3 minutos
And distribuir carga automáticamente
```

**Arquitectura de Escalamiento:**
- **Horizontal:** Load balancer + múltiples instancias API
- **Base de datos:** Connection pooling optimizado
- **Cache:** Redis para sesiones (Fase Nacional)
- **Mensajería:** RabbitMQ para alto volumen (Fase Nacional)

**Validación:**
- Pruebas de carga progresiva
- Simulación de picos de tráfico
- Monitoreo de recursos (CPU, memoria, DB connections)

---

### **RNF-004: Confiabilidad**

**Descripción:** El sistema debe ser confiable en el procesamiento de tickets y envío de notificaciones, sin pérdida de datos.

**Métricas:**
- **Éxito en creación de tickets:** 99.9%
- **Éxito en envío de mensajes:** 99.9%
- **Sin pérdida de datos:** 100%
- **Reintentos automáticos:** 3 intentos con backoff exponencial
- **Recuperación de fallos:** Automática en < 5 minutos

**Criterios de Aceptación:**
```gherkin
Scenario: Confiabilidad en envío de mensajes
Given se crean 100 tickets con teléfonos válidos
When el sistema procesa los mensajes
Then >= 99.9% de mensajes deben enviarse exitosamente
And los fallos deben reintentarse automáticamente
And ningún mensaje debe perderse

Scenario: Recuperación ante fallo de BD
Given la conexión a PostgreSQL se interrumpe
When el sistema detecta el fallo
Then debe reintentar conexión cada 30 segundos
And recuperar automáticamente cuando BD esté disponible
And no debe perder transacciones en progreso
```

**Mecanismos de Confiabilidad:**
- **Transacciones ACID:** PostgreSQL para integridad
- **Reintentos:** 3 intentos con backoff (30s, 60s, 120s)
- **Circuit Breaker:** Para servicios externos (Fase 2)
- **Auditoría:** Registro completo de eventos críticos
- **Backup:** Respaldo automático diario de BD

**Validación:**
- Pruebas de fallos simulados
- Monitoreo de tasas de éxito
- Verificación de integridad de datos

---

### **RNF-005: Seguridad**

**Descripción:** El sistema debe proteger los datos personales y cumplir con regulaciones de privacidad del sector financiero.

**Métricas:**
- **Cumplimiento:** 100% Ley de Protección de Datos Personales
- **Encriptación:** AES-256 para datos sensibles en BD
- **Acceso controlado:** Autenticación para panel administrativo
- **Auditoría:** 100% de accesos y cambios registrados
- **Vulnerabilidades:** 0 críticas, < 5 medias

**Criterios de Aceptación:**
```gherkin
Scenario: Protección de datos personales
Given un cliente proporciona RUT y teléfono
When los datos se almacenan en BD
Then el RUT debe estar encriptado con AES-256
And el teléfono debe estar encriptado
And no debe aparecer en logs del sistema

Scenario: Acceso controlado al panel admin
Given un usuario intenta acceder al dashboard
When no tiene credenciales válidas
Then debe ser rechazado con HTTP 401
And el intento debe registrarse en auditoría
```

**Medidas de Seguridad:**
- **Encriptación en reposo:** AES-256 para campos sensibles
- **Encriptación en tránsito:** HTTPS/TLS 1.3
- **Validación de inputs:** Bean Validation + sanitización
- **Rate limiting:** 10 requests/minuto por IP (futuro)
- **Logs seguros:** Sin datos sensibles en logs
- **Secrets management:** Variables de entorno, no hardcoded

**Validación:**
- Auditoría de seguridad trimestral
- Penetration testing semestral
- Escaneo automático de vulnerabilidades

---

### **RNF-006: Usabilidad**

**Descripción:** El sistema debe ser intuitivo y fácil de usar tanto para clientes como para supervisores.

**Métricas:**
- **Tiempo creación ticket:** < 2 minutos (cliente)
- **Comprensión mensajes:** 95% clientes entienden sin ayuda
- **Dashboard intuitivo:** Supervisor encuentra info en < 30 segundos
- **Tasa de errores usuario:** < 5%
- **Satisfacción:** NPS > 60 puntos

**Criterios de Aceptación:**
```gherkin
Scenario: Facilidad de creación de ticket
Given un cliente nuevo usa el terminal por primera vez
When sigue las instrucciones en pantalla
Then debe completar la creación en <= 2 minutos
And no debe requerir ayuda del personal

Scenario: Claridad de mensajes Telegram
Given un cliente recibe mensaje "✅ Ticket P01, posición #5, 75min"
When lee el mensaje
Then debe entender su posición y tiempo estimado
And no debe llamar a sucursal para aclaración
```

**Características de Usabilidad:**
- **Mensajes claros:** Español simple, emojis descriptivos
- **Interfaz intuitiva:** Terminal touch screen amigable
- **Dashboard comprensible:** Métricas visuales claras
- **Feedback inmediato:** Confirmaciones y estados visibles
- **Accesibilidad:** Textos legibles, contraste adecuado

**Validación:**
- Pruebas de usabilidad con usuarios reales
- Encuestas de satisfacción mensuales
- Análisis de patrones de uso

---

### **RNF-007: Mantenibilidad**

**Descripción:** El sistema debe ser fácil de mantener, actualizar y diagnosticar problemas.

**Métricas:**
- **Tiempo resolución bugs críticos:** < 4 horas
- **Tiempo resolución bugs menores:** < 24 horas
- **Cobertura de código:** > 80%
- **Documentación:** 100% APIs documentadas
- **Deployment:** Automatizado, < 10 minutos

**Criterios de Aceptación:**
```gherkin
Scenario: Diagnóstico rápido de problemas
Given ocurre un error en producción
When el equipo de soporte revisa los logs
Then debe identificar la causa en <= 15 minutos
And tener información suficiente para resolverlo

Scenario: Deployment sin interrupciones
Given hay una nueva versión lista
When se ejecuta el deployment
Then debe completarse en <= 10 minutos
And no debe interrumpir el servicio
```

**Características de Mantenibilidad:**
- **Código limpio:** Principios SOLID, patrones claros
- **Logs detallados:** Structured logging con niveles apropiados
- **Monitoreo:** Métricas de negocio y técnicas
- **Documentación:** APIs, arquitectura, runbooks
- **Testing:** Unit tests, integration tests, e2e tests
- **CI/CD:** Pipeline automatizado con validaciones

**Validación:**
- Code reviews obligatorios
- Análisis estático de código (SonarQube)
- Métricas de mantenibilidad (cyclomatic complexity)

---

## 3. Matriz de Prioridades

| RNF | Categoría | Prioridad | Fase Implementación | Impacto Negocio |
|-----|-----------|-----------|-------------------|-----------------|
| RNF-002 | Performance | Alta | Piloto | Alto |
| RNF-004 | Confiabilidad | Alta | Piloto | Alto |
| RNF-005 | Seguridad | Alta | Piloto | Crítico |
| RNF-001 | Disponibilidad | Media | Piloto | Alto |
| RNF-006 | Usabilidad | Media | Piloto | Medio |
| RNF-003 | Escalabilidad | Media | Expansión | Alto |
| RNF-007 | Mantenibilidad | Baja | Piloto | Medio |

---

## 4. Herramientas de Monitoreo y Validación

### 4.1 Stack de Observabilidad

| Componente | Herramienta | Propósito |
|------------|-------------|-----------|
| **Métricas** | Micrometer + Prometheus | Performance, throughput, errores |
| **Logs** | Logback + ELK Stack | Diagnóstico, auditoría |
| **Trazas** | Spring Cloud Sleuth | Debugging distribuido |
| **Dashboards** | Grafana | Visualización métricas |
| **Alertas** | AlertManager | Notificaciones proactivas |
| **APM** | New Relic/AppDynamics | Performance profiling |

### 4.2 Métricas Clave (KPIs)

```yaml
# Métricas de Negocio
tickets_creados_total: counter
tickets_completados_total: counter
mensajes_enviados_total: counter
tiempo_espera_promedio: histogram

# Métricas Técnicas
http_requests_duration: histogram
database_connections_active: gauge
jvm_memory_used: gauge
system_cpu_usage: gauge
```

### 4.3 Alertas Críticas

| Alerta | Condición | Acción |
|--------|-----------|--------|
| **API Down** | Health check falla > 2 min | Email + SMS inmediato |
| **Performance Degradado** | Response time > 5s por 5 min | Email equipo |
| **BD Conexiones** | Connections > 80% por 3 min | Email DBA |
| **Mensajes Fallando** | Tasa fallo > 5% por 10 min | Email + Slack |
| **Disco Lleno** | Disk usage > 90% | Email + SMS |

---

## 5. Plan de Pruebas No Funcionales

### 5.1 Pruebas de Performance

**Herramientas:** JMeter, Gatling, K6

**Escenarios:**
```
Carga Normal:
- 10 usuarios concurrentes
- 1 ticket/minuto por usuario
- Duración: 1 hora

Carga Pico:
- 50 usuarios concurrentes  
- 5 tickets/minuto por usuario
- Duración: 30 minutos

Stress Test:
- 100 usuarios concurrentes
- Hasta que sistema falle
- Identificar punto de quiebre
```

### 5.2 Pruebas de Disponibilidad

**Escenarios:**
- Reinicio de aplicación
- Fallo de base de datos
- Fallo de red a Telegram
- Sobrecarga de CPU/memoria

### 5.3 Pruebas de Seguridad

**Herramientas:** OWASP ZAP, Burp Suite

**Validaciones:**
- SQL Injection
- XSS (Cross-site scripting)
- CSRF (Cross-site request forgery)
- Autenticación y autorización
- Encriptación de datos

---

## 6. Criterios de Aceptación Global

### 6.1 Fase Piloto (Go-Live)

```gherkin
Given el sistema está desplegado en producción
When se ejecutan las pruebas de aceptación
Then debe cumplir:
  | RNF | Métrica | Valor Mínimo |
  | Performance | Creación ticket | <= 3 segundos |
  | Disponibilidad | Uptime | >= 99.5% |
  | Confiabilidad | Éxito mensajes | >= 99.9% |
  | Seguridad | Vulnerabilidades críticas | 0 |
  | Usabilidad | Tiempo creación | <= 2 minutos |
```

### 6.2 Criterios de Rechazo

El sistema será rechazado si:
- Performance > 150% de los límites especificados
- Disponibilidad < 99% durante pruebas
- Pérdida de datos en cualquier escenario
- Vulnerabilidades críticas de seguridad
- Incumplimiento de regulaciones de privacidad

---

## 7. Roadmap de Mejoras

### 7.1 Fase Expansión (Meses 4-9)
- Implementar Circuit Breakers
- Cache Redis para consultas frecuentes
- Auto-scaling básico
- Métricas avanzadas de negocio

### 7.2 Fase Nacional (Meses 10+)
- Arquitectura de microservicios
- RabbitMQ para mensajería
- Kubernetes para orquestación
- Machine Learning para predicción de tiempos

---

## 8. Responsabilidades

| Rol | Responsabilidad |
|-----|-----------------|
| **Arquitecto** | Definir RNF, validar cumplimiento |
| **Desarrolladores** | Implementar según RNF |
| **QA** | Ejecutar pruebas no funcionales |
| **DevOps** | Configurar monitoreo y alertas |
| **Product Owner** | Aprobar trade-offs de RNF |

---

**Documento completado exitosamente**  
**Estado:** ✅ Listo para implementación y validación  
**Próxima revisión:** Al finalizar Fase Piloto