#!/usr/bin/env bash

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SERVER="${1:?Укажите сервер, например: convey99@192.168.1.10}"
PACKAGE_DIR="${2:-$SCRIPT_DIR/../packages}"
REMOTE_DIR=/tmp/drawboard-logging-packages

ssh "$SERVER" "mkdir -p $REMOTE_DIR"

scp "$PACKAGE_DIR/elasticsearch_8.17.1_amd64.deb" \
  "${SERVER}:$REMOTE_DIR/elasticsearch_8.17.1_amd64.deb"

scp "$PACKAGE_DIR/kibana_8.17.1_amd64.deb" \
  "${SERVER}:$REMOTE_DIR/kibana_8.17.1_amd64.deb"

scp "$PACKAGE_DIR/logstash_8.17.1_amd64.deb" \
  "${SERVER}:$REMOTE_DIR/logstash_8.17.1_amd64.deb"

scp "$PACKAGE_DIR/filebeat_8.17.1_amd64.deb" \
  "${SERVER}:$REMOTE_DIR/filebeat_8.17.1_amd64.deb"

scp "$SCRIPT_DIR/setup_logging_main.sh" "${SERVER}:$REMOTE_DIR/setup_logging_main.sh"
ssh -t "$SERVER" "sudo mkdir -p /root/logging-packages && sudo mv $REMOTE_DIR/*.deb /root/logging-packages/ && sudo mv $REMOTE_DIR/setup_logging_main.sh /root/setup_logging_main.sh && rmdir $REMOTE_DIR"

echo "Пакеты отправлены. На сервере выполните:"
echo "sudo bash /root/setup_logging_main.sh"
