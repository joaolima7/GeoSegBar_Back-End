#!/usr/bin/env python3
"""
Cliente S3 mínimo, assinando SigV4 com a biblioteca padrão do Python.

POR QUE NÃO USAR O curl
    O `--aws-sigv4` do curl 7.76 — versão do servidor — erra a assinatura em dois
    casos que precisamos:

    1. PUT com corpo: calcula o hash do payload por conta própria para o
       canonical request e ignora o `x-amz-content-sha256` informado. Sem o
       header o S3 devolve 400 (é obrigatório); com ele, 403
       SignatureDoesNotMatch. Testado com o hash real e com UNSIGNED-PAYLOAD.
    2. GET com query string: listar com `?list-type=2&prefix=...` também dá
       SignatureDoesNotMatch. Sem parâmetros funciona.

    O curl continua servindo para HEAD sem query, onde acerta.

POR QUE NÃO USAR A AWS CLI
    Resolveria, mas exige instalar pacote no servidor. Aqui bastam hashlib, hmac
    e urllib, que já vêm com o Python.

POR QUE NÃO PEDIR URL PRÉ-ASSINADA À API
    O backup não pode depender de a aplicação estar no ar — o momento em que
    mais se precisa dele é justamente quando ela não está.

Uso:
    s3_client.py put  <arquivo> <bucket> <região> <chave>
    s3_client.py list <bucket> <região> <prefixo>

Credenciais vêm de AWS_ACCESS_KEY_ID e AWS_SECRET_ACCESS_KEY no ambiente.

`put`  imprime o número de bytes enviados.
`list` imprime uma linha por objeto: <tamanho><TAB><data><TAB><chave>.

Em falha, imprime o motivo em stderr e sai diferente de zero.
"""

import datetime
import hashlib
import hmac
import os
import sys
import urllib.error
import urllib.parse
import urllib.request

ALGORITMO = "AWS4-HMAC-SHA256"
SERVICO = "s3"
TAMANHO_BLOCO = 1024 * 1024
SHA256_VAZIO = hashlib.sha256(b"").hexdigest()


def _hmac(chave: bytes, mensagem: str) -> bytes:
    return hmac.new(chave, mensagem.encode("utf-8"), hashlib.sha256).digest()


def _chave_de_assinatura(secret: str, datestamp: str, regiao: str) -> bytes:
    k = _hmac(("AWS4" + secret).encode("utf-8"), datestamp)
    k = _hmac(k, regiao)
    k = _hmac(k, SERVICO)
    return _hmac(k, "aws4_request")


def _credenciais() -> tuple:
    access = os.environ.get("AWS_ACCESS_KEY_ID", "")
    secret = os.environ.get("AWS_SECRET_ACCESS_KEY", "")
    if not access or not secret:
        raise SystemExit("AWS_ACCESS_KEY_ID / AWS_SECRET_ACCESS_KEY ausentes no ambiente")
    return access, secret


def _assinar(metodo, host, canonical_uri, query, payload_hash, regiao, extras=None):
    """Monta o header Authorization e os headers que precisam acompanhá-lo."""
    access, secret = _credenciais()
    agora = datetime.datetime.now(datetime.timezone.utc)
    amzdate = agora.strftime("%Y%m%dT%H%M%SZ")
    datestamp = agora.strftime("%Y%m%d")

    headers = {
        "host": host,
        "x-amz-content-sha256": payload_hash,
        "x-amz-date": amzdate,
    }
    headers.update(extras or {})

    # A ordem alfabética dos headers assinados faz parte da especificação.
    nomes = sorted(headers)
    lista_assinada = ";".join(nomes)
    canonical_headers = "".join(f"{n}:{headers[n]}\n" for n in nomes)

    canonical_request = "\n".join([
        metodo, canonical_uri, query, canonical_headers, lista_assinada, payload_hash,
    ])

    escopo = f"{datestamp}/{regiao}/{SERVICO}/aws4_request"
    string_to_sign = "\n".join([
        ALGORITMO, amzdate, escopo,
        hashlib.sha256(canonical_request.encode("utf-8")).hexdigest(),
    ])

    assinatura = hmac.new(
        _chave_de_assinatura(secret, datestamp, regiao),
        string_to_sign.encode("utf-8"),
        hashlib.sha256,
    ).hexdigest()

    headers_http = {n: headers[n] for n in nomes if n != "host"}
    headers_http["Host"] = host
    headers_http["Authorization"] = (
        f"{ALGORITMO} Credential={access}/{escopo}, "
        f"SignedHeaders={lista_assinada}, Signature={assinatura}"
    )
    return headers_http


def _erro_do_s3(erro) -> str:
    detalhe = erro.read().decode("utf-8", "replace")
    codigo = _entre(detalhe, "<Code>", "</Code>") or "?"
    mensagem = _entre(detalhe, "<Message>", "</Message>") or detalhe[:200]
    return f"S3 respondeu HTTP {erro.code} — {codigo}: {mensagem}"


def _entre(texto: str, inicio: str, fim: str) -> str:
    i = texto.find(inicio)
    if i < 0:
        return ""
    j = texto.find(fim, i + len(inicio))
    return texto[i + len(inicio):j] if j > 0 else ""


def _codificar_chave(chave: str) -> str:
    return "/" + "/".join(urllib.parse.quote(p, safe="") for p in chave.split("/"))


def enviar(caminho: str, bucket: str, regiao: str, chave: str) -> None:
    if not os.path.isfile(caminho):
        raise SystemExit(f"Arquivo não encontrado: {caminho}")

    tamanho = os.path.getsize(caminho)

    # Hash em blocos: não carrega o arquivo inteiro na memória.
    h = hashlib.sha256()
    with open(caminho, "rb") as f:
        for bloco in iter(lambda: f.read(TAMANHO_BLOCO), b""):
            h.update(bloco)
    payload_hash = h.hexdigest()

    host = f"{bucket}.s3.{regiao}.amazonaws.com"
    canonical_uri = _codificar_chave(chave)

    headers = _assinar(
        "PUT", host, canonical_uri, "", payload_hash, regiao,
        extras={"x-amz-server-side-encryption": "AES256"},
    )
    headers["Content-Length"] = str(tamanho)
    headers["Content-Type"] = "application/gzip"

    with open(caminho, "rb") as corpo:
        req = urllib.request.Request(
            url=f"https://{host}{canonical_uri}", data=corpo, method="PUT", headers=headers,
        )
        try:
            with urllib.request.urlopen(req, timeout=900) as resposta:
                if resposta.status != 200:
                    raise SystemExit(f"S3 respondeu HTTP {resposta.status}")
        except urllib.error.HTTPError as erro:
            raise SystemExit(_erro_do_s3(erro))
        except urllib.error.URLError as erro:
            raise SystemExit(f"Falha de rede ao contatar o S3: {erro.reason}")

    print(tamanho)


def listar(bucket: str, regiao: str, prefixo: str) -> None:
    host = f"{bucket}.s3.{regiao}.amazonaws.com"
    continuation = None
    total = 0

    while True:
        # A query canônica exige parâmetros ordenados e percent-encoded.
        params = [("list-type", "2"), ("max-keys", "1000"), ("prefix", prefixo)]
        if continuation:
            params.append(("continuation-token", continuation))
        query = "&".join(
            f"{urllib.parse.quote(k, safe='')}={urllib.parse.quote(v, safe='')}"
            for k, v in sorted(params)
        )

        headers = _assinar("GET", host, "/", query, SHA256_VAZIO, regiao)
        req = urllib.request.Request(
            url=f"https://{host}/?{query}", method="GET", headers=headers,
        )

        try:
            with urllib.request.urlopen(req, timeout=60) as resposta:
                corpo = resposta.read().decode("utf-8", "replace")
        except urllib.error.HTTPError as erro:
            raise SystemExit(_erro_do_s3(erro))
        except urllib.error.URLError as erro:
            raise SystemExit(f"Falha de rede ao contatar o S3: {erro.reason}")

        for bloco in corpo.split("<Contents>")[1:]:
            chave = _entre(bloco, "<Key>", "</Key>")
            tamanho = _entre(bloco, "<Size>", "</Size>")
            data = _entre(bloco, "<LastModified>", "</LastModified>")
            print(f"{tamanho}\t{data}\t{chave}")
            total += 1

        if _entre(corpo, "<IsTruncated>", "</IsTruncated>") != "true":
            break
        continuation = _entre(corpo, "<NextContinuationToken>", "</NextContinuationToken>")
        if not continuation:
            break

    if total == 0:
        sys.exit(0)


if __name__ == "__main__":
    if len(sys.argv) >= 2 and sys.argv[1] == "put" and len(sys.argv) == 6:
        enviar(sys.argv[2], sys.argv[3], sys.argv[4], sys.argv[5])
    elif len(sys.argv) >= 2 and sys.argv[1] == "list" and len(sys.argv) == 5:
        listar(sys.argv[2], sys.argv[3], sys.argv[4])
    else:
        raise SystemExit(
            f"uso:\n"
            f"  {sys.argv[0]} put  <arquivo> <bucket> <região> <chave>\n"
            f"  {sys.argv[0]} list <bucket> <região> <prefixo>"
        )
