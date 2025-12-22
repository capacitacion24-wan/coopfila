@echo off
REM create-tickets.bat - Crear 5 tickets de prueba

set API_URL=http://localhost:8080/api/tickets

echo 🎫 Creando 5 tickets de prueba...

REM Ticket 1: William - CAJA
echo 🎫 Ticket 1: William - CAJA
curl -X POST "%API_URL%" ^
  -H "Content-Type: application/json" ^
  -d "{\"clienteId\": 1, \"branchOffice\": \"Sucursal Centro\", \"queueType\": \"CAJA\"}"
echo.
timeout /t 2 /nobreak >nul

REM Ticket 2: Johanna - PERSONAL_BANKER
echo 🎫 Ticket 2: Johanna - PERSONAL_BANKER
curl -X POST "%API_URL%" ^
  -H "Content-Type: application/json" ^
  -d "{\"clienteId\": 2, \"branchOffice\": \"Sucursal Centro\", \"queueType\": \"PERSONAL_BANKER\"}"
echo.
timeout /t 2 /nobreak >nul

REM Ticket 3: Natalia - EMPRESAS
echo 🎫 Ticket 3: Natalia - EMPRESAS
curl -X POST "%API_URL%" ^
  -H "Content-Type: application/json" ^
  -d "{\"clienteId\": 3, \"branchOffice\": \"Sucursal Centro\", \"queueType\": \"EMPRESAS\"}"
echo.
timeout /t 2 /nobreak >nul

REM Ticket 4: William (repite) - PERSONAL_BANKER
echo 🎫 Ticket 4: William (repite) - PERSONAL_BANKER
curl -X POST "%API_URL%" ^
  -H "Content-Type: application/json" ^
  -d "{\"clienteId\": 1, \"branchOffice\": \"Sucursal Centro\", \"queueType\": \"PERSONAL_BANKER\"}"
echo.
timeout /t 2 /nobreak >nul

REM Ticket 5: Juan - CAJA
echo 🎫 Ticket 5: Juan - CAJA
curl -X POST "%API_URL%" ^
  -H "Content-Type: application/json" ^
  -d "{\"clienteId\": 4, \"branchOffice\": \"Sucursal Centro\", \"queueType\": \"CAJA\"}"
echo.

echo ✅ 5 tickets creados exitosamente
echo 📱 Revisa tu Telegram para ver las notificaciones
pause