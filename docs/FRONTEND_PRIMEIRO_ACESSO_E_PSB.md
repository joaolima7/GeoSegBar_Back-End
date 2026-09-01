# Ajustes de back-end para o front implementar

Referência de contrato para três mudanças entregues no back-end. Envelope padrão de
todas as respostas (sucesso e erro):

```json
{ "success": true, "message": "texto para exibir", "data": { } }
```

Em erro: `success: false`, `data: null` e `message` com o texto pronto para mostrar ao usuário.

---

## 1. PSB — pastas raízes não podem mais ser excluídas

### O que mudou

`DELETE /psb/folders/{id}` passa a recusar a exclusão quando a pasta é raiz, ou seja,
quando `parentFolder` é `null`. Subpastas e arquivos continuam podendo ser excluídos
normalmente. Pastas raízes seguem sendo gerenciadas apenas pela configuração da barragem
(o payload `psbFolders` do update de barragem).

### Resposta nova

| Situação | Status | `message` |
| --- | --- | --- |
| Pasta é raiz | `400` | `Pastas raízes do PSB não podem ser excluídas. Apenas subpastas e arquivos podem ser removidos.` |

### O que o front precisa fazer

- Esconder ou desabilitar a ação de excluir nas pastas de primeiro nível da árvore do PSB
  (as que vêm com `parentFolder: null` em `GET /psb/folders/dam/{damId}/complete`).
- Manter o tratamento genérico de `400` exibindo `message`, para o caso de a chamada
  acontecer mesmo assim.

---

## 2. Troca de senha no primeiro acesso — sem pedir a senha atual

### O que mudou

No primeiro acesso o usuário está trocando uma senha que ele nunca escolheu, então pedir
a "senha atual" não protege nada. O campo `currentPassword` de `PUT /user/{id}/password`
passou a ser **opcional** nesse cenário específico.

A dispensa vale apenas quando **as duas condições** são verdadeiras:

1. O usuário autenticado está alterando a **própria** senha (`id` da rota == `id` do usuário logado); e
2. Esse usuário está com `isFirstAccess: true`.

Em qualquer outro caso `currentPassword` continua obrigatório — inclusive para um
administrador alterando a senha de outra pessoa. (Para esse caso existe
`PUT /user/{id}/reset-password`, ver seção 3.)

### Endpoint

```
PUT /user/{id}/password
Authorization: Bearer <token>
```

**Body no primeiro acesso** (não enviar `currentPassword`, ou enviar `null`):

```json
{ "newPassword": "novaSenha123" }
```

**Body na troca comum:**

```json
{ "currentPassword": "senhaAtual", "newPassword": "novaSenha123" }
```

**Sucesso — `200`:** `data` traz o usuário atualizado, já com `isFirstAccess: false`.

### Erros

| Status | `message` | Quando |
| --- | --- | --- |
| `400` | `A senha atual é obrigatória!` | `currentPassword` ausente/vazio fora do fluxo de primeiro acesso |
| `400` | `Senha atual incorreta!` | `currentPassword` não confere |
| `400` | `A nova senha deve ser diferente da senha atual!` | nova senha igual à atual (vale também no primeiro acesso) |
| `400` | `A nova senha deve ter pelo menos 6 caracteres!` | `newPassword` com menos de 6 caracteres |
| `403` | Usuário não tem permissão para atualizar a senha de outros usuários! | usuário sem permissão para alterar a senha de outra pessoa |

### O que o front precisa fazer

- Na tela de troca obrigatória de senha (a que aparece quando o login retorna
  `isFirstAccess: true`), **remover o campo "senha atual"** e enviar apenas `newPassword`.
- Manter o campo "senha atual" na tela de troca de senha do perfil (usuário já ativo).
- `isFirstAccess` continua vindo em `LoginResponseDTO`, na resposta de
  `POST /user/login/initiate` e `POST /user/login/verify`.

> Esta tela continua necessária para usuários antigos, que ainda estão com
> `isFirstAccess: true` de senhas temporárias emitidas antes da mudança 3.
> Usuários novos chegam ao login já com `isFirstAccess: false`, porque definem a
> senha pelo link antes do primeiro login.

---

## 3. E-mail de primeiro acesso — link em vez de senha em texto plano

### Por que mudou

O e-mail de boas-vindas enviava a senha temporária no corpo da mensagem. Mensagem com
credencial em texto plano é exatamente o padrão que filtros de anti-phishing
(Microsoft Defender, entre outros) classificam como comprometimento de credencial —
por isso ficava em quarentena mesmo com SPF/DKIM/DMARC corretos. O assunto anterior
("Bem-vindo ao GeoSegBar - Sua senha de acesso") reforçava o gatilho.

O sistema não envia mais senha nenhuma. O e-mail carrega só um link de uso único, e o
usuário define a própria senha dentro do sistema.

### Nova rota que o front precisa criar

```
/definir-senha?token=<token>
```

**Esta rota é obrigatória e o caminho é fixo.** O back-end monta o link como
`${FRONTEND_URL}/definir-senha?token=<token>`. Se a rota não existir, o link do e-mail
cai em 404 e ninguém consegue ativar o acesso.

A tela precisa ser **pública** — o usuário ainda não tem senha e portanto não está logado.
Não exigir token de autenticação nem redirecionar para o login.

### Fluxo da tela

**Passo 1 — ao abrir a rota, validar o token:**

```
GET /password-setup/validate?token=<token>
```

Sem `Authorization`.

Sucesso — `200`:

```json
{
  "success": true,
  "message": "Link válido!",
  "data": { "name": "Aline Sayuri", "email": "aline.sayuri@exemplo.com.br" }
}
```

Use `name` para cumprimentar e `email` para mostrar em qual conta a senha será definida.

**Passo 2 — enviar a senha escolhida:**

```
POST /password-setup/complete
Content-Type: application/json
```

```json
{ "token": "<token>", "newPassword": "senhaEscolhida123" }
```

Sucesso — `200`:

```json
{
  "success": true,
  "message": "Senha definida com sucesso! Você já pode entrar no sistema.",
  "data": null
}
```

Depois disso, redirecionar para o login. O usuário entra com o e-mail e a senha que acabou
de definir, e **não** cai na tela de troca obrigatória (`isFirstAccess` já vem `false`).

### Erros dos dois endpoints

| Status | `message` | Quando | Sugestão de tratamento |
| --- | --- | --- | --- |
| `401` | `Link de definição de senha inválido. Solicite um novo link ao administrador ou use 'Esqueci minha senha'.` | token não existe | Tela de erro com link para "Esqueci minha senha" |
| `401` | `Este link já foi utilizado. Se você não reconhece esse acesso, use 'Esqueci minha senha' ou contate o administrador.` | token já consumido | Mesma tela de erro |
| `401` | `Link de definição de senha expirado. Use 'Esqueci minha senha' ou solicite um novo link ao administrador.` | passou de 48 h | Mesma tela de erro |
| `401` | `Esta conta está desativada. Contate o administrador do sistema.` | usuário desativado | Tela de erro, sem oferecer recuperação |
| `400` | `Token de definição de senha é obrigatório!` | `token` vazio | Não deve acontecer se a rota validar o query param |
| `400` | `A nova senha deve ter pelo menos 6 caracteres!` | senha curta | Validação no formulário |
| `429` | — | rate limit por IP | Pedir para tentar de novo em instantes |

Em todos os casos de `401` a mensagem já vem pronta para exibição; não é necessário
traduzir status em texto no front.

### Regras do token

- Uso único: depois de definir a senha, o mesmo link não funciona mais.
- Validade de **48 horas**.
- Só existe um link válido por usuário: emitir um novo invalida o anterior.
- Se expirar, o usuário se resolve sozinho pelo fluxo já existente de
  `POST /user/forgot-password` (código por e-mail), ou o administrador emite um novo link
  em `PUT /user/{id}/reset-password`.

### Efeitos nas telas de administração

`POST /user/register` (criar usuário) e `PUT /user/{id}/reset-password` (redefinir senha)
mantêm o mesmo contrato de request e response. O que muda é o efeito e o texto:

- Nenhuma senha é gerada para ser mostrada ou enviada — o usuário recebe o link.
- `PUT /user/{id}/reset-password` agora responde com
  `"Um link para o usuário definir a própria senha foi enviado por e-mail. O acesso anterior foi invalidado."`
- Ajustar textos de confirmação e tooltips que digam "uma nova senha será enviada por e-mail"
  para "um link para o usuário definir a própria senha será enviado por e-mail".

### Resumo dos endpoints novos

| Método | Rota | Autenticação | Body / Query | Retorno |
| --- | --- | --- | --- | --- |
| `GET` | `/password-setup/validate` | pública | `?token=<token>` | `{ name, email }` |
| `POST` | `/password-setup/complete` | pública | `{ token, newPassword }` | `data: null` |

---

## 4. Tipo de instrumento passa a pertencer ao cliente

### O que mudou

Até agora o catálogo de tipos de instrumento era global: um único "PIEZÔMETRO" compartilhado
por todos os clientes, e renomeá-lo mudava o nome nas barragens de todo mundo.

Agora o tipo pertence a um cliente, no mesmo modelo já usado em Questão:

- Cada cliente tem o seu catálogo. Dois clientes podem ter tipos com o mesmo nome, sem se afetar.
- Uma barragem só pode usar tipos do cliente dono dela.
- Dentro do cliente o catálogo continua compartilhado — **renomear um tipo reflete em todas as
  barragens daquele cliente**. É o comportamento desejado; o que não acontece mais é a alteração
  atravessar para outro cliente.
- O CRUD ficou completo: agora existem `DELETE` e a listagem por cliente.

### Formato do tipo de instrumento

Todas as rotas de `/instrument-types` devolvem este objeto em `data`:

```json
{
  "id": 12,
  "name": "PIEZÔMETRO",
  "clientId": 3,
  "clientName": "Geometrisa",
  "instrumentsCount": 42,
  "damsCount": 5,
  "legacy": false
}
```

| Campo | Observação |
| --- | --- |
| `name` | Sempre gravado em MAIÚSCULAS; pode ser enviado em qualquer caixa |
| `clientId` | Obrigatório no `POST`. Ignorado no `PUT` — o cliente do tipo nunca muda |
| `clientName` | Somente leitura |
| `instrumentsCount` | Quantos instrumentos usam o tipo. `0` = pode excluir |
| `damsCount` | Em quantas barragens do cliente está em uso. Vem preenchido em `GET /{id}`, `POST` e `PUT`; vem `null` nas listagens, para não pesar a consulta |
| `legacy` | `true` = tipo anterior à separação por cliente, ainda sem dono. Somente leitura: não pode ser editado nem excluído |

### Endpoints

| Método | Rota | Situação | O que faz |
| --- | --- | --- | --- |
| `GET` | `/instrument-types` | alterado | Catálogo dos clientes a que o usuário tem acesso (admin vê tudo) |
| `GET` | `/instrument-types/client/{clientId}` | **novo** | Catálogo de um cliente — é este que alimenta o select de cadastro de instrumento |
| `GET` | `/instrument-types/{id}` | alterado | Agora traz `clientId`, `clientName` e os contadores |
| `POST` | `/instrument-types` | alterado | Passa a exigir `clientId` |
| `PUT` | `/instrument-types/{id}` | alterado | Só renomeia; não aceita troca de cliente |
| `DELETE` | `/instrument-types/{id}` | **novo** | Exclui, desde que nenhum instrumento use o tipo |

**Criar** — `POST /instrument-types`

```json
{ "name": "Piezômetro", "clientId": 3 }
```

**Renomear** — `PUT /instrument-types/{id}`

```json
{ "name": "Piezômetro Elétrico" }
```

### Erros

| Status | `message` | Quando |
| --- | --- | --- |
| `400` | O tipo de instrumento deve estar associado a um cliente! | `POST` sem `clientId` |
| `409` | Já existe um tipo de instrumento com o nome 'X' para o cliente Y. | Nome repetido **dentro do mesmo cliente** |
| `400` | Não é permitido mudar o cliente de um tipo de instrumento. Crie um novo tipo no cliente de destino. | `PUT` com `clientId` diferente do atual |
| `400` | Não é possível excluir o tipo 'X' pois ele está em uso por N instrumento(s) em M barragem(ns). Troque o tipo desses instrumentos antes de excluir. | `DELETE` de tipo em uso |
| `400` | Este tipo de instrumento é anterior à separação por cliente… | `PUT` ou `DELETE` de tipo com `legacy: true` |
| `400` | O tipo de instrumento 'X' pertence a outro cliente e não pode ser usado na barragem 'Y'. Selecione um tipo do cliente dono da barragem. | Cadastro/edição de instrumento com tipo de outro cliente |
| `403` | Usuário não tem acesso ao cliente informado! | Tentativa de ler ou alterar o catálogo de um cliente que não é do usuário |
| `403` | Usuário não tem permissão para gerenciar tipos de instrumento! | Sem `editInstruments` na permissão de instrumentação |
| `404` | Cliente não encontrado com ID: N | `clientId` inexistente |

Permissões usadas: leitura exige `instrumentationPermission.viewInstruments`; criar, editar e
excluir exigem `instrumentationPermission.editInstruments`. Admin passa por cima das duas.

### O que o front precisa fazer

1. **Tela de cadastro/edição de instrumento** — trocar a origem do select de tipo de
   `GET /instrument-types` para **`GET /instrument-types/client/{clientId}`**, usando o
   `clientId` da barragem em questão. Sem isso o usuário consegue escolher um tipo de outro
   cliente e o salvamento é recusado com `400`.
2. **Tela de CRUD de tipo de instrumento** — passar a enviar `clientId` no `POST`, e ligar o
   botão de excluir em `DELETE /instrument-types/{id}`. O `PUT` continua igual, só sem
   `clientId`.
3. **Desabilitar excluir** quando `instrumentsCount > 0`, e explicar no tooltip que o tipo está
   em uso. O back recusa de qualquer forma, mas o aviso evita o erro.
4. **Tratar `legacy: true` como somente leitura** — esconder editar e excluir e, se fizer
   sentido, sinalizar na listagem que o tipo aguarda migração.
5. **Avisar antes de renomear**: buscar `GET /instrument-types/{id}` para pegar `damsCount` e
   confirmar com algo como *"Este tipo é usado em N barragens deste cliente. A alteração vale
   para todas elas. Outros clientes não são afetados."* Foi exatamente o cuidado pedido no
   chamado.

### Comportamentos automáticos do back-end (não exigem tela)

- **Barragem que muda de cliente:** os instrumentos dela são reapontados automaticamente para os
  tipos equivalentes do cliente novo, criando o que faltar com o mesmo nome. O catálogo do
  cliente antigo não é alterado — as outras barragens dele seguem intactas.
- **Importação em massa de instrumentos:** a planilha passa a casar apenas com os tipos do
  cliente dono da barragem. Um nome que não exista nesse catálogo é recusado com mensagem
  explicando que os tipos são por cliente.

### Sobre o campo `legacy`

A separação do catálogo por cliente é aplicada automaticamente pelo Flyway no deploy: o catálogo
global é replicado para todos os clientes e cada instrumento passa a apontar para o tipo do
cliente dono da barragem, com o mesmo nome de antes. Ou seja, na prática **todos os tipos chegam
ao front com `legacy: false`**.

O campo continua existindo como rede de proteção. Um tipo com `legacy: true` é um tipo sem
cliente atrelado — situação que só ocorre se a migração não tiver rodado. Nesse estado ele:

- continua funcionando nos instrumentos que já o usam e continua aparecendo nas listagens;
- **não** pode ser editado nem excluído, para não replicar uma alteração entre clientes.

Tratar `legacy: true` como somente leitura é barato e evita que a tela quebre caso apareça.

---

## 5. 401 e 403 padronizados — sessão expirada × falta de permissão

### O problema

Os dois estavam **invertidos**, e é por isso que uma negativa de permissão derrubava a sessão
do usuário:

| Situação | Antes | Agora |
| --- | --- | --- |
| Requisição sem token, ou token expirado | `403` | **`401`** |
| Autenticado, mas sem permissão para a operação | `401` | **`403`** |

O `403` para quem não estava autenticado vinha do próprio Spring Security: sem um
`AuthenticationEntryPoint` configurado, o padrão dele é responder `403`. E as 48 checagens de
permissão espalhadas pelos serviços lançavam a exceção de "não autenticado", que virava `401`.

### A regra, agora

```
401  ->  a sessão acabou (ou nunca existiu)   ->  deslogar e mandar para o login
403  ->  está logado, mas não pode isso        ->  mostrar a mensagem, ficar na tela
```

**O front não deve mais deslogar em `403`.** Essa é a mudança central.

### Campo `errorCode` na resposta

Toda resposta `401` e `403` passa a trazer um `errorCode` estável, para escolher a mensagem sem
depender do texto em português:

```json
{
  "success": false,
  "message": "Sua sessão expirou. Entre novamente para continuar.",
  "data": null,
  "errorCode": "SESSION_EXPIRED"
}
```

| `errorCode` | Status | Quando | O que o front faz |
| --- | --- | --- | --- |
| `NOT_AUTHENTICATED` | `401` | Requisição sem token | Deslogar |
| `SESSION_EXPIRED` | `401` | Token válido, porém vencido | Deslogar, avisando *"sua sessão expirou"* |
| `INVALID_TOKEN` | `401` | Token malformado ou assinatura inválida | Deslogar e limpar o storage |
| `ACCOUNT_UNAVAILABLE` | `401` | Conta apagada ou desativada durante a sessão | Deslogar, avisando para procurar o administrador |
| `FORBIDDEN` | `403` | Autenticado, sem permissão | **Não deslogar** — exibir `message` |

O campo é **omitido** em respostas de sucesso e nos demais erros (`400`, `404`, `409`…), então
nenhuma resposta existente muda de formato.

### O que o front precisa fazer

1. **No interceptor HTTP**, trocar a regra atual por:
   - `401` → limpar sessão e redirecionar para o login;
   - `403` → **não mexer na sessão**; exibir `message` como erro (toast/alerta) e permanecer na tela.
2. **Usar `errorCode` para a mensagem** de logout: `SESSION_EXPIRED` merece *"sua sessão expirou"*;
   os demais, uma mensagem genérica de "entre novamente".
3. **Excluir as rotas públicas do redirecionamento automático.** Login, `/esqueci-senha` e
   `/definir-senha` respondem `401` legitimamente (credencial errada, link inválido ou expirado) e
   não podem cair no fluxo de "sessão expirou" — senão a tela entra em laço de redirecionamento.
   Nessas telas, trate o `401` localmente, exibindo `message`.
4. **Revisar telas que hoje escondem funcionalidade só porque a API devolveu `401`.** Com a
   correção, elas passam a receber `403` e devem mostrar o motivo em vez de deslogar.

### Detalhe: dois casos deixaram de ser erro de permissão

Duas validações estavam usando o erro de autorização para o que na verdade é payload
inconsistente. Passaram a `400` (`BusinessRuleException`):

- `Não é permitido mudar a barragem associada ao PAE`
- `Não é permitido mudar a barragem associada à informação regulatória`

O usuário tem permissão para editar; ele só mandou um `damId` diferente do vínculo existente.
Como já eram tratadas como erro exibido em tela, na prática nada muda para o front.

---

## 6. Compartilhamento público de PSB — o link exigia login

### O que estava acontecendo

O link do e-mail não abria: `GET /share/access/{token}` respondia erro de autenticação mesmo
sendo uma rota pública.

A rota estava liberada no Spring Security, mas o serviço por trás chamava
`PSBFolderService.findById()`, que valida permissão de PSB — e essa validação começa por
`getCurrentUser()`, que lança exceção quando a requisição é anônima. Ou seja: a rota era pública,
o código não era. **O fluxo inteiro quebrava logo no primeiro passo**, antes mesmo de listar a pasta.

O download de arquivo individual tinha o mesmo problema em dobro: `GET /psb/files/download/{fileId}`
estava marcado como público, mas chamava dois métodos que exigem sessão.

### O que mudou

O caminho público agora não passa por nenhuma checagem de sessão. Quem autoriza é o token do link,
validado uma única vez, e daí em diante o acesso é liberado só para o que aquele link cobre.

**Rota nova para baixar arquivo pelo link compartilhado:**

```
GET /share/{token}/files/{fileId}
```

Pública, sem `Authorization`. Responde o arquivo como download (`Content-Disposition: attachment`).

**`GET /psb/files/download/{fileId}` deixou de ser pública.** Continua existindo para o sistema
autenticado, exatamente como hoje — apenas parou de anunciar que era pública quando nunca funcionou
sem login.

### O que o front precisa fazer

Na tela de pasta compartilhada (`/shared/folder/{token}`), trocar a URL de download de cada arquivo:

| | |
| --- | --- |
| Antes | `GET /psb/files/download/{fileId}` |
| Agora | `GET /share/{token}/files/{fileId}` |

Usando o mesmo `token` que já está na URL da página. Nenhum header de autenticação — se o
interceptor global anexa `Authorization` automaticamente, tudo bem, ele é ignorado; o que não pode
é a tela exigir sessão para renderizar.

Os outros dois endpoints públicos continuam iguais: `GET /share/access/{token}` (abre a pasta) e
`GET /share/download/{token}` (baixa tudo em ZIP).

### Erros dessa tela

| Status | Quando | Tratamento |
| --- | --- | --- |
| `404` | Token não existe (link errado ou compartilhamento removido) | "Link inválido ou removido" |
| `400` | Link expirado — `message`: `Este link de compartilhamento expirou!` | Exibir `message` |
| `404` | `fileId` que não pertence à pasta compartilhada | Não deve ocorrer pela tela |
| `400` | Pasta sem nenhum arquivo, no ZIP | Exibir `message` |

Lembrando da seção 5: esta é uma **rota pública**, então esses erros não podem cair no fluxo de
"sessão expirou". A tela precisa estar fora do redirecionamento automático do interceptor.

### Dois problemas de bastidor corrigidos junto

**O ZIP vinha vazio.** O `ZipService` lia os arquivos do disco local (`Files.exists`), mas eles
estão no S3 — a verificação falhava sempre e cada arquivo era pulado em silêncio. O resultado era
um `.zip` vazio com HTTP 200, sem nenhum aviso. Agora lê do S3, e uma pasta sem arquivos devolve
erro explícito em vez de um ZIP vazio.

**O token não é chave-mestra.** Como a autorização passou a ser o token, o download por arquivo
confere se o arquivo realmente pertence à pasta compartilhada — ou a uma subpasta dela, já que o
compartilhamento cobre a subárvore inteira, igual ao ZIP. Sem essa conferência, qualquer link
válido serviria para baixar qualquer arquivo PSB do sistema bastando trocar o id na URL.

### Uma decisão que não é do back-end

`expiresAt` é opcional na criação do compartilhamento. Quando vem nulo, **o link nunca expira**.
Se a intenção for sempre ter validade, o front precisa passar a exigir a data — ou me avise que eu
coloco um prazo padrão no back-end.
