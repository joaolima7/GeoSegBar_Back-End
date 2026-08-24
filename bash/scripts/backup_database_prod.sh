#!/usr/bin/env bash
#
# Backup do banco de produção: dump local verificado + cópia no S3.
#
# Duas cópias com propósitos diferentes:
#   - local, em $BACKUP_BASE_DIR — restauração rápida, sem depender de rede
#   - S3, no mesmo bucket dos arquivos do sistema — sobrevive à perda do servidor
#
# Um backup que ninguém verifica não é backup. Por isso aqui se confere a
# integridade do dump antes de subir, se confere o tamanho depois de subir, e o
# resultado fica registrado em backup_status.json — que é o que
# check_backup_freshness.sh usa para gritar quando os backups param.

set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$SCRIPT_DIR"

LOG_FILE="$SCRIPT_DIR/logs/backup.log"
STATUS_FILE="$SCRIPT_DIR/logs/backup_status.json"
BACKUP_BASE_DIR="${BACKUP_BASE_DIR:-$HOME/db_backups}"
BACKUP_KEEP_COUNT="${BACKUP_KEEP_COUNT:-7}"

# Prefixo no bucket. Fica separado dos arquivos do sistema (PSB, imagens) para
# que uma regra de ciclo de vida do S3 possa tratar backup com retenção própria.
S3_PREFIX="${BACKUP_S3_PREFIX:-db-backups}"

# Upload ao S3 pode ser desligado, mas o padrão é ligado: era a ausência da
# cópia externa que deixava tudo dependente de um único servidor.
BACKUP_S3_ENABLED="${BACKUP_S3_ENABLED:-true}"
S3_MAX_ATTEMPTS="${BACKUP_S3_MAX_ATTEMPTS:-4}"

mkdir -p "$SCRIPT_DIR/logs" "$BACKUP_BASE_DIR"

log() {
  echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a "$LOG_FILE"
}

# Registra o desfecho em JSON. É a fonte da verdade para saber se os backups
# ainda estão acontecendo — a falha de 2026 passou 126 dias despercebida porque
# não havia nada parecido com isto.
write_status() {
  local resultado="$1"
  local detalhe="$2"
  cat > "$STATUS_FILE" <<JSON
{
  "resultado": "${resultado}",
  "detalhe": "${detalhe//\"/\'}",
  "timestamp": "$(date -Iseconds)",
  "epoch": $(date +%s),
  "arquivo_local": "${GZIP_FILE:-}",
  "chave_s3": "${S3_KEY:-}",
  "s3_habilitado": ${BACKUP_S3_ENABLED}
}
JSON
}

# Aviso por e-mail usando o mesmo SMTP da aplicação. Sem isto, uma falha só
# aparece para quem for ler o log — que é ninguém, até precisar restaurar.
notify_failure() {
  local motivo="$1"
  local destino="${BACKUP_ALERT_EMAIL:-${MAIL_USERNAME:-}}"

  [[ -n "$destino" && -n "${MAIL_HOST:-}" && -n "${MAIL_USERNAME:-}" ]] || {
    log "AVISO: e-mail de alerta não configurado (defina BACKUP_ALERT_EMAIL)"
    return 0
  }

  local corpo
  corpo="From: ${MAIL_USERNAME}
To: ${destino}
Subject: [GeoSegBar] FALHA no backup do banco de producao

O backup automatico do banco de producao falhou.

Motivo: ${motivo}
Servidor: $(hostname)
Horario: $(date '+%d/%m/%Y %H:%M:%S')

Ultimas linhas do log:
$(tail -20 "$LOG_FILE" 2>/dev/null)
"

  if curl --silent --show-error --ssl-reqd \
       --url "smtps://${MAIL_HOST}:${MAIL_PORT:-465}" \
       --user "${MAIL_USERNAME}:${MAIL_PASSWORD}" \
       --mail-from "${MAIL_USERNAME}" \
       --mail-rcpt "${destino}" \
       --upload-file <(printf '%s' "$corpo") >/dev/null 2>&1; then
    log "Alerta de falha enviado para ${destino}"
  else
    log "AVISO: não foi possível enviar o e-mail de alerta"
  fi
}

die() {
  log "ERRO: $1"
  write_status "falha" "$1"
  notify_failure "$1"
  exit 1
}

trap 'die "Interrompido inesperadamente na linha $LINENO"' ERR

# ---------------------------------------------------------------- pré-checagem
[[ -f "$SCRIPT_DIR/.env.prod" ]] || die "Arquivo .env.prod não encontrado em $SCRIPT_DIR"

set -a
# shellcheck disable=SC1091
source "$SCRIPT_DIR/.env.prod"
set +a

docker info >/dev/null 2>&1 || die "Docker não está rodando"
docker ps -q -f name=postgres-prod | grep -q . || die "Container postgres-prod não está rodando"

[[ "$BACKUP_KEEP_COUNT" =~ ^[0-9]+$ ]] || die "BACKUP_KEEP_COUNT deve ser numérico"
[[ "$BACKUP_KEEP_COUNT" -gt 0 ]] || die "BACKUP_KEEP_COUNT deve ser maior que zero"

BACKUP_DIR="$BACKUP_BASE_DIR/$(date +%Y)/$(date +%m)"
STAMP="$(date +%Y%m%d_%H%M%S)"
BACKUP_FILE="$BACKUP_DIR/geosegbar_backup_${STAMP}.sql"
GZIP_FILE="$BACKUP_FILE.gz"
S3_KEY="${S3_PREFIX}/$(date +%Y)/$(date +%m)/geosegbar_backup_${STAMP}.sql.gz"

mkdir -p "$BACKUP_DIR"

# Espaço em disco antes de começar: um dump que enche o disco derruba o Postgres
# junto, o que transforma uma rotina de proteção em incidente.
ESPACO_LIVRE_KB="$(df -Pk "$BACKUP_BASE_DIR" | awk 'NR==2 {print $4}')"
TAMANHO_BANCO_KB="$(docker exec postgres-prod psql -U "$DB_USERNAME" -d "$DB_NAME" -tAc \
                    "SELECT pg_database_size(current_database()) / 1024;" 2>/dev/null || echo 0)"
if [[ "$TAMANHO_BANCO_KB" -gt 0 && "$ESPACO_LIVRE_KB" -lt "$TAMANHO_BANCO_KB" ]]; then
  die "Espaço insuficiente: ${ESPACO_LIVRE_KB}KB livres para um banco de ${TAMANHO_BANCO_KB}KB"
fi

# ---------------------------------------------------------------------- dump
log "Iniciando backup do banco: $DB_NAME"

if ! docker exec postgres-prod pg_dump -U "$DB_USERNAME" "$DB_NAME" > "$BACKUP_FILE"; then
  rm -f "$BACKUP_FILE"
  die "pg_dump falhou"
fi

# Verificação de completude. O pg_dump em texto termina sempre com este marcador;
# sua ausência significa dump truncado — que passaria por bom sem esta checagem,
# porque o arquivo existe e o exit code pode ter sido 0.
if ! tail -5 "$BACKUP_FILE" | grep -q "PostgreSQL database dump complete"; then
  rm -f "$BACKUP_FILE"
  die "Dump truncado: marcador de conclusão do pg_dump ausente"
fi

TAMANHO_DUMP="$(stat -c %s "$BACKUP_FILE" 2>/dev/null || stat -f %z "$BACKUP_FILE")"
[[ "$TAMANHO_DUMP" -gt 1024 ]] || { rm -f "$BACKUP_FILE"; die "Dump suspeito: apenas ${TAMANHO_DUMP} bytes"; }

gzip -f "$BACKUP_FILE"

# gzip -t detecta corrupção que só apareceria na hora de restaurar.
gzip -t "$GZIP_FILE" 2>/dev/null || die "Arquivo comprimido corrompido: $GZIP_FILE"

TAMANHO_GZIP="$(stat -c %s "$GZIP_FILE" 2>/dev/null || stat -f %z "$GZIP_FILE")"
log "Dump local concluído e verificado: $GZIP_FILE ($((TAMANHO_GZIP / 1024)) KB)"

# ------------------------------------------------------------------- upload S3
#
# O envio é feito por bash/scripts/lib/s3_client.py, que assina o SigV4 com a
# biblioteca padrão do Python. O `--aws-sigv4` do curl 7.76 — versão do servidor
# — não assina PUT com corpo corretamente: calcula o hash do payload por conta
# própria e ignora o x-amz-content-sha256 informado, devolvendo 400 sem o header
# e 403 SignatureDoesNotMatch com ele. O curl segue sendo usado só nas
# requisições SEM corpo (HEAD e listagem), onde funciona.
#
# SHA-256 do payload vazio — valor fixo, exigido pelo S3 em requisições sem corpo.
SHA256_VAZIO="e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"

upload_para_s3() {
  local enviador="$SCRIPT_DIR/bash/scripts/lib/s3_client.py"

  if [[ ! -f "$enviador" ]]; then
    log "ERRO: enviador não encontrado em ${enviador}"
    return 1
  fi

  local tentativa=1
  while [[ "$tentativa" -le "$S3_MAX_ATTEMPTS" ]]; do
    log "Enviando ao S3 (tentativa ${tentativa}/${S3_MAX_ATTEMPTS}): s3://${AWS_BUCKET_NAME}/${S3_KEY}"

    local saida
    if saida="$(python3 "$enviador" put "$GZIP_FILE" "$AWS_BUCKET_NAME" "$AWS_REGION" "$S3_KEY" 2>&1)"; then

      # Confere o que chegou do outro lado. Um PUT que retorna 200 mas grava
      # menos bytes deixaria um backup inútil parecendo íntegro.
      local tamanho_remoto
      tamanho_remoto="$(curl --fail --silent --head \
                          --aws-sigv4 "aws:amz:${AWS_REGION}:s3" \
                          --user "${AWS_ACCESS_KEY_ID}:${AWS_SECRET_ACCESS_KEY}" \
                          --header "x-amz-content-sha256: ${SHA256_VAZIO}" \
                          --max-time 60 \
                          "https://${AWS_BUCKET_NAME}.s3.${AWS_REGION}.amazonaws.com/${S3_KEY}" 2>/dev/null \
                        | awk 'tolower($1) == "content-length:" {gsub(/\r/,"",$2); print $2}')"

      if [[ "$tamanho_remoto" == "$TAMANHO_GZIP" ]]; then
        log "Upload confirmado no S3: ${tamanho_remoto} bytes conferem com o arquivo local"
        return 0
      fi

      log "AVISO: tamanho no S3 (${tamanho_remoto:-vazio}) difere do local (${TAMANHO_GZIP}). Repetindo."
    else
      log "   ${saida}"
    fi

    # Espera crescente: 5s, 10s, 20s. Falha de rede costuma ser transitória.
    local espera=$((5 * (2 ** (tentativa - 1))))
    [[ "$tentativa" -lt "$S3_MAX_ATTEMPTS" ]] && { log "Aguardando ${espera}s..."; sleep "$espera"; }
    tentativa=$((tentativa + 1))
  done

  return 1
}

S3_OK=false
if [[ "$BACKUP_S3_ENABLED" == "true" ]]; then
  if [[ -z "${AWS_BUCKET_NAME:-}" || -z "${AWS_REGION:-}" || -z "${AWS_ACCESS_KEY_ID:-}" ]]; then
    die "S3 habilitado mas faltam credenciais no .env.prod (AWS_BUCKET_NAME, AWS_REGION, AWS_ACCESS_KEY_ID)"
  fi

  if upload_para_s3; then
    S3_OK=true
  else
    # O dump local existe e está íntegro — não é motivo para descartar tudo.
    # Mas também não pode passar em silêncio: sem a cópia externa, esta noite
    # ficou dependendo de o servidor sobreviver.
    log "ERRO: falha ao enviar para o S3 após ${S3_MAX_ATTEMPTS} tentativas"
    write_status "parcial" "Dump local OK, mas o envio ao S3 falhou"
    notify_failure "Backup local foi criado, mas o envio ao S3 falhou após ${S3_MAX_ATTEMPTS} tentativas. Existe apenas a cópia no servidor."
  fi
else
  log "Upload ao S3 desabilitado (BACKUP_S3_ENABLED=false)"
fi

# -------------------------------------------------------------- rotação local
# Só a cópia local é rotacionada aqui. A retenção no S3 fica por conta de uma
# regra de ciclo de vida do bucket: apagar backup por script é justamente o tipo
# de automação que, com um bug, remove tudo de uma vez.
mapfile -t ALL_BACKUPS < <(find "$BACKUP_BASE_DIR" -type f -name "*.sql.gz" | sort -r)
TOTAL_BACKUPS="${#ALL_BACKUPS[@]}"
REMOVED_BACKUPS=0

if (( TOTAL_BACKUPS > BACKUP_KEEP_COUNT )); then
  for (( i=BACKUP_KEEP_COUNT; i<TOTAL_BACKUPS; i++ )); do
    rm -f "${ALL_BACKUPS[$i]}"
    REMOVED_BACKUPS=$((REMOVED_BACKUPS + 1))
  done
fi

CURRENT_TOTAL=$((TOTAL_BACKUPS - REMOVED_BACKUPS))
log "Rotação local concluída (mantidos: ${BACKUP_KEEP_COUNT}, removidos: ${REMOVED_BACKUPS}, total: ${CURRENT_TOTAL})"

trap - ERR

if [[ "$BACKUP_S3_ENABLED" == "true" && "$S3_OK" != true ]]; then
  # Código 2 = dump local íntegro, cópia externa ausente.
  #
  # Distinto do 1 (nenhum backup) de propósito: para o gate pré-deploy, um dump
  # local verificado já cumpre o papel de ponto de restauração, e travar a
  # publicação por causa da cópia externa transforma um problema de durabilidade
  # em indisponibilidade. Para o cron diário continua sendo falha — quem chama
  # decide o peso.
  log "⚠️  Backup PARCIAL: dump local íntegro, sem cópia no S3"
  exit 2
fi

write_status "sucesso" "Backup concluído"
log "✅ Backup concluído com sucesso"
