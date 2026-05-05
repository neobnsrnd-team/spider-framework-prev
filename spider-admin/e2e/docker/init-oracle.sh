#!/bin/bash
# Oracle E2E init script — grants privileges, then executes SQL files as APP_USER.
#
# Used in three contexts:
#   1. Local docker-compose: mounted to /container-entrypoint-initdb.d/01_init.sh
#      SQL files are in /e2e-init/ → this script executes them manually.
#   2. CI setup-oracle-free: placed alongside SQL files in startup-scripts dir.
#      SQL files use .data extension (not .sql) to prevent entrypoint auto-execution
#      as SYS. This script executes them as APP_USER instead.
#   3. Direct invocation: SQL files in same directory as this script.

CONN="system/${ORACLE_PASSWORD}@//localhost:1521/FREEPDB1"

echo "=== E2E: Granting privileges to ${APP_USER} ==="
sqlplus -s "$CONN" <<EOSQL
GRANT CREATE TABLE TO ${APP_USER};
GRANT UNLIMITED TABLESPACE TO ${APP_USER};
EOSQL

# Determine SQL file directory:
#   - docker-compose: /e2e-init/ (volume mount)
#   - CI: same directory as this script (.data files)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]:-$0}")" && pwd)"
APP_CONN="${APP_USER}/${APP_USER_PASSWORD}@//localhost:1521/FREEPDB1"

if [ -d /e2e-init ]; then
  # Local docker-compose context
  for sqlfile in /e2e-init/*.sql; do
    [ -f "$sqlfile" ] || continue
    echo "=== E2E: Running $(basename "$sqlfile") as ${APP_USER} ==="
    sed 's/\r$//' "$sqlfile" | sqlplus -s "$APP_CONN"
  done
else
  # CI context: SQL files renamed to .data to avoid SYS auto-execution
  for datafile in "$SCRIPT_DIR"/*.data; do
    [ -f "$datafile" ] || continue
    echo "=== E2E: Running $(basename "$datafile") as ${APP_USER} ==="
    sed 's/\r$//' "$datafile" | sqlplus -s "$APP_CONN"
  done
fi

echo "=== E2E: Init complete ==="
