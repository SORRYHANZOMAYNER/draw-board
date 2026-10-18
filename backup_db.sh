#!/bin/bash

set -e

BACKUP_ROOT=/root/mysql-backups
BACKUP_DIR="$BACKUP_ROOT/$(date +%Y-%m-%d_%H-%M-%S)"

mkdir -p "$BACKUP_DIR"

# На время копирования останавливаем применение новых изменений на slave.
mysql -uroot -e "STOP REPLICA SQL_THREAD;"
trap 'mysql -uroot -e "START REPLICA SQL_THREAD;"' EXIT

# Сохраняем позицию binlog и GTID.
mysql -uroot -e "SHOW REPLICA STATUS\G" > "$BACKUP_DIR/replication_status.txt"

# Получаем все пользовательские базы.
mysql -uroot -N -e "SELECT SCHEMA_NAME FROM information_schema.SCHEMATA WHERE SCHEMA_NAME NOT IN ('information_schema', 'performance_schema', 'mysql', 'sys');" |
while read -r DATABASE; do
  mkdir -p "$BACKUP_DIR/$DATABASE"

  # Сохраняем каждую таблицу отдельным архивом.
  mysql -uroot -N -e "SHOW FULL TABLES FROM \`$DATABASE\` WHERE Table_type = 'BASE TABLE';" |
  while read -r TABLE TYPE; do
    mysqldump -uroot --single-transaction --quick --set-gtid-purged=OFF --no-tablespaces "$DATABASE" "$TABLE" > "$BACKUP_DIR/$DATABASE/$TABLE.sql"
    gzip "$BACKUP_DIR/$DATABASE/$TABLE.sql"
  done
done

mysql -uroot -e "START REPLICA SQL_THREAD;"
trap - EXIT

# Удаляем копии старше семи дней.
find "$BACKUP_ROOT" -mindepth 1 -maxdepth 1 -type d -mtime +7 -exec rm -rf {} \;

echo "Бэкап создан: $BACKUP_DIR"
