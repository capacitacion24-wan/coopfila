# Modelo de Datos - CoopFila

**Sistema de Gestión de Tickets con Notificaciones en Tiempo Real**  
**Versión:** 1.0  
**Fecha:** Diciembre 2025  
**Audiencia:** Desarrolladores, DBAs, Arquitectos de Datos

---

## 1. Resumen del Modelo

### 1.1 Descripción General

El modelo de datos de CoopFila está diseñado para soportar un sistema de gestión de tickets eficiente y escalable, con las siguientes características principales:

- **Gestión de tickets** con estados y transiciones controladas
- **Asignación dinámica** de asesores por tipo de cola
- **Trazabilidad completa** de eventos y cambios de estado
- **Sistema de notificaciones** con reintentos y estados
- **Métricas y auditoría** para análisis de performance

### 1.2 Tecnología de Base de Datos

| Atributo | Valor |
|----------|-------|
| **Motor** | PostgreSQL 15 |
| **Charset** | UTF-8 |
| **Timezone** | UTC |
| **Connection Pool** | HikariCP (max 20 conexiones) |
| **Migrations** | Flyway |

### 1.3 Principios de Diseño

✅ **Normalización:** 3NF para evitar redundancia  
✅ **Integridad referencial:** Foreign keys estrictas  
✅ **Auditoría:** Timestamps en todas las tablas  
✅ **Performance:** Índices optimizados para queries frecuentes  
✅ **Escalabilidad:** Diseño preparado para particionado  

---

## 2. Diagrama Entidad-Relación

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│     ADVISOR     │    │     TICKET      │    │  TICKET_EVENT   │
│                 │    │                 │    │                 │
│ • id (PK)       │◄──┤ • id (PK)       │───►│ • id (PK)       │
│ • name          │   │ • numero        │    │ • ticket_id (FK)│
│ • email         │   │ • national_id   │    │ • event_type    │
│ • status        │   │ • telefono      │    │ • description   │
│ • queue_types   │   │ • branch_office │    │ • created_at    │
│ • branch_office │   │ • queue_type    │    └─────────────────┘
│ • module_number │   │ • status        │
│ • created_at    │   │ • position      │    ┌─────────────────┐
│ • updated_at    │   │ • estimated_wait│    │    MENSAJE      │
└─────────────────┘   │ • assigned_adv  │◄───┤ • id (PK)       │
                      │ • created_at    │    │ • ticket_id (FK)│
                      │ • called_at     │    │ • telefono      │
                      │ • started_at    │    │ • mensaje       │
                      │ • completed_at  │    │ • tipo_mensaje  │
                      └─────────────────┘    │ • estado_envio  │
                                             │ • intentos      │
                                             │ • fecha_prog    │
                                             │ • fecha_envio   │
                                             │ • created_at    │
                                             └─────────────────┘
```

---

## 3. Tablas Principales

### 3.1 Tabla: advisor

**Propósito:** Almacena información de los asesores que atienden tickets.

```sql
CREATE TABLE advisor (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    queue_types TEXT[] NOT NULL,
    branch_office VARCHAR(100) NOT NULL,
    module_number INTEGER,
    total_tickets_served INTEGER DEFAULT 0,
    average_service_time_minutes DECIMAL(5,2) DEFAULT 0.0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    
    CONSTRAINT chk_advisor_status 
        CHECK (status IN ('AVAILABLE', 'BUSY', 'OFFLINE')),
    CONSTRAINT chk_module_number 
        CHECK (module_number > 0 AND module_number <= 50)
);
```

#### Campos

| Campo | Tipo | Descripción | Restricciones |
|-------|------|-------------|---------------|
| `id` | BIGSERIAL | Identificador único | PK, Auto-increment |
| `name` | VARCHAR(100) | Nombre completo del asesor | NOT NULL |
| `email` | VARCHAR(100) | Email corporativo | UNIQUE, NOT NULL |
| `status` | VARCHAR(20) | Estado actual | AVAILABLE/BUSY/OFFLINE |
| `queue_types` | TEXT[] | Tipos de cola que puede atender | Array PostgreSQL |
| `branch_office` | VARCHAR(100) | Sucursal asignada | NOT NULL |
| `module_number` | INTEGER | Número de módulo de atención | 1-50 |
| `total_tickets_served` | INTEGER | Total de tickets atendidos | DEFAULT 0 |
| `average_service_time_minutes` | DECIMAL(5,2) | Tiempo promedio de atención | DEFAULT 0.0 |

#### Índices

```sql
CREATE INDEX idx_advisor_status ON advisor(status);
CREATE INDEX idx_advisor_branch ON advisor(branch_office);
CREATE INDEX idx_advisor_queue_types ON advisor USING GIN(queue_types);
CREATE INDEX idx_advisor_status_branch ON advisor(status, branch_office);
```

#### Datos de Ejemplo

```sql
INSERT INTO advisor (name, email, status, queue_types, branch_office, module_number) VALUES
('María González', 'maria.gonzalez@coopfila.cl', 'AVAILABLE', '{"CAJA","PERSONAL"}', 'Sucursal Centro', 1),
('Juan Pérez', 'juan.perez@coopfila.cl', 'BUSY', '{"EMPRESAS","GERENCIA"}', 'Sucursal Centro', 2),
('Ana Silva', 'ana.silva@coopfila.cl', 'AVAILABLE', '{"CAJA"}', 'Sucursal Norte', 3);
```

### 3.2 Tabla: ticket

**Propósito:** Almacena los tickets creados por los clientes.

```sql
CREATE TABLE ticket (
    id BIGSERIAL PRIMARY KEY,
    numero VARCHAR(10) NOT NULL UNIQUE,
    national_id VARCHAR(20) NOT NULL,
    telefono VARCHAR(20),
    branch_office VARCHAR(100) NOT NULL,
    queue_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'WAITING',
    position_in_queue INTEGER DEFAULT 0,
    estimated_wait_time_minutes INTEGER DEFAULT 0,
    assigned_advisor_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    called_at TIMESTAMP,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    
    CONSTRAINT fk_ticket_advisor 
        FOREIGN KEY (assigned_advisor_id) REFERENCES advisor(id),
    CONSTRAINT chk_ticket_status 
        CHECK (status IN ('WAITING', 'CALLED', 'IN_PROGRESS', 'COMPLETED', 'EXPIRED', 'CANCELLED')),
    CONSTRAINT chk_queue_type 
        CHECK (queue_type IN ('CAJA', 'PERSONAL', 'EMPRESAS', 'GERENCIA')),
    CONSTRAINT chk_position 
        CHECK (position_in_queue >= 0),
    CONSTRAINT chk_wait_time 
        CHECK (estimated_wait_time_minutes >= 0),
    CONSTRAINT uk_active_ticket_per_customer 
        UNIQUE (national_id, queue_type) DEFERRABLE INITIALLY DEFERRED
);
```

#### Campos

| Campo | Tipo | Descripción | Restricciones |
|-------|------|-------------|---------------|
| `id` | BIGSERIAL | Identificador único | PK, Auto-increment |
| `numero` | VARCHAR(10) | Número de ticket (ej: C05) | UNIQUE, NOT NULL |
| `national_id` | VARCHAR(20) | RUT o cédula del cliente | NOT NULL |
| `telefono` | VARCHAR(20) | Teléfono para notificaciones | Opcional |
| `branch_office` | VARCHAR(100) | Sucursal donde se creó | NOT NULL |
| `queue_type` | VARCHAR(20) | Tipo de cola | CAJA/PERSONAL/EMPRESAS/GERENCIA |
| `status` | VARCHAR(20) | Estado actual del ticket | Ver enum |
| `position_in_queue` | INTEGER | Posición en la cola | >= 0 |
| `estimated_wait_time_minutes` | INTEGER | Tiempo estimado de espera | >= 0 |
| `assigned_advisor_id` | BIGINT | Asesor asignado | FK a advisor |

#### Estados del Ticket

```sql
-- Flujo normal de estados
WAITING → CALLED → IN_PROGRESS → COMPLETED

-- Estados alternativos
WAITING → EXPIRED    (timeout sin respuesta)
WAITING → CANCELLED  (cancelado por cliente)
CALLED → EXPIRED     (no se presenta)
```

#### Índices

```sql
CREATE INDEX idx_ticket_status ON ticket(status);
CREATE INDEX idx_ticket_queue_type ON ticket(queue_type);
CREATE INDEX idx_ticket_national_id ON ticket(national_id);
CREATE INDEX idx_ticket_created_at ON ticket(created_at DESC);
CREATE INDEX idx_ticket_queue_status ON ticket(queue_type, status);
CREATE INDEX idx_ticket_advisor ON ticket(assigned_advisor_id);
CREATE INDEX idx_ticket_branch_office ON ticket(branch_office);

-- Índice para consultas de cola
CREATE INDEX idx_ticket_queue_waiting ON ticket(queue_type, status, created_at) 
    WHERE status = 'WAITING';
```

#### Datos de Ejemplo

```sql
INSERT INTO ticket (numero, national_id, telefono, branch_office, queue_type, status, position_in_queue) VALUES
('C001', '12345678-9', '+56912345678', 'Sucursal Centro', 'CAJA', 'WAITING', 1),
('P001', '87654321-0', '+56987654321', 'Sucursal Centro', 'PERSONAL', 'CALLED', 0),
('E001', '11111111-1', '+56911111111', 'Sucursal Norte', 'EMPRESAS', 'COMPLETED', 0);
```

### 3.3 Tabla: ticket_event

**Propósito:** Auditoría y trazabilidad de cambios en los tickets.

```sql
CREATE TABLE ticket_event (
    id BIGSERIAL PRIMARY KEY,
    ticket_id BIGINT NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    description TEXT,
    old_status VARCHAR(20),
    new_status VARCHAR(20),
    advisor_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    CONSTRAINT fk_ticket_event_ticket 
        FOREIGN KEY (ticket_id) REFERENCES ticket(id) ON DELETE CASCADE,
    CONSTRAINT fk_ticket_event_advisor 
        FOREIGN KEY (advisor_id) REFERENCES advisor(id)
);
```

#### Tipos de Eventos

| Tipo | Descripción |
|------|-------------|
| `CREATED` | Ticket creado |
| `CALLED` | Ticket llamado por asesor |
| `STARTED` | Atención iniciada |
| `COMPLETED` | Atención completada |
| `EXPIRED` | Ticket expirado por timeout |
| `CANCELLED` | Ticket cancelado |
| `REASSIGNED` | Reasignado a otro asesor |

#### Índices

```sql
CREATE INDEX idx_ticket_event_ticket_id ON ticket_event(ticket_id);
CREATE INDEX idx_ticket_event_type ON ticket_event(event_type);
CREATE INDEX idx_ticket_event_created_at ON ticket_event(created_at DESC);
```

### 3.4 Tabla: mensaje

**Propósito:** Gestión de notificaciones enviadas a clientes.

```sql
CREATE TABLE mensaje (
    id BIGSERIAL PRIMARY KEY,
    ticket_id BIGINT NOT NULL,
    telefono VARCHAR(20) NOT NULL,
    mensaje TEXT NOT NULL,
    tipo_mensaje VARCHAR(50) NOT NULL,
    estado_envio VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    intentos INTEGER DEFAULT 0,
    fecha_programada TIMESTAMP NOT NULL,
    fecha_envio TIMESTAMP,
    error_mensaje TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    CONSTRAINT fk_mensaje_ticket 
        FOREIGN KEY (ticket_id) REFERENCES ticket(id) ON DELETE CASCADE,
    CONSTRAINT chk_estado_envio 
        CHECK (estado_envio IN ('PENDIENTE', 'ENVIADO', 'FALLIDO')),
    CONSTRAINT chk_intentos 
        CHECK (intentos >= 0 AND intentos <= 5)
);
```

#### Tipos de Mensaje

| Tipo | Cuándo se envía |
|------|-----------------|
| `CONFIRMACION` | Al crear el ticket |
| `PRE_AVISO` | Cuando quedan 3 turnos |
| `TURNO_ACTIVO` | Cuando es llamado |
| `COMPLETADO` | Cuando se completa la atención |
| `EXPIRADO` | Cuando expira por timeout |

#### Estados de Envío

```sql
-- Flujo normal
PENDIENTE → ENVIADO

-- Con errores
PENDIENTE → FALLIDO (después de 3 intentos)
```

#### Índices

```sql
CREATE INDEX idx_mensaje_ticket_id ON mensaje(ticket_id);
CREATE INDEX idx_mensaje_estado ON mensaje(estado_envio);
CREATE INDEX idx_mensaje_fecha_prog ON mensaje(fecha_programada);
CREATE INDEX idx_mensaje_pendientes ON mensaje(estado_envio, fecha_programada) 
    WHERE estado_envio = 'PENDIENTE';
```

---

## 4. Vistas y Consultas Optimizadas

### 4.1 Vista: queue_status

**Propósito:** Vista consolidada del estado de las colas.

```sql
CREATE VIEW queue_status AS
SELECT 
    t.queue_type,
    t.branch_office,
    COUNT(*) FILTER (WHERE t.status = 'WAITING') as tickets_waiting,
    COUNT(*) FILTER (WHERE t.status = 'CALLED') as tickets_called,
    COUNT(*) FILTER (WHERE t.status = 'IN_PROGRESS') as tickets_in_progress,
    AVG(EXTRACT(EPOCH FROM (NOW() - t.created_at))/60) 
        FILTER (WHERE t.status = 'WAITING') as avg_wait_time_minutes,
    COUNT(a.*) FILTER (WHERE a.status = 'AVAILABLE') as advisors_available,
    COUNT(a.*) FILTER (WHERE a.status = 'BUSY') as advisors_busy
FROM ticket t
LEFT JOIN advisor a ON (
    a.branch_office = t.branch_office 
    AND t.queue_type = ANY(a.queue_types)
)
WHERE t.status IN ('WAITING', 'CALLED', 'IN_PROGRESS')
GROUP BY t.queue_type, t.branch_office;
```

### 4.2 Vista: advisor_metrics

**Propósito:** Métricas de performance de asesores.

```sql
CREATE VIEW advisor_metrics AS
SELECT 
    a.id,
    a.name,
    a.status,
    a.branch_office,
    COUNT(t.id) as tickets_served_today,
    AVG(EXTRACT(EPOCH FROM (t.completed_at - t.started_at))/60) as avg_service_time_today,
    COUNT(t.id) FILTER (WHERE t.completed_at >= CURRENT_DATE - INTERVAL '7 days') as tickets_served_week,
    a.total_tickets_served,
    a.average_service_time_minutes
FROM advisor a
LEFT JOIN ticket t ON (
    t.assigned_advisor_id = a.id 
    AND t.status = 'COMPLETED'
    AND t.completed_at >= CURRENT_DATE
)
GROUP BY a.id, a.name, a.status, a.branch_office, a.total_tickets_served, a.average_service_time_minutes;
```

### 4.3 Consultas Frecuentes Optimizadas

#### Obtener siguiente ticket en cola

```sql
-- Query optimizada con índice específico
SELECT t.id, t.numero, t.national_id, t.created_at
FROM ticket t
WHERE t.queue_type = $1 
  AND t.status = 'WAITING'
  AND t.branch_office = $2
ORDER BY t.created_at ASC
LIMIT 1;

-- Índice requerido
CREATE INDEX idx_next_ticket ON ticket(queue_type, status, branch_office, created_at)
WHERE status = 'WAITING';
```

#### Calcular posición en cola

```sql
-- Query optimizada para calcular posición
SELECT COUNT(*) + 1 as position
FROM ticket t1
WHERE t1.queue_type = $1
  AND t1.status = 'WAITING'
  AND t1.branch_office = $2
  AND t1.created_at < (
    SELECT t2.created_at 
    FROM ticket t2 
    WHERE t2.id = $3
  );
```

#### Tickets activos por cliente

```sql
-- Query para verificar tickets duplicados
SELECT t.id, t.numero, t.queue_type, t.status
FROM ticket t
WHERE t.national_id = $1
  AND t.status IN ('WAITING', 'CALLED', 'IN_PROGRESS')
  AND t.branch_office = $2;
```

---

## 5. Triggers y Funciones

### 5.1 Trigger: update_ticket_position

**Propósito:** Actualiza automáticamente las posiciones en cola.

```sql
CREATE OR REPLACE FUNCTION update_queue_positions()
RETURNS TRIGGER AS $$
BEGIN
    -- Actualizar posiciones cuando cambia el estado de un ticket
    IF OLD.status = 'WAITING' AND NEW.status != 'WAITING' THEN
        -- Decrementar posición de tickets posteriores
        UPDATE ticket 
        SET position_in_queue = position_in_queue - 1
        WHERE queue_type = OLD.queue_type
          AND branch_office = OLD.branch_office
          AND status = 'WAITING'
          AND created_at > OLD.created_at;
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_update_queue_positions
    AFTER UPDATE ON ticket
    FOR EACH ROW
    WHEN (OLD.status IS DISTINCT FROM NEW.status)
    EXECUTE FUNCTION update_queue_positions();
```

### 5.2 Trigger: audit_ticket_changes

**Propósito:** Registra automáticamente eventos de auditoría.

```sql
CREATE OR REPLACE FUNCTION audit_ticket_changes()
RETURNS TRIGGER AS $$
BEGIN
    -- Insertar evento de auditoría
    INSERT INTO ticket_event (
        ticket_id, 
        event_type, 
        description, 
        old_status, 
        new_status,
        advisor_id
    ) VALUES (
        NEW.id,
        CASE 
            WHEN TG_OP = 'INSERT' THEN 'CREATED'
            WHEN OLD.status != NEW.status THEN 
                CASE NEW.status
                    WHEN 'CALLED' THEN 'CALLED'
                    WHEN 'IN_PROGRESS' THEN 'STARTED'
                    WHEN 'COMPLETED' THEN 'COMPLETED'
                    WHEN 'EXPIRED' THEN 'EXPIRED'
                    WHEN 'CANCELLED' THEN 'CANCELLED'
                    ELSE 'STATUS_CHANGED'
                END
            ELSE 'UPDATED'
        END,
        CASE 
            WHEN TG_OP = 'INSERT' THEN 'Ticket creado'
            WHEN OLD.status != NEW.status THEN 
                'Estado cambiado de ' || OLD.status || ' a ' || NEW.status
            ELSE 'Ticket actualizado'
        END,
        CASE WHEN TG_OP = 'UPDATE' THEN OLD.status ELSE NULL END,
        NEW.status,
        NEW.assigned_advisor_id
    );
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_audit_ticket_changes
    AFTER INSERT OR UPDATE ON ticket
    FOR EACH ROW
    EXECUTE FUNCTION audit_ticket_changes();
```

### 5.3 Función: calculate_estimated_wait_time

**Propósito:** Calcula tiempo estimado de espera.

```sql
CREATE OR REPLACE FUNCTION calculate_estimated_wait_time(
    p_queue_type VARCHAR(20),
    p_branch_office VARCHAR(100)
) RETURNS INTEGER AS $$
DECLARE
    tickets_ahead INTEGER;
    available_advisors INTEGER;
    avg_service_time DECIMAL;
    estimated_minutes INTEGER;
BEGIN
    -- Contar tickets esperando
    SELECT COUNT(*) INTO tickets_ahead
    FROM ticket
    WHERE queue_type = p_queue_type
      AND branch_office = p_branch_office
      AND status = 'WAITING';
    
    -- Contar asesores disponibles
    SELECT COUNT(*) INTO available_advisors
    FROM advisor
    WHERE branch_office = p_branch_office
      AND status IN ('AVAILABLE', 'BUSY')
      AND p_queue_type = ANY(queue_types);
    
    -- Obtener tiempo promedio de servicio
    SELECT COALESCE(AVG(average_service_time_minutes), 10) INTO avg_service_time
    FROM advisor
    WHERE branch_office = p_branch_office
      AND p_queue_type = ANY(queue_types);
    
    -- Calcular estimación
    IF available_advisors > 0 THEN
        estimated_minutes := CEIL(tickets_ahead * avg_service_time / available_advisors);
    ELSE
        estimated_minutes := tickets_ahead * 15; -- Default 15 min por ticket
    END IF;
    
    RETURN estimated_minutes;
END;
$$ LANGUAGE plpgsql;
```

---

## 6. Índices de Performance

### 6.1 Índices Principales

```sql
-- Índices para queries frecuentes de tickets
CREATE INDEX CONCURRENTLY idx_ticket_queue_status_created 
    ON ticket(queue_type, status, created_at);

CREATE INDEX CONCURRENTLY idx_ticket_national_id_active 
    ON ticket(national_id, status) 
    WHERE status IN ('WAITING', 'CALLED', 'IN_PROGRESS');

CREATE INDEX CONCURRENTLY idx_ticket_advisor_status 
    ON ticket(assigned_advisor_id, status);

-- Índices para asesores
CREATE INDEX CONCURRENTLY idx_advisor_status_queue_branch 
    ON advisor(status, branch_office) USING GIN(queue_types);

-- Índices para mensajes
CREATE INDEX CONCURRENTLY idx_mensaje_pendientes_fecha 
    ON mensaje(estado_envio, fecha_programada) 
    WHERE estado_envio = 'PENDIENTE';

-- Índices para eventos (auditoría)
CREATE INDEX CONCURRENTLY idx_ticket_event_ticket_date 
    ON ticket_event(ticket_id, created_at DESC);
```

### 6.2 Índices Parciales

```sql
-- Solo tickets activos
CREATE INDEX idx_active_tickets 
    ON ticket(queue_type, branch_office, created_at) 
    WHERE status IN ('WAITING', 'CALLED', 'IN_PROGRESS');

-- Solo asesores disponibles
CREATE INDEX idx_available_advisors 
    ON advisor(branch_office) USING GIN(queue_types) 
    WHERE status = 'AVAILABLE';

-- Solo mensajes fallidos
CREATE INDEX idx_failed_messages 
    ON mensaje(created_at DESC) 
    WHERE estado_envio = 'FALLIDO';
```

---

## 7. Particionado (Preparación Futura)

### 7.1 Particionado por Fecha

```sql
-- Preparar tabla ticket_event para particionado
CREATE TABLE ticket_event_partitioned (
    LIKE ticket_event INCLUDING ALL
) PARTITION BY RANGE (created_at);

-- Particiones mensuales
CREATE TABLE ticket_event_2025_01 PARTITION OF ticket_event_partitioned
    FOR VALUES FROM ('2025-01-01') TO ('2025-02-01');

CREATE TABLE ticket_event_2025_02 PARTITION OF ticket_event_partitioned
    FOR VALUES FROM ('2025-02-01') TO ('2025-03-01');
```

### 7.2 Particionado por Sucursal

```sql
-- Preparar para múltiples sucursales
CREATE TABLE ticket_by_branch (
    LIKE ticket INCLUDING ALL
) PARTITION BY LIST (branch_office);

CREATE TABLE ticket_centro PARTITION OF ticket_by_branch
    FOR VALUES IN ('Sucursal Centro');

CREATE TABLE ticket_norte PARTITION OF ticket_by_branch
    FOR VALUES IN ('Sucursal Norte');
```

---

## 8. Constraints y Validaciones

### 8.1 Constraints de Integridad

```sql
-- Constraint: Un asesor solo puede tener un ticket activo
ALTER TABLE ticket ADD CONSTRAINT chk_one_active_ticket_per_advisor
    EXCLUDE (assigned_advisor_id WITH =) 
    WHERE (status IN ('CALLED', 'IN_PROGRESS'));

-- Constraint: Fechas lógicas
ALTER TABLE ticket ADD CONSTRAINT chk_logical_dates
    CHECK (
        (called_at IS NULL OR called_at >= created_at) AND
        (started_at IS NULL OR started_at >= called_at) AND
        (completed_at IS NULL OR completed_at >= started_at)
    );

-- Constraint: Solo un ticket activo por cliente por cola
ALTER TABLE ticket ADD CONSTRAINT chk_one_active_per_customer_queue
    EXCLUDE (national_id WITH =, queue_type WITH =) 
    WHERE (status IN ('WAITING', 'CALLED', 'IN_PROGRESS'));
```

### 8.2 Validaciones de Datos

```sql
-- Validar formato de RUT chileno
ALTER TABLE ticket ADD CONSTRAINT chk_valid_rut
    CHECK (national_id ~ '^[0-9]{7,8}-[0-9Kk]$');

-- Validar formato de teléfono
ALTER TABLE ticket ADD CONSTRAINT chk_valid_phone
    CHECK (telefono IS NULL OR telefono ~ '^\+56[0-9]{9}$');

-- Validar número de ticket
ALTER TABLE ticket ADD CONSTRAINT chk_valid_numero
    CHECK (numero ~ '^[A-Z][0-9]{2,3}$');
```

---

## 9. Datos de Referencia

### 9.1 Configuración del Sistema

```sql
CREATE TABLE system_config (
    key VARCHAR(100) PRIMARY KEY,
    value TEXT NOT NULL,
    description TEXT,
    updated_at TIMESTAMP DEFAULT NOW()
);

INSERT INTO system_config (key, value, description) VALUES
('max_wait_time_minutes', '180', 'Tiempo máximo de espera antes de expirar ticket'),
('notification_retry_attempts', '3', 'Número máximo de reintentos para notificaciones'),
('pre_notification_threshold', '3', 'Posición en cola para enviar pre-aviso'),
('service_time_default_minutes', '10', 'Tiempo de servicio por defecto'),
('telegram_bot_token', 'ENCRYPTED_TOKEN', 'Token del bot de Telegram'),
('system_timezone', 'America/Santiago', 'Zona horaria del sistema');
```

### 9.2 Datos Maestros

```sql
-- Tipos de cola con configuración
CREATE TABLE queue_config (
    queue_type VARCHAR(20) PRIMARY KEY,
    display_name VARCHAR(50) NOT NULL,
    description TEXT,
    default_service_time_minutes INTEGER DEFAULT 10,
    max_concurrent_tickets INTEGER DEFAULT 100,
    priority_order INTEGER DEFAULT 1,
    active BOOLEAN DEFAULT true
);

INSERT INTO queue_config VALUES
('CAJA', 'Caja', 'Operaciones bancarias básicas', 5, 200, 1, true),
('PERSONAL', 'Personal Banker', 'Asesoría financiera personalizada', 15, 50, 2, true),
('EMPRESAS', 'Empresas', 'Atención a empresas y corporaciones', 20, 30, 3, true),
('GERENCIA', 'Gerencia', 'Casos especiales y reclamos', 30, 10, 4, true);

-- Sucursales
CREATE TABLE branch_office (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) UNIQUE NOT NULL,
    address TEXT,
    phone VARCHAR(20),
    active BOOLEAN DEFAULT true,
    max_advisors INTEGER DEFAULT 20,
    timezone VARCHAR(50) DEFAULT 'America/Santiago'
);

INSERT INTO branch_office (name, address, phone) VALUES
('Sucursal Centro', 'Av. Libertador 1234, Santiago', '+56222345678'),
('Sucursal Norte', 'Av. Independencia 5678, Santiago', '+56222876543'),
('Sucursal Sur', 'Gran Avenida 9012, San Miguel', '+56223456789');
```

---

## 10. Scripts de Mantenimiento

### 10.1 Limpieza de Datos Antiguos

```sql
-- Eliminar tickets completados antiguos (>90 días)
DELETE FROM ticket 
WHERE status = 'COMPLETED' 
  AND completed_at < NOW() - INTERVAL '90 days';

-- Archivar eventos antiguos (>1 año)
CREATE TABLE ticket_event_archive AS 
SELECT * FROM ticket_event 
WHERE created_at < NOW() - INTERVAL '1 year';

DELETE FROM ticket_event 
WHERE created_at < NOW() - INTERVAL '1 year';

-- Limpiar mensajes antiguos
DELETE FROM mensaje 
WHERE estado_envio = 'ENVIADO' 
  AND fecha_envio < NOW() - INTERVAL '30 days';
```

### 10.2 Reindexado y Mantenimiento

```sql
-- Reindexar tablas principales
REINDEX TABLE ticket;
REINDEX TABLE advisor;
REINDEX TABLE mensaje;

-- Actualizar estadísticas
ANALYZE ticket;
ANALYZE advisor;
ANALYZE ticket_event;
ANALYZE mensaje;

-- Vacuum para recuperar espacio
VACUUM (ANALYZE, VERBOSE) ticket;
VACUUM (ANALYZE, VERBOSE) ticket_event;
```

### 10.3 Monitoreo de Performance

```sql
-- Query para identificar queries lentas
SELECT 
    query,
    calls,
    total_time,
    mean_time,
    rows
FROM pg_stat_statements 
WHERE query LIKE '%ticket%' 
ORDER BY mean_time DESC 
LIMIT 10;

-- Tamaño de tablas
SELECT 
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) as size
FROM pg_tables 
WHERE schemaname = 'public'
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;
```

---

## 11. Backup y Recovery

### 11.1 Estrategia de Backup

```bash
# Backup completo diario
pg_dump -h localhost -U ticketero_user -d ticketero \
    --format=custom --compress=9 \
    --file=backup_$(date +%Y%m%d).dump

# Backup incremental (WAL archiving)
archive_command = 'cp %p /backup/wal/%f'
```

### 11.2 Scripts de Recovery

```sql
-- Restaurar desde backup
pg_restore -h localhost -U ticketero_user -d ticketero_restore \
    --clean --create --verbose backup_20250115.dump

-- Point-in-time recovery
pg_ctl stop -D /var/lib/postgresql/data
cp -R /backup/base/* /var/lib/postgresql/data/
echo "restore_command = 'cp /backup/wal/%f %p'" > recovery.conf
pg_ctl start -D /var/lib/postgresql/data
```

---

## 12. Migración y Versionado

### 12.1 Flyway Migrations

```sql
-- V1__create_initial_schema.sql
-- V2__add_advisor_metrics.sql
-- V3__add_message_retry_logic.sql
-- V4__add_queue_configuration.sql
-- V5__optimize_indexes.sql
```

### 12.2 Schema Evolution

```sql
-- Ejemplo: Agregar nueva columna
ALTER TABLE advisor ADD COLUMN phone VARCHAR(20);
UPDATE advisor SET phone = '+56900000000' WHERE phone IS NULL;
ALTER TABLE advisor ALTER COLUMN phone SET NOT NULL;

-- Ejemplo: Cambiar tipo de dato
ALTER TABLE ticket ALTER COLUMN estimated_wait_time_minutes TYPE BIGINT;
```

---

## 13. Conclusiones

### 13.1 Características del Modelo

✅ **Escalable:** Preparado para múltiples sucursales y alto volumen  
✅ **Auditable:** Trazabilidad completa de cambios  
✅ **Performante:** Índices optimizados para queries frecuentes  
✅ **Íntegro:** Constraints que garantizan consistencia  
✅ **Mantenible:** Scripts automatizados de limpieza  

### 13.2 Métricas de Performance Esperadas

| Operación | Tiempo Objetivo | Índice Requerido |
|-----------|-----------------|------------------|
| Crear ticket | < 100ms | idx_ticket_national_id_active |
| Consultar posición | < 50ms | idx_ticket_queue_status_created |
| Asignar asesor | < 200ms | idx_available_advisors |
| Listar cola | < 150ms | idx_active_tickets |
| Auditoría | < 300ms | idx_ticket_event_ticket_date |

### 13.3 Recomendaciones

1. **Monitoreo continuo** de performance de queries
2. **Particionado** cuando se superen 1M de tickets
3. **Archivado automático** de datos históricos
4. **Replicación** para alta disponibilidad
5. **Connection pooling** optimizado según carga

---

**Fin del Modelo de Datos**  
**Versión:** 1.0  
**Base de Datos:** PostgreSQL 15  
**Última actualización:** 15 de Enero, 2025