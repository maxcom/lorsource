# Не собирается с помощью maven:3.6-jdk-8-slim, поэтому используем 11.
FROM maven:3.6-openjdk-17-slim as builder

RUN apt-get update && apt-get upgrade -y

RUN adduser --system --shell /bin/false --home /opt/lorsource lorsource
USER lorsource
WORKDIR /opt/lorsource

# Кэшируем зависимости Maven.
COPY pom.xml .
RUN mvn dependency:resolve dependency:resolve-plugins \
    && rm pom.xml

COPY . .
COPY docker/config.properties src/main/webapp/WEB-INF/config.properties
COPY docker/liquibase.config sql/liquibase.config
RUN mvn package

COPY docker/docker-entrypoint.sh /opt/docker-entrypoint.sh
ENTRYPOINT ["/opt/docker-entrypoint.sh"]

FROM tomcat:8.5-jdk17-openjdk-slim

COPY --from=builder /opt/lorsource/target/lor-1.0-SNAPSHOT /usr/local/tomcat/webapps/ROOT