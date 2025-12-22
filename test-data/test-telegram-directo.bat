@echo off
REM test-telegram-directo.bat - Probar Telegram sin Spring Boot

set BOT_TOKEN=8591640924:AAG7t3qQ52aOvzEC2XtNh9BhHRPxdqe4VVg
set CHAT_ID=1634964503

echo ════════════════════════════════════════════════════════
echo           PRUEBA DIRECTA DE TELEGRAM
echo ════════════════════════════════════════════════════════
echo.
echo 🤖 Bot Token: %BOT_TOKEN%
echo 📱 Chat ID: %CHAT_ID%
echo.

echo 🔍 1. Verificando bot...
curl -s "https://api.telegram.org/bot%BOT_TOKEN%/getMe"
echo.
echo.

echo 📤 2. Enviando mensaje de prueba...
curl -s -X POST "https://api.telegram.org/bot%BOT_TOKEN%/sendMessage" ^
  -d "chat_id=%CHAT_ID%" ^
  -d "text=🧪 Prueba directa - Bot funcionando correctamente"
echo.
echo.

echo 📤 3. Enviando mensaje simulando ticket creado...
curl -s -X POST "https://api.telegram.org/bot%BOT_TOKEN%/sendMessage" ^
  -d "chat_id=%CHAT_ID%" ^
  -d "text=✅ Ticket C01 creado - Posición #1 - 5 minutos de espera" ^
  -d "parse_mode=HTML"
echo.
echo.

echo 📤 4. Enviando mensaje simulando próximo turno...
curl -s -X POST "https://api.telegram.org/bot%BOT_TOKEN%/sendMessage" ^
  -d "chat_id=%CHAT_ID%" ^
  -d "text=⏰ <b>Pronto será tu turno</b> - Ticket C01" ^
  -d "parse_mode=HTML"
echo.
echo.

echo 📤 5. Enviando mensaje simulando es tu turno...
curl -s -X POST "https://api.telegram.org/bot%BOT_TOKEN%/sendMessage" ^
  -d "chat_id=%CHAT_ID%" ^
  -d "text=🔔 <b>¡ES TU TURNO!</b> - Ticket C01 - Asesor: María - Módulo: 3" ^
  -d "parse_mode=HTML"
echo.
echo.

echo ════════════════════════════════════════════════════════
echo ✅ PRUEBA COMPLETADA
echo ════════════════════════════════════════════════════════
echo.
echo 📱 Revisa tu Telegram para ver los 5 mensajes enviados
pause