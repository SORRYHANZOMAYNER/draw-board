#!/bin/bash

set -e

BACKUP_DIR="$1"

if [ -z "$BACKUP_DIR" ]; then
  echo "Использование: $0 /путь/к/бэкапу"
  exit 1
fi

# Восстанавливаем все базы из указанной копии.
for DATABASE_DIR in "$BACKUP_DIR"/*; do
  [ -d "$DATABASE_DIR" ] || continue

  DATABASE=$(basename "$DATABASE_DIR")
  mysql -uroot -e "CREATE DATABASE IF NOT EXISTS \`$DATABASE\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

  for TABLE_FILE in "$DATABASE_DIR"/*.sql.gz; do
    [ -f "$TABLE_FILE" ] || continue
    gunzip -c "$TABLE_FILE" | mysql -uroot "$DATABASE"
  done
done

echo "Восстановление завершено"
