#!/usr/bin/env bash
# Valida os arquivos de observabilidade com as ferramentas oficiais (promtool e Alloy), via Docker,
# e confere que as regras de alerta cobrem os eventos críticos de docs/infraestrutura.md.
set -u
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
OBS="$ROOT/deploy/observability"
FAILURES=0
fail() { echo "  FALHOU: $*"; FAILURES=$((FAILURES + 1)); }

echo "alertas: sintaxe das regras (promtool)"
docker run --rm --entrypoint promtool -v "$OBS:/obs:ro" prom/prometheus:v3.5.0 check rules /obs/alert-rules.yaml \
  || fail "promtool rejeitou alert-rules.yaml"

echo "alertas: cobertura dos eventos críticos"
for alert in ApiDown High5xxRate EmailOutboxDead PlanGenerationFailed JvmHeapHigh; do
  grep -q "alert: $alert" "$OBS/alert-rules.yaml" || fail "regra $alert ausente"
done
grep -q "prato_email_outbox_dead_total" "$OBS/alert-rules.yaml" || fail "regra do outbox DEAD não usa a métrica"
grep -q "prato_plan_generation_failed_total" "$OBS/alert-rules.yaml" || fail "regra de geração não usa a métrica"

echo "alloy: sintaxe da configuração"
docker run --rm -e GRAFANA_CLOUD_PROMETHEUS_URL=https://prom.invalid/api/prom/push \
  -e GRAFANA_CLOUD_PROMETHEUS_USER=1 -e GRAFANA_CLOUD_LOKI_URL=https://loki.invalid/loki/api/v1/push \
  -e GRAFANA_CLOUD_LOKI_USER=2 -e GRAFANA_CLOUD_API_KEY=x \
  -v "$OBS:/obs:ro" grafana/alloy:v1.10.2 validate /obs/config.alloy \
  || fail "alloy validate rejeitou config.alloy"

echo "contact point: Discord sem segredo no arquivo"
grep -q 'type: discord' "$OBS/contact-point-discord.yaml" || fail "contact point do Discord ausente"
grep -Eq 'discord\.com/api/webhooks/[0-9]' "$OBS/contact-point-discord.yaml" && fail "URL real de webhook versionada"

[ "$FAILURES" -eq 0 ] && echo "OK" || { echo "$FAILURES falha(s)"; exit 1; }
