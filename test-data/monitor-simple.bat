@echo off
REM monitor-simple.bat - Monitor simple sin dependencias externas

setlocal enabledelayedexpansion
set API_URL=http://localhost:8080/api

echo ========================================
echo    MONITOR SIMPLE - SISTEMA TICKETERO
echo ========================================
echo.
echo 🔍 Monitoreando sistema cada 5 segundos...
echo 📱 Telegram Chat: 1634964503
echo.
echo Presiona Ctrl+C para detener
echo ========================================

:simple_monitor
cls
echo ========================================
echo    MONITOR SIMPLE - SISTEMA TICKETERO  
echo ========================================
echo 🕐 %date% %time%
echo.

echo 🎫 CONSULTANDO TICKETS ACTIVOS...
echo ----------------------------------------
curl -s "%API_URL%/admin/tickets/active"
echo.
echo.

echo 👥 CONSULTANDO ASESORES DISPONIBLES...
echo ----------------------------------------
curl -s "%API_URL%/admin/advisors/available"
echo.
echo.

echo 📊 DASHBOARD COMPLETO...
echo ----------------------------------------
curl -s "%API_URL%/admin/dashboard"
echo.
echo.

echo 🧪 ENVIANDO PING A TELEGRAM...
echo ----------------------------------------
curl -s -X POST "%API_URL%/test/telegram?chatId=1634964503&message=🔄 Monitor activo - %time%"
echo.
echo.

echo ⏱️ Próxima actualización en 5 segundos...
echo ========================================

timeout /t 5 /nobreak >nul
goto simple_monitor