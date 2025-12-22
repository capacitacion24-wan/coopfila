@echo off
REM monitor-tickets.bat - Monitor en tiempo real de tickets

setlocal enabledelayedexpansion
set API_URL=http://localhost:8080/api/admin

echo ========================================
echo    MONITOR TIEMPO REAL - TICKETERO
echo ========================================
echo.
echo 🔍 Monitoreando cambios de estado cada 5 segundos...
echo 📱 Chat ID: 1634964503
echo 🤖 Bot: 8591640924:AAG7t3qQ52aOvzEC2XtNh9BhHRPxdqe4VVg
echo.
echo Presiona Ctrl+C para detener
echo ========================================

:monitor_loop
cls
echo ========================================
echo    MONITOR TIEMPO REAL - TICKETERO
echo ========================================
echo 🕐 %date% %time%
echo.

echo 🎫 TICKETS ACTIVOS:
echo ----------------------------------------
curl -s "%API_URL%/tickets/active" | jq -r ".[] | \"📋 \(.numero) | \(.clienteNombre) | \(.queueType) | \(.status) | Pos: \(.positionInQueue) | \(.estimatedWaitMinutes)min\""
if %errorlevel% neq 0 (
    echo ❌ Error consultando tickets activos
)
echo.

echo 👥 ASESORES DISPONIBLES:
echo ----------------------------------------
curl -s "%API_URL%/advisors/available" | jq -r ".[] | \"👤 \(.name) | Módulo \(.moduleNumber) | \(.status) | Tickets: \(.assignedTicketsCount)\""
if %errorlevel% neq 0 (
    echo ❌ Error consultando asesores
)
echo.

echo 📊 RESUMEN DASHBOARD:
echo ----------------------------------------
curl -s "%API_URL%/dashboard" | jq -r "\"📈 Total Tickets Activos: \(.totalActiveTickets) | Asesores Disponibles: \(.totalAvailableAdvisors)\""
echo.

echo ⏱️ Próxima actualización en 5 segundos...
echo ========================================

timeout /t 5 /nobreak >nul
goto monitor_loop