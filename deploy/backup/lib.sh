# Funções compartilhadas por backup.sh e restore-test.sh.

# Envia uma mensagem ao Discord (se DISCORD_WEBHOOK_URL estiver definido). Nunca derruba o script.
notify() {
  [ -n "${DISCORD_WEBHOOK_URL:-}" ] || return 0
  payload="$(printf '%s' "$1" | sed 's/\\/\\\\/g; s/"/\\"/g' | tr '\n' ' ')"
  curl -fsS -m 10 -H 'Content-Type: application/json' \
    -d "{\"content\": \"$payload\"}" "$DISCORD_WEBHOOK_URL" >/dev/null 2>&1 || true
}
