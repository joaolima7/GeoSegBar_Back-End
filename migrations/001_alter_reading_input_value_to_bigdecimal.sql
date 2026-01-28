-- ============================================
-- MIGRATION: Alterar tipo da coluna value
-- FROM: DOUBLE PRECISION (Double)
-- TO: NUMERIC(20, 10) (BigDecimal)
-- ============================================
-- DATA: 2026-01-28
-- DESCRIÇÃO: Mudança necessária para manter precisão com zeros à direita
-- ============================================

-- PASSO 1: Verificar dados antes da migração
-- Execute este SELECT para ver quantos registros serão afetados:
SELECT COUNT(*) as total_registros FROM reading_input_value;

-- PASSO 2: Criar backup (IMPORTANTE!)
-- Crie uma tabela de backup antes de alterar:
CREATE TABLE IF NOT EXISTS reading_input_value_backup_20260128 AS
SELECT * FROM reading_input_value;

-- Verificar se backup foi criado:
SELECT COUNT(*) as backup_count FROM reading_input_value_backup_20260128;

-- PASSO 3: Alterar o tipo da coluna
-- PostgreSQL faz a conversão automaticamente de DOUBLE PRECISION para NUMERIC
BEGIN;

ALTER TABLE reading_input_value 
ALTER COLUMN value TYPE NUMERIC(20, 10);

COMMIT;

-- PASSO 4: Verificar a mudança
-- Conferir o novo tipo da coluna:
SELECT 
    column_name, 
    data_type, 
    numeric_precision, 
    numeric_scale
FROM information_schema.columns 
WHERE table_name = 'reading_input_value' 
  AND column_name = 'value';

-- PASSO 5: Validar dados após migração
-- Comparar alguns registros entre backup e tabela migrada:
SELECT 
    o.id,
    o.value as valor_original,
    n.value as valor_novo,
    o.value::NUMERIC(20,10) as valor_convertido,
    CASE 
        WHEN o.value::NUMERIC(20,10) = n.value THEN '✅ OK'
        ELSE '❌ DIFERENTE'
    END as status
FROM reading_input_value_backup_20260128 o
JOIN reading_input_value n ON o.id = n.id
LIMIT 100;

-- PASSO 6: Verificar se há diferenças significativas
SELECT 
    COUNT(*) as registros_diferentes
FROM reading_input_value_backup_20260128 o
JOIN reading_input_value n ON o.id = n.id
WHERE ABS(o.value::NUMERIC(20,10) - n.value) > 0.0000000001;

-- Se tudo estiver OK (0 registros diferentes), você pode remover o backup:
-- DROP TABLE reading_input_value_backup_20260128;

-- ============================================
-- NOTAS IMPORTANTES:
-- ============================================
-- 1. A conversão de DOUBLE PRECISION para NUMERIC é SEGURA no PostgreSQL
-- 2. Valores existentes são mantidos com a mesma precisão
-- 3. Novos valores serão armazenados com até 10 casas decimais
-- 4. O backup fica disponível caso precise reverter
-- 5. Execute em horário de baixo tráfego se possível

-- ============================================
-- ROLLBACK (se necessário):
-- ============================================
-- Se algo der errado, você pode reverter assim:
-- BEGIN;
-- DELETE FROM reading_input_value;
-- INSERT INTO reading_input_value SELECT * FROM reading_input_value_backup_20260128;
-- ALTER TABLE reading_input_value ALTER COLUMN value TYPE DOUBLE PRECISION;
-- COMMIT;
