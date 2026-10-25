#!/bin/bash

set -e

MAIN_IP="$1"
SLAVE_IP="$2"

bash /root/prepare_slave.sh
bash /root/makedb_slave.sh "$MAIN_IP"
bash /root/setup_monitoring_slave.sh "$MAIN_IP" "$SLAVE_IP"
bash /root/install_cron.sh

sleep 5
mysql -uroot -e "SHOW REPLICA STATUS\G"

echo "Slave-сервер восстановлен"
