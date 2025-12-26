-- ============================================================================
-- Migration: Adicionar relacionamentos Dam→Template e Client→Question
-- Descrição: Adiciona dam_id em template_questionnaires e client_id em questions
-- Data: 2025-12-26
-- ============================================================================

-- Etapa 1: Adicionar coluna dam_id na tabela template_questionnaires
ALTER TABLE template_questionnaires 
ADD COLUMN IF NOT EXISTS dam_id BIGINT;

-- Etapa 2: Migrar dam_id dos checklists para os templates
-- Pega o dam_id do primeiro checklist associado ao template
UPDATE template_questionnaires tq
SET dam_id = (
    SELECT c.dam_id
    FROM checklist_template_questionnaire ctq
    INNER JOIN checklists c ON c.id = ctq.checklist_id
    WHERE ctq.template_questionnaire_id = tq.id
    ORDER BY c.dam_id ASC
    LIMIT 1
)
WHERE EXISTS (
    SELECT 1 
    FROM checklist_template_questionnaire ctq 
    WHERE ctq.template_questionnaire_id = tq.id
);

-- Etapa 3: Verificar templates órfãos
DO $$
DECLARE
    orphan_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO orphan_count
    FROM template_questionnaires
    WHERE dam_id IS NULL;
    
    IF orphan_count > 0 THEN
        RAISE NOTICE 'ATENÇÃO: % template(s) sem dam_id encontrado(s). Verifique manualmente.', orphan_count;
    ELSE
        RAISE NOTICE 'Migração OK: Todos os templates possuem dam_id.';
    END IF;
END $$;

-- Etapa 4: Criar índice na coluna dam_id
CREATE INDEX IF NOT EXISTS idx_template_questionnaire_dam_id ON template_questionnaires(dam_id);

-- Etapa 5: Adicionar foreign key constraint
ALTER TABLE template_questionnaires
ADD CONSTRAINT fk_template_questionnaire_dam
FOREIGN KEY (dam_id) 
REFERENCES dam(id)
ON DELETE CASCADE;

-- Etapa 6: Tornar dam_id NOT NULL (apenas se não houver registros órfãos)
DO $$
DECLARE
    orphan_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO orphan_count
    FROM template_questionnaires
    WHERE dam_id IS NULL;
    
    IF orphan_count = 0 THEN
        ALTER TABLE template_questionnaires ALTER COLUMN dam_id SET NOT NULL;
        RAISE NOTICE 'Coluna dam_id definida como NOT NULL.';
    ELSE
        RAISE WARNING 'Não foi possível definir dam_id como NOT NULL. Existem % registros órfãos.', orphan_count;
    END IF;
END $$;

-- ============================================================================
-- QUESTÕES - Adicionar relacionamento com Client
-- ============================================================================

-- Etapa 7: Adicionar coluna client_id na tabela questions
ALTER TABLE questions 
ADD COLUMN IF NOT EXISTS client_id BIGINT;

-- Etapa 8: Migrar client_id das dams para as questions através dos templates
-- Pega o client_id da primeira dam associada à questão
UPDATE questions q
SET client_id = (
    SELECT d.client_id
    FROM template_questionnaire_questions tqq
    INNER JOIN template_questionnaires tq ON tq.id = tqq.template_questionnaire_id
    INNER JOIN dam d ON d.id = tq.dam_id
    WHERE tqq.question_id = q.id
    ORDER BY d.client_id ASC
    LIMIT 1
)
WHERE EXISTS (
    SELECT 1 
    FROM template_questionnaire_questions tqq 
    WHERE tqq.question_id = q.id
);

-- Etapa 9: Verificar questões órfãs
DO $$
DECLARE
    orphan_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO orphan_count
    FROM questions
    WHERE client_id IS NULL;
    
    IF orphan_count > 0 THEN
        RAISE NOTICE 'ATENÇÃO: % questão(ões) sem client_id encontrada(s). Verifique manualmente.', orphan_count;
    ELSE
        RAISE NOTICE 'Migração OK: Todas as questões possuem client_id.';
    END IF;
END $$;

-- Etapa 10: Criar índice na coluna client_id
CREATE INDEX IF NOT EXISTS idx_question_client_id ON questions(client_id);

-- Etapa 11: Adicionar foreign key constraint
ALTER TABLE questions
ADD CONSTRAINT fk_question_client
FOREIGN KEY (client_id) 
REFERENCES client(id)
ON DELETE CASCADE;

-- Etapa 12: Tornar client_id NOT NULL (apenas se não houver registros órfãos)
DO $$
DECLARE
    orphan_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO orphan_count
    FROM questions
    WHERE client_id IS NULL;
    
    IF orphan_count = 0 THEN
        ALTER TABLE questions ALTER COLUMN client_id SET NOT NULL;
        RAISE NOTICE 'Coluna client_id definida como NOT NULL.';
    ELSE
        RAISE WARNING 'Não foi possível definir client_id como NOT NULL. Existem % registros órfãos.', orphan_count;
    END IF;
END $$;

COMMIT;
