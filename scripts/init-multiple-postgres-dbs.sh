#!/bin/bash
# Creates one database per StreamPulse service on first container start, per the
# POSTGRES_MULTIPLE_DATABASES env var (comma-separated) set in docker-compose.yml.
# Each service still only ever connects to its own database (docs/CONTRACT.md) — this
# single Postgres instance is a dev-compose convenience, not shared access.
set -euo pipefail

if [ -n "${POSTGRES_MULTIPLE_DATABASES:-}" ]; then
    IFS=',' read -ra DATABASES <<< "$POSTGRES_MULTIPLE_DATABASES"
    for db in "${DATABASES[@]}"; do
        echo "Creating database: $db"
        psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
            SELECT 'CREATE DATABASE "$db"' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = '$db')\gexec
EOSQL
    done
fi
