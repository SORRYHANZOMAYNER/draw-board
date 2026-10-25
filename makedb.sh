#!/usr/bin/env bash
set -e

DB_NAME=board
DB_USER=board
ENV_FILE="${ENV_FILE:-/root/drawboard.env}"
. "$ENV_FILE"
REPL_PASS="${REPL_PASS:-$DB_PASS}"
CNF=/etc/mysql/mysql.conf.d/mysqld.cnf

systemctl enable --now mysql

cat > "$CNF" <<'EOF'
[mysqld]
user = mysql
bind-address = 0.0.0.0
mysqlx-bind-address = 127.0.0.1
server-id = 1
log-bin = mysql-bin
binlog_format = ROW
gtid_mode = ON
enforce_gtid_consistency = ON
log_replica_updates = ON
max_binlog_size = 100M
EOF

systemctl restart mysql

mysql -uroot <<SQL
CREATE DATABASE IF NOT EXISTS ${DB_NAME}
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS '${DB_USER}'@'%' IDENTIFIED BY '${DB_PASS}';
ALTER USER '${DB_USER}'@'%' IDENTIFIED BY '${DB_PASS}';
GRANT ALL PRIVILEGES ON ${DB_NAME}.* TO '${DB_USER}'@'%';
CREATE USER IF NOT EXISTS repl@'%' IDENTIFIED WITH 'caching_sha2_password' BY '${REPL_PASS}';
ALTER USER repl@'%' IDENTIFIED WITH 'caching_sha2_password' BY '${REPL_PASS}';
GRANT REPLICATION SLAVE ON *.* TO repl@'%';
FLUSH PRIVILEGES;
SQL

if command -v ufw >/dev/null 2>&1 && ufw status | grep -q 'Status: active'; then
  DOCKER_SUBNET=$(docker network inspect board-net --format '{{(index .IPAM.Config 0).Subnet}}' 2>/dev/null || echo 172.17.0.0/16)
  ufw allow from "$DOCKER_SUBNET" to any port 3306
fi
