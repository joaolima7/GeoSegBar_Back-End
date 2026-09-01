# Solicitações ao backend — GeoSegBar API

> **De:** time do app mobile (Segbar Mobile / Flutter)
> **Para:** time do backend (GeoSegBar_Back-End, Java/Spring)
> **Status:** proposta para avaliação. **Nada aqui foi implementado**, e nenhuma
> linha do repositório do backend foi alterada.
>
> Todas as afirmações vêm de leitura do código, com `arquivo:linha`. Se algo
> aqui estiver errado, é erro nosso de leitura — corrijam sem cerimônia.

---

## 1. Como usar este documento

### O que ele é

Um levantamento do que o app precisa da API, **com o porquê de cada coisa** e
com os números que sustentam o pedido. Ele existe para vocês terem contexto
suficiente para **decidir a melhor forma** — não para dizer como fazer.

### O que ele **não** é

Não é especificação fechada. Todo contrato aqui é **sugestão**. Onde
escrevemos um JSON, leia-se *"o app precisa desta informação"*, não *"o campo
tem que se chamar assim"*.

> ### 🔑 O pedido que vale para todos os itens
>
> **Façam da forma que for melhor para o backend**, considerando:
>
> - **trazer só o dado que é usado** — a seção 3 lista, campo a campo, o que o
>   app efetivamente lê hoje;
> - **menos viagens de rede** — o app baixa por rede móvel, no campo, antes de
>   subir a barragem; a seção 4 propõe três consolidações;
> - **consulta eficiente** — apontamos os N+1 que encontramos, mas vocês
>   conhecem o modelo melhor que nós;
> - **evolução sem quebrar a web** — onde a mudança quebra contrato, sugerimos
>   versionar.
>
> **Se juntar dois ou três destes pedidos num endpoint só for melhor, junte.**
> Se separar for melhor, separe. Nós nos adaptamos ao que vocês decidirem — só
> precisamos saber o que foi decidido (seção 8).

> ### 🛡️ E o mais importante: **prefiram endpoint NOVO a mexer no existente**
>
> A web consome esta mesma API. **Nenhum pedido nosso vale o risco de quebrar
> ela.** Sempre que houver escolha entre criar uma rota nova e alterar uma que
> já existe, **criem a rota nova** — mesmo que pareça duplicação, mesmo que a
> rota antiga vire legado e morra depois, sem pressa.
>
> O app é o cliente novo aqui; o custo de uma rota a mais é nosso, não de
> vocês. Preferimos consumir três rotas novas a ver a web quebrar por causa de
> uma mudança que pedimos.
>
> **Classificamos cada pedido pelo risco** (seção 1.1). Onde a nossa proposta
> original mexia em algo existente, reescrevemos como rota nova.

### Prioridades

| | Significado |
|---|---|
| **P1** | O aparelho recebe dado de barragem que o usuário não pode ver. É o que mais nos preocupa. |
| **P2** | Corrige comportamento contraditório de permissão que o usuário final já relatou. |
| **P3** | Habilita telas novas. O app vive sem, com mais chamadas. |
| **P4** | Inconsistência em endpoint existente. |

### 1.1 Risco para a web — a classificação de cada pedido

| Tipo | Risco | Quais pedidos |
|---|---|---|
| 🟢 **Rota nova** | nenhum | 4.1, 4.2, P1.1, P1.2, P2.1, P2.2, P3.1, P3.4, P4.1 |
| 🟡 **Campo novo numa resposta existente** | baixo — consumidor de JSON ignora campo que não conhece | P3.3 (`isOpen` em `/anomaly-status`) |
| 🟡 **Parâmetro opcional novo, com o padrão preservando o comportamento atual** | baixo | P3.2 (`groupBy`), 4.3 (`updatedSince`) |
| 🔴 **Mudar comportamento de rota existente** | **alto — evitamos** | *nenhum* |

**Três propostas nossas caíram na faixa vermelha e foram reescritas** como rota
nova, justamente para não tocar na web:

| Proposta original | Por que quebraria | Como ficou |
|---|---|---|
| `damIds` nos endpoints de pacote, "ausente ⇒ só as acessíveis" | mudaria o **padrão** de rotas que a web usa: hoje ausência significa "todas do cliente" | rotas `/accessible/**` novas (P1.1) |
| `damIds` opcional no `/dashboard/**` e 403 virar "ignora" | a web pode depender do 403 para barrar tela | `/dashboard/summary` novo (4.2 / P3.1) |
| 5 campos a mais no `DamQuickAccessDTO` | aditivo é seguro, mas mexe num DTO que a web consome | rota `/dams/accessible` nova — com a opção aditiva mantida como alternativa (P1.2) |

**Regra prática que sugerimos:** se a mudança pode ser vista por um cliente que
não pediu por ela, ela merece rota nova. Se ela só acrescenta algo que ninguém
lê hoje, o risco é baixo e vocês decidem.

---

## 2. O problema que originou este documento

O app funciona **offline, em barragem sem sinal**. Ao entrar, ele baixa um
"pacote offline": barragens, checklists, instrumentos, catálogos, respostas
anteriores e últimas leituras.

Esse pacote é montado sobre endpoints **escopados por cliente**, e nenhum deles
respeita a permissão de barragem do usuário:

| Endpoint que o app usa hoje | Filtra por permissão? | Evidência |
|---|---|---|
| `GET /dams/client/{clientId}` | ❌ | `DamService.java:158-160` — delega direto ao repositório |
| `GET /checklists/client/{clientId}/with-last-answers` | ❌ | `ChecklistService.java:178` — `findAllByClientIdWithDetails(clientId)` |
| `GET /instruments/client/{clientId}` | ❌ | `InstrumentService.java:155` — `findByClientId(clientId, active)` |
| `GET /readings/client/{clientId}/latest-grouped` | ❌ | mesmo padrão |
| `GET /dams/quick-access` | ✅ | `DamService.java:178-188` — usa `AuthenticatedUserUtil.getCurrentUser()` |

**Na prática:** um inspetor com acesso a 2 de 12 barragens recebe no aparelho os
checklists e instrumentos das **12**. Ele só descobre a falta de permissão no
`403` do `verify-checklist`, na hora de responder.

Isso é três problemas de uma vez:

1. **Dado.** Informação de barragem que o usuário não pode ver fica **gravada
   no aparelho** dele, que vai a campo e pode ser perdido, roubado ou
   compartilhado.
2. **Banda e disco.** O pacote é várias vezes maior do que precisa, baixado por
   rede móvel.
3. **Experiência.** A barragem aparece na lista e só falha no fim — foi
   exatamente isso que o usuário do app relatou como confuso.

> ### O princípio que gostaríamos de firmar
>
> **O app só deve receber dado das barragens que o usuário pode acessar.**

---

## 3. O que o app realmente consome

Esta seção existe para vocês poderem **cortar payload com segurança**. São os
campos que o app lê hoje, extraídos dos nossos parsers.

### 3.1 Barragem — o app usa **7 campos escalares**

Fonte: `lib/features/dam/data/dtos/dam_output.dto.dart:26-39`.

```
id · name · acronym · city · state · latitude · longitude
```

**Só isso.** O app **não lê** `sections`, `reservoirs`, `psbFolders`,
`instruments`, `checklistResponses`, `damPermissions`, nem os `@OneToOne` de
`DamEntity.java:139-177`.

> ⚠️ **Correção nossa:** numa versão anterior deste documento afirmamos que o
> app precisava das seções da barragem. **Estava errado.** As seções chegam
> pelo instrumento (`sectionId`/`sectionName` em `InstrumentResponseDTO`), não
> pela barragem. Isso **reduz muito** o pedido P1.2.

**O que isso significa hoje:** `GET /dams/client/{clientId}` serializa a
`DamEntity` inteira, com as coleções que o `initializeLazyCollections`
inicializa (`DamService.java:190+`), e o app **descarta tudo** menos 7 campos.

### 3.2 Instrumento — usa quase tudo do DTO, e precisa mesmo

Fonte: `lib/features/instrument/data/dtos/instrument_output.dto.dart`.

```
id · name · location · latitude · longitude · noLimit · active
damId · damName · instrumentType · sectionId · sectionName · activeForSection
inputs[]     → id, acronym, name, precision, measurementUnit{name, acronym}
constants[]  → acronym, value
outputs[]    → id, acronym, name, precision, equation, measurementUnit,
               statisticalLimit{lowerValue, upperValue},
               deterministicLimit{attentionValue, alertValue, emergencyValue}
```

Aqui **não há desperdício relevante** — o app usa o que recebe. `InstrumentResponseDTO`
já é um DTO, e isso ajuda.

### 3.3 Checklist — a árvore inteira, porque o preenchimento precisa dela

```
id · name · dam{id, name} · templateQuestionnaires[] → templateQuestions[]
  → question{id, questionText, type} · options[] · allOptions[]
  · lastSelectedOption{label} · orderIndex
```

O `lastSelectedOption` é essencial: é ele que alimenta as **regras de transição**
que bloqueiam opções conforme a resposta da inspeção anterior.

### 3.4 O que o app **nunca** usa

Se ajudar a cortar:

| Recurso | Campos que não consumimos |
|---|---|
| Barragem | `sections`, `reservoirs`, `psbFolders`, `instruments`, `checklistResponses`, `damPermissions`, `classificationDam`, `potentialDamage`, `pae`, `psb` |
| Permissões | `documentationPermission`, `attributionsPermission`, `instrumentationPermission` — o app só precisa de `damPermissions` e `routineInspectionPermission.isFillMobile` |
| Usuário | `sex`, e o grafo de `clients` além de `{id, name}` |

---

## 4. Três oportunidades de consolidação

> O usuário do app pediu explicitamente que avaliássemos **juntar retornos**.
> As três abaixo são as que fazem mais diferença. **Vocês decidem se valem.**

### 4.1 Um endpoint só para o pacote offline · **o de maior impacto**

**Hoje:** o download offline são **7 etapas** e 7+ viagens de rede. Se
adotarmos o contorno por barragem (seção 6), viram **2×N + 5**. Com 12
barragens, ~29 requisições em rede móvel de campo.

**Proposta:**

```
GET /mobile/offline-package
```

Sem parâmetro; deriva o usuário do token. Devolve, **numa viagem**, tudo que o
app precisa para funcionar offline, já recortado pelas barragens permitidas:

```jsonc
{
  "generatedAt": "2026-08-31T14:02:00Z",
  "permissions": { /* ver P2.1 */ },
  "dams":        [ /* 7 campos da §3.1 */ ],
  "checklists":  [ /* com lastSelectedOption, §3.3 */ ],
  "instruments": [ /* §3.2 */ ],
  "limitStatus": [ /* estado de limite por instrumento */ ],
  "latestReadings": [ /* últimas leituras agrupadas */ ],
  "catalogs": { "dangerLevels": [], "anomalyStatus": [] }
}
```

**Por que vale a pena:**

- **1 requisição no lugar de ~29.** Em rede móvel instável, cada requisição é
  uma chance de falhar; hoje uma falha no meio deixa o pacote incompleto.
- **Consistência.** Hoje as 7 etapas podem ver estados diferentes do banco. Numa
  transação só, o pacote é coerente.
- **Vocês otimizam as consultas juntas**, sabendo que é um pacote — em vez de 7
  serviços que não sabem uns dos outros.
- O app já tem progresso por etapa; podemos derivá-lo do tamanho da resposta ou
  simplesmente mostrar as etapas de **gravação** local.

**Se acharem grande demais:** a alternativa é a P1.1 (aceitar `damIds` nos
endpoints existentes), que resolve o problema de permissão sem consolidar.
As duas resolvem; esta resolve melhor.

### 4.2 Um endpoint só para o painel da Home

**Hoje:** os 5 endpoints de `/dashboard` já existem e já são `@Cacheable`
(`DashboardService.java:76-77, 91-92, 107-108, 122-123, 166-167`). A Home
faria **5 chamadas** que compartilham exatamente a mesma chave de cache
(`damIds` + período).

**Proposta:**

```
GET /dashboard/summary?damIds=&startDate=&endDate=&include=checklists,instruments,anomalies,recent
```

Um objeto com os blocos pedidos em `include`. Como o cache já é por
`damIds`+período, a composição é barata.

**Ganho:** 5 → 1 viagem na abertura da tela mais usada do app.

### 4.3 Sincronização incremental · **o maior ganho de banda**

**Hoje:** o app **rebaixa o pacote inteiro** a cada sincronização (a cada 12h,
ou quando o usuário pede). Um checklist que não mudou é baixado de novo, toda
vez.

**Proposta — 🟡 aditivo, e só nas rotas novas:** qualquer coisa que permita
"só o que mudou". Como as rotas do pacote seriam novas (P1.1 / 4.1), isto não
encosta em nada que a web use:

- `?updatedSince=2026-08-30T10:00:00Z`, devolvendo apenas os registros com
  `updatedAt` posterior — mais uma lista de **ids removidos**, para o app
  apagar o que saiu; **ou**
- `ETag` + `If-None-Match`, com `304 Not Modified` quando nada mudou.

**Por que importa tanto:** o inspetor sincroniza **antes de subir a barragem**,
muitas vezes com sinal ruim e plano de dados limitado. Hoje ele paga o pacote
inteiro para descobrir que nada mudou.

**Requisito para funcionar:** as entidades precisam de `updatedAt` confiável.
`DamPermissionEntity` já tem (`DamPermissionEntity.java:70-74`); as demais,
vocês sabem dizer.

---

## P1 · Os pedidos que sustentam o princípio

### P1.1 — Pacote offline escopado por permissão

**Problema:** os três endpoints da seção 2 são por cliente e não filtram.

**O que o app faz sem isso:** `GET /dams/quick-access` para os ids permitidos e,
depois, `GET /checklists/dam/{damId}/with-last-answers`
(`ChecklistController.java:105`) e `GET /instruments/dam/{damId}`
(`InstrumentController.java:41`) **uma vez por barragem** — 2×N chamadas.
Funciona; é caro.

**Proposta — 🟢 rotas novas, sem tocar nas existentes:**

```
GET /checklists/accessible/with-last-answers
GET /instruments/accessible?active=true
GET /readings/accessible/latest-grouped?limit=3
```

- **Sem parâmetro de escopo.** O conjunto de barragens vem do usuário do token,
  pela mesma regra que o `quick-access` já usa
  (`DamService.java:178-188`), com o atalho de `ADMIN` incluído.
- As rotas `/client/{clientId}/**` de hoje **ficam como estão**, e a web não
  sente nada.
- Se quiserem permitir recorte, um `damIds` **opcional** nessas rotas novas é
  seguro — a regra continua "intersecta com as permitidas".

> **Por que não pedimos `damIds` nas rotas atuais:** teríamos que definir o que
> acontece quando o parâmetro vem ausente. Manter "todas do cliente" não
> resolve o nosso problema; mudar para "só as acessíveis" **mudaria o
> comportamento de uma rota que a web usa**. Rota nova evita a escolha.

**Alternativa melhor ainda:** a consolidação **4.1** (`/mobile/offline-package`),
que também é rota nova e resolve isto junto com o número de viagens de rede.

**Aceite:** usuário com acesso a 2 de 12 barragens recebe, nas três rotas,
exclusivamente dado das 2.

---

### P1.2 — Barragens permitidas com os 7 campos que o app usa

**Problema:** hoje nenhum endpoint entrega, sozinho, "as barragens que eu posso
ver, com os campos que eu preciso":

| | filtra por permissão | tem os 7 campos da §3.1 |
|---|---|---|
| `GET /dams/quick-access` | ✅ | ❌ — faltam `acronym, city, state, latitude, longitude` (`DamQuickAccessDTO.java`) |
| `GET /dams/client/{clientId}` | ❌ | ✅ (e muito mais que não usamos) |

**O que o app faz sem isso:** baixa a lista completa por cliente e filtra pelos
ids do `quick-access`. Funciona — **mas o aparelho recebe o dado das barragens
não permitidas**, que é justamente o que este documento quer evitar.

**Proposta — 🟢 rota nova, aproveitando a consulta que já existe:**

```
GET /dams/accessible
```

```jsonc
{ "damId": 3, "damName": "Barragem Norte", "status": "ACTIVE",
  "clientId": 1, "clientName": "Cliente X",
  "acronym": "BN", "city": "Mariana", "state": "MG",
  "latitude": -20.37, "longitude": -43.41 }
```

São os 5 campos do `DamQuickAccessDTO` **mais os 5 que faltam**. A consulta
filtrada já existe (`DamRepository.findQuickAccessByUserId`); provavelmente é
só ampliar a projeção e expor numa rota nova.

**⚠️ Não precisamos do grafo completo de `DamEntity`** — pelo contrário, quanto
menor melhor. Ver §3.1.

**Alternativa 🟡, se preferirem não criar rota:** acrescentar os 5 campos ao
`DamQuickAccessDTO` existente. Acrescentar campo a uma resposta JSON é
**aditivo** e normalmente não quebra consumidor nenhum — mas como o
`quick-access` é usado pela web, a decisão é de vocês. **Na dúvida, rota
nova.**

**Aceite:** o app monta a lista de barragens **sem nunca receber dado de
barragem não permitida**, e sem baixar coleções que descarta.

**Nota:** é o pedido de menor esforço do documento e o que mais fecha o
princípio. Se for para fazer só um, sugerimos este.

---

## P2 · Permissão sem contradição

### P2.1 — As permissões do usuário logado, enxutas

**Problema:** `LoginResponseDTO` (`LoginResponseDTO.java:9-19`) devolve
`id, name, email, phone, sex, role, isFirstAccess, token, clients` — **nenhuma
permissão**. Logo após o login, o app não sabe se o usuário pode preencher
checklist no aplicativo (`isFillMobile`) nem quais barragens ele vê.

Existe `GET /user-permissions/user/{userId}` (`UserPermissionsController.java:28-34`),
mas ele serializa entidades JPA inteiras — traz `documentationPermission`,
`attributionsPermission` e `instrumentationPermission`, que o app **não usa**
(§3.4) — e exige carregar o `userId` na mão.

**Por que isto importa:** o comportamento que o usuário relatou como
contraditório — *"online diz que não tenho permissão, offline deixa
preencher"* — existe porque **o app não tem onde guardar a permissão**. Sem
isso ele não consegue responder "pode?" sem rede. Com isso, a permissão vira
dado cacheado e a resposta passa a ser a mesma nos dois estados.

**Proposta:**

```
GET /me/permissions
```

```jsonc
{
  "userId": 42,
  "role": "COLLABORATOR",
  "routineInspection": { "isFillMobile": true, "isFillWeb": false },
  "dams": [ { "damId": 3, "clientId": 1, "hasAccess": true  },
            { "damId": 11, "clientId": 1, "hasAccess": false } ],
  "updatedAt": "2026-08-31T14:02:00Z"
}
```

- Sem parâmetro; deriva do token.
- **`updatedAt` importa:** permite ao app dizer *"permissões conferidas há 2
  dias"* quando estiver offline — honesto, em vez de fingir certeza.
- **Incluir as barragens com `hasAccess: false` é útil:** o app pode mostrar a
  barragem **bloqueada com o motivo**, em vez de simplesmente sumir com ela.
- **Se a 4.1 for adotada**, isto pode ser só um bloco dela em vez de rota
  própria — mas continua sendo útil separado, porque o app quer reconferir
  permissão **sem** rebaixar o pacote.

**Aceite:** uma chamada após o login basta para o app saber tudo sobre acesso, e
guardar para uso offline.

---

### P2.2 — Verificação de permissão de checklist **em lote**

**Problema:** `GET /user-permissions/verify-checklist`
(`UserPermissionsController.java:45-67`) responde por **um** par
`(damId, checklistId)`. Para pintar "o que posso responder" com N barragens × M
checklists seriam N×M chamadas.

**O que o app faz sem isso:** deriva de `hasAccess` + `isFillMobile` (P2.1) e
mantém o `verify-checklist` como confirmação ao abrir o preenchimento. Cobre
quase tudo, menos o motivo (c) do serviço — *"checklist não pertence à
barragem"* (`UserPermissionsService.java:302-306`).

**Proposta:**

```
GET /user-permissions/verify-checklists?damId=3
```

```jsonc
{ "damId": 3,
  "checklists": [
    { "checklistId": 10, "allowed": true },
    { "checklistId": 11, "allowed": false,
      "reason": "O usuário não tem permissão para preencher checklists no aplicativo móvel" } ] }
```

**Sobre as mensagens — importante:** o serviço tem hoje **5 motivos distintos**
de negação (`UserPermissionsService.java:274-325`), cada um apontando um
cadastro diferente. **Por favor mantenham essa granularidade** na versão em
lote: é ela que permite ao app dizer ao inspetor *o que fazer*, em vez de só
"sem permissão". Um código estável junto do texto (ex.: `NO_DAM_ACCESS`,
`NO_MOBILE_FILL`) seria ainda melhor — o app poderia agir por código e exibir o
texto de vocês.

---

## P3 · Os painéis

> O app vai passar a consumir os 5 endpoints de `/dashboard`, que já existem.
> Os itens abaixo são melhorias, não bloqueios.

### P3.1 — Dashboard sem `damIds` obrigatório, e sem 403 total

**Dois problemas no mesmo lugar:**

1. Os 5 endpoints exigem `@RequestParam List<Long> damIds`
   (`DashboardController.java:35, 54, 73, 92, 111`) — o app precisa descobrir os
   ids antes de qualquer painel.
2. `DashboardService.validateDamAccess` (`DashboardService.java:58-72`) lança
   `ForbiddenException` se **uma** barragem da lista não for permitida. Uma id
   defasada — permissão revogada entre o cache do app e a chamada — **derruba a
   tela inteira**.

**Proposta — 🟢 rota nova, que é a mesma da consolidação 4.2:**

```
GET /dashboard/summary?include=checklists,instruments,anomalies,recent
                      &damIds=       (opcional)
                      &startDate=&endDate=
```

- `damIds` **opcional**; ausente ⇒ todas as acessíveis do usuário do token.
- Quando vier preenchido, **ignora** as não permitidas em vez de lançar 403, e
  informa no corpo o que considerou:

```jsonc
{ "consideredDamIds": [3, 7], "ignoredDamIds": [11], "checklists": { ... } }
```

- **As 5 rotas `/dashboard/**` de hoje ficam intocadas**, com o `damIds`
  obrigatório e o 403 total. A web não sente nada.

**Por que o 403 parcial dói tanto no app:** ele é offline-first e trabalha com
permissão **cacheada**. A defasagem entre o cache e o servidor é **normal**,
não excepcional — e hoje ela zera a tela inteira. Na web, onde a permissão é
sempre fresca, o 403 total faz todo sentido; por isso a diferença merece rota
própria em vez de mudança de comportamento.

---

### P3.2 — Série por período no resumo de checklist

**Problema:** `ChecklistDashboardSummaryDTO` (`ChecklistDashboardSummaryDTO.java:5-11`)
devolve só o total do período. "Este mês × mês passado" = 2 chamadas; 12 meses
= 12 chamadas.

**Proposta — 🟡 parâmetro opcional novo** (ausente ⇒ comportamento de hoje,
intacto), ou, melhor ainda, só dentro do `/dashboard/summary` novo da P3.1:
`groupBy=month` (ou `week`) acrescentando:

```jsonc
"byPeriod": [ { "period": "2026-07", "totalResponses": 12, "damsInspected": 4 },
              { "period": "2026-08", "totalResponses": 18, "damsInspected": 5 } ]
```

`damsInspected` já resolveria, de saída, o número que a tela precisa —
*"X de Y barragens inspecionadas este mês"* — que hoje o app teria que derivar
contando `responsesByDam` com `totalResponses > 0`.

---

### P3.3 — Saber quais status de anomalia são "abertos"

**Problema:** `/dashboard/anomalies/status-summary` devolve `categories[]` só
com o **nome** do status (`CategoryCountDTO.java:3-7`). Não há como saber quais
nomes significam "aberto". O app teria que **hardcodar nomes** — que quebra no
dia em que alguém renomear um status no cadastro.

**Proposta — 🟡 campo novo, aditivo:** um booleano no catálogo. Em
`GET /anomaly-status`, acrescentar `isOpen` (ou `isTerminal`) a cada
`AnomalyStatusEntity`. Consumidor que não conhece o campo simplesmente o
ignora, então o risco para a web é baixo. O app deriva a contagem sem comparar
texto.

Se ainda assim preferirem não tocar no catálogo, uma rota nova
`GET /anomaly-status/flags` resolve igual.

---

### P3.4 — Listagem de anomalias com filtro e paginação

**Problema:** fora do dashboard, o único caminho é `GET /anomalies/dam/{damId}`
(`AnomalyController.java:57-64`): **todas** as anomalias da barragem, sem
paginação, sem filtro de status, sem período. Impraticável no app.

**Proposta — 🟢 rota nova:**

```
GET /anomalies/filter?damIds=&status=&startDate=&endDate=&page=&size=
```

Seguindo a convenção que vocês já usam em `GET /instruments/filter`
(`InstrumentController.java:53`).

> **Atenção:** `GET /anomalies` **já existe** (`AnomalyController.java:32`) e
> devolve todas. Não queremos parâmetros nela — daí a rota `/filter` separada.

**Prioridade real: baixa.** `/dashboard/anomalies/recent` já cobre o feed da
Home.

---

## P4 · Correção em endpoint existente

### P4.1 — `last-checklist`: data como String, sentinela no mesmo campo, e N+1

**Endpoint:** `GET /checklist-responses/client/{clientId}/last-checklist`
(`ChecklistResponseController.java:63-70`).

Em `ChecklistResponseService.getLastChecklistDateByClient`
(`ChecklistResponseService.java:478-497`):

1. **A data vem como String formatada** `"yyyy-MM-dd HH:mm:ss"` e, quando a
   barragem nunca foi inspecionada, **o mesmo campo** recebe o texto
   `"Nenhuma inspeção realizada."` (linhas 488 e 493). O app precisa tentar
   parsear e, falhando, comparar string — frágil, e quebra se o texto mudar ou
   ganhar acento diferente.
2. **N+1:** o método percorre as barragens do cliente e faz uma consulta por
   barragem dentro do laço (linhas 484-496).

**Proposta:**

```jsonc
[ { "damId": 3, "damName": "Barragem Norte", "lastChecklistDate": "2026-08-14T09:30:00Z" },
  { "damId": 7, "damName": "Dique Auxiliar",  "lastChecklistDate": null } ]
```

- **ISO-8601**, ou `null` quando não houver inspeção.
- O texto amigável fica com o app, que já traduz para o formato da tela.
- Se possível, uma consulta agregada única no lugar do laço.

**Compatibilidade — 🟢 rota nova, obrigatoriamente.** Mudar o tipo de
`lastChecklistDate` de String para data **quebra** qualquer consumidor atual, e
a web usa esta rota. Então: `/last-checklist/v2` (ou o nome que preferirem),
com a rota antiga **intacta e sem prazo para morrer**. O app consome a nova
assim que existir; a web migra quando quiser, ou nunca.

---

## 7. Notas transversais

- **Envelope.** Tudo vem em `WebResponseEntity` — `{success, message, data,
  errorCode}`. Mantenham; o app já depende disso.
- **401 × 403.** `SecurityConfig.java:90-96` firma o contrato que o app segue:
  **401** = não autenticado / sessão expirada ⇒ o app desloga; **403** =
  autenticado sem permissão ⇒ o app só avisa. **Por favor não usem 403 para
  sessão expirada** — o app deslogaria o inspetor no meio de uma inspeção.
- **Datas.** Onde puder, **ISO-8601 com fuso**. O app já teve bug de fuso e
  prefere converter ele mesmo.
- **Nulo × sentinela.** Ausência de dado é `null`, não texto (ver P4.1).
- **Paginação.** Onde houver, o padrão do Spring já usado em
  `/readings/instrument/{id}/grouped` está ótimo.
- **Limites.** Se algum endpoint novo tiver teto de itens, digam qual — o app
  precisa saber para paginar em vez de truncar em silêncio.

---

## 8. 📋 O que precisamos de volta — a documentação da entrega

> **Este é um pedido explícito.** Ao terminar o que for implementado, precisamos
> de um documento de volta para conseguirmos implementar do lado do app sem
> adivinhação e sem ida e volta.

Não precisa ser bonito. Precisa ser **completo e literal**. Um `.md` no
repositório do backend, ou colado numa mensagem — tanto faz.

### Para cada endpoint criado ou alterado

1. **Método e rota completa**, com um exemplo real de URL montada.
2. **Autenticação** — o que vai no header; se deriva do token ou exige id.
3. **Parâmetros**: nome, tipo, **obrigatório ou opcional**, valor padrão quando
   ausente, e o que acontece com valor inválido.
4. **Exemplo de request** de verdade (copiável).
5. **Exemplo de response de sucesso**, **JSON completo e real** — não abreviado
   com `...`. Precisamos ver os nomes exatos, o aninhamento e o tipo de cada
   campo.
6. **Tabela de campos da resposta**: nome, tipo, **pode ser nulo?**, e o que
   significa. O "pode ser nulo" é o que mais nos custa quando falta.
7. **Todos os casos de erro**: status HTTP, `errorCode`, `message`, e **quando**
   cada um acontece. Especialmente: o que devolve quando a lista fica vazia, e
   o que devolve quando o usuário não tem permissão para parte do pedido.
8. **Formato de data** usado, com fuso.
9. **Paginação**, se houver: parâmetros, campos de metadado, e o teto de itens.
10. **Cache**: se tem `@Cacheable`, qual a chave e **quanto tempo o dado pode
    estar defasado** — o app precisa saber se pode mostrar "atualizado agora"
    ou "atualizado há pouco".

### Além disso, para o conjunto

11. **O que mudou em endpoint que já existia** — e se o contrato antigo continua
    funcionando ou não. Se quebrou, dizer **desde quando** e se há versão antiga
    disponível. **Se nada existente foi tocado, digam isso explicitamente** — é
    a confirmação de que a web está a salvo, e vale por si só.
12. **As regras de negócio** que o endpoint aplica e que o app precisa conhecer.
    Exemplo concreto: *"ADMIN recebe todas as barragens; COLLABORATOR só as com
    `hasAccess = true`"* — isso muda o que o app mostra e não dá para inferir do
    JSON.
13. **O que NÃO foi implementado** deste documento, e por quê. Isso é tão útil
    quanto o que foi: evita que a gente construa esperando algo que não vem.
14. **Ambiente onde já está disponível** (homologação, produção) e a partir de
    quando.

### Um formato que serve

````markdown
## GET /dams/accessible

**O que faz:** devolve as barragens que o usuário do token pode acessar.
**Auth:** Bearer token. O usuário é derivado do token; não recebe userId.
**Regra:** ADMIN recebe todas; COLLABORATOR só as com `hasAccess = true`.

### Parâmetros
| nome | tipo | obrigatório | padrão | observação |
|---|---|---|---|---|
| status | string | não | todos | filtra por StatusEnum |

### Request
GET /dams/accessible
Authorization: Bearer eyJ...

### Response 200
```json
{ "success": true, "message": "Barragens obtidas com sucesso!",
  "data": [ { "damId": 3, "damName": "Barragem Norte", "acronym": "BN",
              "city": "Mariana", "state": "MG",
              "latitude": -20.37, "longitude": -43.41,
              "status": "ACTIVE", "clientId": 1, "clientName": "Cliente X" } ],
  "errorCode": null }
```

### Campos
| campo | tipo | nulo? | significado |
|---|---|---|---|
| damId | long | não | id da barragem |
| latitude | double | **sim** | nulo quando não cadastrada |

### Erros
| status | errorCode | quando |
|---|---|---|
| 401 | — | token ausente ou expirado |

### Cache
`@Cacheable("accessibleDams")`, chave = userId, TTL 5 min.
````

**Com isso em mãos, implementamos do lado do app sem precisar perguntar nada.**

---

## 9. Resumo

| # | Pedido | Prio | Tipo | Esforço | Ganho principal |
|---|---|---|---|---|---|
| **4.1** | `GET /mobile/offline-package` | opcional | 🟢 rota nova | alto | ~29 → 1 requisição; pacote consistente |
| **4.2** | `GET /dashboard/summary` | opcional | 🟢 rota nova | baixo | 5 → 1 requisição na tela mais usada |
| **4.3** | `updatedSince`/ETag **nas rotas novas** | opcional | 🟡 aditivo | médio | deixa de rebaixar o que não mudou |
| **P1.1** | `/checklists/accessible/**`, `/instruments/accessible`, `/readings/accessible/**` | **P1** | 🟢 rota nova | médio | para de entregar dado não permitido |
| **P1.2** | `GET /dams/accessible` | **P1** | 🟢 rota nova | **baixo** | idem, com o menor esforço do documento |
| **P2.1** | `GET /me/permissions` | **P2** | 🟢 rota nova | baixo | permissão cacheável ⇒ acaba a contradição online/offline |
| **P2.2** | `GET /user-permissions/verify-checklists` | **P2** | 🟢 rota nova | médio | N×M → 1 chamada por barragem |
| **P3.1** | `/dashboard/summary` sem `damIds` obrigatório | **P3** | 🟢 rota nova (= 4.2) | baixo | painel não morre por permissão defasada |
| **P3.2** | Série por período + `damsInspected` | **P3** | 🟡 param opcional | médio | 12 chamadas → 1 |
| **P3.3** | `isOpen` no status de anomalia | **P3** | 🟡 campo novo | **muito baixo** | app para de hardcodar nome de status |
| **P3.4** | `GET /anomalies/filter` | **P3** | 🟢 rota nova | médio | (baixa prioridade real) |
| **P4.1** | `/last-checklist/v2` — ISO, sem N+1 | **P4** | 🟢 rota nova | baixo | remove parsing frágil e uma consulta em laço |

**Nenhum pedido exige alterar o comportamento de rota existente.** Os dois
únicos 🟡 são aditivos, e ambos têm alternativa em rota nova se vocês
preferirem.

Conferimos que **nenhum dos caminhos propostos colide** com rota já mapeada nos
controllers — `/dams/accessible`, `/checklists/accessible/**`,
`/instruments/accessible`, `/readings/accessible/**`, `/me/permissions`,
`/user-permissions/verify-checklists`, `/dashboard/summary`,
`/anomalies/filter`, `/last-checklist/v2` e `/mobile/offline-package` estão
todos livres. Se algum conflitar com algo que não enxergamos, o nome é o de
menos — troquem à vontade.

**Se for para escolher um:** **P1.2**. Menor esforço, e é o único que fecha de
verdade o princípio de não entregar ao aparelho o dado que o usuário não pode
ver.

**Se der para escolher dois:** P1.2 + **P2.1** — juntos, acabam com a
contradição de permissão que o usuário final já relatou.

---

## 10. O que o app faz enquanto isso

Nada aqui bloqueia a rodada de melhorias do app. Enquanto os pedidos são
avaliados, o app vai:

1. Usar `GET /dams/quick-access` como fonte da verdade sobre acesso.
2. Usar `GET /user-permissions/user/{userId}` para `isFillMobile` e `hasAccess`,
   **guardando localmente** para responder offline com a última verdade
   conhecida — sempre mostrando a data.
3. Montar o pacote offline **por barragem permitida** (2×N chamadas), aceitando
   o custo de rede.
4. Filtrar localmente o que vier a mais e **apagar do aparelho** o dado de
   barragem cuja permissão tenha sido revogada.

Qualquer dúvida sobre o que o app espera de cada contrato, é só chamar — e se
alguma proposta daqui for ruim para o backend, digam: o objetivo é o app
receber o dado certo, não estes contratos em particular.
