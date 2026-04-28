FROM maven:3.9.14-eclipse-temurin-25

RUN apt-get update && apt-get install -y --no-install-recommends \
    postgresql-client \
    git \
    curl \
    && rm -rf /var/lib/apt/lists/*
