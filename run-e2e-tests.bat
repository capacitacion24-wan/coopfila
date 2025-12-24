@echo off
echo ========================================
echo EJECUTANDO TESTS E2E - COOPFILA
echo ========================================

echo.
echo 1. Ejecutando todos los tests de integracion...
mvn test -Dtest="*IT" -Dmaven.test.failure.ignore=true

echo.
echo 2. Generando reporte HTML...
mvn surefire-report:report

echo.
echo 3. Resumen de ejecucion:
echo ========================================

findstr /C:"Tests run:" target\surefire-reports\*.txt | findstr /V "Time elapsed"

echo.
echo 4. Reporte HTML generado en:
echo target\site\surefire-report.html

echo.
echo ========================================
echo TESTS E2E COMPLETADOS
echo ========================================

pause