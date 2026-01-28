#!/bin/bash

# ============================================
# SCRIPT DE MIGRAÇÃO SEGURA PARA PRODUÇÃO
# ============================================
# Este script aplica a migração de DOUBLE para BIGDECIMAL
# de forma segura com backup e validação
# ============================================

set -e  # Para em caso de erro

echo "🔄 Iniciando migração da coluna value para NUMERIC(20, 10)..."

# Carregar variáveis de produção
if [ ! -f .env.prod ]; then
    echo "❌ Arquivo .env.prod não encontrado!"
    exit 1
fi

source .env.prod

# Função para executar SQL
execute_sql() {
    docker exec -i postgres-prod psql -U "$DB_USERNAME" -d "$DB_NAME" -c "$1"
}

# PASSO 1: Verificar conexão
echo "📊 Verificando conexão com banco de dados..."
if ! docker exec postgres-prod pg_isready -U "$DB_USERNAME" > /dev/null 2>&1; then
    echo "❌ Banco de dados não está acessível!"
    exit 1
fi
echo "✅ Conexão OK"

# PASSO 2: Contar registros
echo ""
echo "📊 Contando registros..."
TOTAL_RECORDS=$(execute_sql "SELECT COUNT(*) FROM reading_input_value;" | grep -o '[0-9]*' | head -1)
echo "   Total de registros: $TOTAL_RECORDS"

# PASSO 3: Criar backup
echo ""
echo "💾 Criando backup da tabela..."
execute_sql "DROP TABLE IF EXISTS reading_input_value_backup_20260128;"
execute_sql "CREATE TABLE reading_input_value_backup_20260128 AS SELECT * FROM reading_input_value;"

BACKUP_COUNT=$(execute_sql "SELECT COUNT(*) FROM reading_input_value_backup_20260128;" | grep -o '[0-9]*' | head -1)
echo "   Backup criado: $BACKUP_COUNT registros"

if [ "$BACKUP_COUNT" != "$TOTAL_RECORDS" ]; then
    echo "❌ Erro: Backup incompleto! ($BACKUP_COUNT != $TOTAL_RECORDS)"
    exit 1
fi
echo "✅ Backup verificado"

# PASSO 4: Alterar tipo da coluna
echo ""
echo "🔧 Alterando tipo da coluna value..."
execute_sql "ALTER TABLE reading_input_value ALTER COLUMN value TYPE NUMERIC(20, 10);"
echo "✅ Coluna alterada"

# PASSO 5: Verificar tipo da coluna
echo ""
echo "🔍 Verificando novo tipo..."
NEW_TYPE=$(execute_sql "SELECT data_type FROM information_schema.columns WHERE table_name = 'reading_input_value' AND column_name = 'value';" | grep -o 'numeric')

if [ "$NEW_TYPE" != "numeric" ]; then
    echo "❌ Erro: Tipo não foi alterado corretamente!"
    echo "🔄 Revertendo mudanças..."
    execute_sql "DROP TABLE reading_input_value; ALTER TABLE reading_input_value_backup_20260128 RENAME TO reading_input_value;"
    exit 1
fi
echo "✅ Tipo verificado: NUMERIC(20, 10)"

# PASSO 6: Validar dados
echo ""
echo "🔍 Validando dados..."
DIFF_COUNT=$(execute_sql "SELECT COUNT(*) FROM reading_input_value_backup_20260128 o JOIN reading_input_value n ON o.id = n.id WHERE ABS(o.value::NUMERIC(20,10) - n.value) > 0.0000000001;" | grep -o '[0-9]*' | head -1)

if [ "$DIFF_COUNT" != "0" ]; then
    echo "⚠️  ATENÇÃO: $DIFF_COUNT registros com diferenças detectadas!"
    echo "   Revise manualmente antes de continuar."
    read -p "Deseja continuar mesmo assim? (s/n): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Ss]$ ]]; then
        echo "🔄 Revertendo mudanças..."
        execute_sql "DROP TABLE reading_input_value; ALTER TABLE reading_input_value_backup_20260128 RENAME TO reading_input_value;"
        exit 1
    fi
else
    echo "✅ Todos os dados validados com sucesso!"
fi

# PASSO 7: Limpar
echo ""
read -p "Deseja remover a tabela de backup? (s/n): " -n 1 -r
echo
if [[ $REPLY =~ ^[Ss]$ ]]; then
    execute_sql "DROP TABLE reading_input_value_backup_20260128;"
    echo "✅ Backup removido"
else
    echo "ℹ️  Backup mantido: reading_input_value_backup_20260128"
    echo "   Você pode removê-lo depois com: DROP TABLE reading_input_value_backup_20260128;"
fi

echo ""
echo "🎉 Migração concluída com sucesso!"
echo ""
echo "📝 Próximos passos:"
echo "   1. Teste a aplicação em produção"
echo "   2. Monitore os logs por 24-48h"
echo "   3. Se tudo estiver OK, remova o backup (se ainda não removeu)"
echo ""
