#!/usr/bin/env bash
#
# Vigia dos backups: avisa quando param de acontecer.
#
# Existe porque um alerta emitido pelo próprio script de backup só dispara se o
# script rodar. Em 2026 o cron apontava para um caminho que não existia mais —
# nada executava, nada falhava, nada avisava, e o backup mais recente tinha 126
# dias quando alguém foi olhar.
#
# Este script roda por conta própria e cobra o resultado. Se o backup morrer de
# novo, por qualquer motivo, alguém fica sabendo em até um dia.

set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$SCRIPT_DIR"

STATUS_FILE="$SCRIPT_DIR/logs/backup_status.json"
LOG_FILE="$SCRIPT_DIR/logs/backup_freshness.log"
BACKUP_BASE_DIR="${BACKUP_BASE_DIR:-$HOME/db_backups}"

# Backup é diário; 48h dá margem para uma noite ruim sem falso alarme.
MAX_IDADE_HORAS="${BACKUP_MAX_AGE_HOURS:-48}"

mkdir -p "$SCRIPT_DIR/logs"

log() {
  echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a "$LOG_FILE"
}

if [[ -f "$SCRIPT_DIR/.env.prod" ]]; then
  set -a
  # shellcheck disable=SC1091
  source "$SCRIPT_DIR/.env.prod"
  set +a
fi

alerta() {
  local assunto="$1"
  local corpo="$2"

  log "ALERTA: ${assunto}"

  local destino="${BACKUP_ALERT_EMAIL:-${MAIL_USERNAME:-}}"
  [[ -n "$destino" && -n "${MAIL_HOST:-}" && -n "${MAIL_USERNAME:-}" ]] || {
    log "AVISO: e-mail não configurado — alerta ficou apenas no log"
    return 0
  }

  local mensagem
  mensagem="From: ${MAIL_USERNAME}
To: ${destino}
Subject: [GeoSegBar] ${assunto}

${corpo}

Servidor: $(hostname)
Verificado em: $(date '+%d/%m/%Y %H:%M:%S')
"

  curl --silent --show-error --ssl-reqd \
    --url "smtps://${MAIL_HOST}:${MAIL_PORT:-465}" \
    --user "${MAIL_USERNAME}:${MAIL_PASSWORD}" \
    --mail-from "${MAIL_USERNAME}" \
    --mail-rcpt "${destino}" \
    --upload-file <(printf '%s' "$mensagem") >/dev/null 2>&1 \
    && log "Alerta enviado para ${destino}" \
    || log "AVISO: não foi possível enviar o alerta por e-mail"
}

AGORA="$(date +%s)"
LIMITE_SEGUNDOS=$((MAX_IDADE_HORAS * 3600))

# ---- 1) O backup chegou a rodar alguma vez? ----
if [[ ! -f "$STATUS_FILE" ]]; then
  alerta "Backup do banco NUNCA foi executado" \
    "Não existe ${STATUS_FILE}.

Isso indica que a rotina de backup nunca rodou neste servidor — provavelmente o
cron não está instalado ou aponta para um caminho errado.

Para configurar:  ./bash/cli_app.sh  ->  Configurar cron de backup"
  exit 1
fi

# ---- 2) Quando foi o último resultado, e qual? ----
ULTIMO_EPOCH="$(grep -oE '"epoch"[[:space:]]*:[[:space:]]*[0-9]+' "$STATUS_FILE" | grep -oE '[0-9]+$' || echo 0)"
RESULTADO="$(grep -oE '"resultado"[[:space:]]*:[[:space:]]*"[^"]*"' "$STATUS_FILE" | sed 's/.*"\([^"]*\)"$/\1/' || echo desconhecido)"

IDADE_SEGUNDOS=$((AGORA - ULTIMO_EPOCH))
IDADE_HORAS=$((IDADE_SEGUNDOS / 3600))

if [[ "$ULTIMO_EPOCH" -eq 0 ]]; then
  alerta "Não foi possível ler a data do último backup" \
    "O arquivo ${STATUS_FILE} existe mas está ilegível ou corrompido."
  exit 1
fi

if [[ "$IDADE_SEGUNDOS" -gt "$LIMITE_SEGUNDOS" ]]; then
  alerta "Backup do banco está DESATUALIZADO (${IDADE_HORAS}h)" \
    "O último backup registrado tem ${IDADE_HORAS} horas — o limite aceitável é ${MAX_IDADE_HORAS}h.

Último resultado registrado: ${RESULTADO}

Verifique se o cron ainda existe e se aponta para o caminho certo:
  crontab -l | grep backup

Log do backup: ${SCRIPT_DIR}/logs/backup.log"
  exit 1
fi

if [[ "$RESULTADO" != "sucesso" ]]; then
  alerta "Último backup terminou como '${RESULTADO}'" \
    "O backup rodou há ${IDADE_HORAS}h, mas não concluiu com sucesso.

Resultado: ${RESULTADO}

Se for 'parcial', o dump local existe mas não foi para o S3 — existe apenas uma
cópia, no próprio servidor.

Log: ${SCRIPT_DIR}/logs/backup.log"
  exit 1
fi

# ---- 3) O arquivo mais recente existe mesmo em disco? ----
MAIS_RECENTE="$(find "$BACKUP_BASE_DIR" -type f -name "*.sql.gz" -printf '%T@ %p\n' 2>/dev/null \
                | sort -rn | head -1 | cut -d' ' -f2- || true)"

if [[ -z "$MAIS_RECENTE" ]]; then
  alerta "Status diz sucesso, mas não há arquivo de backup em disco" \
    "Nenhum .sql.gz encontrado em ${BACKUP_BASE_DIR}, apesar de o status indicar sucesso."
  exit 1
fi

TAMANHO="$(stat -c %s "$MAIS_RECENTE" 2>/dev/null || stat -f %z "$MAIS_RECENTE")"
log "OK — último backup há ${IDADE_HORAS}h: $(basename "$MAIS_RECENTE") ($((TAMANHO / 1024)) KB)"
