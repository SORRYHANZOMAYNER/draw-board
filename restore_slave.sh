#!/bin/bash

set -e

bash /root/prepare_slave.sh
bash /root/makedb_slave.sh
bash /root/setup_monitoring_slave.sh
bash /root/install_cron.sh

sleep 5
mysql -uroot -e "SHOW REPLICA STATUS\G"

echo "Slave-сервер восстановлен"
echo "Проверьте Replica_IO_Running и Replica_SQL_Running: оба значения должны быть Yes"
