@echo off
REM ==== Создание дистрибутива с jpackage ====

REM Путь к jar файлу
set INPUT_DIR=LibertyAppSQL\build\libs
set MAIN_JAR=LibertyAppSQL-1.0-SNAPSHOT-all.jar
set MAIN_CLASS=com.example.libertyappsql.Launcher
set APP_NAME=LibSQLApp

REM Запуск jpackage
jpackage ^
  --name %APP_NAME% ^
  --input %INPUT_DIR% ^
  --main-jar %MAIN_JAR% ^
  --main-class %MAIN_CLASS% ^
  --type app-image ^
  --java-options "--enable-preview"

echo.
echo ✅ Приложение собрано! Ищи папку %APP_NAME% в текущем каталоге.
pause
