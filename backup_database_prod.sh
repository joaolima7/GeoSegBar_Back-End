set -e

echo "🔍 Iniciando backup do banco de dados..."

# Carregar variáveis de ambiente de produção
set -a
source .env.prod
set +a

# Diretório para backups
BACKUP_DIR="./database_backups"
BACKUP_FILE="$BACKUP_DIR/geosegbar_backup_$(date +%Y%m%d_%H%M%S).sql"

# Criar diretório de backup se não existir
mkdir -p $BACKUP_DIR

echo "📊 Realizando backup do banco de dados atual..."
docker exec postgres-prod pg_dump -U ${DB_USERNAME} ${DB_NAME} > "$BACKUP_FILE"

if [ $? -eq 0 ]; then
    echo "✅ Backup concluído com sucesso: $BACKUP_FILE"
    echo "📏 Tamanho do arquivo: $(du -h "$BACKUP_FILE" | cut -f1)"
else
    echo "❌ Falha ao realizar o backup!"
    exit 1
fi

# Opcional: comprimir o backup para economizar espaço
echo "🗜️ Comprimindo arquivo de backup..."
gzip -f "$BACKUP_FILE"
COMPRESSED_FILE="${BACKUP_FILE}.gz"

if [ -f "$COMPRESSED_FILE" ]; then
    echo "✅ Backup comprimido: $COMPRESSED_FILE"
    echo "📏 Tamanho após compressão: $(du -h "$COMPRESSED_FILE" | cut -f1)"
else
    echo "⚠️ Compressão falhou, mas o backup original está disponível"
fi

echo "🎉 Processo de backup concluído!"