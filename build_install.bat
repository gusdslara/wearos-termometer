@echo off
setlocal
cd /d "%~dp0"
set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
set "ADB=C:\Users\GUS\AppData\Local\Android\Sdk\platform-tools\adb.exe"
set "WATCH_IP=192.168.15.84"
set "PORT=%1"

if "%PORT%"=="" (
    echo [INFO] Nenhuma porta passada como argumento. Tentando porta padrao 42077...
    set "WATCH=%WATCH_IP%:42077"
) else (
    set "WATCH=%WATCH_IP%:%PORT%"
)

set "LOG=%~dp0build_log.txt"

echo === BUILD TERMOMETRO WEAROS %date% %time% === > "%LOG%"
call gradlew.bat assembleDebug >> "%LOG%" 2>&1
if errorlevel 1 (
  echo BUILD_FAILED >> "%LOG%"
  echo [ERRO] Falha na compilacao. Verifique %LOG%
  goto :end
)
echo BUILD_OK >> "%LOG%"

echo Conectando ao relogio em %WATCH%...
"%ADB%" disconnect %WATCH% >> "%LOG%" 2>&1
"%ADB%" connect %WATCH%
timeout /t 2 /nobreak > nul
"%ADB%" devices

echo Instalando APK no relogio...
"%ADB%" -s %WATCH% install -r app\build\outputs\apk\debug\app-debug.apk
if errorlevel 1 (
  echo [ERRO] Falha ao instalar no relogio. Confira se o Wireless Debugging esta ativado no Wear OS com a porta correta.
  goto :end
)

echo Iniciando o app Termometro...
"%ADB%" -s %WATCH% shell am start -n com.wearports.thermometer/.MainActivity
echo Pronto! App aberto com sucesso.

:end
echo === FIM %time% === >> "%LOG%"
endlocal
