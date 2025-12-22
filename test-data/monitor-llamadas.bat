@echo off
REM monitor-llamadas.bat - Monitor simple para ver orden de llamadas

set API_URL=http://localhost:8080/api

:monitor
cls
echo ════════════════════════════════════════════════════════
echo           MONITOR DE LLAMADAS - TICKETERO
echo ════════════════════════════════════════════════════════
echo 🕐 %date% %time%
echo.

echo 🎫 TICKETS PARA LLAMAR (en orden):
echo ────────────────────────────────────────────────────────

curl -s "%API_URL%/admin/tickets/active" | jq -r "sort_by(.positionInQueue) | .[] | \"📋 \(.numero) | 👤 \(.clienteNombre) | 🏢 \(.queueType) | Pos: \(.positionInQueue) | \(.status)\""

if %errorlevel% neq 0 (
    echo ❌ Error consultando tickets
)

echo.
echo ⏱️ Actualizando en 5 segundos... (Ctrl+C para salir)
echo ════════════════════════════════════════════════════════

timeout /t 5 /nobreak >nul
goto monitor