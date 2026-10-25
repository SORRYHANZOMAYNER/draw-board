#!/bin/bash

set -e

MAIN_IP="$1"
ENV_FILE=/root/wordpress.env

if [ -z "$MAIN_IP" ]; then
  echo "Использование: $0 <ip_main>"
  exit 1
fi

if [ ! -f "$ENV_FILE" ]; then
  cat > "$ENV_FILE" <<EOF
CMS_DB_PASSWORD=$(openssl rand -hex 16)
EOF
  chmod 600 "$ENV_FILE"
fi

. "$ENV_FILE"

mysql -uroot <<SQL
CREATE DATABASE IF NOT EXISTS wordpress CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'wordpress'@'%' IDENTIFIED BY '${CMS_DB_PASSWORD}';
ALTER USER 'wordpress'@'%' IDENTIFIED BY '${CMS_DB_PASSWORD}';
GRANT ALL PRIVILEGES ON wordpress.* TO 'wordpress'@'%';
FLUSH PRIVILEGES;
SQL

docker network inspect board-net >/dev/null 2>&1 || docker network create board-net
docker volume create wordpress-files
docker rm -f wordpress 2>/dev/null || true

docker run -d \
  --name wordpress \
  --network board-net \
  --add-host host.docker.internal:host-gateway \
  --restart unless-stopped \
  -p 8080:80 \
  -e WORDPRESS_DB_HOST=host.docker.internal:3306 \
  -e WORDPRESS_DB_NAME=wordpress \
  -e WORDPRESS_DB_USER=wordpress \
  -e WORDPRESS_DB_PASSWORD="$CMS_DB_PASSWORD" \
  -v wordpress-files:/var/www/html \
  wordpress:latest

if command -v ufw >/dev/null 2>&1 && ufw status | grep -q 'Status: active'; then
  ufw allow 8080/tcp
fi

echo "WordPress: http://$MAIN_IP:8080"
