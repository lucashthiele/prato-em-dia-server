#!/bin/bash
# Confere o endurecimento da VPS (rodar na VPS, como root ou com sudo):
#   deploy/vps/verify-hardening.sh
# Opcional, de FORA da VPS: deploy/vps/verify-hardening.sh --remote <host> confere as portas.
set -u
status=0
ok() { echo "ok:    $*"; }
bad() { echo "FALHA: $*"; status=1; }

if [ "${1:-}" = "--remote" ]; then
  host="${2:?uso: verify-hardening.sh --remote <host>}"
  for port in 22 80 443; do
    nc -z -w 3 "$host" "$port" && ok "porta $port aberta" || bad "porta $port deveria estar aberta"
  done
  for port in 5432 8080 8081 12345; do
    nc -z -w 3 "$host" "$port" && bad "porta $port aberta ao público" || ok "porta $port fechada"
  done
  exit "$status"
fi

echo "== firewall =="
ufw_out="$(ufw status)"
echo "$ufw_out" | grep -q "^Status: active" && ok "ufw ativo" || bad "ufw inativo"
allowed="$(echo "$ufw_out" | grep -E 'ALLOW' | awk '{print $1}' | sed 's#/.*##' | sort -u | tr '\n' ' ')"
[ "$allowed" = "22 443 80 " ] && ok "ufw libera somente 22, 80 e 443" || bad "ufw libera: $allowed"

echo "== sshd =="
sshd_out="$(sshd -T)"
echo "$sshd_out" | grep -q "^passwordauthentication no" && ok "senha desligada" || bad "login por senha habilitado"
echo "$sshd_out" | grep -q "^permitrootlogin no" && ok "root desligado" || bad "login de root habilitado"
echo "$sshd_out" | grep -q "^pubkeyauthentication yes" && ok "chave habilitada" || bad "chave desabilitada"

echo "== containers: só o caddy publica portas =="
published="$(docker ps --format '{{.Names}} {{.Ports}}' | grep -E '0\.0\.0\.0:|:::' | grep -v '^caddy ' || true)"
[ -z "$published" ] && ok "nenhum outro container publicado no host" || bad "publicado no host: $published"

echo "== usuário de deploy =="
id deploy | grep -Eq 'sudo|wheel|root' && bad "deploy está em grupo administrativo" || ok "deploy sem sudo"

exit "$status"
