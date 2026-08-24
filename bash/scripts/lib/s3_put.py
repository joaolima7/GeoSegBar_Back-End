#!/usr/bin/env python3
"""
Envia um arquivo ao S3 assinando com SigV4, usando apenas a biblioteca padrão.

POR QUE NÃO USAR O curl
    O `--aws-sigv4` do curl 7.76 (versão do servidor) não assina corretamente um
    PUT com corpo: ele calcula o hash do payload por conta própria para o
    canonical request e ignora o header `x-amz-content-sha256` que se passa a
    ele. Sem o header, o S3 recusa com 400 (o header é obrigatório); com o
    header, a assinatura diverge e vem 403 SignatureDoesNotMatch. Testado com o
    hash real e com UNSIGNED-PAYLOAD — os dois falham.

POR QUE NÃO USAR A AWS CLI
    Resolveria, mas exige instalar pacote no servidor. Aqui bastam hashlib, hmac
    e urllib, que já vêm com o Python.

POR QUE NÃO PEDIR URL PRÉ-ASSINADA À API
    O backup não pode depender de a aplicação estar no ar — o momento em que
    mais se precisa dele é justamente quando ela não está.

Uso:
    s3_put.py <arquivo> <bucket> <região> <chave-no-bucket>

Credenciais vêm de AWS_ACCESS_KEY_ID e AWS_SECRET_ACCESS_KEY no ambiente.
Sai com 0 em sucesso; em falha, imprime o motivo em stderr e sai diferente de 0.
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


def _hmac(chave: bytes, mensagem: str) -> bytes:
    return hmac.new(chave, mensagem.encode("utf-8"), hashlib.sha256).digest()


def chave_de_assinatura(secret: str, datestamp: str, regiao: str) -> bytes:
    k = _hmac(("AWS4" + secret).encode("utf-8"), datestamp)
    k = _hmac(k, regiao)
    k = _hmac(k, SERVICO)
    return _hmac(k, "aws4_request")


def sha256_do_arquivo(caminho: str) -> str:
    """Hash lido em blocos — não carrega o arquivo inteiro na memória."""
    h = hashlib.sha256()
    with open(caminho, "rb") as f:
        for bloco in iter(lambda: f.read(TAMANHO_BLOCO), b""):
            h.update(bloco)
    return h.hexdigest()


def enviar(caminho: str, bucket: str, regiao: str, chave: str) -> None:
    access_key = os.environ.get("AWS_ACCESS_KEY_ID", "")
    secret_key = os.environ.get("AWS_SECRET_ACCESS_KEY", "")
    if not access_key or not secret_key:
        raise SystemExit("AWS_ACCESS_KEY_ID / AWS_SECRET_ACCESS_KEY ausentes no ambiente")

    if not os.path.isfile(caminho):
        raise SystemExit(f"Arquivo não encontrado: {caminho}")

    tamanho = os.path.getsize(caminho)
    payload_hash = sha256_do_arquivo(caminho)

    host = f"{bucket}.s3.{regiao}.amazonaws.com"
    agora = datetime.datetime.now(datetime.timezone.utc)
    amzdate = agora.strftime("%Y%m%dT%H%M%SZ")
    datestamp = agora.strftime("%Y%m%d")

    # Cada segmento do caminho é codificado; a barra permanece literal.
    canonical_uri = "/" + "/".join(
        urllib.parse.quote(parte, safe="") for parte in chave.split("/")
    )

    # A ordem alfabética dos headers assinados faz parte da especificação.
    headers_assinados = {
        "host": host,
        "x-amz-content-sha256": payload_hash,
        "x-amz-date": amzdate,
        "x-amz-server-side-encryption": "AES256",
    }
    lista_assinada = ";".join(sorted(headers_assinados))
    canonical_headers = "".join(
        f"{nome}:{headers_assinados[nome]}\n" for nome in sorted(headers_assinados)
    )

    canonical_request = "\n".join([
        "PUT",
        canonical_uri,
        "",                      # sem query string
        canonical_headers,
        lista_assinada,
        payload_hash,
    ])

    escopo = f"{datestamp}/{regiao}/{SERVICO}/aws4_request"
    string_to_sign = "\n".join([
        ALGORITMO,
        amzdate,
        escopo,
        hashlib.sha256(canonical_request.encode("utf-8")).hexdigest(),
    ])

    assinatura = hmac.new(
        chave_de_assinatura(secret_key, datestamp, regiao),
        string_to_sign.encode("utf-8"),
        hashlib.sha256,
    ).hexdigest()

    authorization = (
        f"{ALGORITMO} Credential={access_key}/{escopo}, "
        f"SignedHeaders={lista_assinada}, Signature={assinatura}"
    )

    with open(caminho, "rb") as corpo:
        requisicao = urllib.request.Request(
            url=f"https://{host}{canonical_uri}",
            data=corpo,                 # enviado em streaming, não lido de uma vez
            method="PUT",
            headers={
                "Host": host,
                "x-amz-content-sha256": payload_hash,
                "x-amz-date": amzdate,
                "x-amz-server-side-encryption": "AES256",
                "Authorization": authorization,
                "Content-Length": str(tamanho),
                "Content-Type": "application/gzip",
            },
        )

        try:
            with urllib.request.urlopen(requisicao, timeout=900) as resposta:
                if resposta.status != 200:
                    raise SystemExit(f"S3 respondeu HTTP {resposta.status}")
        except urllib.error.HTTPError as erro:
            detalhe = erro.read().decode("utf-8", "replace")
            # O corpo do erro do S3 é XML; extrai só o essencial.
            codigo = _entre(detalhe, "<Code>", "</Code>") or "?"
            mensagem = _entre(detalhe, "<Message>", "</Message>") or detalhe[:200]
            raise SystemExit(f"S3 respondeu HTTP {erro.code} — {codigo}: {mensagem}")
        except urllib.error.URLError as erro:
            raise SystemExit(f"Falha de rede ao contatar o S3: {erro.reason}")

    print(f"{tamanho}")


def _entre(texto: str, inicio: str, fim: str) -> str:
    i = texto.find(inicio)
    if i < 0:
        return ""
    j = texto.find(fim, i + len(inicio))
    return texto[i + len(inicio):j] if j > 0 else ""


if __name__ == "__main__":
    if len(sys.argv) != 5:
        raise SystemExit(f"uso: {sys.argv[0]} <arquivo> <bucket> <região> <chave>")
    enviar(sys.argv[1], sys.argv[2], sys.argv[3], sys.argv[4])
