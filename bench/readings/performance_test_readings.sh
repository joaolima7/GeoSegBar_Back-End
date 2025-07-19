#!/bin/bash

echo "=== TESTE DE PERFORMANCE - READINGS ==="
echo "Data: $(date)"
echo "==============================================="

BASE_URL="http://geometrisa-prod.com.br:9090/readings"
CONCURRENT_USERS=5
TOTAL_REQUESTS=50
TIMEOUT=30
JWT_TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJHZW9TZWdCYXIiLCJzdWIiOiJqY2dhbTNyNjlAZ21haWwuY29tIiwiZXhwIjoxNzUyOTkyMDAxfQ.Dzq2DWS5-ZsiIJl1qEZC7__zv6eMsF1kxPn4Zh5uwds"

# Função para testar endpoint
test_endpoint() {
    local endpoint=$1
    local description=$2
    local method=${3:-GET}
    local output_file="readings_test_$(echo $description | tr ' ' '_' | tr '[:upper:]' '[:lower:]').txt"
    
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
mkdir -p bench/readings
cd bench/readings

echo "Iniciando testes de performance para readings..."

# 1. Buscar leituras por instrumento (paginado)
test_endpoint "/instrument/1?page=0&size=20" "Buscar leituras por instrumento paginado"

# 2. Buscar leituras por instrumento (sem paginação)
test_endpoint "/instrument/1?page=0&size=100" "Buscar leituras por instrumento grandes"

# 3. Buscar leituras por output
test_endpoint "/output/1" "Buscar leituras por output"

# 4. Buscar leitura por ID
test_endpoint "/1" "Buscar leitura por ID"

# 5. Buscar status limite do instrumento
test_endpoint "/instrument/1/limit-status?limit=10" "Buscar status limite instrumento"

# 6. Buscar status limite de todos instrumentos do cliente
test_endpoint "/client/1/instruments-limit-status?limit=10" "Buscar status limite cliente"

# 7. Filtrar leituras com parâmetros
test_endpoint "/instrument/1?outputId=1&active=true&page=0&size=20" "Filtrar leituras por output ativo"

# 8. Filtrar leituras por data
test_endpoint "/instrument/1?startDate=2024-01-01&endDate=2024-12-31&page=0&size=20" "Filtrar leituras por período"

echo ""
echo "=== TESTE DE PERFORMANCE READINGS FINALIZADO ==="