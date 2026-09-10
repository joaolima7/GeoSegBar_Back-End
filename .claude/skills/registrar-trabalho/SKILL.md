---
name: registrar-trabalho
description: >
  Registra no portal MySpace da Somos Dev's o que está sendo feito neste
  repositório — cria ou avança um chamado simples (título + contexto, sem
  detalhe técnico) para o funcionário de IA Alex usar no relatório diário do
  cliente. Use isto sempre que uma tarefa for concluída ou avançar de forma
  relevante, tipicamente antes de dar commit/push, ou quando o dev disser que
  terminou algo. Um chamado por tarefa — chamadas seguintes na MESMA branch
  atualizam o mesmo card em vez de criar outro. Na primeira vez que rodar neste
  repositório, pergunta uma única vez qual cliente e produto ele atende, e
  guarda a resposta. Triggers: "vou commitar", "vou subir isso", "terminei essa
  tarefa", "pronto pra dar push", "registra isso no myspace", "abre um card do
  que eu fiz", "finalizei a implementação", "marca como resolvido".
---

# Registrar Trabalho — MySpace / Somos Dev's

Este skill é **genérico e portátil**: o mesmo arquivo funciona em qualquer
repositório da casa (GeoSegBar Web, GeoSegBar Api, GeoSegBar Mobile, PixPay
App, MySpace, ou o próximo projeto que a Somos Dev's pegar). Ele não sabe, de
fábrica, de qual cliente é o repositório onde está rodando — a primeira seção
resolve isso uma vez, e a partir daí ele já sabe sozinho.

**Por que isto existe:** o funcionário de IA Alex monta, todo dia, um
relatório por cliente a partir do board de chamados do MySpace — e dentro
desse relatório ele detalha o que foi feito **por produto** (ex.: a PixPay tem
Web e Mobile; a Geometrisa tem GeoSegBar Web, Api e Mobile). Sem um chamado
registrado aqui, o Alex não tem o que relatar. Este skill é a ponte entre
"o dev terminou uma tarefa neste repositório" e "o Alex sabe que ela
aconteceu".

🔴 **Isto não é o `abre-chamados`** (que existe só dentro do repositório
`somos-devs`, com acesso `service_role` completo, e serve para PEDIR trabalho
ao runner ou a um humano). Este skill faz o oposto: REGISTRA trabalho que já
foi ou está sendo feito, rodando em qualquer repositório, sem nenhuma
credencial de administrador — só uma API key escopada a um cliente e um
produto só.

---

## 1. Configuração — roda uma vez por repositório

### 1.1 A identidade do repositório: cliente e produto

Antes de fazer qualquer outra coisa, confira se `.claude/portal-link.json`
existe na raiz deste repositório.

**Se não existir:** pergunte ao dev, em uma frase, algo como:

> "Ainda não sei para qual cliente e produto este repositório é. Qual cliente
> (ex.: PixPay, Geometrisa) e qual produto (ex.: GeoSegBar Web, GeoSegBar Api,
> GeoSegBar Mobile)? E qual é o slug do produto no MySpace (minúsculo,
> hífen — ex.: `geosegbar-web`)? Se não souber o slug de cor, me diga só o
> nome que eu confirmo com você depois de ver a mensagem de erro da API."

`product_slug` **precisa ser exato** — 🔴 desde que a chave passou a poder
cobrir um cliente inteiro (Seção 1.2), o produto às vezes é resolvido a partir
deste slug, mandado no corpo da requisição. Slug errado não vaza nada (a API
recusa com 400, nunca cria em cliente ou produto errado — ver Seção 1.2), mas
também não funciona: se a primeira chamada dessa nova configuração voltar com
"Produto não encontrado para este cliente", é sinal de slug errado — pergunte
de novo ao dev, ou peça para um master conferir em `products.slug` no MySpace.

Depois de obter a resposta, grave:

```json
{
  "organization_name": "Geometrisa",
  "product_name": "GeoSegBar Web",
  "product_slug": "geosegbar-web",
  "repo": "geosegbar-web"
}
```

`repo` é o nome curto do repositório — normalmente o nome da pasta, ou o nome
do repositório no GitHub. Use `git remote get-url origin` se ele existir, senão
o nome da pasta atual.

Este arquivo **não tem segredo nenhum** — pode e deve ser commitado junto com
o código, para que qualquer sessão futura (sua ou de outro dev) já saiba a
resposta sem perguntar de novo.

**Se já existir:** leia e siga em frente, sem perguntar nada.

### 1.2 A credencial — uma API key por CLIENTE, reaproveitada entre os repositórios dele

🔴 A chave **não** é por repositório — é por cliente. A Geometrisa, por
exemplo, tem GeoSegBar Web, Api e Mobile em três repositórios diferentes; os
três usam a **mesma** chave. Ela só enxerga chamados **daquele cliente**
(nunca de outro), mas dentro dele cobre todos os produtos — é por isso que
toda chamada à API leva o `product_slug` no corpo (Seção 2.4): é o jeito de
dizer, dentro do cliente já autorizado pela chave, qual produto é este.

Confira se as variáveis abaixo estão disponíveis, lendo `.env.somosdevs` na
raiz do repositório — um arquivo **dedicado**, separado do `.env.local` que o
próprio projeto já usa para as credenciais dele (nunca leia de outro lugar, e
nunca imprima o valor de `SOMOSDEVS_API_KEY` no terminal ou no chat):

```
SOMOSDEVS_API_URL=https://myspace.somosdevs.com
SOMOSDEVS_API_KEY=sdv_...
```

Um `.env.somosdevs.example` (sem segredo, este sim commitável) acompanha este
skill — copie para `.env.somosdevs` e preencha.

**Se `SOMOSDEVS_API_KEY` estiver ausente**, pare e explique ao dev, com estas
palavras:

> "Este repositório ainda não tem a chave de acesso ao MySpace. Se **outro
> repositório deste MESMO cliente já tem uma** (confira com o time — a mesma
> chave serve para todos os produtos dele), é só copiar o `.env.somosdevs`
> de lá para cá. Senão, peça para o João ou o Gabriel emitirem uma nova,
> rodando no repositório `somos-devs`, com `.env.local` carregado:
>
> ```bash
> node --env-file=.env.local scripts/seed-commit-bot.mjs list-orgs
> # o "-" no lugar do product_id é o que faz a chave cobrir TODOS os
> # produtos deste cliente, não um só:
> node --env-file=.env.local scripts/seed-commit-bot.mjs create-key <organization_id> - "<Nome do cliente> — registro de trabalho"
> ```
>
> O comando `create-key` imprime a chave **uma única vez**. Cole-a em
> `.env.somosdevs` (que precisa estar no `.gitignore` deste repositório —
> confira, e avise se não estiver) como: `SOMOSDEVS_API_KEY=sdv_...` e
> `SOMOSDEVS_API_URL=https://myspace.somosdevs.com`."

Depois disso, não tente criar o card sem a chave — só siga quando ela existir.
Não há forma de este skill se auto-provisionar: a emissão da chave exige
`service_role`, que só existe dentro do repositório `somos-devs`, e é assim de
propósito — cada chave só enxerga o cliente para o qual foi emitida.

---

## 2. O ciclo normal de uso

Depois que a configuração acima existe, o uso do dia a dia é direto.

### 2.1 Quando acionar

- Sempre que o dev indicar que uma tarefa foi concluída ou avançou de forma
  relevante — "terminei", "vou commitar", "pronto pra subir", "resolvi aquele
  bug".
- **Não precisa de um card por detalhe.** Se o dev está no meio de uma tarefa
  grande e vai continuar na mesma branch, uma chamada de atualização (Passo
  2.3) basta — não crie um segundo card para a mesma coisa.
- Se surgir uma tarefa **claramente diferente** (outro bug, outra
  funcionalidade), aí sim é um card novo — normalmente isso significa uma
  branch nova também.

### 2.2 Uma pergunta só, natural

Não interrogue o dev com um formulário. Uma pergunta cobre tudo:

> "O que foi feito? (título curto + contexto — sem precisar de detalhe
> técnico) E já terminou essa parte, ou ainda está em andamento?"

Da resposta, você extrai:

- **título** — curto, direto, em linguagem de negócio. "Corrige o filtro de
  período que ignorava o fuso horário" é bom; "fix bug in DateFilter.tsx" não
  é — quem lê isso depois é o Alex, montando relatório pro cliente, não outro
  dev.
- **contexto** — 2 a 4 frases: o que motivou, o que mudou, para quem importa.
  Sem nome de arquivo, sem stack técnica, sem jargão de código. Pense em como
  você explicaria para alguém que não programa.
- **está pronto, ou em andamento** — decide o `status` do Passo 2.4.
- **tipo da tarefa** — infira, não pergunte: corrigiu algo que já existia e
  estava quebrado → `bug`; construiu algo novo → `feature`; ajudou/orientou/
  configurou algo pontual → `support`; não se encaixa → `other`.

### 2.3 A branch é a chave do card — não invente outra

```bash
git branch --show-current
```

Use exatamente este valor como `branch` no corpo da requisição (Passo 2.4). A
API do MySpace faz **upsert pelo chamado mais recente desta branch, dentro do
cliente+produto da chave — independente do status atual dele**: se existir um,
ele é **atualizado** (o status avança, a descrição mais recente entra); se não
existir nenhum, um chamado novo é **criado**. Isto sozinho garante "um card
por tarefa" sem você precisar guardar nada — a mesma branch, chamada de novo,
nunca duplica.

⚠️ Justamente por ser "o mais recente da branch, seja qual for o status": se
uma branch antiga (de uma tarefa já `resolved` há semanas) for reaproveitada
para uma tarefa **nova e sem relação**, a próxima chamada atualizaria aquele
card antigo em vez de criar um novo — reabrindo, na prática, um chamado que já
tinha sido fechado. Branch reaproveitada para tarefa nova pede nome de branch
novo.

Se o repositório não tiver branch (ex.: script batido direto na `main`), use
como `branch` uma frase curta e estável que identifique a tarefa, em
kebab-case: `ajuste-filtro-periodo-2026-09-10`.

### 2.4 Criar ou avançar o chamado

```bash
set -a; [ -f .env.somosdevs ] && . ./.env.somosdevs; set +a

curl -s -X POST "${SOMOSDEVS_API_URL}/api/external/tickets" \
  -H "Authorization: Bearer ${SOMOSDEVS_API_KEY}" \
  -H "Content-Type: application/json" \
  -d @- <<JSON
{
  "branch": "$(git branch --show-current)",
  "repo": "<repo do .claude/portal-link.json>",
  "product": "<product_slug do .claude/portal-link.json>",
  "title": "<título curto>",
  "description": "<contexto em 2-4 frases>",
  "type": "bug|feature|support|other",
  "priority": "low|medium|high|critical",
  "status": "in_progress|resolved"
}
JSON
```

🔴 **Sempre mande `product`**, mesmo sem saber se a chave deste repositório é
de cliente inteiro ou de produto único — o servidor ignora o campo quando a
chave já é de produto único, e o exige quando é de cliente inteiro. Mandar
sempre elimina a necessidade de o skill saber qual dos dois tipos de chave
está configurado.

Valores possíveis (são exatamente estes slugs, nada além):
- `type`: `bug` · `feature` · `support` · `other`
- `priority`: `low` · `medium` · `high` · `critical` — na dúvida, `medium`
- `status`: em andamento → `in_progress`; terminado de verdade → `resolved`.
  **Nunca** mande `cancelled` nem `unresolved` por este caminho — essas duas
  situações são decisão de quem acompanha o chamado na tela, não do dev que
  está codando.

Se o dev não deu prioridade nenhuma pista sobre urgência, omita o campo
`priority` — o servidor usa `medium` como padrão.

Se a resposta vier `400` com "Produto não encontrado para este cliente", o
`product_slug` gravado em `.claude/portal-link.json` está errado — confira o
slug certo com um master e corrija o arquivo (Seção 1.1). Isto **nunca** cria
o card em cliente ou produto errado — a API recusa antes.

A resposta traz `{ ok, id, status, created, attributed_to }`. Confira `ok` e o
`status` batendo com o que você mandou.

### 2.5 Reporte ao dev, curto

> "Registrado no MySpace: **{título}** — {'card novo' se created, senão
> 'chamado atualizado'}, status {status}.
> https://myspace.somosdevs.com/portal/app/chamados/{id}"

Se a chamada falhar (401, 400, 500), **diga exatamente o que falhou** — nunca
finja que registrou algo que não registrou. 401 quase sempre significa
`SOMOSDEVS_API_KEY` errada ou revogada; volte ao Passo 1.2.

---

## 3. O que este skill nunca faz

- **Nunca inventa cliente ou produto** — se `.claude/portal-link.json` não
  existir, pergunta; nunca adivinha pelo nome da pasta ou do remote.
- **Nunca guarda a API key em texto commitado, e nunca a embute no próprio
  arquivo deste skill.** Ela mora só em `.env.somosdevs` (gitignored). Se
  `.env.somosdevs` não estiver no `.gitignore` deste repositório, avise o dev
  antes de continuar.
- **Nunca move um chamado para `cancelled`/`unresolved`.** Essas são decisões
  de quem gerencia o board, tomadas na tela — este skill só avança o trabalho
  normal (`in_progress` → `resolved`).
- **Nunca cria um card por commit ou por arquivo alterado.** A unidade é a
  tarefa (a branch), não o commit.
- **Nunca expõe `SOMOSDEVS_API_KEY`** em log, commit, comentário de código ou
  mensagem para o usuário.

---

## 4. Referência rápida — os arquivos que este skill usa

| Arquivo | Onde | Segredo? | O que guarda |
|---|---|---|---|
| `.claude/portal-link.json` | raiz do repo, **commitado** | Não | cliente, produto, `product_slug`, nome do repo |
| `.env.somosdevs` | raiz do repo, **gitignored** | Sim | `SOMOSDEVS_API_URL`, `SOMOSDEVS_API_KEY` |
| `.env.somosdevs.example` | raiz do repo, **commitado** | Não | os mesmos dois nomes, sem valor — molde para o próximo dev |

**A chave é por cliente, não por repositório.** Todo repositório do mesmo
cliente (ex.: os três da Geometrisa) usa o mesmo `.env.somosdevs` — copiar o
arquivo de um repositório do cliente para outro do mesmo cliente já resolve a
Seção 1.2 inteira, sem precisar emitir chave nova. Só a Seção 1.1
(`.claude/portal-link.json`, com o `product_slug` certo deste repositório
específico) precisa ser respondida de novo em cada repositório — porque é ela
que diz qual produto, dentro do cliente, é este.
