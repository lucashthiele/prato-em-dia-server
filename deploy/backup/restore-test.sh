#!/bin/sh
# Teste de restauração: baixa o último dump, decifra, restaura num Postgres DESCARTÁVEL e confere
# o Flyway e a contagem de linhas. O resultado vai para o Discord. Roda onde a chave privada existe
# (a máquina do responsável), nunca na VPS de produção.
#
# Variáveis: BACKUP_SOURCE (ex.: r2:prato-backups/daily), AGE_IDENTITY_FILE (chave privada),
# PGHOST/PGUSER/PGPASSWORD do Postgres descartável, DISCORD_WEBHOOK_URL.
set -eu
. /usr/local/bin/lib.sh

: "${BACKUP_SOURCE:?}" "${AGE_IDENTITY_FILE:?}" "${PGHOST:?}"
CHECK_DB="restore_check"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"; psql -d postgres -qc "DROP DATABASE IF EXISTS $CHECK_DB" >/dev/null 2>&1 || true' EXIT

fail() {
  echo "teste de restauração falhou: $1" >&2
  notify "Teste de restauração do backup FALHOU: $1"
  exit 1
}

LATEST="$(rclone lsf --files-only "$BACKUP_SOURCE" | sort | tail -1)"
[ -n "$LATEST" ] || fail "nenhum backup encontrado em $BACKUP_SOURCE"
rclone copyto "$BACKUP_SOURCE/$LATEST" "$TMP/$LATEST" || fail "download de $LATEST"
age --decrypt --identity "$AGE_IDENTITY_FILE" --output "$TMP/dump" "$TMP/$LATEST" || fail "decifrar $LATEST (chave errada?)"

psql -d postgres -qc "DROP DATABASE IF EXISTS $CHECK_DB" -c "CREATE DATABASE $CHECK_DB" || fail "criar banco descartável"
pg_restore --no-owner --no-privileges --exit-on-error -d "$CHECK_DB" "$TMP/dump" || fail "pg_restore"

failed="$(psql -d "$CHECK_DB" -Atc "SELECT count(*) FROM flyway_schema_history WHERE NOT success")" \
  || fail "flyway_schema_history ausente"
[ "$failed" = "0" ] || fail "$failed migration(s) com falha no flyway_schema_history"
latest_version="$(psql -d "$CHECK_DB" -Atc "SELECT max(version::int) FROM flyway_schema_history WHERE success")"

counts="$(psql -d "$CHECK_DB" -At -F= -c "
  SELECT table_name, (xpath('/row/c/text()', query_to_xml(
    format('SELECT count(*) AS c FROM %I.%I', table_schema, table_name), false, true, '')))[1]::text
  FROM information_schema.tables
  WHERE table_schema = 'public' AND table_type = 'BASE TABLE' AND table_name <> 'flyway_schema_history'
  ORDER BY table_name")" || fail "contagem de linhas"

echo "arquivo=$LATEST"
echo "flyway_latest_version=$latest_version"
echo "$counts"
notify "Teste de restauração OK: $LATEST, Flyway na versão $latest_version. Linhas: $(echo "$counts" | tr '\n' ' ')"
