# Dev Container

Окружение для разработки на базе Docker Compose. Включает:

- Java + Maven
- PostgreSQL
- OpenSearch

## Требования

- [Docker](https://docs.docker.com/get-docker/)
- [devcontainer CLI](https://github.com/devcontainers/cli): `npm install -g @devcontainers/cli`

Или VS Code с расширением [Dev Containers](https://marketplace.visualstudio.com/items?itemName=ms-vscode-remote.remote-containers).

## Инициализация

```bash
devcontainer up
```

При старте автоматически выполняется `init-db.sh`:

1. Ждёт готовности PostgreSQL
2. Создаёт пользователей `maxcom`, `linuxweb`, `jamwiki`
3. Пересоздаёт базу `lor` из `sql/demo.db`
4. Накатывает миграции через `mvn liquibase:update`
5. Создаёт `src/main/webapp/WEB-INF/config.properties`

## Вход в контейнер

```bash
devcontainer exec bash
```

## Запуск тестов

Юнит-тесты:

```bash
devcontainer exec mvn test
```

Интеграционные тесты:

```bash
devcontainer exec mvn verify
```

## Запуск dev server

```bash
devcontainer exec mvn -DskipTests package jetty:run-war
```

Сервер доступен на хосте по адресу: **http://127.0.0.1:8080/**. Остановка через Ctrl-C.

В БД пароли всех пользователей установлены в `passwd`.

## Пересборка окружения

Пересоздать контейнеры с нуля (база сбрасывается и инициализируется заново):

```bash
devcontainer up --workspace-folder . --remove-existing-container
```
