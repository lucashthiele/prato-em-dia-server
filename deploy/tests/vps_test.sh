#!/usr/bin/env bash
# Testes do endurecimento da VPS (deploy/vps): gate de comandos do SSH de deploy, harden.sh em modo
# simulado (DRY_RUN) e validação da configuração do sshd com o próprio sshd (via Docker).
set -u
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
VPS="$ROOT/deploy/vps"
FAILURES=0
fail() { echo "  FALHOU: $*"; FAILURES=$((FAILURES + 1)); }

gate() { SSH_ORIGINAL_COMMAND="$1" GATE_DRY_RUN=1 bash "$VPS/ssh-gate.sh" >/dev/null 2>&1; }

echo "gate: comandos permitidos"
gate "REGISTRY_IMAGE=ghcr.io/lucashthiele/prato-em-dia-server PORTAL_ARCHIVE=/srv/prato/portal.tar.gz /srv/prato/deploy.sh --api-sha 0123abc --portal-sha 9f8e7d6" \
  || fail "deploy.sh com argumentos válidos deveria passar"
gate "REGISTRY_IMAGE=ghcr.io/lucashthiele/prato-em-dia-server PORTAL_ARCHIVE=/srv/prato/portal.tar.gz              /srv/prato/deploy.sh --api-sha 0123abc --portal-sha v1.2.3" \
  || fail "espaços extras do workflow deveriam passar"
gate "rsync --server -logDtpre.iLsfxCIvu --delete . /srv/prato/backup/" || fail "rsync para /srv/prato deveria passar"
gate "scp -t /srv/prato/portal.tar.gz" || fail "scp do arquivo do portal deveria passar"

echo "gate: comandos negados"
gate "" && fail "sem comando (shell interativo) deveria ser negado"
gate "bash" && fail "bash deveria ser negado"
gate "cat /etc/shadow" && fail "cat deveria ser negado"
gate "/srv/prato/deploy.sh --api-sha abc --portal-sha def; rm -rf /" && fail "injeção com ; deveria ser negada"
gate "/srv/prato/deploy.sh --api-sha \$(id) --portal-sha def" && fail "substituição de comando deveria ser negada"
gate "REGISTRY_IMAGE=evil.example/img PORTAL_ARCHIVE=/srv/prato/portal.tar.gz /srv/prato/deploy.sh --api-sha abc1234 --portal-sha def" \
  && fail "imagem fora do GHCR deveria ser negada"
gate "rsync --server -logDtpre.iLsfxCIvu . /etc/" && fail "rsync fora de /srv/prato deveria ser negado"
gate "rsync --server -logDtpre.iLsfxCIvu . /srv/prato/../../etc/" && fail "path traversal no rsync deveria ser negado"
gate "scp -t /root/.ssh/authorized_keys" && fail "scp para fora de /srv/prato deveria ser negado"

echo "harden.sh: modo simulado grava configs e só planeja comandos"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT
DRY_RUN=1 ROOT_DIR="$WORK" DEPLOY_PUBKEY="ssh-ed25519 AAAAC3Nza-test deploy@gha" ADMIN_USER=admin bash "$VPS/harden.sh" >"$WORK/plan.log" 2>&1 \
  || { fail "harden.sh --dry-run falhou"; cat "$WORK/plan.log"; }
SSHD="$WORK/etc/ssh/sshd_config.d/99-prato-hardening.conf"
[ -f "$SSHD" ] || fail "drop-in do sshd não foi gerado"
for rule in "PasswordAuthentication no" "PermitRootLogin no" "PubkeyAuthentication yes" "KbdInteractiveAuthentication no" "AllowUsers deploy admin"; do
  grep -q "^$rule" "$SSHD" 2>/dev/null || fail "sshd sem '$rule'"
done
grep -q "ufw default deny incoming" "$WORK/plan.log" || fail "ufw deve negar entrada por padrão"
for port in 22 80 443; do grep -q "ufw allow $port/tcp" "$WORK/plan.log" || fail "ufw deve liberar $port/tcp"; done
[ "$(grep -c 'ufw allow' "$WORK/plan.log")" = "3" ] || fail "ufw deve liberar somente 3 portas"
KEYS="$WORK/home/deploy/.ssh/authorized_keys"
grep -q 'restrict,command="/usr/local/bin/prato-ssh-gate"' "$KEYS" 2>/dev/null || fail "chave de deploy sem restrict/command do gate"
grep -q "AAAAC3Nza-test" "$KEYS" 2>/dev/null || fail "chave pública de deploy não instalada"
grep -q "fail2ban" "$WORK/plan.log" || fail "fail2ban (limite de tentativas de SSH) deve ser configurado"
grep -q "PasswordAuthentication yes" "$SSHD" && fail "login por senha não pode ficar habilitado"

echo "harden.sh: exige a chave pública de deploy"
DRY_RUN=1 ROOT_DIR="$WORK/x" bash "$VPS/harden.sh" >/dev/null 2>&1 && fail "sem DEPLOY_PUBKEY deveria falhar"

echo "verify-hardening.sh: aprova servidor correto e reprova exposição indevida"
STUBS="$WORK/stubs"; mkdir -p "$STUBS"
cat >"$STUBS/ufw" <<'STUB'
#!/bin/bash
printf 'Status: active\n\nTo Action From\n-- ------ ----\n22/tcp ALLOW IN Anywhere\n80/tcp ALLOW IN Anywhere\n443/tcp ALLOW IN Anywhere\n%s' "${FAKE_UFW_EXTRA:-}"
STUB
cat >"$STUBS/sshd" <<'STUB'
#!/bin/bash
printf 'passwordauthentication %s\npermitrootlogin no\npubkeyauthentication yes\n' "${FAKE_PASSWORD_AUTH:-no}"
STUB
cat >"$STUBS/docker" <<'STUB'
#!/bin/bash
printf 'caddy 0.0.0.0:80->80/tcp, 0.0.0.0:443->443/tcp\npostgres 5432/tcp\napp-blue %s\n' "${FAKE_APP_PORTS:-8080/tcp}"
STUB
cat >"$STUBS/id" <<'STUB'
#!/bin/bash
echo "uid=1001(deploy) gid=1001(deploy) groups=1001(deploy),999(docker)"
STUB
chmod +x "$STUBS"/*
verify() { PATH="$STUBS:$PATH" bash "$VPS/verify-hardening.sh" >"$WORK/verify.log" 2>&1; }
verify || { fail "servidor correto deveria passar"; cat "$WORK/verify.log"; }
FAKE_PASSWORD_AUTH=yes verify && fail "senha SSH habilitada deveria reprovar"
FAKE_UFW_EXTRA="5432/tcp ALLOW IN Anywhere" verify && fail "porta extra no ufw deveria reprovar"
FAKE_APP_PORTS="0.0.0.0:8080->8080/tcp" verify && fail "app publicado no host deveria reprovar"

echo "sshd: a configuração gerada é aceita pelo sshd real"
docker run --rm -v "$SSHD:/hardening.conf:ro" alpine sh -c '
  apk add --no-cache openssh >/dev/null 2>&1 && ssh-keygen -A >/dev/null 2>&1 &&
  adduser -D deploy >/dev/null 2>&1 && passwd -u deploy >/dev/null 2>&1
  cp /hardening.conf /etc/ssh/sshd_config
  sshd -t || exit 1
  out="$(sshd -T)"
  echo "$out" | grep -q "^passwordauthentication no" &&
  echo "$out" | grep -q "^permitrootlogin no" &&
  echo "$out" | grep -q "^allowusers deploy" &&
  echo "$out" | grep -q "^allowusers admin"' \
  || fail "sshd rejeitou a configuração ou não aplicou as diretivas"

[ "$FAILURES" -eq 0 ] && echo "OK" || { echo "$FAILURES falha(s)"; exit 1; }
