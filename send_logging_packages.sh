#!/usr/bin/env bash

set -e

SERVER="${1:-root@138.124.121.179}"
PACKAGE_DIR="${2:-/c/Users/egor.aksenov/Downloads}"
REMOTE_DIR=/tmp/drawboard-logging-packages

ssh "$SERVER" "mkdir -p $REMOTE_DIR"

scp "$PACKAGE_DIR/elasticsearch_8.17.1_amd64-224190-c7e1d4(1).deb" \
  "${SERVER}:$REMOTE_DIR/elasticsearch_8.17.1_amd64.deb"

scp "$PACKAGE_DIR/kibana_8.17.1_amd64-224190-5a9c08(1).deb" \
  "${SERVER}:$REMOTE_DIR/kibana_8.17.1_amd64.deb"

scp "$PACKAGE_DIR/logstash_8.17.1_amd64-224190-ec9eaf(1).deb" \
  "${SERVER}:$REMOTE_DIR/logstash_8.17.1_amd64.deb"

scp "$PACKAGE_DIR/filebeat_8.17.1_amd64-224190-171ce6(1).deb" \
  "${SERVER}:$REMOTE_DIR/filebeat_8.17.1_amd64.deb"

scp setup_logging_main.sh "${SERVER}:$REMOTE_DIR/setup_logging_main.sh"
ssh "$SERVER" "sudo mkdir -p /root/logging-packages && sudo mv $REMOTE_DIR/*.deb /root/logging-packages/ && sudo mv $REMOTE_DIR/setup_logging_main.sh /root/setup_logging_main.sh && rmdir $REMOTE_DIR"

echo "Пакеты отправлены. На сервере выполните:"
echo "bash /root/setup_logging_main.sh"
