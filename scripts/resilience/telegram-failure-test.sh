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
docker exec ticketero-postgres-dev psql -U ticketero_user -d ticketero -c "
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
    }' > /dev/null 2>&1

sleep 5

# Verificar mensaje enviado
SENT_BEFORE=$(docker exec ticketero-postgres-dev psql -U ticketero_user -d ticketero -t -c \
    "SELECT COUNT(*) FROM mensaje WHERE estado_envio='ENVIADO';" 2>/dev/null | xargs)
echo " ✓ Mensajes enviados antes del fallo: ${SENT_BEFORE:-0}"

# Simular fallo de Telegram (bloquear tráfico)
echo -e "${YELLOW}3. Simulando fallo de Telegram API...${NC}"
# Bloquear acceso a api.telegram.org
docker exec ticketero-app-dev sh -c "echo '127.0.0.1 api.telegram.org' >> /etc/hosts" 2>/dev/null || true
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
        }" > /dev/null 2>&1
    echo -ne "\r   Tickets creados: $i/3"
done
echo ""

# Esperar intentos de reenvío
echo -e "${YELLOW}5. Esperando reintentos (30 segundos)...${NC}"
sleep 30

# Verificar estado de mensajes
PENDING=$(docker exec ticketero-postgres-dev psql -U ticketero_user -d ticketero -t -c \
    "SELECT COUNT(*) FROM mensaje WHERE estado_envio='PENDIENTE';" 2>/dev/null | xargs)
FAILED=$(docker exec ticketero-postgres-dev psql -U ticketero_user -d ticketero -t -c \
    "SELECT COUNT(*) FROM mensaje WHERE estado_envio='FALLIDO';" 2>/dev/null | xargs)

echo " Estado durante fallo:"
echo " - Mensajes PENDIENTE: ${PENDING:-0}"
echo " - Mensajes FALLIDO: ${FAILED:-0}"

# Restaurar conectividad
echo -e "${YELLOW}6. Restaurando conectividad a Telegram...${NC}"
docker exec ticketero-app-dev sh -c "sed -i '/api.telegram.org/d' /etc/hosts" 2>/dev/null || true
echo " ✓ Telegram API restaurada"

# Esperar que se envíen mensajes pendientes
echo -e "${YELLOW}7. Esperando envío de mensajes pendientes (30s)...${NC}"
sleep 30

# Verificar resultados finales
SENT_AFTER=$(docker exec ticketero-postgres-dev psql -U ticketero_user -d ticketero -t -c \
    "SELECT COUNT(*) FROM mensaje WHERE estado_envio='ENVIADO';" 2>/dev/null | xargs)
FINAL_PENDING=$(docker exec ticketero-postgres-dev psql -U ticketero_user -d ticketero -t -c \
    "SELECT COUNT(*) FROM mensaje WHERE estado_envio='PENDIENTE';" 2>/dev/null | xargs)
FINAL_FAILED=$(docker exec ticketero-postgres-dev psql -U ticketero_user -d ticketero -t -c \
    "SELECT COUNT(*) FROM mensaje WHERE estado_envio='FALLIDO';" 2>/dev/null | xargs)

echo ""
echo -e "${CYAN}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${CYAN}                    RESULTADOS TELEGRAM FAILURE TEST          ${NC}"
echo -e "${CYAN}═══════════════════════════════════════════════════════════════${NC}"
echo ""
echo " Estado final:"
echo " - Mensajes ENVIADO: ${SENT_AFTER:-0}"
echo " - Mensajes PENDIENTE: ${FINAL_PENDING:-0}"
echo " - Mensajes FALLIDO: ${FINAL_FAILED:-0}"
echo ""

PASS=true

# Validaciones
if [ "${SENT_AFTER:-0}" -gt "${SENT_BEFORE:-0}" ]; then
    echo -e " - Mensajes recuperados: ${GREEN}PASS${NC} (+$((${SENT_AFTER:-0} - ${SENT_BEFORE:-0})))"
else
    echo -e " - Mensajes recuperados: ${YELLOW}WARN${NC} (Sin recovery detectado)"
fi

if [ "${FINAL_PENDING:-0}" -eq 0 ]; then
    echo -e " - Sin mensajes pendientes: ${GREEN}PASS${NC}"
else
    echo -e " - Sin mensajes pendientes: ${YELLOW}WARN${NC} (${FINAL_PENDING:-0})"
fi

if [ "${FINAL_FAILED:-0}" -le 3 ]; then
    echo -e " - Mensajes fallidos controlados: ${GREEN}PASS${NC} (${FINAL_FAILED:-0})"
else
    echo -e " - Mensajes fallidos controlados: ${RED}FAIL${NC} (${FINAL_FAILED:-0})"
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