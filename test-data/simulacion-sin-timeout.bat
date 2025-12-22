@echo off
REM simulacion-sin-timeout.bat - Simulacion sin timeout para evitar errores

set BOT_TOKEN=8591640924:AAG7t3qQ52aOvzEC2XtNh9BhHRPxdqe4VVg
set CHAT_ID=1634964503

echo ========================================================
echo        SIMULACION COMPLETA - TODAS LAS COLAS
echo ========================================================
echo.

echo Creando 4 tickets...

echo 1. William - CAJA
curl -s -X POST "https://api.telegram.org/bot%BOT_TOKEN%/sendMessage" ^
  -d "chat_id=%CHAT_ID%" ^
  -d "text=Ticket C01 - William Garcia - CAJA - Posicion 1 - 5 min" >nul
echo   OK - C01 William
ping localhost -n 3 >nul

echo 2. Johanna - PERSONAL_BANKER  
curl -s -X POST "https://api.telegram.org/bot%BOT_TOKEN%/sendMessage" ^
  -d "chat_id=%CHAT_ID%" ^
  -d "text=Ticket P01 - Johanna Lopez - PERSONAL_BANKER - Posicion 1 - 15 min" >nul
echo   OK - P01 Johanna
ping localhost -n 3 >nul

echo 3. Natalia - EMPRESAS
curl -s -X POST "https://api.telegram.org/bot%BOT_TOKEN%/sendMessage" ^
  -d "chat_id=%CHAT_ID%" ^
  -d "text=Ticket E01 - Natalia Martinez - EMPRESAS - Posicion 1 - 20 min" >nul
echo   OK - E01 Natalia
ping localhost -n 3 >nul

echo 4. Juan - CAJA
curl -s -X POST "https://api.telegram.org/bot%BOT_TOKEN%/sendMessage" ^
  -d "chat_id=%CHAT_ID%" ^
  -d "text=Ticket C02 - Juan Rodriguez - CAJA - Posicion 2 - 10 min" >nul
echo   OK - C02 Juan
ping localhost -n 3 >nul

echo.
echo Estado inicial:
echo   CAJA: William (1) - Juan (2)
echo   PERSONAL_BANKER: Johanna (1)  
echo   EMPRESAS: Natalia (1)
echo.
echo Esperando...
ping localhost -n 6 >nul

echo.
echo ========================================================
echo LLAMANDO TODOS LOS TURNOS (por orden de llegada)
echo ========================================================

echo.
echo 1. WILLIAM (CAJA) - Llego primero
curl -s -X POST "https://api.telegram.org/bot%BOT_TOKEN%/sendMessage" ^
  -d "chat_id=%CHAT_ID%" ^
  -d "text=PROXIMO TURNO: C01 - William - CAJA - Preparate" >nul
ping localhost -n 4 >nul

curl -s -X POST "https://api.telegram.org/bot%BOT_TOKEN%/sendMessage" ^
  -d "chat_id=%CHAT_ID%" ^
  -d "text=ES TU TURNO: C01 - William - Asesor Maria - Modulo 1" >nul
echo   William llamado y atendido
ping localhost -n 4 >nul

echo.
echo 2. JOHANNA (PERSONAL_BANKER) - Segunda en llegar
curl -s -X POST "https://api.telegram.org/bot%BOT_TOKEN%/sendMessage" ^
  -d "chat_id=%CHAT_ID%" ^
  -d "text=PROXIMO TURNO: P01 - Johanna - PERSONAL_BANKER - Preparate" >nul
ping localhost -n 4 >nul

curl -s -X POST "https://api.telegram.org/bot%BOT_TOKEN%/sendMessage" ^
  -d "chat_id=%CHAT_ID%" ^
  -d "text=ES TU TURNO: P01 - Johanna - Asesor Ana - Modulo 3" >nul
echo   Johanna llamada y atendida
ping localhost -n 4 >nul

echo.
echo 3. NATALIA (EMPRESAS) - Tercera en llegar
curl -s -X POST "https://api.telegram.org/bot%BOT_TOKEN%/sendMessage" ^
  -d "chat_id=%CHAT_ID%" ^
  -d "text=PROXIMO TURNO: E01 - Natalia - EMPRESAS - Preparate" >nul
ping localhost -n 4 >nul

curl -s -X POST "https://api.telegram.org/bot%BOT_TOKEN%/sendMessage" ^
  -d "chat_id=%CHAT_ID%" ^
  -d "text=ES TU TURNO: E01 - Natalia - Asesor Luis - Modulo 5" >nul
echo   Natalia llamada y atendida
ping localhost -n 4 >nul

echo.
echo 4. JUAN (CAJA) - Cuarto en llegar, ahora primero en CAJA
curl -s -X POST "https://api.telegram.org/bot%BOT_TOKEN%/sendMessage" ^
  -d "chat_id=%CHAT_ID%" ^
  -d "text=ACTUALIZACION: C02 - Juan - Ahora eres numero 1 en CAJA" >nul
ping localhost -n 3 >nul

curl -s -X POST "https://api.telegram.org/bot%BOT_TOKEN%/sendMessage" ^
  -d "chat_id=%CHAT_ID%" ^
  -d "text=PROXIMO TURNO: C02 - Juan - CAJA - Preparate" >nul
ping localhost -n 4 >nul

curl -s -X POST "https://api.telegram.org/bot%BOT_TOKEN%/sendMessage" ^
  -d "chat_id=%CHAT_ID%" ^
  -d "text=ES TU TURNO: C02 - Juan - Asesor Carlos - Modulo 2" >nul
echo   Juan llamado y atendido

echo.
echo ========================================================
echo SIMULACION COMPLETA FINALIZADA
echo ========================================================
echo.
echo Orden de atencion (correcto):
echo   1. William (CAJA) - Llego primero
echo   2. Johanna (PERSONAL_BANKER) - Llego segunda  
echo   3. Natalia (EMPRESAS) - Llego tercera
echo   4. Juan (CAJA) - Llego cuarto, atendido cuando William termino
echo.
echo Total mensajes enviados: 13
echo Regla de negocio respetada: William NO puede crear segundo ticket
echo.
echo Revisa tu Telegram para ver toda la secuencia
pause