#!/bin/bash

TARGET_DIR="$1"

if [ -z "$TARGET_DIR" ]; then
    echo "Использование: $0 <папка>"
    exit 1
fi

find "$TARGET_DIR" -type f | while read -r file; do
    echo "===== FILE: $file ====="
    cat "$file"
    echo
done
