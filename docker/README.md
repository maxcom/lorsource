# Разработка с помощью контейнера

## Зависимости
- [Docker](https://www.docker.com/) (или [Podman](https://podman.io/) — не протестировано)
- [docker-compose](https://docs.docker.com/compose/) (или [podman-compose](https://github.com/containers/podman-compose) — не протестировано)

## Шаги
Среда разработки состоит из трёх контейнеров: development-версии, production-версии и базы данных. Для функционирования
production-версии требуется сборка development-версии, с помощью которой производятся миграции базы данных и откуда копируется
собранный движок. Оба контейнера используют одну и ту же базу данных с тестовыми данными.

### Development-версия с Jetty
- Склонируйте репозиторий и перейдите в директорию `docker/`.
- Соберите движок. `docker-compose -f docker-compose.dev.yml build web`
- Запустите базу данных. `docker-compose -f docker-compose.dev.yml up -d db`
- Запустите скрипт с остальными действиями над базой данных. `docker-compose -f docker-compose.dev.yml exec db /opt/setup_db.sh`
- Запустите движок. `docker-compose -f docker-compose.dev.yml up web`

Миграции базы данных будут совершены автоматически. Сервер будет доступен по адресу `http://localhost:8080`.

### Production-версия с Tomcat
Напоминаем, что для работы этого контейнера вам нужна собранная development-версия и база данных с уже совершёнными миграциями.

- Остановите работающий development-контейнер.
- Соберите движок. `docker-compose build web`
- Запустите движок. `docker-compose up web`

Сервер будет доступен по адресу `http://localhost:8080`.