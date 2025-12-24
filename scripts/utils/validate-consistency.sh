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
INCONSISTENT=$(docker exec ticketero-postgres-dev psql -U ticketero_user -d ticketero -t -c "
SELECT COUNT(*) FROM ticket t
WHERE (t.status = 'ATENDIENDO' AND t.assigned_advisor_id IS NULL);
" | xargs)

if [ "$INCONSISTENT" -eq 0 ]; then
    echo -e "${GREEN}PASS${NC} (0 encontrados)"
else
    echo -e "${RED}FAIL${NC} ($INCONSISTENT encontrados)"
    ERRORS=$((ERRORS + 1))
fi

# 2. Asesores en estado inconsistente
echo -n "2. Asesores BUSY sin ticket activo... "
BUSY_NO_TICKET=$(docker exec ticketero-postgres-dev psql -U ticketero_user -d ticketero -t -c "
SELECT COUNT(*) FROM advisor a
WHERE a.status = 'BUSY'
  AND NOT EXISTS (
    SELECT 1 FROM ticket t
    WHERE t.assigned_advisor_id = a.id
      AND t.status IN ('ATENDIENDO')
  );
" | xargs)

if [ "$BUSY_NO_TICKET" -eq 0 ]; then
    echo -e "${GREEN}PASS${NC} (0 encontrados)"
else
    echo -e "${YELLOW}WARN${NC} ($BUSY_NO_TICKET encontrados)"
fi

# 3. Tickets duplicados (mismo nationalId + cola en estado activo)
echo -n "3. Tickets potencialmente duplicados... "
DUPLICATES=$(docker exec ticketero-postgres-dev psql -U ticketero_user -d ticketero -t -c "
SELECT COUNT(*) FROM (
    SELECT national_id, queue_type, COUNT(*) as cnt
    FROM ticket
    WHERE status IN ('EN_ESPERA', 'PROXIMO', 'ATENDIENDO')
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
PENDING_MESSAGES=$(docker exec ticketero-postgres-dev psql -U ticketero_user -d ticketero -t -c "
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
FAILED_MESSAGES=$(docker exec ticketero-postgres-dev psql -U ticketero_user -d ticketero -t -c "
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
DB_CONN=$(docker exec ticketero-postgres-dev psql -U ticketero_user -d ticketero -t -c \
    "SELECT count(*) FROM pg_stat_activity WHERE datname='ticketero';" | xargs)

if [ "$DB_CONN" -lt 15 ]; then
    echo -e "${GREEN}OK${NC} ($DB_CONN conexiones)"
else
    echo -e "${YELLOW}WARN${NC} ($DB_CONN conexiones - revisar pool)"
fi

# 7. Integridad referencial
echo -n "7. Integridad referencial... "
ORPHAN_TICKETS=$(docker exec ticketero-postgres-dev psql -U ticketero_user -d ticketero -t -c "
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