#!/usr/bin/env bash
#
# Instala (ou corrige) as tarefas agendadas de backup.
#
# Duas entradas no cron, com papéis distintos:
#   1. o backup diário
#   2. um vigia que cobra o resultado do backup
#
# O vigia é separado de propósito. Em 2026 o projeto mudou de diretório e a
# entrada do cron continuou apontando para o caminho antigo: nada executava,
# nada falhava, nada avisava. Passaram-se 126 dias com um único backup, de
# quatro meses antes. Um alerta emitido pelo próprio backup não teria ajudado —
# o backup não rodava.

set -Eeuo pipefail

if [[ "${GEOSEGBAR_CLI_CONTEXT:-0}" != "1" ]]; then
	echo "❌ Execução direta não permitida. Use: ./bash/cli_app.sh"
	exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$SCRIPT_DIR"

BACKUP_SCRIPT="$SCRIPT_DIR/bash/scripts/backup_database_prod.sh"
CHECK_SCRIPT="$SCRIPT_DIR/bash/scripts/check_backup_freshness.sh"

CRON_SCHEDULE="${BACKUP_CRON_SCHEDULE:-0 2 * * *}"
# Meio da manhã: se o backup das 2h falhou, o aviso chega em horário de trabalho.
CHECK_SCHEDULE="${BACKUP_CHECK_CRON_SCHEDULE:-0 9 * * *}"

CRON_LOG="$SCRIPT_DIR/logs/cron_backup.log"
CHECK_LOG="$SCRIPT_DIR/logs/cron_backup_check.log"

for s in "$BACKUP_SCRIPT" "$CHECK_SCRIPT"; do
  [[ -f "$s" ]] || { echo "❌ Script não encontrado: $s"; exit 1; }
  chmod +x "$s"
done
mkdir -p "$SCRIPT_DIR/logs"

CRON_JOB="$CRON_SCHEDULE bash $BACKUP_SCRIPT >> $CRON_LOG 2>&1"
CHECK_JOB="$CHECK_SCHEDULE bash $CHECK_SCRIPT >> $CHECK_LOG 2>&1"

CURRENT_CRONTAB="$(crontab -l 2>/dev/null || true)"

# Remove QUALQUER entrada antiga destes scripts, inclusive apontando para
# caminhos que já não existem.
#
# A versão anterior filtrava por "$BACKUP_SCRIPT" — o caminho atual. Uma entrada
# apontando para o diretório antigo do projeto não casava com esse filtro e
# sobrevivia a cada reinstalação, mantendo no ar um cron quebrado enquanto todo
# mundo achava que o backup estava configurado. Filtrar pelo NOME do script pega
# a entrada esteja ela onde estiver.
FILTERED_CRONTAB="$(printf '%s\n' "$CURRENT_CRONTAB" \
                    | grep -v "backup_database_prod.sh" \
                    | grep -v "check_backup_freshness.sh" || true)"

ENTRADAS_REMOVIDAS="$(printf '%s\n' "$CURRENT_CRONTAB" \
                      | grep -cE "backup_database_prod\.sh|check_backup_freshness\.sh" || true)"

printf "%s\n%s\n%s\n" "$FILTERED_CRONTAB" "$CRON_JOB" "$CHECK_JOB" \
  | sed '/^$/N;/^\n$/D' \
  | crontab -

echo "✅ Cron de backup configurado"
echo ""
[[ "${ENTRADAS_REMOVIDAS:-0}" -gt 0 ]] && \
  echo "🧹 ${ENTRADAS_REMOVIDAS} entrada(s) antiga(s) removida(s) — inclusive de caminhos obsoletos"
echo "📅 Backup diário:   $CRON_SCHEDULE"
echo "🔎 Verificação:     $CHECK_SCHEDULE"
echo "📁 Diretório atual: $SCRIPT_DIR"
echo ""

# Confere se o que ficou instalado aponta para arquivos que existem de fato.
# É esta checagem que teria evitado 126 dias de silêncio.
echo "🔍 Validando as entradas instaladas:"
PROBLEMA=false
while IFS= read -r linha; do
  caminho="$(printf '%s' "$linha" | grep -oE '/[^ ]*\.sh' | head -1)"
  [[ -z "$caminho" ]] && continue
  if [[ -f "$caminho" ]]; then
    echo "   ✅ $caminho"
  else
    echo "   ❌ $caminho — NÃO EXISTE"
    PROBLEMA=true
  fi
done < <(crontab -l 2>/dev/null | grep -E "backup_database_prod\.sh|check_backup_freshness\.sh")

if [[ "$PROBLEMA" == true ]]; then
  echo ""
  echo "❌ Há entrada de cron apontando para arquivo inexistente."
  exit 1
fi

echo ""
echo "💡 Para testar o backup agora, sem esperar as 2h:"
echo "   ./bash/cli_app.sh  ->  Backup banco produção"
