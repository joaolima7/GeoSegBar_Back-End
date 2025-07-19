#!/bin/bash

# performance_test_light.sh
echo "=== TESTE DE PERFORMANCE LEVE - CHECKLIST RESPONSE ==="
echo "Data: $(date)"
echo "==============================================="

BASE_URL="http://geometrisa-prod.com.br:9090/checklist-responses"
CONCURRENT_USERS=5  # Reduzido de 50 para 5
TOTAL_REQUESTS=50   # Reduzido de 1000 para 50
TIMEOUT=30          # Timeout de 30 segundos
JWT_TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJHZW9TZWdCYXIiLCJzdWIiOiJqY2dhbTNyNjlAZ21haWwuY29tIiwiZXhwIjoxNzUyOTkyMDAxfQ.Dzq2DWS5-ZsiIJl1qEZC7__zv6eMsF1kxPn4Zh5uwds"

# Função para testar endpoint
test_endpoint() {
    local endpoint=$1
    local description=$2
    local method=${3:-GET}
    local output_file="light_test_$(echo $description | tr ' ' '_' | tr '[:upper:]' '[:lower:]').txt"
    
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

# Testes dos endpoints mais críticos
echo "Iniciando testes de performance com carga reduzida..."

# 1. Buscar por barragem paginado (endpoint mais usado)
test_endpoint "/dam/3/paged?page=0&size=10" "Buscar respostas por barragem - paginado"

# 2. Buscar por cliente paginado 
test_endpoint "/client/1/paged?page=0&size=10" "Buscar respostas por cliente - paginado"

# 3. Buscar detalhes por barragem
test_endpoint "/dam/3/detail" "Buscar detalhes por barragem"

echo ""
echo "=== TESTE DE PERFORMANCE LEVE FINALIZADO ==="