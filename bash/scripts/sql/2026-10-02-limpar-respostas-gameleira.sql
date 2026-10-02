-- ============================================================================
-- Limpeza de respostas de checklist e anomalias — PCH Gameleira (dam_id = 5)
-- Cliente: Tradener Ltda. (client_id = 3)
-- Pedido por Pedro Sismoto em 02/10/2026: a equipe vai relançar os checklists
-- usando a data retroativa, e para isso precisa do histórico limpo.
-- ============================================================================
--
-- O QUE APAGA
--   As RESPOSTAS dos checklists e as anomalias que nasceram delas.
--
-- O QUE NÃO APAGA
--   O checklist em si, os questionários do modelo, as perguntas, as opções e os
--   instrumentos. Confirmado com o Pedro: "Exato, instrumentos se mantêm".
--
-- COMO RODAR
--   Este script roda em transação e termina em ROLLBACK. Rode assim primeiro:
--   nada é alterado e os números de conferência aparecem na tela.
--
--     docker exec -i postgres-prod psql -U <usuario> -d <banco> \
--       -v ON_ERROR_STOP=1 -f /caminho/2026-10-02-limpar-respostas-gameleira.sql
--
--   Conferidos os números, troque a ÚLTIMA linha de ROLLBACK para COMMIT e rode
--   de novo. Só então a alteração é gravada.
--
-- ANTES DE COMMITAR: tire um backup.
--   bash/scripts/backup_database_prod.sh
--
-- NÃO COBERTO POR ESTE SCRIPT
--   As imagens no S3 (48 fotos de resposta + 28 de anomalia) ficam órfãs no
--   bucket. Não são apagadas aqui de propósito: apagar objeto do S3 não tem
--   rollback. Se quiser limpar, é um passo manual depois, com a lista de
--   caminhos que o bloco de conferência imprime.
-- ============================================================================

\set ON_ERROR_STOP on
\timing on

BEGIN;

-- Trava a barragem alvo numa variável, para o id não se perder no meio do script.
CREATE TEMP TABLE alvo ON COMMIT DROP AS SELECT 5::bigint AS dam_id;

-- ---------------------------------------------------------------------------
-- 0. Confere que é a barragem certa. Se não for, nada abaixo faz sentido.
-- ---------------------------------------------------------------------------
SELECT d.id, d.name AS barragem, c.name AS cliente
FROM dam d JOIN client c ON c.id = d.client_id
WHERE d.id = (SELECT dam_id FROM alvo);

DO $$
DECLARE nome TEXT;
BEGIN
    SELECT d.name INTO nome FROM dam d WHERE d.id = (SELECT dam_id FROM alvo);
    IF nome IS DISTINCT FROM 'PCH Gameleira' THEN
        RAISE EXCEPTION 'ABORTADO: dam_id % não é a PCH Gameleira (é "%")',
            (SELECT dam_id FROM alvo), nome;
    END IF;
END $$;

-- ---------------------------------------------------------------------------
-- 1. Antes
-- ---------------------------------------------------------------------------
SELECT 'ANTES' AS momento,
 (SELECT count(*) FROM checklist_responses WHERE dam_id = (SELECT dam_id FROM alvo))                 AS respostas_checklist,
 (SELECT count(*) FROM questionnaire_responses WHERE checklist_response_id IN
    (SELECT id FROM checklist_responses WHERE dam_id = (SELECT dam_id FROM alvo)))                   AS questionarios,
 (SELECT count(*) FROM answers a JOIN questionnaire_responses qr ON qr.id = a.questionnaire_response_id
   WHERE qr.checklist_response_id IN (SELECT id FROM checklist_responses WHERE dam_id = (SELECT dam_id FROM alvo))) AS respostas,
 (SELECT count(*) FROM anomalies WHERE dam_id = (SELECT dam_id FROM alvo))                           AS anomalias;

-- Caminhos das imagens que ficarão órfãs no S3 — guarde esta saída.
SELECT 'S3 ORFAO (resposta)' AS tipo, ap.image_path
FROM answer_photos ap
JOIN answers a ON a.id = ap.answer_id
JOIN questionnaire_responses qr ON qr.id = a.questionnaire_response_id
WHERE qr.checklist_response_id IN (SELECT id FROM checklist_responses WHERE dam_id = (SELECT dam_id FROM alvo))
UNION ALL
SELECT 'S3 ORFAO (anomalia)', anp.image_path
FROM anomaly_photos anp
WHERE anp.anomaly_id IN (SELECT id FROM anomalies WHERE dam_id = (SELECT dam_id FROM alvo));

-- ---------------------------------------------------------------------------
-- 2. Apaga, de baixo para cima
--    Nenhuma FK é ON DELETE CASCADE neste banco (todas NO ACTION), então cada
--    nível precisa ser apagado explicitamente, na ordem.
-- ---------------------------------------------------------------------------

CREATE TEMP TABLE respostas_alvo ON COMMIT DROP AS
    SELECT id FROM checklist_responses WHERE dam_id = (SELECT dam_id FROM alvo);

CREATE TEMP TABLE questionarios_alvo ON COMMIT DROP AS
    SELECT id FROM questionnaire_responses WHERE checklist_response_id IN (SELECT id FROM respostas_alvo);

CREATE TEMP TABLE answers_alvo ON COMMIT DROP AS
    SELECT id FROM answers WHERE questionnaire_response_id IN (SELECT id FROM questionarios_alvo);

CREATE TEMP TABLE anomalias_alvo ON COMMIT DROP AS
    SELECT id FROM anomalies WHERE dam_id = (SELECT dam_id FROM alvo);

DELETE FROM answer_options WHERE answer_id IN (SELECT id FROM answers_alvo);
DELETE FROM answer_photos  WHERE answer_id IN (SELECT id FROM answers_alvo);
DELETE FROM answers        WHERE id        IN (SELECT id FROM answers_alvo);
DELETE FROM questionnaire_responses WHERE id IN (SELECT id FROM questionarios_alvo);
DELETE FROM checklist_responses     WHERE id IN (SELECT id FROM respostas_alvo);

DELETE FROM anomaly_photos WHERE anomaly_id IN (SELECT id FROM anomalias_alvo);
DELETE FROM anomalies      WHERE id         IN (SELECT id FROM anomalias_alvo);

-- A barragem mostrava "última realização 01/10/2026". Sem respostas, o campo
-- tem que voltar a vazio, senão a tela aponta uma inspeção que não existe mais.
UPDATE documentation_dam
SET last_achievement_checklist = NULL
WHERE dam_id = (SELECT dam_id FROM alvo);

-- ---------------------------------------------------------------------------
-- 3. Depois — as quatro primeiras colunas têm que ser ZERO
-- ---------------------------------------------------------------------------
SELECT 'DEPOIS' AS momento,
 (SELECT count(*) FROM checklist_responses WHERE dam_id = (SELECT dam_id FROM alvo))                 AS respostas_checklist,
 (SELECT count(*) FROM questionnaire_responses WHERE checklist_response_id IN
    (SELECT id FROM checklist_responses WHERE dam_id = (SELECT dam_id FROM alvo)))                   AS questionarios,
 (SELECT count(*) FROM answers a JOIN questionnaire_responses qr ON qr.id = a.questionnaire_response_id
   WHERE qr.checklist_response_id IN (SELECT id FROM checklist_responses WHERE dam_id = (SELECT dam_id FROM alvo))) AS respostas,
 (SELECT count(*) FROM anomalies WHERE dam_id = (SELECT dam_id FROM alvo))                           AS anomalias;

-- ---------------------------------------------------------------------------
-- 4. O que tinha que sobrar, sobrou
-- ---------------------------------------------------------------------------
SELECT 'PRESERVADO' AS o_que,
 (SELECT count(*) FROM checklists WHERE dam_id = (SELECT dam_id FROM alvo))             AS checklists_modelo,
 (SELECT count(*) FROM template_questionnaires WHERE dam_id = (SELECT dam_id FROM alvo)) AS questionarios_modelo,
 (SELECT count(*) FROM instrument WHERE dam_id = (SELECT dam_id FROM alvo))              AS instrumentos;

-- ---------------------------------------------------------------------------
-- 5. Nenhuma outra barragem pode ter sido tocada
-- ---------------------------------------------------------------------------
DO $$
DECLARE
    outras_respostas BIGINT;
    outras_anomalias BIGINT;
BEGIN
    SELECT count(*) INTO outras_respostas FROM checklist_responses WHERE dam_id <> (SELECT dam_id FROM alvo);
    SELECT count(*) INTO outras_anomalias FROM anomalies           WHERE dam_id <> (SELECT dam_id FROM alvo);
    RAISE NOTICE 'Outras barragens seguem com % resposta(s) e % anomalia(s) — compare com o valor de antes.',
        outras_respostas, outras_anomalias;

    IF EXISTS (SELECT 1 FROM checklist_responses WHERE dam_id = (SELECT dam_id FROM alvo))
       OR EXISTS (SELECT 1 FROM anomalies WHERE dam_id = (SELECT dam_id FROM alvo)) THEN
        RAISE EXCEPTION 'ABORTADO: sobrou resposta ou anomalia na barragem alvo.';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM checklists WHERE dam_id = (SELECT dam_id FROM alvo)) THEN
        RAISE EXCEPTION 'ABORTADO: o checklist do modelo sumiu. Ele deveria ser preservado.';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM instrument WHERE dam_id = (SELECT dam_id FROM alvo)) THEN
        RAISE EXCEPTION 'ABORTADO: os instrumentos sumiram. Eles deveriam ser preservados.';
    END IF;
END $$;

-- ============================================================================
-- TROQUE PARA COMMIT QUANDO OS NÚMEROS ACIMA ESTIVEREM CONFERIDOS
-- ============================================================================
ROLLBACK;
