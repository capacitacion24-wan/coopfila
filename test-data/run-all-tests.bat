@echo off
echo ========================================
echo EJECUTANDO PRUEBAS FUNCIONALES
echo ========================================

echo.
echo [1/4] Compilando proyecto...
call mvn clean compile -q

if %ERRORLEVEL% neq 0 (
    echo ERROR: Fallo en compilacion
    exit /b 1
)

echo.
echo [2/4] Ejecutando pruebas unitarias...
call mvn test -Dtest="**/*Test" -q

if %ERRORLEVEL% neq 0 (
    echo ERROR: Fallo en pruebas unitarias
    exit /b 1
)

echo.
echo [3/4] Ejecutando pruebas de integracion...
call mvn test -Dtest="**/*IntegrationTest" -q

if %ERRORLEVEL% neq 0 (
    echo ERROR: Fallo en pruebas de integracion
    exit /b 1
)

echo.
echo [4/4] Ejecutando pruebas funcionales...
call mvn test -Dtest="**/*FunctionalTest,**/*FlowTest" -q

if %ERRORLEVEL% neq 0 (
    echo ERROR: Fallo en pruebas funcionales
    exit /b 1
)

echo.
echo ========================================
echo ✅ TODAS LAS PRUEBAS COMPLETADAS
echo ========================================

echo.
echo Generando reporte de cobertura...
call mvn jacoco:report -q

echo.
echo 📊 Reporte disponible en: target/site/jacoco/index.html
echo.