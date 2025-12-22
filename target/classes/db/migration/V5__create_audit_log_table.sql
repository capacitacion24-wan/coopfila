-- V5__create_audit_log_table.sql
-- Tabla de auditoría para trazabilidad

CREATE TABLE audit_log (
    id BIGSERIAL PRIMARY KEY,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    tipo_evento VARCHAR(50) NOT NULL,
    actor VARCHAR(100) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id VARCHAR(50) NOT NULL,
    cambios_estado JSONB,
    detalles_adicionales JSONB,
    ip_address INET
);

-- Índices para consultas frecuentes
CREATE INDEX idx_audit_timestamp ON audit_log(timestamp);
CREATE INDEX idx_audit_entity ON audit_log(entity_type, entity_id);
CREATE INDEX idx_audit_tipo_evento ON audit_log(tipo_evento);
CREATE INDEX idx_audit_actor ON audit_log(actor);

COMMENT ON TABLE audit_log IS 'Registro de auditoría de eventos del sistema';