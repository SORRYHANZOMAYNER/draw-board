#!/bin/bash

set -e

export DEBIAN_FRONTEND=noninteractive

apt update
apt install -y git docker.io docker-compose-v2 mysql-server cron curl openssl ca-certificates

systemctl enable --now docker
systemctl enable --now mysql
systemctl enable --now cron

mkdir -p /root/board-backend
mkdir -p /root/board-frontend

docker network create board-net || true
