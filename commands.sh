#!/bin/bash

IP_MAIN="$1"

if [ -z "$IP_MAIN" ]; then
  echo "Использование: $0 <ip_main>"
  exit 1
fi

docker compose build
docker build -t drawboard-backend:latest .
docker save drawboard-backend:latest -o drawboard-backend.tar
scp drawboard-backend.tar "root@${IP_MAIN}:/root/board-backend"


docker load -i drawboard-backend.tar
docker network create board-net
docker run -d \
  --name drawboard-backend \
  --network board-net \
  --restart unless-stopped \
  -p 5465:5465   -e SERVER_PORT=5465 \
  -e SPRING_DATASOURCE_URL=jdbc:mysql://172.17.0.1:3306/board \
  -e SPRING_DATASOURCE_USERNAME=board \
  -e SPRING_DATASOURCE_PASSWORD="$DB_PASS" \
  drawboard-backend:latest

crontab -eq
0 3 * * * docker stop drawboard-frontend; docker run --rm -p 80:80 -v letsencrypt:/etc/letsencrypt certbot/certbot renew --quiet; docker start drawboard-frontend
