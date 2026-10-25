#!/usr/bin/env bash

set -e

MAIN_SERVER="$1"
SLAVE_SERVER="$2"

if [ -z "$MAIN_SERVER" ] || [ -z "$SLAVE_SERVER" ]; then
  echo "Использование: $0 <user@ip_main> <user@ip_slave>"
  exit 1
fi

docker compose build
docker build -t drawboard-backend:latest .
docker save drawboard-backend:latest -o drawboard-backend.tar
scp drawboard-backend.tar makeback.sh setup_git_projects.sh "${MAIN_SERVER}:/root/board-backend"
scp makeall.sh makedb.sh makedb_slave.sh prepare_main.sh deploy_main.sh restore_db.sh restore_main.sh send_env_to_slave.sh setup_cms.sh README.md setup_monitoring_main.sh setup_logging_main.sh "${MAIN_SERVER}:/root"
scp makedb_slave.sh prepare_slave.sh backup_db.sh restore_db.sh install_cron.sh restore_slave.sh README.md setup_monitoring_slave.sh "${SLAVE_SERVER}:/root"
