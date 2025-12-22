#!/bin/bash
# create-tickets.sh - Crear 5 tickets de prueba

API_URL="http://localhost:8080/api/tickets"

echo "🎫 Creando 5 tickets de prueba..."

# Ticket 1: William - CAJA
echo "🎫 Ticket 1: William - CAJA"
curl -X POST "$API_URL" \
  -H "Content-Type: application/json" \
  -d '{
    "clienteId": 1,
    "branchOffice": "Sucursal Centro",
    "queueType": "CAJA"
  }'
echo -e "\n"

sleep 2

# Ticket 2: Johanna - PERSONAL_BANKER
echo "🎫 Ticket 2: Johanna - PERSONAL_BANKER"
curl -X POST "$API_URL" \
  -H "Content-Type: application/json" \
  -d '{
    "clienteId": 2,
    "branchOffice": "Sucursal Centro",
    "queueType": "PERSONAL_BANKER"
  }'
echo -e "\n"

sleep 2

# Ticket 3: Natalia - EMPRESAS
echo "🎫 Ticket 3: Natalia - EMPRESAS"
curl -X POST "$API_URL" \
  -H "Content-Type: application/json" \
  -d '{
    "clienteId": 3,
    "branchOffice": "Sucursal Centro",
    "queueType": "EMPRESAS"
  }'
echo -e "\n"

sleep 2

# Ticket 4: William (repite) - PERSONAL_BANKER
echo "🎫 Ticket 4: William (repite) - PERSONAL_BANKER"
curl -X POST "$API_URL" \
  -H "Content-Type: application/json" \
  -d '{
    "clienteId": 1,
    "branchOffice": "Sucursal Centro",
    "queueType": "PERSONAL_BANKER"
  }'
echo -e "\n"

sleep 2

# Ticket 5: Juan - CAJA
echo "🎫 Ticket 5: Juan - CAJA"
curl -X POST "$API_URL" \
  -H "Content-Type: application/json" \
  -d '{
    "clienteId": 4,
    "branchOffice": "Sucursal Centro",
    "queueType": "CAJA"
  }'
echo -e "\n"

echo "✅ 5 tickets creados exitosamente"
echo "📱 Revisa tu Telegram para ver las notificaciones"