#!/usr/bin/env bash
# Valida as migrations Flyway contra um ref base (padrão: origin/main):
#  - migration existente nunca é alterada, removida nem renomeada (o Flyway calcula checksum);
#  - nomes seguem V<versão>__<descrição>.sql;
#  - não há duas migrations com a mesma versão.
# Uso: scripts/check-migrations.sh [ref-base]
set -euo pipefail

BASE="${1:-origin/main}"
DIR="src/main/resources/db/migration"
status=0

while IFS=$'\t' read -r change path _; do
  [ -n "$change" ] || continue
  case "$change" in
    A) ;;
    *) echo "ERRO: migration existente foi alterada, removida ou renomeada ($change): $path" >&2; status=1 ;;
  esac
done < <(git diff --name-status --no-renames "$(git merge-base "$BASE" HEAD)" HEAD -- "$DIR")

versions=""
for file in "$DIR"/*.sql; do
  name="$(basename "$file")"
  if ! echo "$name" | grep -Eq '^V[0-9]+__[A-Za-z0-9_]+\.sql$'; then
    echo "ERRO: nome fora do padrão Flyway: $name" >&2
    status=1
    continue
  fi
  versions="$versions
$(echo "$name" | sed -E 's/^V([0-9]+)__.*/\1/' | sed -E 's/^0+//')"
done
duplicated="$(echo "$versions" | grep -v '^$' | sort | uniq -d || true)"
if [ -n "$duplicated" ]; then
  echo "ERRO: versões de migration duplicadas: $(echo "$duplicated" | tr '\n' ' ')" >&2
  status=1
fi

[ "$status" -eq 0 ] && echo "migrations ok"
exit "$status"
