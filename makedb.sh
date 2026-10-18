#!/usr/bin/env bash
set -e
DB_NAME=board
DB_USER=board
ENV_FILE="${ENV_FILE:-/root/drawboard.env}"

if [ ! -f "$ENV_FILE" ]; then
  echo "Не найден файл $ENV_FILE"
  exit 1
fi

set -a
. "$ENV_FILE"
set +a

: "${DB_PASS:?DB_PASS не указан в $ENV_FILE}"
REPL_PASS="${REPL_PASS:-$DB_PASS}"
CNF=/etc/mysql/mysql.conf.d/mysqld.cnf
systemctl enable --now mysql
cat > "$CNF" <<'EOF'
#
# The MySQL database server configuration file.
#
# One can use all long options that the program supports.
# Run program with --help to get a list of available options and with
# --print-defaults to see which it would actually understand and use.
#
# For explanations see
# http://dev.mysql.com/doc/mysql/en/server-system-variables.html

# Here is entries for some specific programs
# The following values assume you have at least 32M ram

[mysqld]
#
# * Basic Settings
#
user            = mysql
# pid-file      = /var/run/mysqld/mysqld.pid
# socket        = /var/run/mysqld/mysqld.sock
# port          = 3306
# datadir       = /var/lib/mysql


# If MySQL is running as a replication slave, this should be
# changed. Ref https://dev.mysql.com/doc/refman/8.0/en/server-system-variables.html#sysvar_tmpdir
# tmpdir                = /tmp
#
# Instead of skip-networking the default is now to listen only on
# localhost which is more compatible and is not less secure.
bind-address            = 0.0.0.0
mysqlx-bind-address     = 127.0.0.1
#
# * Fine Tuning
#
key_buffer_size         = 16M
# max_allowed_packet    = 64M
# thread_stack          = 256K

# thread_cache_size       = -1

# This replaces the startup script and checks MyISAM tables if needed
# the first time they are touched
myisam-recover-options  = BACKUP

# max_connections        = 151

# table_open_cache       = 4000

#
# * Logging and Replication
#
# Both location gets rotated by the cronjob.
#
# Log all queries
# Be aware that this log type is a performance killer.
# general_log_file        = /var/log/mysql/query.log
# general_log             = 1
#
# Error log - should be very few entries.
#
log_error = /var/log/mysql/error.log
#
# Here you can see queries with especially long duration
# slow_query_log                = 1
# slow_query_log_file   = /var/log/mysql/mysql-slow.log
# long_query_time = 2
# log-queries-not-using-indexes
#
# The following can be used as easy to replay backup logs or for replication.
# note: if you are setting up a replication slave, see README.Debian about
#       other settings you may need to changes
server-id = 1
log-bin = mysql-bin
binlog_format = row
gtid-mode=ON
enforce-gtid-consistency
log-replica-updates
# log_bin                       = /var/log/mysql/mysql-bin.log
# binlog_expire_logs_seconds    = 2592000
max_binlog_size   = 100M
# binlog_do_db          = include_database_name
# binlog_ignore_db      = include_database_name
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
