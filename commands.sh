docker compose build
docker build -t drawboard-backend:latest .
docker save drawboard-backend:latest -o drawboard-backend.tar
scp drawboard-backend.tar.gz root@138.124.121.179:/root/board-backend


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
