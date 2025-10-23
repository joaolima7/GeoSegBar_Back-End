set -e

echo "⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️"
echo "⚠️                                      ⚠️"
echo "⚠️  ATENÇÃO! ESTA OPERAÇÃO IRÁ EXCLUIR  ⚠️"
echo "⚠️  TODOS OS DADOS DE PRODUÇÃO!         ⚠️"
echo "⚠️                                      ⚠️"
echo "⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️"
echo ""
echo "Esta operação irá:"
echo "  1. Parar a aplicação em execução"
echo "  2. Excluir o banco de dados atual"
echo "  3. Criar um novo banco de dados vazio"
echo "  4. Reiniciar a aplicação (que realizará a criação das tabelas)"
echo ""
echo "⚠️ RECOMENDAÇÃO: Execute ./backup_database_prod.sh antes para criar um backup!"
echo ""
read -p "Digite 'RESET' para confirmar esta operação: " confirmacao

if [ "$confirmacao" != "RESET" ]; then
    echo "Operação cancelada."
    exit 1
fi

# Carregar variáveis de ambiente de produção
set -a
source .env.prod
set +a

echo "🛑 Parando a aplicação..."
docker stop geosegbar-api-prod || true
docker rm geosegbar-api-prod || true

echo "🗑️ Eliminando o banco de dados atual..."
docker exec postgres-prod psql -U ${DB_USERNAME} -c "DROP DATABASE ${DB_NAME};"

echo "🆕 Criando um novo banco de dados vazio..."
docker exec postgres-prod psql -U ${DB_USERNAME} -c "CREATE DATABASE ${DB_NAME};"

echo "🔄 Limpando o cache Redis..."
docker exec redis-prod redis-cli FLUSHALL

echo "🚀 Reiniciando a aplicação..."
# Usar o script de deploy existente
./deploy_vps.sh

echo "🎉 Reset do banco de dados concluído com sucesso!"