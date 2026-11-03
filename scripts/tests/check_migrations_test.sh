#!/usr/bin/env bash
# Testa scripts/check-migrations.sh num repositório git temporário.
set -u
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
CHECK="$ROOT/scripts/check-migrations.sh"
FAILURES=0
fail() { echo "  FALHOU: $*"; FAILURES=$((FAILURES + 1)); }

new_repo() {
  REPO="$(mktemp -d)"
  cd "$REPO" || exit 1
  git init -q -b main . && git config user.email t@t && git config user.name t
  mkdir -p src/main/resources/db/migration
  echo "select 1;" >src/main/resources/db/migration/V001__a.sql
  echo "select 2;" >src/main/resources/db/migration/V002__b.sql
  git add -A && git commit -q -m base
  git checkout -q -b feature
}

echo "migrations: aceita migration nova"
new_repo
echo "select 3;" >src/main/resources/db/migration/V060__c.sql
git add -A && git commit -q -m nova
bash "$CHECK" main || fail "migration nova deveria passar"

echo "migrations: rejeita migration alterada"
new_repo
echo "select 99;" >src/main/resources/db/migration/V001__a.sql
git add -A && git commit -q -m altera
bash "$CHECK" main >/dev/null 2>&1 && fail "migration alterada deveria falhar"

echo "migrations: rejeita migration removida ou renomeada"
new_repo
git mv src/main/resources/db/migration/V002__b.sql src/main/resources/db/migration/V002__renomeada.sql
git commit -q -m renomeia
bash "$CHECK" main >/dev/null 2>&1 && fail "migration renomeada deveria falhar"

echo "migrations: rejeita versão duplicada"
new_repo
echo "select 3;" >src/main/resources/db/migration/V002__outra.sql
git add -A && git commit -q -m duplicada
bash "$CHECK" main >/dev/null 2>&1 && fail "versão duplicada deveria falhar"

echo "migrations: rejeita nome fora do padrão Flyway"
new_repo
echo "select 3;" >src/main/resources/db/migration/migration_3.sql
git add -A && git commit -q -m nome
bash "$CHECK" main >/dev/null 2>&1 && fail "nome inválido deveria falhar"

[ "$FAILURES" -eq 0 ] && echo "OK" || { echo "$FAILURES falha(s)"; exit 1; }
