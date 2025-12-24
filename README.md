# CoopFila - Sistema de Gestión de Tickets

**Sistema de Gestión de Tickets con Notificaciones en Tiempo Real**

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.java.net/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue.svg)](https://www.postgresql.org/)
[![Docker](https://img.shields.io/badge/Docker-Compose-blue.svg)](https://docs.docker.com/compose/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

---

## 🎯 Descripción

CoopFila es una solución innovadora que **elimina las filas físicas** en sucursales bancarias, permitiendo a los clientes obtener tickets digitales y recibir notificaciones automáticas en tiempo real a través de Telegram. Los clientes pueden **salir de la sucursal** durante su espera y regresar justo cuando sea su turno.

### ✨ Características Principales

- 🎫 **Tickets digitales** generados en terminales de autoservicio
- 📱 **Notificaciones Telegram** automáticas en tiempo real
- 🚶‍♂️ **Libertad de movimiento** - sal de la sucursal mientras esperas
- 📊 **Dashboard en tiempo real** para supervisores
- ⚡ **Asignación inteligente** de asesores por especialización
- 📈 **Métricas y analytics** completos
- 🔄 **Sistema de reintentos** robusto para notificaciones

---

## 🚀 Quick Start

### Prerrequisitos

- **Java 21+**
- **Docker & Docker Compose**
- **PostgreSQL 15** (incluido en docker-compose)
- **Token de Telegram Bot** (opcional para notificaciones)

### Instalación Rápida

```bash
# 1. Clonar el repositorio
git clone https://github.com/tu-org/coopfila.git
cd coopfila

# 2. Configurar variables de entorno
cp .env.example .env
# Editar .env con tus configuraciones

# 3. Levantar servicios con Docker
docker-compose up -d

# 4. Verificar que todo esté funcionando
curl http://localhost:8080/actuator/health
```

### Acceso Rápido

- **API REST:** http://localhost:8080
- **Dashboard:** http://localhost:8080/dashboard
- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **PostgreSQL:** localhost:5432 (usuario: `ticketero_user`)

---

## 📚 Documentación Completa

### 📖 Para Desarrolladores

| Documento | Descripción | Audiencia |
|-----------|-------------|-----------|
| **[📋 Documentación de Código](docs-final/01-DOCUMENTACION-CODIGO.md)** | Arquitectura técnica, APIs, componentes | Desarrolladores |
| **[🔧 Manual Técnico](docs-final/03-MANUAL-TECNICO.md)** | Instalación, configuración, troubleshooting | DevOps, SysAdmins |
| **[🚀 Manual de Deploy](docs-final/04-MANUAL-DEPLOY.md)** | Despliegue local y AWS con CDK | DevOps |
| **[🌐 Documentación API](docs-final/06-DOCUMENTACION-API.md)** | Endpoints REST completos con ejemplos | Integradores |
| **[🗄️ Modelo de Datos](docs-final/07-MODELO-DATOS.md)** | Esquemas DB, relaciones, índices | DBAs, Arquitectos |

### 👥 Para Usuarios y Negocio

| Documento | Descripción | Audiencia |
|-----------|-------------|-----------|
| **[👤 Manual de Usuario](docs-final/02-MANUAL-USUARIO.md)** | Guía completa para clientes y supervisores | Usuarios finales |
| **[📊 Resumen Ejecutivo](docs-final/08-RESUMEN-EJECUTIVO.md)** | ROI, beneficios de negocio, decisiones | Directivos, Stakeholders |

### 🧪 Calidad y Testing

| Documento | Descripción | Audiencia |
|-----------|-------------|-----------|
| **[✅ Informe de Pruebas](docs-final/05-INFORME-PRUEBAS.md)** | Testing completo: unitario, integración, performance | QA, Project Managers |

---

## 🏗️ Arquitectura del Sistema

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   TERMINAL      │    │   API REST      │    │   POSTGRESQL    │
│   AUTOSERVICIO  │───▶│   SPRING BOOT   │───▶│   DATABASE      │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                              │
                              ▼
                       ┌─────────────────┐
                       │  TELEGRAM API   │
                       │  NOTIFICACIONES │
                       └─────────────────┘
```

### Stack Tecnológico

- **Backend:** Spring Boot 3.2 + Java 21
- **Base de Datos:** PostgreSQL 15
- **Notificaciones:** Telegram Bot API
- **Containerización:** Docker + Docker Compose
- **Cloud:** AWS (ECS, RDS, ALB) con CDK
- **Monitoreo:** Spring Actuator + Micrometer

---

## 🎮 Uso Básico

### 1. Crear un Ticket

```bash
curl -X POST "http://localhost:8080/api/tickets" \
  -H "Content-Type: application/json" \
  -d '{
    "nationalId": "12345678-9",
    "telefono": "+56912345678",
    "branchOffice": "Sucursal Centro",
    "queueType": "CAJA"
  }'
```

**Respuesta:**
```json
{
  "id": 123,
  "numero": "C05",
  "positionInQueue": 8,
  "estimatedWaitTimeMinutes": 45,
  "status": "WAITING"
}
```

### 2. Consultar Estado

```bash
curl "http://localhost:8080/api/tickets/123"
```

### 3. Dashboard de Métricas

```bash
curl "http://localhost:8080/api/metrics/dashboard"
```

---

## 🔧 Configuración

### Variables de Entorno Principales

```bash
# Base de datos
DB_HOST=localhost
DB_PORT=5432
DB_NAME=ticketero
DB_USER=ticketero_user
DB_PASSWORD=ticketero_pass

# Telegram Bot
TELEGRAM_BOT_TOKEN=your_bot_token_here
TELEGRAM_BOT_USERNAME=CoopFilaBot

# Aplicación
SERVER_PORT=8080
SPRING_PROFILES_ACTIVE=local

# Logging
LOGGING_LEVEL_ROOT=INFO
LOGGING_LEVEL_CL_COOPFILA=DEBUG
```

### Configuración de Telegram Bot

1. **Crear bot:** Habla con [@BotFather](https://t.me/botfather) en Telegram
2. **Obtener token:** Guarda el token proporcionado
3. **Configurar webhook:** (Opcional) Para producción
4. **Actualizar .env:** Agregar `TELEGRAM_BOT_TOKEN`

---

## 🧪 Testing

### Ejecutar Tests

```bash
# Tests unitarios
./mvnw test

# Tests de integración
./mvnw test -Dtest="*IntegrationTest"

# Tests de performance (requiere K6)
k6 run --vus 10 --duration 2m k6/load-test.js

# Validar consistencia del sistema
./scripts/utils/validate-consistency.sh
```

### Cobertura de Código

```bash
./mvnw jacoco:report
open target/site/jacoco/index.html
```

**Cobertura actual:** 85.2% (líneas), 78.9% (branches)

---

## 📊 Métricas y Monitoreo

### Endpoints de Salud

- **Health Check:** `/actuator/health`
- **Métricas:** `/actuator/metrics`
- **Info:** `/actuator/info`
- **Prometheus:** `/actuator/prometheus`

### Dashboard en Tiempo Real

Accede a http://localhost:8080/dashboard para ver:

- 📈 Tickets por cola en tiempo real
- 👥 Estado de asesores
- ⏱️ Tiempos de espera promedio
- 📱 Estado de notificaciones Telegram
- 🎯 KPIs de performance

---

## 🚀 Despliegue

### Local (Desarrollo)

```bash
# Opción 1: Docker Compose (Recomendado)
docker-compose up -d

# Opción 2: Maven + PostgreSQL local
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

### Producción (AWS)

```bash
# Usando AWS CDK
cd ticketero-infra
npm install
cdk deploy --all

# Ver guía completa en docs-final/04-MANUAL-DEPLOY.md
```

### Staging

```bash
docker-compose -f docker-compose.staging.yml up -d
```

---

## 🤝 Contribución

### Flujo de Desarrollo

1. **Fork** el repositorio
2. **Crear branch:** `git checkout -b feature/nueva-funcionalidad`
3. **Commit cambios:** `git commit -am 'Add nueva funcionalidad'`
4. **Push branch:** `git push origin feature/nueva-funcionalidad`
5. **Crear Pull Request**

### Estándares de Código

- **Java:** Seguir [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)
- **Commits:** [Conventional Commits](https://www.conventionalcommits.org/)
- **Testing:** Cobertura mínima 80%
- **Documentación:** Actualizar docs con cambios

### Revisión de Código

Todos los PRs requieren:
- ✅ Tests pasando
- ✅ Cobertura de código mantenida
- ✅ Revisión de al menos 1 desarrollador
- ✅ Documentación actualizada

---

## 📈 Roadmap

### v1.1 (Q2 2025)
- [ ] Soporte para WhatsApp Business API
- [ ] Dashboard mejorado con más métricas
- [ ] API para reserva de citas online
- [ ] Integración con sistemas de CRM

### v1.2 (Q3 2025)
- [ ] App móvil nativa
- [ ] Predicción de demanda con ML
- [ ] Soporte multi-sucursal
- [ ] Analytics avanzados

### v2.0 (Q4 2025)
- [ ] Inteligencia artificial para optimización
- [ ] Chatbot integrado
- [ ] Plataforma omnicanal
- [ ] API pública para partners

---

## 🐛 Problemas Conocidos

### Issues Abiertos

- **#12:** Optimizar query de cálculo de posición en cola
- **#8:** Mejorar manejo de reconexión Telegram
- **#5:** Agregar más validaciones de entrada

### Reportar Bugs

1. **Verificar** que no esté ya reportado
2. **Usar template** de issue
3. **Incluir logs** y pasos para reproducir
4. **Etiquetar** apropiadamente

---

## 📄 Licencia

Este proyecto está licenciado bajo la **MIT License** - ver el archivo [LICENSE](LICENSE) para detalles.

---

## 👥 Equipo

### Core Team

- **Tech Lead:** [@tech-lead](https://github.com/tech-lead)
- **Backend Developer:** [@backend-dev](https://github.com/backend-dev)
- **Frontend Developer:** [@frontend-dev](https://github.com/frontend-dev)
- **DevOps Engineer:** [@devops-eng](https://github.com/devops-eng)

### Contribuidores

Ver la lista completa en [CONTRIBUTORS.md](CONTRIBUTORS.md)

---

## 📞 Soporte

### Canales de Soporte

- **Issues:** [GitHub Issues](https://github.com/tu-org/coopfila/issues)
- **Discussions:** [GitHub Discussions](https://github.com/tu-org/coopfila/discussions)
- **Email:** coopfila-support@tu-org.com
- **Slack:** #coopfila-support

### Documentación Adicional

- **Wiki:** [GitHub Wiki](https://github.com/tu-org/coopfila/wiki)
- **FAQ:** [Preguntas Frecuentes](docs/FAQ.md)
- **Troubleshooting:** [Guía de Resolución](docs-final/03-MANUAL-TECNICO.md#troubleshooting)

---

## 🏆 Reconocimientos

- **Spring Boot Community** por el excelente framework
- **PostgreSQL Team** por la robusta base de datos
- **Telegram** por la API de bots
- **Docker** por simplificar el despliegue

---

## 📊 Estadísticas del Proyecto

![GitHub stars](https://img.shields.io/github/stars/tu-org/coopfila?style=social)
![GitHub forks](https://img.shields.io/github/forks/tu-org/coopfila?style=social)
![GitHub issues](https://img.shields.io/github/issues/tu-org/coopfila)
![GitHub pull requests](https://img.shields.io/github/issues-pr/tu-org/coopfila)

**Líneas de código:** ~15,000  
**Tests:** 127 casos  
**Cobertura:** 85.2%  
**Última actualización:** Enero 2025

---

*CoopFila - Transformando la espera en libertad* 🚀