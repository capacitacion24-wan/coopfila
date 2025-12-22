@echo off
REM run-complete-test.bat - Ejecutar prueba completa del sistema

echo ========================================
echo    PRUEBA COMPLETA SISTEMA TICKETERO
echo ========================================
echo.
echo Configuración:
echo - Bot Token: 8591640924:AAG7t3qQ52aOvzEC2XtNh9BhHRPxdqe4VVg
echo - Chat ID: 1634964503
echo - API URL: http://localhost:8080
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

echo 📋 PASO 1: Creando 4 clientes...
call create-clients.bat
echo.

echo 🎫 PASO 2: Creando 5 tickets...
call create-tickets.bat
echo.

echo 📊 PASO 3: Consultando dashboard...
curl -X GET "http://localhost:8080/api/admin/dashboard" -H "Accept: application/json"
echo.
echo.

echo 🧪 PASO 4: Probando Telegram directamente...
curl -X POST "http://localhost:8080/api/test/telegram?chatId=1634964503&message=🧪 Prueba completa finalizada - Sistema Ticketero funcionando"
echo.
echo.

echo ========================================
echo ✅ PRUEBA COMPLETA FINALIZADA
echo ========================================
echo.
echo Resultados esperados:
echo - 4 clientes creados (William, Johanna, Natalia, Juan)
echo - 5 tickets creados (William repite antes de Juan)
echo - Mensajes enviados a Telegram (Chat ID: 1634964503)
echo - Dashboard muestra tickets activos
echo.
echo 📱 Revisa tu Telegram para ver todas las notificaciones
pause