@echo off
REM test-con-monitor.bat - Prueba completa con monitor integrado

set API_URL=http://localhost:8080/api

echo ════════════════════════════════════════════════════════
echo    PRUEBA COMPLETA - SISTEMA TICKETERO
echo ════════════════════════════════════════════════════════
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

echo 👥 Creando clientes...
call create-clients.bat
echo.

echo 🎫 Creando tickets (en orden de llegada)...
echo ────────────────────────────────────────────────────────

echo 🎫 1. William - CAJA
curl -s -X POST "%API_URL%/tickets" ^
  -H "Content-Type: application/json" ^
  -d "{\"clienteId\": 1, \"branchOffice\": \"Sucursal Centro\", \"queueType\": \"CAJA\"}"
echo  ✅
timeout /t 2 /nobreak >nul

echo 🎫 2. Johanna - PERSONAL_BANKER
curl -s -X POST "%API_URL%/tickets" ^
  -H "Content-Type: application/json" ^
  -d "{\"clienteId\": 2, \"branchOffice\": \"Sucursal Centro\", \"queueType\": \"PERSONAL_BANKER\"}"
echo  ✅
timeout /t 2 /nobreak >nul

echo 🎫 3. Natalia - EMPRESAS
curl -s -X POST "%API_URL%/tickets" ^
  -H "Content-Type: application/json" ^
  -d "{\"clienteId\": 3, \"branchOffice\": \"Sucursal Centro\", \"queueType\": \"EMPRESAS\"}"
echo  ✅
timeout /t 2 /nobreak >nul

echo 🎫 4. Juan - CAJA (segundo en cola CAJA después de William)
curl -s -X POST "%API_URL%/tickets" ^
  -H "Content-Type: application/json" ^
  -d "{\"clienteId\": 4, \"branchOffice\": \"Sucursal Centro\", \"queueType\": \"CAJA\"}"
echo  ✅

echo.
echo ════════════════════════════════════════════════════════
echo ✅ 4 TICKETS CREADOS
echo ════════════════════════════════════════════════════════
echo.
echo 📋 Orden esperado en cola CAJA:
echo   1. William (llegó primero)
echo   2. Juan (llegó después)
echo.
echo 📝 NOTA: William NO puede crear segundo ticket hasta
echo          completar el primero (regla de negocio)
echo.
echo 🎯 Iniciando monitor en 3 segundos...
timeout /t 3 /nobreak >nul

REM Iniciar monitor
call monitor-llamadas.bat