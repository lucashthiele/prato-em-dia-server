#!/bin/sh
# Backup diário: pg_dump compactado (formato custom) + criptografia com age + envio por rclone.
# Só a chave PÚBLICA (AGE_RECIPIENT) vive na VPS; a privada fica com o responsável.
#
# Variáveis: PGHOST PGUSER PGPASSWORD PGDATABASE, AGE_RECIPIENT, BACKUP_DEST (destino rclone,
# ex.: r2:prato-backups), DISCORD_WEBHOOK_URL (alerta de falha), HEARTBEAT_URL (ping no sucesso;
# o monitor externo alerta se o ping NÃO chegar) e BACKUP_TODAY (AAAA-MM-DD, só para testes).
#
# Retenção por ciclo de vida do bucket: daily/ 7 dias, weekly/ 28 dias, monthly/ 90 dias.
# Este script grava no daily/ todo dia, no weekly/ aos domingos e no monthly/ no dia 1.
set -eu
. /usr/local/bin/lib.sh

TODAY="${BACKUP_TODAY:-$(date +%F)}"
STAMP="$(echo "$TODAY" | tr -d -)T$(date -u +%H%M%S)Z"
FILE="prato-$STAMP.dump.age"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

fail() {
  echo "backup falhou: $1" >&2
  notify "Backup do Prato em Dia FALHOU em $TODAY: $1"
  exit 1
}

: "${AGE_RECIPIENT:?}" "${BACKUP_DEST:?}" "${PGDATABASE:?}"

# pipefail não existe em todo sh; o dump vai para arquivo antes de criptografar para checar cada etapa.
pg_dump --format=custom --compress=9 --file="$TMP/dump" || fail "pg_dump"
[ -s "$TMP/dump" ] || fail "dump vazio"
age --recipient "$AGE_RECIPIENT" --output "$TMP/$FILE" "$TMP/dump" || fail "criptografia com age"
rm -f "$TMP/dump"

rclone copyto "$TMP/$FILE" "$BACKUP_DEST/daily/$FILE" || fail "envio para daily/"
if [ "$(date -d "$TODAY" +%u)" = "7" ]; then
  rclone copyto "$TMP/$FILE" "$BACKUP_DEST/weekly/$FILE" || fail "envio para weekly/"
fi
if [ "$(date -d "$TODAY" +%d)" = "01" ]; then
  rclone copyto "$TMP/$FILE" "$BACKUP_DEST/monthly/$FILE" || fail "envio para monthly/"
fi

echo "backup concluído: $FILE ($(wc -c <"$TMP/$FILE") bytes)"
if [ -n "${HEARTBEAT_URL:-}" ]; then
  curl -fsS -m 10 "$HEARTBEAT_URL" >/dev/null || echo "aviso: heartbeat não enviado" >&2
fi
