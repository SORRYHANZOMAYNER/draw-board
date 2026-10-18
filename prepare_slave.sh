#!/bin/bash

apt update
apt install -y git docker.io docker-compose-v2 mysql-server cron curl

systemctl enable --now docker
systemctl enable --now mysql
systemctl enable --now cron

mkdir -p /root/mysql-backups
