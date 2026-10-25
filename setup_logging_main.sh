#!/usr/bin/env bash

set -e

PACKAGE_DIR="${1:-/root/logging-packages}"

ELASTICSEARCH_DEB="$PACKAGE_DIR/elasticsearch_8.17.1_amd64.deb"
KIBANA_DEB="$PACKAGE_DIR/kibana_8.17.1_amd64.deb"
LOGSTASH_DEB="$PACKAGE_DIR/logstash_8.17.1_amd64.deb"
FILEBEAT_DEB="$PACKAGE_DIR/filebeat_8.17.1_amd64.deb"

if [ ! -f "$ELASTICSEARCH_DEB" ] || [ ! -f "$KIBANA_DEB" ] || [ ! -f "$LOGSTASH_DEB" ] || [ ! -f "$FILEBEAT_DEB" ]; then
  echo "Не найдены пакеты в $PACKAGE_DIR"
  exit 1
fi

if ! swapon --show | grep -q .; then
  fallocate -l 2G /swapfile
  chmod 600 /swapfile
  mkswap /swapfile
  swapon /swapfile
  echo '/swapfile none swap sw 0 0' >> /etc/fstab
fi

apt update
apt install -y curl "$ELASTICSEARCH_DEB" "$KIBANA_DEB" "$LOGSTASH_DEB" "$FILEBEAT_DEB"

cat > /etc/sysctl.d/99-elasticsearch.conf <<'EOF'
vm.max_map_count=262144
EOF
sysctl --system >/dev/null

mkdir -p /etc/elasticsearch/jvm.options.d
cat > /etc/elasticsearch/jvm.options.d/heap.options <<'EOF'
-Xms512m
-Xmx512m
EOF

cat > /etc/elasticsearch/elasticsearch.yml <<'EOF'
cluster.name: drawboard-logs
node.name: elk-main
path.data: /var/lib/elasticsearch
path.logs: /var/log/elasticsearch
network.host: 127.0.0.1
http.port: 9200
discovery.type: single-node
xpack.security.enabled: false
xpack.security.enrollment.enabled: false
xpack.security.http.ssl.enabled: false
xpack.security.transport.ssl.enabled: false
EOF

chown root:elasticsearch /etc/elasticsearch/elasticsearch.yml
chmod 660 /etc/elasticsearch/elasticsearch.yml

cat > /etc/kibana/kibana.yml <<'EOF'
server.port: 5601
server.host: "127.0.0.1"
elasticsearch.hosts: ["http://127.0.0.1:9200"]
EOF

chown root:kibana /etc/kibana/kibana.yml
chmod 660 /etc/kibana/kibana.yml

mkdir -p /etc/systemd/system/kibana.service.d
cat > /etc/systemd/system/kibana.service.d/memory.conf <<'EOF'
[Service]
Environment="NODE_OPTIONS=--max-old-space-size=512"
EOF

mkdir -p /etc/logstash/jvm.options.d /etc/logstash/conf.d
cat > /etc/logstash/jvm.options.d/heap.options <<'EOF'
-Xms256m
-Xmx256m
EOF

mkdir -p /etc/systemd/system/logstash.service.d
cat > /etc/systemd/system/logstash.service.d/memory.conf <<'EOF'
[Service]
Environment="LS_JAVA_OPTS=-Xms256m -Xmx256m"
EOF

cat > /etc/logstash/logstash.yml <<'EOF'
path.data: /var/lib/logstash
path.config: /etc/logstash/conf.d
path.logs: /var/log/logstash
pipeline.workers: 1
pipeline.batch.size: 50
EOF

cat > /etc/logstash/conf.d/drawboard.conf <<'EOF'
input {
  beats {
    host => "127.0.0.1"
    port => 5400
  }
}

output {
  elasticsearch {
    hosts => ["http://127.0.0.1:9200"]
    index => "drawboard-logs-%{+YYYY.MM.dd}"
  }
}
EOF

chown -R root:logstash /etc/logstash/logstash.yml /etc/logstash/conf.d /etc/logstash/jvm.options.d
chmod 640 /etc/logstash/logstash.yml /etc/logstash/conf.d/drawboard.conf /etc/logstash/jvm.options.d/heap.options

mkdir -p /var/lib/logstash/queue /var/lib/logstash/dead_letter_queue /var/log/logstash
chown -R logstash:logstash /var/lib/logstash /var/log/logstash

cat > /etc/filebeat/filebeat.yml <<'EOF'
filebeat.inputs:
  - type: filestream
    id: docker-logs
    enabled: true
    paths:
      - /var/lib/docker/containers/*/*.log
    parsers:
      - container:
          stream: all
          format: docker

output.logstash:
  hosts: ["127.0.0.1:5400"]

processors:
  - add_host_metadata: ~
EOF

chown root:root /etc/filebeat/filebeat.yml
chmod 600 /etc/filebeat/filebeat.yml

runuser -u logstash -- /usr/share/logstash/bin/logstash --path.settings /etc/logstash --config.test_and_exit -f /etc/logstash/conf.d/drawboard.conf
filebeat test config -e

chown -R logstash:logstash /var/lib/logstash /var/log/logstash

systemctl daemon-reload
systemctl enable elasticsearch logstash kibana filebeat
systemctl restart elasticsearch

for i in $(seq 1 60); do
  if curl -fsS http://127.0.0.1:9200 >/dev/null; then
    break
  fi
  sleep 2
done

curl -fsS http://127.0.0.1:9200 >/dev/null

systemctl restart logstash
systemctl restart kibana

for i in $(seq 1 90); do
  if ss -ltn | grep -q ':5400 '; then
    break
  fi
  sleep 2
done

ss -ltn | grep -q ':5400 '

for i in $(seq 1 180); do
  if curl -fsS http://127.0.0.1:5601/api/status >/dev/null; then
    break
  fi
  sleep 2
done

curl -fsS http://127.0.0.1:5601/api/status >/dev/null
curl -fsS -X POST http://127.0.0.1:5601/api/data_views/data_view \
  -H 'kbn-xsrf: true' \
  -H 'Content-Type: application/json' \
  -d '{"data_view":{"title":"drawboard-logs-*","name":"Drawboard logs","timeFieldName":"@timestamp"}}' \
  >/dev/null || true

sleep 20
filebeat test output
systemctl restart filebeat

systemctl is-active --quiet elasticsearch
systemctl is-active --quiet logstash
systemctl is-active --quiet kibana
systemctl is-active --quiet filebeat

rm -f "$ELASTICSEARCH_DEB" "$KIBANA_DEB" "$LOGSTASH_DEB" "$FILEBEAT_DEB"
apt clean

echo "Elasticsearch: http://127.0.0.1:9200"
echo "Kibana: http://127.0.0.1:5601"
echo "Откройте SSH-туннель: ssh -L 5601:127.0.0.1:5601 ${SUDO_USER:-root}@$(hostname -I | awk '{print $1}')"

