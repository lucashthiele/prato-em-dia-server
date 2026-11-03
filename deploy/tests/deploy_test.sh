#!/usr/bin/env bash
# Testes do deploy blue/green (deploy/scripts/deploy.sh) com docker e caddy simulados.
# Uso: deploy/tests/deploy_test.sh
set -u

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
DEPLOY="$ROOT/deploy/scripts/deploy.sh"
FAILURES=0

fail() { echo "  FALHOU: $*"; FAILURES=$((FAILURES + 1)); }
assert_eq() { [ "$1" = "$2" ] || fail "$3 (esperado '$2', obtido '$1')"; }
assert_contains() { grep -q -- "$2" "$1" || fail "$3 (log não contém '$2')"; }
assert_not_contains() { ! grep -q -- "$2" "$1" || fail "$3 (log contém '$2')"; }

# Cria um ambiente isolado: estado, bin com docker falso e artefato do portal.
new_env() {
  WORK="$(mktemp -d)"
  export STATE_DIR="$WORK/state" PORTAL_ROOT="$WORK/portal" FAKE_LOG="$WORK/docker.log"
  mkdir -p "$STATE_DIR" "$PORTAL_ROOT/releases" "$WORK/bin" "$WORK/portal-src"
  echo "<html>portal</html>" >"$WORK/portal-src/index.html"
  tar -czf "$WORK/portal.tar.gz" -C "$WORK/portal-src" .
  cat >"$WORK/bin/docker" <<'STUB'
#!/usr/bin/env bash
echo "docker $*" >>"$FAKE_LOG"
case "$*" in
  *"wget"*) [ "${FAKE_READY:-yes}" = "yes" ] ;;
  *"caddy reload"*) [ "${FAKE_RELOAD:-yes}" = "yes" ] ;;
  *) exit 0 ;;
esac
STUB
  chmod +x "$WORK/bin/docker"
  export PATH="$WORK/bin:$PATH"
  export COMPOSE_FILE="$ROOT/deploy/compose.yaml"
  export REGISTRY_IMAGE="ghcr.io/example/server" READINESS_TIMEOUT=2 READINESS_INTERVAL=1
  export PORTAL_ARCHIVE="$WORK/portal.tar.gz"
  export UPSTREAM_FILE="$STATE_DIR/upstream.caddy"
  : >"$FAKE_LOG"
}

echo "deploy: primeira execução sobe o verde e vira o Caddy"
new_env
FAKE_READY=yes bash "$DEPLOY" --api-sha abc123 --portal-sha p111 >"$WORK/out.log" 2>&1
assert_eq "$?" "0" "deploy deve terminar com sucesso"
assert_eq "$(cat "$STATE_DIR/active_color")" "green" "primeira virada vai para green (padrão ativo = blue)"
assert_contains "$UPSTREAM_FILE" "app-green:8080" "upstream aponta para green"
assert_eq "$(readlink "$PORTAL_ROOT/current")" "$PORTAL_ROOT/releases/p111" "symlink do portal na release nova"
assert_contains "$STATE_DIR/images.env" "APP_GREEN_IMAGE=ghcr.io/example/server:abc123" "imagem do green gravada"
assert_contains "$FAKE_LOG" "up -d app-green" "sobe só a cor ociosa"
assert_contains "$FAKE_LOG" "caddy reload" "recarrega o Caddy"
assert_not_contains "$FAKE_LOG" "stop app-blue" "cor antiga continua ligada"

echo "deploy: segunda execução alterna de volta para blue"
FAKE_READY=yes bash "$DEPLOY" --api-sha def456 --portal-sha p222 >"$WORK/out.log" 2>&1
assert_eq "$?" "0" "segundo deploy com sucesso"
assert_eq "$(cat "$STATE_DIR/active_color")" "blue" "alterna para blue"
assert_contains "$UPSTREAM_FILE" "app-blue:8080" "upstream volta para blue"
assert_eq "$(readlink "$PORTAL_ROOT/current")" "$PORTAL_ROOT/releases/p222" "symlink na segunda release"
[ -d "$PORTAL_ROOT/releases/p111" ] || fail "release anterior do portal fica no disco para rollback"

echo "deploy: readiness que não fica verde faz rollback sem mudar nada"
new_env
echo blue >"$STATE_DIR/active_color"
echo "reverse_proxy app-blue:8080" >"$UPSTREAM_FILE"
FAKE_READY=no bash "$DEPLOY" --api-sha bad999 --portal-sha p333 >"$WORK/out.log" 2>&1
assert_eq "$?" "1" "deploy falha quando o readiness não fica verde"
assert_eq "$(cat "$STATE_DIR/active_color")" "blue" "cor ativa não muda"
assert_contains "$UPSTREAM_FILE" "app-blue:8080" "upstream não muda"
[ ! -e "$PORTAL_ROOT/current" ] || fail "symlink do portal não deve ser criado"
[ ! -d "$PORTAL_ROOT/releases/p333" ] || fail "portal não é enviado antes do readiness"
assert_not_contains "$FAKE_LOG" "caddy reload" "Caddy não é recarregado"
assert_contains "$FAKE_LOG" "stop app-green" "cor ociosa que falhou é desligada"

echo "deploy: falha no reload do Caddy restaura upstream e portal anteriores"
new_env
echo blue >"$STATE_DIR/active_color"
echo "reverse_proxy app-blue:8080" >"$UPSTREAM_FILE"
mkdir -p "$PORTAL_ROOT/releases/old" && ln -sfn "$PORTAL_ROOT/releases/old" "$PORTAL_ROOT/current"
FAKE_READY=yes FAKE_RELOAD=no bash "$DEPLOY" --api-sha abc123 --portal-sha p444 >"$WORK/out.log" 2>&1
assert_eq "$?" "1" "deploy falha quando o reload falha"
assert_eq "$(cat "$STATE_DIR/active_color")" "blue" "cor ativa não muda após falha do reload"
assert_contains "$UPSTREAM_FILE" "app-blue:8080" "upstream restaurado"
assert_eq "$(readlink "$PORTAL_ROOT/current")" "$PORTAL_ROOT/releases/old" "symlink restaurado"

echo "deploy: avisa o Discord quando faz rollback"
new_env
export DISCORD_WEBHOOK_URL="http://discord.invalid/hook"
cat >"$WORK/bin/curl" <<'STUB'
#!/usr/bin/env bash
echo "curl $*" >>"$FAKE_LOG"
STUB
chmod +x "$WORK/bin/curl"
FAKE_READY=no bash "$DEPLOY" --api-sha bad999 --portal-sha p555 >"$WORK/out.log" 2>&1
assert_contains "$FAKE_LOG" "discord.invalid/hook" "webhook do Discord acionado no rollback"
unset DISCORD_WEBHOOK_URL

echo "deploy: exige as duas versões"
new_env
bash "$DEPLOY" --api-sha abc123 >"$WORK/out.log" 2>&1
assert_eq "$?" "2" "sem --portal-sha o uso é inválido"

[ "$FAILURES" -eq 0 ] && echo "OK" || { echo "$FAILURES falha(s)"; exit 1; }
