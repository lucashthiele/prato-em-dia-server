#!/bin/sh
# Agenda o backup às 03:00 (fuso America/Sao_Paulo) com o crond do busybox.
# O cron não herda variáveis de ambiente, então elas são gravadas em /etc/backup.env.
set -eu
printenv | grep -E '^(PG|AGE_|BACKUP_|DISCORD_|HEARTBEAT_|RCLONE_|TZ)' \
  | sed "s/'/'\\\\''/g; s/^\([^=]*\)=\(.*\)$/export \1='\2'/" >/etc/backup.env
chmod 600 /etc/backup.env
echo '0 3 * * * . /etc/backup.env; /usr/local/bin/backup.sh >/proc/1/fd/1 2>&1' >/etc/crontabs/root
echo "backup agendado para 03:00 ($TZ)"
exec crond -f -l 8
