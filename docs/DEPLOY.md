# Deploy de produção

Publicação sem tirar a API do ar, com rollback em um comando.

```bash
./bash/cli_app.sh
```

Menu → **1) Deploy produção**. Rollback é a opção **2**.

---

## Como funciona

A ideia é simples: **a versão antiga só sai do ar depois que a nova provou que
funciona.** Elas convivem por alguns segundos, e o tráfego migra por um reload de
configuração do nginx — que é gracioso, não derruba conexão em andamento.

```
1. git pull                                    versão antiga atendendo
2. docker build (minutos)                      versão antiga atendendo
3. sobe a nova em container paralelo           versão antiga atendendo
4. espera a nova responder saudável            versão antiga atendendo
5. nginx -s reload  → aponta para a nova       ponto de virada
6. smoke test pela porta pública               nova atendendo
7. drena 15s e remove a antiga                 nova atendendo
```

Os containers alternam entre dois nomes, `geosegbar-api-prod-blue` e
`geosegbar-api-prod-green`. Quem está no ar num deploy vira o "antigo" no
próximo. O nginx e o Postgres não são recriados.

### Onde o deploy pode falhar, e o que acontece

| Falha em | Consequência |
| --- | --- |
| `git pull` ou build | Nada muda. Versão anterior no ar, intacta |
| Boot da nova versão (migração, config) | Container novo é removido. Versão anterior no ar, intacta |
| Nova não fica saudável no prazo | Idem — e o log da migração é destacado na saída |
| Config do nginx inválida | Upstream é revertido, container novo removido |
| Smoke test pela porta pública | **Tráfego volta para a anterior em ~1s** e o container novo é removido |

Em nenhum desses casos a API sai do ar.

---

## Rollback

Quando o deploy passou em tudo mas o problema apareceu depois — regressão que só
aparece com tráfego real, erro de comportamento, o que for:

```bash
./bash/cli_app.sh
```

Menu → **2) Rollback produção**.

Sobe `geosegbar-prod:previous` em paralelo, espera ficar saudável e só então
migra o tráfego. Se a versão anterior não subir, a atual continua no ar — um
rollback que falha não pode piorar a situação.

### O rollback NÃO reverte o banco

Esta é a parte que exige atenção. Migrações aplicadas pela versão problemática
continuam valendo depois do rollback. O código volta; o schema não.

Por isso o deploy faz **backup do banco antes de qualquer coisa**. Se o problema
for de schema, o caminho é restaurar o backup — operação manual, com autorização,
conforme [ACESSO_SERVIDOR.md](ACESSO_SERVIDOR.md).

---

## A regra que faz isso funcionar: migração compatível com as duas versões

Durante a janela de sobreposição, **duas versões da aplicação falam com o mesmo
banco**. A nova já aplicou suas migrações; a antiga ainda está atendendo.

Se a migração remover ou renomear algo que a versão antiga usa, a versão antiga
quebra durante a janela — e o rollback fica impossível, porque o código antigo
não roda mais contra o schema novo.

**Toda migração precisa funcionar para as duas versões ao mesmo tempo.**

### Seguro em um único deploy

- Criar tabela
- Adicionar coluna nula, ou com default
- Criar índice (use `CREATE INDEX CONCURRENTLY` em tabela grande)
- Adicionar valor a um enum

### Exige dois deploys — o padrão expand/contract

Para remover ou renomear coluna, apertar uma constraint, ou trocar um tipo:

**Deploy 1 (expand):** adiciona o novo, mantém o antigo, código escreve nos dois
e lê do novo com fallback para o antigo.

**Deploy 2 (contract):** depois que o deploy 1 está estável, remove o antigo.

Renomear `nome` para `nome_completo`, por exemplo:

1. Deploy 1 — cria `nome_completo`, copia os dados, aplicação escreve nas duas
2. Deploy 2 — remove `nome`

Chato? É. Mas é o que permite publicar sem janela de indisponibilidade e voltar
atrás quando dá errado.

Detalhes de como escrever as migrações em [MIGRACOES.md](MIGRACOES.md).

---

## Variáveis que ajustam o comportamento

| Variável | Padrão | Para quê |
| --- | --- | --- |
| `DEPLOY_HEALTH_TIMEOUT_SECONDS` | `300` | Espera máxima pela nova versão ficar saudável |
| `DEPLOY_DRAIN_SECONDS` | `15` | Tempo para as requisições em andamento na versão antiga terminarem |
| `SKIP_PRE_DEPLOY_BACKUP` | `false` | Pula o backup do banco — evite |
| `SKIP_GIT_PULL` | `false` | Usa o código já presente no servidor |
| `FLYWAY_ENABLED` | `true` | Sobe sem aplicar migração |
| `DEPLOY_MODE` | `FULL` | `DB_ONLY` prepara só Postgres e Redis |

Aumente `DEPLOY_DRAIN_SECONDS` se houver upload longo em andamento — o PSB aceita
arquivos de até 512 MB, e um upload em curso é cortado se a drenagem for curta.

---

## Sondas de saúde

| Endpoint | Quem usa | Público? |
| --- | --- | --- |
| `/actuator/health/liveness` | HEALTHCHECK do Docker | Não — nginx bloqueia |
| `/actuator/health/readiness` | Deploy e rollback | Não — nginx bloqueia |
| `/actuator/prometheus` | Prometheus | Não — nginx bloqueia |

Todos são liberados no Spring, para que healthcheck e Prometheus alcancem o
container pela rede interna do Docker. O nginx nega `/actuator/` vindo da
internet: como `management.endpoints.web.exposure.include=*`, expor isso
publicaria `/actuator/env` e `/actuator/configprops`, que carregam segredos.

`ActuatorExposureTest` quebra se alguém ampliar essa liberação sem querer.

> **Histórico:** essas rotas caíam em `anyRequest().authenticated()` e respondiam
> 403. O HEALTHCHECK do Docker falhou 29.238 vezes seguidas (container
> "unhealthy" desde que subiu) e o Prometheus nunca coletou uma métrica — todos
> os dashboards e alertas estavam cegos.

---

## Verificando um deploy

```bash
ssh geosegbar-vps 'docker ps --filter "name=geosegbar-api" --format "table {{.Names}}\t{{.Status}}\t{{.Image}}"'
```

Espera-se **um** container `-blue` ou `-green`, com status `healthy`.

```bash
ssh geosegbar-vps 'cat ~/GeoSegBar_Back-End/runtime/upstream_active.conf'
```

Deve apontar para o container que está no ar.

```bash
ssh geosegbar-vps 'docker exec prometheus-prod wget -qO- "http://localhost:9090/api/v1/targets?state=any" | grep -o "\"health\":\"[a-z]*\"" | sort | uniq -c'
```

Os alvos devem estar `up`.

---

## Quando algo dá errado

**Ficaram dois containers no ar.** O deploy foi interrompido no meio. Confira o
`upstream_active.conf`, veja quem está recebendo tráfego, e remova o outro:

```bash
ssh geosegbar-vps 'docker rm -f geosegbar-api-prod-<cor-que-nao-esta-no-upstream>'
```

**Não existe imagem `:previous`.** O rollback automático só funciona a partir do
segundo deploy blue-green. Antes disso, publique a versão desejada pelo deploy
normal.

**O nginx não recarrega.** Valide a configuração antes de insistir:

```bash
ssh geosegbar-vps 'docker exec nginx-prod nginx -t'
```

---

## O que mudou em relação ao processo anterior

O deploy antigo fazia, nesta ordem: parava o container, removia, buildava, subia
o novo, dormia 30 segundos, recriava o nginx.

Três problemas:

**A API ficava fora do ar durante o build inteiro** — vários minutos, porque o
build roda Maven dentro do Docker.

**Não havia rollback.** O container antigo já tinha sido removido antes do build
começar. Se o build falhasse, ou se a nova versão não subisse, não havia para
onde voltar — só refazer o deploy com o código anterior, com a API fora do ar o
tempo todo.

**O nginx era recriado a cada deploy**, abrindo mais uma janela de
indisponibilidade sem necessidade nenhuma.

Além disso o `sleep 30` seguido de um `grep` no log era frágil: se o boot
demorasse mais que isso, o nginx passava a apontar para um container que ainda
não estava pronto.
