#!/bin/bash

# Получаем путь к скрипту
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

# Путь к fat jar
JAR_FILE="$SCRIPT_DIR/LibertyAppSQL-1.0-SNAPSHOT-all.jar"

# Путь к папке lib рядом с jar
LIB_DIR="$SCRIPT_DIR/lib"

# Проверяем, что jar существует
if [ ! -f "$JAR_FILE" ]; then
    echo "Ошибка: jar файл не найден: $JAR_FILE"
    exit 1
fi

# Запуск fat jar с JavaFX
java \
  --module-path "$LIB_DIR" \
  --add-modules javafx.controls,javafx.fxml \
  -Dprism.order=sw \
  -jar "$JAR_FILE"
