#!/usr/bin/env bash
# Teste ponta a ponta do backup (deploy/backup): dump, criptografia com age, envio por rclone
# (destino local), alertas e teste de restauração em Postgres descartável. Requer Docker.
set -u

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
IMAGE="prato-backup:test"
NET="prato-backup-test-$$"
WORK="$(mktemp -d)"
FAILURES=0
fail() { echo "  FALHOU: $*"; FAILURES=$((FAILURES + 1)); }

cleanup() {
  docker rm -f "pg-$NET" "restore-$NET" "hook-$NET" >/dev/null 2>&1
  docker network rm "$NET" >/dev/null 2>&1
  # Arquivos criados pelos containers (root) não podem ser apagados pelo usuário do runner.
  docker run --rm -v "$WORK:/work" alpine rm -rf /work/bucket /work/keys /work/hook >/dev/null 2>&1
  rm -rf "$WORK"
}
trap cleanup EXIT

docker build -q -t "$IMAGE" "$ROOT/deploy/backup" >/dev/null || { echo "build da imagem falhou"; exit 1; }
docker network create "$NET" >/dev/null

mkdir -p "$WORK/bucket" "$WORK/keys" "$WORK/hook"
chmod 777 "$WORK/bucket" "$WORK/keys" "$WORK/hook"
# Criado aqui para pertencer ao usuário do teste: o container (root) só acrescenta linhas.
: >"$WORK/hook/requests.log"
chmod 666 "$WORK/hook/requests.log"

wait_pg() {
  for _ in $(seq 1 30); do
    docker exec "$1" pg_isready -U postgres >/dev/null 2>&1 && return 0
    sleep 1
  done
  return 1
}

# Banco de origem com um schema mínimo no estilo do projeto.
docker run -d --name "pg-$NET" --network "$NET" -e POSTGRES_PASSWORD=pw -e POSTGRES_DB=pratoemdia postgres:17-alpine >/dev/null
wait_pg "pg-$NET" || { echo "postgres de origem não subiu"; exit 1; }
sleep 2
docker exec -i "pg-$NET" psql -q -U postgres -d pratoemdia <<'SQL'
CREATE TABLE flyway_schema_history (installed_rank int PRIMARY KEY, version text, description text, success boolean NOT NULL);
INSERT INTO flyway_schema_history VALUES (1, '1', 'create identity', true), (2, '60', 'create shedlock', true);
CREATE TABLE food (id int PRIMARY KEY, name text);
INSERT INTO food SELECT g, 'alimento ' || g FROM generate_series(1, 25) g;
SQL

# Servidor de webhook que registra o caminho de cada requisição (Discord e heartbeat).
docker run -d --name "hook-$NET" --network "$NET" -v "$WORK/hook:/out" alpine sh -c \
  'while true; do printf "HTTP/1.1 204 No Content\r\nConnection: close\r\n\r\n" | nc -l -p 8080 | head -1 >> /out/requests.log; done' >/dev/null

# Par de chaves age: a privada só existe aqui, nunca dentro do container de backup.
docker run --rm -v "$WORK/keys:/keys" "$IMAGE" sh -c 'age-keygen -o /keys/identity.txt 2>/dev/null; chmod 644 /keys/identity.txt'
RECIPIENT="$(grep '^# public key:' "$WORK/keys/identity.txt" | awk '{print $4}')"

run_backup() { # $1 = data simulada, $2 = senha
  docker run --rm --network "$NET" -v "$WORK/bucket:/bucket" \
    -e PGHOST="pg-$NET" -e PGUSER=postgres -e PGPASSWORD="$2" -e PGDATABASE=pratoemdia \
    -e AGE_RECIPIENT="$RECIPIENT" -e BACKUP_DEST=/bucket -e BACKUP_TODAY="$1" \
    -e DISCORD_WEBHOOK_URL="http://hook-$NET:8080/discord" -e HEARTBEAT_URL="http://hook-$NET:8080/heartbeat" \
    "$IMAGE" /usr/local/bin/backup.sh
}

echo "backup: dia comum gera só o diário, criptografado, e dá o heartbeat"
run_backup 2026-11-03 pw >"$WORK/backup1.log" 2>&1 || { fail "backup deveria terminar com sucesso"; cat "$WORK/backup1.log"; }
daily="$(ls "$WORK"/bucket/daily/*.dump.age 2>/dev/null | head -1)"
[ -n "$daily" ] || fail "arquivo diário não foi criado"
[ -n "$daily" ] && head -c 5 "$daily" | grep -q "PGDMP" && fail "dump está em texto claro (sem criptografia)"
[ -n "$daily" ] && ! head -c 40 "$daily" | grep -q "age-encryption.org" && fail "arquivo não tem cabeçalho do age"
[ -d "$WORK/bucket/weekly" ] && fail "terça-feira não gera cópia semanal"
[ -d "$WORK/bucket/monthly" ] && fail "dia 3 não gera cópia mensal"
sleep 1
grep -q "/heartbeat" "$WORK/hook/requests.log" 2>/dev/null || fail "heartbeat de sucesso não foi enviado"

echo "backup: domingo dia 1 gera também semanal e mensal"
run_backup 2026-11-01 pw >"$WORK/backup2.log" 2>&1 || fail "backup de domingo falhou"
ls "$WORK"/bucket/weekly/*.dump.age >/dev/null 2>&1 || fail "cópia semanal ausente no domingo"
ls "$WORK"/bucket/monthly/*.dump.age >/dev/null 2>&1 || fail "cópia mensal ausente no dia 1"

echo "backup: falha avisa o Discord e não envia heartbeat"
: >"$WORK/hook/requests.log"
if run_backup 2026-11-04 senha-errada >"$WORK/backup3.log" 2>&1; then fail "backup com senha errada deveria falhar"; fi
sleep 1
grep -q "/discord" "$WORK/hook/requests.log" || fail "alerta de falha não chegou ao Discord"
grep -q "/heartbeat" "$WORK/hook/requests.log" && fail "heartbeat não pode ser enviado após falha"
ls "$WORK"/bucket/daily/*2026-11-04* >/dev/null 2>&1 && fail "não deve sobrar arquivo parcial de backup falho"

echo "restauração: restaura o último diário num Postgres descartável e confere"
docker run -d --name "restore-$NET" --network "$NET" -e POSTGRES_PASSWORD=pw postgres:17-alpine >/dev/null
wait_pg "restore-$NET" || { echo "postgres descartável não subiu"; exit 1; }
sleep 2
restore() { # $1 = arquivo de identidade
  docker run --rm --network "$NET" -v "$WORK/bucket:/bucket" -v "$1:/identity.txt:ro" \
    -e BACKUP_SOURCE=/bucket/daily -e AGE_IDENTITY_FILE=/identity.txt \
    -e PGHOST="restore-$NET" -e PGUSER=postgres -e PGPASSWORD=pw \
    -e DISCORD_WEBHOOK_URL="http://hook-$NET:8080/discord" \
    "$IMAGE" /usr/local/bin/restore-test.sh
}
: >"$WORK/hook/requests.log"
if restore "$WORK/keys/identity.txt" >"$WORK/restore1.log" 2>&1; then
  grep -q "food=25" "$WORK/restore1.log" || fail "contagem de linhas da tabela food não aparece no relatório"
  grep -q "flyway_latest_version=60" "$WORK/restore1.log" || fail "versão do Flyway não aparece no relatório"
else
  fail "restauração deveria terminar com sucesso"; cat "$WORK/restore1.log"
fi
sleep 1
grep -q "/discord" "$WORK/hook/requests.log" || fail "resultado da restauração não foi para o Discord"

echo "restauração: chave errada falha"
docker run --rm -v "$WORK/keys:/keys" "$IMAGE" sh -c 'age-keygen -o /keys/other.txt 2>/dev/null; chmod 644 /keys/other.txt'
if restore "$WORK/keys/other.txt" >"$WORK/restore2.log" 2>&1; then fail "restauração com chave errada deveria falhar"; fi

[ "$FAILURES" -eq 0 ] && echo "OK" || { echo "$FAILURES falha(s)"; exit 1; }
