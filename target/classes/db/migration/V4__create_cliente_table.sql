-- V4__create_cliente_table.sql
-- Crear tabla cliente y normalizar datos

CREATE TABLE cliente (
    id BIGSERIAL PRIMARY KEY,
    national_id VARCHAR(20) NOT NULL UNIQUE,
    nombre VARCHAR(100) NOT NULL,
    apellido VARCHAR(100) NOT NULL,
    telefono VARCHAR(20),
    email VARCHAR(100),
    fecha_nacimiento DATE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Índices
CREATE UNIQUE INDEX idx_cliente_national_id ON cliente(national_id);
CREATE INDEX idx_cliente_telefono ON cliente(telefono);
CREATE INDEX idx_cliente_created_at ON cliente(created_at);

-- Migrar datos existentes de ticket a cliente
INSERT INTO cliente (national_id, telefono, nombre, apellido)
SELECT DISTINCT 
    national_id,
    telefono,
    'Cliente',  -- nombre por defecto
    'Pendiente' -- apellido por defecto
FROM ticket 
WHERE national_id IS NOT NULL;

-- Agregar cliente_id a ticket
ALTER TABLE ticket ADD COLUMN cliente_id BIGINT;

-- Actualizar tickets con cliente_id
UPDATE ticket 
SET cliente_id = c.id 
FROM cliente c 
WHERE ticket.national_id = c.national_id;

-- Hacer cliente_id NOT NULL y agregar FK
ALTER TABLE ticket ALTER COLUMN cliente_id SET NOT NULL;
ALTER TABLE ticket ADD CONSTRAINT fk_ticket_cliente 
    FOREIGN KEY (cliente_id) REFERENCES cliente(id) ON DELETE RESTRICT;

-- Eliminar campos redundantes de ticket
ALTER TABLE ticket DROP COLUMN national_id;
ALTER TABLE ticket DROP COLUMN telefono;

COMMENT ON TABLE cliente IS 'Información de clientes del sistema';