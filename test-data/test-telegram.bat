@echo off
REM test-telegram.bat - Probar integración con Telegram

echo 📱 Probando integración con Telegram...

echo 🧪 Enviando mensaje de prueba...
curl -X POST "http://localhost:8080/api/test/telegram?chatId=1634964503&message=🤖 Test desde Ticketero API"
echo.
echo.

echo 📋 Consultando formatos de mensajes...
curl -X GET "http://localhost:8080/api/test/telegram/formats"
echo.
echo.

echo ✅ Prueba de Telegram completada
echo 📱 Revisa tu chat para ver el mensaje de prueba
pause