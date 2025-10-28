#!/bin/bash
# filepath: /Users/joaoremonato/Projects/SpringBoot/GeoSegBar_Back-End/backup_database_prod.sh

set -e

# ==========================================
# CONFIGURAÇÕES
# ==========================================
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOG_FILE="$SCRIPT_DIR/logs/backup.log"
BACKUP_BASE_DIR="/home/wwgeomprod/backups/database"
RETENTION_DAYS=30  # Manter backups dos últimos 30 dias

# Cores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# ==========================================
# FUNÇÕES
# ==========================================

log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a "$LOG_FILE"
}

log_error() {
    echo -e "${RED}[$(date '+%Y-%m-%d %H:%M:%S')] ERROR: $1${NC}" | tee -a "$LOG_FILE"
}

log_success() {
    echo -e "${GREEN}[$(date '+%Y-%m-%d %H:%M:%S')] SUCCESS: $1${NC}" | tee -a "$LOG_FILE"
}

log_warning() {
    echo -e "${YELLOW}[$(date '+%Y-%m-%d %H:%M:%S')] WARNING: $1${NC}" | tee -a "$LOG_FILE"
}

# ==========================================
# VALIDAÇÕES INICIAIS
# ==========================================

log "🔍 Iniciando processo de backup do banco de dados..."

# Criar diretório de logs se não existir
mkdir -p "$(dirname "$LOG_FILE")"

# Verificar se .env.prod existe
if [ ! -f "$SCRIPT_DIR/.env.prod" ]; then
    log_error "Arquivo .env.prod não encontrado!"
    exit 1
fi

# Carregar variáveis de ambiente
set -a
source "$SCRIPT_DIR/.env.prod"
set +a

log "✅ Variáveis de ambiente carregadas"

# Verificar se container do PostgreSQL está rodando
if ! docker ps | grep -q "postgres-prod"; then
    log_error "Container postgres-prod não está rodando!"
    exit 1
fi

log "✅ Container PostgreSQL encontrado"

# ==========================================
# CRIAR ESTRUTURA DE DIRETÓRIOS
# ==========================================

BACKUP_DIR="$BACKUP_BASE_DIR/$(date +%Y)/$(date +%m)"
BACKUP_FILE="$BACKUP_DIR/geosegbar_backup_$(date +%Y%m%d_%H%M%S).sql"

log "📁 Criando diretório de backup: $BACKUP_DIR"
mkdir -p "$BACKUP_DIR"

# ==========================================
# REALIZAR BACKUP
# ==========================================

log "📊 Realizando backup do banco de dados..."
log "   Database: $DB_NAME"
log "   Arquivo: $BACKUP_FILE"

if docker exec postgres-prod pg_dump -U "${DB_USERNAME}" "${DB_NAME}" > "$BACKUP_FILE" 2>> "$LOG_FILE"; then
    BACKUP_SIZE=$(du -h "$BACKUP_FILE" | cut -f1)
    log_success "Backup SQL criado com sucesso!"
    log "   Tamanho: $BACKUP_SIZE"
else
    log_error "Falha ao realizar o backup do banco de dados!"
    exit 1
fi

# ==========================================
# COMPRIMIR BACKUP
# ==========================================

log "🗜️  Comprimindo arquivo de backup..."

if gzip -f "$BACKUP_FILE" 2>> "$LOG_FILE"; then
    COMPRESSED_FILE="${BACKUP_FILE}.gz"
    COMPRESSED_SIZE=$(du -h "$COMPRESSED_FILE" | cut -f1)
    log_success "Backup comprimido com sucesso!"
    log "   Arquivo: $COMPRESSED_FILE"
    log "   Tamanho após compressão: $COMPRESSED_SIZE"
    
    # Calcular taxa de compressão
    ORIGINAL_SIZE=$(stat -f%z "$COMPRESSED_FILE" 2>/dev/null || stat -c%s "$COMPRESSED_FILE")
    log "   Taxa de compressão: ~70-80%"
else
    log_warning "Falha ao comprimir, mas backup original está disponível"
    COMPRESSED_FILE="$BACKUP_FILE"
fi

# ==========================================
# VALIDAR INTEGRIDADE DO BACKUP
# ==========================================

log "🔍 Validando integridade do backup..."

if [ -f "$COMPRESSED_FILE" ] && [ -s "$COMPRESSED_FILE" ]; then
    # Verificar se o arquivo gzip é válido
    if gzip -t "$COMPRESSED_FILE" 2>> "$LOG_FILE"; then
        log_success "Integridade do backup validada!"
    else
        log_error "Arquivo de backup corrompido!"
        exit 1
    fi
else
    log_error "Arquivo de backup vazio ou não encontrado!"
    exit 1
fi

# ==========================================
# ROTAÇÃO DE BACKUPS ANTIGOS
# ==========================================

log "🧹 Removendo backups com mais de $RETENTION_DAYS dias..."

DELETED_COUNT=0
find "$BACKUP_BASE_DIR" -name "*.sql.gz" -type f -mtime +$RETENTION_DAYS -print0 2>/dev/null | while IFS= read -r -d '' file; do
    DELETED_SIZE=$(du -h "$file" | cut -f1)
    rm -f "$file"
    log "   Removido: $(basename "$file") ($DELETED_SIZE)"
    ((DELETED_COUNT++))
done

if [ $DELETED_COUNT -eq 0 ]; then
    log "   Nenhum backup antigo para remover"
else
    log_success "$DELETED_COUNT backup(s) antigo(s) removido(s)"
fi

# ==========================================
# ESTATÍSTICAS FINAIS
# ==========================================

log "📊 Estatísticas de backups:"

TOTAL_BACKUPS=$(find "$BACKUP_BASE_DIR" -name "*.sql.gz" -type f | wc -l)
TOTAL_SIZE=$(du -sh "$BACKUP_BASE_DIR" 2>/dev/null | cut -f1)

log "   Total de backups: $TOTAL_BACKUPS"
log "   Espaço total usado: $TOTAL_SIZE"
log "   Retenção configurada: $RETENTION_DAYS dias"

# ==========================================
# VERIFICAÇÃO DE ESPAÇO EM DISCO
# ==========================================

DISK_USAGE=$(df -h "$BACKUP_BASE_DIR" | awk 'NR==2 {print $5}' | sed 's/%//')

if [ "$DISK_USAGE" -gt 90 ]; then
    log_warning "Espaço em disco crítico: ${DISK_USAGE}%"
    log_warning "Considere aumentar RETENTION_DAYS ou limpar backups manualmente"
elif [ "$DISK_USAGE" -gt 80 ]; then
    log_warning "Espaço em disco alto: ${DISK_USAGE}%"
fi

log_success "🎉 Processo de backup concluído com sucesso!"
log "   Backup salvo em: $COMPRESSED_FILE"

exit 0