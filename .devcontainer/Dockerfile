FROM maven:3.9.14-eclipse-temurin-25

# libatomic1: the Node binary that runs the JS tests (downloaded by frontend-maven-plugin) is linked
# against libatomic.so.1, which the base image does not ship
RUN apt-get update && apt-get install -y --no-install-recommends \
    postgresql-client \
    git \
    curl \
    libatomic1 \
    && rm -rf /var/lib/apt/lists/*
