# Monitoramento da API GeoSegBar

Métricas, logs, traces e alertas da API em produção, num Grafana que só o time
abre.

| Sinal | Ferramenta | Retenção | Responde |
|---|---|---|---|
| Métricas | Prometheus | 30 dias (teto 8 GB) | volume, latência, erros, saturação, servidor, banco |
| Logs | Loki | 30 dias | o que aconteceu, com `requestId`, `userId` e `traceId` |
| Traces | Tempo | 7 dias | onde cada requisição gastou o tempo (SQL, S3, ANA, SMTP) |
| Alertas | Alertmanager | n/a | Telegram (crítico com som, aviso em silêncio) |

Tudo roda no próprio servidor, em `/opt/geosegbar-monitoring`, num compose
separado do deploy da API. Nenhuma porta nova na internet.

---

## Como chegar ao Grafana

**Pelo Tailscale (normal).** Com o app do Tailscale ligado no Mac ou no
celular, abra `https://geosegbar-prod.<sua-tailnet>.ts.net:8443/`. A URL exata
fica em `GRAFANA_ROOT_URL`, em `/etc/geosegbar/monitoring/monitoring.env`.

**Por túnel SSH (emergência).** Com a chave autorizada para o usuário
`gsb-monitor` (ver "Usuário de túnel"):

```bash
ssh -N -L 3000:127.0.0.1:3000 -p 22022 gsb-monitor@<ip-do-servidor>
```

e abra `http://localhost:3000`. No Safari o login só funciona com
`GRAFANA_COOKIE_SECURE=false`; Chrome e Firefox aceitam como está.

**Usuário e senha.** O admin chama `GRAFANA_ADMIN_USER` (padrão `gsb-admin`). A
senha inicial fica em `/etc/geosegbar/monitoring/secrets/grafana_admin_password`
(só root lê):

```bash
ssh geosegbar-vps 'cat /etc/geosegbar/monitoring/secrets/grafana_admin_password'
```

Crie no Grafana uma conta Viewer para cada pessoa e use o admin só para
administrar.

### Trocar a senha do admin

O Grafana lê o arquivo de senha só na criação do banco dele. Para trocar
depois, grave a senha nova no arquivo de segredo e aplique a partir dele (a
senha não passa pela linha de comando nem pelo histórico do shell):

```bash
ssh geosegbar-vps
read -rs NOVA && printf '%s' "$NOVA" > /etc/geosegbar/monitoring/secrets/grafana_admin_password && unset NOVA
docker exec -i geosegbar-grafana grafana cli admin reset-admin-password --password-from-stdin \
  < /etc/geosegbar/monitoring/secrets/grafana_admin_password
```

---

## Primeira instalação

Ordem importa. Cada passo diz quem faz.

### 1. Medir o servidor (João, só leitura)

```bash
ssh geosegbar-vps 'free -h; nproc; df -hT -x tmpfs -x devtmpfs; docker version --format "{{.Server.Version}}"; docker compose version; ip -4 addr show docker0'
```

```bash
ssh geosegbar-vps 'docker info --format "{{.LoggingDriver}}"; readlink -f /var/log/apache2/error_log; crontab -l | grep -n backup; docker ps -a --format "{{.Names}}" | grep -E "prometheus-prod|grafana-prod|postgres-exporter-prod"'
```

O que conferir:

- **Docker 28 ou mais novo.** Em versões anteriores, uma porta publicada em
  `127.0.0.1` ainda pode ser alcançada por outra máquina do mesmo segmento de
  rede do datacenter. Abaixo do 28, atualize o Docker ou bloqueie no nftables
  tráfego para `127.0.0.0/8` que não venha da interface `lo`.
- **Log driver `json-file`** (é o padrão). O deploy da API passa a fixar
  `--log-driver json-file`, e o Alloy lê os logs por ele.
- **`/var/log/apache2/error_log` dentro de `/var/log`.** Se for link para fora
  de `/var/log`, o Alloy não enxerga o arquivo e os eventos do ModSecurity não
  chegam: ajuste `HOST_LOG_DIR` ou o caminho no `config.alloy`.
- **Cron do backup no crontab do root.** Só root grava a métrica de backup em
  `/var/lib/geosegbar-monitoring/textfile`.

A stack usa de 1,5 a 2,5 GB de RAM (teto somado de 4,1 GB) e até 10 GB de
disco. Precisa sobrar 3 GB de memória com dois containers da API no ar (o
blue/green sobrepõe os dois por alguns segundos). Sem `docker compose`, instale
o plugin: `dnf install -y docker-compose-plugin`.

### 2. Criar as contas externas (João)

**Bot do Telegram.**

1. No Telegram, fale com `@BotFather`, mande `/newbot` e guarde o token.
2. Crie um grupo (ex. "GeoSegBar alertas"), adicione o bot e mande qualquer
   mensagem no grupo.
3. Abra `https://api.telegram.org/bot<TOKEN>/getUpdates` e copie o
   `chat.id` do grupo (número negativo, ex. `-1001234567890`).

**healthchecks.io** (plano grátis). Crie dois checks:

| Check | Período | Tolerância | Quem pinga |
|---|---|---|---|
| Watchdog | 1 minuto | 5 minutos | Alertmanager, a cada minuto |
| Backup | 1 dia | 4 horas | script de backup, às 02:00 |

Ligue a notificação dos dois para o seu e-mail e o Telegram. Guarde a URL de
ping de cada um (`https://hc-ping.com/<uuid>`). Quem tiver a URL consegue
pingar no lugar do servidor: trate como segredo.

**Uptime externo** (UptimeRobot ou Better Stack, plano grátis). Um monitor HTTPS
para `https://api.geosegbar.com.br/nginx-health`, a cada 5 minutos, com alerta
no seu e-mail. Ele avisa quando o servidor inteiro cai, caso em que o
monitoramento interno cai junto.

### 3. Subir o monitoramento (João, pela CLI no servidor)

```bash
ssh geosegbar-vps
cd /home/wwvpsb/apps/api && git pull
sudo ./bash/cli_app.sh        # opção 7) Deploy do monitoramento
```

Na primeira vez o deploy cria `/etc/geosegbar/monitoring/`, gera as senhas
internas e para, listando os segredos que faltam. Preencha cada um sem quebra
de linha. O `read -rs` não mostra o valor e não o deixa no histórico do shell:

```bash
S=/etc/geosegbar/monitoring/secrets
read -rs v && printf '%s' "$v" > $S/telegram_bot_token; unset v          # cole o token do bot
read -rs v && printf '%s' "$v" > $S/telegram_chat_id; unset v            # cole o chat id
read -rs v && printf '%s' "$v" > $S/healthchecks_watchdog_url; unset v   # cole a URL do Watchdog
```

Revise `/etc/geosegbar/monitoring/monitoring.env` (`PG_EXPORTER_DATABASE` vem do
`DB_NAME`; `BLACKBOX_TARGETS` lista as URLs públicas) e rode a opção 7 de novo.
O deploy valida tudo com os binários das imagens antes de trocar qualquer coisa.

O Prometheus passa a coletar a API depois do passo 5. Até lá o alerta
`ApiSemColeta` é esperado.

### 4. Papel de leitura no banco (João, pela CLI)

Opção 8 da CLI. Mostra o SQL e só executa depois de você digitar `sim`:

```sql
CREATE ROLE monitoring WITH LOGIN PASSWORD '<arquivo pg_monitoring_password>' CONNECTION LIMIT 3;
GRANT pg_monitor TO monitoring;
```

O papel lê estatísticas do Postgres e não lê nenhuma tabela da aplicação.

### 5. Deploy da API (João, pela CLI)

Opção 1, o deploy de sempre. A imagem nova traz o actuator na porta 8081, os
labels que o Prometheus procura e liga o tracing ao encontrar o
`geosegbar-alloy`.

**Neste primeiro deploy o `git pull` do passo 3 é obrigatório.** O bash
continua executando a versão do `deploy_vps.sh` que já estava aberta, e a
versão anterior sonda a saúde na 9090: com a imagem nova ela recebe 404 e o
deploy aborta (com segurança: a versão no ar não muda). Se isso acontecer,
basta rodar o deploy de novo: o arquivo já está atualizado. Da próxima vez em
diante o próprio script se reexecuta quando o pull muda o arquivo.

O deploy também remove o `prometheus-prod` legado, que coletava a API na 9090.

### 6. Tailscale (João, como root no servidor)

```bash
sudo bash bash/scripts/monitoring/setup_tailscale.sh
```

Autorize o servidor pelo link que aparece. No painel da Tailscale:

- **Machines**: desative a expiração da chave do `geosegbar-prod`.
- **Access controls**: só o grupo do time alcança o servidor na porta 8443.
  Exemplo (troque os e-mails):

```json
{
  "groups": { "group:geosegbar": ["joao@exemplo.com"] },
  "tagOwners": { "tag:geosegbar-prod": ["group:geosegbar"] },
  "acls": [
    { "action": "accept", "src": ["group:geosegbar"], "dst": ["tag:geosegbar-prod:8443"] }
  ]
}
```

Grave a URL impressa em `GRAFANA_ROOT_URL` (`monitoring.env`) e rode a opção 7
de novo.

### 7. Backup e o vigia externo (João)

No `.env.prod`, defina `HEALTHCHECKS_BACKUP_URL` com a URL do check Backup e
rode a opção 3 da CLI uma vez. Ela grava a primeira métrica de backup e pinga o
healthchecks.io.

### 8. Conferência final

- [ ] O Grafana abre pela tailnet e o painel "Visão geral" mostra dados.
- [ ] Painel "Saúde do monitoramento": todos os jobs com `up = 1`.
- [ ] Alerta de teste chega no Telegram:

  ```bash
  docker exec geosegbar-alertmanager amtool alert add Teste severity=warning \
    --annotation=summary="Teste do caminho de alertas" --alertmanager.url=http://localhost:9093
  ```

- [ ] `docker stop geosegbar-alertmanager` por 6 minutos faz o healthchecks.io
  avisar. Depois `docker start geosegbar-alertmanager`.
- [ ] De fora do servidor, `nmap -p- <ip>` mostra as mesmas portas abertas de
  antes. Nada em 3000, 3100, 3200, 4317, 4318, 8081, 9090, 9093, 9100, 12345.
- [ ] Um JWT colado num log de teste aparece como `[jwt]` no Loki.

---

## Painéis

Todos na pasta **GeoSegBar**, com links entre si no canto superior.

| Painel | Para quê |
|---|---|
| Visão geral | Requisições/s, taxa de 5xx, p95, versão no ar, SLO do mês, alertas disparando, marcas de deploy |
| Endpoints | Tabela de rotas por volume, p95, p99, erro e **tempo total consumido** (o ranking de gargalo). Clique na rota para o detalhe e os traces lentos |
| Erros | 4xx/5xx por rota, exceções por tipo, logs ERROR e traces com erro |
| Tráfego (nginx) | Volume, top IPs, user agents, bytes, requisições mais lentas, web × app |
| Banco de dados | Conexões, transações longas, locks, cache hit, tabelas, pool do HikariCP, top queries (depois da Fase 4) |
| JVM e executores | Heap, GC, threads, Tomcat, filas dos executores @Async, Redis (Lettuce) |
| Servidor e containers | CPU, memória, disco com previsão, rede, consumo por container, backup, sondas externas, Redis |
| Segurança | Logins, MFA, resets, bypassKey, 401/403/429, IPs com 4xx, ModSecurity, SSH |
| Jobs e negócio | Cada `@Scheduled`, coleta ANA, fila histórica, e-mails, S3, uploads, checklists, leituras |
| Saúde do monitoramento | `up` de cada componente, séries, notificações falhadas |

**Tempo total consumido** é `rate(http_server_requests_seconds_sum[...])` por
rota: quantos segundos de servidor a rota gasta por segundo de relógio, ou seja,
quantas threads ela ocupa em média. Uma rota de 300 ms chamada 50 vezes por
segundo pesa mais que uma de 5 s chamada uma vez por hora. Comece a otimização
pelo topo dessa coluna.

### Do gráfico ao trace e ao log

- No gráfico de latência, os losangos são **exemplares**: clique e abra o trace
  daquela requisição no Tempo.
- No trace, o botão **Logs for this span** abre os logs com o mesmo `traceId`.
- No log JSON da API, o campo `traceId` vira link para o trace.
- Toda resposta da API traz `X-Request-Id` e, com tracing ligado, `X-Trace-Id`.
  Quando um usuário reclamar de erro, peça esse código ao front e busque no
  Grafana (Explore, Loki: `{service="api"} |= "<código>"`; Tempo: cole o trace id).

---

## Alertas

Críticos chegam no Telegram com som, a qualquer hora. Avisos chegam em
silêncio. O alerta de cada seção abaixo traz este link em "o que fazer".

### Watchdog

Sempre disparando, de propósito. O Alertmanager o entrega a cada minuto ao
healthchecks.io; se parar, o healthchecks.io avisa que o servidor, o Prometheus
ou o Alertmanager caiu. Nunca silencie.

### ApiSemColeta

Nenhum container da API responde na porta 8081.

1. `docker ps --filter name=geosegbar-api` mostra um container `healthy`?
2. Se sim e foi um rollback para imagem antiga (sem a porta 8081), o alerta é
   esperado até o próximo deploy.
3. Se não, `docker logs --tail 100 <container>` e o painel Servidor.

### SloDisponibilidadeQueimaRapida

Mais de 7,2% das requisições com 5xx na última hora e nos últimos 5 minutos.

1. Painel **Erros**: qual rota e qual exceção.
2. Clique num trace com erro para ver onde falhou.
3. Coincide com deploy (marca na Visão geral)? Considere o rollback (CLI, opção 2).

### SloDisponibilidadeQueimaLenta

Mais de 3% de 5xx nas últimas 6 horas. Mesmo roteiro, sem urgência de madrugada.

### LatenciaAlta

p95 global acima de 1 s por 15 minutos (sem upload, download, exportação e
importação). Painel **Endpoints**, coluna tempo total consumido: a rota do
topo é a causa provável. Veja também o pool do HikariCP no painel Banco.

### EndpointLento

Uma rota com p95 acima de 3 s. No painel Endpoints, selecione a rota e abra os
traces lentos: o span mais largo diz se é SQL, S3 ou a ANA. Muitos spans de SQL
iguais e seguidos indicam N+1.

### PoolDoBancoEsgotando

Requisições esperando conexão do HikariCP (15 por instância).

1. Painel Banco: transações longas e queries mais caras.
2. `docker exec postgres-prod psql -U <DB_USERNAME> -d <DB_NAME> -c "SELECT pid, state, now()-xact_start AS idade, left(query,120) FROM pg_stat_activity WHERE state <> 'idle' ORDER BY idade DESC LIMIT 10;"`
3. O log da API acusa `leak-detection-threshold` quando uma conexão fica presa
   mais de 30 s.

### HeapAlto

Heap ocupado depois do GC acima de 85%. Se continuar subindo, a JVM morre por
OutOfMemoryError e grava um heap dump em `logs/` no servidor. Compare com o
último deploy: vazamento novo costuma aparecer logo depois.

### ThreadsDoTomcatSaturadas

Mais de 90% das 100 threads do Tomcat ocupadas. Mesmo roteiro de LatenciaAlta.
Download de ZIP longo também segura thread.

### ApiReiniciandoEmLoop

O processo Java reiniciou mais de 2 vezes em 15 minutos. `docker logs` do
container e, se houver, o heap dump em `logs/`. O alerta OomNoKernel diz se foi
o kernel que matou.

### ExecutorSaturado

A fila de um executor @Async (`emailExecutor`, `auditExecutor`,
`historicalDataExecutor`...) encheu e tarefas novas são recusadas. Painel JVM e
executores mostra qual. Costuma ser SMTP ou S3 lento travando as threads.

### JobAgendadoFalhando

Um `@Scheduled` terminou com exceção. Painel Jobs e negócio, tabela de jobs, e
os logs ERROR do mesmo horário.

### ErrosNoLogEmPico

Mais de 20 linhas ERROR em 10 minutos. Painel Erros, quadro de logs.

### PicoDeNegacoes

401/403/429 cinco vezes acima da mesma hora da semana anterior. Painel
Segurança, top IPs com 4xx. Um único IP concentrado é varredura; muitos IPs com
401 depois de um deploy costuma ser token invalidado.

### DiscoQuaseCheio

Menos de 10% livre, ou projeção de encher em 24 horas.

```bash
ssh geosegbar-vps 'df -h; du -sh /home/wwvpsb/db_backups /var/lib/docker/volumes/* 2>/dev/null | sort -h | tail'
```

Candidatos: backups locais (`BACKUP_KEEP_COUNT`), volumes do Docker, logs.

### HostPressionado

CPU acima de 85% ou memória abaixo de 10% (alertas HostCpuAlta,
HostMemoriaBaixa e ContainerPertoDoLimiteDeMemoria). Painel Servidor e
containers: qual container consome.

### PostgresFora

O exporter não conecta no Postgres (ou o banco está perto do
`max_connections`, no alerta ConexoesDoBancoAltas).
`docker ps -a --filter name=postgres-prod` e `docker logs --tail 50 postgres-prod`.

### TransacaoLongaNoBanco

Transação aberta há mais de 5 minutos (ou deadlock recente). A consulta de
PoolDoBancoEsgotando mostra qual. `idle in transaction` é a aplicação que abriu
e não fechou.

### RedisFora

Redis sem resposta. O rate limit libera tudo (fail-open) e a fila de jobs
históricos para. `docker start redis-prod` ou o deploy da API (que recria o
Redis se precisar).

### SondaExternaFalhando

A URL pública não respondeu 2xx (de dentro do servidor, passando por DNS,
Apache e nginx). Confirme no uptime externo. Se só a sonda interna falha, o
problema pode ser DNS ou o hairpin do próprio servidor.

### CertificadoTlsExpirando

Certificado vence em menos de 14 dias. O AutoSSL do cPanel deveria ter
renovado: WHM → SSL/TLS → Manage AutoSSL.

### BypassKeyUsado

Alguém chamou `POST /user/reset-password` com `bypassKey`, aceita ou não.
Painel Segurança, quadro `[SECURITY]`: o log traz o `userId` alvo. Se não foi
você, troque a `ADMIN_BYPASS_KEY` e a senha da conta alvo, e revise o
`audit_logs` daquele horário (só leitura).

### PicoDeFalhasDeLogin

Mais de 30 senhas erradas em 15 minutos. Painel Segurança: IPs e contas.

### ColetaAnaParada

Nenhuma coleta da ANA com sucesso em 26 horas (ou a última terminou em falha,
no alerta ColetaAnaFalhou). Leia `docs/INCIDENTE_2026-08-19_COLETA_ANA.md`
antes de reprocessar.

### BackupAtrasado

O último backup com sucesso tem mais de 26 horas, a última execução falhou, ou
a métrica não existe. `./bash/cli_app.sh` → 4 (Verificar estado dos backups) e
`tail -50 logs/backup.log`.

### AlvoDeColetaFora

Um componente do monitoramento ou exporter parou de responder (ou uma regra
falha na avaliação, ou o Prometheus chegou perto do teto de disco).
`docker ps --filter label=com.docker.compose.project=geosegbar-monitoring` e o
log do container parado.

### NotificacaoDeAlertaFalhando

O Alertmanager não entregou no Telegram. Confira o token e o chat
(`docker logs geosegbar-alertmanager`). Enquanto isso, só o healthchecks.io e
o uptime externo avisam.

### OomNoKernel

O kernel matou um processo por falta de memória. Painel Servidor e containers e
`grep -i "killed process" /var/log/messages | tail`.

### ModSecurityBloqueiosEmPico

Mais de 50 eventos do ModSecurity em 10 minutos. Painel Segurança. Bloqueio do
próprio front (falso positivo) aparece como o mesmo caminho repetido para IPs
variados.

### ForcaBrutaSsh

Mais de 300 tentativas de SSH recusadas em 10 minutos. O SSH só aceita chave,
então é ruído, a não ser que apareça uma linha `Accepted` de IP desconhecido no
painel Segurança.

---

## Operação

### Deploy do monitoramento

`./bash/cli_app.sh` → 7. Valida, instala em `/opt/geosegbar-monitoring` e recria
os containers da stack (monitoramento fica cego uns 30 segundos). A versão
anterior fica em `/opt/geosegbar-monitoring.anterior`. O deploy da API não
mexe no monitoramento, e vice-versa.

### Mudar um painel

Edite `monitoring/grafana/dashboards-src/build_dashboards.py`, rode:

```bash
python3 monitoring/grafana/dashboards-src/build_dashboards.py
python3 monitoring/grafana/dashboards-src/check_dashboards.py
```

e commite o gerador e os JSON. Edição pela interface do Grafana está desligada:
o arquivo do git é a fonte.

### Mudar um alerta

`monitoring/prometheus/rules/*.yml` (métricas) ou `monitoring/loki/rules/fake/`
(logs). Toda regra precisa de `severity`, `summary`, `description` e
`runbook_url` apontando para uma seção deste arquivo. Sem `<`, `>` ou `&` nos
textos (o Telegram recebe HTML).

### Atualizar versões

Cada imagem em `monitoring/docker-compose.yml` está fixada por tag e digest.
Para atualizar, troque os dois juntos (o digest sai de
`https://hub.docker.com/v2/repositories/<imagem>/tags/<tag>`), leia as notas de
versão e faça o deploy. Revise uma vez por mês.

### Retenção e disco

| Onde | Variável | Padrão |
|---|---|---|
| Prometheus | `PROMETHEUS_RETENTION_TIME`, `PROMETHEUS_RETENTION_SIZE` (`monitoring.env`) | 30d, 8GB |
| Loki | `retention_period` em `monitoring/loki/loki.yml` | 720h |
| Tempo | `block_retention` em `monitoring/tempo/tempo.yml` | 168h |
| Amostragem de traces | `TRACING_SAMPLING_PROBABILITY` no `.env.prod` | 1.0 |

### Volumes antigos

O primeiro deploy do monitoramento remove os containers `prometheus-prod`,
`grafana-prod` e `postgres-exporter-prod` e mantém os volumes. Depois de 30
dias sem precisar deles:

```bash
docker volume rm prometheus-prod-data grafana-prod-data
```

### Fase 4: pg_stat_statements

`./bash/cli_app.sh` → 9. **Reinicia o Postgres** (a API devolve erro por 10 a 30
segundos): rode numa janela combinada. O script confere a biblioteca, faz
backup, mostra o SQL, pede `sim`, grava `shared_preload_libraries` com `ALTER
SYSTEM`, reinicia, cria a extensão e liga o coletor no exporter.

### Desfazer a Fase 4

Se o Postgres não voltar depois do reinício:

```bash
VOL=$(docker volume inspect postgres-prod-data -f '{{.Mountpoint}}')
sed -i '/shared_preload_libraries/d' "$VOL/postgresql.auto.conf"
docker start postgres-prod
```

### Usuário de túnel

Para dar acesso de emergência a alguém sem Tailscale, sem shell no servidor:

```bash
sudo bash bash/scripts/monitoring/setup_tunnel_user.sh /caminho/chave_da_pessoa.pub
```

A chave só abre túnel até `127.0.0.1:3000`.

---

## Problemas conhecidos

### node_exporter inalcançável

O deploy avisa quando o Prometheus não alcança `host.docker.internal:9100`. O
node_exporter escuta no IP da `docker0` e o firewall nftables (`geosegbar_fw`)
pode negar tráfego vindo das redes do Docker. Libere só a origem das pontes do
Docker para a porta 9100, por exemplo:

```bash
nft -a list table inet geosegbar_fw       # ache a cadeia de entrada
nft insert rule inet geosegbar_fw <cadeia> iifname "br-*" ip daddr <ip-da-docker0> tcp dport 9100 accept
```

`insert`, não `add`: `add` põe a regra depois do descarte final, e ela nunca
casa. Grave a mesma regra em `/etc/geosegbar/firewall.nft` para valer depois do
reboot.

### Traces não chegam

1. `docker inspect <container da API> | grep TRACING_ENABLED` deve dar `true`.
   Se `false`, o `geosegbar-alloy` não estava no ar no deploy da API: faça o
   deploy da API de novo.
2. `docker logs geosegbar-alloy` mostra erro de exportação para o Tempo?

### Logs não chegam

O Alloy lê os containers pelo `geosegbar-socket-proxy`. `docker logs
geosegbar-alloy` e `docker logs geosegbar-socket-proxy`. Logs do host (SSH,
kernel, ModSecurity) dependem dos caminhos `/var/log/secure`,
`/var/log/messages` e `/var/log/apache2/error_log`.

### cAdvisor não sobe

Ele roda sem `privileged`. Se o log acusar permissão em `/sys/fs/cgroup`,
acrescente `privileged: true` só nele, no `docker-compose.yml`, e faça o deploy.

---

## Segurança

- Nenhuma porta publicada na internet. O Grafana publica só em `127.0.0.1` e o
  time chega pelo Tailscale (`tailscale serve`, nunca `funnel`) ou por túnel SSH.
- O actuator da API escuta na 8081, que o nginx não encaminha, e expõe só
  `health`, `prometheus` e `info`.
- Rede `geosegbar-monitoring` interna, sem rota para a internet. Alertmanager,
  blackbox e Grafana saem pela rede `egress`; Prometheus, Alloy e os exporters
  de Postgres e Redis também estão na `geosegbar-network` (a da API), onde as
  portas deles ficam visíveis para os outros containers dessa rede, nunca para
  fora do servidor.
- Nenhum container recebe o socket do Docker direto: o socket-proxy aceita só
  leitura (`POST=0`) e fica numa rede própria (`docker-api`) que só Prometheus,
  Alloy e cAdvisor alcançam. Leitura em `/containers` já devolve o ambiente de
  qualquer container, segredos da API inclusive; por isso o isolamento.
- Segredos em `/etc/geosegbar/monitoring/secrets` (pasta 0700 de root),
  montados como compose secrets. Nada de segredo no repositório, que é público.
- Configuração em execução em `/opt/geosegbar-monitoring`, de root, fora da home
  do `wwvpsb`.
- Containers com `no-new-privileges`, sem capabilities quando a imagem permite,
  sistema de arquivos só de leitura e limite de memória.
- Grafana sem cadastro, sem anônimo, sem snapshots externos, sem dashboards
  públicos, sem plugin baixado em tempo de execução, sem edição pela UI.

### Dados pessoais (LGPD)

- Log nunca leva corpo de requisição, senha, token ou código. O Alloy troca
  qualquer JWT por `[jwt]` e o token do bot do Telegram por `[telegram-token]`;
  o nginx registra o caminho sem query string e troca o token das rotas de
  compartilhamento por `:token`.
- Na primeira leitura, o Alloy descarta logs com mais de 1 hora: o histórico
  antigo dos containers (com tokens no formato de log anterior) não entra no
  Loki.
- O usuário aparece por `userId`, nunca por e-mail ou nome.
- IP é guardado por 30 dias, para segurança.
- Mensagens de alerta levam nome, valor e link, nunca dado de cliente.
- O que sai do servidor: mensagens para o Telegram e pings para o
  healthchecks.io.

---

## Desenvolvimento local

```bash
cp monitoring/.env.dev.example monitoring/.env.dev
./monitoring/scripts/dev-secrets.sh
docker compose up -d
docker compose --env-file monitoring/.env.dev \
  -f monitoring/docker-compose.yml -f monitoring/docker-compose.dev.yml up -d
```

Grafana em `http://localhost:3000` (usuário `admin`, senha impressa pelo
`dev-secrets.sh`). Para ver traces em dev, ponha `TRACING_ENABLED=true` no
`.env` da raiz. O Alertmanager de dev descarta tudo.
