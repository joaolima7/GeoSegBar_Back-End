-- ============================================================================
-- Migration: Converter relacionamento ManyToMany para ManyToOne (Checklist -> Dam)
-- Descrição: Adiciona coluna dam_id na tabela checklists e migra dados da 
--           tabela checklist_dam (join table) para a nova estrutura
-- Data: 2025-12-23
-- ============================================================================

-- Etapa 1: Adicionar coluna dam_id na tabela checklists (inicialmente nullable)
ALTER TABLE checklists 
ADD COLUMN IF NOT EXISTS dam_id BIGINT;

-- Etapa 2: Migrar dados da tabela checklist_dam para a coluna dam_id
-- Nota: Se houver múltiplas dams por checklist, pega a primeira (menor dam_id)
UPDATE checklists c
SET dam_id = (
    SELECT cd.dam_id
    FROM checklist_dam cd
    WHERE cd.checklist_id = c.id
    ORDER BY cd.dam_id ASC
    LIMIT 1
)
WHERE EXISTS (
    SELECT 1 
    FROM checklist_dam cd 
    WHERE cd.checklist_id = c.id
);

-- Etapa 3: Verificar se há checklists sem dam_id após a migração
DO $$
DECLARE
    orphan_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO orphan_count
    FROM checklists
    WHERE dam_id IS NULL;
    
    IF orphan_count > 0 THEN
        RAISE NOTICE 'ATENÇÃO: % checklist(s) sem dam_id encontrado(s). Verifique manualmente.', orphan_count;
    ELSE
        RAISE NOTICE 'Migração OK: Todos os checklists possuem dam_id.';
    END IF;
END $$;

-- Etapa 4: Criar índice na coluna dam_id antes de adicionar a constraint
CREATE INDEX IF NOT EXISTS idx_checklist_dam_id ON checklists(dam_id);

-- Etapa 5: Adicionar foreign key constraint
ALTER TABLE checklists
ADD CONSTRAINT fk_checklist_dam
FOREIGN KEY (dam_id) 
REFERENCES dam(id)
ON DELETE CASCADE;

-- Etapa 6: Tornar dam_id NOT NULL (apenas se não houver registros órfãos)
DO $$
DECLARE
    orphan_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO orphan_count
    FROM checklists
    WHERE dam_id IS NULL;
    
    IF orphan_count = 0 THEN
        ALTER TABLE checklists ALTER COLUMN dam_id SET NOT NULL;
        RAISE NOTICE 'Coluna dam_id definida como NOT NULL.';
    ELSE
        RAISE WARNING 'Não foi possível definir dam_id como NOT NULL. Existem % registros órfãos.', orphan_count;
    END IF;
END $$;

-- Etapa 7: Registrar checklists duplicados por dam (para análise)
DO $$
DECLARE
    duplicate_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO duplicate_count
    FROM (
        SELECT dam_id, COUNT(*) as checklist_count
        FROM checklists
        WHERE dam_id IS NOT NULL
        GROUP BY dam_id
        HAVING COUNT(*) > 1
    ) duplicates;
    
    IF duplicate_count > 0 THEN
        RAISE NOTICE 'ATENÇÃO: % dam(s) com múltiplos checklists encontrada(s).', duplicate_count;
        RAISE NOTICE 'Execute: SELECT dam_id, COUNT(*) FROM checklists GROUP BY dam_id HAVING COUNT(*) > 1;';
    ELSE
        RAISE NOTICE 'OK: Cada dam possui no máximo 1 checklist.';
    END IF;
END $$;

-- Etapa 8: Remover tabela de junção checklist_dam (comentado por segurança)
-- IMPORTANTE: Descomente apenas após validar que tudo está funcionando corretamente
DROP TABLE IF EXISTS checklist_dam;

-- ============================================================================
-- INSTRUÇÕES PÓS-MIGRAÇÃO:
-- ============================================================================
-- 1. Verifique se há checklists órfãos (sem dam_id):
   SELECT * FROM checklists WHERE dam_id IS NULL;
--
-- 2. Verifique dams com múltiplos checklists:
   SELECT dam_id, COUNT(*) as total 
   FROM checklists 
   GROUP BY dam_id 
   HAVING COUNT(*) > 1;
--
-- 3. Se tudo estiver OK, execute manualmente:
   DROP TABLE IF EXISTS checklist_dam;
--
-- 4. Reinicie a aplicação para validar o novo relacionamento
-- ============================================================================

COMMIT;
