-- ============================================================================
-- V2 — instrument.created_at
-- ============================================================================
-- Âncora que separa "as variáveis nunca foram alteradas desde o cadastro" de
-- "as variáveis foram alteradas depois". Sem ela, o cadastro do instrumento era
-- confundido com uma alteração de variáveis e a primeira leitura do instrumento
-- não podia ser editada.
--
-- A coluna fica NULA nos instrumentos já existentes, e isso é intencional: o
-- código trata NULL como "não sei quando foi criado" e cai na comparação por
-- minuto, que já resolve o falso positivo. Preencher created_at com um valor
-- inventado seria pior — afirmaria que nenhum desses instrumentos jamais teve
-- variável alterada, desligando a proteção para todos eles.
-- ============================================================================

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = current_schema() AND table_name = 'instrument'
    ) THEN
        RAISE NOTICE 'V2: tabela "instrument" ainda não existe (banco novo) — nada a fazer.';
        RETURN;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'instrument' AND column_name = 'created_at'
    ) THEN
        RAISE NOTICE 'V2: coluna "instrument.created_at" já existe — nada a fazer.';
        RETURN;
    END IF;

    -- Coluna nullable e sem default: no Postgres é alteração de catálogo, instantânea,
    -- sem reescrever a tabela e sem lock demorado.
    ALTER TABLE instrument ADD COLUMN created_at TIMESTAMP(6);

    RAISE NOTICE 'V2: coluna "instrument.created_at" criada.';
END $$;
