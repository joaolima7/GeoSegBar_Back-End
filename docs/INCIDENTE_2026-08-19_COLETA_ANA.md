# Incidente 19/08/2026 — coleta histórica da ANA parou em 2019

Job de backfill do instrumento 1377 (EST TELEMETRICA, estação 21780250) parou de
registrar leituras e ficou 4 dias girando sem sair do lugar.

**Período afetado:** 19/08/2026 15:23 até 23/08/2026 (descoberta)
**Dados coletados:** 2015-02-24 a 2019-01-18 — faltou 2019-01-18 até 2026-08

---

## O que o usuário viu

Criou o instrumento com código ANA. A leitura instantânea do dia funcionou, o
backfill começou de 2015 e avançou normalmente até 2019 — e então parou. Nada
mais foi registrado, sem erro visível na interface.

---

## A cadeia de falhas

Cinco defeitos encadeados. Nenhum deles sozinho teria causado isso.

### 1. A ANA respondeu HTML em vez de JSON

```
15:23:12 ERROR Erro de API no job 1: Erro ao obter dados históricos: <html>
```

A API devolveu uma página HTML de erro. O cliente colocou **o corpo inteiro** na
mensagem da exceção.

### 2. Registrar o erro virou, ele próprio, um erro

```
ERROR: value too long for type character varying(2000)
```

`historical_data_job.error_message` é `varchar(2000)`. O HTML estourou o limite e
o `UPDATE` foi abortado. `handleGenericError` truncava em 2000 caracteres;
**`handleApiError` não truncava**.

Consequência: a pausa nunca foi gravada e o job **continuou como PROCESSING**.

### 3. O detector de travamento pausou pelo motivo errado

```
16:20:00 WARN Job 1 travado há 64 minutos — retry 2/3
```

Uma hora depois, `detectStalledJobs` viu o job parado em PROCESSING e o pausou.
Dessa vez a mensagem era curta e gravou. Status virou PAUSED, `retry_count = 2`.

O erro original nunca foi registrado — o que ficou no banco foi "travado por 64
minutos", que é sintoma, não causa.

### 4. Livelock entre dois schedulers

A cada 30 segundos, ininterruptamente:

```
🔄 Job 1 (PAUSED, retry 2/3) re-enfileirado na fila Redis (recovery)
⚠️ Job 1 não está QUEUED (status: PAUSED). Ignorando.
```

`recoverOrphanedJobs` empurrava o id para o Redis **sem gravar QUEUED**, e
`processQueue` recusava tudo que não estivesse QUEUED. Nenhum dos dois alterava
o estado. Rodou ~11.500 vezes em 4 dias.

E como nunca chegou ao retry 3, também nunca virou FAILED — limbo permanente.

### 5. O scheduler que deveria resolver isso nunca funcionou

`requeuePausedJobs` chamava `findPausedJobs()`, que era um esboço:

```java
return jobService.findById(0L).map(job -> List.<HistoricalDataJobEntity>of())
        .orElseGet(() -> List.of());
```

Devolvia lista vazia em qualquer situação. O caminho correto nunca executou; só
o caminho quebrado.

---

## Defeito adicional encontrado na investigação

O agrupamento por data usava `HashMap`, cuja ordem de iteração é arbitrária. No
log aparece literalmente:

```
✅ Criando reading para 2015-02-28
✅ Criando reading para 2015-02-26
✅ Criando reading para 2015-02-27
✅ Criando reading para 2015-02-24
```

O checkpoint era gravado a partir dessa iteração — então apontava para uma data
qualquer no meio da janela. Retomar dali pularia dias ou reprocessaria outros.

E o `catch (Exception)` genérico do laço avançava 30 dias e seguia em silêncio:
uma janela inteira sumia da série, e o job ainda podia terminar como COMPLETED.

---

## Correções aplicadas

| # | Onde | O quê |
| --- | --- | --- |
| 1 | `HistoricalDataJobService` | `truncateError()` na fronteira de persistência — registrar erro nunca mais falha por tamanho |
| 2 | `HistoricalDataJobService` | `recoverOrphanedJobs` grava `PAUSED → QUEUED` em transação antes de enfileirar — fim do livelock |
| 3 | `HistoricalDataJobScheduler` | Removido o `requeuePausedJobs` quebrado; responsabilidade unificada. Sobrou `failExhaustedPausedJobs`, que encerra quem esgotou tentativas |
| 4 | `HistoricalDataJobProcessor` | `TreeMap` no lugar de `HashMap`; checkpoint monotônico, nunca retrocede |
| 5 | `HistoricalDataJobProcessor` | Janela que falha fica registrada; job com buraco não termina como COMPLETED |
| 6 | `AnaApiService` | 429 tratado com `Retry-After` e backoff exponencial (2s/4s/8s); corpo de erro resumido, HTML identificado sem despejar o conteúdo |

Regressão coberta em
`src/test/java/com/geosegbar/infra/historical_data_job/service/HistoricalDataJobRecoveryTest.java`.

---

## Como diagnosticar de novo

Acesso ao servidor: [ACESSO_SERVIDOR.md](ACESSO_SERVIDOR.md).

```bash
# Estado dos jobs
ssh geosegbar-vps 'docker exec postgres-prod psql -U postgres -d geosegbar_prod -P pager=off -c "
SELECT id, instrument_name, status, checkpoint_date, created_readings,
       retry_count, started_at, error_message
FROM historical_data_job ORDER BY id DESC LIMIT 10;"'
```

```bash
# Livelock: se isto retornar número alto, os schedulers estão brigando
ssh geosegbar-vps 'docker logs --since 5m geosegbar-api-prod 2>&1 | grep -c "não está QUEUED"'
```

```bash
# Cobertura real por ano
ssh geosegbar-vps 'docker exec postgres-prod psql -U postgres -d geosegbar_prod -P pager=off -c "
SELECT EXTRACT(YEAR FROM date)::int AS ano, COUNT(*) FROM reading
WHERE instrument_id = <ID> GROUP BY 1 ORDER BY 1;"'
```

**Sinais de alerta:**

- `status = PAUSED` com `retry_count < 3` parado há mais de 10 minutos
- `error_message` descrevendo sintoma ("travado por N minutos") em vez de causa
- `checkpoint_date` que não avança entre duas consultas
- Log repetindo "não está QUEUED"

---

## Pendência

O job 1 continua PAUSED com checkpoint em 2019-01-18. Depois do deploy da
correção, `recoverOrphanedJobs` deve retomá-lo sozinho a partir do checkpoint.

Se não retomar, aí sim é caso de intervenção no banco — que segue as regras de
[ACESSO_SERVIDOR.md](ACESSO_SERVIDOR.md): mostrar o SQL exato e obter
autorização antes de executar.

Vale conferir depois se as lacunas dentro de 2015–2018 (~40 dias por ano) são
ausência de dado na ANA ou resquício das janelas puladas em silêncio.
