# `GET /mobile/dashboard` — o painel do aplicativo

> Branch `feat/mobile-dashboard`. Escrito para quem for revisar antes do merge.
> Complementa [`../ENTREGA-BACKEND.md`](../ENTREGA-BACKEND.md), que descreve as
> cinco rotas anteriores.

---

## 1. O que é, em uma frase

A tela inicial do app deixou de ser institucional e virou painel. Esta rota
devolve, **numa chamada**, tudo que esse painel desenha: os números do mês, a
série mensal de atividade do próprio inspetor, a inspeção por barragem, o estado
dos instrumentos, os instrumentos críticos, e as anomalias por grau de perigo.

## 2. Por que uma rota nova em vez de usar `/dashboard/**`

As cinco rotas de `/dashboard` foram feitas para a web e estão certas para ela.
Três coisas as tornam inviáveis no aplicativo:

| | `/dashboard/**` | `/mobile/dashboard` |
|---|---|---|
| Recorte de barragem | `damIds` **obrigatório**, vindo do cliente | deriva do **token** |
| Barragem não permitida na lista | **403 na resposta inteira** (`validateDamAccess`) | impossível — o servidor monta a lista |
| Cache | `@Cacheable`, 60s | nenhum |
| Chamadas para montar o painel | 5 | 1 |

O ponto do meio é o que decide. O app é **offline-first**: ele guarda a
permissão em cache para poder trabalhar sem sinal, e reconfere na
sincronização. Isso significa que a lista de barragens que ele mandaria está,
por construção, sempre a um passo de estar defasada — e cada defasagem viraria
uma tela de erro na mão de quem está no talude. Com o recorte nascendo no
servidor, o pior caso deixa de existir.

O cache é o segundo ponto. Sessenta segundos servem bem a um painel web que se
recarrega sozinho. No app existe **puxar-para-atualizar**: devolver a um gesto
explícito do inspetor o mesmo número de um minuto atrás seria pior do que a
espera da consulta — que é curta, porque tudo aqui é agregado.

**Nada existente foi tocado.** Namespace `/mobile` novo, e as seis consultas
são **métodos novos** em repositórios que já existiam. A web não corre risco.

## 3. Contrato

```
GET /mobile/dashboard?months=6&criticalLimit=5
Authorization: Bearer <token>
```

Os dois parâmetros são opcionais e **presos no servidor** — são as duas únicas
coisas que fariam a resposta crescer:

| | padrão | faixa aceita |
|---|---|---|
| `months` | 6 | 1–12 |
| `criticalLimit` | 5 | 1–25 |

Valor fora da faixa não dá `400`: é achatado para o limite. O cliente não
precisa saber a regra para acertar.

**Regra de acesso:** a mesma de `/dams/accessible` — usuário associado ao
cliente dono da barragem **E** `DamPermission.hasAccess = true`; `ADMIN` recebe
todas. Reaproveitada de `DamRepository`, não reescrita, para as duas rotas nunca
divergirem sobre o que o usuário pode ver.

**Sem barragem liberada:** `200` com tudo zerado. Nunca `403` — quem está
esperando o cadastro sair precisa ver a tela inicial, não um erro.

### Resposta

Envelope de sempre (`{success, message, data, errorCode}`, com `errorCode`
omitido quando nulo). O `data`:

```jsonc
{
  "generatedAt": "2026-09-02T14:03:11.204",
  "periodStart": "2026-04-01",
  "periodEnd":   "2026-09-02",

  "scope": { "damsTotal": 5, "instrumentsTotal": 41, "damIds": [1,7,12,19,23] },

  "kpis": {
    "myChecklistsThisMonth": 3,   "myReadingsThisMonth": 27,
    "myChecklistsInPeriod": 18,   "myReadingsInPeriod": 142,
    "damsInspectedThisMonth": 2,  "damsTotal": 5,
    "instrumentsTotal": 41,       "instrumentsWithReading": 38,
    "instrumentsWithoutReading": 3,
    "instrumentsAttention": 4,
    "instrumentsCritical": 2,
    "anomaliesInPeriod": 9
  },

  "activityByMonth": [
    { "month": "2026-04", "monthStart": "2026-04-01", "checklists": 4, "readings": 31 }
  ],

  "inspectionsByDam": [
    { "damId": 7, "damName": "Dique Auxiliar", "responsesInPeriod": 0,
      "lastInspectionAt": null, "inspectedThisMonth": false }
  ],

  "instrumentStatus": [ { "name": "NORMAL", "count": 30, "percentage": 78.9 } ],
  "instrumentsByType": [ { "typeId": 1, "typeName": "Piezômetro", "total": 20 } ],
  "myReadingsByType":  [ { "typeId": 1, "typeName": "Piezômetro", "total": 88 } ],
  "anomaliesByDangerLevel": [ { "name": "Baixo", "count": 6, "percentage": 66.7 } ],

  "criticalInstruments": [
    { "instrumentId": 55, "instrumentName": "PZ-04",
      "damId": 1, "damName": "PCH Exemplo",
      "limitStatus": "EMERGENCIA", "lastReadingDate": "2026-08-30" }
  ]
}
```

Datas seguem a convenção da casa: ISO-8601 **sem fuso**, hora local do
servidor.

### Cinco garantias que o serviço dá — e que o app depende

1. **`activityByMonth` é densa.** Sempre `months` pontos, na ordem. Mês sem
   trabalho vem com zero, não some. Um buraco viraria interpolação no gráfico de
   linha, e interpolação aqui contaria uma história falsa: "fez alguma coisa"
   onde não fez nada.
2. **`instrumentStatus` traz sempre os seis estados**, na ordem `NORMAL ·
   INFERIOR · SUPERIOR · ATENCAO · ALERTA · EMERGENCIA`, mesmo os zerados. Uma
   fatia que sumisse entre uma atualização e outra faria o gráfico trocar de cor
   e a legenda mudar de tamanho — e isso se lê como se o dado tivesse mudado de
   natureza, não de valor.
3. **`lastInspectionAt: null` é "nunca inspecionada"** — nunca data inválida,
   nunca texto no campo de data (o problema que a `last-checklist/v2` resolveu).
   E `inspectionsByDam` já vem **ordenada por quem está mais atrasado**: as nunca
   inspecionadas primeiro, depois da mais antiga para a mais recente. É a ordem
   em que o inspetor decide para onde ir.
4. **`instrumentsWithoutReading` fecha a conta.** Instrumento sem leitura no
   período **não** aparece em `instrumentStatus` — ele não é "normal", é sem
   dado, e a tela precisa poder dizer isso.
5. **Uma leitura é uma visita, não uma linha.** Ver a seção 5.

## 4. Custo — as nove agregações

Nenhuma leitura, resposta ou anomalia individual trafega. A resposta inteira
fica na casa das poucas dezenas de linhas.

| # | Consulta | Onde | Índice que cobre |
|---|---|---|---|
| 1 | ids das barragens acessíveis | `DamRepository` (já existia) | `dam_permissions(user_id)` |
| 2 | minhas inspeções por mês | `ChecklistResponseRepository` **nova** | `idx_checklist_response_user_created` |
| 3 | inspeção por barragem (todas, `LEFT JOIN`) | `ChecklistResponseRepository` **nova** | `idx_checklist_response_dam_created_desc` |
| 4 | minhas leituras por mês | `ReadingRepository` **nova** | `idx_reading_active_date_hour` |
| 5 | minhas leituras por tipo | `ReadingRepository` **nova** | idem |
| 6 | instrumentos por estado de limite | `ReadingRepository` **nova** | `idx_reading_instrument_active_date_hour` |
| 7 | instrumentos críticos (limitada) | `ReadingRepository` **nova** | idem |
| 8 | instrumentos por tipo | `InstrumentRepository` (já existia) | `idx_instrument_dam_type` |
| 9 | anomalias por grau de perigo | `AnomalyRepository` (já existia) | `idx_anomaly_dam_created` |

A consulta 3 usa `COUNT(...) FILTER (WHERE ...)` junto de um `MAX(...)` sem
recorte de data porque são **duas perguntas diferentes** sobre a mesma
varredura: "quanto se inspecionou no período" e "há quanto tempo esta barragem
não é visitada". Resolver cada uma numa consulta separada dobraria o trabalho
para o mesmo resultado.

**Nenhum índice novo foi criado.** Se o perfilamento em homologação mostrar que
as consultas 4 e 5 pesam, o candidato óbvio é `reading(user_id, date)` — hoje o
filtro por usuário cai depois do recorte por data, que já é seletivo.

## 5. Os dois cuidados de correção que valem registro

**A tabela `reading` guarda uma linha por saída do instrumento.** Um piezômetro
com três saídas grava três linhas para a mesma visita. Contar linhas responderia
"quantos valores foram calculados", não "quantas leituras o inspetor fez" — que
é a pergunta do painel. Por isso `COUNT(DISTINCT (instrument_id, date, hour))`:
a visita é a chave.

**Entre as saídas da mesma visita, o estado do instrumento é o pior delas.** Sem
isso, um instrumento com uma saída em `EMERGENCIA` e outra em `NORMAL` poderia
ser contado como normal, conforme a ordem física das linhas. Num sistema de
segurança de barragem esse é o erro que não se pode cometer. A ordem de
gravidade é **a mesma** já usada por `findInstrumentStatusDistributionByType`,
que alimenta o painel da web — de propósito, para os dois números nunca
divergirem.

## 6. O que falta antes de subir

⚠️ **As consultas não foram executadas contra banco.** O módulo compila
(`BUILD SUCCESS`) e todas as construções usadas já existem e estão provadas
neste mesmo repositório — `DISTINCT ON` com CTE
(`ReadingRepository.findInstrumentStatusDistributionByType`), projeção de
interface em query nativa (`DamRepository.findAccessibleByUserId`), e
`LIMIT :param` (`AnomalyRepository.findRecentByDamIds`). As construções
**novas** para este repositório são três, todas padrão Postgres:
`COUNT(DISTINCT (a, b, c))` com construtor de linha, `COUNT(...) FILTER (WHERE ...)`
e `to_char(..., 'YYYY-MM')`.

Rodar antes do merge:

```bash
./mvnw test -Dgroups=integration
```

(Precisa do Docker de pé — os testes usam Testcontainers com `postgres:16-alpine`.)

Vale a pena somar um `MobileDashboardRepositoryIT` que exercite as seis
consultas novas com dados de fixture, cobrindo em especial:

- um instrumento com **duas saídas na mesma visita**, uma em `NORMAL` e outra em
  `EMERGENCIA` → tem que contar **um** instrumento, em `EMERGENCIA`;
- uma barragem **sem nenhuma inspeção** → tem que aparecer, com
  `lastInspectionAt: null`;
- um usuário **sem barragem liberada** → `200` zerado, não `403`;
- um mês **sem trabalho** no meio do período → ponto com zero, não ausente.

## 7. Perguntas para o time

1. **`/mobile/dashboard` torna o `/dashboard/summary` (4.2) desnecessário para o
   app?** Para a web, não — ela precisa do recorte por `damIds` escolhido pelo
   usuário. A pergunta é se ainda vale construir a rota consolidada, ou se o
   esforço vai todo para o N+1 do `lastSelectedOption` e para o
   `/mobile/offline-package` (4.1).
2. **Vale espelhar o `criticalInstruments` numa rota própria** (`/mobile/instruments/critical`),
   para a aba de instrumentos não precisar do painel inteiro? Hoje ela vai
   reaproveitar o campo daqui.
3. **`generatedAt` sem fuso continua sendo o certo?** É a convenção da casa e
   foi mantida por coerência, mas esta rota é nova e não tem cliente web — seria
   o lugar barato de começar a devolver offset explícito, se a casa quiser
   caminhar para isso (o app trata o que chega como hora do servidor, e
   documentou a premissa).
