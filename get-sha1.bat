@echo off
REM Script para obtener SHA-1 y SHA-256 fingerprints para Google Sign-In (Windows)
REM Estos fingerprints son necesarios para configurar Google Cloud Console

echo ==========================================
echo    Obteniendo SHA-1 y SHA-256 Fingerprints
echo ==========================================
echo.

REM Ruta del keystore de debug
set KEYSTORE_PATH=%USERPROFILE%\.android\debug.keystore

REM Verificar si existe el keystore
if not exist "%KEYSTORE_PATH%" (
    echo ERROR: No se encontro el keystore de debug en:
    echo    %KEYSTORE_PATH%
    echo.
    echo El keystore se crea automaticamente al compilar la app.
    echo Intenta compilar la app primero: gradlew.bat assembleDebug
    pause
    exit /b 1
)

echo Keystore encontrado en:
echo    %KEYSTORE_PATH%
echo.
echo ==========================================
echo    DEBUG KEYSTORE (Para desarrollo^)
echo ==========================================
echo.

REM Obtener fingerprints del keystore de debug
REM Contraseña por defecto: android
keytool -list -v -keystore "%KEYSTORE_PATH%" -alias androiddebugkey -storepass android -keypass android | findstr "SHA1 SHA256"

echo.
echo ==========================================
echo    INSTRUCCIONES
echo ==========================================
echo.
echo 1. Copia los valores SHA1 y SHA256 de arriba
echo 2. Ve a Google Cloud Console:
echo    https://console.cloud.google.com/apis/credentials
echo.
echo 3. Selecciona tu proyecto o crea uno nuevo
echo.
echo 4. Crea credenciales OAuth 2.0:
echo    - Tipo: Android
echo    - Nombre del paquete: com.flowboard
echo    - SHA-1: [pega el valor SHA1 de arriba]
echo.
echo 5. IMPORTANTE: Tambien crea credenciales Web:
echo    - Tipo: ID de cliente web
echo    - Nombre: FlowBoard Web Client
echo    - Copia el Client ID y actualiza GoogleAuthManager.kt
echo.
echo 6. Habilita la API de Google Sign-In:
echo    https://console.cloud.google.com/apis/library/identitytoolkit.googleapis.com
echo.
echo ==========================================
echo.
echo NOTA: Para RELEASE (produccion^), necesitaras repetir esto
echo       con el keystore de release cuando lo tengas.
echo.
pause
