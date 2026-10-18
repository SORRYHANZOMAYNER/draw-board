#!/bin/bash

set -e

NODE_EXPORTER_VERSION=1.6.1
MAIN_IP=138.124.121.179

apt update
apt install -y curl tar

cd /tmp
curl -LO https://github.com/prometheus/node_exporter/releases/download/v$NODE_EXPORTER_VERSION/node_exporter-$NODE_EXPORTER_VERSION.linux-amd64.tar.gz
tar xzf node_exporter-$NODE_EXPORTER_VERSION.linux-amd64.tar.gz

useradd --no-create-home --shell /usr/sbin/nologin node_exporter 2>/dev/null || true
install -m 0755 node_exporter-$NODE_EXPORTER_VERSION.linux-amd64/node_exporter /usr/local/bin/node_exporter
chown node_exporter:node_exporter /usr/local/bin/node_exporter

cat > /etc/systemd/system/node_exporter.service <<'EOF'
[Unit]
Description=Node Exporter
Wants=network-online.target
After=network-online.target

[Service]
User=node_exporter
Group=node_exporter
Type=simple
ExecStart=/usr/local/bin/node_exporter
Restart=on-failure

[Install]
WantedBy=multi-user.target
EOF

systemctl daemon-reload
systemctl enable node_exporter
systemctl restart node_exporter

if command -v ufw >/dev/null 2>&1 && ufw status | grep -q 'Status: active'; then
  ufw allow from $MAIN_IP to any port 9100 proto tcp
fi

curl -f http://127.0.0.1:9100/metrics > /dev/null
echo "Node Exporter: http://81.90.28.225:9100"
