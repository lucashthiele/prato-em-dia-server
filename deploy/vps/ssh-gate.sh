#!/bin/bash
# Gate do SSH de deploy: instalado como `command=` da chave do usuário `deploy` (authorized_keys).
# Só deixa passar o que o pipeline precisa (docs/infraestrutura.md): o deploy.sh com argumentos
# validados, o rsync para /srv/prato e o scp do arquivo do portal. Todo o resto é negado.
set -u

cmd="${SSH_ORIGINAL_COMMAND:-}"

deny() {
  echo "prato-ssh-gate: comando não permitido" >&2
  logger -t prato-ssh-gate "negado: $cmd" 2>/dev/null || true
  exit 1
}

[ -n "$cmd" ] || deny
case "$cmd" in *..*) deny ;; esac

deploy_re='^REGISTRY_IMAGE=ghcr\.io/[a-z0-9._/-]+ +PORTAL_ARCHIVE=/srv/prato/portal\.tar\.gz +/srv/prato/deploy\.sh --api-sha [0-9a-f]{7,40} --portal-sha [A-Za-z0-9._-]+$'
rsync_re='^rsync --server [A-Za-z0-9._ =+-]+ \. /srv/prato/[A-Za-z0-9._/-]*$'
scp_re='^scp (-[a-z]+ )*-t /srv/prato/portal\.tar\.gz$'

if [[ "$cmd" =~ $deploy_re || "$cmd" =~ $rsync_re || "$cmd" =~ $scp_re ]]; then
  [ "${GATE_DRY_RUN:-0}" = "1" ] && exit 0
  exec bash -c "$cmd"
fi
deny
