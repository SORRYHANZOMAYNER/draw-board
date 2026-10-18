#!/bin/bash

set -e

PROMETHEUS_VERSION=2.46.0
NODE_EXPORTER_VERSION=1.6.1
SLAVE_IP="${SLAVE_IP:-81.90.28.225}"
SERVER_IP=$(hostname -I | awk '{print $1}')

cleanup() {
  rm -rf \
    "/tmp/prometheus-$PROMETHEUS_VERSION.linux-amd64.tar.gz" \
    "/tmp/prometheus-$PROMETHEUS_VERSION.linux-amd64" \
    "/tmp/node_exporter-$NODE_EXPORTER_VERSION.linux-amd64.tar.gz" \
    "/tmp/node_exporter-$NODE_EXPORTER_VERSION.linux-amd64"
  apt clean
}

trap cleanup EXIT

apt update
apt install -y curl tar

cd /tmp
curl -LO https://github.com/prometheus/prometheus/releases/download/v$PROMETHEUS_VERSION/prometheus-$PROMETHEUS_VERSION.linux-amd64.tar.gz
curl -LO https://github.com/prometheus/node_exporter/releases/download/v$NODE_EXPORTER_VERSION/node_exporter-$NODE_EXPORTER_VERSION.linux-amd64.tar.gz

tar xzf prometheus-$PROMETHEUS_VERSION.linux-amd64.tar.gz
tar xzf node_exporter-$NODE_EXPORTER_VERSION.linux-amd64.tar.gz

useradd --no-create-home --shell /usr/sbin/nologin prometheus 2>/dev/null || true
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

mkdir -p /etc/prometheus /var/lib/prometheus
install -m 0755 prometheus-$PROMETHEUS_VERSION.linux-amd64/prometheus /usr/local/bin/prometheus
install -m 0755 prometheus-$PROMETHEUS_VERSION.linux-amd64/promtool /usr/local/bin/promtool
rm -rf /etc/prometheus/consoles /etc/prometheus/console_libraries
cp -r prometheus-$PROMETHEUS_VERSION.linux-amd64/consoles /etc/prometheus/
cp -r prometheus-$PROMETHEUS_VERSION.linux-amd64/console_libraries /etc/prometheus/
chown -R prometheus:prometheus /etc/prometheus /var/lib/prometheus
chown prometheus:prometheus /usr/local/bin/prometheus /usr/local/bin/promtool

cat > /etc/systemd/system/prometheus.service <<'EOF'
[Unit]
Description=Prometheus Monitoring
Wants=network-online.target
After=network-online.target

[Service]
User=prometheus
Group=prometheus
Type=simple
ExecStart=/usr/local/bin/prometheus \
  --config.file=/etc/prometheus/prometheus.yml \
  --storage.tsdb.path=/var/lib/prometheus \
  --web.console.templates=/etc/prometheus/consoles \
  --web.console.libraries=/etc/prometheus/console_libraries
ExecReload=/bin/kill -HUP $MAINPID
Restart=on-failure

[Install]
WantedBy=multi-user.target
EOF

cat > /etc/prometheus/prometheus.yml <<EOF
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: prometheus
    static_configs:
      - targets: ['127.0.0.1:9090']

  - job_name: node_exporter
    static_configs:
      - targets: ['127.0.0.1:9100']
        labels:
          server: main
      - targets: ['$SLAVE_IP:9100']
        labels:
          server: slave
EOF

chown prometheus:prometheus /etc/prometheus/prometheus.yml

mkdir -p /etc/grafana/provisioning/datasources
cat > /etc/grafana/provisioning/datasources/prometheus.yml <<'EOF'
apiVersion: 1

datasources:
  - name: Prometheus
    type: prometheus
    access: proxy
    url: http://127.0.0.1:9090
    isDefault: true
    editable: true
EOF

docker volume create grafana-storage
docker rm -f grafana 2>/dev/null || true
docker run -d \
  --name grafana \
  --network host \
  --restart unless-stopped \
  -v grafana-storage:/var/lib/grafana \
  -v /etc/grafana/provisioning/datasources/prometheus.yml:/etc/grafana/provisioning/datasources/prometheus.yml:ro \
  grafana/grafana:12.4.0

systemctl daemon-reload
systemctl enable node_exporter
systemctl enable prometheus
systemctl restart node_exporter
systemctl restart prometheus

curl --retry 30 --retry-connrefused --retry-delay 1 -f http://127.0.0.1:9100/metrics > /dev/null
curl --retry 30 --retry-connrefused --retry-delay 1 -f http://127.0.0.1:9090/-/ready > /dev/null
curl --retry 30 --retry-connrefused --retry-delay 1 -f http://127.0.0.1:3000/login > /dev/null

echo "Prometheus: http://$SERVER_IP:9090"
echo "Grafana: http://$SERVER_IP:3000"
