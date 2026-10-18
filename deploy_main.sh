#!/usr/bin/env bash

set -e

BACKEND_REPO="$1"
FRONTEND_REPO="$2"
MODE="${3:-production}"
DOMAIN="${4:-drawboard.duckdns.org}"
EMAIL="${5:-egoraksenov821@gmail.com}"

if [ -z "$BACKEND_REPO" ] || [ -z "$FRONTEND_REPO" ]; then
  echo "Использование: $0 <backend_git_url> <frontend_git_url> [production|local] [домен] [email]"
  exit 1
fi

bash /root/prepare_main.sh

if [ ! -f /root/drawboard.env ]; then
  cat > /root/drawboard.env <<EOF
DB_PASS=$(openssl rand -hex 16)
REPL_PASS=$(openssl rand -hex 16)
JWT_SECRET=$(openssl rand -hex 32)
EOF
  chmod 600 /root/drawboard.env
fi

rmdir /root/board-backend /root/board-frontend 2>/dev/null || {
  echo "Каталоги /root/board-backend или /root/board-frontend не пусты. Скрипт предназначен для чистого сервера."
  exit 1
}
git clone "$BACKEND_REPO" /root/board-backend
git clone "$FRONTEND_REPO" /root/board-frontend

bash /root/board-backend/makedb.sh

cd /root/board-backend
docker build -t drawboard-backend:latest .
docker save drawboard-backend:latest -o drawboard-backend.tar
bash ./makeback.sh

docker volume create letsencrypt
docker volume create certbot-www

if command -v ufw >/dev/null 2>&1 && ufw status | grep -q 'Status: active'; then
  ufw allow 80/tcp
  ufw allow 443/tcp
  ufw allow 3000/tcp
  ufw allow 9090/tcp
  ufw allow 9100/tcp
fi

cd /root/board-frontend
if [ "$MODE" = "local" ]; then
  cp nginx.local.conf nginx.conf
else
  docker run --rm -p 80:80 \
    -v letsencrypt:/etc/letsencrypt \
    certbot/certbot certonly --standalone \
    -d "$DOMAIN" \
    --email "$EMAIL" \
    --agree-tos \
    --no-eff-email
fi

docker build -t drawboard-frontend:latest .
docker save drawboard-frontend:latest -o drawboard-frontend.tar
bash ./makefrnt.sh

bash /root/board-backend/setup_monitoring_main.sh

if [ -d /root/logging-packages ]; then
  bash /root/board-backend/setup_logging_main.sh /root/logging-packages
else
  echo "Логирование пропущено: пакеты не найдены в /root/logging-packages"
fi

rm -f /root/board-backend/drawboard-backend.tar
rm -f /root/board-frontend/drawboard-frontend.tar
docker builder prune -af
apt clean

echo "Развёртывание main завершено"

