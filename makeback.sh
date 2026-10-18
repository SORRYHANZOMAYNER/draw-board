#!/usr/bin/env bash
set -e

cd "$(dirname "$0")"

ENV_FILE="${ENV_FILE:-/root/drawboard.env}"

if [ ! -f "$ENV_FILE" ]; then
  echo "Не найден файл $ENV_FILE"
  exit 1
fi

set -a
. "$ENV_FILE"
set +a

: "${DB_PASS:?DB_PASS не указан в $ENV_FILE}"
: "${JWT_SECRET:?JWT_SECRET не указан в $ENV_FILE}"

if [ -f drawboard-backend.tar ]; then
  docker load -i drawboard-backend.tar
fi
docker network inspect board-net >/dev/null 2>&1 || docker network create board-net
docker rm -f drawboard-backend drawboard-backend-1 drawboard-backend-2 2>/dev/null || true

docker run -d \
  --name drawboard-backend-1 \
  --network board-net \
  --add-host host.docker.internal:host-gateway \
  --restart unless-stopped \
  -e SERVER_PORT=5465 \
  -e SPRING_DATASOURCE_URL=jdbc:mysql://host.docker.internal:3306/board \
  -e SPRING_DATASOURCE_USERNAME=board \
  -e SPRING_DATASOURCE_PASSWORD="$DB_PASS" \
  -e JWT_SECRET="$JWT_SECRET" \
  drawboard-backend:latest

docker run -d \
  --name drawboard-backend-2 \
  --network board-net \
  --add-host host.docker.internal:host-gateway \
  --restart unless-stopped \
  -e SERVER_PORT=5465 \
  -e SPRING_DATASOURCE_URL=jdbc:mysql://host.docker.internal:3306/board \
  -e SPRING_DATASOURCE_USERNAME=board \
  -e SPRING_DATASOURCE_PASSWORD="$DB_PASS" \
  -e JWT_SECRET="$JWT_SECRET" \
  drawboard-backend:latest
