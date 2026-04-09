#!/bin/bash
set -e

PGHOST="${PGHOST:-postgres}"
PGUSER="${PGUSER:-postgres}"
PGPASSWORD="${PGPASSWORD:-postgres}"

export PGHOST PGUSER PGPASSWORD

echo "Waiting for PostgreSQL to be ready..."
until pg_isready -h "$PGHOST" -U "$PGUSER"; do
  sleep 1
done

echo "Initializing database..."

# Create users (ignore errors if they already exist)
createuser -d -S -R maxcom 2>/dev/null || echo "User maxcom already exists"
createuser -D -S -R linuxweb 2>/dev/null || echo "User linuxweb already exists"
createuser -D -S -R jamwiki 2>/dev/null || echo "User jamwiki already exists"

psql -c "ALTER USER maxcom WITH PASSWORD 'maxcom'" -U postgres template1
psql -c "ALTER USER linuxweb WITH PASSWORD 'linuxweb'" -U postgres template1

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
WORKSPACE_DIR="$(dirname "$SCRIPT_DIR")"

# Drop and recreate database to always have a fresh schema from demo.db
psql -c "DROP DATABASE IF EXISTS lor;" -U postgres
PGPASSWORD=maxcom createdb -U maxcom lor

psql -c 'create extension hstore;' -U postgres lor
psql -c 'create extension fuzzystrmatch;' -U postgres lor

PGPASSWORD=maxcom psql -f "$WORKSPACE_DIR/sql/demo.db" -U maxcom lor

echo "Running Liquibase migrations..."
mvn -f "$WORKSPACE_DIR/pom.xml" --batch-mode liquibase:update

echo "Database initialization complete."

# Create config.properties for devcontainer if not present
CONFIG="$WORKSPACE_DIR/src/main/webapp/WEB-INF/config.properties"
if [ ! -f "$CONFIG" ]; then
  echo "Creating $CONFIG for devcontainer..."
  cat > "$CONFIG" <<'EOF'
Elasticsearch=http://opensearch:9200
EOF
fi
