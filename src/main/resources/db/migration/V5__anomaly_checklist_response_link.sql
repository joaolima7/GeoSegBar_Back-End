-- ============================================================================
-- V5 — Anomalia passa a saber de qual resposta de checklist ela nasceu
-- ============================================================================
--
-- SITUAÇÃO ANTERIOR
--   anomalies guardava questionnaire_id (o id do MODELO, igual para todas as
--   respostas) e question_id, mas nada que apontasse para a checklist_response
--   que a gerou. Quem precisava dessa ligação — o PDF do relatório — casava por
--   proximidade de horário, numa janela de 15 minutos.
--
-- POR QUE A JANELA NÃO BASTA MAIS
--   A data da inspeção passou a ser editável (V6). Assim que alguém lança um
--   checklist retroativo, a data da resposta muda e a da anomalia não: a janela
--   deixa de casar e o PDF para de mostrar anomalias que mostrava antes.
--
-- O QUE ESTA MIGRAÇÃO FAZ
--   Preenche checklist_response_id das anomalias JÁ existentes usando a mesma
--   heurística da janela, uma única vez. Daqui em diante o vínculo é gravado na
--   criação e nenhuma heurística é usada.
--
-- SEGURANÇA
--   Só faz UPDATE em linhas com checklist_response_id IS NULL. Nenhuma linha é
--   excluída, nenhuma coluna é removida. Rodar de novo não muda nada.
--   Anomalias de origem WEB (criadas à mão) são ignoradas de propósito: elas não
--   nasceram de checklist.
-- ============================================================================

DO $$
DECLARE
    qtd_alvo      BIGINT;
    qtd_ligadas   BIGINT;
    qtd_orfas     BIGINT;
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'anomalies'
          AND column_name = 'checklist_response_id'
    ) THEN
        RAISE NOTICE 'V5: coluna checklist_response_id ainda não existe (banco novo) — nada a fazer.';
        RETURN;
    END IF;

    SELECT COUNT(*) INTO qtd_alvo
    FROM anomalies
    WHERE origin = 'CHECKLIST' AND checklist_response_id IS NULL;

    RAISE NOTICE 'V5: % anomalia(s) de checklist sem vínculo.', qtd_alvo;

    IF qtd_alvo = 0 THEN
        RETURN;
    END IF;

    -- Para cada anomalia, a resposta candidata mais próxima no tempo, dentro da
    -- janela. DISTINCT ON garante no máximo uma resposta por anomalia, mesmo que
    -- duas inspeções da mesma barragem e usuário caiam a menos de 15 min.
    WITH candidatas AS (
        SELECT DISTINCT ON (a.id)
               a.id  AS anomaly_id,
               cr.id AS response_id
        FROM anomalies a
        JOIN checklist_responses cr
          ON cr.dam_id  = a.dam_id
         AND cr.user_id = a.user_id
         AND a.created_at >= cr.created_at - INTERVAL '2 seconds'
         AND a.created_at <  cr.created_at + INTERVAL '15 minutes'
        WHERE a.origin = 'CHECKLIST'
          AND a.checklist_response_id IS NULL
        ORDER BY a.id, cr.created_at DESC
    )
    UPDATE anomalies a
    SET checklist_response_id = c.response_id
    FROM candidatas c
    WHERE a.id = c.anomaly_id;

    GET DIAGNOSTICS qtd_ligadas = ROW_COUNT;

    SELECT COUNT(*) INTO qtd_orfas
    FROM anomalies
    WHERE origin = 'CHECKLIST' AND checklist_response_id IS NULL;

    RAISE NOTICE 'V5: % anomalia(s) ligadas; % permaneceram sem vínculo.',
        qtd_ligadas, qtd_orfas;

    -- Nenhuma anomalia pode ter sido ligada a uma resposta de outra barragem.
    IF EXISTS (
        SELECT 1 FROM anomalies a
        JOIN checklist_responses cr ON cr.id = a.checklist_response_id
        WHERE a.dam_id <> cr.dam_id
    ) THEN
        RAISE EXCEPTION 'V5 ABORTADA: vínculo cruzou barragens diferentes. Nenhuma alteração foi aplicada.';
    END IF;
END $$;
