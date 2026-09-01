# Acesso ao servidor de produção (VPS Hostgator)

Como acessar o servidor onde a API roda, o que é permitido fazer lá dentro e o
que exige autorização explícita antes.

Vale para qualquer pessoa — e para qualquer assistente de IA — que precise
investigar um incidente em produção.

---

## As regras, antes de qualquer comando

Estas regras não são recomendação. São a condição para ter esse acesso.

### Liberado sem pedir nada

Tudo que **apenas lê** e não altera estado:

- Ler logs (`docker logs`, arquivos em `logs/`)
- Inspecionar containers (`docker ps`, `docker inspect`, `docker stats`)
- Consultas `SELECT` no banco
- Ler arquivos de configuração
- Ver uso de disco, memória, processos

### Exige autorização explícita, sempre

Qualquer coisa que **mude** alguma coisa, por menor que pareça:

- **Banco de dados:** `INSERT`, `UPDATE`, `DELETE`, `TRUNCATE`, DDL
  (`CREATE`/`ALTER`/`DROP`), rodar migração à mão
- **Containers:** `restart`, `stop`, `start`, `rm`, `docker compose up/down`,
  rebuild, deploy
- **Arquivos:** editar qualquer arquivo, inclusive `.env.prod`
- **Sistema:** instalar pacote, mudar configuração, cron, firewall, usuários
- **Serviços externos:** qualquer chamada que escreva (S3, e-mail, API da ANA)

O pedido de autorização mostra **o comando exato** que será executado, e só
prossegue depois de um "pode" claro. Autorização vale para aquela operação
específica — não abre precedente para as próximas.

### Nunca

- Apagar dado de produção sem backup verificado na mão
- Rodar comando destrutivo "para testar"
- Colar senha, chave privada ou token em chat, log ou commit
- Deixar terminal logado como root sem necessidade

Na dúvida entre "isso é leitura?" e "isso altera?", trate como alteração e
pergunte.

---

## Configuração do acesso (uma vez)

O acesso é por **chave SSH**, nunca por senha. Senha em chat vaza; chave não sai
da máquina.

### 1. Gerar o par de chaves (na máquina de quem vai acessar)

```bash
ssh-keygen -t ed25519 -f ~/.ssh/geosegbar_vps -N "" -C "seu-nome@geosegbar"
```

Isso cria dois arquivos:

| Arquivo | O que é | Pode compartilhar? |
| --- | --- | --- |
| `~/.ssh/geosegbar_vps` | chave **privada** | **Nunca.** Não sai da máquina |
| `~/.ssh/geosegbar_vps.pub` | chave **pública** | Sim — é ela que vai para o servidor |

### 2. Instalar a chave pública no servidor

Pelo WHM (`https://<IP-DO-SERVIDOR>:2087`):

1. **Home → Security Center → Manage root's SSH Keys**
2. **Import Key**
3. Cole o conteúdo de `~/.ssh/geosegbar_vps.pub`
4. Depois de importar, clique em **Authorize** na chave

Ou, com acesso a um terminal do servidor:

```bash
mkdir -p ~/.ssh && chmod 700 ~/.ssh
echo "<conteúdo do .pub>" >> ~/.ssh/authorized_keys
chmod 600 ~/.ssh/authorized_keys
```

### 3. Registrar os atalhos em `~/.ssh/config`

Dois ambientes, dois atalhos. **Confira sempre em qual você está** antes de
rodar qualquer coisa — os caminhos do projeto são diferentes.

```
Host geosegbar-vps
    HostName 129.121.35.91
    User root
    Port 22022
    IdentityFile ~/.ssh/geosegbar_vps
    IdentitiesOnly yes
    ServerAliveInterval 30

Host geosegbar-homolog
    HostName 162.240.165.193
    User wwgeomprod
    Port 22022
    IdentityFile ~/.ssh/geosegbar_vps
    IdentitiesOnly yes
    ServerAliveInterval 30
```

| | Produção | Homologação |
| --- | --- | --- |
| Atalho | `geosegbar-vps` | `geosegbar-homolog` |
| Host | `vps-14922836` | `vps-13448162` |
| Usuário | `root` | `wwgeomprod` |
| Projeto | `/home/wwvpsb/apps/api` | `/home/wwgeomprod/backend.geometrisa-prod.com.br` |
| Bucket S3 | `geosegbar-prod` | `geosegbar-homolog` |
| Backups | `/root/db_backups` | `/home/wwgeomprod/db_backups` |

O nome do host de homologação contém "prod" — `vps-13448162.geometrisa-prod.com.br`.
Não se guie por ele; confira o número da VPS ou o caminho do projeto.

### 4. Testar

```bash
ssh geosegbar-vps 'hostname'
ssh geosegbar-homolog 'hostname'
```

### Instalando a chave sem WHM

Se o acesso for por usuário comum (caso de homologação), a chave vai direto pelo
terminal, sem passar pelo WHM:

```bash
mkdir -p ~/.ssh && chmod 700 ~/.ssh
echo '<conteúdo do .pub>' >> ~/.ssh/authorized_keys
chmod 600 ~/.ssh/authorized_keys
```

---

## Investigando um incidente

Todos os comandos desta seção são de leitura.

### Situação geral

```bash
ssh geosegbar-vps 'docker ps --format "table {{.Names}}\t{{.Status}}\t{{.RunningFor}}"'
```

### Logs da API

O container é `geosegbar-api-prod`.

```bash
# Últimas 200 linhas
ssh geosegbar-vps 'docker logs --tail 200 geosegbar-api-prod'

# Uma janela de tempo específica (formato UTC ou ISO local)
ssh geosegbar-vps 'docker logs --since 2026-08-19T00:00:00 --until 2026-08-20T00:00:00 geosegbar-api-prod'

# Filtrando por assunto — o log da coleta histórica usa emojis como marcador
ssh geosegbar-vps 'docker logs --since 2026-08-19 geosegbar-api-prod 2>&1 | grep -E "job|ANA|Token|429|❌|⚠️"'
```

Há também volume de log montado em `logs/` na raiz do projeto no servidor.

### Banco de dados (somente `SELECT`)

O container é `postgres-prod`. As credenciais estão em `.env.prod` no servidor —
não precisam ser copiadas para lugar nenhum.

```bash
ssh geosegbar-vps 'docker exec postgres-prod psql -U "$DB_USERNAME" -d "$DB_NAME" -c "SELECT 1;"'
```

Na prática, é mais confiável carregar o `.env.prod` antes:

```bash
ssh geosegbar-vps 'cd ~/GeoSegBar_Back-End && set -a && . ./.env.prod && set +a && \
  docker exec postgres-prod psql -U "$DB_USERNAME" -d "$DB_NAME" -c "SELECT ..."'
```

**Só `SELECT`.** Qualquer escrita passa pelo fluxo de autorização acima.

### Recursos do servidor

```bash
ssh geosegbar-vps 'df -h / && free -m && docker stats --no-stream'
```

---

## Consultas úteis para o job de coleta histórica da ANA

```sql
-- Estado dos jobs, do mais recente para o mais antigo
SELECT id, instrument_id, instrument_name, status, start_date, end_date,
       checkpoint_date, total_created, total_skipped, retry_count,
       started_at, finished_at, error_message
FROM historical_data_job
ORDER BY id DESC
LIMIT 20;
```

```sql
-- Até onde a coleta chegou, por instrumento
SELECT i.id, i.name, i.linimetric_ruler_code,
       MIN(r.date) AS primeira_leitura,
       MAX(r.date) AS ultima_leitura,
       COUNT(*)    AS total
FROM reading r
JOIN instrument i ON i.id = r.instrument_id
WHERE r.comment = 'Coleta histórica automática ANA'
GROUP BY i.id, i.name, i.linimetric_ruler_code
ORDER BY i.id;
```

```sql
-- Buracos na série: anos sem nenhuma leitura coletada
SELECT EXTRACT(YEAR FROM r.date) AS ano, COUNT(*) AS leituras
FROM reading r
WHERE r.instrument_id = :instrumentId
GROUP BY 1
ORDER BY 1;
```

```sql
-- Falhas registradas na auditoria
SELECT created_at, action, status, message, error_message
FROM audit_log
WHERE action = 'JOB_HISTORICAL_DATA'
ORDER BY created_at DESC
LIMIT 50;
```

---

## Se precisar mesmo alterar algo

O caminho é sempre o mesmo:

1. **Descrever** o que vai ser feito e por quê.
2. **Mostrar o comando exato** (ou o SQL exato).
3. **Esperar autorização** explícita.
4. Para mudança em banco, **fazer backup antes**:
   `./bash/cli_app.sh` → opção 2.
5. Executar e **mostrar o resultado**.

Deploy e migração de banco têm caminho próprio, documentado em
[MIGRACOES.md](MIGRACOES.md) — não se aplica SQL de migração à mão.

---

## Revogando um acesso

Pelo WHM: **Security Center → Manage root's SSH Keys → Deauthorize / Delete**.

Ou no servidor, removendo a linha correspondente de `~/.ssh/authorized_keys`.

Revogue quando a pessoa sair do projeto, quando a máquina que guardava a chave
privada for trocada, ou ao menor sinal de que a chave privada vazou.
