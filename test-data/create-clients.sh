#!/bin/bash
# create-clients.sh - Crear 4 clientes de prueba

API_URL="http://localhost:8080/api/tickets"

echo "🔄 Creando 4 clientes de prueba..."

# Cliente 1: William
echo "📝 Creando cliente: William"
curl -X POST "$API_URL/cliente" \
  -H "Content-Type: application/json" \
  -d '{
    "nationalId": "12345678-9",
    "nombre": "William",
    "apellido": "García",
    "telefono": "1634964503",
    "email": "william@test.com"
  }'
echo -e "\n"

# Cliente 2: Johanna
echo "📝 Creando cliente: Johanna"
curl -X POST "$API_URL/cliente" \
  -H "Content-Type: application/json" \
  -d '{
    "nationalId": "98765432-1",
    "nombre": "Johanna",
    "apellido": "López",
    "telefono": "1634964503",
    "email": "johanna@test.com"
  }'
echo -e "\n"

# Cliente 3: Natalia
echo "📝 Creando cliente: Natalia"
curl -X POST "$API_URL/cliente" \
  -H "Content-Type: application/json" \
  -d '{
    "nationalId": "11111111-1",
    "nombre": "Natalia",
    "apellido": "Martínez",
    "telefono": "1634964503",
    "email": "natalia@test.com"
  }'
echo -e "\n"

# Cliente 4: Juan
echo "📝 Creando cliente: Juan"
curl -X POST "$API_URL/cliente" \
  -H "Content-Type: application/json" \
  -d '{
    "nationalId": "22222222-2",
    "nombre": "Juan",
    "apellido": "Rodríguez",
    "telefono": "1634964503",
    "email": "juan@test.com"
  }'
echo -e "\n"

echo "✅ 4 clientes creados exitosamente"