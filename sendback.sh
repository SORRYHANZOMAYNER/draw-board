#!/usr/bin/env bash
docker compose build
docker build -t drawboard-backend:latest .
docker save drawboard-backend:latest -o drawboard-backend.tar
scp drawboard-backend.tar makeback.sh setup_git_projects.sh root@138.124.121.179:/root/board-backend
scp makeall.sh makedb.sh makedb_slave.sh prepare_main.sh deploy_main.sh restore_db.sh restore_main.sh README.md setup_monitoring_main.sh setup_logging_main.sh root@138.124.121.179:/root
scp makedb_slave.sh prepare_slave.sh backup_db.sh restore_db.sh install_cron.sh restore_slave.sh README.md setup_monitoring_slave.sh root@81.90.28.225:/root
