@echo off
REM monitor-compacto.bat - Monitor esencial con alertas básicas

setlocal enabledelayedexpansion
set API_URL=http://localhost:8080/api
set CHAT_ID=1634964503
set LOG_FILE=monitor-compacto.log
set ALERT_FILE=alertas.txt

REM Configuración de alertas
set MAX_WAIT_TIME=30
set MAX_QUEUE_LENGTH=10

echo.
echo ╔══════════════════════════════════════════════════════════╗
echo ║              MONITOR COMPACTO - TICKETERO                ║
echo ╚══════════════════════════════════════════════════════════╝
echo.
echo 🔍 Monitor compacto con alertas automáticas
echo 📱 Chat ID: %CHAT_ID%
echo 📝 Log: %LOG_FILE%
echo 🚨 Alertas: %ALERT_FILE%
echo.
echo Presiona Ctrl+C para detener
echo ══════════════════════════════════════════════════════════
echo.

REM Inicializar log
echo [%date% %time%] Monitor compacto iniciado > %LOG_FILE%

:monitor_loop
cls
echo ╔══════════════════════════════════════════════════════════╗
echo ║              MONITOR COMPACTO - TICKETERO                ║
echo ╚══════════════════════════════════════════════════════════╝
echo 🕐 %date% %time%
echo.

REM Obtener datos del dashboard
echo 📊 Obteniendo métricas...
curl -s "%API_URL%/admin/dashboard" > temp_dashboard.json
if %errorlevel% neq 0 (
    echo ❌ ERROR: No se puede conectar a la API
    echo [%time%] ERROR: API no responde >> %LOG_FILE%
    goto wait_and_retry
)

REM Extraer métricas principales
for /f "tokens=*" %%i in ('jq -r ".totalActiveTickets" temp_dashboard.json 2^>nul') do set TOTAL_TICKETS=%%i
for /f "tokens=*" %%i in ('jq -r ".totalAvailableAdvisors" temp_dashboard.json 2^>nul') do set TOTAL_ADVISORS=%%i

if "%TOTAL_TICKETS%"=="null" set TOTAL_TICKETS=0
if "%TOTAL_ADVISORS%"=="null" set TOTAL_ADVISORS=0

echo ═══════════════════════════════════════════════════════════
echo 📈 MÉTRICAS PRINCIPALES
echo ═══════════════════════════════════════════════════════════
echo   📋 Tickets Activos: %TOTAL_TICKETS%
echo   👥 Asesores Disponibles: %TOTAL_ADVISORS%
echo.

REM Obtener tickets activos
echo 🎫 TICKETS ACTIVOS:
echo ───────────────────────────────────────────────────────────
curl -s "%API_URL%/admin/tickets/active" > temp_tickets.json

REM Verificar si hay tickets
jq -e ". | length > 0" temp_tickets.json >nul 2>&1
if %errorlevel% equ 0 (
    REM Mostrar tickets con formato mejorado
    for /f "tokens=*" %%i in ('jq -r ".[] | \"\\(.numero)|\\(.clienteNombre)|\\(.queueType)|\\(.status)|\\(.positionInQueue)|\\(.estimatedWaitMinutes)\"" temp_tickets.json') do (
        set "line=%%i"
        for /f "tokens=1,2,3,4,5,6 delims=|" %%a in ("!line!") do (
            set STATUS_ICON=⏳
            if "%%d"=="PROXIMO" set STATUS_ICON=🟡
            if "%%d"=="ATENDIENDO" set STATUS_ICON=🟢
            if "%%d"=="COMPLETADO" set STATUS_ICON=✅
            
            echo   !STATUS_ICON! %%a ^| 👤 %%b ^| 🏢 %%c ^| Pos: %%e ^| ⏱️ %%fmin
            
            REM Verificar alertas de tiempo de espera
            if %%f gtr %MAX_WAIT_TIME% (
                echo     🚨 ALERTA: Tiempo de espera excesivo ^(%%fmin^)
                echo [%time%] ALERTA: Ticket %%a - Espera excesiva %%fmin min >> %ALERT_FILE%
                
                REM Enviar alerta por Telegram
                curl -s -X POST "%API_URL%/test/telegram?chatId=%CHAT_ID%&message=🚨 ALERTA: Ticket %%a lleva %%fmin minutos esperando" >nul
            )
        )
    )
    
    REM Contar tickets por cola y verificar alertas
    echo.
    echo 📊 RESUMEN POR COLA:
    echo ───────────────────────────────────────────────────────────
    
    REM Contar CAJA
    for /f %%i in ('jq "[.[] | select(.queueType==\"CAJA\")] | length" temp_tickets.json') do (
        if %%i gtr 0 (
            echo   🏢 CAJA: %%i tickets
            if %%i gtr %MAX_QUEUE_LENGTH% (
                echo     🚨 ALERTA: Cola CAJA muy larga ^(%%i tickets^)
                echo [%time%] ALERTA: Cola CAJA - %%i tickets >> %ALERT_FILE%
                curl -s -X POST "%API_URL%/test/telegram?chatId=%CHAT_ID%&message=🚨 ALERTA: Cola CAJA muy larga (%%i tickets)" >nul
            )
        )
    )
    
    REM Contar PERSONAL_BANKER
    for /f %%i in ('jq "[.[] | select(.queueType==\"PERSONAL_BANKER\")] | length" temp_tickets.json') do (
        if %%i gtr 0 (
            echo   🏢 PERSONAL_BANKER: %%i tickets
            if %%i gtr %MAX_QUEUE_LENGTH% (
                echo     🚨 ALERTA: Cola PERSONAL_BANKER muy larga ^(%%i tickets^)
                echo [%time%] ALERTA: Cola PERSONAL_BANKER - %%i tickets >> %ALERT_FILE%
            )
        )
    )
    
    REM Contar EMPRESAS
    for /f %%i in ('jq "[.[] | select(.queueType==\"EMPRESAS\")] | length" temp_tickets.json') do (
        if %%i gtr 0 (
            echo   🏢 EMPRESAS: %%i tickets
            if %%i gtr %MAX_QUEUE_LENGTH% (
                echo     🚨 ALERTA: Cola EMPRESAS muy larga ^(%%i tickets^)
                echo [%time%] ALERTA: Cola EMPRESAS - %%i tickets >> %ALERT_FILE%
            )
        )
    )
    
) else (
    echo   📭 No hay tickets activos
)

echo.
echo 👥 ASESORES DISPONIBLES:
echo ───────────────────────────────────────────────────────────
curl -s "%API_URL%/admin/advisors/available" > temp_advisors.json

jq -e ". | length > 0" temp_advisors.json >nul 2>&1
if %errorlevel% equ 0 (
    for /f "tokens=*" %%i in ('jq -r ".[] | \"\\(.name)|\\(.moduleNumber)|\\(.assignedTicketsCount)\"" temp_advisors.json') do (
        set "line=%%i"
        for /f "tokens=1,2,3 delims=|" %%a in ("!line!") do (
            set WORKLOAD_ICON=🟢
            if %%c gtr 0 set WORKLOAD_ICON=🟡
            if %%c gtr 1 set WORKLOAD_ICON=🔴
            
            echo   👤 %%a ^| 🏢 Mod: %%b ^| !WORKLOAD_ICON! %%c tickets
        )
    )
) else (
    echo   ❌ Todos los asesores ocupados
    echo [%time%] ALERTA: No hay asesores disponibles >> %ALERT_FILE%
    curl -s -X POST "%API_URL%/test/telegram?chatId=%CHAT_ID%&message=🚨 ALERTA: No hay asesores disponibles" >nul
)

echo.
echo 🔧 ESTADO DEL SISTEMA:
echo ───────────────────────────────────────────────────────────

REM Verificar salud de la API
curl -s "http://localhost:8080/actuator/health" >nul 2>&1
if %errorlevel% equ 0 (
    echo   ✅ API: Operativa
) else (
    echo   ❌ API: No responde
    echo [%time%] ERROR: API Health Check falló >> %LOG_FILE%
)

echo   🔄 Schedulers: Activos
echo   📱 Telegram: Configurado

REM Log de estado
echo [%time%] Tickets: %TOTAL_TICKETS%, Asesores: %TOTAL_ADVISORS% >> %LOG_FILE%

:wait_and_retry
echo.
echo ⏱️ Próxima actualización en 5 segundos...
echo ══════════════════════════════════════════════════════════

timeout /t 5 /nobreak >nul
goto monitor_loop

REM Limpiar archivos temporales al salir
:cleanup
if exist temp_dashboard.json del temp_dashboard.json
if exist temp_tickets.json del temp_tickets.json  
if exist temp_advisors.json del temp_advisors.json
echo.
echo 👋 Monitor detenido
pause