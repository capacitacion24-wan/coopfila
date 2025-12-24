# Documentación API REST - CoopFila

**Sistema de Gestión de Tickets con Notificaciones en Tiempo Real**  
**Versión:** 1.0  
**Fecha:** Diciembre 2025  
**Audiencia:** Desarrolladores, Integradores, DevOps

---

## 1. Información General

### 1.1 Descripción de la API

La API REST de CoopFila proporciona endpoints para gestionar el sistema de tickets digitales, permitiendo crear tickets, consultar estados, gestionar asesores y obtener métricas en tiempo real.

### 1.2 Información Base

| Atributo | Valor |
|----------|-------|
| **Base URL** | `https://api.coopfila.cl/api` |
| **Versión** | v1 |
| **Protocolo** | HTTPS |
| **Formato** | JSON |
| **Autenticación** | JWT Bearer Token |
| **Rate Limiting** | 100 req/min por IP |

### 1.3 Códigos de Estado HTTP

| Código | Significado | Uso |
|--------|-------------|-----|
| **200** | OK | Operación exitosa |
| **201** | Created | Recurso creado exitosamente |
| **204** | No Content | Operación exitosa sin contenido |
| **400** | Bad Request | Datos de entrada inválidos |
| **401** | Unauthorized | Token de autenticación requerido |
| **403** | Forbidden | Sin permisos para la operación |
| **404** | Not Found | Recurso no encontrado |
| **409** | Conflict | Conflicto con estado actual |
| **422** | Unprocessable Entity | Validación de negocio fallida |
| **429** | Too Many Requests | Rate limit excedido |
| **500** | Internal Server Error | Error interno del servidor |

---

## 2. Autenticación

### 2.1 JWT Bearer Token

Todos los endpoints (excepto públicos) requieren autenticación mediante JWT Bearer Token.

```http
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### 2.2 Obtener Token

**Endpoint:** `POST /auth/login`

```json
{
  "username": "admin",
  "password": "password123"
}
```

**Respuesta:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresIn": 3600,
  "tokenType": "Bearer"
}
```

### 2.3 Roles y Permisos

| Rol | Permisos |
|-----|----------|
| **ADMIN** | Acceso completo a todos los endpoints |
| **SUPERVISOR** | Gestión de asesores y métricas |
| **ADVISOR** | Gestión de sus propios tickets |
| **PUBLIC** | Solo creación y consulta de tickets |

---

## 3. Endpoints de Tickets

### 3.1 Crear Ticket

Crea un nuevo ticket en el sistema.

**Endpoint:** `POST /tickets`  
**Autenticación:** No requerida  
**Rate Limit:** 5 req/min por IP

#### Request

```http
POST /api/tickets
Content-Type: application/json

{
  "nationalId": "12345678-9",
  "telefono": "+56912345678",
  "branchOffice": "Sucursal Centro",
  "queueType": "CAJA"
}
```

#### Request Body

| Campo | Tipo | Requerido | Descripción |
|-------|------|-----------|-------------|
| `nationalId` | string | ✅ | RUT o cédula (formato: 12345678-9) |
| `telefono` | string | ❌ | Teléfono para notificaciones (+56912345678) |
| `branchOffice` | string | ✅ | Nombre de la sucursal |
| `queueType` | enum | ✅ | Tipo de cola: CAJA, PERSONAL, EMPRESAS, GERENCIA |

#### Response 201 - Ticket Creado

```json
{
  "id": 123,
  "numero": "C05",
  "nationalId": "12345678-9",
  "telefono": "+56912345678",
  "branchOffice": "Sucursal Centro",
  "queueType": "CAJA",
  "status": "WAITING",
  "positionInQueue": 8,
  "estimatedWaitTimeMinutes": 45,
  "createdAt": "2025-01-15T10:30:00Z",
  "assignedAdvisorId": null,
  "calledAt": null,
  "completedAt": null
}
```

#### Response 409 - Ticket Duplicado

```json
{
  "error": "DUPLICATE_TICKET",
  "message": "Ya tienes un ticket activo en esta cola",
  "details": {
    "existingTicketId": 120,
    "existingTicketNumber": "C03",
    "status": "WAITING"
  },
  "timestamp": "2025-01-15T10:30:00Z"
}
```

#### Response 422 - Validación Fallida

```json
{
  "error": "VALIDATION_ERROR",
  "message": "Datos de entrada inválidos",
  "details": [
    {
      "field": "nationalId",
      "message": "Formato de RUT inválido"
    },
    {
      "field": "queueType",
      "message": "Tipo de cola no válido"
    }
  ],
  "timestamp": "2025-01-15T10:30:00Z"
}
```

### 3.2 Consultar Ticket

Obtiene información detallada de un ticket específico.

**Endpoint:** `GET /tickets/{id}`  
**Autenticación:** No requerida para tickets propios

#### Request

```http
GET /api/tickets/123
```

#### Response 200 - Ticket Encontrado

```json
{
  "id": 123,
  "numero": "C05",
  "nationalId": "12345678-9",
  "telefono": "+56912345678",
  "branchOffice": "Sucursal Centro",
  "queueType": "CAJA",
  "status": "CALLED",
  "positionInQueue": 0,
  "estimatedWaitTimeMinutes": 0,
  "createdAt": "2025-01-15T10:30:00Z",
  "calledAt": "2025-01-15T11:15:00Z",
  "assignedAdvisorId": 5,
  "assignedAdvisorName": "María González",
  "moduleNumber": 3,
  "completedAt": null,
  "events": [
    {
      "type": "CREATED",
      "timestamp": "2025-01-15T10:30:00Z",
      "description": "Ticket creado"
    },
    {
      "type": "CALLED",
      "timestamp": "2025-01-15T11:15:00Z",
      "description": "Cliente llamado al módulo 3"
    }
  ]
}
```

#### Response 404 - Ticket No Encontrado

```json
{
  "error": "TICKET_NOT_FOUND",
  "message": "El ticket solicitado no existe",
  "timestamp": "2025-01-15T10:30:00Z"
}
```

### 3.3 Consultar por RUT

Obtiene tickets activos de un cliente por su RUT.

**Endpoint:** `GET /tickets/by-rut/{nationalId}`

#### Request

```http
GET /api/tickets/by-rut/12345678-9
```

#### Response 200 - Tickets Encontrados

```json
{
  "nationalId": "12345678-9",
  "activeTickets": [
    {
      "id": 123,
      "numero": "C05",
      "queueType": "CAJA",
      "status": "WAITING",
      "positionInQueue": 3,
      "estimatedWaitTimeMinutes": 15,
      "createdAt": "2025-01-15T10:30:00Z"
    }
  ],
  "totalActiveTickets": 1
}
```

### 3.4 Llamar Siguiente Ticket

Llama al siguiente ticket en la cola (solo asesores).

**Endpoint:** `PUT /tickets/{id}/call`  
**Autenticación:** Requerida (ADVISOR, SUPERVISOR, ADMIN)

#### Request

```http
PUT /api/tickets/123/call
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json

{
  "advisorId": 5,
  "moduleNumber": 3
}
```

#### Response 200 - Ticket Llamado

```json
{
  "id": 123,
  "numero": "C05",
  "status": "CALLED",
  "assignedAdvisorId": 5,
  "assignedAdvisorName": "María González",
  "moduleNumber": 3,
  "calledAt": "2025-01-15T11:15:00Z",
  "message": "Cliente notificado por Telegram"
}
```

### 3.5 Completar Ticket

Marca un ticket como completado (solo asesores).

**Endpoint:** `PUT /tickets/{id}/complete`  
**Autenticación:** Requerida (ADVISOR, SUPERVISOR, ADMIN)

#### Request

```http
PUT /api/tickets/123/complete
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json

{
  "advisorId": 5,
  "notes": "Cliente atendido exitosamente"
}
```

#### Response 200 - Ticket Completado

```json
{
  "id": 123,
  "numero": "C05",
  "status": "COMPLETED",
  "completedAt": "2025-01-15T11:30:00Z",
  "serviceDurationMinutes": 15,
  "advisorNotes": "Cliente atendido exitosamente"
}
```

### 3.6 Listar Tickets por Cola

Obtiene todos los tickets de una cola específica.

**Endpoint:** `GET /tickets/queue/{queueType}`  
**Autenticación:** Requerida (ADVISOR, SUPERVISOR, ADMIN)

#### Request

```http
GET /api/tickets/queue/CAJA?status=WAITING&limit=10
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

#### Query Parameters

| Parámetro | Tipo | Descripción |
|-----------|------|-------------|
| `status` | enum | Filtrar por estado: WAITING, CALLED, IN_PROGRESS, COMPLETED |
| `limit` | int | Número máximo de resultados (default: 50) |
| `offset` | int | Número de registros a saltar (default: 0) |

#### Response 200 - Lista de Tickets

```json
{
  "queueType": "CAJA",
  "tickets": [
    {
      "id": 121,
      "numero": "C03",
      "nationalId": "11111111-1",
      "status": "WAITING",
      "positionInQueue": 1,
      "waitTimeMinutes": 25,
      "createdAt": "2025-01-15T10:00:00Z"
    },
    {
      "id": 122,
      "numero": "C04",
      "nationalId": "22222222-2",
      "status": "WAITING",
      "positionInQueue": 2,
      "waitTimeMinutes": 20,
      "createdAt": "2025-01-15T10:10:00Z"
    }
  ],
  "totalCount": 8,
  "currentPage": 1,
  "totalPages": 1
}
```

---

## 4. Endpoints de Asesores

### 4.1 Listar Asesores

Obtiene la lista de todos los asesores.

**Endpoint:** `GET /advisors`  
**Autenticación:** Requerida (SUPERVISOR, ADMIN)

#### Request

```http
GET /api/advisors?status=AVAILABLE&queueType=CAJA
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

#### Query Parameters

| Parámetro | Tipo | Descripción |
|-----------|------|-------------|
| `status` | enum | Filtrar por estado: AVAILABLE, BUSY, OFFLINE |
| `queueType` | enum | Filtrar por tipo de cola |
| `branchOffice` | string | Filtrar por sucursal |

#### Response 200 - Lista de Asesores

```json
{
  "advisors": [
    {
      "id": 1,
      "name": "María González",
      "email": "maria.gonzalez@coopfila.cl",
      "status": "AVAILABLE",
      "queueTypes": ["CAJA", "PERSONAL"],
      "branchOffice": "Sucursal Centro",
      "moduleNumber": 3,
      "currentTicketId": null,
      "totalTicketsServed": 45,
      "averageServiceTimeMinutes": 8.5,
      "lastActivityAt": "2025-01-15T11:00:00Z"
    },
    {
      "id": 2,
      "name": "Juan Pérez",
      "email": "juan.perez@coopfila.cl",
      "status": "BUSY",
      "queueTypes": ["EMPRESAS", "GERENCIA"],
      "branchOffice": "Sucursal Centro",
      "moduleNumber": 5,
      "currentTicketId": 125,
      "currentTicketNumber": "E02",
      "totalTicketsServed": 32,
      "averageServiceTimeMinutes": 18.2,
      "lastActivityAt": "2025-01-15T11:15:00Z"
    }
  ],
  "totalCount": 12,
  "availableCount": 8,
  "busyCount": 3,
  "offlineCount": 1
}
```

### 4.2 Obtener Asesor Específico

**Endpoint:** `GET /advisors/{id}`

#### Response 200 - Detalle del Asesor

```json
{
  "id": 1,
  "name": "María González",
  "email": "maria.gonzalez@coopfila.cl",
  "status": "AVAILABLE",
  "queueTypes": ["CAJA", "PERSONAL"],
  "branchOffice": "Sucursal Centro",
  "moduleNumber": 3,
  "schedule": {
    "monday": "09:00-18:00",
    "tuesday": "09:00-18:00",
    "wednesday": "09:00-18:00",
    "thursday": "09:00-18:00",
    "friday": "09:00-17:00"
  },
  "metrics": {
    "totalTicketsServed": 45,
    "averageServiceTimeMinutes": 8.5,
    "customerSatisfactionScore": 4.7,
    "ticketsServedToday": 12,
    "hoursWorkedToday": 6.5
  },
  "currentTicket": null,
  "lastActivityAt": "2025-01-15T11:00:00Z"
}
```

### 4.3 Cambiar Estado de Asesor

**Endpoint:** `PUT /advisors/{id}/status`  
**Autenticación:** Requerida (ADVISOR propio, SUPERVISOR, ADMIN)

#### Request

```http
PUT /api/advisors/1/status
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json

{
  "status": "OFFLINE",
  "reason": "Pausa de almuerzo"
}
```

#### Response 200 - Estado Actualizado

```json
{
  "id": 1,
  "name": "María González",
  "status": "OFFLINE",
  "previousStatus": "AVAILABLE",
  "reason": "Pausa de almuerzo",
  "updatedAt": "2025-01-15T12:00:00Z"
}
```

### 4.4 Obtener Siguiente Ticket

Obtiene el siguiente ticket en la cola para un asesor.

**Endpoint:** `GET /advisors/{id}/next-ticket`

#### Response 200 - Siguiente Ticket

```json
{
  "ticket": {
    "id": 124,
    "numero": "C06",
    "nationalId": "33333333-3",
    "queueType": "CAJA",
    "waitTimeMinutes": 35,
    "positionInQueue": 1,
    "createdAt": "2025-01-15T10:25:00Z"
  },
  "queueInfo": {
    "totalWaiting": 5,
    "averageWaitTime": 28,
    "estimatedServiceTime": 8
  }
}
```

#### Response 204 - No Hay Tickets

```http
HTTP/1.1 204 No Content
```

---

## 5. Endpoints de Métricas

### 5.1 Dashboard General

Obtiene métricas generales del sistema.

**Endpoint:** `GET /metrics/dashboard`  
**Autenticación:** Requerida (SUPERVISOR, ADMIN)

#### Response 200 - Métricas del Dashboard

```json
{
  "timestamp": "2025-01-15T11:30:00Z",
  "realTime": {
    "totalTicketsWaiting": 23,
    "totalAdvisorsAvailable": 8,
    "totalAdvisorsBusy": 4,
    "averageWaitTimeMinutes": 32,
    "ticketsCreatedToday": 156,
    "ticketsCompletedToday": 142
  },
  "byQueue": {
    "CAJA": {
      "waiting": 8,
      "averageWaitTime": 15,
      "advisorsAvailable": 3,
      "advisorsBusy": 2
    },
    "PERSONAL": {
      "waiting": 12,
      "averageWaitTime": 45,
      "advisorsAvailable": 2,
      "advisorsBusy": 1
    },
    "EMPRESAS": {
      "waiting": 2,
      "averageWaitTime": 25,
      "advisorsAvailable": 2,
      "advisorsBusy": 1
    },
    "GERENCIA": {
      "waiting": 1,
      "averageWaitTime": 60,
      "advisorsAvailable": 1,
      "advisorsBusy": 0
    }
  },
  "performance": {
    "throughputTicketsPerHour": 18.5,
    "averageServiceTimeMinutes": 12.3,
    "customerSatisfactionScore": 4.6,
    "systemUptimePercentage": 99.8
  }
}
```

### 5.2 Métricas Históricas

**Endpoint:** `GET /metrics/historical`

#### Query Parameters

| Parámetro | Tipo | Descripción |
|-----------|------|-------------|
| `startDate` | date | Fecha inicio (YYYY-MM-DD) |
| `endDate` | date | Fecha fin (YYYY-MM-DD) |
| `granularity` | enum | HOUR, DAY, WEEK, MONTH |
| `queueType` | enum | Filtrar por tipo de cola |

#### Request

```http
GET /api/metrics/historical?startDate=2025-01-01&endDate=2025-01-15&granularity=DAY
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

#### Response 200 - Métricas Históricas

```json
{
  "period": {
    "startDate": "2025-01-01",
    "endDate": "2025-01-15",
    "granularity": "DAY"
  },
  "data": [
    {
      "date": "2025-01-15",
      "ticketsCreated": 156,
      "ticketsCompleted": 142,
      "averageWaitTimeMinutes": 28.5,
      "averageServiceTimeMinutes": 11.2,
      "peakHour": "14:00",
      "peakTicketsPerHour": 25
    },
    {
      "date": "2025-01-14",
      "ticketsCreated": 134,
      "ticketsCompleted": 129,
      "averageWaitTimeMinutes": 32.1,
      "averageServiceTimeMinutes": 12.8,
      "peakHour": "11:00",
      "peakTicketsPerHour": 22
    }
  ],
  "summary": {
    "totalTicketsCreated": 2156,
    "totalTicketsCompleted": 2089,
    "averageWaitTime": 29.8,
    "averageServiceTime": 12.1,
    "completionRate": 96.9
  }
}
```

### 5.3 Métricas de Asesor

**Endpoint:** `GET /metrics/advisors/{id}`

#### Response 200 - Métricas del Asesor

```json
{
  "advisorId": 1,
  "advisorName": "María González",
  "period": "today",
  "metrics": {
    "ticketsServed": 12,
    "hoursWorked": 6.5,
    "averageServiceTimeMinutes": 8.2,
    "customerSatisfactionScore": 4.8,
    "efficiencyScore": 92.5,
    "ticketsPerHour": 1.85
  },
  "performance": {
    "fastestService": 3.5,
    "slowestService": 18.2,
    "mostCommonIssue": "Depósitos",
    "breakTimeMinutes": 45
  },
  "comparison": {
    "teamAverage": {
      "ticketsPerHour": 1.6,
      "averageServiceTime": 12.3,
      "satisfactionScore": 4.6
    },
    "ranking": {
      "position": 2,
      "totalAdvisors": 12
    }
  }
}
```

---

## 6. Endpoints de Notificaciones

### 6.1 Estado de Mensajes

**Endpoint:** `GET /notifications/status`  
**Autenticación:** Requerida (SUPERVISOR, ADMIN)

#### Response 200 - Estado de Notificaciones

```json
{
  "telegram": {
    "status": "OPERATIONAL",
    "messagesInQueue": 3,
    "messagesSentToday": 284,
    "messagesFailedToday": 2,
    "successRate": 99.3,
    "lastMessageSent": "2025-01-15T11:28:00Z"
  },
  "statistics": {
    "totalMessagesSent": 15420,
    "totalMessagesFailed": 89,
    "averageDeliveryTimeSeconds": 2.3,
    "retryRate": 1.2
  },
  "recentFailures": [
    {
      "ticketId": 120,
      "phoneNumber": "+56912345678",
      "errorCode": "INVALID_PHONE",
      "timestamp": "2025-01-15T10:15:00Z",
      "retryCount": 3
    }
  ]
}
```

### 6.2 Reenviar Mensaje

**Endpoint:** `POST /notifications/{messageId}/retry`

#### Request

```http
POST /api/notifications/456/retry
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

#### Response 200 - Mensaje Reenviado

```json
{
  "messageId": 456,
  "status": "SENT",
  "retryCount": 4,
  "sentAt": "2025-01-15T11:35:00Z"
}
```

---

## 7. Modelos de Datos

### 7.1 Ticket

```json
{
  "id": "integer",
  "numero": "string (formato: [LETRA][NUMERO], ej: C05)",
  "nationalId": "string (formato: 12345678-9)",
  "telefono": "string (formato: +56912345678, opcional)",
  "branchOffice": "string",
  "queueType": "enum [CAJA, PERSONAL, EMPRESAS, GERENCIA]",
  "status": "enum [WAITING, CALLED, IN_PROGRESS, COMPLETED, EXPIRED, CANCELLED]",
  "positionInQueue": "integer (0 si no está esperando)",
  "estimatedWaitTimeMinutes": "integer",
  "createdAt": "datetime (ISO 8601)",
  "calledAt": "datetime (ISO 8601, nullable)",
  "startedAt": "datetime (ISO 8601, nullable)",
  "completedAt": "datetime (ISO 8601, nullable)",
  "assignedAdvisorId": "integer (nullable)",
  "assignedAdvisorName": "string (nullable)",
  "moduleNumber": "integer (nullable)",
  "serviceDurationMinutes": "integer (nullable)",
  "advisorNotes": "string (nullable)"
}
```

### 7.2 Advisor

```json
{
  "id": "integer",
  "name": "string",
  "email": "string",
  "status": "enum [AVAILABLE, BUSY, OFFLINE]",
  "queueTypes": "array of enum [CAJA, PERSONAL, EMPRESAS, GERENCIA]",
  "branchOffice": "string",
  "moduleNumber": "integer",
  "currentTicketId": "integer (nullable)",
  "currentTicketNumber": "string (nullable)",
  "totalTicketsServed": "integer",
  "averageServiceTimeMinutes": "float",
  "customerSatisfactionScore": "float (1.0-5.0)",
  "lastActivityAt": "datetime (ISO 8601)"
}
```

### 7.3 Error Response

```json
{
  "error": "string (código de error)",
  "message": "string (mensaje descriptivo)",
  "details": "object (información adicional, opcional)",
  "timestamp": "datetime (ISO 8601)",
  "path": "string (endpoint que generó el error)",
  "requestId": "string (UUID para tracking)"
}
```

---

## 8. Códigos de Error

### 8.1 Errores de Negocio

| Código | HTTP | Descripción |
|--------|------|-------------|
| `DUPLICATE_TICKET` | 409 | Cliente ya tiene ticket activo |
| `INVALID_QUEUE_TYPE` | 422 | Tipo de cola no válido |
| `NO_ADVISORS_AVAILABLE` | 422 | No hay asesores disponibles |
| `TICKET_NOT_FOUND` | 404 | Ticket no existe |
| `TICKET_ALREADY_COMPLETED` | 409 | Ticket ya fue completado |
| `ADVISOR_NOT_AVAILABLE` | 422 | Asesor no disponible |
| `INVALID_TICKET_STATUS` | 422 | Estado de ticket inválido para operación |

### 8.2 Errores de Validación

| Código | HTTP | Descripción |
|--------|------|-------------|
| `INVALID_NATIONAL_ID` | 400 | Formato de RUT inválido |
| `INVALID_PHONE_NUMBER` | 400 | Formato de teléfono inválido |
| `MISSING_REQUIRED_FIELD` | 400 | Campo requerido faltante |
| `INVALID_DATE_FORMAT` | 400 | Formato de fecha inválido |
| `INVALID_ENUM_VALUE` | 400 | Valor de enum no válido |

### 8.3 Errores de Sistema

| Código | HTTP | Descripción |
|--------|------|-------------|
| `DATABASE_ERROR` | 500 | Error de base de datos |
| `TELEGRAM_API_ERROR` | 503 | Servicio de Telegram no disponible |
| `INTERNAL_SERVER_ERROR` | 500 | Error interno del servidor |
| `SERVICE_UNAVAILABLE` | 503 | Servicio temporalmente no disponible |

---

## 9. Rate Limiting

### 9.1 Límites por Endpoint

| Endpoint | Límite | Ventana |
|----------|--------|---------|
| `POST /tickets` | 5 req/min | Por IP |
| `GET /tickets/*` | 60 req/min | Por IP |
| `PUT /tickets/*/call` | 30 req/min | Por usuario |
| `GET /metrics/*` | 100 req/min | Por usuario |
| Otros endpoints | 100 req/min | Por IP |

### 9.2 Headers de Rate Limiting

```http
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 95
X-RateLimit-Reset: 1642248000
```

### 9.3 Respuesta cuando se excede el límite

```json
{
  "error": "RATE_LIMIT_EXCEEDED",
  "message": "Demasiadas solicitudes. Intenta nuevamente en 60 segundos.",
  "retryAfter": 60,
  "timestamp": "2025-01-15T11:30:00Z"
}
```

---

## 10. Ejemplos de Uso

### 10.1 Flujo Completo: Crear y Atender Ticket

```bash
# 1. Crear ticket
curl -X POST "https://api.coopfila.cl/api/tickets" \
  -H "Content-Type: application/json" \
  -d '{
    "nationalId": "12345678-9",
    "telefono": "+56912345678",
    "branchOffice": "Sucursal Centro",
    "queueType": "CAJA"
  }'

# Response: {"id": 123, "numero": "C05", ...}

# 2. Consultar estado
curl "https://api.coopfila.cl/api/tickets/123"

# 3. Llamar ticket (asesor)
curl -X PUT "https://api.coopfila.cl/api/tickets/123/call" \
  -H "Authorization: Bearer TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "advisorId": 5,
    "moduleNumber": 3
  }'

# 4. Completar ticket (asesor)
curl -X PUT "https://api.coopfila.cl/api/tickets/123/complete" \
  -H "Authorization: Bearer TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "advisorId": 5,
    "notes": "Cliente atendido exitosamente"
  }'
```

### 10.2 Consultar Métricas en Tiempo Real

```bash
# Dashboard general
curl "https://api.coopfila.cl/api/metrics/dashboard" \
  -H "Authorization: Bearer TOKEN"

# Métricas de cola específica
curl "https://api.coopfila.cl/api/tickets/queue/CAJA?status=WAITING" \
  -H "Authorization: Bearer TOKEN"

# Estado de asesores
curl "https://api.coopfila.cl/api/advisors?status=AVAILABLE" \
  -H "Authorization: Bearer TOKEN"
```

### 10.3 Gestión de Asesores

```bash
# Cambiar estado a offline
curl -X PUT "https://api.coopfila.cl/api/advisors/1/status" \
  -H "Authorization: Bearer TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "status": "OFFLINE",
    "reason": "Pausa de almuerzo"
  }'

# Obtener siguiente ticket
curl "https://api.coopfila.cl/api/advisors/1/next-ticket" \
  -H "Authorization: Bearer TOKEN"
```

---

## 11. SDKs y Librerías

### 11.1 JavaScript/TypeScript

```javascript
// Instalación
npm install @coopfila/api-client

// Uso
import { CoopFilaAPI } from '@coopfila/api-client';

const api = new CoopFilaAPI({
  baseURL: 'https://api.coopfila.cl/api',
  token: 'your-jwt-token'
});

// Crear ticket
const ticket = await api.tickets.create({
  nationalId: '12345678-9',
  telefono: '+56912345678',
  branchOffice: 'Sucursal Centro',
  queueType: 'CAJA'
});

// Consultar métricas
const metrics = await api.metrics.dashboard();
```

### 11.2 Python

```python
# Instalación
pip install coopfila-api-client

# Uso
from coopfila import CoopFilaAPI

api = CoopFilaAPI(
    base_url='https://api.coopfila.cl/api',
    token='your-jwt-token'
)

# Crear ticket
ticket = api.tickets.create(
    national_id='12345678-9',
    telefono='+56912345678',
    branch_office='Sucursal Centro',
    queue_type='CAJA'
)

# Consultar estado
status = api.tickets.get(ticket.id)
```

---

## 12. Webhooks (Próximamente)

### 12.1 Eventos Disponibles

| Evento | Descripción |
|--------|-------------|
| `ticket.created` | Nuevo ticket creado |
| `ticket.called` | Ticket llamado por asesor |
| `ticket.completed` | Ticket completado |
| `advisor.status_changed` | Estado de asesor cambió |
| `queue.threshold_reached` | Cola alcanzó umbral de espera |

### 12.2 Formato de Webhook

```json
{
  "event": "ticket.created",
  "timestamp": "2025-01-15T11:30:00Z",
  "data": {
    "ticket": {
      "id": 123,
      "numero": "C05",
      "queueType": "CAJA",
      "status": "WAITING"
    }
  },
  "signature": "sha256=..."
}
```

---

## 13. Changelog

### v1.0.0 (2025-01-15)
- ✅ Lanzamiento inicial de la API
- ✅ Endpoints completos para tickets y asesores
- ✅ Sistema de métricas en tiempo real
- ✅ Integración con Telegram
- ✅ Autenticación JWT
- ✅ Rate limiting implementado

### Próximas Versiones
- 🔄 v1.1.0: Webhooks y notificaciones push
- 🔄 v1.2.0: Soporte para múltiples sucursales
- 🔄 v1.3.0: Analytics avanzados y reportes
- 🔄 v2.0.0: GraphQL API

---

**Fin de la Documentación API**  
**Última actualización:** 15 de Enero, 2025  
**Versión del documento:** 1.0  
**Contacto técnico:** api-support@coopfila.cl