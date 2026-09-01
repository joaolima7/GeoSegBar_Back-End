# Subida para produção — mudanças de agosto/2026

Roteiro escrito **depois** de subir tudo em homologação. Cada passo aqui existe
porque algo tropeçou lá — não é checklist genérico.

Estado de produção conferido em 24/08/2026 18h. Se muito tempo passar, revalide
antes de seguir.

---

## O que vai subir

| Área | Mudança |
| --- | --- |
| Sessão | `401` e `403` deixam de estar invertidos; campo `errorCode` nas respostas |
| PSB | Compartilhamento público volta a funcionar (`GET /share/{token}/files/{fileId}`) |
| ANA | Coleta histórica: fim do livelock, checkpoint confiável, 429 tratado |
| Barragem | Nome pode conter número |
| Deploy | Blue-green com rollback |
| Backup | Cópia no S3, verificação de integridade, vigia de frescor |
| Monitoramento | Sondas de saúde acessíveis — healthcheck e Prometheus saem do escuro |
| Memória | ZIP do PSB transmitido em fluxo; JVM encerra no OOM em vez de virar zumbi |

---

## Antes de começar — três coisas que vão travar se você não fizer

### 1. O `git pull` vai abortar

Produção tem alteração local no `deploy_vps.sh`:

```
 M bash/scripts/deploy_vps.sh
```

É **só mudança de permissão** (`chmod +x` feito à mão em algum momento; no git o
arquivo está como `100644`). Foi exatamente o que travou o primeiro deploy em
homologação e deixou a API fora do ar, porque o script antigo derrubava o
container *antes* de puxar o código.

Confira que o diff é vazio e descarte:

```bash
cd /home/wwvpsb/apps/api && git diff --stat bash/scripts/deploy_vps.sh
```

Deve mostrar `1 file changed, 0 insertions(+), 0 deletions(-)`. Se for isso:

```bash
cd /home/wwvpsb/apps/api && git checkout -- bash/scripts/deploy_vps.sh
```

Se mostrar alterações de conteúdo, **pare** e guarde antes:
`git stash push -m "alteracoes locais prod"`.

O commit novo já traz o arquivo como `100755`, então esse conflito não volta.

### 2. Falta `BACKUP_ALERT_EMAIL` no `.env.prod`

Sem ela, os alertas de falha de backup vão para `noreply@geosegbar.com.br` — uma
caixa que ninguém lê. Seria repetir o problema que deixou produção 126 dias sem
backup e sem ninguém saber.

Acrescente ao `.env.prod`:

```
BACKUP_ALERT_EMAIL=alguem-que-le@geometrisa.com.br
```

### 3. Existe um cron de backup quebrado, em outro usuário

```
usuário wwvpsb:
  0 2 * * * bash /home/wwvpsb/backend.geometrisa-prod.com.br/bash/scripts/backup_database_prod.sh
```

Esse caminho **não existe** — o projeto está em `/home/wwvpsb/apps/api`. É a
causa dos 126 dias sem backup.

O `setup_cron_backup.sh` remove entradas antigas pelo nome do script, mas só no
crontab **de quem o executa**. Então:

- Rode a CLI **como `wwvpsb`** → a entrada velha é substituída. Preferível.
- Se rodar como `root` → você fica com duas: a nova funcionando e a velha
  falhando toda noite, calada.

Decida isso antes do passo 8.

### 4. Conferir o `JAVA_OPTS` — foi o que agravou a queda em homologação

Produção já está bem configurada:

```
-Xms1152m -Xmx1152m -XX:+UseG1GC -XX:MaxGCPauseMillis=200
-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/app/logs/heapdump.hprof
-XX:+ExitOnOutOfMemoryError -XX:MetaspaceSize=256m -XX:MaxMetaspaceSize=512m
-XX:MaxDirectMemorySize=256m -XX:ReservedCodeCacheSize=192m
```

Homologação tinha apenas `-Xms1024m -Xmx2048m -XX:+UseG1GC`, e por isso o
OutOfMemoryError de 24/08 deixou a JVM viva porém inútil por 18 horas. Em
produção o `ExitOnOutOfMemoryError` teria reiniciado o container em ~40 segundos.

**Nada a mudar em produção**, mas duas observações:

- As flags de segurança agora também vêm da imagem (`JAVA_SAFETY_OPTS`), então
  valem mesmo que o `.env.prod` seja alterado por engano.
- `-Xmx1152m` fixo num container de 2 GB deixa folga adequada. Se um dia o
  limite do container mudar, considere trocar por
  `-XX:MaxRAMPercentage=70`, que acompanha o cgroup — é o padrão da imagem
  quando `JAVA_OPTS` não é informado.

---

## Passo a passo

### 1. Confirmar que homologação está saudável

Não suba para produção com homologação torta.

```bash
ssh geosegbar-homolog 'cd /home/wwgeomprod/backend.geometrisa-prod.com.br && bash bash/scripts/verify_backups.sh'
ssh geosegbar-homolog 'docker ps --filter "name=geosegbar-api" --format "{{.Names}} {{.Status}}"'
```

Espera-se um container `-blue` ou `-green` com `(healthy)`.

### 2. Resolver o bloqueio do git

```bash
cd /home/wwvpsb/apps/api && git diff --stat bash/scripts/deploy_vps.sh
```

Diff vazio → `git checkout -- bash/scripts/deploy_vps.sh`

### 3. Editar o `.env.prod`

Acrescentar `BACKUP_ALERT_EMAIL`. É alteração de arquivo em produção — faça
consciente, e confira que não quebrou nenhuma linha existente:

```bash
cd /home/wwvpsb/apps/api && grep -c '=' .env.prod
```

O total de variáveis deve ser o de antes **+1** (eram 42).

### 4. Conferir o `JAVA_OPTS`

```bash
cd /home/wwvpsb/apps/api && grep "^JAVA_OPTS=" .env.prod
```

Deve conter `-XX:+ExitOnOutOfMemoryError`. Já contém hoje — veja a seção acima
para o porquê de isso importar.

### 5. Puxar o código

```bash
cd /home/wwvpsb/apps/api && git pull origin main
```

Produção está em `da85ab2`. Devem entrar os commits até `b0f299b`.

### 6. Deploy

```bash
cd /home/wwvpsb/apps/api && ./bash/cli_app.sh
```

Opção **1) Deploy produção**.

O que esperar, e por quê:

- O backup pré-deploy roda **antes** de qualquer coisa e agora envia ao S3.
  Se o envio falhar, o deploy **prossegue com aviso** — o dump local íntegro já
  é ponto de restauração suficiente.
- O build acontece com a API **antiga ainda atendendo**. São vários minutos; é
  esperado.
- A versão nova sobe como `geosegbar-api-prod-green`, ao lado do container
  legado `geosegbar-api-prod`.
- Só depois de a nova responder `readiness UP` o tráfego migra.
- A migração **V3** roda aqui: replica o catálogo de tipos de instrumento para
  cada cliente. Em homologação resultou em 7 clientes × 5 tipos = 35 registros,
  sem duplicata. Produção terá mais clientes, proporcionalmente.

Se o deploy abortar, a versão anterior continua no ar — foi assim em homologação
todas as vezes em que falhou.

### 7. Recriar o nginx — passo que homologação ainda não fez

**Este é o item mais fácil de esquecer, e o único com indisponibilidade.**

O nginx em produção roda há 11 dias com o **template antigo**. Reload não
resolve: o template é processado por `envsubst` apenas na criação do container.

Sem recriar:

- `location /actuator/ { deny all; }` não existe → depois do deploy,
  `/actuator/health` e `/actuator/prometheus` ficam alcançáveis pela internet
- O `include upstream_active.conf` não existe → o arquivo de upstream fica
  **inerte**, e a troca de tráfego do blue-green acontece só pelo
  `--network-alias`
- Durante a janela de sobreposição, o DNS do Docker alterna entre a versão
  antiga e a nova, então parte das requisições vai para cada uma

Nada disso derruba o sistema — homologação está assim agora e funciona. Mas o
blue-green só fica completo depois de recriar.

**Custo: 3 a 5 segundos de indisponibilidade.** Faça em horário de baixo
movimento, e depois do deploy ter dado certo:

```bash
cd /home/wwvpsb/apps/api && set -a && . ./.env.prod && set +a && \
mkdir -p runtime && \
echo "set \$upstream_server $(docker ps --format '{{.Names}}' | grep -E 'geosegbar-api-prod-(blue|green)'):9090;" > runtime/upstream_active.conf && \
docker rm -f nginx-prod && \
docker run -d --name nginx-prod --restart unless-stopped --network geosegbar-network \
  -p ${SERVER_PORT}:80 \
  -v $PWD/nginx/default.conf.template:/etc/nginx/templates/default.conf.template:ro \
  -v $PWD/runtime/upstream_active.conf:/etc/nginx/upstream_active.conf:ro \
  nginx:alpine
```

Confirme depois:

```bash
cd /home/wwvpsb/apps/api && set -a && . ./.env.prod && set +a && \
curl -s -o /dev/null -w "actuator publico (esperado 404): %{http_code}\n" http://localhost:${SERVER_PORT}/actuator/health && \
curl -s -o /dev/null -w "API pela porta publica (esperado != 502/504): %{http_code}\n" -X POST -H 'Content-Type: application/json' -d '{}' http://localhost:${SERVER_PORT}/user/login/initiate
```

A partir daqui os deploys seguintes não recriam mais o nginx.

### 8. Configurar o cron de backup

Como o usuário decidido lá em cima:

```bash
cd /home/wwvpsb/apps/api && ./bash/cli_app.sh
```

Opção **5) Configurar cron de backup** no menu. Instala duas entradas: backup às 2h,
vigia às 9h.

> A CLI engole a saída dos scripts — você verá só as linhas `[INFO]`, sem a
> validação detalhada. Não é sinal de erro; confirme pelo passo 8.

### 9. Verificar

```bash
cd /home/wwvpsb/apps/api && ./bash/cli_app.sh
```

Opção **4) Verificar estado dos backups**. Espera-se:

```
📅 TAREFAS AGENDADAS
   ✅ [0 2 * * *] backup_database_prod.sh
   ✅ [0 9 * * *] check_backup_freshness.sh
🕐 ÚLTIMA EXECUÇÃO
   ✅ sucesso
☁️  CÓPIAS NO S3
   1 arquivo(s)...
```

Se aparecer `❌ CAMINHO NÃO EXISTE`, o cron velho do `wwvpsb` sobreviveu — volte
ao item 3 da seção anterior.

E confira o resto:

```bash
ssh geosegbar-vps 'docker ps --filter "name=geosegbar-api" --format "{{.Names}} {{.Status}} {{.Image}}"'
```

Um único container, `-blue` ou `-green`, com `(healthy)`. Se seguir
`(unhealthy)`, a sonda não está passando — veja a seção de problemas.

```bash
ssh geosegbar-vps 'docker exec prometheus-prod wget -qO- "http://localhost:9090/api/v1/targets?state=any" | grep -o "\"health\":\"[a-z]*\"" | sort | uniq -c'
```

Os alvos devem estar `up`. Estavam `down` desde sempre, por causa do 403.

---

## O que o front precisa fazer

Duas mudanças que **não** dependem do deploy, mas cujo sintoma aparece depois dele.

### Tipos de instrumento — trocar o endpoint

A tela de criação de instrumento chama `GET /instrument-types`, que é o endpoint
global. Para um admin ele devolve os tipos de **todos** os clientes, e a lista
aparece com nomes repetidos — um por cliente.

Não é duplicação de dados. Em homologação o banco tem 7 clientes × 5 tipos, zero
duplicata dentro de um mesmo cliente.

O endpoint certo para essa tela:

```
GET /instrument-types/client/{clientId}
```

**Melhor corrigir antes do deploy em produção**, porque lá há mais clientes e a
lista fica proporcionalmente maior.

### Sessão — parar de deslogar em `403`

- `401` → sessão acabou → deslogar
- `403` → autenticado, sem permissão → exibir a mensagem, **não** deslogar

E excluir do redirecionamento automático as rotas públicas — login,
`/esqueci-senha`, `/definir-senha` —, que respondem `401` legitimamente e
entrariam em laço.

Detalhes em [FRONTEND_PRIMEIRO_ACESSO_E_PSB.md](FRONTEND_PRIMEIRO_ACESSO_E_PSB.md).

---

## Se algo der errado

### O deploy abortou

A versão anterior continua no ar, sem alteração. Leia o motivo na saída — o log
da migração é destacado quando o Flyway falha. Corrija e rode de novo.

### O deploy passou mas apareceu problema depois

```bash
cd /home/wwvpsb/apps/api && ./bash/cli_app.sh
```

Opção **2) Rollback produção**.

**O banco não volta.** A migração V3 já terá sido aplicada. Se o problema for de
schema, o caminho é restaurar o backup pré-deploy — operação manual, com
autorização, conforme [ACESSO_SERVIDOR.md](ACESSO_SERVIDOR.md).

### Container segue `(unhealthy)`

A sonda usa `GET /actuator/health/liveness`. Teste por dentro:

```bash
ssh geosegbar-vps 'docker exec $(docker ps --format "{{.Names}}" | grep geosegbar-api) wget -q -O - http://localhost:9090/actuator/health/liveness'
```

Deve responder `{"status":"UP"}`. Se der 401 ou 403, o `SecurityConfig` novo não
entrou no build — confira se o `git pull` realmente trouxe os commits.

> Foi o que travou o primeiro deploy blue-green em homologação: a sonda usava
> `wget --spider`, que envia **HEAD**, e o matcher estava restrito a `GET`. A
> aplicação subia saudável e o deploy a rejeitava mesmo assim.

### Ficaram dois containers no ar

Deploy interrompido no meio. Veja quem recebe tráfego e remova o outro:

```bash
ssh geosegbar-vps 'cat /home/wwvpsb/apps/api/runtime/upstream_active.conf; docker ps --filter "name=geosegbar-api" --format "{{.Names}}"'
```

### Backup marca `parcial`

Dump local íntegro, envio ao S3 falhou. O deploy prossegue de propósito. Veja o
motivo — agora o erro aparece no console com o código HTTP e a mensagem do S3:

```bash
ssh geosegbar-vps 'tail -30 /home/wwvpsb/apps/api/logs/backup.log'
```

---

## Depois, quando der

Não bloqueiam a subida, mas ficam pendentes:

- **Regra de ciclo de vida no bucket** `geosegbar-prod`, prefixo `db-backups/`.
  Hoje nada expira. Confirme antes o prazo legal de retenção — barragem tem
  obrigação de guardar histórico.
- **Job 1 da coleta ANA**, em produção, parado em 2019-01-18. Depois do deploy o
  `recoverOrphanedJobs` corrigido deve retomá-lo sozinho. Confirme:
  `docker logs --since 5m <container> | grep "PAUSED → QUEUED"`
- **Lacunas de 2015–2018** na série do instrumento 1377 (~40 dias/ano). Falta
  saber se é ausência de dado na ANA ou resquício das janelas puladas.
- **A CLI engole a saída dos scripts** (`run_cmd`). Não quebra nada, mas obriga a
  confiar às cegas em comandos de configuração.
- **Healthcheck do container legado** falhou 29.238 vezes seguidas. Some com o
  deploy, mas vale conferir que o novo fica `healthy` de verdade.
