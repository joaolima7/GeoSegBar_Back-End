-- ============================================================================
-- V6 — created_at da resposta de checklist e da anomalia deixa de ser imutável
-- ============================================================================
--
-- POR QUE
--   A engenharia da Geometrisa preenche checklist no papel em campo e lança no
--   sistema semanas depois. Até aqui a data era sempre a do lançamento, então
--   uma inspeção de abril aparecia como outubro — na lista, no filtro por
--   período, no gráfico mensal, no "último checklist" da barragem e na cadeia
--   de regras de monitoramento, que decide quais rótulos a próxima inspeção
--   pode usar.
--
--   A escolha foi tornar created_at gravável, em vez de criar um campo separado:
--   assim as ~15 consultas que já usam created_at passam a enxergar abril como
--   abril sem precisar ser reescritas uma a uma, e sem risco de o painel
--   divergir da lista.
--
-- O QUE ESTA MIGRAÇÃO FAZ
--   Remove o NOT NULL de anomalies.created_at. O Hibernate com ddl-auto=update
--   não desfaz NOT NULL de coluna existente, e a entidade passou a declarar a
--   coluna como opcional: a data é atribuída no serviço (herdada da inspeção),
--   não mais por @CreationTimestamp.
--
--   Nenhuma linha é lida ou alterada. checklist_responses.created_at já era
--   nullable no banco, então não precisa de ALTER.
-- ============================================================================

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'anomalies'
          AND column_name = 'created_at'
          AND is_nullable = 'NO'
    ) THEN
        ALTER TABLE anomalies ALTER COLUMN created_at DROP NOT NULL;
        RAISE NOTICE 'V6: NOT NULL removido de anomalies.created_at.';
    ELSE
        RAISE NOTICE 'V6: anomalies.created_at já era nullable — nada a fazer.';
    END IF;
END $$;
