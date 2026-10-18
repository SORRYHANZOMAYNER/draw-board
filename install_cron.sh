#!/bin/bash

echo '0 3 * * * root bash /root/backup_db.sh >> /var/log/mysql-backup.log 2>&1' > /etc/cron.d/mysql-backup
chmod 644 /etc/cron.d/mysql-backup
systemctl enable --now cron

echo "Автоматический бэкап настроен на 03:00"
