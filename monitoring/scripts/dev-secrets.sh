#!/usr/bin/env bash
#
# Cria segredos FALSOS para rodar o monitoramento em desenvolvimento.
# Nunca use estes valores fora da sua máquina.

set -Eeuo pipefail

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)/.secrets-dev"
mkdir -p "$DIR"
chmod 700 "$DIR"

escreve() {
  local nome="$1" valor="$2"
  [[ -f "$DIR/$nome" ]] && return 0
  printf '%s' "$valor" > "$DIR/$nome"
  chmod 444 "$DIR/$nome"
}

escreve grafana_admin_password "admin-dev-$(openssl rand -hex 6)"
# Em dev o exporter usa o usuário do Postgres de dev (PG_EXPORTER_USER em .env.dev):
# troque este valor pela senha dele (DB_PASSWORD do .env).
escreve pg_monitoring_password "postgres"
escreve telegram_bot_token "000000:dev-sem-telegram"
escreve telegram_chat_id "0"
escreve healthchecks_watchdog_url "http://localhost:9/dev-sem-healthchecks"

echo "Segredos de dev em $DIR"
echo "Senha do Grafana (usuário admin): $(cat "$DIR/grafana_admin_password")"
