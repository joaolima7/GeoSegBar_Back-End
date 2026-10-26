#!/usr/bin/env python3
"""Resumo diário da API GeoSegBar no Telegram.

Roda no host de produção, como root, pelo cron que o deploy do monitoramento
instala (/etc/cron.d/geosegbar-monitoring-report, todo dia às 21h de Brasília).
Lê o Prometheus por dentro do container (ele não tem porta publicada) e manda
uma mensagem curta no mesmo chat dos alertas: tráfego, disponibilidade, rotas
lentas e pesadas, erros, logins, servidor, banco, backup, coleta ANA e os
avisos do dia (os alertas de severidade warning não mandam mensagem na hora;
aparecem aqui).

Só biblioteca padrão. Uso:
    python3 -I daily_report.py            # envia
    python3 -I daily_report.py --dry-run  # só imprime a mensagem
"""

import argparse
import datetime
import html
import json
import os
import shutil
import subprocess
import sys
import urllib.parse
import urllib.request

PROM_CONTAINER = "geosegbar-prometheus"
SECRETS_DIR = os.environ.get("GSB_SECRETS_DIR", "/etc/geosegbar/monitoring/secrets")
LONG_ROUTES = 'uri!~".*(export|download|upload|massive|import|zip).*"'
API = 'job="geosegbar-api"'
TZ_BRASILIA = datetime.timezone(datetime.timedelta(hours=-3))


def query(expr):
    """Consulta instantânea no Prometheus, pelo wget de dentro do container."""
    docker = shutil.which("docker") or "/usr/bin/docker"
    url = "http://localhost:9090/api/v1/query?query=" + urllib.parse.quote(expr, safe="")
    out = subprocess.run([docker, "exec", PROM_CONTAINER, "wget", "-qO-", url],
                         capture_output=True, text=True, timeout=60)
    if out.returncode != 0:
        raise RuntimeError("Prometheus não respondeu: " + out.stderr.strip()[:200])
    data = json.loads(out.stdout)
    if data.get("status") != "success":
        raise RuntimeError("consulta recusada: " + str(data.get("error"))[:200])
    return data["data"]["result"]


def scalar(expr):
    """Primeiro valor numérico do resultado, ou None se vazio ou NaN."""
    try:
        res = query(expr)
    except Exception:
        return None
    if not res:
        return None
    try:
        v = float(res[0]["value"][1])
    except (KeyError, ValueError, IndexError):
        return None
    return None if v != v else v  # NaN


def vector(expr):
    try:
        return query(expr)
    except Exception:
        return []


def num(v, casas=0):
    """Número no formato brasileiro (1.234,5)."""
    if v is None:
        return "sem dado"
    s = f"{v:,.{casas}f}"
    return s.replace(",", "X").replace(".", ",").replace("X", ".")


def pct(v, casas=1):
    return "sem dado" if v is None else num(v * 100, casas) + "%"


def dur(seg):
    if seg is None:
        return "sem dado"
    if seg < 1:
        return num(seg * 1000) + " ms"
    if seg < 120:
        return num(seg, 1) + " s"
    if seg < 7200:
        return num(seg / 60) + " min"
    return num(seg / 3600, 1) + " h"


def esc(s):
    return html.escape(str(s), quote=False)


def build_message(janela="24h"):
    w = janela
    linhas = []
    hoje = datetime.datetime.now(TZ_BRASILIA).strftime("%d/%m/%Y")
    linhas.append(f"📊 <b>GeoSegBar · resumo de {hoje}</b> (últimas {w})")

    # ------------------------------------------------------------ tráfego
    total = scalar(f'sum(increase(http_server_requests_seconds_count{{{API}}}[{w}]))')
    erros = scalar(f'sum(increase(http_server_requests_seconds_count{{{API}, status=~"5.."}}[{w}]))') or 0.0
    pico = scalar(f'max_over_time(geosegbar:http_requests:rate5m[{w}])')
    p95 = scalar(f'histogram_quantile(0.95, sum by (le) (increase(http_server_requests_seconds_bucket{{{API}, {LONG_ROUTES}}}[{w}])))')
    disp = None if not total else 1 - erros / total
    sonda = scalar(f'min(avg_over_time(probe_success{{job="blackbox"}}[{w}]))')

    status = "🟢" if (disp is None or disp >= 0.995) else ("🟡" if disp >= 0.98 else "🔴")
    linhas.append("")
    linhas.append(f"{status} <b>Disponibilidade</b>: {pct(disp, 2)} sem erro 5xx "
                  f"({num(erros)} erros em {num(total)} requisições)")
    linhas.append(f"• Pico: {num(pico * 60 if pico is not None else None)} req/min · "
                  f"p95 do dia: {dur(p95)}")
    if sonda is not None:
        linhas.append(f"• Sonda externa (api.geosegbar.com.br): {pct(sonda, 2)} no ar")

    # ------------------------------------------------------------ rotas
    lentas = vector(
        f'topk(3, histogram_quantile(0.95, sum by (le, method, uri) '
        f'(increase(http_server_requests_seconds_bucket{{{API}, {LONG_ROUTES}}}[{w}]))) '
        f'and on (method, uri) (sum by (method, uri) '
        f'(increase(http_server_requests_seconds_count{{{API}}}[{w}])) >= 20))')
    pesadas = vector(f'topk(3, sum by (method, uri) (increase(http_server_requests_seconds_sum{{{API}}}[{w}])))')
    com_erro = vector(f'topk(3, sum by (method, uri) (increase(http_server_requests_seconds_count{{{API}, status=~"5.."}}[{w}])) > 0)')

    def rota(r):
        m = r["metric"]
        return esc(f'{m.get("method", "")} {m.get("uri", "")}'.strip())

    if lentas:
        linhas.append("")
        linhas.append("🐢 <b>Mais lentas</b> (p95, mín. 20 chamadas)")
        for r in sorted(lentas, key=lambda r: -float(r["value"][1])):
            linhas.append(f"• {rota(r)}: {dur(float(r['value'][1]))}")
    if pesadas:
        linhas.append("")
        linhas.append("🏋️ <b>Mais pesadas</b> (tempo total de servidor)")
        for r in sorted(pesadas, key=lambda r: -float(r["value"][1])):
            linhas.append(f"• {rota(r)}: {dur(float(r['value'][1]))}")
    if com_erro:
        linhas.append("")
        linhas.append("🧯 <b>Erros 5xx por rota</b>")
        for r in sorted(com_erro, key=lambda r: -float(r["value"][1])):
            linhas.append(f"• {rota(r)}: {num(float(r['value'][1]))}")

    # ------------------------------------------------------------ segurança
    logins = {r["metric"].get("result"): float(r["value"][1]) for r in
              vector(f'sum by (result) (increase(geosegbar_auth_login_total{{{API}}}[{w}]))')}
    bypass = scalar(f'sum(increase(geosegbar_auth_bypass_key_total{{{API}}}[{w}]))') or 0.0
    negados = scalar(f'sum(increase(http_server_requests_seconds_count{{{API}, status=~"401|403|429"}}[{w}]))')
    linhas.append("")
    linhas.append("🔐 <b>Segurança</b>")
    ok = logins.get("success", 0) + logins.get("mfa_required", 0)
    linhas.append(f"• Logins: {num(ok)} com sucesso · {num(logins.get('invalid_credentials', 0))} com senha errada")
    linhas.append(f"• Respostas 401/403/429: {num(negados)}")
    linhas.append(("⚠️ " if bypass > 0 else "• ") + f"Uso do bypassKey: {num(bypass)}")

    # ------------------------------------------------------------ servidor e banco
    # Média de taxas de 5 min, não rate(...[24h]): rate divide pelo intervalo
    # inteiro e, com lacuna ou menos de 24 h de dados, a média sairia errada.
    cpu = scalar(f'avg_over_time((1 - avg(rate(node_cpu_seconds_total{{job="node", mode="idle"}}[5m])))[{w}:5m])')
    cpu_max = scalar(f'max_over_time((1 - avg(rate(node_cpu_seconds_total{{job="node", mode="idle"}}[5m])))[{w}:5m])')
    mem_min = scalar(f'min_over_time((node_memory_MemAvailable_bytes{{job="node"}} / node_memory_MemTotal_bytes{{job="node"}})[{w}:5m])')
    disco = scalar('min(node_filesystem_avail_bytes{job="node", mountpoint="/"} / node_filesystem_size_bytes{job="node", mountpoint="/"})')
    conexoes = scalar(f'max_over_time(sum(pg_stat_activity_count{{job="postgres"}})[{w}:5m])')
    tx = scalar(f'max_over_time(max(pg_stat_activity_max_tx_duration{{job="postgres", backend_type="client backend", application_name!~"pg_dump.*"}})[{w}:5m])')
    linhas.append("")
    linhas.append("🖥️ <b>Servidor e banco</b>")
    linhas.append(f"• CPU: média {pct(cpu, 0)}, pico {pct(cpu_max, 0)} · memória livre mínima {pct(mem_min, 0)}")
    linhas.append(f"• Disco livre: {pct(disco, 0)} · conexões no banco (máx.): {num(conexoes)}")
    linhas.append(f"• Maior transação aberta: {dur(tx)}")

    # ------------------------------------------------------------ rotinas
    backup = scalar('time() - max(geosegbar_backup_last_success_timestamp_seconds)')
    ana = {r["metric"].get("result"): float(r["value"][1]) for r in
           vector(f'sum by (result) (increase(geosegbar_ana_collection_runs_total{{{API}}}[{w}]))')}
    jobs_falha = scalar(f'sum(increase(geosegbar_job_runs_total{{{API}, result="failure"}}[{w}]))') or 0.0
    linhas.append("")
    linhas.append("🗂️ <b>Rotinas</b>")
    linhas.append(("• " if backup is not None and backup < 26 * 3600 else "⚠️ ")
                  + f"Último backup do banco: há {dur(backup)}")
    if ana:
        linhas.append(f"• Coleta ANA: {num(ana.get('success', 0))} ok · {num(ana.get('failure', 0))} com falha")
    linhas.append(f"• Falhas de jobs: {num(jobs_falha)}")

    # ------------------------------------------------------------ alertas do dia
    disparados = vector(f'max by (alertname, severity) (max_over_time(ALERTS{{alertstate="firing", alertname!="Watchdog"}}[{w}]))')
    linhas.append("")
    if disparados:
        criticos = sorted({r["metric"]["alertname"] for r in disparados if r["metric"].get("severity") == "critical"})
        avisos = sorted({r["metric"]["alertname"] for r in disparados if r["metric"].get("severity") != "critical"})
        linhas.append("🚨 <b>Alertas do dia</b>")
        if criticos:
            linhas.append("• Críticos: " + esc(", ".join(criticos)))
        if avisos:
            linhas.append("• Avisos: " + esc(", ".join(avisos)))
    else:
        linhas.append("✅ Nenhum alerta disparou no dia.")

    return "\n".join(linhas)


def read_secret(nome):
    with open(os.path.join(SECRETS_DIR, nome), encoding="utf-8") as f:
        return f.read().strip()


def send(texto):
    token = read_secret("telegram_bot_token")
    chat = read_secret("telegram_chat_id")
    if not token or not chat:
        print("Segredos do Telegram vazios: resumo não enviado.")
        return 0
    corpo = urllib.parse.urlencode({
        "chat_id": chat, "text": texto[:4000], "parse_mode": "HTML",
        "disable_web_page_preview": "true", "disable_notification": "true",
    }).encode()
    req = urllib.request.Request(f"https://api.telegram.org/bot{token}/sendMessage", data=corpo)
    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            ok = json.loads(resp.read()).get("ok")
    except Exception as e:  # sem imprimir a URL: ela leva o token
        print("Falha ao enviar ao Telegram:", type(e).__name__)
        return 1
    print("Resumo enviado." if ok else "O Telegram recusou a mensagem.")
    return 0 if ok else 1


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--dry-run", action="store_true", help="só imprime a mensagem")
    ap.add_argument("--janela", default="24h", help="janela do resumo (padrão 24h)")
    args = ap.parse_args()
    try:
        texto = build_message(args.janela)
    except Exception as e:
        print("Falha ao montar o resumo:", e)
        return 1
    if args.dry_run:
        print(texto)
        return 0
    return send(texto)


if __name__ == "__main__":
    sys.exit(main())
