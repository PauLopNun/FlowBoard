@echo off
REM FlowBoard - Script de utilidades para desarrollo
REM Uso: flow.bat [comando]

if "%1"=="" goto help
if "%1"=="help" goto help
if "%1"=="build" goto build
if "%1"=="clean" goto clean
if "%1"=="test" goto test
if "%1"=="run" goto run
if "%1"=="backend" goto backend
if "%1"=="install" goto install
goto unknown

:help
echo.
echo ================================
echo   FlowBoard - Comandos Rapidos
echo ================================
echo.
echo Uso: flow [comando]
echo.
echo Comandos disponibles:
echo.
echo   build      - Compilar app Android (debug)
echo   clean      - Limpiar builds
echo   test       - Ejecutar tests de Android
echo   run        - Instalar y ejecutar en dispositivo
echo   backend    - Iniciar backend Ktor
echo   install    - Instalar APK en dispositivo
echo   help       - Mostrar esta ayuda
echo.
echo Ejemplos:
echo   flow build
echo   flow run
echo   flow backend
echo.
goto end

:build
echo [FlowBoard] Compilando Android app (Debug)...
call gradlew.bat -p android assembleDebug
goto end

:clean
echo [FlowBoard] Limpiando builds...
call gradlew.bat clean
call gradlew.bat -p android clean
if exist backend call gradlew.bat -p backend clean
echo [FlowBoard] Limpieza completada.
goto end

:test
echo [FlowBoard] Ejecutando tests de Android...
call gradlew.bat -p android test
goto end

:run
echo [FlowBoard] Instalando y ejecutando app...
call gradlew.bat -p android installDebug
if errorlevel 1 (
    echo [ERROR] No se pudo instalar la app. Verifica que haya un dispositivo conectado.
) else (
    echo [FlowBoard] App instalada correctamente.
    echo [FlowBoard] Inicia la app manualmente en tu dispositivo.
)
goto end

:backend
echo [FlowBoard] Iniciando backend Ktor...
echo [INFO] Asegurate de tener PostgreSQL corriendo.
cd backend
call gradlew.bat run
cd ..
goto end

:install
echo [FlowBoard] Instalando APK en dispositivo...
call gradlew.bat -p android installDebug
goto end

:unknown
echo [ERROR] Comando desconocido: %1
echo Usa 'flow help' para ver los comandos disponibles.
goto end

:end
