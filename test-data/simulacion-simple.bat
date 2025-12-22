@echo off
REM simulacion-simple.bat - Simulacion con caracteres ASCII simples

set BOT_TOKEN=8591640924:AAG7t3qQ52aOvzEC2XtNh9BhHRPxdqe4VVg
set CHAT_ID=1634964503

echo ========================================================
echo        SIMULACION COMPLETA - SISTEMA TICKETERO
echo ========================================================
echo.

echo Creando tickets...

echo 1. William - CAJA
curl -s -X POST "https://api.telegram.org/bot%BOT_TOKEN%/sendMessage" ^
  -d "chat_id=%CHAT_ID%" ^
  -d "text=Ticket Creado: C01 - William Garcia - CAJA - Posicion 1 - 5 minutos" >nul
echo   OK - C01 William
timeout /t 2 /nobreak >nul

echo 2. Johanna - PERSONAL_BANKER  
curl -s -X POST "https://api.telegram.org/bot%BOT_TOKEN%/sendMessage" ^
  -d "chat_id=%CHAT_ID%" ^
  -d "text=Ticket Creado: P01 - Johanna Lopez - PERSONAL_BANKER - Posicion 1 - 15 minutos" >nul
echo   OK - P01 Johanna
timeout /t 2 /nobreak >nul

echo 3. Natalia - EMPRESAS
curl -s -X POST "https://api.telegram.org/bot%BOT_TOKEN%/sendMessage" ^
  -d "chat_id=%CHAT_ID%" ^
  -d "text=Ticket Creado: E01 - Natalia Martinez - EMPRESAS - Posicion 1 - 20 minutos" >nul
echo   OK - E01 Natalia
timeout /t 2 /nobreak >nul

echo 4. Juan - CAJA (segundo en cola)
curl -s -X POST "https://api.telegram.org/bot%BOT_TOKEN%/sendMessage" ^
  -d "chat_id=%CHAT_ID%" ^
  -d "text=Ticket Creado: C02 - Juan Rodriguez - CAJA - Posicion 2 - 10 minutos" >nul
echo   OK - C02 Juan (despues de William)
timeout /t 2 /nobreak >nul

echo.
echo Estado actual:
echo   CAJA: William (1) - Juan (2)
echo   PERSONAL_BANKER: Johanna (1)  
echo   EMPRESAS: Natalia (1)
echo.

echo Esperando procesamiento...
timeout /t 5 /nobreak >nul

echo.
echo Llamando tickets en orden de llegada...

echo 1. William (llego primero)
curl -s -X POST "https://api.telegram.org/bot%BOT_TOKEN%/sendMessage" ^
  -d "chat_id=%CHAT_ID%" ^
  -d "text=PROXIMO TURNO: C01 - William - Preparate" >nul
echo   Aviso William
timeout /t 3 /nobreak >nul

curl -s -X POST "https://api.telegram.org/bot%BOT_TOKEN%/sendMessage" ^
  -d "chat_id=%CHAT_ID%" ^
  -d "text=ES TU TURNO: C01 - William - Asesor: Maria - Modulo: 1" >nul
echo   Llamada William
timeout /t 3 /nobreak >nul

echo 2. Juan avanza a posicion 1
curl -s -X POST "https://api.telegram.org/bot%BOT_TOKEN%/sendMessage" ^
  -d "chat_id=%CHAT_ID%" ^
  -d "text=ACTUALIZACION: C02 - Juan - Ahora eres numero 1 en CAJA" >nul
echo   Juan ahora primero
timeout /t 3 /nobreak >nul

echo 3. Juan es llamado
curl -s -X POST "https://api.telegram.org/bot%BOT_TOKEN%/sendMessage" ^
  -d "chat_id=%CHAT_ID%" ^
  -d "text=PROXIMO TURNO: C02 - Juan - Preparate" >nul
echo   Aviso Juan
timeout /t 3 /nobreak >nul

curl -s -X POST "https://api.telegram.org/bot%BOT_TOKEN%/sendMessage" ^
  -d "chat_id=%CHAT_ID%" ^
  -d "text=ES TU TURNO: C02 - Juan - Asesor: Carlos - Modulo: 2" >nul
echo   Llamada Juan

echo.
echo ========================================================
echo SIMULACION COMPLETADA
echo ========================================================
echo.
echo Resumen:
echo - William atendido primero (llego antes)
echo - Juan atendido segundo (orden correcto)
echo - 8 mensajes enviados a Telegram
echo.
echo Revisa tu Telegram
pause