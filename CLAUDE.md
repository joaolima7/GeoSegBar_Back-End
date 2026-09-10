# GeoSegBar API — backend

Sistema de gestão de segurança de barragens (Geometrisa). Spring Boot 3.4.2 ·
Java 21 · Maven · PostgreSQL · Flyway. `spring.application.name=geosegbar-api`,
porta padrão `9090` (`SERVER_PORT`).

## Onde procurar antes de perguntar

`docs/` já documenta a operação — leia antes de assumir:

- `docs/DEPLOY.md` — como sobe em produção
- `docs/MIGRACOES.md` — o runbook de Flyway (ver a armadilha abaixo)
- `docs/ACESSO_SERVIDOR.md` — acesso ao servidor
- `docs/BACKUP.md` — rotina de backup
- `docs/PRESIGNED_UPLOAD_GUIDE.md` — upload de arquivo (PSB, fotos de anomalia)
- `docs/checklist/`, `docs/db/`, `docs/audit/` — mais fundo em cada área

`ENTREGA-BACKEND.md` e `SOLICITACOES-BACKEND.md`, na raiz: o contrato vivo
entre este backend e o app mobile — pedido do time mobile de um lado, resposta
item a item do backend do outro. Confira ali antes de mudar contrato de rota
que o app já consome.

## 🔴 Migração de banco: Flyway e Hibernate convivem, de propósito

`ddl-auto=update` cria tabela e coluna nova a partir das entidades — é o que
mantém o banco em dia sem migração pra cada campo. Flyway
(`src/main/resources/db/migration/V*.sql`) é só para o que o Hibernate não
sabe fazer sozinho: backfill de dado, mudança que não é "coluna nova", índice
específico. Ver `docs/MIGRACOES.md` antes de escrever uma migração — a divisão
de responsabilidade entre os dois está documentada lá, e não é óbvia.

## Estrutura

`src/main/java/com/geosegbar/`:
- `entities/` — as entidades JPA
- `infra/<dominio>/` — um pacote por domínio (dam, instrument, reading,
  checklist, anomaly, pae, psb, user, roles, permissions, audit, ...),
  cada um tipicamente com controller/service/repository/dto juntos
- `configs/` — configuração (segurança, CORS, etc.)
- `common/`, `exceptions/` — utilitário e tratamento de erro compartilhado

## Ambiente

Variáveis em `.env` (fora do git — `.env.example` tem as chaves, sem valor).
Principais: `DB_HOST/PORT/NAME/USERNAME/PASSWORD`, `SPRING_PROFILES_ACTIVE`,
`SERVER_PORT`, `JWT_SECRET`, `FRONTEND_URL`, `MAIL_*`, `FILE_*` (upload).

## Registro de trabalho no MySpace

Ao terminar uma tarefa (ou antes de dar push), use o skill
`registrar-trabalho` (`.claude/skills/registrar-trabalho/SKILL.md`) — ele
pergunta um título e um contexto curto, sem jargão técnico, e cria ou avança
um chamado no board do cliente. É a matéria-prima que o funcionário de IA Alex
usa pra montar o relatório diário. Um chamado por tarefa (a branch é a chave
de dedupe) — não crie um card por detalhe pequeno.
