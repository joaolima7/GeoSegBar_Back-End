set -e

if [ -z "$1" ]; then
    echo "❌ Erro: Informe o arquivo de backup"
    echo "Uso: ./restore_database_prod.sh caminho/para/backup.sql[.gz]"
    
    echo ""
    echo "Backups disponíveis:"
    find ./database_backups -type f -name "*.sql*" | sort
    exit 1
fi

BACKUP_FILE=$1

# Verificar se é um arquivo comprimido
if [[ "$BACKUP_FILE" == *.gz ]]; then
    is_compressed=true
    echo "🗜️ Arquivo comprimido detectado"
    TEMP_SQL_FILE="/tmp/temp_restore_$(date +%s).sql"
else
    is_compressed=false
fi

if [ ! -f "$BACKUP_FILE" ]; then
    echo "❌ Erro: Arquivo de backup não encontrado: $BACKUP_FILE"
    exit 1
fi

# Carregar variáveis de ambiente de produção
set -a
source .env.prod
set +a

echo "⚠️ Esta operação irá substituir todos os dados atuais pelos dados do backup."
read -p "Deseja continuar? (s/n): " confirmacao

if [ "$confirmacao" != "s" ]; then
    echo "Operação cancelada."
    exit 1
fi

echo "🛑 Parando a aplicação..."
docker stop geosegbar-api-prod || true

echo "🗑️ Eliminando o banco de dados atual..."
docker exec postgres-prod psql -U ${DB_USERNAME} -c "DROP DATABASE IF EXISTS ${DB_NAME};"

echo "🆕 Criando um novo banco de dados vazio..."
docker exec postgres-prod psql -U ${DB_USERNAME} -c "CREATE DATABASE ${DB_NAME};"

echo "📥 Restaurando dados do backup..."
if [ "$is_compressed" = true ]; then
    echo "🗜️ Descomprimindo arquivo..."
    gunzip -c "$BACKUP_FILE" > "$TEMP_SQL_FILE"
    cat "$TEMP_SQL_FILE" | docker exec -i postgres-prod psql -U ${DB_USERNAME} -d ${DB_NAME}
    echo "🧹 Removendo arquivo temporário..."
    rm "$TEMP_SQL_FILE"
else
    cat "$BACKUP_FILE" | docker exec -i postgres-prod psql -U ${DB_USERNAME} -d ${DB_NAME}
fi

echo "🔄 Limpando o cache Redis..."
docker exec redis-prod redis-cli FLUSHALL

echo "🚀 Reiniciando a aplicação..."
# Usar o script de deploy existente para reiniciar a aplicação
./deploy_vps.sh

echo "✅ Restauração concluída com sucesso!"