# Dashboards do Grafana como código

Os painéis do Grafana não são editados na interface. Cada dashboard é definido
em Python e o JSON que o Grafana carrega é gerado a partir dele.

| Pasta / arquivo | Para quê |
|---|---|
| `build_dashboards.py` | Define os 10 dashboards e escreve os JSON |
| `check_dashboards.py` | Valida os JSON gerados (uid, ids, datasources, PromQL, variáveis) |
| `../dashboards/*.json` | Saída gerada, é o que o Grafana lê |

## Como alterar um painel

1. Edite `build_dashboards.py`. Cada dashboard é uma função (`overview()`,
   `endpoints()`, `errors()`...) que monta painéis com `stat()`, `timeseries()`,
   `table()`, `logs()`, `gauge()`, `bargauge()`, `heatmap()`, `pie()`, `text()`,
   `traces_table()` e `row()`. A posição é automática, da esquerda para a direita
   numa grade de 24 colunas; painéis da mesma linha precisam ter a mesma altura.
2. Gere os JSON:

   ```bash
   python3 monitoring/grafana/dashboards-src/build_dashboards.py
   ```

3. Valide. O comando sai com código diferente de zero se houver erro:

   ```bash
   python3 monitoring/grafana/dashboards-src/check_dashboards.py
   ```

4. Faça commit do `.py` e dos `.json` alterados juntos. Rodar o build duas vezes
   gera arquivos idênticos, então um diff nos JSON mostra só o que você mudou.

Só biblioteca padrão, Python 3.9 ou mais novo. Dentro das expressões, `@A` vira
`job="geosegbar-api"` e `@N` vira `container="nginx-prod"`.

## Regras que o validador cobra

- toda `description` em português, uma frase dizendo como ler o painel;
- datasource `{type, uid}` com uid `prometheus`, `loki` ou `tempo` em painéis e alvos;
- série de latência (`histogram_quantile` em `timeseries`) com `"exemplar": true`;
- `$variavel` usada na consulta precisa existir em `templating`;
- uid com até 40 caracteres, igual a `gsb-<nome do arquivo>`.

## Editar pela interface do Grafana não vale

Em produção os dashboards são provisionados com `allowUiUpdates: false` e o JSON
tem `"editable": false`. Quem alterar um painel pela tela perde a mudança no
próximo restart, ou nem consegue salvar. Para testar uma ideia, copie o painel para
um dashboard seu (Salvar como), ajuste, e depois passe o resultado para o
`build_dashboards.py`.
