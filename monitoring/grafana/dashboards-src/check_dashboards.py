#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Valida os dashboards JSON gerados em monitoring/grafana/dashboards/.

Uso:
    python3 monitoring/grafana/dashboards-src/check_dashboards.py [DIR]

Verificações:
  - JSON válido, sem chave duplicada;
  - os 10 arquivos esperados existem e o uid de cada um é gsb-<nome do arquivo>;
  - uid único entre arquivos e com no máximo 40 caracteres;
  - id de painel único por dashboard, gridPos dentro da grade e sem sobreposição;
  - painel e alvo com datasource {type, uid}, uid em {prometheus, loki, tempo}
    (painéis row e text ficam de fora);
  - toda expressão PromQL/LogQL com (), {}, [] e aspas balanceados e com
    janelas [..] válidas;
  - toda variável $var / ${var} usada em expressão existe em templating
    (as internas como $__rate_interval são aceitas);
  - convenções do projeto: description em todo painel, exemplar nas séries de
    latência, refId único por painel, editable false, tag geosegbar.

Só biblioteca padrão. Sai com código diferente de zero se houver erro.
"""
from __future__ import annotations

import json
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
DEFAULT_DIR = os.path.normpath(os.path.join(HERE, "..", "dashboards"))

EXPECTED_FILES = [
    "overview.json", "endpoints.json", "errors.json", "traffic.json",
    "database.json", "jvm.json", "host.json", "security.json",
    "jobs.json", "monitoring.json",
]
ALLOWED_DS = {"prometheus", "loki", "tempo"}
DS_TYPE_BY_UID = {"prometheus": "prometheus", "loki": "loki", "tempo": "tempo"}
BUILTIN_VARS = {
    "__interval", "__interval_ms", "__rate_interval", "__rate_interval_ms",
    "__range", "__range_s", "__range_ms", "__auto", "__from", "__to",
    "__dashboard", "__org", "__user",
}
NO_DS_TYPES = {"row", "text"}
VAR_RE = re.compile(r"\$\{([A-Za-z_][A-Za-z0-9_]*)[^}]*\}|\$([A-Za-z_][A-Za-z0-9_]*)")
RANGE_RE = re.compile(r"^(\$[A-Za-z_]\w*|\$\{[^}]+\}|(\d+[smhdwy])+(:((\d+[smhdwy])+)?)?)$")


class Report:
    def __init__(self):
        self.errors = []
        self.warnings = []

    def err(self, where, msg):
        self.errors.append("%s: %s" % (where, msg))

    def warn(self, where, msg):
        self.warnings.append("%s: %s" % (where, msg))


def load_strict(path):
    def no_dupes(pairs):
        seen = {}
        for k, v in pairs:
            if k in seen:
                raise ValueError("chave duplicada %r" % k)
            seen[k] = v
        return seen

    with open(path, "r", encoding="utf-8") as fh:
        return json.loads(fh.read(), object_pairs_hook=no_dupes)


def check_balanced(expr):
    """Retorna None se (), {}, [] e aspas estão balanceados; senão a mensagem."""
    pairs = {")": "(", "}": "{", "]": "["}
    stack = []
    i = 0
    n = len(expr)
    while i < n:
        c = expr[i]
        if c in "\"'`":
            quote = c
            j = i + 1
            while j < n:
                if quote != "`" and expr[j] == "\\":
                    j += 2
                    continue
                if expr[j] == quote:
                    break
                j += 1
            if j >= n:
                return "aspas %s sem fechamento na posição %d" % (quote, i)
            i = j + 1
            continue
        if c in "({[":
            stack.append((c, i))
        elif c in ")}]":
            if not stack or stack[-1][0] != pairs[c]:
                return "%r sem par na posição %d" % (c, i)
            open_c, open_i = stack.pop()
            if open_c == "[":
                inner = expr[open_i + 1:i]
                if not RANGE_RE.match(inner.strip()):
                    return "janela [%s] inválida" % inner
        i += 1
    if stack:
        return "%r aberto na posição %d sem fechar" % (stack[-1][0], stack[-1][1])
    return None


def vars_in(text):
    found = set()
    for m in VAR_RE.finditer(text):
        found.add(m.group(1) or m.group(2))
    return found


def flatten(panels):
    for p in panels:
        yield p
        for child in p.get("panels", []) or []:
            yield child


def ds_ok(ds, where, rep, label):
    if not isinstance(ds, dict) or "type" not in ds or "uid" not in ds:
        rep.err(where, "%s sem datasource {type, uid}" % label)
        return None
    uid = ds["uid"]
    if uid not in ALLOWED_DS:
        rep.err(where, "%s com datasource uid %r fora de %s" % (label, uid, sorted(ALLOWED_DS)))
        return None
    if ds["type"] != DS_TYPE_BY_UID[uid]:
        rep.err(where, "%s com type %r incompatível com uid %r" % (label, ds["type"], uid))
        return None
    return uid


def check_overlaps(panels, where, rep):
    boxes = []
    for p in panels:
        g = p.get("gridPos")
        if not isinstance(g, dict) or not all(k in g for k in ("x", "y", "w", "h")):
            rep.err(where, "painel %r sem gridPos completo" % p.get("id"))
            continue
        if g["w"] < 1 or g["h"] < 1 or g["x"] < 0 or g["y"] < 0 or g["x"] + g["w"] > 24:
            rep.err(where, "painel %r com gridPos fora da grade de 24 colunas: %s" % (p.get("id"), g))
            continue
        boxes.append((p.get("id"), g["x"], g["y"], g["x"] + g["w"], g["y"] + g["h"]))
    for i in range(len(boxes)):
        for j in range(i + 1, len(boxes)):
            a, b = boxes[i], boxes[j]
            if a[1] < b[3] and b[1] < a[3] and a[2] < b[4] and b[2] < a[4]:
                rep.err(where, "gridPos sobreposto entre painéis %s e %s" % (a[0], b[0]))


def check_dashboard(fname, dash, rep, stats):
    where = fname
    for key in ("uid", "title", "panels", "templating", "annotations", "time", "refresh", "tags", "links"):
        if key not in dash:
            rep.err(where, "campo %r ausente" % key)
    uid = dash.get("uid", "")
    if len(uid) > 40:
        rep.err(where, "uid com %d caracteres (máximo 40)" % len(uid))
    expected_uid = "gsb-" + fname[:-5]
    if uid != expected_uid:
        rep.err(where, "uid %r deveria ser %r" % (uid, expected_uid))
    if dash.get("editable") is not False:
        rep.err(where, "editable deve ser false")
    if dash.get("schemaVersion") != 39:
        rep.err(where, "schemaVersion deve ser 39")
    if "geosegbar" not in dash.get("tags", []):
        rep.err(where, "tag geosegbar ausente")
    if dash.get("graphTooltip") != 1:
        rep.err(where, "graphTooltip deve ser 1")
    if dash.get("time") != {"from": "now-6h", "to": "now"}:
        rep.err(where, "time deve ser now-6h até now")
    if dash.get("refresh") != "30s":
        rep.err(where, "refresh deve ser 30s")
    if dash.get("timezone") != "browser":
        rep.err(where, "timezone deve ser browser")
    ann = dash.get("annotations", {}).get("list", [])
    if not any(a.get("builtIn") == 1 for a in ann):
        rep.err(where, "falta a anotação nativa do Grafana")
    for a in ann:
        if a.get("builtIn") == 1:
            continue
        ds_ok(a.get("datasource"), where, rep, "anotação %r" % a.get("name"))
        if a.get("expr"):
            msg = check_balanced(a["expr"])
            if msg:
                rep.err(where, "anotação %r: %s" % (a.get("name"), msg))

    # variáveis de template
    declared = set()
    used = set()
    for v in dash.get("templating", {}).get("list", []):
        name = v.get("name")
        if not name:
            rep.err(where, "variável sem name")
            continue
        if name in declared:
            rep.err(where, "variável %r duplicada" % name)
        declared.add(name)
        if v.get("type") == "query":
            ds_ok(v.get("datasource"), where, rep, "variável %r" % name)

    all_panels = list(flatten(dash.get("panels", [])))
    ids = [p.get("id") for p in all_panels]
    if len(ids) != len(set(ids)):
        dup = sorted({i for i in ids if ids.count(i) > 1})
        rep.err(where, "ids de painel repetidos: %s" % dup)
    if any(not isinstance(i, int) for i in ids):
        rep.err(where, "todo painel precisa de id inteiro")

    check_overlaps(dash.get("panels", []), where, rep)
    for p in dash.get("panels", []):
        if p.get("type") == "row" and p.get("panels"):
            check_overlaps(p["panels"], where + " (linha %r)" % p.get("title"), rep)

    titles = set()
    n_targets = 0
    n_panels = 0
    n_rows = 0
    for p in all_panels:
        ptype = p.get("type")
        pw = "%s painel %s (%s)" % (fname, p.get("id"), p.get("title"))
        if ptype == "row":
            n_rows += 1
            if not p.get("title"):
                rep.err(pw, "linha sem título")
            continue
        n_panels += 1
        if not p.get("title"):
            rep.err(pw, "painel sem título")
        elif p["title"] in titles:
            rep.warn(pw, "título repetido no dashboard")
        titles.add(p.get("title"))
        desc = p.get("description")
        if not isinstance(desc, str) or not desc.strip():
            rep.err(pw, "painel sem description")
        if ptype in NO_DS_TYPES:
            continue

        panel_uid = ds_ok(p.get("datasource"), pw, rep, "painel")
        targets = p.get("targets")
        if not targets:
            rep.err(pw, "painel sem targets")
            continue
        refids = []
        for t in targets:
            n_targets += 1
            tuid = ds_ok(t.get("datasource"), pw, rep, "alvo %s" % t.get("refId"))
            if panel_uid and tuid and tuid != panel_uid:
                rep.err(pw, "alvo %s usa datasource %s diferente do painel (%s)" % (t.get("refId"), tuid, panel_uid))
            refids.append(t.get("refId"))
            if tuid in ("prometheus", "loki"):
                expr = t.get("expr")
                if not isinstance(expr, str) or not expr.strip():
                    rep.err(pw, "alvo %s sem expr" % t.get("refId"))
                    continue
                if "@" in expr:
                    rep.err(pw, "alvo %s com atalho não expandido em %r" % (t.get("refId"), expr))
                msg = check_balanced(expr)
                if msg:
                    rep.err(pw, "alvo %s: %s em %r" % (t.get("refId"), msg, expr))
                used |= vars_in(expr)
                for name in vars_in(expr):
                    if name not in declared and name not in BUILTIN_VARS:
                        rep.err(pw, "alvo %s usa variável $%s inexistente" % (t.get("refId"), name))
                if tuid == "prometheus":
                    if ptype == "timeseries" and "histogram_quantile" in expr and t.get("exemplar") is not True:
                        rep.err(pw, "alvo %s de latência sem exemplar true" % t.get("refId"))
                    if ptype == "heatmap" and t.get("format") != "heatmap":
                        rep.err(pw, "alvo %s do heatmap sem format heatmap" % t.get("refId"))
            elif tuid == "tempo":
                if not t.get("query"):
                    rep.err(pw, "alvo %s do Tempo sem query" % t.get("refId"))
                if t.get("queryType") != "traceql":
                    rep.err(pw, "alvo %s do Tempo deve ter queryType traceql" % t.get("refId"))
                if t.get("limit") is None:
                    rep.err(pw, "alvo %s do Tempo sem limit" % t.get("refId"))
                if t.get("tableType") != "traces" and ptype == "table":
                    rep.err(pw, "alvo %s do Tempo deve ter tableType traces" % t.get("refId"))
        if len(refids) != len(set(refids)) or any(not r for r in refids):
            rep.err(pw, "refId vazio ou repetido: %s" % refids)

        # variáveis usadas em links de dados e overrides (só aviso se inexistentes)
        blob = json.dumps(p.get("fieldConfig", {}), ensure_ascii=False)
        for name in vars_in(blob):
            if name.startswith("__"):
                continue
            if name not in declared and name not in BUILTIN_VARS:
                rep.err(pw, "fieldConfig usa variável $%s inexistente" % name)

    for name in sorted(declared - used):
        # a variável pode ser usada só por links; aviso apenas
        rep.warn(where, "variável %r declarada mas não usada em nenhuma expressão" % name)

    stats[fname] = (n_panels, n_rows, n_targets, uid)


def main(argv=None):
    argv = sys.argv[1:] if argv is None else argv
    directory = os.path.abspath(argv[0]) if argv else DEFAULT_DIR
    rep = Report()
    stats = {}
    uids = {}

    if not os.path.isdir(directory):
        print("erro: pasta %s não existe" % directory)
        return 2

    files = sorted(f for f in os.listdir(directory) if f.endswith(".json"))
    for f in EXPECTED_FILES:
        if f not in files:
            rep.err(f, "arquivo esperado não encontrado em %s" % directory)
    for f in files:
        if f not in EXPECTED_FILES:
            rep.warn(f, "arquivo fora da lista de dashboards esperados")

    for f in files:
        path = os.path.join(directory, f)
        try:
            dash = load_strict(path)
        except (ValueError, OSError) as exc:
            rep.err(f, "JSON inválido: %s" % exc)
            continue
        if not isinstance(dash, dict):
            rep.err(f, "raiz do JSON não é um objeto")
            continue
        uid = dash.get("uid")
        if uid in uids:
            rep.err(f, "uid %r repetido (também em %s)" % (uid, uids[uid]))
        else:
            uids[uid] = f
        check_dashboard(f, dash, rep, stats)

    print("Dashboards em %s" % directory)
    print("%-18s %-16s %8s %8s %8s" % ("arquivo", "uid", "painéis", "linhas", "alvos"))
    tp = tr = tt = 0
    for f in files:
        if f not in stats:
            continue
        n_panels, n_rows, n_targets, uid = stats[f]
        print("%-18s %-16s %8d %8d %8d" % (f, uid, n_panels, n_rows, n_targets))
        tp += n_panels
        tr += n_rows
        tt += n_targets
    print("%-18s %-16s %8d %8d %8d" % ("TOTAL", "", tp, tr, tt))

    for w in rep.warnings:
        print("AVISO  " + w)
    for e in rep.errors:
        print("ERRO   " + e)
    print("%d dashboards, %d painéis, %d erros, %d avisos"
          % (len(stats), tp, len(rep.errors), len(rep.warnings)))
    return 1 if rep.errors else 0


if __name__ == "__main__":
    sys.exit(main())
