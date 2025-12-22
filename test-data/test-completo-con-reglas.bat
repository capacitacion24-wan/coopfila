@echo off
REM test-completo-con-reglas.bat - Prueba que respeta reglas de negocio

set API_URL=http://localhost:8080/api

echo ════════════════════════════════════════════════════════
echo    PRUEBA COMPLETA CON REGLAS DE NEGOCIO
echo ════════════════════════════════════════════════════════
echo.
echo 📋 Escenario:
echo   1. William crea ticket CAJA (será atendido primero)
echo   2. Johanna crea ticket PERSONAL_BANKER
echo   3. Natalia crea ticket EMPRESAS  
echo   4. Juan crea ticket CAJA (será segundo en CAJA)
echo   5. William completa su ticket y crea otro PERSONAL_BANKER
echo.
echo 🔍 Verificando aplicación...

curl -s http://localhost:8080/actuator/health >nul
if %errorlevel% neq 0 (
    echo ❌ ERROR: Aplicación no está corriendo
    pause
    exit /b 1
)
echo ✅ Aplicación OK
echo.

echo 👥 PASO 1: Creando clientes...
call create-clients.bat
echo.

echo 🎫 PASO 2: Creando tickets iniciales (respetando reglas)...
echo ────────────────────────────────────────────────────────

echo 🎫 Ticket 1: William - CAJA
curl -X POST "%API_URL%/tickets" ^
  -H "Content-Type: application/json" ^
  -d "{\"clienteId\": 1, \"branchOffice\": \"Sucursal Centro\", \"queueType\": \"CAJA\"}"
echo.
timeout /t 3 /nobreak >nul

echo 🎫 Ticket 2: Johanna - PERSONAL_BANKER  
curl -X POST "%API_URL%/tickets" ^
  -H "Content-Type: application/json" ^
  -d "{\"clienteId\": 2, \"branchOffice\": \"Sucursal Centro\", \"queueType\": \"PERSONAL_BANKER\"}"
echo.
timeout /t 3 /nobreak >nul

echo 🎫 Ticket 3: Natalia - EMPRESAS
curl -X POST "%API_URL%/tickets" ^
  -H "Content-Type: application/json" ^
  -d "{\"clienteId\": 3, \"branchOffice\": \"Sucursal Centro\", \"queueType\": \"EMPRESAS\"}"
echo.
timeout /t 3 /nobreak >nul

echo 🎫 Ticket 4: Juan - CAJA (segundo en cola CAJA)
curl -X POST "%API_URL%/tickets" ^
  -H "Content-Type: application/json" ^
  -d "{\"clienteId\": 4, \"branchOffice\": \"Sucursal Centro\", \"queueType\": \"CAJA\"}"
echo.
timeout /t 3 /nobreak >nul

echo 📊 Estado actual de tickets:
curl -s "%API_URL%/admin/tickets/active" | jq -r "sort_by(.positionInQueue) | .[] | \"📋 \(.numero) | 👤 \(.clienteNombre) | 🏢 \(.queueType) | Pos: \(.positionInQueue) | \(.status)\""
echo.

echo ⏱️ Esperando 10 segundos para que los schedulers procesen...
timeout /t 10 /nobreak >nul

echo 🔄 PASO 3: Simulando atención de William (completar primer ticket)...
echo ────────────────────────────────────────────────────────

REM Obtener el ticket de William en CAJA para completarlo
for /f "tokens=*" %%i in ('curl -s "%API_URL%/admin/tickets/active" ^| jq -r ".[] | select(.clienteNombre==\"William García\" and .queueType==\"CAJA\") | .id"') do set WILLIAM_TICKET_ID=%%i

if defined WILLIAM_TICKET_ID (
    echo 🎫 Completando ticket de William ID: %WILLIAM_TICKET_ID%
    curl -X PUT "%API_URL%/admin/tickets/%WILLIAM_TICKET_ID%/complete" ^
      -H "Content-Type: application/json"
    echo.
    echo ✅ William completó su atención en CAJA
) else (
    echo ❌ No se encontró ticket activo de William en CAJA
)

timeout /t 5 /nobreak >nul

echo 🎫 PASO 4: William crea segundo ticket (PERSONAL_BANKER)...
echo ────────────────────────────────────────────────────────
curl -X POST "%API_URL%/tickets" ^
  -H "Content-Type: application/json" ^
  -d "{\"clienteId\": 1, \"branchOffice\": \"Sucursal Centro\", \"queueType\": \"PERSONAL_BANKER\"}"
echo.
echo ✅ William ahora puede crear segundo ticket (ya completó el primero)

echo.
echo 📊 ESTADO FINAL:
echo ────────────────────────────────────────────────────────
curl -s "%API_URL%/admin/tickets/active" | jq -r "sort_by(.positionInQueue) | .[] | \"📋 \(.numero) | 👤 \(.clienteNombre) | 🏢 \(.queueType) | Pos: \(.positionInQueue) | \(.status)\""

echo.
echo ════════════════════════════════════════════════════════
echo ✅ PRUEBA COMPLETA FINALIZADA
echo ════════════════════════════════════════════════════════
echo.
echo 📋 Resultado esperado:
echo   - Juan ahora es primero en cola CAJA
echo   - William es segundo en cola PERSONAL_BANKER  
echo   - Regla de negocio respetada ✅
echo.
echo 🎯 Ahora ejecuta: monitor-llamadas.bat
pause