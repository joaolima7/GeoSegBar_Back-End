# Entrega ao app — GeoSegBar API

Resposta ao `SOLICITACOES-BACKEND.md`. Um item por seção, no formato pedido na
seção 8 daquele documento.

**Leiam primeiro a seção 0.** Ela tem as três respostas que mudam o que vocês
vão construir.

---

## 0. O que vocês precisam saber antes de ler o resto

### 0.1 Nenhum endpoint existente foi tocado

**Confirmação explícita, que a seção 8 pediu:** nenhuma rota que já existia
mudou de comportamento, de contrato, de tipo de campo ou de formato de
resposta. Tudo abaixo é **rota nova**. A web está a salvo.

Isso vale inclusive para o que vocês classificaram como 🟡: chegamos a
implementar o `isOpen` da P3.3 como campo novo em resposta existente e
**voltamos atrás** — ver 0.3.

### 0.2 `acronym` de barragem não existe

O `dam_output.dto.dart` declara `acronym` entre os 7 campos escalares da §3.1.
**Esse campo não existe no backend** — nem na entidade `DamEntity`, nem na
tabela `dam`, e nunca existiu. Existe sigla de constante, de input e de output
de instrumento; de barragem, não.

Ou seja: o campo sempre chegou nulo para vocês, e continuará chegando nulo.
Criar a coluna agora entregaria nulo para as 849 barragens do mesmo jeito, só
que com uma migração no meio.

**Se a sigla for necessária de verdade, é cadastro novo**, não campo esquecido:
alguém precisa preencher 849 valores, e isso é decisão de produto. Digam se
querem que a gente encaminhe.

O `GET /dams/accessible` entrega **9 campos**: os 5 do quick-access mais
`city`, `state`, `latitude` e `longitude`.

### 0.3 A P3.3 (`isOpen`) não foi feita — por decisão nossa

Foi implementada e revertida. O motivo é o que vocês mesmos escreveram como
regra prática: se a mudança pode ser vista por um cliente que não pediu por
ela, ela merece rota nova. O `isOpen` aparecia em `GET /anomaly-status` **e**
aninhado em `status.isOpen` dentro de `GET /anomalies`, que a web consome.

Não achamos que o ganho justificasse mexer no catálogo. **O problema que vocês
levantaram continua real** — comparar nome de status com texto fixo quebra no
dia em que alguém renomear um status. Se isso doer na prática, tragam de volta
e a gente resolve com rota nova (`GET /anomaly-status/flags`), sem tocar no
catálogo.

Por ora, os status em produção são:

| id | nome |
|---|---|
| 1 | Pendente |
| 2 | Em andamento |
| 3 | Concluído |
| 4 | Em monitoramento |
| 5 | `--` |

Nota que pode ser útil: **157 das 161 anomalias em produção estão no status
`--`**. Ele é o default de fato, não um status de exceção.

---

## 1. Convenções válidas para tudo abaixo

**Envelope.** Mantido como vocês pediram:

```json
{ "success": true, "message": "...", "data": {}, "errorCode": null }
```

`errorCode` é omitido do JSON quando nulo (`@JsonInclude(NON_NULL)`) — ou seja,
em resposta de sucesso o campo **não aparece**, em vez de aparecer como `null`.

**Datas.** `LocalDateTime` serializado em ISO-8601 **sem fuso**:
`"2026-07-02T09:36:02.530351"`. É horário local do servidor. Não há `Z` nem
offset — não tratem como UTC.

**Autenticação.** `Authorization: Bearer <token>` em todas. Onde diz "deriva do
token", o endpoint **não aceita** `userId` e não há como pedir dado de outro
usuário.

**401 × 403.** Contrato inalterado: 401 = deslogar; 403 = avisar e ficar onde
está. `errorCode` nas duas: `NOT_AUTHENTICATED`, `SESSION_EXPIRED`,
`INVALID_TOKEN`, `ACCOUNT_UNAVAILABLE` (401) e `FORBIDDEN` (403).

**Cache.** **Nenhum** dos endpoints abaixo é `@Cacheable`. Todo dado é lido no
momento da chamada. Vocês podem mostrar "atualizado agora".

**Ambiente.** Nada subiu ainda. Tudo está em branch, compilando, com a suíte no
mesmo baseline de antes. Avisamos quando for para homologação.

---

## 2. A regra de acesso a barragem, que vale em todas as rotas novas

Foi centralizada num único componente (`DamAccessService`). Antes existia
escrita em três lugares diferentes, com critérios levemente distintos.

**A regra:**

> O usuário precisa estar associado ao **cliente** dono da barragem **E** ter
> `DamPermission` com `hasAccess = true`. `ADMIN` recebe todas.

**Duas maneiras de reagir a barragem não permitida**, e importa saber qual cada
rota usa:

| Comportamento | Onde | Por quê |
|---|---|---|
| **Ignora** a não permitida | `/anomalies/filter` | vocês trabalham com permissão cacheada; a defasagem é normal, não excepcional. Um id velho não derruba a tela |
| **403** | rotas que pedem uma barragem específica | aí vocês precisam saber que não podem |

As rotas sem parâmetro de escopo (`/dams/accessible`, `/me/permissions`)
simplesmente devolvem o conjunto permitido — não têm como dar 403 por
permissão.

---

## 3. `GET /dams/accessible`  · P1.2

**O que faz:** devolve as barragens que o usuário do token pode acessar, com os
campos escalares que o app usa.
**Auth:** Bearer. Usuário derivado do token; **não** recebe `userId` nem
`clientId`.
**Regra:** ADMIN recebe todas; demais, só as com `hasAccess = true` **e** cujo
cliente o usuário está associado.

### Parâmetros

Nenhum.

### Request

```http
GET /dams/accessible
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

### Response 200

```json
{
  "success": true,
  "message": "Barragens acessíveis obtidas com sucesso!",
  "data": [
    {
      "damId": 1,
      "damName": "PCH Exemplo",
      "status": "ACTIVE",
      "clientId": 1,
      "clientName": "Geometrisa",
      "city": "Ilha Solteira",
      "state": "São Paulo",
      "latitude": -20.427013,
      "longitude": -51.345806
    }
  ]
}
```

### Campos

| campo | tipo | nulo? | significado |
|---|---|---|---|
| `damId` | long | não | id da barragem |
| `damName` | string | não | nome |
| `status` | string | não | `ACTIVE` ou `DISABLED` |
| `clientId` | long | não | id do cliente dono |
| `clientName` | string | não | nome do cliente |
| `city` | string | **sim** | nulo quando não cadastrada |
| `state` | string | **sim** | nome por extenso (`"São Paulo"`), **não** sigla |
| `latitude` | double | **sim** | nulo quando não cadastrada |
| `longitude` | double | **sim** | nulo quando não cadastrada |

> ⚠️ `state` vem por extenso, como está no cadastro. Se vocês esperavam `"SP"`,
> a conversão fica com o app.

### Erros

| status | errorCode | quando |
|---|---|---|
| 401 | `NOT_AUTHENTICATED` / `SESSION_EXPIRED` / `INVALID_TOKEN` | token ausente, vencido ou inválido |

**Lista vazia:** `200` com `"data": []`. Usuário sem nenhuma barragem permitida
**não** recebe 403 — recebe lista vazia.

### Performance

Uma consulta nativa, 9 colunas escalares, sem carregar o grafo da `DamEntity`.
Substitui o padrão atual de baixar a lista completa por cliente e filtrar no app.

---

## 4. `GET /me/permissions`  · P2.1

**O que faz:** tudo que o app precisa saber sobre o próprio acesso, numa chamada.
**Auth:** Bearer. Derivado do token.

### Parâmetros

Nenhum.

### Request

```http
GET /me/permissions
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

### Response 200

```json
{
  "success": true,
  "message": "Permissões do usuário obtidas com sucesso!",
  "data": {
    "userId": 34,
    "role": "COLLABORATOR",
    "routineInspection": {
      "isFillMobile": true,
      "isFillWeb": false
    },
    "dams": [
      { "damId": 1, "damName": "PCH Exemplo", "clientId": 1, "hasAccess": true },
      { "damId": 11, "damName": "Dique Auxiliar", "clientId": 1, "hasAccess": false }
    ],
    "updatedAt": "2026-08-26T19:47:54.538455"
  }
}
```

### Campos

| campo | tipo | nulo? | significado |
|---|---|---|---|
| `userId` | long | não | id do usuário do token |
| `role` | string | **sim** | `ADMIN`, `COLLABORATOR`… Nulo se o usuário não tiver papel |
| `routineInspection.isFillMobile` | boolean | não | pode preencher checklist **no app** |
| `routineInspection.isFillWeb` | boolean | não | pode preencher na web |
| `dams[].damId` | long | não | |
| `dams[].damName` | string | não | |
| `dams[].clientId` | long | **sim** | nulo se a permissão não apontar cliente |
| `dams[].hasAccess` | boolean | não | |
| `updatedAt` | datetime | **sim** | ver abaixo |

**Duas coisas importantes sobre `dams`:**

1. A lista traz **também as barragens com `hasAccess: false`**, como vocês
   pediram — para mostrar a barragem bloqueada com o motivo em vez de sumir
   com ela.
2. Ela lista as barragens que **têm registro de permissão** para esse usuário.
   Barragem que nunca teve permissão criada não aparece em lugar nenhum. Para
   "as que eu posso ver", usem `/dams/accessible` ou filtrem por
   `hasAccess == true`.

**Sobre `routineInspection` quando não há cadastro:** vem
`{"isFillMobile": false, "isFillWeb": false}`, não `null`. Ausência de
permissão é tratada como negação.

**Sobre `updatedAt`:** é a data de alteração mais recente **entre as permissões
de barragem** (`updated_at`, ou `created_at` quando aquela é nula). É `null`
quando o usuário não tem nenhuma permissão de barragem registrada. A permissão
de inspeção de rotina **não tem carimbo de alteração** no banco, então mudanças
só nela não movem esse campo — vale para o "conferidas há X dias", não como
garantia forte.

### Erros

| status | errorCode | quando |
|---|---|---|
| 401 | `NOT_AUTHENTICATED` / `SESSION_EXPIRED` / `INVALID_TOKEN` | token ausente, vencido ou inválido |
| 404 | — | usuário do token não existe mais no banco |

### Performance

Uma consulta com `EntityGraph` (`clients`, `damPermissions`,
`damPermissions.dam`, `damPermissions.client`, `routineInspection`). Não
serializa `documentationPermission`, `attributionsPermission` nem
`instrumentationPermission`.

---

## 5. `GET /user-permissions/verify-checklists`  · P2.2

**O que faz:** verifica, de uma vez, os checklists de uma barragem.
**Auth:** Bearer.

> ⚠️ **Leiam isto antes de construir a tela.** No modelo atual, **cada barragem
> tem no máximo UM checklist** (`DamEntity.checklist` é `@OneToOne`). O `N×M`
> da seção P2.2 é, na prática, `N×1`. Esta rota economiza a viagem por
> barragem, não uma matriz. Se a barragem não tiver checklist, `checklists`
> vem `[]`.

### Parâmetros

| nome | tipo | obrigatório | padrão | observação |
|---|---|---|---|---|
| `userId` | long | **sim** | — | mantido por simetria com a rota unitária |
| `clientId` | long | **sim** | — | |
| `damId` | long | **sim** | — | |
| `isMobile` | boolean | **sim** | — | `true` avalia `isFillMobile`; `false`, `isFillWeb` |

Parâmetro ausente → `400`. Tipo inválido (ex.: `damId=abc`) → `400`.

### Request

```http
GET /user-permissions/verify-checklists?userId=34&clientId=1&damId=1&isMobile=true
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

### Response 200 — autorizado

```json
{
  "success": true,
  "message": "Permissões de checklist da barragem obtidas com sucesso!",
  "data": {
    "damId": 1,
    "checklists": [
      {
        "checklistId": 10,
        "checklistName": "Inspeção de Rotina",
        "allowed": true,
        "reasonCode": null,
        "reason": null
      }
    ]
  }
}
```

### Response 200 — negado

Negação **não** é 403 nesta rota: ela existe para dizer o que pode e o que não
pode, então a resposta é `200` com `allowed: false`.

```json
{
  "success": true,
  "message": "Permissões de checklist da barragem obtidas com sucesso!",
  "data": {
    "damId": 1,
    "checklists": [
      {
        "checklistId": 10,
        "checklistName": "Inspeção de Rotina",
        "allowed": false,
        "reasonCode": "NO_MOBILE_FILL",
        "reason": "O usuário não tem permissão para preencher checklists no aplicativo móvel"
      }
    ]
  }
}
```

### Campos

| campo | tipo | nulo? | significado |
|---|---|---|---|
| `damId` | long | não | eco do parâmetro |
| `checklists` | array | não | vazio se a barragem não tem checklist |
| `checklists[].checklistId` | long | não | |
| `checklists[].checklistName` | string | **sim** | nome cadastrado |
| `checklists[].allowed` | boolean | não | |
| `checklists[].reasonCode` | string | **sim** | `null` quando `allowed: true` |
| `checklists[].reason` | string | **sim** | `null` quando `allowed: true` |

### Códigos de motivo

Os 5 motivos do serviço foram preservados com a granularidade que vocês
pediram. **Ajam pelo código, exibam o texto de vocês** — o `reason` pode ser
reescrito, o `reasonCode` não.

| `reasonCode` | significado | o que dizer ao inspetor |
|---|---|---|
| `NOT_IN_CLIENT` | usuário não está associado ao cliente | problema de cadastro; falar com o gestor |
| `NO_DAM_ACCESS` | sem `DamPermission` para esta barragem | pedir liberação da barragem |
| `CHECKLIST_NOT_IN_DAM` | o checklist não pertence à barragem | erro de navegação do app |
| `NO_ROUTINE_PERMISSION` | sem cadastro de permissão de inspeção | pedir cadastro |
| `NO_MOBILE_FILL` | tem cadastro, mas `isFillMobile = false` | pedir liberação para o app |
| `NO_WEB_FILL` | tem cadastro, mas `isFillWeb = false` | só ocorre com `isMobile=false` |
| `UNKNOWN` | motivo não reconhecido | tratem como negação genérica |

`ADMIN` sempre recebe `allowed: true`.

### Erros

| status | errorCode | quando |
|---|---|---|
| 400 | — | parâmetro obrigatório ausente ou de tipo inválido |
| 401 | `NOT_AUTHENTICATED` / `SESSION_EXPIRED` / `INVALID_TOKEN` | token |
| 404 | — | `userId`, `clientId` ou `damId` inexistente |

---

## 6. `GET /anomalies/filter`  · P3.4

**O que faz:** listagem de anomalias filtrada e paginada, recortada pelas
barragens permitidas.
**Auth:** Bearer.
**Regra:** `damIds` ausente ⇒ **todas as acessíveis**. `damIds` preenchido ⇒
**intersecta** com as acessíveis; as não permitidas são **ignoradas em
silêncio**, sem 403.

> A rota `GET /anomalies` existente não foi tocada e continua devolvendo todas,
> sem parâmetros, como vocês pediram.

### Parâmetros

| nome | tipo | obrigatório | padrão | observação |
|---|---|---|---|---|
| `damIds` | lista de long | não | todas as acessíveis | `?damIds=1,3` ou `?damIds=1&damIds=3` |
| `statusId` | long | não | todos | id de `anomaly_status` (1–5) |
| `startDate` | datetime ISO | não | sem limite inferior | compara com `createdAt` (`>=`) |
| `endDate` | datetime ISO | não | sem limite superior | compara com `createdAt` (`<=`) |
| `page` | int | não | `0` | base zero |
| `size` | int | não | `20` | **sem teto imposto** — ver abaixo |

**Sobre `size`:** não há validação de máximo. `size=10000` será aceito e
tentará montar 10 000 itens. Pedimos que o app fique em 20–50. Se preferirem
que o backend imponha o teto, digam e a gente adiciona.

Data em formato inválido → `400`. `statusId` inexistente → `200` com lista
vazia, não erro.

### Request

```http
GET /anomalies/filter?damIds=1&statusId=1&startDate=2026-01-01T00:00:00&page=0&size=20
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

### Response 200

```json
{
  "success": true,
  "message": "Anomalias obtidas com sucesso!",
  "data": {
    "content": [
      {
        "id": 42,
        "createdAt": "2026-07-02T09:36:02.530351",
        "damId": 1,
        "damName": "PCH Exemplo",
        "userId": 34,
        "userName": "João Inspetor",
        "latitude": -20.427013,
        "longitude": -51.345806,
        "origin": "CHECKLIST",
        "observation": "Trinca longitudinal no talude de jusante",
        "recommendation": "Monitorar semanalmente",
        "dangerLevelId": 2,
        "dangerLevelName": "Atenção",
        "statusId": 1,
        "statusName": "Pendente",
        "questionnaireId": 7,
        "questionId": 103,
        "photoPaths": [
          "https://storage.exemplo/anomalies/42/foto-1.jpg"
        ]
      }
    ],
    "pageNumber": 0,
    "pageSize": 20,
    "totalElements": 1,
    "totalPages": 1,
    "last": true,
    "first": true
  }
}
```

### Campos

| campo | tipo | nulo? | significado |
|---|---|---|---|
| `content[].id` | long | não | |
| `content[].createdAt` | datetime | não | quando a anomalia foi registrada |
| `content[].damId` / `damName` | long / string | não | |
| `content[].userId` / `userName` | long / string | não | quem registrou |
| `content[].latitude` / `longitude` | double | não | obrigatórias no cadastro |
| `content[].origin` | string | não | `CHECKLIST`, `WEB` ou `OTHER` |
| `content[].observation` | string | **sim** | texto livre |
| `content[].recommendation` | string | **sim** | texto livre |
| `content[].dangerLevelId` / `dangerLevelName` | long / string | não | |
| `content[].statusId` / `statusName` | long / string | não | ver 0.3 |
| `content[].questionnaireId` | long | **sim** | nulo quando origem não é checklist |
| `content[].questionId` | long | **sim** | idem |
| `content[].photoPaths` | array de string | não | **`[]`** quando não há fotos, nunca `null` |
| `pageNumber` | int | não | eco, base zero |
| `pageSize` | int | não | eco |
| `totalElements` | long | não | total no filtro, não na página |
| `totalPages` | int | não | |
| `last` / `first` | boolean | não | |

> Esta rota devolve um **DTO enxuto**, não a `AnomalyEntity`. Não vem o grafo
> de usuário, cliente e barragem que as rotas antigas de anomalia arrastam.

### Erros

| status | errorCode | quando |
|---|---|---|
| 400 | — | data em formato inválido, `page`/`size` não numéricos |
| 401 | `NOT_AUTHENTICATED` / `SESSION_EXPIRED` / `INVALID_TOKEN` | token |

**Nenhuma barragem permitida no escopo:** `200` com `content: []`,
`totalElements: 0`, `totalPages: 0`, `last: true`, `first: true`. **Nunca 403**
— nem quando todos os `damIds` pedidos são inaccessíveis.

### Performance

Duas consultas por página, independentemente do tamanho: a página das anomalias
(com as associações num `EntityGraph`) e as fotos em lote para os ids da
página. As fotos ficam fora do `EntityGraph` de propósito — coleção somada a
paginação faria o Hibernate paginar em memória.

---

## 7. `GET /checklist-responses/client/{clientId}/last-checklist/v2`  · P4.1

**O que faz:** data da última inspeção de cada barragem do cliente, sem
sentinela de texto no campo de data.
**Auth:** Bearer.

> **A v1 continua no ar, intacta e sem prazo para morrer.** Migrem quando
> quiserem. A web segue na v1.

### Parâmetros

| nome | tipo | obrigatório | padrão | observação |
|---|---|---|---|---|
| `clientId` | long | **sim** | — | no path |

### Request

```http
GET /checklist-responses/client/1/last-checklist/v2
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

### Response 200

```json
{
  "success": true,
  "message": "Última inspeção de cada barragem do cliente obtida com sucesso!",
  "data": [
    {
      "damId": 1,
      "damName": "PCH Exemplo",
      "lastChecklistDate": "2026-07-02T09:36:02.530351"
    },
    {
      "damId": 7,
      "damName": "Dique Auxiliar",
      "lastChecklistDate": null
    }
  ]
}
```

### Campos

| campo | tipo | nulo? | significado |
|---|---|---|---|
| `damId` | long | não | |
| `damName` | string | não | |
| `lastChecklistDate` | datetime | **sim** | **`null` = nunca inspecionada.** É a diferença para a v1 |

### O que mudou em relação à v1

| | v1 | v2 |
|---|---|---|
| tipo de `lastChecklistDate` | `String` `"yyyy-MM-dd HH:mm:ss"` | `LocalDateTime` ISO-8601 |
| nunca inspecionada | texto `"Nenhuma inspeção realizada."` **no mesmo campo** | `null` |
| consultas | uma por barragem, carregando todas as respostas em memória | **uma** consulta agregada |

### Erros

| status | errorCode | quando |
|---|---|---|
| 401 | `NOT_AUTHENTICATED` / `SESSION_EXPIRED` / `INVALID_TOKEN` | token |

**Cliente sem barragens:** `200` com `"data": []`.

> ⚠️ **Esta rota não filtra por permissão de barragem** — devolve todas as
> barragens do cliente, igual à v1. Foi mantida assim de propósito: é a v2 de
> uma rota existente, não uma rota de escopo. Se o app precisar do recorte,
> cruzem com `/dams/accessible`, ou peçam e a gente cria a variante.

---

## 8. O que NÃO foi implementado, e por quê

Tão importante quanto o que foi — para vocês não construírem esperando algo que
não vem.

| item | situação | motivo |
|---|---|---|
| **4.1** `/mobile/offline-package` | **não feito** | ver 8.1 — depende de um conserto anterior |
| **4.2 / P3.1** `/dashboard/summary` | **não feito** | próximo da fila; a peça de permissão que ele precisa já existe |
| **P3.2** `groupBy=month` | **não feito** | faz parte do `/dashboard/summary` |
| **4.3** `updatedSince` / ETag | **não feito** | exige `updatedAt` confiável em entidades que não têm. É mudança de modelo, não de rota — precisa de decisão |
| **P1.1** rotas `/accessible/**` | **não feito** | seria a mesma superfície que a 4.1, duas vezes. Se preferirem garantir as duas, digam |
| **P3.3** `isOpen` | **revertido** | ver 0.3 |

### 8.1 Por que o pacote offline ainda não veio

Não é falta de tempo. O `lastSelectedOption`, que vocês corretamente apontam
como essencial para as regras de transição, é montado hoje com **uma consulta
por pergunta**:

```java
for (TemplateQuestionnaireQuestionEntity tqQuestion : template.getTemplateQuestions()) {
    answerRepository.findLatestNonNIAnswer(damId, question.getId(), template.getId());
}
```

**O maior checklist em produção tem 266 perguntas** — 266 consultas, cada uma
com subconsulta correlacionada. Um pacote de 12 barragens multiplica isso por
todos os checklists de todas elas.

Consolidar 29 requisições em 1 **sem consertar isso antes** troca 29 chamadas
rápidas por uma chamada muito lenta. Em rede móvel instável, uma requisição
longa falha mais do que várias curtas — o ganho viraria prejuízo, e vocês
teriam trocado um problema conhecido por um pior.

A ordem que vamos seguir:

1. consertar o N+1 do `lastSelectedOption` (consulta em lote)
2. `/dashboard/summary` (4.2 / P3.1 / P3.2)
3. `/mobile/offline-package` (4.1)

Avisamos a cada etapa.

---

## 9. Perguntas para vocês

1. **Sigla de barragem** — deixamos de fora (0.2). Precisam dela de verdade?
2. **`size` sem teto** em `/anomalies/filter` — querem que o backend imponha
   máximo, ou confiam no app?
3. **`last-checklist/v2` sem filtro de permissão** — serve assim, ou precisam
   da variante recortada?
4. **P1.1** — pulamos mesmo em favor da 4.1, ou querem as duas?
5. **4.3** — vale a gente levantar o custo de dar `updatedAt` às entidades?
