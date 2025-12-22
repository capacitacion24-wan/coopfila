@echo off
REM monitor-simple-sin-jq.bat - Monitor sin dependencias externas

set API_URL=http://localhost:8080/api

:monitor
cls
echo ════════════════════════════════════════════════════════
echo           MONITOR SIMPLE - SISTEMA TICKETERO
echo ════════════════════════════════════════════════════════
echo 🕐 %date% %time%
echo.

echo 🎫 CONSULTANDO TICKETS ACTIVOS...
echo ────────────────────────────────────────────────────────
curl -s "%API_URL%/admin/tickets/active"
echo.
echo.

echo 👥 CONSULTANDO ASESORES DISPONIBLES...
echo ────────────────────────────────────────────────────────
curl -s "%API_URL%/admin/advisors/available"
echo.
echo.

echo 📊 DASHBOARD COMPLETO...
echo ────────────────────────────────────────────────────────
curl -s "%API_URL%/admin/dashboard"
echo.
echo.

echo ⏱️ Actualizando en 5 segundos... (Ctrl+C para salir)
echo ════════════════════════════════════════════════════════

timeout /t 5 /nobreak >nul
goto monitor