# Backup do banco de produção

Duas cópias, verificadas, com alerta quando param de acontecer.

```bash
./bash/cli_app.sh
```

- **3) Backup banco produção** — roda agora
- **4) Verificar estado dos backups** — mostra o que existe de fato
- **5) Configurar cron de backup** — instala/corrige o agendamento

---

## O que aconteceu antes (e por que o desenho é assim)

Em 24/08/2026, ao conferir o servidor, o estado era:

- **Um único backup, de 20/04/2026** — 126 dias antes
- O cron existia, mas apontava para `/home/wwvpsb/backend.geometrisa-prod.com.br`,
  um diretório que **não existe mais**. O projeto está em `/home/wwvpsb/apps/api`
- Nenhum log de erro — o comando falhava antes de conseguir escrever no log
- Nenhum alerta, porque não havia nada que verificasse

Três lições viraram requisito:

1. **A cópia não pode viver só no servidor.** Se a VPS morre, o backup morre junto.
2. **Um alerta emitido pelo script de backup não basta.** Se o script não roda,
   ele não alerta. Precisa de um vigia independente.
3. **Backup não verificado não é backup.** Dump truncado e arquivo corrompido
   passam despercebidos até a hora em que você precisa restaurar.

---

## Como funciona agora

```
02:00  backup_database_prod.sh
       ├── confere espaço em disco antes de começar
       ├── pg_dump
       ├── verifica o marcador de conclusão do pg_dump  → dump truncado é recusado
       ├── gzip + gzip -t                                → arquivo corrompido é recusado
       ├── envia ao S3 (4 tentativas, espera crescente)
       ├── confere o tamanho no S3 contra o local        → upload parcial é recusado
       ├── rotaciona as cópias locais (mantém 7)
       └── grava logs/backup_status.json

09:00  check_backup_freshness.sh
       └── se o último sucesso tem mais de 48h, envia e-mail
```

O vigia das 9h é o ponto central: ele roda **independente** do backup. Se o cron
do backup sumir, quebrar ou apontar para o lugar errado de novo, o vigia percebe
a ausência e avisa. É exatamente o que faltava.

### Onde ficam as cópias

| Onde | Retenção | Para quê |
| --- | --- | --- |
| `~/db_backups/AAAA/MM/` | 7 arquivos | Restauração rápida, sem baixar nada |
| `s3://geosegbar-prod/db-backups/AAAA/MM/` | regra de ciclo de vida | Sobrevive à perda do servidor |

O prefixo `db-backups/` é separado dos arquivos do sistema (`anomalies/`, PSB,
imagens) justamente para permitir uma retenção própria.

---

## Por que não usamos URL pré-assinada, como no PSB

Faz sentido no PSB porque quem envia é o **navegador do usuário**, que não pode
ter as credenciais da AWS. A API assina, o navegador sobe direto ao S3.

Aqui quem envia é um script no próprio servidor, que **já tem as credenciais** no
`.env.prod`. Pedir uma URL pré-assinada à API significaria depender da API estar
no ar para conseguir fazer backup — e o momento em que você mais precisa de um
backup é exatamente aquele em que a aplicação está quebrada.

O envio é assinado localmente com `curl --aws-sigv4`. Sem dependência da API, sem
instalar a AWS CLI.

> **Detalhe de implementação:** o `--aws-sigv4` do curl 7.76 não envia o header
> `x-amz-content-sha256`, que o S3 exige — sem ele, a resposta é
> `400 InvalidRequest`. O script envia explicitamente: o hash do arquivo no PUT
> (o que faz o S3 recusar o objeto se ele chegar corrompido) e o hash do payload
> vazio nas requisições sem corpo.

---

## Configuração

Variáveis opcionais no `.env.prod`:

| Variável | Padrão | Para quê |
| --- | --- | --- |
| `BACKUP_ALERT_EMAIL` | `MAIL_USERNAME` | Destinatário dos alertas de falha |
| `BACKUP_BASE_DIR` | `~/db_backups` | Onde ficam as cópias locais |
| `BACKUP_KEEP_COUNT` | `7` | Quantas cópias locais manter |
| `BACKUP_S3_ENABLED` | `true` | Desliga o envio ao S3 |
| `BACKUP_S3_PREFIX` | `db-backups` | Prefixo no bucket |
| `BACKUP_S3_MAX_ATTEMPTS` | `4` | Tentativas de envio |
| `BACKUP_MAX_AGE_HOURS` | `48` | A partir de quando o vigia reclama |
| `BACKUP_CRON_SCHEDULE` | `0 2 * * *` | Horário do backup |
| `BACKUP_CHECK_CRON_SCHEDULE` | `0 9 * * *` | Horário da verificação |

**Defina `BACKUP_ALERT_EMAIL`.** Sem ele, os alertas vão para o remetente
configurado no SMTP, que provavelmente não é quem precisa ver.

### Regra de ciclo de vida no S3 (recomendada)

A retenção no S3 fica por conta do bucket, não do script — apagar backup por
script é justamente o tipo de automação que, com um bug, remove tudo de uma vez.

No console da AWS, em **S3 → geosegbar-prod → Management → Lifecycle rules**,
crie uma regra com prefixo `db-backups/`:

- Transição para *Glacier Instant Retrieval* após 30 dias
- Expiração após 365 dias

Ajuste conforme a exigência regulatória de vocês. Uma barragem tem obrigação
legal de guardar histórico — vale confirmar o prazo antes de definir a expiração.

---

## Estados possíveis

O `backup_status.json` registra um destes:

| Resultado | Significa | O que fazer |
| --- | --- | --- |
| `sucesso` | Dump verificado, local e no S3 | Nada |
| `parcial` | Dump local OK, **envio ao S3 falhou** | Investigar rede/credenciais; existe só a cópia local |
| `falha` | Nem o dump local foi feito | Urgente — sem backup dessa data |

Em `parcial` e `falha` o script sai com código diferente de zero e envia e-mail.

---

## Restauração

```bash
# 1. Baixar do S3, se necessário
curl --fail --output backup.sql.gz \
  --aws-sigv4 "aws:amz:us-east-1:s3" \
  --user "$AWS_ACCESS_KEY_ID:$AWS_SECRET_ACCESS_KEY" \
  --header "x-amz-content-sha256: e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855" \
  "https://geosegbar-prod.s3.us-east-1.amazonaws.com/db-backups/2026/08/geosegbar_backup_XXXX.sql.gz"

# 2. Conferir a integridade ANTES de mexer no banco
gzip -t backup.sql.gz && echo "arquivo íntegro"

# 3. Restaurar
gunzip -c backup.sql.gz | docker exec -i postgres-prod psql -U postgres -d geosegbar_prod
```

**Restaurar é operação destrutiva.** Segue as regras de
[ACESSO_SERVIDOR.md](ACESSO_SERVIDOR.md): mostrar o comando exato e obter
autorização antes de executar. Na prática, o certo é restaurar primeiro num banco
temporário e conferir os dados, antes de tocar em produção.

---

## Conferindo que está tudo certo

```bash
./bash/cli_app.sh
```

Opção **4) Verificar estado dos backups**. Mostra:

- Se o cron existe e se aponta para arquivos que existem de fato
- Quando foi a última execução e com que resultado
- As cópias locais mais recentes
- As cópias no S3 mais recentes

Direto por SSH:

```bash
ssh geosegbar-vps 'cd /home/wwvpsb/apps/api && bash bash/scripts/verify_backups.sh'
```

Vale rodar uma vez por mês. Backup é a coisa que todo mundo assume que funciona
até o dia em que precisa — e aí é tarde.
