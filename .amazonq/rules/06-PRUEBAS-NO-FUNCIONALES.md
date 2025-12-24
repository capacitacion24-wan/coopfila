# 06 - PRUEBAS NO FUNCIONALES - Performance, Concurrencia y Resiliencia

## OBJETIVO: VALIDAR REQUISITOS NO FUNCIONALES CRÍTICOS

Eres un Performance Engineer Senior experto en testing no funcional. Tu tarea es diseñar e implementar pruebas de performance, concurrencia y resiliencia para el sistema **CoopFila**, validando que cumple con los requisitos no funcionales críticos.

---

## CARACTERÍSTICAS DEL PROYECTO COOPFILA

**Stack Tecnológico:**
- API REST con Spring Boot 3.2, Java 21
- PostgreSQL 15 como única base de datos
- Telegram Bot API para notificaciones
- Docker + Docker Compose para infraestructura
- Sin RabbitMQ (procesamiento directo)
- Sin patrón Outbox (mensajes síncronos a Telegram)

**Arquitectura Simplificada:**
```
Cliente → API REST → PostgreSQL
                  ↓
              Telegram API
```

**IMPORTANTE:** Después de completar CADA paso, debes DETENERTE y solicitar una revisión exhaustiva antes de continuar.

---

## REQUISITOS NO FUNCIONALES A VALIDAR

| ID | Requisito | Métrica | Umbral |
|----|-----------|---------|--------|
| **RNF-01** | Throughput | Tickets procesados/minuto | ≥ 50 |
| **RNF-02** | Latencia API | p95 response time | < 2 segundos |
| **RNF-03** | Concurrencia | Race conditions | 0 detectadas |
| **RNF-04** | Consistencia | Tickets inconsistentes | 0 |
| **RNF-05** | Recovery Time | Detección fallo Telegram | < 30 segundos |
| **RNF-06** | Disponibilidad | Uptime durante carga | 99.5% |
| **RNF-07** | Recursos | Memory leak | 0 (estable 30 min) |

---

## METODOLOGÍA DE TRABAJO

**Principio:**
"Diseñar → Implementar → Ejecutar → Analizar → Confirmar → Continuar"

**Después de CADA paso:**
- ✅ Diseña los escenarios de prueba
- ✅ Implementa scripts/tests
- ✅ Ejecuta y captura métricas
- ✅ Analiza resultados vs umbrales
- ⏸️ **DETENTE** y solicita revisión
- ✅ Espera confirmación antes de continuar

**Formato de Solicitud de Revisión:**
```
✅ PASO X COMPLETADO

Escenarios ejecutados:
- [Lista de escenarios]

Métricas capturadas:
- Throughput: X tickets/min (umbral: ≥50)
- Latencia p95: Xms (umbral: <2000ms)
- Errores: X% (umbral: <1%)

🔍 SOLICITO REVISIÓN:
- ¿Los resultados son aceptables?
- ¿Hay ajustes necesarios?
- ¿Puedo continuar con el siguiente paso?

⏸️ ESPERANDO CONFIRMACIÓN...
```

---

## TU TAREA: 8 PASOS

| Paso | Descripción | Escenarios |
|------|-------------|------------|
| **PASO 1** | Setup de Herramientas + Scripts Base | 3 scripts |
| **PASO 2** | Performance - Load Test Sostenido | 3 escenarios |
| **PASO 3** | Concurrencia - Race Conditions | 3 escenarios |
| **PASO 4** | Resiliencia - Telegram Failures | 3 escenarios |
| **PASO 5** | Consistencia - Transacciones | 2 escenarios |
| **PASO 6** | Graceful Shutdown | 2 escenarios |
| **PASO 7** | Escalabilidad | 2 escenarios |
| **PASO 8** | Reporte Final y Dashboard | 1 reporte |

**Total:** ~18 escenarios | **Cobertura NFR:** 100%

---

## ESTRUCTURA DE ARCHIVOS A CREAR

```
coopfila/
├── scripts/
│   ├── performance/
│   │   ├── load-test.sh
│   │   ├── spike-test.sh
│   │   └── soak-test.sh
│   ├── concurrency/
│   │   ├── race-condition-test.sh
│   │   └── concurrent-creation-test.sh
│   ├── resilience/
│   │   ├── telegram-failure-test.sh
│   │   ├── database-failure-test.sh
│   │   └── recovery-test.sh
│   ├── chaos/
│   │   ├── kill-app.sh
│   │   └── network-delay.sh
│   └── utils/
│       ├── metrics-collector.sh
│       └── validate-consistency.sh
├── k6/
│   ├── load-test.js
│   ├── spike-test.js
│   └── stress-test.js
└── docs/
    └── NFR-TEST-RESULTS.md
```

---

## PASO 1: Setup de Herramientas + Scripts Base

**Objetivo:** Configurar herramientas de testing y scripts utilitarios.

### 1.1 metrics-collector.sh

```bash
#!/bin/bash

#=============================================================================
# COOPFILA - Metrics Collector
#=============================================================================
# Recolecta métricas del sistema durante pruebas de performance
# Usage: ./scripts/utils/metrics-collector.sh [duration_seconds] [output_file]
#=============================================================================

DURATION=${1:-60}
OUTPUT_FILE=${2:-"metrics-$(date +%Y%m%d-%H%M%S).csv"}

echo "timestamp,cpu_app,mem_app_mb,cpu_postgres,mem_postgres_mb,db_connections,tickets_waiting,tickets_completed,tickets_called,advisors_busy,advisors_available" > "$OUTPUT_FILE"

echo "📊 Collecting metrics for ${DURATION} seconds..."
echo "📁 Output: ${OUTPUT_FILE}"

START_TIME=$(date +%s)
END_TIME=$((START_TIME + DURATION))

while [ $(date +%s) -lt $END_TIME ]; do
    TIMESTAMP=$(date +%Y-%m-%d\ %H:%M:%S)
    
    # Container stats
    APP_STATS=$(docker stats ticketero-app --no-stream --format "{{.CPUPerc}},{{.MemUsage}}" 2>/dev/null | head -1)
    APP_CPU=$(echo "$APP_STATS" | cut -d',' -f1 | tr -d '%')
    APP_MEM=$(echo "$APP_STATS" | cut -d',' -f2 | cut -d'/' -f1 | tr -d 'MiB ')
    
    PG_STATS=$(docker stats ticketero-postgres --no-stream --format "{{.CPUPerc}},{{.MemUsage}}" 2>/dev/null | head -1)
    PG_CPU=$(echo "$PG_STATS" | cut -d',' -f1 | tr -d '%')
    PG_MEM=$(echo "$PG_STATS" | cut -d',' -f2 | cut -d'/' -f1 | tr -d 'MiB ')
    
    # Database metrics
    DB_CONNECTIONS=$(docker exec ticketero-postgres psql -U ticketero_user -d ticketero -t -c \
        "SELECT count(*) FROM pg_stat_activity WHERE datname='ticketero';" 2>/dev/null | xargs)
    
    # Ticket stats
    TICKETS_WAITING=$(docker exec ticketero-postgres psql -U ticketero_user -d ticketero -t -c \
        "SELECT COUNT(*) FROM ticket WHERE status='WAITING';" 2>/dev/null | xargs)
    TICKETS_COMPLETED=$(docker exec ticketero-postgres psql -U ticketero_user -d ticketero -t -c \
        "SELECT COUNT(*) FROM ticket WHERE status='COMPLETED';" 2>/dev/null | xargs)
    TICKETS_CALLED=$(docker exec ticketero-postgres psql -U ticketero_user -d ticketero -t -c \
        "SELECT COUNT(*) FROM ticket WHERE status='CALLED';" 2>/dev/null | xargs)
    
    # Advisor stats
    ADVISORS_BUSY=$(docker exec ticketero-postgres psql -U ticketero_user -d ticketero -t -c \
        "SELECT COUNT(*) FROM advisor WHERE status='BUSY';" 2>/dev/null | xargs)
    ADVISORS_AVAILABLE=$(docker exec ticketero-postgres psql -U ticketero_user -d ticketero -t -c \
        "SELECT COUNT(*) FROM advisor WHERE status='AVAILABLE';" 2>/dev/null | xargs)
    
    # Write to CSV
    echo "${TIMESTAMP},${APP_CPU:-0},${APP_MEM:-0},${PG_CPU:-0},${PG_MEM:-0},${DB_CONNECTIONS:-0},${TICKETS_WAITING:-0},${TICKETS_COMPLETED:-0},${TICKETS_CALLED:-0},${ADVISORS_BUSY:-0},${ADVISORS_AVAILABLE:-0}" >> "$OUTPUT_FILE"
    
    sleep 5
done

echo "✅ Metrics collection complete: ${OUTPUT_FILE}"
```

### 1.2 validate-consistency.sh

```bash
#!/bin/bash

#=============================================================================
# COOPFILA - Consistency Validator
#=============================================================================
# Valida consistencia del sistema después de pruebas de carga
# Usage: ./scripts/utils/validate-consistency.sh
#=============================================================================

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo "═══════════════════════════════════════════════════════════════"
echo " COOPFILA - VALIDACIÓN DE CONSISTENCIA"
echo "═══════════════════════════════════════════════════════════════"
echo ""

ERRORS=0

# 1. Tickets en estado inconsistente
echo -n "1. Tickets en estado inconsistente... "
INCONSISTENT=$(docker exec ticketero-postgres psql -U ticketero_user -d ticketero -t -c "
SELECT COUNT(*) FROM ticket t
WHERE (t.status = 'IN_PROGRESS' AND t.started_at IS NULL)
   OR (t.status = 'COMPLETED' AND t.completed_at IS NULL)
   OR (t.status = 'CALLED' AND t.assigned_advisor_id IS NULL);
" | xargs)

if [ "$INCONSISTENT" -eq 0 ]; then
    echo -e "${GREEN}PASS${NC} (0 encontrados)"
else
    echo -e "${RED}FAIL${NC} ($INCONSISTENT encontrados)"
    ERRORS=$((ERRORS + 1))
fi

# 2. Asesores en estado inconsistente
echo -n "2. Asesores BUSY sin ticket activo... "
BUSY_NO_TICKET=$(docker exec ticketero-postgres psql -U ticketero_user -d ticketero -t -c "
SELECT COUNT(*) FROM advisor a
WHERE a.status = 'BUSY'
  AND NOT EXISTS (
    SELECT 1 FROM ticket t
    WHERE t.assigned_advisor_id = a.id
      AND t.status IN ('CALLED', 'IN_PROGRESS')
  );
" | xargs)

if [ "$BUSY_NO_TICKET" -eq 0 ]; then
    echo -e "${GREEN}PASS${NC} (0 encontrados)"
else
    echo -e "${YELLOW}WARN${NC} ($BUSY_NO_TICKET encontrados)"
fi

# 3. Tickets duplicados (mismo nationalId + cola en estado activo)
echo -n "3. Tickets potencialmente duplicados... "
DUPLICATES=$(docker exec ticketero-postgres psql -U ticketero_user -d ticketero -t -c "
SELECT COUNT(*) FROM (
    SELECT national_id, queue_type, COUNT(*) as cnt
    FROM ticket
    WHERE status IN ('WAITING', 'CALLED', 'IN_PROGRESS')
    GROUP BY national_id, queue_type
    HAVING COUNT(*) > 1
) dups;
" | xargs)

if [ "$DUPLICATES" -eq 0 ]; then
    echo -e "${GREEN}PASS${NC} (0 duplicados)"
else
    echo -e "${YELLOW}WARN${NC} ($DUPLICATES posibles duplicados)"
fi

# 4. Mensajes Telegram sin enviar (status PENDIENTE por mucho tiempo)
echo -n "4. Mensajes Telegram pendientes (>5 min)... "
PENDING_MESSAGES=$(docker exec ticketero-postgres psql -U ticketero_user -d ticketero -t -c "
SELECT COUNT(*) FROM mensaje
WHERE estado_envio = 'PENDIENTE'
  AND fecha_programada < NOW() - INTERVAL '5 minutes';
" | xargs)

if [ "$PENDING_MESSAGES" -eq 0 ]; then
    echo -e "${GREEN}PASS${NC} (0 pendientes)"
else
    echo -e "${YELLOW}WARN${NC} ($PENDING_MESSAGES mensajes Telegram atrasados)"
fi

# 5. Mensajes Telegram fallidos
echo -n "5. Mensajes Telegram fallidos... "
FAILED_MESSAGES=$(docker exec ticketero-postgres psql -U ticketero_user -d ticketero -t -c "
SELECT COUNT(*) FROM mensaje WHERE estado_envio = 'FALLIDO';
" | xargs)

if [ "$FAILED_MESSAGES" -eq 0 ]; then
    echo -e "${GREEN}PASS${NC} (0 fallidos)"
else
    echo -e "${RED}FAIL${NC} ($FAILED_MESSAGES mensajes Telegram fallidos)"
    ERRORS=$((ERRORS + 1))
fi

# 6. Conexiones DB abiertas
echo -n "6. Conexiones PostgreSQL... "
DB_CONN=$(docker exec ticketero-postgres psql -U ticketero_user -d ticketero -t -c \
    "SELECT count(*) FROM pg_stat_activity WHERE datname='ticketero';" | xargs)

if [ "$DB_CONN" -lt 15 ]; then
    echo -e "${GREEN}OK${NC} ($DB_CONN conexiones)"
else
    echo -e "${YELLOW}WARN${NC} ($DB_CONN conexiones - revisar pool)"
fi

# 7. Integridad referencial
echo -n "7. Integridad referencial... "
ORPHAN_TICKETS=$(docker exec ticketero-postgres psql -U ticketero_user -d ticketero -t -c "
SELECT COUNT(*) FROM ticket t
WHERE t.assigned_advisor_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM advisor a WHERE a.id = t.assigned_advisor_id);
" | xargs)

if [ "$ORPHAN_TICKETS" -eq 0 ]; then
    echo -e "${GREEN}PASS${NC} (0 huérfanos)"
else
    echo -e "${RED}FAIL${NC} ($ORPHAN_TICKETS tickets huérfanos)"
    ERRORS=$((ERRORS + 1))
fi

echo ""
echo "═══════════════════════════════════════════════════════════════"
if [ $ERRORS -eq 0 ]; then
    echo -e " RESULTADO: ${GREEN}SISTEMA CONSISTENTE${NC}"
else
    echo -e " RESULTADO: ${RED}$ERRORS ERRORES DE CONSISTENCIA${NC}"
fi
echo "═══════════════════════════════════════════════════════════════"

exit $ERRORS
```

### 1.3 k6/load-test.js (K6 Script Base)

```javascript
// =============================================================================
// COOPFILA - K6 Load Test Base
// =============================================================================
// Usage: k6 run --vus 10 --duration 2m k6/load-test.js
// =============================================================================

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

// Custom metrics
const ticketsCreated = new Counter('tickets_created');
const ticketErrors = new Rate('ticket_errors');
const createLatency = new Trend('create_latency', true);

// Configuration
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const QUEUES = ['CAJA', 'PERSONAL', 'EMPRESAS', 'GERENCIA'];

// Test options (can be overridden via CLI)
export const options = {
    vus: 10,
    duration: '2m',
    thresholds: {
        http_req_duration: ['p(95)<2000'], // p95 < 2s
        ticket_errors: ['rate<0.01'], // < 1% errors
        tickets_created: ['count>50'], // > 50 tickets
    },
};

// Unique ID generator
function generateNationalId() {
    return Math.floor(10000000 + Math.random() * 90000000).toString();
}

function generatePhone() {
    return '+569' + Math.floor(10000000 + Math.random() * 90000000);
}

// Main test function
export default function () {
    const queue = QUEUES[Math.floor(Math.random() * QUEUES.length)];
    
    const payload = JSON.stringify({
        nationalId: generateNationalId(),
        telefono: generatePhone(),
        branchOffice: 'Sucursal Centro',
        queueType: queue,
    });
    
    const params = {
        headers: {
            'Content-Type': 'application/json',
        },
        tags: { name: 'CreateTicket' },
    };
    
    const startTime = Date.now();
    const response = http.post(`${BASE_URL}/api/tickets`, payload, params);
    const duration = Date.now() - startTime;
    
    // Record metrics
    createLatency.add(duration);
    
    const success = check(response, {
        'status is 201': (r) => r.status === 201,
        'has ticket number': (r) => r.json('numero') !== undefined,
        'has position': (r) => r.json('positionInQueue') > 0,
    });
    
    if (success) {
        ticketsCreated.add(1);
    } else {
        ticketErrors.add(1);
        console.log(`Error: ${response.status} - ${response.body}`);
    }
    
    // Think time between requests
    sleep(Math.random() * 2 + 1); // 1-3 seconds
}

// Summary handler
export function handleSummary(data) {
    return {
        'stdout': textSummary(data, { indent: ' ', enableColors: true }),
        'results/load-test-summary.json': JSON.stringify(data, null, 2),
    };
}

function textSummary(data, options) {
    const checks = data.metrics.checks;
    const duration = data.metrics.http_req_duration;
    
    return `
═══════════════════════════════════════════════════════════════
COOPFILA - LOAD TEST RESULTS
═══════════════════════════════════════════════════════════════

Total Requests: ${data.metrics.http_reqs.values.count}
Tickets Created: ${data.metrics.tickets_created?.values.count || 0}
Error Rate: ${(data.metrics.ticket_errors?.values.rate * 100 || 0).toFixed(2)}%

Latency:
  p50: ${duration.values['p(50)'].toFixed(0)}ms
  p95: ${duration.values['p(95)'].toFixed(0)}ms
  p99: ${duration.values['p(99)'].toFixed(0)}ms
  max: ${duration.values.max.toFixed(0)}ms

Throughput: ${(data.metrics.http_reqs.values.count / (data.state.testRunDurationMs / 1000 / 60)).toFixed(1)} req/min

═══════════════════════════════════════════════════════════════
`;
}
```

**Validaciones:**
```bash
chmod +x scripts/utils/*.sh
./scripts/utils/validate-consistency.sh
# All checks should pass
```

---

## 🔍 PUNTO DE REVISIÓN 1

✅ **PASO 1 COMPLETADO**

**Scripts creados:**
- `metrics-collector.sh`: Recolecta métricas cada 5s
- `validate-consistency.sh`: 6 validaciones de consistencia
- `k6/load-test.js`: Script base K6 con métricas custom

**Herramientas configuradas:**
- K6 para load testing
- Bash scripts para chaos testing
- CSV output para análisis

🔍 **SOLICITO REVISIÓN:**
- ¿Los scripts cubren las métricas necesarias?
- ¿Puedo continuar con PASO 2?

⏸️ **ESPERANDO CONFIRMACIÓN...**

---

## PASO 2: Performance - Load Test Sostenido

**Objetivo:** Validar throughput ≥50 tickets/min y latencia p95 <2s.

### Escenarios

**Test:** PERF-01 Load Test Sostenido  
**Category:** Performance  
**Priority:** P1

**Objetivo:** Validar throughput sostenido de 50+ tickets/minuto

**Setup:**
- Sistema limpio (DB sin tickets previos)
- 5 asesores AVAILABLE
- Telegram API disponible

**Execution:**
- 100 tickets en 2 minutos (distribución uniforme)
- 10 VUs concurrentes
- Think time: 1-3 segundos

**Success Criteria:**
- Throughput: ≥ 50 tickets/minuto
- Latencia p95: < 2000ms
- Error rate: < 1%
- Sin deadlocks en BD
- Mensajes Telegram enviados correctamente

### 2.1 load-test.sh

```bash
#!/bin/bash

#=============================================================================
# COOPFILA - Load Test Sostenido
#=============================================================================
# Ejecuta test de carga sostenida: 100 tickets en 2 minutos
# Usage: ./scripts/performance/load-test.sh
#=============================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

echo -e "${CYAN}╔══════════════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║                COOPFILA - LOAD TEST SOSTENIDO (PERF-01)     ║${NC}"
echo -e "${CYAN}╚══════════════════════════════════════════════════════════════╝${NC}"
echo ""

#=============================================================================
# 1. PRE-TEST CLEANUP
#=============================================================================
echo -e "${YELLOW}1. Limpiando estado previo...${NC}"

docker exec ticketero-postgres psql -U ticketero_user -d ticketero -c "
DELETE FROM ticket_event;
DELETE FROM mensaje;
DELETE FROM ticket;
UPDATE advisor SET status = 'AVAILABLE', total_tickets_served = 0;
" > /dev/null 2>&1

echo " ✓ Base de datos limpia"

#=============================================================================
# 2. CAPTURE BASELINE
#=============================================================================
echo -e "${YELLOW}2. Capturando baseline...${NC}"

ADVISORS_AVAILABLE=$(docker exec ticketero-postgres psql -U ticketero_user -d ticketero -t -c \
    "SELECT COUNT(*) FROM advisor WHERE status='AVAILABLE';" | xargs)
echo " ✓ Asesores disponibles: $ADVISORS_AVAILABLE"

#=============================================================================
# 3. START METRICS COLLECTION (background)
#=============================================================================
echo -e "${YELLOW}3. Iniciando recolección de métricas...${NC}"

METRICS_FILE="$PROJECT_ROOT/results/load-test-metrics-$(date +%Y%m%d-%H%M%S).csv"
mkdir -p "$PROJECT_ROOT/results"

"$SCRIPT_DIR/../utils/metrics-collector.sh" 150 "$METRICS_FILE" &
METRICS_PID=$!
echo " ✓ Métricas: $METRICS_FILE (PID: $METRICS_PID)"

#=============================================================================
# 4. EXECUTE LOAD TEST
#=============================================================================
echo -e "${YELLOW}4. Ejecutando load test (2 minutos)...${NC}"
echo ""

START_TIME=$(date +%s)

# Check if K6 is available
if command -v k6 &> /dev/null; then
    echo " Usando K6..."
    k6 run --vus 10 --duration 2m "$PROJECT_ROOT/k6/load-test.js" \
        --out json="$PROJECT_ROOT/results/load-test-k6.json" 2>&1 | tee "$PROJECT_ROOT/results/load-test-output.txt"
else
    echo " K6 no disponible, usando script bash..."
    
    TICKETS_TO_CREATE=100
    CREATED=0
    ERRORS=0
    
    for i in $(seq 1 $TICKETS_TO_CREATE); do
        QUEUE_INDEX=$((i % 4))
        QUEUES=("CAJA" "PERSONAL" "EMPRESAS" "GERENCIA")
        QUEUE=${QUEUES[$QUEUE_INDEX]}
        NATIONAL_ID="300000$(printf '%03d' $i)"
        
        RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "http://localhost:8080/api/tickets" \
            -H "Content-Type: application/json" \
            -d "{
                \"nationalId\": \"${NATIONAL_ID}\",
                \"telefono\": \"+5691234${i}\",
                \"branchOffice\": \"Sucursal Test\",
                \"queueType\": \"${QUEUE}\"
            }")
        
        HTTP_CODE=$(echo "$RESPONSE" | tail -1)
        
        if [ "$HTTP_CODE" = "201" ]; then
            CREATED=$((CREATED + 1))
            echo -ne "\r   Tickets creados: $CREATED/$TICKETS_TO_CREATE"
        else
            ERRORS=$((ERRORS + 1))
        fi
        
        # Rate limiting: ~50 tickets/min = 1 ticket/1.2s
        sleep 1.2
    done
    
    echo ""
    echo "   ✓ Creados: $CREATED, Errores: $ERRORS"
fi

END_TIME=$(date +%s)
DURATION=$((END_TIME - START_TIME))

#=============================================================================
# 5. WAIT FOR PROCESSING
#=============================================================================
echo -e "${YELLOW}5. Esperando procesamiento completo...${NC}"

MAX_WAIT=120
WAITED=0

while [ $WAITED -lt $MAX_WAIT ]; do
    WAITING=$(docker exec ticketero-postgres psql -U ticketero_user -d ticketero -t -c \
        "SELECT COUNT(*) FROM ticket WHERE status='WAITING';" | xargs)
    IN_PROGRESS=$(docker exec ticketero-postgres psql -U ticketero_user -d ticketero -t -c \
        "SELECT COUNT(*) FROM ticket WHERE status IN ('CALLED', 'IN_PROGRESS');" | xargs)
    
    if [ "$WAITING" -eq 0 ] && [ "$IN_PROGRESS" -eq 0 ]; then
        echo "   ✓ Todos los tickets procesados"
        break
    fi
    
    echo -ne "\r   Esperando... WAITING: $WAITING, IN_PROGRESS: $IN_PROGRESS    "
    sleep 5
    WAITED=$((WAITED + 5))
done

echo ""

#=============================================================================
# 6. STOP METRICS COLLECTION
#=============================================================================
kill $METRICS_PID 2>/dev/null || true
echo " ✓ Recolección de métricas detenida"

#=============================================================================
# 7. COLLECT RESULTS
#=============================================================================
echo -e "${YELLOW}6. Recolectando resultados...${NC}"

TOTAL_TICKETS=$(docker exec ticketero-postgres psql -U ticketero_user -d ticketero -t -c \
    "SELECT COUNT(*) FROM ticket;" | xargs)
COMPLETED_TICKETS=$(docker exec ticketero-postgres psql -U ticketero_user -d ticketero -t -c \
    "SELECT COUNT(*) FROM ticket WHERE status='COMPLETED';" | xargs)
FAILED_MESSAGES=$(docker exec ticketero-postgres psql -U ticketero_user -d ticketero -t -c \
    "SELECT COUNT(*) FROM mensaje WHERE estado_envio='FALLIDO';" | xargs)

THROUGHPUT=$(echo "scale=1; $COMPLETED_TICKETS * 60 / $DURATION" | bc)

#=============================================================================
# 8. VALIDATE CONSISTENCY
#=============================================================================
echo -e "${YELLOW}7. Validando consistencia...${NC}"
"$SCRIPT_DIR/../utils/validate-consistency.sh"
CONSISTENCY_RESULT=$?

#=============================================================================
# 9. PRINT RESULTS
#=============================================================================
echo ""
echo -e "${CYAN}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${CYAN}                    RESULTADOS LOAD TEST SOSTENIDO            ${NC}"
echo -e "${CYAN}═══════════════════════════════════════════════════════════════${NC}"
echo ""
echo " Duración: ${DURATION} segundos"
echo " Tickets creados: ${TOTAL_TICKETS}"
echo " Tickets completados: ${COMPLETED_TICKETS}"
echo " Mensajes Telegram fallidos: ${FAILED_MESSAGES}"
echo ""
echo " 📊 MÉTRICAS:"
echo " ─────────────────────────────────────────────"

# Throughput check
if (( $(echo "$THROUGHPUT >= 50" | bc -l) )); then
    echo -e " Throughput: ${GREEN}${THROUGHPUT} tickets/min${NC} (≥50 ✓)"
else
    echo -e " Throughput: ${RED}${THROUGHPUT} tickets/min${NC} (<50 ✗)"
fi

# Completion check
COMPLETION_RATE=$(echo "scale=1; $COMPLETED_TICKETS * 100 / $TOTAL_TICKETS" | bc)
if (( $(echo "$COMPLETION_RATE >= 99" | bc -l) )); then
    echo -e " Completion rate: ${GREEN}${COMPLETION_RATE}%${NC} (≥99% ✓)"
else
    echo -e " Completion rate: ${RED}${COMPLETION_RATE}%${NC} (<99% ✗)"
fi

# Consistency check
if [ $CONSISTENCY_RESULT -eq 0 ]; then
    echo -e " Consistencia: ${GREEN}PASS${NC}"
else
    echo -e " Consistencia: ${RED}FAIL${NC}"
fi

echo ""
echo " 📁 Archivos generados:"
echo " - $METRICS_FILE"
echo ""
echo -e "${CYAN}═══════════════════════════════════════════════════════════════${NC}"

# Exit code based on results
if (( $(echo "$THROUGHPUT >= 50" | bc -l) )) && [ $CONSISTENCY_RESULT -eq 0 ]; then
    echo -e "${GREEN}✅ LOAD TEST PASSED${NC}"
    exit 0
else
    echo -e "${RED}❌ LOAD TEST FAILED${NC}"
    exit 1
fi
```

### 2.2 spike-test.sh

```bash
#!/bin/bash

#=============================================================================
# COOPFILA - Spike Test
#=============================================================================
# Ejecuta test de spike: 50 tickets simultáneos en 10 segundos
# Usage: ./scripts/performance/spike-test.sh
#=============================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

echo -e "${CYAN}╔══════════════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║                    COOPFILA - SPIKE TEST (PERF-02)          ║${NC}"
echo -e "${CYAN}╚══════════════════════════════════════════════════════════════╝${NC}"
echo ""

# Cleanup
echo -e "${YELLOW}1. Limpiando estado previo...${NC}"
docker exec ticketero-postgres psql -U ticketero_user -d ticketero -c "
DELETE FROM ticket_event;
DELETE FROM mensaje;
DELETE FROM ticket;
UPDATE advisor SET status = 'AVAILABLE', total_tickets_served = 0;
" > /dev/null 2>&1

# Start metrics
METRICS_FILE="$PROJECT_ROOT/results/spike-test-metrics-$(date +%Y%m%d-%H%M%S).csv"
mkdir -p "$PROJECT_ROOT/results"
"$SCRIPT_DIR/../utils/metrics-collector.sh" 120 "$METRICS_FILE" &
METRICS_PID=$!

# Execute spike
echo -e "${YELLOW}2. Ejecutando spike (50 tickets en 10 segundos)...${NC}"
START_TIME=$(date +%s)

# Crear 50 tickets en paralelo
for i in $(seq 1 50); do
    (
        QUEUE_INDEX=$((i % 4))
        QUEUES=("CAJA" "PERSONAL" "EMPRESAS" "GERENCIA")
        QUEUE=${QUEUES[$QUEUE_INDEX]}
        
        curl -s -X POST "http://localhost:8080/api/tickets" \
            -H "Content-Type: application/json" \
            -d "{
                \"nationalId\": \"400000$(printf '%03d' $i)\",
                \"telefono\": \"+5691234${i}\",
                \"branchOffice\": \"Sucursal Test\",
                \"queueType\": \"${QUEUE}\"
            }" > /dev/null
    ) &
done

wait
SPIKE_END=$(date +%s)
SPIKE_DURATION=$((SPIKE_END - START_TIME))
echo " ✓ Spike completado en ${SPIKE_DURATION} segundos"

# Wait for processing
echo -e "${YELLOW}3. Esperando procesamiento...${NC}"
MAX_WAIT=180
WAITED=0

while [ $WAITED -lt $MAX_WAIT ]; do
    COMPLETED=$(docker exec ticketero-postgres psql -U ticketero_user -d ticketero -t -c \
        "SELECT COUNT(*) FROM ticket WHERE status='COMPLETED';" | xargs)
    
    if [ "$COMPLETED" -ge 50 ]; then
        PROCESS_END=$(date +%s)
        TOTAL_PROCESS_TIME=$((PROCESS_END - START_TIME))
        echo "   ✓ Todos procesados en ${TOTAL_PROCESS_TIME} segundos"
        break
    fi
    
    echo -ne "\r   Completados: $COMPLETED/50    "
    sleep 5
    WAITED=$((WAITED + 5))
done

# Stop metrics
kill $METRICS_PID 2>/dev/null || true

# Results
echo ""
echo -e "${CYAN}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${CYAN}                        RESULTADOS SPIKE TEST                  ${NC}"
echo -e "${CYAN}═══════════════════════════════════════════════════════════════${NC}"
echo ""
echo " Tickets creados: 50 en ${SPIKE_DURATION}s"
echo " Tiempo procesamiento: ${TOTAL_PROCESS_TIME:-timeout}s"
echo ""

# Validate
"$SCRIPT_DIR/../utils/validate-consistency.sh"

if [ "${TOTAL_PROCESS_TIME:-999}" -lt 180 ]; then
    echo -e "${GREEN}✅ SPIKE TEST PASSED${NC}"
else
    echo -e "${RED}❌ SPIKE TEST FAILED (timeout)${NC}"
    exit 1
fi
```

### 2.3 soak-test.sh

```bash
#!/bin/bash

#=============================================================================
# COOPFILA - Soak Test (30 minutos)
#=============================================================================
# Carga constante de 30 tickets/minuto durante 30 minutos
# Detecta memory leaks y degradación progresiva
# Usage: ./scripts/performance/soak-test.sh [duration_minutes]
#=============================================================================

DURATION_MIN=${1:-30}
TICKETS_PER_MIN=30

echo "╔══════════════════════════════════════════════════════════════╗"
echo "║                    COOPFILA - SOAK TEST (PERF-03)           ║"
echo "╚══════════════════════════════════════════════════════════════╝"
echo ""
echo " Duración: ${DURATION_MIN} minutos"
echo " Carga: ${TICKETS_PER_MIN} tickets/minuto"
echo ""

# Start metrics with longer duration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
METRICS_FILE="$PROJECT_ROOT/results/soak-test-metrics-$(date +%Y%m%d-%H%M%S).csv"
mkdir -p "$PROJECT_ROOT/results"

DURATION_SEC=$((DURATION_MIN * 60 + 120))
"$SCRIPT_DIR/../utils/metrics-collector.sh" $DURATION_SEC "$METRICS_FILE" &
METRICS_PID=$!

# Capture initial memory
INITIAL_MEM=$(docker stats ticketero-app --no-stream --format "{{.MemUsage}}" | cut -d'/' -f1 | tr -d 'MiB ')

echo " Memoria inicial: ${INITIAL_MEM}MB"
echo ""

# Execute soak test
START_TIME=$(date +%s)
END_TIME=$((START_TIME + DURATION_MIN * 60))
TICKET_COUNTER=0
INTERVAL=$(echo "scale=2; 60 / $TICKETS_PER_MIN" | bc)

while [ $(date +%s) -lt $END_TIME ]; do
    TICKET_COUNTER=$((TICKET_COUNTER + 1))
    QUEUE_INDEX=$((TICKET_COUNTER % 4))
    QUEUES=("CAJA" "PERSONAL" "EMPRESAS" "GERENCIA")
    QUEUE=${QUEUES[$QUEUE_INDEX]}
    
    curl -s -X POST "http://localhost:8080/api/tickets" \
        -H "Content-Type: application/json" \
        -d "{
            \"nationalId\": \"500$(printf '%06d' $TICKET_COUNTER)\",
            \"telefono\": \"+56912345678\",
            \"branchOffice\": \"Sucursal Test\",
            \"queueType\": \"${QUEUE}\"
        }" > /dev/null &
    
    ELAPSED=$(( ($(date +%s) - START_TIME) / 60 ))
    CURRENT_MEM=$(docker stats ticketero-app --no-stream --format "{{.MemUsage}}" 2>/dev/null | cut -d'/' -f1 | tr -d 'MiB ')
    
    echo -ne "\r  Minuto ${ELAPSED}/${DURATION_MIN} | Tickets: ${TICKET_COUNTER} | Memoria: ${CURRENT_MEM:-?}MB    "
    
    sleep $INTERVAL
done

wait

# Stop metrics
kill $METRICS_PID 2>/dev/null || true

# Final memory
FINAL_MEM=$(docker stats ticketero-app --no-stream --format "{{.MemUsage}}" | cut -d'/' -f1 | tr -d 'MiB ')

echo ""
echo ""
echo "═══════════════════════════════════════════════════════════════"
echo "                        RESULTADOS SOAK TEST                  "
echo "═══════════════════════════════════════════════════════════════"
echo ""
echo " Duración: ${DURATION_MIN} minutos"
echo " Tickets creados: ${TICKET_COUNTER}"
echo " Memoria inicial: ${INITIAL_MEM}MB"
echo " Memoria final: ${FINAL_MEM}MB"

# Check for memory leak
MEM_DIFF=$(echo "$FINAL_MEM - $INITIAL_MEM" | bc)
MEM_INCREASE_PCT=$(echo "scale=1; $MEM_DIFF * 100 / $INITIAL_MEM" | bc)

if (( $(echo "$MEM_INCREASE_PCT < 20" | bc -l) )); then
    echo -e " Memory leak: \033[0;32mNO DETECTADO\033[0m (+${MEM_INCREASE_PCT}%)"
else
    echo -e " Memory leak: \033[0;31mPOSIBLE\033[0m (+${MEM_INCREASE_PCT}%)"
fi

echo ""
"$SCRIPT_DIR/../utils/validate-consistency.sh"
```

---

## 🔍 PUNTO DE REVISIÓN 2

✅ **PASO 2 COMPLETADO**

**Escenarios ejecutados:**
- PERF-01: Load Test Sostenido (100 tickets en 2 min)
- PERF-02: Spike Test (50 tickets simultáneos)
- PERF-03: Soak Test (30 min de carga constante)

**Métricas capturadas:**
- Throughput: X tickets/min (umbral: ≥50)
- Latencia p95: Xms (umbral: <2000ms)
- Memory leak: X% incremento (umbral: <20%)

⏸️ **ESPERANDO CONFIRMACIÓN...**

---

## PASO 4: Resiliencia - Telegram Failures

**Objetivo:** Validar comportamiento ante fallos de Telegram API.

### Escenarios

**Test:** RES-01 Telegram API Failure  
**Category:** Resiliency  
**Priority:** P0

**Objetivo:** Validar que el sistema maneja fallos de Telegram correctamente

**Setup:**
- Sistema funcionando normalmente
- Telegram API disponible inicialmente
- Mensajes programados pendientes

**Execution:**
- Simular fallo de Telegram API (network/timeout)
- Crear tickets durante el fallo
- Verificar reintentos automáticos
- Restaurar conectividad

**Success Criteria:**
- Mensajes se marcan como FALLIDO después de 3 intentos
- Backoff exponencial funciona (30s, 60s, 120s)
- Al restaurar conectividad, mensajes pendientes se envían
- No se pierden mensajes
- Sistema sigue creando tickets normalmente

### 4.1 telegram-failure-test.sh

```bash
#!/bin/bash

#=============================================================================
# COOPFILA - Telegram Failure Test
#=============================================================================
# Simula fallo de Telegram API y valida reintentos
# Usage: ./scripts/resilience/telegram-failure-test.sh
#=============================================================================

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

echo -e "${CYAN}╔══════════════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║                COOPFILA - TELEGRAM FAILURE TEST (RES-01)    ║${NC}"
echo -e "${CYAN}╚══════════════════════════════════════════════════════════════╝${NC}"
echo ""

# Setup
echo -e "${YELLOW}1. Limpiando estado previo...${NC}"
docker exec ticketero-postgres psql -U ticketero_user -d ticketero -c "
DELETE FROM ticket_event;
DELETE FROM mensaje;
DELETE FROM ticket;
UPDATE advisor SET status = 'AVAILABLE';
" > /dev/null 2>&1

# Crear ticket normal (Telegram funcionando)
echo -e "${YELLOW}2. Creando ticket con Telegram funcionando...${NC}"
curl -s -X POST "http://localhost:8080/api/tickets" \
    -H "Content-Type: application/json" \
    -d '{
        "nationalId": "12345678",
        "telefono": "+56912345678",
        "branchOffice": "Sucursal Test",
        "queueType": "CAJA"
    }' > /dev/null

sleep 5

# Verificar mensaje enviado
SENT_BEFORE=$(docker exec ticketero-postgres psql -U ticketero_user -d ticketero -t -c \
    "SELECT COUNT(*) FROM mensaje WHERE estado_envio='ENVIADO';" | xargs)
echo " ✓ Mensajes enviados antes del fallo: $SENT_BEFORE"

# Simular fallo de Telegram (bloquear tráfico)
echo -e "${YELLOW}3. Simulando fallo de Telegram API...${NC}"
# Bloquear acceso a api.telegram.org
docker exec ticketero-app sh -c "echo '127.0.0.1 api.telegram.org' >> /etc/hosts" 2>/dev/null || true
echo " ✓ Telegram API bloqueada"

# Crear tickets durante el fallo
echo -e "${YELLOW}4. Creando tickets durante fallo de Telegram...${NC}"
for i in $(seq 1 3); do
    curl -s -X POST "http://localhost:8080/api/tickets" \
        -H "Content-Type: application/json" \
        -d "{
            \"nationalId\": \"1234567$i\",
            \"telefono\": \"+5691234567$i\",
            \"branchOffice\": \"Sucursal Test\",
            \"queueType\": \"CAJA\"
        }" > /dev/null
    echo -ne "\r   Tickets creados: $i/3"
done
echo ""

# Esperar intentos de reenvío
echo -e "${YELLOW}5. Esperando reintentos (3 minutos)...${NC}"
sleep 180

# Verificar estado de mensajes
PENDING=$(docker exec ticketero-postgres psql -U ticketero_user -d ticketero -t -c \
    "SELECT COUNT(*) FROM mensaje WHERE estado_envio='PENDIENTE';" | xargs)
FAILED=$(docker exec ticketero-postgres psql -U ticketero_user -d ticketero -t -c \
    "SELECT COUNT(*) FROM mensaje WHERE estado_envio='FALLIDO';" | xargs)

echo " Estado durante fallo:"
echo " - Mensajes PENDIENTE: $PENDING"
echo " - Mensajes FALLIDO: $FAILED"

# Restaurar conectividad
echo -e "${YELLOW}6. Restaurando conectividad a Telegram...${NC}"
docker exec ticketero-app sh -c "sed -i '/api.telegram.org/d' /etc/hosts" 2>/dev/null || true
echo " ✓ Telegram API restaurada"

# Esperar que se envíen mensajes pendientes
echo -e "${YELLOW}7. Esperando envío de mensajes pendientes (2 min)...${NC}"
sleep 120

# Verificar resultados finales
SENT_AFTER=$(docker exec ticketero-postgres psql -U ticketero_user -d ticketero -t -c \
    "SELECT COUNT(*) FROM mensaje WHERE estado_envio='ENVIADO';" | xargs)
FINAL_PENDING=$(docker exec ticketero-postgres psql -U ticketero_user -d ticketero -t -c \
    "SELECT COUNT(*) FROM mensaje WHERE estado_envio='PENDIENTE';" | xargs)
FINAL_FAILED=$(docker exec ticketero-postgres psql -U ticketero_user -d ticketero -t -c \
    "SELECT COUNT(*) FROM mensaje WHERE estado_envio='FALLIDO';" | xargs)

echo ""
echo -e "${CYAN}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${CYAN}                    RESULTADOS TELEGRAM FAILURE TEST          ${NC}"
echo -e "${CYAN}═══════════════════════════════════════════════════════════════${NC}"
echo ""
echo " Estado final:"
echo " - Mensajes ENVIADO: $SENT_AFTER"
echo " - Mensajes PENDIENTE: $FINAL_PENDING"
echo " - Mensajes FALLIDO: $FINAL_FAILED"
echo ""

PASS=true

# Validaciones
if [ "$SENT_AFTER" -gt "$SENT_BEFORE" ]; then
    echo -e " - Mensajes recuperados: ${GREEN}PASS${NC} (+$(($SENT_AFTER - $SENT_BEFORE)))"
else
    echo -e " - Mensajes recuperados: ${RED}FAIL${NC}"
    PASS=false
fi

if [ "$FINAL_PENDING" -eq 0 ]; then
    echo -e " - Sin mensajes pendientes: ${GREEN}PASS${NC}"
else
    echo -e " - Sin mensajes pendientes: ${YELLOW}WARN${NC} ($FINAL_PENDING)"
fi

if [ "$FINAL_FAILED" -le 3 ]; then
    echo -e " - Mensajes fallidos controlados: ${GREEN}PASS${NC} ($FINAL_FAILED)"
else
    echo -e " - Mensajes fallidos controlados: ${RED}FAIL${NC} ($FINAL_FAILED)"
    PASS=false
fi

echo ""
echo -e "${CYAN}═══════════════════════════════════════════════════════════════${NC}"

if [ "$PASS" = true ]; then
    echo -e "${GREEN}✅ TELEGRAM FAILURE TEST PASSED${NC}"
    exit 0
else
    echo -e "${RED}❌ TELEGRAM FAILURE TEST FAILED${NC}"
    exit 1
fi
```

---

## 🔍 PUNTO DE REVISIÓN 4

✅ **PASO 4 COMPLETADO**

**Escenarios ejecutados:**
- RES-01: Telegram API Failure (bloqueo + recovery)

**Métricas capturadas:**
- Recovery time: Xs (umbral: <30s)
- Mensajes perdidos: 0 (umbral: 0)
- Reintentos funcionando: ✓

🔍 **SOLICITO REVISIÓN:**
- ¿Los escenarios de resiliencia son suficientes?
- ¿Puedo continuar con PASO 5 (Consistencia)?

---

**Versión:** 1.0  
**Proyecto:** CoopFila  
**Stack:** Spring Boot 3.2 + PostgreSQL 15 + Telegram API  
**Enfoque:** Pruebas no funcionales adaptadas para arquitectura simplificada