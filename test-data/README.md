# PRUEBA COMPLETA SISTEMA TICKETERO

## Configuración de Prueba

**Bot Telegram:** `8591640924:AAG7t3qQ52aOvzEC2XtNh9BhHRPxdqe4VVg`  
**Chat ID:** `1634964503`  
**API URL:** `http://localhost:8080`

## Datos de Prueba

### 4 Clientes Creados:
1. **William García** (RUT: 12345678-9) - Teléfono: 1634964503
2. **Johanna López** (RUT: 98765432-1) - Teléfono: 1634964503  
3. **Natalia Martínez** (RUT: 11111111-1) - Teléfono: 1634964503
4. **Juan Rodríguez** (RUT: 22222222-2) - Teléfono: 1634964503

### 5 Tickets Creados:
1. **C01** - William - CAJA (posición 1, ~5 min)
2. **P01** - Johanna - PERSONAL_BANKER (posición 1, ~15 min)
3. **E01** - Natalia - EMPRESAS (posición 1, ~20 min)
4. **P02** - William (repite) - PERSONAL_BANKER (posición 2, ~30 min)
5. **C02** - Juan - CAJA (posición 2, ~10 min)

## Ejecución de Prueba

### Opción 1: Ejecutar todo de una vez
```bash
cd test-data
run-complete-test.bat
```

### Opción 2: Paso a paso
```bash
cd test-data

# 1. Crear clientes
create-clients.bat

# 2. Crear tickets
create-tickets.bat

# 3. Ver dashboard
curl http://localhost:8080/api/admin/dashboard
```

## Resultados Esperados

### En Telegram (Chat ID: 1634964503):
Deberías recibir **15 mensajes** en total:

**Mensajes inmediatos (5):**
- ✅ Ticket C01 creado, posición #1, 5 minutos
- ✅ Ticket P01 creado, posición #1, 15 minutos  
- ✅ Ticket E01 creado, posición #1, 20 minutos
- ✅ Ticket P02 creado, posición #2, 30 minutos
- ✅ Ticket C02 creado, posición #2, 10 minutos

**Mensajes programados (10):**
- ⏰ Pronto será tu turno (5 mensajes)
- 🔔 ¡ES TU TURNO! (5 mensajes)

### En Base de Datos:
- 4 registros en tabla `cliente`
- 5 registros en tabla `ticket`
- 15 registros en tabla `mensaje` (3 por ticket)
- 5 asesores en tabla `advisor`
- Múltiples registros en `audit_log`

### En API Dashboard:
```json
{
  "activeTickets": [...],
  "totalActiveTickets": 5,
  "availableAdvisors": [...],
  "totalAvailableAdvisors": 5
}
```

## Verificaciones

### 1. Verificar que la app esté corriendo:
```bash
curl http://localhost:8080/actuator/health
```

### 2. Ver logs en tiempo real:
```bash
docker-compose logs -f app
```

### 3. Consultar base de datos:
```bash
docker exec -it ticketero-postgres psql -U dev -d ticketero

-- Ver clientes
SELECT * FROM cliente;

-- Ver tickets
SELECT t.numero, c.nombre, t.queue_type, t.status, t.position_in_queue 
FROM ticket t JOIN cliente c ON t.cliente_id = c.id;

-- Ver mensajes
SELECT m.plantilla, m.estado_envio, t.numero 
FROM mensaje m JOIN ticket t ON m.ticket_id = t.id;
```

## Troubleshooting

### Si no llegan mensajes a Telegram:
1. Verificar que el bot token sea correcto
2. Verificar que el chat ID sea correcto
3. Revisar logs: `docker-compose logs app | grep -i telegram`

### Si falla la creación de tickets:
1. Verificar que los clientes existan primero
2. Revisar logs de errores: `docker-compose logs app | grep -i error`

### Si la aplicación no responde:
1. Verificar contenedores: `docker-compose ps`
2. Reiniciar: `docker-compose restart app`

---

**Fecha de creación:** 22 Diciembre 2025  
**Versión:** 1.0  
**Sistema:** Ticketero Digital con Telegram Bot