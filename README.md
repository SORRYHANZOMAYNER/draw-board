# Развёртывание и восстановление Drawboard

## Что проверено

Полная установка main-сервера проверена 13 октября 2026 года на чистой Ubuntu 26.04.1 LTS:

- VM: 2 CPU, 3,8 ГБ RAM, диск 25 ГБ;
- скрипт добавил 2 ГБ swap для Elastic Stack;
- установлены Git, Docker, Docker Compose, MySQL и cron;
- созданы база `board`, пользователь приложения, пользователь репликации, GTID и binlog;
- собраны и запущены `drawboard-backend-1`, `drawboard-backend-2` и `drawboard-frontend`;
- главная страница вернула HTTP 200, а `/api/auth` дошёл до Spring и без авторизации вернул ожидаемый HTTP 403;
- Hibernate создал таблицы `board_templates`, `drawing_events`, `room`, `userat`;
- Prometheus, Node Exporter и Grafana вернули HTTP 200;
- Elasticsearch, Logstash, Kibana и Filebeat запустились;
- Filebeat передал Docker-логи, в Elasticsearch появился индекс `drawboard-logs-*`.

На тестовой VM использовался адрес `10.241.29.73` и режим `local`: сайт работал по HTTP без сертификата. HTTPS нельзя полноценно проверить на локальном IP — для него нужен публичный домен, направленный на сервер.

Slave, репликация и восстановление из реального бэкапа на этой VM не проверялись, потому что для них нужна вторая машина.

## Репозитории

По принятому порядку ссылок используются:

- backend: `https://github.com/SORRYHANZOMAYNER/2.git`;
- frontend: `https://github.com/SORRYHANZOMAYNER/1.git`.

На момент проверки оба репозитория пустые. Перед запуском `deploy_main.sh` в них обязательно нужно отправить проекты. Нельзя добавлять в Git файл `/root/drawboard.env`, пароли, токены, приватные ключи и `.deb`-пакеты.

В обоих проектах есть простой `setup_git_projects.sh`. Пример из Git Bash на Windows:

```bash
cd /c/Users/egor.aksenov/IdeaProjects/board/board
bash ./setup_git_projects.sh https://github.com/SORRYHANZOMAYNER/2.git "Первый коммит backend"

cd /c/Users/egor.aksenov/Desktop/frontend-board
bash ./setup_git_projects.sh https://github.com/SORRYHANZOMAYNER/1.git "Первый коммит frontend"
```

Если `origin` уже существует, сначала проверить его командой `git remote -v` и при необходимости заменить:

```bash
git remote set-url origin НОВАЯ_ССЫЛКА
```

## Схема системы

Main-сервер содержит:

- MySQL с базой `board`;
- два контейнера Spring Boot;
- контейнер Nginx с фронтендом и балансировкой между двумя бэкендами;
- Prometheus и Node Exporter как systemd-службы;
- Grafana в Docker-контейнере;
- Elasticsearch, Logstash, Kibana и Filebeat как systemd-службы.

Slave-сервер содержит:

- MySQL-реплику main;
- Node Exporter;
- ежедневный потабличный бэкап в `/root/mysql-backups`.

## Требования

Для учебной демонстрации требуется не меньше:

- 2 CPU;
- 4 ГБ RAM;
- 25 ГБ диска;
- Ubuntu 24.04 или 26.04;
- доступ в интернет для Ubuntu, Docker Hub и GitHub.

Elastic Stack заметно нагружает маленькую VM. `setup_logging_main.sh` автоматически создаёт swap-файл 2 ГБ, если swap отсутствует. На проверенной VM с 4 ГБ RAM swap оказался почти полностью занят, поэтому для постоянной работы лучше выделить 8 ГБ RAM. После установки и очистки временных файлов на диске 25 ГБ осталось около 7 ГБ.

Все команды на сервере выполняются через `sudo`. Для удобства можно один раз открыть root-shell:

```bash
sudo -i
```

## Основные скрипты

- `prepare_main.sh` — ставит системные пакеты и создаёт Docker-сеть;
- `deploy_main.sh` — первая установка main без восстановления бэкапа;
- `makedb.sh` — настраивает основной MySQL;
- `makeback.sh` — запускает два экземпляра бэкенда;
- `makefrnt.sh` — запускает фронтенд;
- `setup_monitoring_main.sh` — ставит Prometheus, Node Exporter и Grafana;
- `send_logging_packages.sh` — отправляет четыре локальных Elastic-пакета;
- `setup_logging_main.sh` — ставит и настраивает Elastic Stack;
- `backup_db.sh` — делает на slave отдельный архив каждой таблицы;
- `install_cron.sh` — запускает бэкап ежедневно в 03:00;
- `restore_main.sh` — восстанавливает main из Git и бэкапа;
- `restore_slave.sh` — настраивает чистый slave.

Все `.sh` зафиксированы в Git с окончаниями строк LF и предназначены для Linux.

## Первая установка main на чистую VM

### 1. Подготовить SSH и firewall

На VM должен работать SSH. Не закрывать текущую сессию, пока не проверен новый вход.

```bash
sudo systemctl enable --now ssh
sudo ufw allow 22/tcp
sudo ufw enable
```

### 2. Отправить управляющие скрипты

Из Git Bash на Windows:

```bash
cd /c/Users/egor.aksenov/IdeaProjects/board/board
scp prepare_main.sh deploy_main.sh convey99@IP_MAIN:/home/convey99/
```

На VM:

```bash
sudo cp /home/convey99/prepare_main.sh /home/convey99/deploy_main.sh /root/
sudo chmod +x /root/prepare_main.sh /root/deploy_main.sh
```

### 3. Заранее отправить Elastic-пакеты

На Windows в `Downloads` должны находиться четыре выданных файла версии 8.17.1. Скрипт переименует их в короткие имена, положит во временный каталог, затем через `sudo` переместит в `/root/logging-packages`.

```bash
cd /c/Users/egor.aksenov/IdeaProjects/board/board
bash ./send_logging_packages.sh convey99@IP_MAIN /c/Users/egor.aksenov/Downloads
```

Пакеты занимают примерно 1,4 ГБ. После успешной установки они удаляются с сервера.

### 4. Запустить локальную установку

Для VM без публичного домена:

```bash
sudo bash /root/deploy_main.sh \
  https://github.com/SORRYHANZOMAYNER/2.git \
  https://github.com/SORRYHANZOMAYNER/1.git \
  local
```

После завершения сайт доступен по адресу `http://IP_MAIN`.

### 5. Запустить production-установку с HTTPS

До запуска DNS-имя должно указывать на публичный IP main, а входящие порты 80 и 443 должны быть доступны.

```bash
sudo bash /root/deploy_main.sh \
  https://github.com/SORRYHANZOMAYNER/2.git \
  https://github.com/SORRYHANZOMAYNER/1.git \
  production \
  drawboard.duckdns.org \
  egoraksenov821@gmail.com
```

`deploy_main.sh` рассчитан именно на чистый сервер. Если `/root/board-backend` или `/root/board-frontend` уже содержит файлы, скрипт остановится и ничего не удалит.

## Что делает deploy_main.sh

1. Устанавливает Git, Docker, Docker Compose, MySQL, cron, curl и OpenSSL.
2. Создаёт `/root/drawboard.env` со случайными `DB_PASS`, `REPL_PASS`, `JWT_SECRET` и правами 600.
3. Клонирует backend и frontend.
4. Настраивает MySQL, GTID, binlog и firewall для фактической Docker-подсети.
5. Собирает и запускает два бэкенда.
6. В режиме `local` использует HTTP-конфиг Nginx; в режиме `production` получает сертификат Let's Encrypt.
7. Собирает и запускает фронтенд.
8. Открывает необходимые порты UFW.
9. Устанавливает мониторинг.
10. Устанавливает логирование, если в `/root/logging-packages` лежат четыре пакета.
11. Удаляет временные архивы, Docker build-cache и кэш APT.

## Проверка main

Проверить контейнеры:

```bash
sudo docker ps
```

Должны работать `drawboard-backend-1`, `drawboard-backend-2`, `drawboard-frontend` и `grafana`.

Проверить базу и сайт:

```bash
sudo mysql -e "SHOW DATABASES; USE board; SHOW TABLES;"
curl -I http://127.0.0.1/
curl -i http://127.0.0.1/api/auth
```

Для `/api/auth` без токена нормален ответ 401 или 403. Ответ 502 означает, что Nginx не видит бэкенд.

Проверить мониторинг:

```bash
curl -f http://127.0.0.1:9100/metrics >/dev/null
curl -f http://127.0.0.1:9090/-/ready
curl -I http://127.0.0.1:3000/login
```

Интерфейсы:

- Grafana: `http://IP_MAIN:3000`;
- Prometheus: `http://IP_MAIN:9090`;
- Targets: `http://IP_MAIN:9090/targets`.

Адрес slave для Prometheus по умолчанию равен `81.90.28.225`. Другой адрес можно передать так:

```bash
sudo SLAVE_IP=IP_SLAVE bash /root/board-backend/setup_monitoring_main.sh
```

## Проверка логирования

```bash
sudo systemctl is-active elasticsearch logstash kibana filebeat
curl -f http://127.0.0.1:9200
curl -f http://127.0.0.1:5601/api/status
curl 'http://127.0.0.1:9200/_cat/indices/drawboard-logs-*?v'
```

Путь лога: `Docker JSON logs -> Filebeat -> Logstash:5400 -> Elasticsearch -> Kibana`.

Elasticsearch и Kibana слушают только `127.0.0.1`; открывать 9200 и 5601 в UFW не нужно. HTTPS-сертификат сайта им также не нужен. Kibana открывается через SSH-туннель:

```bash
ssh -L 5601:127.0.0.1:5601 convey99@IP_MAIN
```

Пока SSH открыт, перейти в браузере на `http://127.0.0.1:5601`. Представление данных называется `Drawboard logs` и соответствует индексу `drawboard-logs-*`.

## Подготовка slave

С backend-проекта отправить файлы:

```bash
scp prepare_slave.sh makedb_slave.sh backup_db.sh restore_db.sh install_cron.sh setup_monitoring_slave.sh restore_slave.sh convey99@IP_SLAVE:/home/convey99/
```

На slave:

```bash
sudo cp /home/convey99/*.sh /root/
sudo chmod +x /root/*.sh
sudo MASTER_HOST=IP_MAIN bash /root/restore_slave.sh
```

На main разрешить подключение MySQL только с IP slave:

```bash
sudo ufw allow from IP_SLAVE to any port 3306 proto tcp
```

Проверить репликацию на slave:

```bash
sudo mysql -e "SHOW REPLICA STATUS\G"
```

Нормальное состояние:

- `Replica_IO_Running: Yes`;
- `Replica_SQL_Running: Yes`;
- `Last_IO_Error` и `Last_SQL_Error` пустые;
- `Seconds_Behind_Source` равен 0 или временно содержит небольшое значение.

## Бэкап на slave

Ручная проверка:

```bash
sudo bash /root/backup_db.sh
sudo find /root/mysql-backups -type f
```

`backup_db.sh` проходит по всем пользовательским базам и сохраняет каждую таблицу отдельным файлом `.sql.gz`. Также сохраняется `replication_status.txt`. Копии старше семи дней удаляются.

Настроить ежедневный запуск:

```bash
sudo bash /root/install_cron.sh
cat /etc/cron.d/mysql-backup
sudo systemctl status cron
```

Лог cron:

```bash
sudo tail -n 100 /var/log/mysql-backup.log
```

Хотя бы одну копию нужно регулярно переносить за пределы обеих VM. Если потеряны main и slave вместе, копия только на slave не поможет.

## Аварийное восстановление main

На чистую VM передать `prepare_main.sh` и `restore_main.sh`, а также последний каталог бэкапа. При необходимости заранее передать Elastic-пакеты.

Локальный режим:

```bash
sudo bash /root/restore_main.sh \
  https://github.com/SORRYHANZOMAYNER/2.git \
  https://github.com/SORRYHANZOMAYNER/1.git \
  /root/mysql-restore/2026-10-13_03-00-00 \
  local
```

Production с HTTPS:

```bash
sudo bash /root/restore_main.sh \
  https://github.com/SORRYHANZOMAYNER/2.git \
  https://github.com/SORRYHANZOMAYNER/1.git \
  /root/mysql-restore/2026-10-13_03-00-00 \
  production \
  drawboard.duckdns.org \
  egoraksenov821@gmail.com
```

Скрипт клонирует проекты, создаёт новый файл секретов, настраивает MySQL, восстанавливает все таблицы, запускает приложение, мониторинг и логирование.

## Важные замечания

- Первый запуск занимает заметно больше времени: загружаются JDK, Maven, Node, Nginx, Grafana и зависимости npm.
- Инициализация MySQL на VM с медленным диском может занимать около минуты после каждого изменения конфигурации.
- `setup_monitoring_main.sh` использует Docker-образ Grafana, потому что `dl.grafana.com` на тестовой VM не отвечал.
- Сборка frontend сейчас сообщает о зависимостях npm с известными уязвимостями. Не запускать `npm audit fix --force` без отдельной проверки приложения: команда может внести несовместимые обновления.
- Если PowerShell `Invoke-WebRequest` использует корпоративный прокси и не открывает локальную VM, проверить напрямую: `curl.exe --noproxy "*" http://IP_MAIN/`.
- Файл `/root/drawboard.env` нельзя публиковать. Для резервного восстановления его нужно хранить отдельно в защищённом месте либо принять, что на новой машине будут созданы новые пароли и JWT-секрет.

## Контрольный список

1. Репозитории backend и frontend не пустые и не содержат секретов.
2. Сайт отвечает по HTTP/HTTPS.
3. Оба бэкенда имеют статус `Up`.
4. В базе `board` есть четыре таблицы.
5. Prometheus, Node Exporter и Grafana отвечают.
6. Elasticsearch, Logstash, Kibana и Filebeat активны.
7. Индекс `drawboard-logs-*` существует и содержит документы.
8. На slave оба потока репликации имеют статус `Yes`.
9. Ручной и cron-бэкап создаются.
10. Последний бэкап хранится ещё и вне двух серверов.
