@echo off
REM execute-full-test.bat - Ejecutar prueba completa con monitor

echo ========================================
echo    PRUEBA COMPLETA CON MONITOR
echo ========================================
echo.

echo 🔍 Verificando que la aplicación esté corriendo...
curl -s http://localhost:8080/actuator/health >nul
if %errorlevel% neq 0 (
    echo ❌ ERROR: La aplicación no está corriendo
    echo    Ejecuta: docker-compose up -d
    pause
    exit /b 1
)
echo ✅ Aplicación corriendo correctamente
echo.

echo 📋 ¿Qué tipo de monitor quieres usar?
echo.
echo 1. Monitor Simple (sin dependencias)
echo 2. Monitor Detallado (requiere jq)
echo 3. Solo ejecutar prueba sin monitor
echo.
set /p choice="Selecciona opción (1-3): "

if "%choice%"=="1" (
    echo.
    echo 🚀 Iniciando monitor simple en nueva ventana...
    start "Monitor Ticketero" monitor-simple.bat
    timeout /t 3 /nobreak >nul
) else if "%choice%"=="2" (
    echo.
    echo 🚀 Iniciando monitor detallado en nueva ventana...
    start "Monitor Ticketero Detallado" monitor-detailed.bat
    timeout /t 3 /nobreak >nul
) else (
    echo.
    echo 📊 Ejecutando solo la prueba...
)

echo.
echo 🔄 Ejecutando prueba completa en 3 segundos...
timeout /t 3 /nobreak >nul

echo.
echo 📝 PASO 1: Creando 4 clientes...
call create-clients.bat

echo.
echo 🎫 PASO 2: Creando 5 tickets (con delay para ver cambios)...
call create-tickets.bat

echo.
echo 📊 PASO 3: Consultando estado final...
curl -X GET "http://localhost:8080/api/admin/dashboard"
echo.
echo.

echo 🧪 PASO 4: Mensaje final a Telegram...
curl -X POST "http://localhost:8080/api/test/telegram?chatId=1634964503&message=✅ Prueba completa finalizada - %date% %time%"
echo.
echo.

echo ========================================
echo ✅ PRUEBA COMPLETA EJECUTADA
echo ========================================
echo.
echo Resultados:
echo - 4 clientes creados
echo - 5 tickets creados (William repite)
echo - 15 mensajes programados para Telegram
echo - Monitor corriendo en ventana separada
echo.
echo 📱 Revisa tu Telegram para ver las notificaciones
echo 🖥️ El monitor seguirá mostrando cambios de estado
echo.
pause