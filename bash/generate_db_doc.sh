#!/bin/bash

set -e

echo "📊 Gerando documentação do banco de dados com TBLS..."
echo ""

# Limpar documentação anterior
rm -rf ./db-documentation
mkdir -p ./db-documentation

# String de conexão PostgreSQL
DB_URL="postgres://postgres:Geometr!s@@localhost:5433/geosegbar_dev?sslmode=disable"

# Gerar documentação
tbls doc "$DB_URL" ./db-documentation

echo ""
echo "✅ Documentação gerada com sucesso!"
echo ""
echo "📂 Arquivos gerados:"
echo "   📄 README.md (Markdown completo)"
echo "   📊 schema.svg (Diagrama ER)"
echo "   📋 Cada tabela em detalhes"
echo ""

# Converter Markdown para HTML (opcional)
if command -v pandoc &> /dev/null; then
    cd db-documentation
    pandoc README.md -o index.html --metadata title="GeoSegBar Database" --standalone
    cd ..
    echo "📄 HTML gerado: db-documentation/index.html"
    open db-documentation/index.html
else
    echo "📄 Documentação Markdown: db-documentation/README.md"
    open db-documentation/README.md
fi

echo ""
echo "🎉 Concluído!"