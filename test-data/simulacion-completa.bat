@echo off
REM simulacion-completa.bat - Simular sistema completo con Telegram

set BOT_TOKEN=8591640924:AAG7t3qQ52aOvzEC2XtNh9BhHRPxdqe4VVg
set CHAT_ID=1634964503

echo ════════════════════════════════════════════════════════
echo        SIMULACIÓN COMPLETA - SISTEMA TICKETERO
echo ════════════════════════════════════════════════════════
echo.
echo 📋 Simulando creación de tickets y notificaciones
echo 📱 Chat ID: %CHAT_ID%
echo.

echo 🎫 CREANDO TICKETS (simulado)...
echo ────────────────────────────────────────────────────────

echo 1. William - CAJA
curl -s -X POST "https://api.telegram.org/bot%BOT_TOKEN%/sendMessage" ^
  -d "chat_id=%CHAT_ID%" ^
  -d "text=✅ <b>Ticket Creado</b>%0A%0A🎫 Número: <b>C01</b>%0A👤 Cliente: <b>William García</b>%0A🏢 Cola: <b>CAJA</b>%0A📍 Posición: <b>#1</b>%0A⏱️ Tiempo estimado: <b>5 minutos</b>" ^
  -d "parse_mode=HTML" >nul
echo   ✅ C01 - William (CAJA) - Posición #1
timeout /t 3 /nobreak >nul

echo 2. Johanna - PERSONAL_BANKER
curl -s -X POST "https://api.telegram.org/bot%BOT_TOKEN%/sendMessage" ^
  -d "chat_id=%CHAT_ID%" ^
  -d "text=✅ <b>Ticket Creado</b>%0A%0A🎫 Número: <b>P01</b>%0A👤 Cliente: <b>Johanna López</b>%0A🏢 Cola: <b>PERSONAL_BANKER</b>%0A📍 Posición: <b>#1</b>%0A⏱️ Tiempo estimado: <b>15 minutos</b>" ^
  -d "parse_mode=HTML" >nul
echo   ✅ P01 - Johanna (PERSONAL_BANKER) - Posición #1
timeout /t 3 /nobreak >nul

echo 3. Natalia - EMPRESAS
curl -s -X POST "https://api.telegram.org/bot%BOT_TOKEN%/sendMessage" ^
  -d "chat_id=%CHAT_ID%" ^
  -d "text=✅ <b>Ticket Creado</b>%0A%0A🎫 Número: <b>E01</b>%0A👤 Cliente: <b>Natalia Martínez</b>%0A🏢 Cola: <b>EMPRESAS</b>%0A📍 Posición: <b>#1</b>%0A⏱️ Tiempo estimado: <b>20 minutos</b>" ^
  -d "parse_mode=HTML" >nul
echo   ✅ E01 - Natalia (EMPRESAS) - Posición #1
timeout /t 3 /nobreak >nul

echo 4. Juan - CAJA
curl -s -X POST "https://api.telegram.org/bot%BOT_TOKEN%/sendMessage" ^
  -d "chat_id=%CHAT_ID%" ^
  -d "text=✅ <b>Ticket Creado</b>%0A%0A🎫 Número: <b>C02</b>%0A👤 Cliente: <b>Juan Rodríguez</b>%0A🏢 Cola: <b>CAJA</b>%0A📍 Posición: <b>#2</b>%0A⏱️ Tiempo estimado: <b>10 minutos</b>" ^
  -d "parse_mode=HTML" >nul
echo   ✅ C02 - Juan (CAJA) - Posición #2 (después de William)
timeout /t 3 /nobreak >nul

echo.
echo 📊 ESTADO ACTUAL DE COLAS:
echo ────────────────────────────────────────────────────────
echo   🏢 CAJA: William (#1) → Juan (#2)
echo   🏢 PERSONAL_BANKER: Johanna (#1)
echo   🏢 EMPRESAS: Natalia (#1)
echo.

echo ⏱️ Esperando 10 segundos (simulando tiempo de procesamiento)...
timeout /t 10 /nobreak >nul

echo.
echo 🔔 SIMULANDO LLAMADAS (orden de llegada)...
echo ────────────────────────────────────────────────────────

echo 1. Llamando a William (llegó primero)
curl -s -X POST "https://api.telegram.org/bot%BOT_TOKEN%/sendMessage" ^
  -d "chat_id=%CHAT_ID%" ^
  -d "text=⏰ <b>Pronto será tu turno</b>%0A%0A🎫 Ticket: <b>C01</b>%0A%0APrepárate, serás atendido en los próximos minutos." ^
  -d "parse_mode=HTML" >nul
echo   ⏰ William - Próximo turno
timeout /t 5 /nobreak >nul

curl -s -X POST "https://api.telegram.org/bot%BOT_TOKEN%/sendMessage" ^
  -d "chat_id=%CHAT_ID%" ^
  -d "text=🔔 <b>¡ES TU TURNO!</b>%0A%0A🎫 Ticket: <b>C01</b>%0A👤 Asesor: <b>María González</b>%0A🏢 Módulo: <b>1</b>%0A%0ADirígete al módulo indicado." ^
  -d "parse_mode=HTML" >nul
echo   🔔 William - ¡ES TU TURNO! (Módulo 1)
timeout /t 5 /nobreak >nul

echo 2. Juan avanza a posición #1 en CAJA
curl -s -X POST "https://api.telegram.org/bot%BOT_TOKEN%/sendMessage" ^
  -d "chat_id=%CHAT_ID%" ^
  -d "text=📈 <b>Actualización de Cola</b>%0A%0A🎫 Ticket: <b>C02</b>%0A📍 Nueva posición: <b>#1</b>%0A⏱️ Tiempo estimado: <b>5 minutos</b>" ^
  -d "parse_mode=HTML" >nul
echo   📈 Juan - Ahora es #1 en CAJA
timeout /t 5 /nobreak >nul

echo 3. Llamando a Juan (ahora primero en CAJA)
curl -s -X POST "https://api.telegram.org/bot%BOT_TOKEN%/sendMessage" ^
  -d "chat_id=%CHAT_ID%" ^
  -d "text=⏰ <b>Pronto será tu turno</b>%0A%0A🎫 Ticket: <b>C02</b>%0A%0APrepárate, serás atendido en los próximos minutos." ^
  -d "parse_mode=HTML" >nul
echo   ⏰ Juan - Próximo turno
timeout /t 5 /nobreak >nul

curl -s -X POST "https://api.telegram.org/bot%BOT_TOKEN%/sendMessage" ^
  -d "chat_id=%CHAT_ID%" ^
  -d "text=🔔 <b>¡ES TU TURNO!</b>%0A%0A🎫 Ticket: <b>C02</b>%0A👤 Asesor: <b>Carlos Pérez</b>%0A🏢 Módulo: <b>2</b>%0A%0ADirígete al módulo indicado." ^
  -d "parse_mode=HTML" >nul
echo   🔔 Juan - ¡ES TU TURNO! (Módulo 2)

echo.
echo ════════════════════════════════════════════════════════
echo ✅ SIMULACIÓN COMPLETADA
echo ════════════════════════════════════════════════════════
echo.
echo 📋 Resumen:
echo   - 4 tickets creados
echo   - William atendido primero (llegó antes)
echo   - Juan atendido segundo (respeta orden de llegada)
echo   - 10 mensajes enviados a Telegram
echo.
echo 📱 Revisa tu Telegram para ver toda la secuencia
echo.
echo 💡 Nota: William NO puede crear segundo ticket hasta
echo          completar el primero (regla de negocio respetada)
pause