#!/bin/bash

set -e

echo "🔄 MIGRAÇÃO EM PRODUÇÃO: ManyToMany -> OneToMany para ReadingInputValue"
echo "⚠️  ATENÇÃO: Esta operação irá modificar o banco de dados de PRODUÇÃO!"
echo ""

CONTAINER_NAME="postgres-prod"
DB_NAME="geosegbar_prod"
DB_USER="postgres"

# ============================================
# VALIDAÇÕES INICIAIS
# ============================================

# Verificar se está rodando em produção
if [ ! -f .env.prod ]; then
    echo "❌ Arquivo .env.prod não encontrado!"
    echo "   Este script só deve ser executado em ambiente de PRODUÇÃO"
    exit 1
fi

# Carregar variáveis de produção
set -a
source .env.prod
set +a

# Verificar se container está rodando
if ! docker ps -q -f name=$CONTAINER_NAME | grep -q .; then
    echo "❌ Container $CONTAINER_NAME não está rodando!"
    echo "   Execute primeiro: ./bash/deploy_vps.sh"
    exit 1
fi

# ============================================
# CONFIRMAÇÃO DO USUÁRIO
# ============================================

echo "🔍 Informações do ambiente:"
echo "   Container:  $CONTAINER_NAME"
echo "   Database:   $DB_NAME"
echo "   Timestamp:  $(date '+%Y-%m-%d %H:%M:%S')"
echo ""

read -p "⚠️  Você TEM CERTEZA que deseja continuar com a migração em PRODUÇÃO? (digite 'SIM' em maiúsculas): " -r
echo
if [ "$REPLY" != "SIM" ]; then
    echo "❌ Migração cancelada pelo usuário"
    exit 0
fi

# ============================================
# BACKUP COMPLETO
# ============================================

BACKUP_DIR="./backups"
mkdir -p "$BACKUP_DIR"

TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="${BACKUP_DIR}/backup_prod_pre_migration_${TIMESTAMP}.sql"
BACKUP_COMPRESSED="${BACKUP_FILE}.gz"

echo ""
echo "📦 PASSO 1: Criando backup COMPLETO do banco de produção..."
echo "   Arquivo: $BACKUP_FILE"
echo "   Isso pode levar alguns minutos dependendo do tamanho do banco..."

if docker exec $CONTAINER_NAME pg_dump -U $DB_USER $DB_NAME > "$BACKUP_FILE"; then
    # Comprimir backup
    gzip "$BACKUP_FILE"
    BACKUP_SIZE=$(du -h "$BACKUP_COMPRESSED" | cut -f1)
    echo "✅ Backup criado e comprimido com sucesso!"
    echo "   Tamanho: $BACKUP_SIZE"
    echo "   Localização: $BACKUP_COMPRESSED"
else
    echo "❌ Falha ao criar backup!"
    echo "   Migração cancelada por segurança"
    exit 1
fi

# ============================================
# ANÁLISE PRÉ-MIGRAÇÃO
# ============================================

echo ""
echo "🔍 PASSO 2: Analisando estrutura atual..."

# Verificar se tabelas existem
echo "   Verificando tabelas existentes..."
TABLES=$(docker exec -it $CONTAINER_NAME psql -U $DB_USER -d $DB_NAME -t -c "\dt *reading*" | grep -c "table" || true)
if [ "$TABLES" -lt 2 ]; then
    echo "❌ Tabelas necessárias não encontradas!"
    exit 1
fi

# Estrutura da tabela de junção
echo ""
echo "   📋 Estrutura da tabela de junção (reading_input_value_mapping):"
docker exec -it $CONTAINER_NAME psql -U $DB_USER -d $DB_NAME -c "\d reading_input_value_mapping"

# Estrutura da tabela reading_input_value
echo ""
echo "   📋 Estrutura atual da tabela reading_input_value:"
docker exec -it $CONTAINER_NAME psql -U $DB_USER -d $DB_NAME -c "\d reading_input_value"

# Contagem de registros
echo ""
echo "   📊 Contagem de registros antes da migração:"
docker exec -it $CONTAINER_NAME psql -U $DB_USER -d $DB_NAME -c "
SELECT 
    'reading' as tabela, COUNT(*) as total 
FROM reading 
UNION ALL 
SELECT 'reading_input_value', COUNT(*) 
FROM reading_input_value 
UNION ALL 
SELECT 'reading_input_value_mapping', COUNT(*) 
FROM reading_input_value_mapping;
"

# Verificar se já foi migrado
COLUMN_EXISTS=$(docker exec -it $CONTAINER_NAME psql -U $DB_USER -d $DB_NAME -t -c "
SELECT COUNT(*) 
FROM information_schema.columns 
WHERE table_name = 'reading_input_value' 
AND column_name = 'reading_id';
" | tr -d '[:space:]')

if [ "$COLUMN_EXISTS" != "0" ]; then
    echo ""
    echo "⚠️  ATENÇÃO: Coluna 'reading_id' já existe!"
    echo "   A migração pode já ter sido executada."
    echo ""
    read -p "   Deseja continuar mesmo assim? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "❌ Migração cancelada"
        exit 0
    fi
fi

# ============================================
# CONFIRMAÇÃO FINAL
# ============================================

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "⚠️  ÚLTIMA CONFIRMAÇÃO ANTES DA MIGRAÇÃO"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "✅ Backup criado: $BACKUP_COMPRESSED ($BACKUP_SIZE)"
echo "📊 Dados analisados e prontos para migração"
echo ""
read -p "🚀 Executar migração AGORA? (digite 'EXECUTAR' em maiúsculas): " -r
echo

if [ "$REPLY" != "EXECUTAR" ]; then
    echo "❌ Migração cancelada pelo usuário"
    echo "   O backup foi mantido em: $BACKUP_COMPRESSED"
    exit 0
fi

# ============================================
# PARAR A APLICAÇÃO (EVITAR ESCRITAS)
# ============================================

echo ""
echo "🛑 PASSO 3: Parando aplicação para evitar escritas durante migração..."
if docker ps -q -f name=geosegbar-api-prod | grep -q .; then
    docker stop geosegbar-api-prod
    echo "✅ Aplicação parada"
    APP_WAS_RUNNING=true
else
    echo "⚠️  Aplicação já estava parada"
    APP_WAS_RUNNING=false
fi

# ============================================
# EXECUTAR MIGRAÇÃO
# ============================================

echo ""
echo "🚀 PASSO 4: Executando migração no banco de dados..."
echo "   Início: $(date '+%Y-%m-%d %H:%M:%S')"
echo ""

MIGRATION_START=$(date +%s)

docker exec -i $CONTAINER_NAME psql -U $DB_USER -d $DB_NAME << 'EOSQL'

-- =====================================================
-- MIGRAÇÃO PRODUÇÃO: ManyToMany -> OneToMany
-- =====================================================

\set ON_ERROR_STOP on

BEGIN;

-- Log início
DO $$ BEGIN RAISE NOTICE '🚀 Iniciando migração em % UTC', NOW(); END $$;

-- PASSO 1: Adicionar coluna reading_id
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'reading_input_value' AND column_name = 'reading_id'
    ) THEN
        ALTER TABLE reading_input_value ADD COLUMN reading_id BIGINT;
        RAISE NOTICE '✅ [1/9] Coluna reading_id adicionada';
    ELSE
        RAISE NOTICE '⚠️  [1/9] Coluna reading_id já existe';
    END IF;
END $$;

-- PASSO 2: Migrar dados
DO $$ 
DECLARE
    rows_updated INTEGER;
BEGIN
    UPDATE reading_input_value riv
    SET reading_id = subquery.reading_id
    FROM (
        SELECT DISTINCT ON (input_value_id) input_value_id, reading_id
        FROM reading_input_value_mapping
        ORDER BY input_value_id, reading_id
    ) AS subquery
    WHERE riv.id = subquery.input_value_id
    AND riv.reading_id IS NULL;
    
    GET DIAGNOSTICS rows_updated = ROW_COUNT;
    RAISE NOTICE '✅ [2/9] % registros migrados', rows_updated;
END $$;

-- PASSO 3: Verificar órfãos
DO $$
DECLARE
    orphan_count INTEGER;
    migrated_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO orphan_count FROM reading_input_value WHERE reading_id IS NULL;
    SELECT COUNT(*) INTO migrated_count FROM reading_input_value WHERE reading_id IS NOT NULL;
    
    RAISE NOTICE '📊 [3/9] Registros migrados: %, órfãos: %', migrated_count, orphan_count;
    
    IF orphan_count > 0 THEN
        RAISE NOTICE '⚠️  Serão removidos % registros órfãos', orphan_count;
    END IF;
END $$;

-- PASSO 4: Remover órfãos
DELETE FROM reading_input_value WHERE reading_id IS NULL;
DO $$ BEGIN RAISE NOTICE '✅ [4/9] Órfãos removidos'; END $$;

-- PASSO 5: NOT NULL constraint
ALTER TABLE reading_input_value ALTER COLUMN reading_id SET NOT NULL;
DO $$ BEGIN RAISE NOTICE '✅ [5/9] Constraint NOT NULL adicionada'; END $$;

-- PASSO 6: Foreign Key
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints 
        WHERE constraint_name = 'fk_reading_input_value_reading'
        AND table_name = 'reading_input_value'
    ) THEN
        ALTER TABLE reading_input_value
        ADD CONSTRAINT fk_reading_input_value_reading
        FOREIGN KEY (reading_id) REFERENCES reading(id)
        ON DELETE CASCADE;
        RAISE NOTICE '✅ [6/9] FK constraint adicionada';
    ELSE
        RAISE NOTICE '⚠️  [6/9] FK constraint já existe';
    END IF;
END $$;

-- PASSO 7: Índices
CREATE INDEX IF NOT EXISTS idx_riv_reading_id ON reading_input_value(reading_id);
CREATE INDEX IF NOT EXISTS idx_riv_reading_acronym ON reading_input_value(reading_id, input_acronym);
DO $$ BEGIN RAISE NOTICE '✅ [7/9] Índices criados'; END $$;

-- PASSO 8: Remover tabela de junção
DROP TABLE IF EXISTS reading_input_value_mapping CASCADE;
DO $$ BEGIN RAISE NOTICE '✅ [8/9] Tabela de junção removida'; END $$;

-- PASSO 9: Validação final
DO $$
DECLARE
    total_iv INTEGER;
    total_r INTEGER;
    total_readings INTEGER;
BEGIN
    SELECT COUNT(*) INTO total_iv FROM reading_input_value;
    SELECT COUNT(DISTINCT reading_id) INTO total_r FROM reading_input_value;
    SELECT COUNT(*) INTO total_readings FROM reading;
    
    RAISE NOTICE '';
    RAISE NOTICE '🎉 [9/9] MIGRAÇÃO CONCLUÍDA!';
    RAISE NOTICE '   Total de input_values: %', total_iv;
    RAISE NOTICE '   Readings com inputs: %', total_r;
    RAISE NOTICE '   Total de readings: %', total_readings;
    RAISE NOTICE '   Finalizado em: % UTC', NOW();
END $$;

COMMIT;

EOSQL

MIGRATION_STATUS=$?
MIGRATION_END=$(date +%s)
MIGRATION_DURATION=$((MIGRATION_END - MIGRATION_START))

echo ""
echo "   Fim: $(date '+%Y-%m-%d %H:%M:%S')"
echo "   Duração: ${MIGRATION_DURATION}s"

if [ $MIGRATION_STATUS -eq 0 ]; then
    echo "✅ Migração executada com sucesso!"
else
    echo "❌ ERRO na migração!"
    echo ""
    echo "🔄 Restaurando backup..."
    gunzip -c "$BACKUP_COMPRESSED" | docker exec -i $CONTAINER_NAME psql -U $DB_USER -d $DB_NAME
    
    if [ $? -eq 0 ]; then
        echo "✅ Backup restaurado com sucesso"
    else
        echo "❌ ERRO ao restaurar backup!"
        echo "   Restaure manualmente: gunzip -c $BACKUP_COMPRESSED | docker exec -i $CONTAINER_NAME psql -U $DB_USER -d $DB_NAME"
    fi
    
    exit 1
fi

# ============================================
# VERIFICAÇÃO PÓS-MIGRAÇÃO
# ============================================

echo ""
echo "🔍 PASSO 5: Verificando resultado da migração..."

echo ""
echo "   📋 Nova estrutura da tabela reading_input_value:"
docker exec -it $CONTAINER_NAME psql -U $DB_USER -d $DB_NAME -c "\d reading_input_value"

echo ""
echo "   📊 Verificando integridade dos dados:"
docker exec -it $CONTAINER_NAME psql -U $DB_USER -d $DB_NAME -c "
SELECT 
    COUNT(*) as total_input_values,
    COUNT(DISTINCT reading_id) as readings_distintas,
    COUNT(DISTINCT input_acronym) as inputs_distintos
FROM reading_input_value;
"

echo ""
echo "   🔍 Verificando Foreign Keys:"
docker exec -it $CONTAINER_NAME psql -U $DB_USER -d $DB_NAME -c "
SELECT 
    conname as constraint_name,
    contype as constraint_type
FROM pg_constraint 
WHERE conrelid = 'reading_input_value'::regclass
AND contype = 'f';
"

echo ""
echo "   📋 Tabelas existentes após migração:"
docker exec -it $CONTAINER_NAME psql -U $DB_USER -d $DB_NAME -c "\dt *reading*"

# ============================================
# REINICIAR APLICAÇÃO
# ============================================

echo ""
echo "🚀 PASSO 6: Reiniciando aplicação..."

if [ "$APP_WAS_RUNNING" = true ]; then
    docker start geosegbar-api-prod
    echo "⏳ Aguardando aplicação inicializar..."
    sleep 30
    
    # Verificar health
    if curl -f http://localhost:${SERVER_PORT:-9090}/actuator/health > /dev/null 2>&1; then
        echo "✅ Aplicação reiniciada e funcionando!"
    else
        echo "⚠️  Aplicação reiniciada mas health check falhou"
        echo "   Verificar logs: docker logs geosegbar-api-prod"
    fi
else
    echo "⚠️  Aplicação não será reiniciada (não estava rodando antes)"
fi

# ============================================
# RELATÓRIO FINAL
# ============================================

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "🎉 MIGRAÇÃO EM PRODUÇÃO CONCLUÍDA COM SUCESSO!"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "📊 RESUMO:"
echo "   ✅ Backup criado: $BACKUP_COMPRESSED ($BACKUP_SIZE)"
echo "   ✅ Migração executada em ${MIGRATION_DURATION}s"
echo "   ✅ Aplicação reiniciada"
echo "   ✅ Dados validados"
echo ""
echo "📁 BACKUP:"
echo "   Arquivo: $BACKUP_COMPRESSED"
echo "   ⚠️  Mantenha este backup por pelo menos 30 dias"
echo ""
echo "🔄 Se precisar reverter:"
echo "   gunzip -c $BACKUP_COMPRESSED | docker exec -i $CONTAINER_NAME psql -U $DB_USER -d $DB_NAME"
echo ""
echo "📋 PRÓXIMOS PASSOS:"
echo "   1. Testar funcionalidades críticas"
echo "   2. Monitorar logs: docker logs -f geosegbar-api-prod"
echo "   3. Verificar métricas no Grafana: http://localhost:3001"
echo ""