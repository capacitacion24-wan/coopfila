@echo off
REM monitor-detailed.bat - Monitor detallado con historial de cambios

setlocal enabledelayedexpansion
set API_URL=http://localhost:8080/api
set LOG_FILE=monitor-log.txt

echo ========================================
echo   MONITOR DETALLADO - SISTEMA TICKETERO
echo ========================================
echo.
echo 🔍 Monitor con historial de cambios cada 5 segundos
echo 📝 Log guardado en: monitor-log.txt
echo 📱 Notificaciones Telegram: 1634964503
echo.
echo Presiona Ctrl+C para detener
echo ========================================

REM Crear archivo de log
echo [%date% %time%] Monitor iniciado > %LOG_FILE%

:detailed_monitor
cls
echo ========================================
echo   MONITOR DETALLADO - SISTEMA TICKETERO  
echo ========================================
echo 🕐 %date% %time%
echo.

REM Obtener datos actuales
curl -s "%API_URL%/admin/dashboard" > temp_dashboard.json
curl -s "%API_URL%/admin/tickets/active" > temp_tickets.json

echo 🎫 ESTADO ACTUAL DE TICKETS:
echo ========================================
for /f "tokens=*" %%i in ('jq -r ".[] | \"\(.numero)|\(.clienteNombre)|\(.status)|\(.positionInQueue)|\(.estimatedWaitMinutes)|\(.assignedAdvisorName // \"Sin asignar\")|\(.assignedModuleNumber // \"N/A\")\"" temp_tickets.json') do (
    set "line=%%i"
    for /f "tokens=1,2,3,4,5,6,7 delims=|" %%a in ("!line!") do (
        echo 📋 %%a ^| 👤 %%b ^| 🔄 %%c ^| Pos: %%d ^| ⏱️ %%emin ^| 👨‍💼 %%f ^| 🏢 Mod: %%g
        echo [%time%] %%a - %%b - %%c - Posicion %%d >> %LOG_FILE%
    )
)
echo.

echo 👥 ASESORES Y SU ESTADO:
echo ========================================
curl -s "%API_URL%/admin/advisors/available" | jq -r ".[] | \"👤 \(.name) | 🏢 Módulo \(.moduleNumber) | 🔄 \(.status) | 📊 \(.assignedTicketsCount) tickets\""
echo.

echo 📊 MÉTRICAS GENERALES:
echo ========================================
jq -r "\"📈 Tickets Activos: \(.totalActiveTickets) | 👥 Asesores Disponibles: \(.totalAvailableAdvisors)\"" temp_dashboard.json
echo.

echo 📱 ÚLTIMOS MENSAJES TELEGRAM:
echo ========================================
REM Simular consulta de mensajes recientes (los schedulers los procesan)
echo 💬 MessageScheduler ejecutándose cada 60s
echo 🔄 QueueProcessor ejecutándose cada 5s
echo 📤 Mensajes enviados automáticamente a Chat ID: 1634964503
echo.

echo 🔄 CAMBIOS DETECTADOS:
echo ========================================
REM Comparar con estado anterior (simplificado)
if exist temp_tickets_prev.json (
    fc /N temp_tickets.json temp_tickets_prev.json >nul
    if !errorlevel! neq 0 (
        echo 🆕 CAMBIO DETECTADO en tickets
        echo [%time%] CAMBIO DE ESTADO DETECTADO >> %LOG_FILE%
    ) else (
        echo ⏸️ Sin cambios desde última consulta
    )
) else (
    echo 🆕 Primera consulta - estableciendo baseline
)

REM Guardar estado actual para próxima comparación
copy temp_tickets.json temp_tickets_prev.json >nul

echo.
echo ⏱️ Próxima actualización en 5 segundos...
echo ========================================

timeout /t 5 /nobreak >nul
goto detailed_monitor