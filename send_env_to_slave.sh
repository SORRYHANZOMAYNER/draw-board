#!/bin/bash

set -e

SLAVE_USER="$1"
SLAVE_IP="$2"

if [ -z "$SLAVE_USER" ] || [ -z "$SLAVE_IP" ]; then
  echo "Использование: $0 <пользователь_slave> <ip_slave>"
  exit 1
fi

scp /root/drawboard.env "${SLAVE_USER}@${SLAVE_IP}:/tmp/drawboard.env"
ssh -t "${SLAVE_USER}@${SLAVE_IP}" "sudo install -o root -g root -m 600 /tmp/drawboard.env /root/drawboard.env && rm -f /tmp/drawboard.env"

echo "Файл /root/drawboard.env передан на slave"
