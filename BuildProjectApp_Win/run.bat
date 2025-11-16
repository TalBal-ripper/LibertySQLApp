@echo off
setlocal

set SCRIPT_DIR=%~dp0
set JAR_FILE=%SCRIPT_DIR%LibertyAppSQL-1.0-SNAPSHOT-all.jar
set LIB_DIR=%SCRIPT_DIR%lib

if not exist "%JAR_FILE%" (
    echo Ошибка: JAR не найден: %JAR_FILE%
    pause
    exit /b 1
)

REM Попытка запуска с аппаратным рендерингом (es2)
echo Попытка запуска с аппаратным рендерингом...
java -Dprism.order=es2 ^
     -Dprism.verbose=true ^
     --module-path "%LIB_DIR%" ^
     --add-modules=javafx.controls,javafx.fxml ^
     -jar "%JAR_FILE%"
if %ERRORLEVEL%==0 (
    echo Запуск успешен!
    pause
    exit /b 0
)

REM Если аппаратный рендеринг не сработал — пробуем программный (sw)
echo Аппаратный рендеринг не сработал, пробуем программный...
java -Dprism.order=sw ^
     -Dprism.verbose=true ^
     --module-path "%LIB_DIR%" ^
     --add-modules=javafx.controls,javafx.fxml ^
     -jar "%JAR_FILE%"
if %ERRORLEVEL%==0 (
    echo Запуск успешен с программным рендерингом.
    pause
    exit /b 0
)

echo Не удалось запустить приложение ни с одним рендерером.
pause
exit /b 1

