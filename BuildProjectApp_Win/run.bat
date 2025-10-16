@echo off
REM Получаем путь к папке скрипта
SET SCRIPT_DIR=%~dp0

REM Путь к fat jar
SET JAR_FILE=%SCRIPT_DIR%LibertyAppSQL-1.0-SNAPSHOT-all.jar

REM Путь к папке lib рядом с jar
SET LIB_DIR=%SCRIPT_DIR%lib

REM Проверяем, что jar существует
IF NOT EXIST "%JAR_FILE%" (
    ECHO Ошибка: jar файл не найден: %JAR_FILE%
    PAUSE
    EXIT /B 1
)

REM Запуск fat jar с JavaFX
java ^
    --module-path "%LIB_DIR%" ^
    --add-modules javafx.controls,javafx.fxml ^
    -Dprism.order=sw ^
    -jar "%JAR_FILE%"
