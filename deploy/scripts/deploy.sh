#!/usr/bin/env bash
# Deploy blue/green (ADR 0003, docs/infraestrutura.md). Roda na VPS, chamado pela GitHub Actions por SSH.
#
#   deploy.sh --api-sha <sha-da-imagem> --portal-sha <sha-do-portal>
#
# 1. sobe a cor ociosa com a imagem nova (o Flyway roda na subida);
# 2. espera o readiness; se não ficar verde, desliga a cor nova e sai sem mudar nada;
# 3. envia a release do portal para $PORTAL_ROOT/releases/<sha>;
# 4. vira o upstream do Caddy e o symlink $PORTAL_ROOT/current juntos (restaura os dois se falhar);
# 5. mantém a cor antiga ligada, para rollback instantâneo.
set -euo pipefail

STATE_DIR="${STATE_DIR:-/srv/prato}"
PORTAL_ROOT="${PORTAL_ROOT:-/srv/portal}"
COMPOSE_FILE="${COMPOSE_FILE:-$STATE_DIR/compose.yaml}"
REGISTRY_IMAGE="${REGISTRY_IMAGE:?defina REGISTRY_IMAGE (ex.: ghcr.io/dono/prato-em-dia-server)}"
UPSTREAM_FILE="${UPSTREAM_FILE:-$STATE_DIR/upstream.caddy}"
READINESS_URL="${READINESS_URL:-http://localhost:8081/actuator/health/readiness}"
READINESS_TIMEOUT="${READINESS_TIMEOUT:-300}"
READINESS_INTERVAL="${READINESS_INTERVAL:-5}"
PORTAL_ARCHIVE="${PORTAL_ARCHIVE:-}"

API_SHA=""
PORTAL_SHA=""
while [ $# -gt 0 ]; do
  case "$1" in
    --api-sha) API_SHA="${2:-}"; shift 2 ;;
    --portal-sha) PORTAL_SHA="${2:-}"; shift 2 ;;
    *) echo "argumento desconhecido: $1" >&2; exit 2 ;;
  esac
done
if [ -z "$API_SHA" ] || [ -z "$PORTAL_SHA" ]; then
  echo "uso: deploy.sh --api-sha <sha> --portal-sha <sha>" >&2
  exit 2
fi

compose() { docker compose -f "$COMPOSE_FILE" --env-file "$STATE_DIR/images.env" "$@"; }

notify() {
  [ -n "${DISCORD_WEBHOOK_URL:-}" ] || return 0
  curl -fsS -m 10 -H 'Content-Type: application/json' \
    -d "{\"content\": \"$1\"}" "$DISCORD_WEBHOOK_URL" >/dev/null || true
}

ACTIVE="$(cat "$STATE_DIR/active_color" 2>/dev/null || echo blue)"
if [ "$ACTIVE" = "blue" ]; then IDLE="green"; else IDLE="blue"; fi
IDLE_UPPER="$(echo "$IDLE" | tr '[:lower:]' '[:upper:]')"
echo "cor ativa: $ACTIVE; cor nova: $IDLE (api $API_SHA, portal $PORTAL_SHA)"

# images.env guarda a imagem de cada cor; a cor ativa mantém a sua.
touch "$STATE_DIR/images.env"
grep -v "^APP_${IDLE_UPPER}_IMAGE=" "$STATE_DIR/images.env" >"$STATE_DIR/images.env.tmp" || true
echo "APP_${IDLE_UPPER}_IMAGE=$REGISTRY_IMAGE:$API_SHA" >>"$STATE_DIR/images.env.tmp"
mv "$STATE_DIR/images.env.tmp" "$STATE_DIR/images.env"

rollback() {
  echo "ROLLBACK: $1" >&2
  compose stop "app-$IDLE" || true
  notify "Deploy da API $API_SHA revertido: $1. A cor $ACTIVE continua atendendo."
  exit 1
}

compose up -d "app-$IDLE"

waited=0
until compose exec -T "app-$IDLE" wget -qO- "$READINESS_URL" >/dev/null 2>&1; do
  if [ "$waited" -ge "$READINESS_TIMEOUT" ]; then
    rollback "readiness de app-$IDLE não ficou verde em ${READINESS_TIMEOUT}s"
  fi
  sleep "$READINESS_INTERVAL"
  waited=$((waited + READINESS_INTERVAL))
done
echo "app-$IDLE pronta após ${waited}s"

# Portal: só depois do readiness, para não mexer em nada se a API falhou.
mkdir -p "$PORTAL_ROOT/releases"
RELEASE_DIR="$PORTAL_ROOT/releases/$PORTAL_SHA"
if [ ! -d "$RELEASE_DIR" ]; then
  mkdir -p "$RELEASE_DIR"
  if [ -n "$PORTAL_ARCHIVE" ]; then
    tar -xzf "$PORTAL_ARCHIVE" -C "$RELEASE_DIR"
  fi
fi

PREVIOUS_UPSTREAM="$(cat "$UPSTREAM_FILE" 2>/dev/null || true)"
PREVIOUS_PORTAL="$(readlink "$PORTAL_ROOT/current" 2>/dev/null || true)"

restore_routing() {
  if [ -n "$PREVIOUS_UPSTREAM" ]; then echo "$PREVIOUS_UPSTREAM" >"$UPSTREAM_FILE"; else rm -f "$UPSTREAM_FILE"; fi
  if [ -n "$PREVIOUS_PORTAL" ]; then ln -sfn "$PREVIOUS_PORTAL" "$PORTAL_ROOT/current"; else rm -f "$PORTAL_ROOT/current"; fi
}

echo "reverse_proxy app-$IDLE:8080" >"$UPSTREAM_FILE"
ln -sfn "$RELEASE_DIR" "$PORTAL_ROOT/current"
if ! compose exec -T caddy caddy reload --config /etc/caddy/Caddyfile; then
  restore_routing
  rollback "caddy reload falhou"
fi

echo "$IDLE" >"$STATE_DIR/active_color"
echo "deploy concluído: $IDLE ativa; $ACTIVE segue ligada para rollback"
