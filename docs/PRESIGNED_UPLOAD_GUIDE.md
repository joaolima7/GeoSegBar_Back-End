# Guia de Upload PSB — Presigned Multipart Upload

## Resumo

O upload de arquivos PSB agora é feito **diretamente para o S3** via URLs pré-assinadas.
O Spring apenas coordena (gera URLs e registra no banco). **Laravel nunca envia bytes para o Spring.**

**TODOS os uploads usam multipart** — sem exceção, independente do tamanho do arquivo.

---

## Fluxo (3 passos)

```
Laravel                       Spring API                    AWS S3
  |                               |                            |
  |── POST /init/{folderId} ────→|                            |
  |←── { uploadId, parts[] } ────|                            |
  |                               |                            |
  |── PUT part 1 ────────────────────────────────────────────→|
  |←── 200 + ETag ←──────────────────────────────────────────|
  |── PUT part 2 ────────────────────────────────────────────→|
  |←── 200 + ETag ←──────────────────────────────────────────|
  |── PUT part N... ─────────────────────────────────────────→|
  |                               |                            |
  |── POST /complete/{folderId} →|── CompleteMultipart ──────→|
  |←── PSBFileEntity ←───────────|←── 200 OK ←───────────────|
```

---

## Passo 1: INICIAR UPLOAD

```http
POST /psb/files/presigned/init/{folderId}
Content-Type: application/json
Authorization: Bearer {token}
```

**Body:**
```json
{
  "filename": "relatorio_mensal.pdf",
  "fileSize": 52428800,
  "contentType": "application/pdf",
  "uploadedById": 123
}
```

| Campo | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| `filename` | string | Sim | Nome original do arquivo |
| `fileSize` | long | Sim | Tamanho em **bytes** |
| `contentType` | string | Sim | MIME type (`application/pdf`, `image/png`, etc) |
| `uploadedById` | long | Sim | ID do usuário que faz upload |

**Resposta (SEMPRE multipart):**
```json
{
  "data": {
    "uploadId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "s3Key": "psb/barragem_x/1708300000_relatorio_mensal.pdf",
    "uploadUrl": null,
    "s3UploadId": "AbcD1234567890ExampleUploadId",
    "partSize": 52428800,
    "totalParts": 1,
    "parts": [
      {
        "partNumber": 1,
        "uploadUrl": "https://geosegbar-arquivos-prod.s3.amazonaws.com/psb/...?partNumber=1&uploadId=...&X-Amz-Algorithm=..."
      }
    ],
    "multipart": true
  },
  "message": "Upload pré-assinado inicializado com sucesso!",
  "success": true
}
```

**Exemplo para arquivo grande (150MB → 3 partes de 50MB):**
```json
{
  "data": {
    "uploadId": "...",
    "s3Key": "psb/barragem_x/1708300000_video.mp4",
    "s3UploadId": "AbcD1234567890",
    "partSize": 52428800,
    "totalParts": 3,
    "parts": [
      { "partNumber": 1, "uploadUrl": "https://geosegbar-arquivos-prod.s3.amazonaws.com/...?partNumber=1&..." },
      { "partNumber": 2, "uploadUrl": "https://geosegbar-arquivos-prod.s3.amazonaws.com/...?partNumber=2&..." },
      { "partNumber": 3, "uploadUrl": "https://geosegbar-arquivos-prod.s3.amazonaws.com/...?partNumber=3&..." }
    ],
    "multipart": true
  }
}
```

**Part Size dinâmico:**

| Tamanho do arquivo | Part size | Partes |
|---|---|---|
| Até 50MB | arquivo inteiro (1 parte) | 1 |
| 50MB – 500MB | 50MB | 1 – 10 |
| > 500MB | 100MB | N |

---

## Passo 2: ENVIAR PARTES PARA O S3

Para **cada parte** em `response.data.parts[]`, faça um PUT direto na `uploadUrl`:

```http
PUT {part.uploadUrl}
Content-Type: application/pdf
Content-Length: {tamanhoDaParte}

[bytes da parte]
```

**⚠️ CRITICAL: Coletar o `ETag` do header da resposta S3!**

Resposta S3 por parte:
```http
HTTP/1.1 200 OK
ETag: "1234567890abcdef1234567890abcdef"
```

**Regras:**
- O `ETag` vem entre aspas duplas — remover as aspas antes de enviar no complete
- A **última parte** pode ser menor que `partSize` (o resto do arquivo)
- As partes podem ser enviadas **em paralelo** para máxima performance
- Timeout de cada PUT: 5 minutos (ajustar conforme velocidade da rede)
- As URLs expiram em **15 minutos** — completar todas as partes antes disso

### PHP/Laravel — Upload das partes

```php
function uploadParts(array $initResponse, string $filePath): array
{
    $client = new \GuzzleHttp\Client([
        'timeout' => 300, // 5 min por parte
        'connect_timeout' => 10,
    ]);

    $completedParts = [];
    $fileHandle = fopen($filePath, 'rb');
    $partSize = $initResponse['data']['partSize'];

    foreach ($initResponse['data']['parts'] as $part) {
        $data = fread($fileHandle, $partSize);

        $putResponse = $client->put($part['uploadUrl'], [
            'body' => $data,
            'headers' => [
                'Content-Length' => strlen($data),
            ],
        ]);

        // ⚠️ OBRIGATÓRIO: coletar ETag (sem aspas)
        $eTag = trim($putResponse->getHeader('ETag')[0], '"');

        $completedParts[] = [
            'partNumber' => $part['partNumber'],
            'eTag' => $eTag,
        ];
    }

    fclose($fileHandle);
    return $completedParts;
}
```

### Upload em PARALELO (Guzzle Promises) — Recomendado

```php
use GuzzleHttp\Client;
use GuzzleHttp\Promise\Utils;

function uploadPartsParallel(array $initResponse, string $filePath): array
{
    $client = new Client(['timeout' => 300, 'connect_timeout' => 10]);
    $completedParts = [];
    $partSize = $initResponse['data']['partSize'];

    // Ler partes em array
    $fileHandle = fopen($filePath, 'rb');
    $partsData = [];
    foreach ($initResponse['data']['parts'] as $part) {
        $partsData[] = [
            'partNumber' => $part['partNumber'],
            'uploadUrl' => $part['uploadUrl'],
            'data' => fread($fileHandle, $partSize),
        ];
    }
    fclose($fileHandle);

    // Enviar todas as partes em paralelo (max 4 concurrent)
    $promises = [];
    foreach ($partsData as $part) {
        $promises[$part['partNumber']] = $client->putAsync($part['uploadUrl'], [
            'body' => $part['data'],
            'headers' => ['Content-Length' => strlen($part['data'])],
        ]);
    }

    // Aguardar todas completarem
    $results = Utils::settle($promises)->wait();

    foreach ($results as $partNumber => $result) {
        if ($result['state'] === 'fulfilled') {
            $eTag = trim($result['value']->getHeader('ETag')[0], '"');
            $completedParts[] = [
                'partNumber' => (int) $partNumber,
                'eTag' => $eTag,
            ];
        } else {
            throw new \RuntimeException("Falha no upload da parte $partNumber: " . $result['reason']->getMessage());
        }
    }

    // Ordenar por partNumber
    usort($completedParts, fn($a, $b) => $a['partNumber'] <=> $b['partNumber']);
    return $completedParts;
}
```

---

## Passo 3: CONFIRMAR UPLOAD

```http
POST /psb/files/presigned/complete/{folderId}
Content-Type: application/json
Authorization: Bearer {token}
```

**Body:**
```json
{
  "uploadId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "completedParts": [
    { "partNumber": 1, "eTag": "1234567890abcdef1234567890abcdef" },
    { "partNumber": 2, "eTag": "abcdef1234567890abcdef1234567890" },
    { "partNumber": 3, "eTag": "fedcba0987654321fedcba0987654321" }
  ]
}
```

| Campo | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| `uploadId` | string | Sim | O `uploadId` retornado no init |
| `completedParts` | array | Sim | Array com todas as partes (partNumber + eTag) |
| `completedParts[].partNumber` | int | Sim | Número da parte (começa em 1) |
| `completedParts[].eTag` | string | Sim | ETag da resposta S3 (SEM aspas) |

**Nota:** Mesmo para arquivos com 1 parte, enviar `completedParts` com 1 item.

**Resposta:**
```json
{
  "data": {
    "id": 456,
    "filename": "1708300000_relatorio_mensal.pdf",
    "originalFilename": "relatorio_mensal.pdf",
    "filePath": "psb/barragem_x/1708300000_relatorio_mensal.pdf",
    "downloadUrl": "https://geosegbar-arquivos-prod.s3.us-east-1.amazonaws.com/psb/barragem_x/1708300000_relatorio_mensal.pdf",
    "contentType": "application/pdf",
    "size": 52428800,
    "uploadedAt": "2026-02-18T12:00:00",
    "psbFolder": { "id": 10 },
    "uploadedBy": { "id": 123 }
  },
  "message": "Upload pré-assinado confirmado com sucesso!",
  "success": true
}
```

---

## (Opcional) CANCELAR UPLOAD

Se o usuário cancelar ou ocorrer erro antes do complete:

```http
POST /psb/files/presigned/abort/{uploadId}
Authorization: Bearer {token}
```

**Importante:** Chamar abort libera os fragmentos no S3. Se não chamar, expira automaticamente em 2 horas.

---

## Fluxo Completo (Laravel Controller)

```php
<?php

namespace App\Http\Controllers;

use GuzzleHttp\Client;
use GuzzleHttp\Promise\Utils;
use Illuminate\Http\Request;

class PSBUploadController extends Controller
{
    private string $apiBaseUrl = 'https://geometrisa-prod.com.br/api';

    public function upload(Request $request, int $folderId)
    {
        $file = $request->file('file');
        $token = $request->bearerToken();

        // 1. INIT — pedir URLs pré-assinadas ao Spring
        $initResponse = $this->apiPost("/psb/files/presigned/init/$folderId", [
            'filename' => $file->getClientOriginalName(),
            'fileSize' => $file->getSize(),
            'contentType' => $file->getMimeType(),
            'uploadedById' => auth()->id(),
        ], $token);

        try {
            // 2. UPLOAD — enviar partes em paralelo direto ao S3
            $completedParts = $this->uploadPartsParallel($initResponse, $file->getRealPath());

            // 3. COMPLETE — confirmar no Spring (salva no banco)
            $completeResponse = $this->apiPost("/psb/files/presigned/complete/$folderId", [
                'uploadId' => $initResponse['data']['uploadId'],
                'completedParts' => $completedParts,
            ], $token);

            return response()->json($completeResponse, 201);

        } catch (\Exception $e) {
            // Em caso de erro, cancelar o multipart no S3
            $uploadId = $initResponse['data']['uploadId'];
            $this->apiPost("/psb/files/presigned/abort/$uploadId", [], $token);
            throw $e;
        }
    }

    private function uploadPartsParallel(array $initResponse, string $filePath): array
    {
        $client = new Client(['timeout' => 300, 'connect_timeout' => 10]);
        $partSize = $initResponse['data']['partSize'];
        $completedParts = [];

        $fileHandle = fopen($filePath, 'rb');
        $promises = [];

        foreach ($initResponse['data']['parts'] as $part) {
            $data = fread($fileHandle, $partSize);
            $promises[$part['partNumber']] = $client->putAsync($part['uploadUrl'], [
                'body' => $data,
                'headers' => ['Content-Length' => strlen($data)],
            ]);
        }
        fclose($fileHandle);

        $results = Utils::settle($promises)->wait();

        foreach ($results as $partNumber => $result) {
            if ($result['state'] !== 'fulfilled') {
                throw new \RuntimeException("Falha na parte $partNumber");
            }
            $eTag = trim($result['value']->getHeader('ETag')[0], '"');
            $completedParts[] = [
                'partNumber' => (int) $partNumber,
                'eTag' => $eTag,
            ];
        }

        usort($completedParts, fn($a, $b) => $a['partNumber'] <=> $b['partNumber']);
        return $completedParts;
    }

    private function apiPost(string $endpoint, array $body, string $token): array
    {
        $client = new Client(['timeout' => 30, 'connect_timeout' => 5]);
        $response = $client->post($this->apiBaseUrl . $endpoint, [
            'json' => $body,
            'headers' => [
                'Authorization' => "Bearer $token",
                'Content-Type' => 'application/json',
                'Accept' => 'application/json',
            ],
        ]);
        return json_decode($response->getBody()->getContents(), true);
    }
}
```

---

## DELETE — Excluir Arquivo

O delete não mudou, continua igual:

```http
DELETE /psb/files/{fileId}
Authorization: Bearer {token}
```

Resposta:
```json
{
  "data": null,
  "message": "Arquivo PSB excluído com sucesso!",
  "success": true
}
```

---

## Tratamento de Erros

| HTTP Status | Erro | Causa | Solução |
|---|---|---|---|
| 400 | Campo obrigatório faltando | `filename`, `fileSize`, `contentType` ou `uploadedById` ausente | Enviar todos os campos |
| 400 | Upload não encontrado ou expirado | `uploadId` inválido ou >2h se passaram | Fazer init novamente |
| 400 | Lista de partes vazia | `completedParts` vazio no complete | Enviar todos os ETags |
| 403 | S3 Forbidden no PUT | URL pré-assinada expirou (>60 min) | Fazer init novamente |
| 404 | Pasta PSB não encontrada | `folderId` inválido | Verificar ID da pasta |
| 404 | Usuário não encontrado | `uploadedById` inválido | Verificar ID do usuário |

---

## Resumo dos Endpoints

| Endpoint | Método | Descrição |
|---|---|---|
| `/psb/files/presigned/init/{folderId}` | POST | Inicializa upload multipart — retorna URLs |
| `/psb/files/presigned/complete/{folderId}` | POST | Confirma upload — salva no banco |
| `/psb/files/presigned/abort/{uploadId}` | POST | Cancela upload em andamento |
| `/psb/files/upload/{folderId}` | POST | **[LEGADO]** Upload via multipart form (não usar) |
| `/psb/files/download/{fileId}` | GET | Download do arquivo |
| `/psb/files/{fileId}` | DELETE | Excluir arquivo |
| `/psb/files/folder/{folderId}` | GET | Listar arquivos de uma pasta |

---

## Limites e Timeouts

| Item | Valor |
|---|---|
| URLs pré-assinadas expiram em | **15 minutos** |
| Upload pendente expira em | **30 minutos** |
| Cleanup em memória (scheduler) | A cada **5 minutos** |
| Startup cleanup no S3 | **Automático** ao reiniciar API (aborta uploads orphãos >30min) |
| Tamanho mínimo de parte (S3) | 5MB |
| Tamanho máximo de parte | 100MB |
| Partes máximas por upload (S3) | 10.000 |
| Tamanho máximo total (S3) | 5TB |

