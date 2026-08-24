-- ============================================================================
-- V3 — Tipo de instrumento passa a pertencer a um cliente
-- ============================================================================
--
-- SITUAÇÃO ANTERIOR
--   instrument_type era um catálogo global, com UNIQUE(name). Todos os clientes
--   compartilhavam as mesmas linhas: renomear "PIEZÔMETRO" mudava o nome nas
--   barragens de todos os clientes ao mesmo tempo.
--
-- SITUAÇÃO NOVA
--   Cada cliente tem o seu catálogo, e a unicidade passa a ser (client_id, name).
--
-- COMO A MIGRAÇÃO RESOLVE O PROBLEMA DOS INSTRUMENTOS JÁ ASSOCIADOS
--   O catálogo global é replicado para todos os clientes — cada cliente continua
--   enxergando exatamente os mesmos tipos que enxergava antes — e então cada
--   instrumento é reapontado para a linha do SEU cliente com o MESMO NOME de
--   tipo que ele já usava.
--
--   Ou seja: nenhum instrumento muda de tipo. Ele passa a apontar para outra
--   linha, com o mesmo nome, dentro do catálogo do cliente dono da barragem.
--   Para o usuário nada muda na tela; o que muda é que a partir daí uma edição
--   feita por um cliente não alcança mais os outros.
--
--   Exemplo — "PIEZÔMETRO" (id 5) usado por instrumentos dos clientes A, B e C:
--     antes   instrumentos de A, B e C  ->  id 5
--     depois  instrumentos de A -> id 5 (agora do cliente A)
--             instrumentos de B -> id 61 ("PIEZÔMETRO" do cliente B)
--             instrumentos de C -> id 62 ("PIEZÔMETRO" do cliente C)
--
-- SEGURANÇA
--   Nada é excluído, exceto linhas duplicadas que diferem só por caixa
--   ("Piezômetro" vs "PIEZÔMETRO") e cujos instrumentos são reapontados antes.
--   No fim, o passo 7 confere instrumento por instrumento que o NOME do tipo
--   continua idêntico ao de antes. Qualquer divergência levanta exceção e o
--   Postgres desfaz a migração inteira — o Flyway roda cada migração em uma
--   transação e o Postgres suporta DDL transacional.
--
--   Em banco novo, onde o Flyway roda antes do Hibernate criar as tabelas, a
--   migração não faz nada: o schema nasce correto a partir das entidades.
-- ============================================================================

DO $$
DECLARE
    cliente_base       BIGINT;
    qtd_clientes       BIGINT;
    qtd_tipos_antes    BIGINT;
    qtd_tipos_depois   BIGINT;
    qtd_instrumentos   BIGINT;
    qtd_repontados     BIGINT;
    qtd_divergentes    BIGINT;
    qtd_sem_dono       BIGINT;
    qtd_barragem_sem_cliente BIGINT;
    dup                RECORD;
BEGIN
    -- ------------------------------------------------------------------
    -- Passo 0 — pré-condições
    -- ------------------------------------------------------------------
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = current_schema() AND table_name = 'instrument_type'
    ) THEN
        RAISE NOTICE 'V3: tabela "instrument_type" ainda não existe (banco novo) — nada a fazer.';
        RETURN;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = current_schema() AND table_name = 'client'
    ) THEN
        RAISE NOTICE 'V3: tabela "client" ainda não existe (banco novo) — nada a fazer.';
        RETURN;
    END IF;

    SELECT COUNT(*) INTO qtd_clientes     FROM client;
    SELECT COUNT(*) INTO qtd_tipos_antes  FROM instrument_type;
    SELECT COUNT(*) INTO qtd_instrumentos FROM instrument;

    RAISE NOTICE 'V3: início — % cliente(s), % tipo(s) no catálogo global, % instrumento(s).',
        qtd_clientes, qtd_tipos_antes, qtd_instrumentos;

    -- ------------------------------------------------------------------
    -- Passo 1 — coluna client_id e chave estrangeira
    -- ------------------------------------------------------------------
    -- Nullable de propósito: o Hibernate (ddl-auto) pode já ter criado a coluna
    -- em um deploy anterior, e uma coluna NOT NULL não pode ser adicionada a uma
    -- tabela que já tem linhas. O preenchimento acontece no passo 4.
    ALTER TABLE instrument_type ADD COLUMN IF NOT EXISTS client_id BIGINT;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_instrument_type_client') THEN
        ALTER TABLE instrument_type
            ADD CONSTRAINT fk_instrument_type_client
            FOREIGN KEY (client_id) REFERENCES client (id);
        RAISE NOTICE 'V3: FK fk_instrument_type_client criada.';
    END IF;

    -- ------------------------------------------------------------------
    -- Passo 2 — remove a UNIQUE global em (name)
    -- ------------------------------------------------------------------
    -- É ela que impede dois clientes de terem um tipo com o mesmo nome. Enquanto
    -- existir, a separação por cliente simplesmente não funciona.
    IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_instrument_type_name') THEN
        ALTER TABLE instrument_type DROP CONSTRAINT uk_instrument_type_name;
        RAISE NOTICE 'V3: constraint uk_instrument_type_name removida.';
    END IF;

    DROP INDEX IF EXISTS uk_instrument_type_name;

    -- ------------------------------------------------------------------
    -- Passo 3 — fotografia de segurança: qual NOME de tipo cada instrumento usa
    -- ------------------------------------------------------------------
    -- Comparada com o estado final no passo 7. É o que garante que nenhum
    -- instrumento saiu de "PIEZÔMETRO" para outra coisa.
    DROP TABLE IF EXISTS _v3_tipo_antes;
    CREATE TEMP TABLE _v3_tipo_antes ON COMMIT DROP AS
        SELECT i.id AS instrument_id, UPPER(TRIM(it.name)) AS type_name
        FROM instrument i
        JOIN instrument_type it ON it.id = i.instrument_type_id;

    -- ------------------------------------------------------------------
    -- Passo 4 — normaliza nomes e funde duplicatas que só diferem por caixa
    -- ------------------------------------------------------------------
    -- O serviço grava tudo em MAIÚSCULAS e a unicidade nova é exata. Se o
    -- catálogo tiver "Piezômetro" e "PIEZÔMETRO" como linhas separadas, elas
    -- passam a colidir — então precisam virar uma linha só, com os instrumentos
    -- da linha descartada reapontados antes da exclusão.
    FOR dup IN
        SELECT COALESCE(client_id, -1) AS cli,
               UPPER(TRIM(name))       AS nome,
               MIN(id)                 AS manter,
               ARRAY_AGG(id)           AS todos
        FROM instrument_type
        GROUP BY COALESCE(client_id, -1), UPPER(TRIM(name))
        HAVING COUNT(*) > 1
    LOOP
        UPDATE instrument
        SET instrument_type_id = dup.manter
        WHERE instrument_type_id = ANY(dup.todos)
          AND instrument_type_id <> dup.manter;

        DELETE FROM instrument_type
        WHERE id = ANY(dup.todos) AND id <> dup.manter;

        RAISE NOTICE 'V3: tipos duplicados por caixa para "%" fundidos na linha % (linhas descartadas: %).',
            dup.nome, dup.manter, dup.todos;
    END LOOP;

    UPDATE instrument_type
    SET name = UPPER(TRIM(name))
    WHERE name <> UPPER(TRIM(name));

    -- ------------------------------------------------------------------
    -- Passo 5 — replica o catálogo para todos os clientes
    -- ------------------------------------------------------------------
    IF qtd_clientes = 0 THEN
        RAISE NOTICE 'V3: não há clientes cadastrados — catálogo permanece sem dono e a migração para aqui.';
        RETURN;
    END IF;

    -- 5a: as linhas originais ganham um dono. Escolha determinística (menor id de
    --     cliente) só para ter um ponto de partida; os demais clientes recebem
    --     cópias logo abaixo, então nenhum cliente sai em desvantagem.
    SELECT MIN(id) INTO cliente_base FROM client;

    UPDATE instrument_type SET client_id = cliente_base WHERE client_id IS NULL;

    -- 5b: cada cliente passa a ter o catálogo completo — exatamente os tipos que
    --     ele já enxergava quando o catálogo era global.
    INSERT INTO instrument_type (name, client_id)
    SELECT nomes.name, c.id
    FROM (SELECT DISTINCT name FROM instrument_type) AS nomes
    CROSS JOIN client c
    WHERE NOT EXISTS (
        SELECT 1 FROM instrument_type existente
        WHERE existente.client_id = c.id
          AND existente.name = nomes.name
    );

    SELECT COUNT(*) INTO qtd_tipos_depois FROM instrument_type;
    RAISE NOTICE 'V3: catálogo replicado — de % para % linha(s) em instrument_type.',
        qtd_tipos_antes, qtd_tipos_depois;

    -- ------------------------------------------------------------------
    -- Passo 6 — reaponta cada instrumento para o tipo do seu próprio cliente
    -- ------------------------------------------------------------------
    -- Mesmo nome, linha do cliente dono da barragem.
    WITH reapontados AS (
        UPDATE instrument i
        SET instrument_type_id = destino.id
        FROM dam d, instrument_type atual, instrument_type destino
        WHERE d.id = i.dam_id
          AND atual.id = i.instrument_type_id
          AND d.client_id IS NOT NULL
          AND destino.client_id = d.client_id
          AND destino.name = atual.name
          AND destino.id <> atual.id
        RETURNING i.id
    )
    SELECT COUNT(*) INTO qtd_repontados FROM reapontados;

    RAISE NOTICE 'V3: % instrumento(s) reapontados para o catálogo do próprio cliente.', qtd_repontados;

    -- Barragens sem cliente não têm para onde apontar. Ficam como estão e o
    -- código não bloqueia esse caso — mas vale saber que existem.
    SELECT COUNT(*) INTO qtd_barragem_sem_cliente
    FROM instrument i JOIN dam d ON d.id = i.dam_id
    WHERE d.client_id IS NULL;

    IF qtd_barragem_sem_cliente > 0 THEN
        RAISE NOTICE 'V3: ATENÇÃO — % instrumento(s) estão em barragem sem cliente e não foram reapontados.',
            qtd_barragem_sem_cliente;
    END IF;

    -- ------------------------------------------------------------------
    -- Passo 7 — unicidade por cliente e índices
    -- ------------------------------------------------------------------
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_instrument_type_client_name') THEN
        ALTER TABLE instrument_type
            ADD CONSTRAINT uk_instrument_type_client_name UNIQUE (client_id, name);
        RAISE NOTICE 'V3: constraint uk_instrument_type_client_name criada.';
    END IF;

    CREATE INDEX IF NOT EXISTS idx_instrument_type_client_id   ON instrument_type (client_id);
    CREATE INDEX IF NOT EXISTS idx_instrument_type_client_name ON instrument_type (client_id, name);
    CREATE INDEX IF NOT EXISTS idx_instrument_type_name_search ON instrument_type (name);

    -- ------------------------------------------------------------------
    -- Passo 8 — verificações. Qualquer falha desfaz a migração inteira.
    -- ------------------------------------------------------------------

    -- 8.1 Nenhum instrumento pode ter mudado o NOME do seu tipo.
    SELECT COUNT(*) INTO qtd_divergentes
    FROM _v3_tipo_antes antes
    JOIN instrument i        ON i.id = antes.instrument_id
    JOIN instrument_type it  ON it.id = i.instrument_type_id
    WHERE it.name IS DISTINCT FROM antes.type_name;

    IF qtd_divergentes > 0 THEN
        RAISE EXCEPTION 'V3 ABORTADA: % instrumento(s) ficariam com um tipo de nome diferente do original. Nenhuma alteração foi aplicada.',
            qtd_divergentes;
    END IF;

    -- 8.2 Nenhum instrumento pode ter ficado sem tipo.
    IF EXISTS (SELECT 1 FROM instrument WHERE instrument_type_id IS NULL) THEN
        RAISE EXCEPTION 'V3 ABORTADA: há instrumento sem tipo após a migração. Nenhuma alteração foi aplicada.';
    END IF;

    -- 8.3 A contagem de instrumentos não pode ter mudado.
    IF (SELECT COUNT(*) FROM instrument) <> qtd_instrumentos THEN
        RAISE EXCEPTION 'V3 ABORTADA: a quantidade de instrumentos mudou (% -> %). Nenhuma alteração foi aplicada.',
            qtd_instrumentos, (SELECT COUNT(*) FROM instrument);
    END IF;

    -- 8.4 Nenhum instrumento pode usar tipo de outro cliente — a razão de ser da migração.
    SELECT COUNT(*) INTO qtd_divergentes
    FROM instrument i
    JOIN dam d              ON d.id = i.dam_id
    JOIN instrument_type it ON it.id = i.instrument_type_id
    WHERE d.client_id IS NOT NULL
      AND it.client_id IS NOT NULL
      AND it.client_id <> d.client_id;

    IF qtd_divergentes > 0 THEN
        RAISE EXCEPTION 'V3 ABORTADA: % instrumento(s) continuariam usando tipo de outro cliente. Nenhuma alteração foi aplicada.',
            qtd_divergentes;
    END IF;

    -- 8.5 Tipos sem dono não deveriam sobrar. Não é motivo para abortar, mas precisa aparecer.
    SELECT COUNT(*) INTO qtd_sem_dono FROM instrument_type WHERE client_id IS NULL;
    IF qtd_sem_dono > 0 THEN
        RAISE NOTICE 'V3: ATENÇÃO — % tipo(s) permaneceram sem cliente e ficarão somente-leitura no sistema.',
            qtd_sem_dono;
    END IF;

    RAISE NOTICE 'V3: concluída com sucesso. % instrumento(s) verificados, todos com o mesmo nome de tipo de antes.',
        qtd_instrumentos;
END $$;
