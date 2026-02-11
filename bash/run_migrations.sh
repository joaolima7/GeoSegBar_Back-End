#!/bin/bash

set -e

echo "🔄 Script de execução manual de migrations"
echo "=========================================="

# Define o diretório raiz do projeto
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$SCRIPT_DIR"

# Verificar se o arquivo .env.prod existe
if [ ! -f .env.prod ]; then
    echo "❌ Arquivo .env.prod não encontrado!"
    exit 1
fi

# Carregar variáveis do .env.prod
set -a
source .env.prod
set +a

# Verificar se o container do postgres está rodando
if ! docker ps -q -f name=postgres-prod | grep -q .; then
    echo "❌ Container postgres-prod não está rodando!"
    echo "💡 Execute: ./bash/deploy_vps.sh primeiro"
    exit 1
fi

# Verificar se o diretório de migrations existe
if [ ! -d "$SCRIPT_DIR/migrations" ]; then
    echo "❌ Diretório de migrations não encontrado!"
    exit 1
fi

# Perguntar se deseja fazer backup antes
read -p "⚠️  Deseja fazer backup do banco antes de executar as migrations? (s/n): " -n 1 -r
echo
if [[ $REPLY =~ ^[Ss]$ ]]; then
    echo "📦 Criando backup..."
    ./bash/backup_database_prod.sh
    echo "✅ Backup criado"
fi

# Listar migrations disponíveis
echo ""
echo "📋 Migrations disponíveis:"
echo "=========================="
ls -1 "$SCRIPT_DIR/migrations"/*.sql 2>/dev/null || echo "Nenhuma migration encontrada"
echo ""

# Perguntar qual migration executar
echo "Opções:"
echo "  1) Executar todas as migrations"
echo "  2) Executar uma migration específica"
echo "  3) Cancelar"
echo ""
read -p "Escolha uma opção (1-3): " option

case $option in
    1)
        echo ""
        echo "🔄 Executando todas as migrations..."
        for migration_file in "$SCRIPT_DIR/migrations"/*.sql; do
            if [ -f "$migration_file" ]; then
                echo ""
                echo "📝 Executando: $(basename "$migration_file")"
                docker exec -i postgres-prod psql -U ${DB_USERNAME} -d ${DB_NAME} < "$migration_file"
                if [ $? -eq 0 ]; then
                    echo "✅ Migration executada com sucesso"
                else
                    echo "❌ Erro ao executar migration"
                    read -p "Deseja continuar? (s/n): " -n 1 -r
                    echo
                    if [[ ! $REPLY =~ ^[Ss]$ ]]; then
                        exit 1
                    fi
                fi
            fi
        done
        echo ""
        echo "✅ Todas as migrations foram processadas"
        ;;
    2)
        echo ""
        read -p "Digite o nome do arquivo da migration: " migration_name
        migration_file="$SCRIPT_DIR/migrations/$migration_name"
        
        if [ ! -f "$migration_file" ]; then
            echo "❌ Migration não encontrada: $migration_name"
            exit 1
        fi
        
        echo ""
        echo "📝 Executando: $migration_name"
        docker exec -i postgres-prod psql -U ${DB_USERNAME} -d ${DB_NAME} < "$migration_file"
        
        if [ $? -eq 0 ]; then
            echo "✅ Migration executada com sucesso"
        else
            echo "❌ Erro ao executar migration"
            exit 1
        fi
        ;;
    3)
        echo "Operação cancelada"
        exit 0
        ;;
    *)
        echo "❌ Opção inválida"
        exit 1
        ;;
esac

echo ""
echo "🎉 Processo concluído!"
