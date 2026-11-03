#!/bin/bash
# Endurecimento da VPS (LUC-72, Marco 5). Idempotente; rode como root, UMA vez por servidor:
#
#   DEPLOY_PUBKEY="ssh-ed25519 AAAA... deploy@gha" ADMIN_USER=lucas sudo -E deploy/vps/harden.sh
#
# ANTES: o ADMIN_USER precisa existir, estar no grupo sudo e já ter uma chave SSH funcionando
# (testada numa segunda sessão), porque o login por senha e o login de root serão desligados.
# DRY_RUN=1 só mostra os comandos; ROOT_DIR=<dir> redireciona os arquivos (usado nos testes).
set -euo pipefail

: "${DEPLOY_PUBKEY:?defina DEPLOY_PUBKEY com a chave pública do usuário de deploy}"
: "${ADMIN_USER:=admin}"
DRY_RUN="${DRY_RUN:-0}"
ROOT_DIR="${ROOT_DIR:-}"
HERE="$(cd "$(dirname "$0")" && pwd)"

run() {
  echo "+ $*"
  [ "$DRY_RUN" = "1" ] || "$@"
}

write_file() { # $1 = caminho (relativo a ROOT_DIR), conteúdo no stdin
  mkdir -p "$(dirname "$ROOT_DIR$1")"
  cat >"$ROOT_DIR$1"
}

if [ "$DRY_RUN" != "1" ]; then
  [ "$(id -u)" = "0" ] || { echo "rode como root" >&2; exit 1; }
  if [ ! -s "/home/$ADMIN_USER/.ssh/authorized_keys" ]; then
    echo "ERRO: $ADMIN_USER não tem chave SSH; desligar a senha trancaria você fora." >&2
    exit 1
  fi
fi

echo "== pacotes =="
run apt-get update -y
run apt-get install -y ufw fail2ban unattended-upgrades

echo "== firewall: só 22, 80 e 443 =="
run ufw default deny incoming
run ufw default allow outgoing
run ufw allow 22/tcp
run ufw allow 80/tcp
run ufw allow 443/tcp
run ufw --force enable

echo "== usuário de deploy com permissões mínimas =="
if [ "$DRY_RUN" = "1" ] || ! id deploy >/dev/null 2>&1; then
  run useradd --create-home --shell /bin/bash deploy
fi
run usermod -aG docker deploy
run install -d -o deploy -g deploy /srv/prato /srv/portal /srv/portal/releases
run install -m 755 "$HERE/ssh-gate.sh" /usr/local/bin/prato-ssh-gate
write_file /home/deploy/.ssh/authorized_keys <<KEYS
restrict,command="/usr/local/bin/prato-ssh-gate" $DEPLOY_PUBKEY
KEYS
run chown -R deploy:deploy /home/deploy/.ssh
run chmod 700 /home/deploy/.ssh
run chmod 600 /home/deploy/.ssh/authorized_keys

echo "== sshd: só chave, sem root =="
write_file /etc/ssh/sshd_config.d/99-prato-hardening.conf <<SSHD
PasswordAuthentication no
KbdInteractiveAuthentication no
PermitRootLogin no
PubkeyAuthentication yes
PermitEmptyPasswords no
X11Forwarding no
MaxAuthTries 3
LoginGraceTime 30
AllowUsers deploy $ADMIN_USER
SSHD
run sshd -t
run systemctl reload ssh

echo "== fail2ban: limita tentativas de SSH =="
write_file /etc/fail2ban/jail.d/prato-sshd.local <<JAIL
[sshd]
enabled  = true
maxretry = 5
findtime = 10m
bantime  = 1h
JAIL
run systemctl enable --now fail2ban
run systemctl restart fail2ban

echo "== atualizações de segurança automáticas =="
run dpkg-reconfigure -f noninteractive unattended-upgrades

echo "Concluído. Rode deploy/vps/verify-hardening.sh e teste um NOVO login SSH antes de fechar a sessão atual."
