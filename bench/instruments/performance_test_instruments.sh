#!/bin/bash

echo "=== TESTE DE PERFORMANCE - INSTRUMENTS ==="
echo "Data: $(date)"
echo "==============================================="

BASE_URL="http://geometrisa-prod.com.br:9090/instruments"
CONCURRENT_USERS=5
TOTAL_REQUESTS=50
TIMEOUT=30
JWT_TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJHZW9TZWdCYXIiLCJzdWIiOiJqY2dhbTNyNjlAZ21haWwuY29tIiwiZXhwIjoxNzUyOTkyMDAxfQ.Dzq2DWS5-ZsiIJl1qEZC7__zv6eMsF1kxPn4Zh5uwds"

# Função para testar endpoint
test_endpoint() {
    local endpoint=$1
    local description=$2
    local method=${3:-GET}
    local output_file="instruments_test_$(echo $description | tr ' ' '_' | tr '[:upper:]' '[:lower:]').txt"
    
    echo ""
    echo "--- Testando: $description ---"
    echo "Endpoint: $endpoint"
    echo "Método: $method"
    echo "Usuários simultâneos: $CONCURRENT_USERS"
    echo "Total de requisições: $TOTAL_REQUESTS"
    echo "Timeout: ${TIMEOUT}s"
    echo ""
    
    if [ "$method" = "GET" ]; then
        ab -n $TOTAL_REQUESTS -c $CONCURRENT_USERS -s $TIMEOUT \
           -H "Authorization: Bearer $JWT_TOKEN" \
           -H "Content-Type: application/json" \
           "$BASE_URL$endpoint" > "$output_file" 2>&1
    fi
    
    # Mostrar resultados imediatamente
    if [ -f "$output_file" ]; then
        echo "Resultados salvos em: $output_file"
        echo "RPS: $(grep 'Requests per second' $output_file | awk '{print $4}' 2>/dev/null || echo 'N/A')"
        echo "Tempo médio: $(grep 'Time per request' $output_file | head -1 | awk '{print $4}' 2>/dev/null || echo 'N/A')ms"
        echo "Falhas: $(grep 'Failed requests' $output_file | awk '{print $3}' 2>/dev/null || echo 'N/A')"
    fi
    
    echo ""
    echo "=========================================="
}

# Criar diretório se não existir
mkdir -p bench/instruments
cd bench/instruments

echo "Iniciando testes de performance para instruments..."

# 1. Buscar todos os instrumentos
test_endpoint "" "Buscar todos instrumentos"

# 2. Buscar instrumentos por barragem
test_endpoint "/dam/1" "Buscar instrumentos por barragem"

# 3. Buscar instrumento por ID (com detalhes completos)
test_endpoint "/1" "Buscar instrumento por ID com detalhes"

# 4. Buscar instrumentos por cliente
test_endpoint "/client/1?active=true" "Buscar instrumentos por cliente ativo"

# 5. Filtrar instrumentos
test_endpoint "/filter?damId=1&active=true" "Filtrar instrumentos por barragem ativa"

# 6. Filtrar instrumentos complexo
test_endpoint "/filter?clientId=1&instrumentType=Piezômetro&active=true" "Filtrar instrumentos complexo"

echo ""
echo "=== TESTE DE PERFORMANCE INSTRUMENTS FINALIZADO ==="