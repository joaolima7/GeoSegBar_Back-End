# Migrações de banco (Flyway)

Runbook do time de back-end. As migrações rodam sozinhas no boot da aplicação —
este documento existe para você saber o que vai acontecer, como conferir antes e
depois, e o que fazer se algo der errado.

---

## Como está montado

Flyway e `ddl-auto=update` convivem de propósito, com divisão clara de trabalho:

| Quem | Faz o quê |
| --- | --- |
| **Hibernate** (`ddl-auto=update`) | Cria tabelas e colunas novas a partir das entidades, como sempre fez. É o que mantém o banco em dia sem exigir uma migração para cada campo novo. |
| **Flyway** | Faz o que o Hibernate não sabe fazer: remover constraint antiga, migrar dados, reorganizar linhas existentes. |

Não trocamos para `ddl-auto=validate` agora porque o schema de produção foi
construído por `update` ao longo de meses e qualquer divergência mínima entre
entidade e banco derrubaria a aplicação no boot. A migração para `validate` é um
passo separado, feito depois de comparar entidade × banco com calma.

### Ordem no boot

```
1. Flyway aplica as migrações pendentes
2. Hibernate roda o ddl-auto=update
3. A aplicação sobe
```

O Flyway rodar **antes** do Hibernate tem uma consequência que explica o formato
das migrações: em um banco novo e vazio, as tabelas ainda não existem quando as
migrações rodam. Por isso **toda migração começa checando se a tabela existe** e
não faz nada quando não existe — nesse caso o schema nasce correto direto das
entidades.

### Configuração

```properties
spring.flyway.enabled=${FLYWAY_ENABLED:true}
spring.flyway.locations=classpath:db/migration
spring.flyway.baseline-on-migrate=true
spring.flyway.baseline-version=0
spring.flyway.validate-on-migrate=true
spring.flyway.clean-disabled=true
```

`baseline-on-migrate` + `baseline-version=0`: o banco de produção já tem schema e
não tem histórico do Flyway. Sem isso o Flyway se recusaria a rodar. Com
`version=0`, a V1 em diante é aplicada normalmente.

`FLYWAY_ENABLED=false` desliga tudo, caso precise subir a aplicação sem aplicar
migração nenhuma.

Nos testes o Flyway fica desligado (`application-test.properties`): o
Testcontainers sobe um Postgres vazio e o schema vem do `create-drop`.

---

## Migrações existentes

### V1 — `password_setup_token`

Cria a tabela do token de uso único do primeiro acesso por link. Só cria se
`users` já existir e se a tabela ainda não existir.

### V2 — `instrument.created_at`

Adiciona a coluna que separa "as variáveis nunca foram alteradas desde o
cadastro" de "foram alteradas depois".

A coluna fica **nula nos instrumentos existentes**, e isso é intencional: o
código trata `NULL` como "não sei quando foi criado" e cai na comparação por
minuto, que já resolve o falso positivo. Preencher com um valor inventado
afirmaria que nenhum desses instrumentos jamais teve variável alterada,
desligando a proteção para todos eles.

Coluna nullable sem default: no Postgres é alteração de catálogo, instantânea,
sem reescrever a tabela.

### V3 — tipo de instrumento por cliente

A delicada. Detalhada na seção seguinte.

---

## V3 em detalhe

### O problema

`instrument_type` era um catálogo global com `UNIQUE(name)`. Todos os clientes
compartilhavam as mesmas linhas — renomear "PIEZÔMETRO" mudava o nome nas
barragens de todos os clientes ao mesmo tempo. E os instrumentos já existentes
apontam para essas linhas globais.

### A solução

O catálogo global é **replicado para todos os clientes** — cada cliente continua
enxergando exatamente os mesmos tipos que enxergava antes — e cada instrumento é
reapontado para a linha do **seu** cliente com o **mesmo nome** de tipo que já
usava.

Nenhum instrumento muda de tipo. Ele passa a apontar para outra linha, com o
mesmo nome, dentro do catálogo do cliente dono da barragem. Na tela nada muda; o
que muda é que a partir daí uma edição feita por um cliente não alcança os outros.

```
"PIEZÔMETRO" (id 5) usado por instrumentos dos clientes A, B e C

antes    instrumentos de A, B e C  ->  id 5

depois   instrumentos de A  ->  id 5   ("PIEZÔMETRO" agora do cliente A)
         instrumentos de B  ->  id 61  ("PIEZÔMETRO" do cliente B)
         instrumentos de C  ->  id 62  ("PIEZÔMETRO" do cliente C)
```

### Passo a passo

1. Cria `client_id` (nullable) e a FK para `client`.
2. Remove a `UNIQUE(name)` global — é ela que impede dois clientes de terem um
   tipo com o mesmo nome.
3. Tira uma fotografia: qual **nome** de tipo cada instrumento usa hoje.
4. Normaliza nomes para MAIÚSCULAS e funde duplicatas que só diferem por caixa
   (`"Piezômetro"` × `"PIEZÔMETRO"`), reapontando os instrumentos antes de
   descartar a linha repetida.
5. Replica o catálogo para todos os clientes.
6. Reaponta cada instrumento para o tipo do próprio cliente.
7. Cria `UNIQUE(client_id, name)` e os índices.
8. **Verifica e aborta se algo estiver errado.**

### As verificações do passo 8

Qualquer uma que falhe levanta exceção, e o Postgres desfaz a migração inteira —
o Flyway roda cada migração em uma transação e o Postgres suporta DDL
transacional. Não existe estado pela metade.

| # | Verificação | Se falhar |
| --- | --- | --- |
| 8.1 | Nenhum instrumento mudou o **nome** do seu tipo | aborta |
| 8.2 | Nenhum instrumento ficou sem tipo | aborta |
| 8.3 | A quantidade de instrumentos não mudou | aborta |
| 8.4 | Nenhum instrumento usa tipo de outro cliente | aborta |
| 8.5 | Sobraram tipos sem dono | avisa (não aborta) |

### Casos de borda tratados

| Caso | Comportamento |
| --- | --- |
| Tipo usado por vários clientes | Replicado, um por cliente, instrumentos reapontados |
| Tipo sem nenhum instrumento | Replicado mesmo assim — o cliente pode excluir depois pela tela |
| `"Piezômetro"` e `"PIEZÔMETRO"` como linhas separadas | Fundidos em um, instrumentos reapontados |
| Nome com espaço sobrando | Normalizado com `TRIM` |
| Cliente sem nenhuma barragem | Recebe o catálogo completo do mesmo jeito |
| **Barragem sem cliente** (`dam.client_id IS NULL`) | Instrumentos ficam onde estão e a migração **avisa** no log. Não há cliente para escolher. O código não bloqueia esse caso |
| Banco sem nenhum cliente | Migração para no passo 5 e avisa; catálogo fica sem dono |
| Migração rodada duas vezes | Idempotente — segunda execução não altera nada |

### O que fica "legacy"

Um tipo com `client_id` nulo aparece na API com `legacy: true` e fica
somente-leitura (não pode ser editado nem excluído), justamente para não
replicar alteração entre clientes. Depois da V3 isso só acontece se o banco não
tiver nenhum cliente cadastrado.

---

## O que acontece no deploy

`./bash/cli_app.sh` → opção 1 → `deploy_vps.sh`. A sequência relevante:

```
1. Sobe/confere PostgreSQL e Redis
2. Backup do banco  (pg_dump, aborta o deploy se falhar)
3. Para o container da API
4. git pull origin main
5. docker build   (mvn clean package -DskipTests — as migrações entram no jar)
6. docker run da API
     └── Flyway aplica as migrações pendentes
     └── Hibernate roda o ddl-auto=update
     └── "Started GeosegbarApplication"
7. Health check aguarda até 180s por essa linha no log
```

Não é preciso rodar nada à mão: **subiu o deploy, as migrações rodam**.

O backup no passo 2 acontece **antes** de a API ser derrubada, para que uma falha
de backup não gere indisponibilidade. Para pular:
`SKIP_PRE_DEPLOY_BACKUP=true`.

Como a API é parada antes de subir a nova, existe **uma instância só** durante a
migração — não há risco de duas rodarem em paralelo.

### Se a migração falhar no deploy

O deploy detecta, destaca as linhas do Flyway no log e falha com código 1. O
banco fica intacto (a transação é desfeita).

Um detalhe do container: ele sobe com `--restart unless-stopped`. Se a aplicação
não conseguir subir por causa da migração, o Docker fica reiniciando em loop.
Pare antes de investigar:

```bash
docker stop geosegbar-api-prod
docker logs geosegbar-api-prod 2>&1 | grep -iE "flyway|migration"
```

Para colocar a API no ar sem aplicar migração, enquanto investiga:
`FLYWAY_ENABLED=false` no `.env.prod` e rode o deploy de novo.

---

## Antes do deploy — diagnóstico (somente leitura)

Rode no banco de produção para saber o que a V3 vai encontrar. Nenhuma dessas
consultas escreve.

```sql
-- 1. Dimensão do trabalho
SELECT
    (SELECT COUNT(*) FROM client)          AS clientes,
    (SELECT COUNT(*) FROM instrument_type) AS tipos_hoje,
    (SELECT COUNT(*) FROM instrument)      AS instrumentos,
    (SELECT COUNT(*) FROM client) * (SELECT COUNT(DISTINCT UPPER(TRIM(name))) FROM instrument_type)
                                           AS tipos_depois_estimado;
```

```sql
-- 2. Quais tipos são compartilhados entre clientes (o caso que gera cópias)
SELECT it.id, it.name,
       COUNT(DISTINCT d.client_id) AS clientes_distintos,
       COUNT(i.id)                 AS instrumentos,
       STRING_AGG(DISTINCT c.name, ', ') AS clientes
FROM instrument_type it
LEFT JOIN instrument i ON i.instrument_type_id = it.id
LEFT JOIN dam d        ON d.id = i.dam_id
LEFT JOIN client c     ON c.id = d.client_id
GROUP BY it.id, it.name
ORDER BY clientes_distintos DESC, it.name;
```

```sql
-- 3. Duplicatas que só diferem por caixa ou espaço (serão fundidas)
SELECT UPPER(TRIM(name)) AS nome_normalizado,
       COUNT(*) AS linhas,
       ARRAY_AGG(id) AS ids,
       ARRAY_AGG(name) AS nomes_originais
FROM instrument_type
GROUP BY UPPER(TRIM(name))
HAVING COUNT(*) > 1;
```

```sql
-- 4. Barragens sem cliente — instrumentos que não têm para onde ser reapontados
SELECT d.id AS barragem_id, d.name AS barragem, COUNT(i.id) AS instrumentos
FROM dam d LEFT JOIN instrument i ON i.dam_id = d.id
WHERE d.client_id IS NULL
GROUP BY d.id, d.name;
```

Se a consulta 4 retornar linhas, decida antes do deploy: atribuir um cliente a
essas barragens é melhor do que deixar os instrumentos apontando para o catálogo
de um cliente arbitrário.

---

## Depois do deploy — verificação

O log da aplicação traz o resumo da V3 (linhas `NOTICE`, com quantos
instrumentos foram reapontados e quantos tipos existem agora). Para conferir no
banco:

```sql
-- Deve retornar 0 linhas: ninguém usando tipo de outro cliente
SELECT i.id, i.name, d.name AS barragem, d.client_id AS cliente_barragem,
       it.name AS tipo, it.client_id AS cliente_tipo
FROM instrument i
JOIN dam d              ON d.id = i.dam_id
JOIN instrument_type it ON it.id = i.instrument_type_id
WHERE d.client_id IS NOT NULL AND it.client_id IS NOT NULL
  AND it.client_id <> d.client_id;
```

```sql
-- Catálogo final por cliente
SELECT c.name AS cliente, it.name AS tipo, COUNT(i.id) AS instrumentos
FROM instrument_type it
LEFT JOIN client c     ON c.id = it.client_id
LEFT JOIN instrument i ON i.instrument_type_id = it.id
GROUP BY c.name, it.name
ORDER BY c.name NULLS FIRST, it.name;
```

```sql
-- Histórico do Flyway
SELECT installed_rank, version, description, success, installed_on
FROM flyway_schema_history ORDER BY installed_rank;
```

---

## Se der errado

**Migração falhou no meio.** Não existe meio: a transação foi desfeita e o banco
está exatamente como antes. O Flyway marca a migração como falha em
`flyway_schema_history` e a aplicação não sobe. Corrija o SQL, remova a linha
com `success = false` do histórico e suba de novo.

**Aplicação não sobe por causa do Flyway.** Suba com `FLYWAY_ENABLED=false` para
recuperar o serviço enquanto investiga. O código funciona com ou sem a V3
aplicada — sem ela, os tipos aparecem como `legacy` e ficam somente-leitura.

**Precisa reverter a V3 depois de aplicada.** Não há migração de volta escrita,
porque desfazer significaria decidir qual das cópias por cliente sobrevive.
Restaure o backup. Faça o backup antes do deploy.

---

## Regras daqui pra frente

1. **Nunca edite uma migração já aplicada.** O Flyway guarda um checksum; alterar
   o arquivo faz a aplicação se recusar a subir. Crie uma V4, V5…
2. **Numeração sequencial**, sem buracos: `V4__descricao_curta.sql`.
3. **Mudança só de schema aditiva** (coluna nova, tabela nova) não precisa de
   migração — o `ddl-auto=update` resolve. Migração é para o que ele não faz:
   remover constraint, migrar dado, reorganizar linha.
4. **Toda migração começa checando se a tabela existe**, para não quebrar em
   banco novo.
5. **Migração que mexe em dado termina verificando**, e levanta exceção se a
   verificação falhar. Melhor não aplicar do que aplicar errado.
