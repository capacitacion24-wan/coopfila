# Tests E2E - CoopFila

## 📋 Resumen de Tests Implementados

### Total: **24 escenarios E2E** distribuidos en 5 features

| Feature | Tests | Prioridad | Descripción |
|---------|-------|-----------|-------------|
| **TicketCreationIT** | 6 | P0-P1 | Creación de tickets y clientes |
| **TicketProcessingIT** | 5 | P0-P1 | Procesamiento automático |
| **NotificationIT** | 4 | P0-P1 | Notificaciones Telegram |
| **ValidationIT** | 5 | P1 | Validaciones de entrada |
| **AdminDashboardIT** | 4 | P2 | Endpoints administrativos |

---

## 🚀 Ejecución de Tests

### Opción 1: Script Automático (Recomendado)
```bash
# Windows
run-e2e-tests.bat

# El script ejecuta:
# 1. Todos los tests *IT
# 2. Genera reporte HTML
# 3. Muestra resumen
```

### Opción 2: Maven Directo
```bash
# Ejecutar todos los tests E2E
mvn test -Dtest="*IT"

# Ejecutar feature específico
mvn test -Dtest="TicketCreationIT"
mvn test -Dtest="TicketProcessingIT"
mvn test -Dtest="NotificationIT"
mvn test -Dtest="ValidationIT"
mvn test -Dtest="AdminDashboardIT"

# Suite completa ordenada
mvn test -Dtest="E2ETestSuite"
```

### Opción 3: Tests Individuales
```bash
# Setup y validación
mvn test -Dtest="SetupValidationIT"

# Por escenario específico
mvn test -Dtest="TicketCreationIT#crearTicket_clienteExistente_debeCrear"
```

---

## 📊 Resultados Esperados

```
[INFO] Tests run: 24, Failures: 0, Errors: 0, Skipped: 0

Distribución por feature:
- TicketCreationIT: 6 tests ✅
- TicketProcessingIT: 5 tests ✅  
- NotificationIT: 4 tests ✅
- ValidationIT: 5 tests ✅
- AdminDashboardIT: 4 tests ✅
```

---

## 🔧 Configuración

### Prerrequisitos
- ✅ Docker Compose ejecutándose (`docker-compose up -d`)
- ✅ PostgreSQL disponible en puerto 5432
- ✅ WireMock configurado para Telegram (puerto 8089)

### Perfil de Test
- **Profile:** `test`
- **Base de datos:** PostgreSQL dockerizada
- **Limpieza:** Automática entre tests
- **Timeout:** 30s para procesamiento asíncrono

---

## 📈 Cobertura de Escenarios

### Happy Path (54% - 13 tests)
- ✅ Creación exitosa de tickets
- ✅ Procesamiento completo WAITING → COMPLETED
- ✅ Notificaciones automáticas
- ✅ Dashboard administrativo

### Edge Cases (25% - 6 tests)
- ✅ Formatos de números de ticket
- ✅ Consultas por código de referencia
- ✅ Asesores no disponibles
- ✅ Clientes sin teléfono

### Error Handling (21% - 5 tests)
- ✅ Validaciones de entrada (400)
- ✅ Recursos no encontrados (404)
- ✅ Duplicados (409)

---

## 🎯 Flujos E2E Validados

### 1. Flujo Completo de Ticket
```
Cliente → Ticket → Procesamiento → Notificación → Completado
```

### 2. Flujo de Validaciones
```
Input Inválido → Validación → Error HTTP → Mensaje Descriptivo
```

### 3. Flujo Administrativo
```
Dashboard → Estadísticas → Cambio Estados → Monitoreo
```

---

## 🐛 Troubleshooting

### Error: "Connection refused"
```bash
# Verificar que PostgreSQL esté ejecutándose
docker ps | grep postgres

# Reiniciar contenedores
docker-compose down && docker-compose up -d
```

### Error: "WireMock not found"
```bash
# Verificar puerto 8089 libre
netstat -an | findstr 8089

# WireMock se inicia automáticamente en tests
```

### Tests lentos
```bash
# Ejecutar con logs para debug
mvn test -Dtest="*IT" -X

# Verificar timeouts en Awaitility (30s por defecto)
```

---

## 📝 Reportes

### Reporte HTML
- **Ubicación:** `target/site/surefire-report.html`
- **Generación:** Automática con `mvn surefire-report:report`

### Logs de Test
- **Ubicación:** `target/surefire-reports/`
- **Formato:** XML + TXT por cada test class

---

## ✅ Checklist de Validación

Antes de ejecutar tests E2E:

- [ ] Docker Compose ejecutándose
- [ ] PostgreSQL accesible (puerto 5432)
- [ ] Aplicación compilada (`mvn compile`)
- [ ] Perfil `test` configurado
- [ ] Puerto 8089 libre para WireMock

Después de ejecutar:

- [ ] 24 tests ejecutados
- [ ] 0 failures
- [ ] Reporte HTML generado
- [ ] Logs sin errores críticos