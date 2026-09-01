-- ============================================================================
-- V4 — preenche instrument.created_at nos instrumentos anteriores à coluna
-- ============================================================================
-- A V2 criou a coluna e deixou os instrumentos existentes com NULL, apostando
-- que a comparação por minuto no código resolveria o falso positivo sozinha.
-- Não resolve, e a produção mostrou o tamanho do estrago:
--
--     271.528 de 272.868 leituras (99,5%), em 845 dos 849 instrumentos com
--     leitura, estavam BLOQUEADAS para edição de valores.
--
-- Por quê: lastUpdateVariablesDate nasce igual ao momento do cadastro. Um
-- instrumento cadastrado em 2026 que recebeu leituras históricas de 1901 em
-- diante tem, para quase toda leitura, "variáveis alteradas depois do
-- registro" — quando na verdade nada foi alterado, o instrumento é que é mais
-- novo que as medições que carrega.
--
-- A proteção que existe para esse caso é justamente a âncora created_at
-- ("lastUpdate == createdAt significa que nada foi mexido"), e ela estava
-- desligada para os 1.034 instrumentos, todos com created_at NULL.
--
-- O que este backfill afirma: "este instrumento nunca teve variável alterada
-- desde o cadastro". Para a esmagadora maioria isso é verdade. Onde não for, o
-- efeito é que uma leitura antiga volta a poder ser editada e será recalculada
-- com a configuração vigente hoje — que é exatamente o comportamento que o
-- gestor está pedindo como funcionalidade. Não há como distinguir os dois casos
-- retroativamente: não existe histórico de constantes nem de equações, e a
-- leitura não guarda quando foi registrada, só a data medida.
--
-- Correção estrutural (fora do escopo desta migration): dar um created_at à
-- própria leitura e comparar contra ele, em vez de contra a data da medição; e
-- versionar a configuração do instrumento, que remove a heurística de vez.
-- ============================================================================

DO $$
DECLARE
    afetados INTEGER;
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'instrument' AND column_name = 'created_at'
    ) THEN
        RAISE NOTICE 'V4: coluna "instrument.created_at" não existe (banco novo) — nada a fazer.';
        RETURN;
    END IF;

    UPDATE instrument
       SET created_at = last_update_variables_date
     WHERE created_at IS NULL
       AND last_update_variables_date IS NOT NULL;

    GET DIAGNOSTICS afetados = ROW_COUNT;

    RAISE NOTICE 'V4: created_at preenchido em % instrumento(s).', afetados;
END $$;
