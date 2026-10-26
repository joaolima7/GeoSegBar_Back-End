#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Gera os dashboards do Grafana do GeoSegBar a partir de código.

Uso:
    python3 monitoring/grafana/dashboards-src/build_dashboards.py [--out DIR]

Escreve um JSON por dashboard em monitoring/grafana/dashboards/ (ou em DIR).
A saída é determinística: ids sequenciais, sem datas, sem aleatoriedade.
Só biblioteca padrão (Python 3.9+).

Atalhos aceitos dentro das expressões (PromQL, LogQL), expandidos na hora de
montar o alvo da consulta:

    @A   ->  job="geosegbar-api"
    @N   ->  container="nginx-prod"

Para editar um painel, mexa neste arquivo, rode o build e depois o
check_dashboards.py. Veja o README.md desta pasta.
"""
from __future__ import annotations

import argparse
import json
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
DEFAULT_OUT = os.path.normpath(os.path.join(HERE, "..", "dashboards"))

# --------------------------------------------------------------------------
# Datasources (uids fixos, definidos em grafana/provisioning/datasources)
# --------------------------------------------------------------------------
PROM = {"type": "prometheus", "uid": "prometheus"}
LOKI = {"type": "loki", "uid": "loki"}
TEMPO = {"type": "tempo", "uid": "tempo"}

TOKENS = (
    ("@A", 'job="geosegbar-api"'),
    ("@N", 'container="nginx-prod"'),
)


def expand(expr):
    for token, value in TOKENS:
        expr = expr.replace(token, value)
    return expr


# --------------------------------------------------------------------------
# Construtores de expressão (evitam repetir seletores longos)
# --------------------------------------------------------------------------
def sel(*parts):
    """sel('@A', 'uri=~"$uri"') -> {@A, uri=~"$uri"}"""
    return "{" + ", ".join(parts) + "}"


def http(kind, *extra):
    """Série http_server_requests_seconds_<kind> da API, com filtros extras."""
    return "http_server_requests_seconds_" + kind + sel("@A", *extra)


def hq(q, *extra, by=None, rng="$__rate_interval"):
    """histogram_quantile sobre o histograma HTTP da API, somado entre instâncias."""
    keys = (by + ", " if by else "") + "le"
    return "histogram_quantile(%s, sum by (%s) (rate(%s[%s])))" % (
        q, keys, http("bucket", *extra), rng)


# O Tomcat da porta de management (8081) também publica threads; os painéis de
# saturação olham só o conector da API.
TOMCAT_API = 'name="http-nio-9090"'


def api(name, *extra):
    """Métrica da API (job geosegbar-api) com filtros extras."""
    return name + sel("@A", *extra)


# --------------------------------------------------------------------------
# Alvos (queries)
# --------------------------------------------------------------------------
def prom(expr, legend=None, instant=False, fmt=None, exemplar=False):
    t = {
        "datasource": PROM,
        "editorMode": "code",
        "expr": expand(expr),
        "instant": bool(instant),
        "range": not instant,
        "refId": "",
    }
    if legend is not None:
        t["legendFormat"] = legend
    if fmt:
        t["format"] = fmt
    if exemplar:
        t["exemplar"] = True
    return t


def loki(expr, legend=None, instant=False, max_lines=None):
    t = {
        "datasource": LOKI,
        "editorMode": "code",
        "expr": expand(expr),
        "queryType": "instant" if instant else "range",
        "refId": "",
    }
    if legend is not None:
        t["legendFormat"] = legend
    if max_lines:
        t["maxLines"] = max_lines
    return t


def tempo(query, limit=20):
    return {
        "datasource": TEMPO,
        "query": query,
        "queryType": "traceql",
        "limit": limit,
        "tableType": "traces",
        "refId": "",
    }


def _refs(targets):
    for i, t in enumerate(targets):
        t["refId"] = chr(ord("A") + i)
    return targets


# --------------------------------------------------------------------------
# Limiares, mapeamentos, overrides, transformações
# --------------------------------------------------------------------------
def th(*steps):
    """th('green', ('yellow', 0.5), ('red', 1)) -> thresholds absolutos."""
    out = []
    for s in steps:
        color, value = s if isinstance(s, tuple) else (s, None)
        out.append({"color": color, "value": value})
    return {"mode": "absolute", "steps": out}


def th_ok(warn, crit):
    """Quanto maior, pior: verde, amarelo em warn, vermelho em crit."""
    return th("green", ("yellow", warn), ("red", crit))


def th_low(warn, ok):
    """Quanto menor, pior: vermelho, amarelo em warn, verde em ok."""
    return th("red", ("yellow", warn), ("green", ok))


BLUE = th("blue")


def map_ok_fail(ok="NO AR", fail="FORA"):
    return [{
        "type": "value",
        "options": {
            "0": {"text": fail, "color": "red", "index": 0},
            "1": {"text": ok, "color": "green", "index": 1},
        },
    }]


def fixed(color):
    return ("color", {"mode": "fixed", "fixedColor": color})


DASH_LINE = ("custom.lineStyle", {"fill": "dash", "dash": [10, 10]})


def ov(match, props, regex=False):
    """Override por nome (ou regex) da série/coluna."""
    return {
        "matcher": {"id": "byRegexp" if regex else "byName", "options": match},
        "properties": [{"id": k, "value": v} for k, v in props],
    }


STATUS_CLASS_COLORS = [
    ov("2xx", [fixed("green")]),
    ov("3xx", [fixed("blue")]),
    ov("4xx", [fixed("orange")]),
    ov("5xx", [fixed("red")]),
]

STATUS_CODE_COLORS = [
    ov("^2", [fixed("green")], regex=True),
    ov("^3", [fixed("blue")], regex=True),
    ov("^4", [fixed("orange")], regex=True),
    ov("^5", [fixed("red")], regex=True),
]

CELL_TEXT = {"type": "color-text"}
CELL_BG = {"type": "color-background", "mode": "basic"}
CELL_GAUGE = {"type": "gauge", "mode": "gradient", "valueDisplayMode": "text"}


def col(name, unit=None, decimals=None, thresholds=None, cell=None, mappings=None,
        minv=None, maxv=None, width=None, links=None, no_value=None):
    """Override de coluna de tabela."""
    props = []
    if unit:
        props.append(("unit", unit))
    if decimals is not None:
        props.append(("decimals", decimals))
    if minv is not None:
        props.append(("min", minv))
    if maxv is not None:
        props.append(("max", maxv))
    if thresholds:
        props.append(("thresholds", thresholds))
        props.append(("color", {"mode": "thresholds"}))
    if mappings:
        props.append(("mappings", mappings))
    if cell:
        props.append(("custom.cellOptions", cell))
    if width:
        props.append(("custom.width", width))
    if links:
        props.append(("links", links))
    if no_value:
        props.append(("noValue", no_value))
    return ov(name, props)


def merge():
    return {"id": "merge", "options": {}}


def labels_to_fields():
    return {"id": "labelsToFields", "options": {}}


def organize(exclude=(), include=None, rename=None, order=None):
    opts = {}
    if exclude:
        opts["excludeByName"] = {k: True for k in exclude}
    if include:
        opts["includeByName"] = {k: True for k in include}
    if order:
        opts["indexByName"] = {k: i for i, k in enumerate(order)}
    if rename:
        opts["renameByName"] = dict(rename)
    return {"id": "organize", "options": opts}


# --------------------------------------------------------------------------
# Painéis
# --------------------------------------------------------------------------
def _base(ptype, title, desc, w, h, targets):
    ts = _refs(targets)
    return {
        "id": 0,
        "type": ptype,
        "title": title,
        "description": desc,
        "datasource": ts[0]["datasource"],
        "gridPos": {"h": h, "w": w, "x": 0, "y": 0},
        "targets": ts,
    }


def _defaults(unit=None, decimals=None, minv=None, maxv=None, thresholds=None,
              mappings=None, color=None, custom=None, no_value=None):
    d = {}
    d["color"] = color or {"mode": "thresholds"}
    if custom is not None:
        d["custom"] = custom
    if mappings:
        d["mappings"] = mappings
    d["thresholds"] = thresholds or BLUE
    if unit:
        d["unit"] = unit
    if decimals is not None:
        d["decimals"] = decimals
    if minv is not None:
        d["min"] = minv
    if maxv is not None:
        d["max"] = maxv
    if no_value is not None:
        d["noValue"] = no_value
    return d


def _reduce(calc="lastNotNull"):
    return {"calcs": [calc], "fields": "", "values": False}


def timeseries(title, desc, targets, w=12, h=8, unit="short", decimals=None,
               minv=None, maxv=None, stack=False, bars=False, fill=None,
               thresholds=None, thr_style=None, overrides=None, legend="list",
               calcs=None, mappings=None, no_value=None):
    p = _base("timeseries", title, desc, w, h, targets)
    if fill is None:
        fill = 70 if bars else (35 if stack else 10)
    custom = {
        "axisBorderShow": False,
        "axisCenteredZero": False,
        "axisColorMode": "text",
        "axisLabel": "",
        "axisPlacement": "auto",
        "barAlignment": 0,
        "drawStyle": "bars" if bars else "line",
        "fillOpacity": fill,
        "gradientMode": "none",
        "hideFrom": {"legend": False, "tooltip": False, "viz": False},
        "insertNulls": False,
        "lineInterpolation": "linear",
        "lineWidth": 1,
        "pointSize": 4,
        "scaleDistribution": {"type": "linear"},
        "showPoints": "never",
        "spanNulls": False,
        "stacking": {"group": "A", "mode": "normal" if stack else "none"},
        "thresholdsStyle": {"mode": thr_style or "off"},
    }
    p["fieldConfig"] = {
        "defaults": _defaults(unit, decimals, minv, maxv,
                              thresholds or th("green"), mappings,
                              {"mode": "palette-classic"}, custom, no_value),
        "overrides": overrides or [],
    }
    p["options"] = {
        "legend": {
            "calcs": calcs or [],
            "displayMode": "table" if legend == "table" else "list",
            "placement": "bottom",
            "showLegend": legend != "hidden",
        },
        "tooltip": {"mode": "multi", "sort": "desc"},
    }
    return p


def stat(title, desc, targets, w=4, h=4, unit="short", decimals=None, minv=None,
         maxv=None, thresholds=None, mappings=None, color_mode="value",
         graph="area", text_mode="auto", no_value=None, reduce="lastNotNull",
         overrides=None):
    p = _base("stat", title, desc, w, h, targets)
    p["fieldConfig"] = {
        "defaults": _defaults(unit, decimals, minv, maxv, thresholds, mappings,
                              None, None, no_value),
        "overrides": overrides or [],
    }
    p["options"] = {
        "colorMode": color_mode,
        "graphMode": graph,
        "justifyMode": "auto",
        "orientation": "auto",
        "reduceOptions": _reduce(reduce),
        "showPercentChange": False,
        "textMode": text_mode,
        "wideLayout": True,
    }
    return p


def gauge(title, desc, targets, w=4, h=5, unit="percentunit", decimals=None,
          minv=0, maxv=1, thresholds=None, mappings=None, no_value=None):
    p = _base("gauge", title, desc, w, h, targets)
    p["fieldConfig"] = {
        "defaults": _defaults(unit, decimals, minv, maxv, thresholds, mappings,
                              None, None, no_value),
        "overrides": [],
    }
    p["options"] = {
        "minVizHeight": 75,
        "minVizWidth": 75,
        "orientation": "auto",
        "reduceOptions": _reduce(),
        "showThresholdLabels": False,
        "showThresholdMarkers": True,
        "sizing": "auto",
    }
    return p


def bargauge(title, desc, targets, w=12, h=8, unit="short", decimals=None,
             minv=0, maxv=None, thresholds=None, mode="gradient",
             orientation="horizontal", no_value=None):
    p = _base("bargauge", title, desc, w, h, targets)
    p["fieldConfig"] = {
        "defaults": _defaults(unit, decimals, minv, maxv, thresholds, None,
                              None, None, no_value),
        "overrides": [],
    }
    p["options"] = {
        "displayMode": mode,
        "maxVizHeight": 300,
        "minVizHeight": 16,
        "minVizWidth": 8,
        "namePlacement": "auto",
        "orientation": orientation,
        "reduceOptions": _reduce(),
        "showUnfilled": True,
        "sizing": "auto",
        "valueMode": "color",
    }
    return p


def table(title, desc, targets, w=24, h=10, unit=None, decimals=None,
          transformations=None, sort_by=None, sort_desc=True, overrides=None,
          thresholds=None, no_value=None):
    p = _base("table", title, desc, w, h, targets)
    custom = {
        "align": "auto",
        "cellOptions": {"type": "auto"},
        "filterable": False,
        "inspect": False,
    }
    p["fieldConfig"] = {
        "defaults": _defaults(unit, decimals, None, None,
                              thresholds or th("green"), None, None, custom,
                              no_value),
        "overrides": overrides or [],
    }
    options = {
        "cellHeight": "sm",
        "footer": {"countRows": False, "fields": "", "reducer": ["sum"], "show": False},
        "showHeader": True,
    }
    if sort_by:
        options["sortBy"] = [{"desc": bool(sort_desc), "displayName": sort_by}]
    p["options"] = options
    if transformations:
        p["transformations"] = transformations
    return p


def traces_table(title, desc, query, w=24, h=8, limit=20):
    """Tabela de traces do Tempo (consulta TraceQL)."""
    p = table(title, desc, [tempo(query, limit)], w=w, h=h)
    return p


def logs(title, desc, expr, w=24, h=10, max_lines=100):
    p = _base("logs", title, desc, w, h, [loki(expr, max_lines=max_lines)])
    p["options"] = {
        "dedupStrategy": "none",
        "enableLogDetails": True,
        "prettifyLogMessage": False,
        "showCommonLabels": False,
        "showLabels": False,
        "showTime": True,
        "sortOrder": "Descending",
        "wrapLogMessage": True,
    }
    return p


def heatmap(title, desc, targets, w=12, h=9, unit="s"):
    p = _base("heatmap", title, desc, w, h, targets)
    p["fieldConfig"] = {
        "defaults": {
            "custom": {
                "hideFrom": {"legend": False, "tooltip": False, "viz": False},
                "scaleDistribution": {"type": "linear"},
            },
        },
        "overrides": [],
    }
    p["options"] = {
        "calculate": False,
        "cellGap": 1,
        "color": {
            "exponent": 0.5,
            "fill": "dark-orange",
            "mode": "scheme",
            "reverse": False,
            "scale": "exponential",
            "scheme": "Spectral",
            "steps": 64,
        },
        "exemplars": {"color": "rgba(255,0,255,0.7)"},
        "filterValues": {"le": 1e-09},
        "legend": {"show": True},
        "rowsFrame": {"layout": "auto"},
        "tooltip": {"mode": "single", "showColorScale": False, "yHistogram": True},
        "yAxis": {"axisPlacement": "left", "reverse": False, "unit": unit},
    }
    return p


def pie(title, desc, targets, w=12, h=8, unit="short"):
    p = _base("piechart", title, desc, w, h, targets)
    p["fieldConfig"] = {
        "defaults": _defaults(unit, None, None, None, None, None,
                              {"mode": "palette-classic"},
                              {"hideFrom": {"legend": False, "tooltip": False, "viz": False}}),
        "overrides": [],
    }
    p["options"] = {
        "displayLabels": ["percent"],
        "legend": {"displayMode": "table", "placement": "right",
                   "showLegend": True, "values": ["value", "percent"]},
        "pieType": "donut",
        "reduceOptions": _reduce(),
        "tooltip": {"mode": "single", "sort": "none"},
    }
    return p


def text(title, desc, content, w=24, h=3):
    return {
        "id": 0,
        "type": "text",
        "title": title,
        "description": desc,
        "gridPos": {"h": h, "w": w, "x": 0, "y": 0},
        "options": {"code": {"language": "plaintext", "showLineNumbers": False,
                             "showMiniMap": False},
                    "content": content, "mode": "markdown"},
    }


# --------------------------------------------------------------------------
# Variáveis e anotações
# --------------------------------------------------------------------------
def var_label(name, label, metric_selector, ds=PROM, multi=True, all_value=".*"):
    """Variável de consulta: label_values(<seletor>, <name>)."""
    query = "label_values(%s, %s)" % (metric_selector, name)
    return {
        "allValue": all_value,
        "current": {"selected": True, "text": ["All"], "value": ["$__all"]},
        "datasource": ds,
        "definition": query,
        "hide": 0,
        "includeAll": True,
        "label": label,
        "multi": multi,
        "name": name,
        "options": [],
        "query": {"qryType": 1, "query": query,
                  "refId": "PrometheusVariableQueryEditor-VariableQuery"},
        "refresh": 2,
        "regex": "",
        "skipUrlSync": False,
        "sort": 1,
        "type": "query",
    }


BUILTIN_ANNOTATION = {
    "builtIn": 1,
    "datasource": {"type": "grafana", "uid": "-- Grafana --"},
    "enable": True,
    "hide": True,
    "iconColor": "rgba(0, 211, 255, 1)",
    "name": "Annotations & Alerts",
    "type": "dashboard",
}

DEPLOY_ANNOTATION = {
    "datasource": PROM,
    "enable": True,
    "expr": "changes(geosegbar_build_info[2m]) > 0 or (geosegbar_build_info unless geosegbar_build_info offset 2m)",
    "iconColor": "purple",
    "name": "Deploy",
    "step": "60s",
    "tagKeys": "version",
    "textFormat": "Versão {{version}}",
    "titleFormat": "Deploy",
    "useValueForTime": False,
}


# --------------------------------------------------------------------------
# Dashboard com layout automático em grade de 24 colunas
# --------------------------------------------------------------------------
class Dash:
    def __init__(self, uid, title, description, variables=None, deploy=False):
        self.uid = uid
        self.title = title
        self.description = description
        self.variables = list(variables or [])
        self.deploy = deploy
        self.panels = []
        self._next_id = 1
        self._x = 0
        self._y = 0
        self._line_h = 0
        self._line_hs = set()

    def _take_id(self):
        i = self._next_id
        self._next_id += 1
        return i

    def _new_line(self):
        if self._x > 0:
            if len(self._line_hs) > 1:
                raise ValueError(
                    "%s: painéis da mesma linha com alturas diferentes %s"
                    % (self.uid, sorted(self._line_hs)))
            self._y += self._line_h
        self._x = 0
        self._line_h = 0
        self._line_hs = set()

    def row(self, title):
        self._new_line()
        self.panels.append({
            "id": self._take_id(),
            "type": "row",
            "title": title,
            "collapsed": False,
            "gridPos": {"h": 1, "w": 24, "x": 0, "y": self._y},
            "panels": [],
        })
        self._y += 1

    def add(self, *panels):
        for p in panels:
            w = p["gridPos"]["w"]
            h = p["gridPos"]["h"]
            if self._x + w > 24:
                self._new_line()
            p["gridPos"]["x"] = self._x
            p["gridPos"]["y"] = self._y
            self._x += w
            self._line_h = max(self._line_h, h)
            self._line_hs.add(h)
            p["id"] = self._take_id()
            self.panels.append(p)

    def build(self):
        self._new_line()
        annotations = [dict(BUILTIN_ANNOTATION)]
        if self.deploy:
            annotations.append(dict(DEPLOY_ANNOTATION))
        return {
            "annotations": {"list": annotations},
            "description": self.description,
            "editable": False,
            "fiscalYearStartMonth": 0,
            "graphTooltip": 1,
            "id": None,
            "links": [{
                "type": "dashboards",
                "tags": ["geosegbar"],
                "asDropdown": True,
                "title": "Painéis",
                "includeVars": False,
                "keepTime": True,
            }],
            "liveNow": False,
            "panels": self.panels,
            "refresh": "30s",
            "schemaVersion": 39,
            "tags": ["geosegbar"],
            "templating": {"list": self.variables},
            "time": {"from": "now-6h", "to": "now"},
            "timepicker": {},
            "timezone": "browser",
            "title": self.title,
            "uid": self.uid,
            "version": 1,
            "weekStart": "",
        }


# ==========================================================================
# 1. Visão geral
# ==========================================================================
def overview():
    d = Dash("gsb-overview", "GeoSegBar · Visão geral",
             "Sinais de ouro da API: tráfego, erros, latência, saturação, SLO e alertas.",
             deploy=True)

    d.row("Agora")
    d.add(
        stat("Requisições por segundo",
             "Volume de requisições atendidas pela API agora (média de 5 minutos, soma das instâncias).",
             [prom('sum(rate(%s[5m]))' % http("count"))],
             unit="reqps", decimals=2, thresholds=BLUE),
        stat("Taxa de erro 5xx",
             "Fração das requisições que terminaram em erro do servidor nos últimos 5 minutos; fica vermelho acima de 1%.",
             [prom('sum(rate(%s[5m])) / sum(rate(%s[5m]))'
                   % (http("count", 'status=~"5.."'), http("count")))],
             unit="percentunit", decimals=2, thresholds=th_ok(0.005, 0.01),
             no_value="sem tráfego"),
        stat("Latência p95",
             "Tempo em que 95% das requisições terminam, nos últimos 5 minutos; quanto menor, melhor.",
             [prom(hq(0.95, rng="5m"))],
             unit="s", decimals=2, thresholds=th_ok(1, 2.5)),
        stat("Instâncias no ar",
             "Quantidade de instâncias da API que o Prometheus consegue raspar agora; zero significa API fora.",
             [prom('count(up{@A} == 1) or vector(0)')],
             unit="short", decimals=0, thresholds=th("red", ("green", 1)),
             color_mode="background", graph="none"),
        stat("Uptime externo",
             "Resultado da sonda externa que acessa o site de fora; NO AR quando todas as sondas respondem.",
             [prom('min(probe_success{job="blackbox"})')],
             mappings=map_ok_fail(), thresholds=th("red", ("green", 1)),
             color_mode="background", graph="none", no_value="sem sonda"),
        stat("Alertas disparando",
             "Quantidade de alertas em estado firing agora, sem contar o Watchdog (que dispara sempre de propósito).",
             [prom('count(ALERTS{alertstate="firing", alertname!="Watchdog"}) or vector(0)')],
             unit="short", decimals=0, thresholds=th("green", ("red", 1)),
             color_mode="background", graph="none"),
    )
    d.add(
        table("Versão no ar",
              "Versão (build) que cada instância da API está rodando; durante um deploy azul e verde podem aparecer duas.",
              [prom('geosegbar_build_info{@A}', instant=True, fmt="table")],
              w=8, h=5,
              transformations=[organize(include=["instance", "version"],
                                        order=["instance", "version"],
                                        rename={"instance": "Instância", "version": "Versão"})]),
        stat("Disponibilidade 30 dias (meta 99,5%)",
             "Fração das requisições sem erro 5xx nos últimos 30 dias; vermelho abaixo da meta de 99,5%.",
             [prom('geosegbar:slo_availability:ratio_30d')],
             h=5, unit="percentunit", decimals=3,
             thresholds=th("red", ("yellow", 0.995), ("green", 0.998)),
             color_mode="background", graph="none"),
        gauge("Orçamento de erro restante (disponibilidade)",
              "Quanto do erro permitido pela meta de 99,5% ainda sobra no mês; 100% é intocado e 0% é orçamento esgotado.",
              [prom('1 - (1 - geosegbar:slo_availability:ratio_30d) / (1 - 0.995)')],
              thresholds=th_low(0.25, 0.5), decimals=0),
        stat("Latência ≤ 1 s em 30 dias (meta 95%)",
             "Fração das requisições respondidas em até 1 segundo nos últimos 30 dias, sem rotas longas; vermelho abaixo de 95%.",
             [prom('geosegbar:slo_latency:ratio_30d')],
             h=5, unit="percentunit", decimals=2,
             thresholds=th("red", ("yellow", 0.95), ("green", 0.97)),
             color_mode="background", graph="none"),
        gauge("Orçamento de erro restante (latência)",
              "Quanto das requisições lentas permitidas pela meta de 95% ainda sobra no mês; 0% significa meta estourada.",
              [prom('1 - (1 - geosegbar:slo_latency:ratio_30d) / (1 - 0.95)')],
              thresholds=th_low(0.25, 0.5), decimals=0),
    )
    d.add(
        table("Alertas em disparo",
              "Lista dos alertas que estão disparando agora; vazia quando está tudo bem.",
              [prom('ALERTS{alertstate="firing", alertname!="Watchdog"}', instant=True, fmt="table")],
              h=5,
              transformations=[organize(include=["alertname", "severity", "job", "instance"],
                                        order=["alertname", "severity", "job", "instance"],
                                        rename={"alertname": "Alerta", "severity": "Severidade",
                                                "job": "Alvo", "instance": "Instância"})],
              no_value="nenhum alerta disparando"),
    )

    d.row("Tendência")
    d.add(
        timeseries("Requisições por segundo, por classe de status",
                   "Tráfego empilhado por classe de status; uma faixa vermelha (5xx) crescendo indica falha do servidor.",
                   [prom('sum by (status_class) (label_replace(rate(%s[$__rate_interval]), "status_class", "${1}xx", "status", "([0-9]).."))'
                         % http("count"), "{{status_class}}")],
                   unit="reqps", decimals=2, stack=True, overrides=STATUS_CLASS_COLORS),
        timeseries("Latência p50, p95 e p99",
                   "Percentis de latência da API; os losangos são exemplares, clique para abrir o trace daquela requisição.",
                   [prom(hq(0.50), "p50", exemplar=True),
                    prom(hq(0.95), "p95", exemplar=True),
                    prom(hq(0.99), "p99", exemplar=True)],
                   unit="s", decimals=3),
        timeseries("Taxa de erro 5xx ao longo do tempo",
                   "Fração das requisições com erro do servidor ao longo do tempo; picos mostram quando os erros começaram.",
                   [prom('sum(rate(%s[$__rate_interval])) / sum(rate(%s[$__rate_interval]))'
                         % (http("count", 'status=~"5.."'), http("count")), "erro 5xx")],
                   unit="percentunit", decimals=2, minv=0, fill=20,
                   overrides=[ov("erro 5xx", [fixed("red")])]),
        timeseries("Saturação",
                   "Uso de cada recurso em relação ao seu limite; linhas perto de 100% indicam gargalo prestes a travar a API.",
                   [prom('sum(tomcat_threads_busy_threads{@A, %s}) / sum(tomcat_threads_config_max_threads{@A, %s})' % (TOMCAT_API, TOMCAT_API),
                         "Threads do Tomcat"),
                    prom('sum(hikaricp_connections_active{@A}) / sum(hikaricp_connections_max{@A})',
                         "Pool do Hikari"),
                    prom('max(jvm_gc_live_data_size_bytes{@A} / jvm_gc_max_data_size_bytes{@A})',
                         "Heap após GC"),
                    prom('1 - avg(rate(node_cpu_seconds_total{job="node", mode="idle"}[$__rate_interval]))',
                         "CPU do servidor")],
                   unit="percentunit", decimals=0, minv=0, maxv=1,
                   thresholds=th_ok(0.7, 0.9), thr_style="dashed"),
    )
    return d


# ==========================================================================
# 2. Endpoints
# ==========================================================================
EP = ('uri=~"$uri"', 'method=~"$method"')


def endpoints():
    d = Dash("gsb-endpoints", "GeoSegBar · Endpoints",
             "Ranking e detalhe de cada rota (método e URI) da API, com foco em achar o gargalo.",
             variables=[
                 var_label("uri", "URI",
                           'http_server_requests_seconds_count{job="geosegbar-api"}'),
                 var_label("method", "Método",
                           'http_server_requests_seconds_count{job="geosegbar-api"}'),
             ], deploy=True)

    cnt_all = http("count", *EP)
    d.row("Ranking de endpoints")
    d.add(text("Como ler este painel",
               "Resume o que cada coluna da tabela de endpoints significa.",
               "A coluna **Tempo total (s/s)** é o ranking de gargalo: segundos de servidor gastos por segundo, "
               "isto é, a média de threads ocupadas por aquela rota. Uma rota de 300 ms chamada 50 vezes por segundo "
               "pesa mais que uma de 5 s chamada uma vez por hora. Clique em um URI para filtrar o painel.",
               h=3))
    d.add(table(
        "Endpoints",
        "Cada linha é um método e URI na janela escolhida; ordene pela coluna Tempo total para achar o maior consumidor de servidor, que é o gargalo.",
        [
            prom('sum by (method, uri) (rate(%s[$__range]))' % cnt_all, instant=True, fmt="table"),
            prom(hq(0.95, *EP, by="method, uri", rng="$__range"), instant=True, fmt="table"),
            prom(hq(0.99, *EP, by="method, uri", rng="$__range"), instant=True, fmt="table"),
            prom('(sum by (method, uri) (rate(%s[$__range])) / sum by (method, uri) (rate(%s[$__range]))) or (sum by (method, uri) (rate(%s[$__range])) * 0)'
                 % (http("count", *EP, 'status=~"5.."'), cnt_all, cnt_all), instant=True, fmt="table"),
            prom('sum by (method, uri) (rate(%s[$__range]))' % http("sum", *EP), instant=True, fmt="table"),
        ],
        h=14,
        transformations=[
            merge(),
            organize(exclude=["Time"],
                     order=["method", "uri", "Value #E", "Value #A", "Value #B", "Value #C", "Value #D"],
                     rename={"Value #A": "Req/s", "Value #B": "p95", "Value #C": "p99",
                             "Value #D": "% erro 5xx", "Value #E": "Tempo total (s/s)"}),
        ],
        sort_by="Tempo total (s/s)",
        overrides=[
            col("uri", width=420, links=[{
                "title": "Filtrar por este endpoint",
                "url": "/d/gsb-endpoints?var-uri=${__data.fields.uri}&${__url_time_range}",
            }]),
            col("Req/s", unit="reqps", decimals=3),
            col("p95", unit="s", decimals=3, cell=CELL_TEXT, thresholds=th_ok(1, 2.5)),
            col("p99", unit="s", decimals=3, cell=CELL_TEXT, thresholds=th_ok(2, 5)),
            col("% erro 5xx", unit="percentunit", decimals=2, cell=CELL_TEXT,
                thresholds=th_ok(0.005, 0.01)),
            col("Tempo total (s/s)", unit="short", decimals=3, cell=CELL_GAUGE,
                minv=0, thresholds=th("green", ("yellow", 0.5), ("red", 2))),
        ],
    ))

    d.row("Os 10 maiores")
    d.add(
        timeseries("Top 10 por requisições por segundo",
                   "Os 10 endpoints mais chamados ao longo do tempo; mostra quem concentra o tráfego.",
                   [prom('topk(10, sum by (method, uri) (rate(%s[$__rate_interval])))' % cnt_all,
                         "{{method}} {{uri}}")],
                   w=8, unit="reqps", decimals=2),
        timeseries("Top 10 por latência p95",
                   "Os 10 endpoints mais lentos no percentil 95; os losangos são exemplares que abrem o trace.",
                   [prom('topk(10, %s)' % hq(0.95, *EP, by="method, uri"),
                         "{{method}} {{uri}}", exemplar=True)],
                   w=8, unit="s", decimals=3),
        timeseries("Top 10 por tempo total consumido",
                   "Segundos de servidor gastos por segundo em cada endpoint (threads ocupadas em média); quem está no topo é o gargalo.",
                   [prom('topk(10, sum by (method, uri) (rate(%s[$__rate_interval])))' % http("sum", *EP),
                         "{{method}} {{uri}}")],
                   w=8, unit="short", decimals=3),
    )

    d.row("Endpoint selecionado")
    d.add(
        heatmap("Distribuição de latência",
                "Cada coluna é um instante e as cores mostram quantas requisições caíram em cada faixa de tempo; manchas no alto indicam lentidão.",
                [prom('sum by (le) (increase(%s[$__rate_interval]))' % http("bucket", *EP),
                      "{{le}}", fmt="heatmap")], w=12, h=9),
        timeseries("Status por segundo",
                   "Requisições por segundo do endpoint selecionado, separadas por código de status.",
                   [prom('sum by (status) (rate(%s[$__rate_interval]))' % cnt_all, "{{status}}")],
                   w=12, h=9, unit="reqps", decimals=2, stack=True, overrides=STATUS_CODE_COLORS),
    )

    d.row("Traces lentos")
    d.add(traces_table("Traces com mais de 1 segundo",
                       "Traces da API com duração acima de 1 segundo; clique no ID para abrir o trace completo no Tempo.",
                       '{resource.service.name="geosegbar-api" && duration > 1s}'))
    return d


# ==========================================================================
# 3. Erros
# ==========================================================================
def errors():
    d = Dash("gsb-errors", "GeoSegBar · Erros",
             "Onde e por que a API está falhando: erros HTTP, exceções, logs de erro e traces com falha.",
             deploy=True)

    d.row("Resumo da janela")
    d.add(
        stat("Respostas 5xx",
             "Total de respostas de erro do servidor na janela de tempo selecionada.",
             [prom('sum(increase(%s[$__range])) or vector(0)' % http("count", 'status=~"5.."'), instant=True)],
             w=6, unit="short", decimals=0, thresholds=th("green", ("yellow", 1), ("red", 20)),
             color_mode="background", graph="none"),
        stat("Respostas 4xx",
             "Total de respostas de erro do cliente (rota inexistente, sem permissão, dados inválidos) na janela.",
             [prom('sum(increase(%s[$__range])) or vector(0)' % http("count", 'status=~"4.."'), instant=True)],
             w=6, unit="short", decimals=0, thresholds=BLUE, graph="none"),
        stat("Exceções registradas",
             "Quantidade de requisições que terminaram com alguma exceção Java na janela.",
             [prom('sum(increase(%s[$__range])) or vector(0)' % http("count", 'exception!="none"'), instant=True)],
             w=6, unit="short", decimals=0, thresholds=th("green", ("yellow", 1), ("red", 50)),
             graph="none"),
        stat("Logs em nível ERROR",
             "Quantidade de linhas de log em nível ERROR escritas pela API na janela.",
             [prom('sum(increase(logback_events_total{@A, level="error"}[$__range])) or vector(0)', instant=True)],
             w=6, unit="short", decimals=0, thresholds=th("green", ("yellow", 1), ("red", 50)),
             graph="none"),
    )

    d.row("Erros HTTP")
    d.add(
        timeseries("5xx por endpoint (top 10)",
                   "Os 10 endpoints que mais devolvem erro do servidor, em respostas por segundo.",
                   [prom('topk(10, sum by (method, uri) (rate(%s[$__rate_interval])))'
                         % http("count", 'status=~"5.."'), "{{method}} {{uri}}")],
                   unit="reqps", decimals=3),
        timeseries("4xx por status",
                   "Erros do cliente por código (400, 401, 403, 404, 429...); um código dominante aponta a causa.",
                   [prom('sum by (status) (rate(%s[$__rate_interval]))' % http("count", 'status=~"4.."'),
                         "{{status}}")],
                   unit="reqps", decimals=3, stack=True),
        timeseries("Exceções por tipo",
                   "Exceções Java por classe, em ocorrências por segundo; o nome da classe indica a origem do problema.",
                   [prom('sum by (exception) (rate(%s[$__rate_interval]))' % http("count", 'exception!="none"'),
                         "{{exception}}")],
                   unit="ops", decimals=3, stack=True),
        timeseries("Linhas de log por nível",
                   "Linhas de log de ERROR e WARN por segundo; um salto em ERROR costuma preceder reclamação de usuário.",
                   [prom('sum(rate(logback_events_total{@A, level="error"}[$__rate_interval]))', "error"),
                    prom('sum(rate(logback_events_total{@A, level="warn"}[$__rate_interval]))', "warn")],
                   unit="ops", decimals=3,
                   overrides=[ov("error", [fixed("red")]), ov("warn", [fixed("yellow")])]),
    )
    d.add(table("Exceções por endpoint",
                "Combinações de exceção, método e URI com mais ocorrências na janela, para localizar de onde cada erro vem.",
                [prom('topk(15, sum by (exception, method, uri) (increase(%s[$__range])))'
                      % http("count", 'exception!="none"'), instant=True, fmt="table")],
                h=8,
                transformations=[organize(exclude=["Time"],
                                          order=["exception", "method", "uri", "Value"],
                                          rename={"exception": "Exceção", "method": "Método",
                                                  "uri": "URI", "Value": "Ocorrências"})],
                sort_by="Ocorrências",
                overrides=[col("Ocorrências", unit="short", decimals=0, cell=CELL_GAUGE, minv=0,
                               thresholds=th("yellow", ("red", 50)))],
                no_value="nenhuma exceção na janela"))

    d.row("Logs e traces de erro")
    d.add(
        logs("Logs de erro da API",
             "Últimas linhas de log em nível ERROR da API, da mais recente para a mais antiga; abra uma linha para ver o stack trace e o traceId.",
             '{service="api", level="error"}', h=10),
        logs("Respostas 5xx no nginx",
             "Últimas requisições que o nginx registrou com status 500 ou maior, com URI, IP e request_id.",
             '{container="nginx-prod"} | json | status >= 500', h=10),
        traces_table("Traces com erro",
                     "Traces da API marcados com erro; clique no ID para ver qual etapa falhou.",
                     '{resource.service.name="geosegbar-api" && status = error}'),
    )
    return d


# ==========================================================================
# 4. Tráfego (nginx)
# ==========================================================================
NG = '{@N} | json'


def NGJ(*campos):
    """Seletor do nginx extraindo SÓ os campos pedidos do JSON.

    `| json` sem lista transforma todo campo em rótulo, e ts/request_id mudam a
    cada linha: cada requisição vira uma série. Em consulta de métrica isso
    multiplica a memória do Loki e faz quantile_over_time calcular o percentil
    de uma amostra só (o resultado vira o máximo). Use NG puro só em painel de
    logs.
    """
    return '{@N} | json ' + ', '.join('%s="%s"' % (c, c) for c in campos)


def traffic():
    d = Dash("gsb-traffic", "GeoSegBar · Tráfego (nginx)",
             "Quem acessa e o que trafega, lido do log de acesso do nginx: volume, IPs, user agents, bytes e lentidão.")

    d.row("Resumo da janela")
    d.add(
        stat("Requisições",
             "Total de requisições que o nginx atendeu na janela de tempo selecionada.",
             [loki('sum(count_over_time({@N} [$__range]))', instant=True)],
             w=6, unit="short", decimals=0, graph="none"),
        stat("IPs de cliente distintos",
             "Quantidade de IPs diferentes que acessaram na janela; salto repentino pode indicar varredura ou ataque.",
             [loki('count(sum by (client_ip) (count_over_time(%s | __error__="" [$__range])))' % NGJ("client_ip"), instant=True)],
             w=6, unit="short", decimals=0, graph="none"),
        stat("Dados enviados",
             "Total de bytes que o nginx devolveu aos clientes na janela.",
             [loki('sum(sum_over_time(%s | unwrap bytes | __error__="" [$__range]))' % NGJ("bytes"), instant=True)],
             w=6, unit="decbytes", decimals=1, graph="none"),
        stat("Tempo de resposta p95",
             "Tempo em que 95% das requisições foram respondidas, medido pelo nginx na janela.",
             [loki('max(quantile_over_time(0.95, %s | unwrap rt | __error__="" [$__range]) by (container))' % NGJ("rt"), instant=True)],
             w=6, unit="s", decimals=2, thresholds=th_ok(1, 2.5), graph="none"),
    )

    d.row("Fluxo")
    d.add(
        timeseries("Requisições por intervalo, por status",
                   "Contagem de requisições do nginx em cada intervalo do gráfico (mínimo de cerca de 1 minuto), separada por status HTTP.",
                   [loki('sum by (status) (count_over_time(%s | __error__="" [$__interval]))' % NGJ("status"), "{{status}}")],
                   unit="short", decimals=0, stack=True, overrides=STATUS_CODE_COLORS),
        timeseries("Tempo de resposta no nginx",
                   "Percentis do tempo de resposta registrado no log do nginx; a diferença para o p95 da API mostra o custo de rede e proxy.",
                   [loki('max(quantile_over_time(0.5, %s | unwrap rt | __error__="" [$__interval]) by (container))' % NGJ("rt"), "p50"),
                    loki('max(quantile_over_time(0.95, %s | unwrap rt | __error__="" [$__interval]) by (container))' % NGJ("rt"), "p95"),
                    loki('max(quantile_over_time(0.99, %s | unwrap rt | __error__="" [$__interval]) by (container))' % NGJ("rt"), "p99")],
                   unit="s", decimals=3),
        timeseries("Bytes enviados por intervalo",
                   "Volume de dados devolvidos aos clientes em cada intervalo; picos costumam ser downloads grandes de arquivos.",
                   [loki('sum(sum_over_time(%s | unwrap bytes | __error__="" [$__interval]))' % NGJ("bytes"), "bytes")],
                   unit="decbytes", decimals=1, legend="hidden"),
        pie("App móvel contra web",
            "Participação do aplicativo móvel (Dart, okhttp, CFNetwork) e dos navegadores nas requisições da janela.",
            [loki('sum(count_over_time(%s | __error__="" | ua=~".*(Dart|okhttp|CFNetwork).*" [$__range]))' % NGJ("ua"),
                  "App móvel", instant=True),
             loki('sum(count_over_time(%s | __error__="" | ua!~".*(Dart|okhttp|CFNetwork).*" [$__range]))' % NGJ("ua"),
                  "Web", instant=True)]),
    )

    d.row("Quem acessa")
    d.add(
        table("Top 15 IPs de cliente",
              "Os 15 IPs que mais fizeram requisições na janela; um IP muito acima dos outros merece investigação.",
              [loki('topk(15, sum by (client_ip) (count_over_time(%s | __error__="" [$__range])))' % NGJ("client_ip"), instant=True)],
              w=8, h=10,
              transformations=[labels_to_fields(), merge(),
                               organize(exclude=["Time"], order=["client_ip", "Value"],
                                        rename={"client_ip": "IP do cliente", "Value": "Requisições"})],
              sort_by="Requisições", decimals=0,
              overrides=[col("Requisições", unit="short", decimals=0, cell=CELL_GAUGE, minv=0)]),
        table("Top user agents",
              "Os 15 user agents mais frequentes na janela; separa navegadores, o aplicativo móvel e robôs.",
              [loki('topk(15, sum by (ua) (count_over_time(%s | __error__="" [$__range])))' % NGJ("ua"), instant=True)],
              w=8, h=10,
              transformations=[labels_to_fields(), merge(),
                               organize(exclude=["Time"], order=["ua", "Value"],
                                        rename={"ua": "User agent", "Value": "Requisições"})],
              sort_by="Requisições", decimals=0,
              overrides=[col("Requisições", unit="short", decimals=0, cell=CELL_GAUGE, minv=0)]),
        table("Top 15 URIs",
              "Os 15 caminhos mais pedidos ao nginx na janela, antes de chegar à API.",
              [loki('topk(15, sum by (uri) (count_over_time(%s | __error__="" [$__range])))' % NGJ("uri"), instant=True)],
              w=8, h=10,
              transformations=[labels_to_fields(), merge(),
                               organize(exclude=["Time"], order=["uri", "Value"],
                                        rename={"uri": "URI", "Value": "Requisições"})],
              sort_by="Requisições", decimals=0,
              overrides=[col("Requisições", unit="short", decimals=0, cell=CELL_GAUGE, minv=0)]),
    )

    d.row("Requisições lentas")
    d.add(logs("Requisições com mais de 2 segundos",
               "Linhas do log do nginx cujo tempo de resposta passou de 2 segundos, com URI, IP do cliente e request_id para cruzar com o trace.",
               '{container="nginx-prod"} | json | rt > 2', h=10))
    return d


# ==========================================================================
# 5. Banco de dados
# ==========================================================================
DB = 'datname=~"$datname"'


def pg(name, *extra):
    return name + sel('job="postgres"', *extra)


def database():
    d = Dash("gsb-database", "GeoSegBar · Banco de dados",
             "PostgreSQL (conexões, transações, cache, locks, tabelas, consultas) e pool de conexões HikariCP da API.",
             variables=[var_label("datname", "Banco", 'pg_database_size_bytes{job="postgres"}')],
             deploy=True)

    d.row("PostgreSQL")
    hit = pg("pg_stat_database_blks_hit", DB)
    read = pg("pg_stat_database_blks_read", DB)
    d.add(
        stat("PostgreSQL no ar",
             "Resultado da conexão do exporter com o banco; NO AR significa que o Postgres respondeu à última coleta.",
             [prom('min(pg_up{job="postgres"})')],
             mappings=map_ok_fail(), thresholds=th("red", ("green", 1)),
             color_mode="background", graph="none"),
        stat("Uso do limite de conexões",
             "Conexões abertas em relação ao máximo configurado (max_connections); acima de 90% novas conexões começam a falhar.",
             [prom('sum(pg_stat_database_numbackends{job="postgres"}) / max(pg_settings_max_connections{job="postgres"})')],
             unit="percentunit", decimals=0, thresholds=th_ok(0.7, 0.9)),
        stat("Cache hit",
             "Fração das leituras de bloco atendidas pela memória em vez do disco; abaixo de 95% o banco está lendo muito do disco.",
             [prom('sum(rate(%s[5m])) / (sum(rate(%s[5m])) + sum(rate(%s[5m])))' % (hit, hit, read))],
             unit="percentunit", decimals=2, thresholds=th("red", ("yellow", 0.95), ("green", 0.99))),
        stat("Deadlocks na janela",
             "Quantidade de deadlocks detectados pelo Postgres na janela; o ideal é zero.",
             [prom('sum(increase(%s[$__range]))' % pg("pg_stat_database_deadlocks", DB), instant=True)],
             unit="short", decimals=0, thresholds=th("green", ("red", 1)), graph="none"),
        stat("Transação mais longa",
             "Duração da transação aberta há mais tempo; valores altos seguram locks e impedem a limpeza do vacuum.",
             [prom('max(%s)' % pg("pg_stat_activity_max_tx_duration", DB))],
             unit="s", decimals=1, thresholds=th_ok(60, 300), no_value="nenhuma"),
        stat("Tamanho do banco",
             "Espaço em disco ocupado pelos bancos selecionados.",
             [prom('sum(%s)' % pg("pg_database_size_bytes", DB))],
             unit="decbytes", decimals=1, graph="none"),
    )
    d.add(
        timeseries("Conexões por estado",
                   "Conexões abertas separadas por estado; muitas em idle in transaction indicam transações esquecidas abertas.",
                   [prom('sum by (state) (%s)' % pg("pg_stat_activity_count", DB), "{{state}}"),
                    prom('max(pg_settings_max_connections{job="postgres"})', "máximo")],
                   unit="short", decimals=0, stack=True, minv=0,
                   overrides=[ov("máximo", [fixed("red"), DASH_LINE,
                                            ("custom.stacking", {"group": False, "mode": "none"}),
                                            ("custom.fillOpacity", 0)])]),
        timeseries("Maior transação aberta, por estado",
                   "Tempo da transação mais antiga em cada estado; uma linha subindo sem parar é uma transação travada.",
                   [prom('max by (state) (%s)' % pg("pg_stat_activity_max_tx_duration", DB), "{{state}}")],
                   unit="s", decimals=1),
    )
    d.add(
        timeseries("Transações por segundo",
                   "Commits e rollbacks por segundo; rollbacks constantes indicam erro de aplicação ou conflito de concorrência.",
                   [prom('sum(rate(%s[$__rate_interval]))' % pg("pg_stat_database_xact_commit", DB), "commit"),
                    prom('sum(rate(%s[$__rate_interval]))' % pg("pg_stat_database_xact_rollback", DB), "rollback")],
                   unit="ops", decimals=2,
                   overrides=[ov("commit", [fixed("green")]), ov("rollback", [fixed("red")])]),
        timeseries("Cache hit ao longo do tempo",
                   "Fração das leituras servidas da memória; queda sustentada abaixo de 95% pede mais memória ou índices.",
                   [prom('sum(rate(%s[$__rate_interval])) / (sum(rate(%s[$__rate_interval])) + sum(rate(%s[$__rate_interval])))'
                         % (hit, hit, read), "cache hit")],
                   unit="percentunit", decimals=2, maxv=1, legend="hidden"),
    )
    d.add(
        timeseries("Deadlocks detectados",
                   "Deadlocks detectados em cada intervalo do gráfico; qualquer barra merece investigação.",
                   [prom('sum(increase(%s[$__rate_interval]))' % pg("pg_stat_database_deadlocks", DB), "deadlocks")],
                   w=8, unit="short", decimals=0, bars=True, legend="hidden"),
        timeseries("Arquivos temporários",
                   "Bytes por segundo gravados em arquivos temporários; consultas que estouram a memória de ordenação aparecem aqui.",
                   [prom('sum(rate(%s[$__rate_interval]))' % pg("pg_stat_database_temp_bytes", DB), "temporários")],
                   w=8, unit="Bps", decimals=1, legend="hidden"),
        timeseries("Locks por modo",
                   "Locks ativos por modo; muitos em modos exclusivos por muito tempo indicam bloqueio entre consultas.",
                   [prom('sum by (mode) (%s)' % pg("pg_locks_count", DB), "{{mode}}")],
                   w=8, unit="short", decimals=0, stack=True),
    )
    d.add(
        timeseries("Tamanho do banco ao longo do tempo",
                   "Espaço ocupado em disco por banco ao longo do tempo; use para prever quando o disco enche.",
                   [prom(pg("pg_database_size_bytes", DB), "{{datname}}")],
                   unit="decbytes", decimals=1),
        bargauge("Top 10 tabelas por tuplas mortas",
                 "Tabelas com mais linhas mortas aguardando vacuum; valores altos incham a tabela e deixam consultas lentas.",
                 [prom('topk(10, sum by (relname) (%s))' % pg("pg_stat_user_tables_n_dead_tup"),
                       "{{relname}}", instant=True)],
                 unit="short", decimals=0, thresholds=th_ok(10000, 100000)),
    )
    d.add(
        bargauge("Top 10 tabelas por leituras sequenciais",
                 "Tabelas mais lidas por varredura completa na janela; números altos em tabelas grandes sugerem falta de índice.",
                 [prom('topk(10, sum by (relname) (increase(%s[$__range])))' % pg("pg_stat_user_tables_seq_scan"),
                       "{{relname}}", instant=True)],
                 unit="short", decimals=0, thresholds=th("green", ("yellow", 1000), ("red", 10000))),
        bargauge("Top 10 tabelas por leituras por índice",
                 "Tabelas mais lidas via índice na janela; serve de comparação com as leituras sequenciais ao lado.",
                 [prom('topk(10, sum by (relname) (increase(%s[$__range])))' % pg("pg_stat_user_tables_idx_scan"),
                       "{{relname}}", instant=True)],
                 unit="short", decimals=0, thresholds=BLUE),
    )

    d.row("Pool de conexões (HikariCP)")
    d.add(
        timeseries("Conexões do pool",
                   "Conexões ativas, ociosas e pendentes contra o máximo do pool; pendentes acima de zero por muito tempo é gargalo.",
                   [prom('sum by (pool) (%s)' % api("hikaricp_connections_active"), "{{pool}} ativas"),
                    prom('sum by (pool) (%s)' % api("hikaricp_connections_idle"), "{{pool}} ociosas"),
                    prom('sum by (pool) (%s)' % api("hikaricp_connections_pending"), "{{pool}} pendentes"),
                    prom('sum by (pool) (%s)' % api("hikaricp_connections_max"), "{{pool}} máximo")],
                   unit="short", decimals=0, minv=0,
                   overrides=[ov(".*pendentes", [fixed("red")], regex=True),
                              ov(".*máximo", [fixed("dark-red"), DASH_LINE], regex=True)]),
        timeseries("Tempo para obter conexão",
                   "Quanto a API espera por uma conexão livre do pool, média e máximo; acima de 100 ms o pool está apertado.",
                   [prom('sum(rate(%s[$__rate_interval])) / sum(rate(%s[$__rate_interval]))'
                         % (api("hikaricp_connections_acquire_seconds_sum"),
                            api("hikaricp_connections_acquire_seconds_count")), "média"),
                    prom('max(%s)' % api("hikaricp_connections_acquire_seconds_max"), "máximo")],
                   unit="s", decimals=3),
    )
    d.add(
        timeseries("Tempo de uso da conexão",
                   "Quanto tempo cada conexão fica emprestada à aplicação, média e máximo; valores altos indicam consultas ou transações longas.",
                   [prom('sum(rate(%s[$__rate_interval])) / sum(rate(%s[$__rate_interval]))'
                         % (api("hikaricp_connections_usage_seconds_sum"),
                            api("hikaricp_connections_usage_seconds_count")), "média"),
                    prom('max(%s)' % api("hikaricp_connections_usage_seconds_max"), "máximo")],
                   unit="s", decimals=3),
        timeseries("Timeouts ao obter conexão",
                   "Quantas vezes a API desistiu de esperar uma conexão do pool; qualquer barra significa requisição que falhou por falta de conexão.",
                   [prom('sum(increase(%s[$__rate_interval]))' % api("hikaricp_connections_timeout_total"), "timeouts")],
                   unit="short", decimals=0, bars=True, legend="hidden",
                   overrides=[ov("timeouts", [fixed("red")])]),
    )

    d.row("Consultas mais pesadas (pg_stat_statements)")
    d.add(text("Disponibilidade destes painéis",
               "Explica por que os painéis de consultas podem aparecer sem dados.",
               "Estes painéis dependem da extensão `pg_stat_statements` habilitada no Postgres. "
               "Enquanto ela não estiver ativa, mostram **No data**, e isso é esperado.",
               h=2))
    d.add(table(
        "Top 10 consultas por tempo total",
        "As 10 consultas que mais consumiram tempo de banco na janela, com o texto do SQL; é onde otimizar rende mais.",
        [prom('topk(10, sum by (queryid) (increase(pg_stat_statements_seconds_total{job="postgres"}[$__range])) '
              '* on(queryid) group_left(query) pg_stat_statements_query_id{job="postgres"})',
              instant=True, fmt="table")],
        h=11,
        transformations=[organize(exclude=["Time"], order=["query", "queryid", "Value"],
                                  rename={"query": "Consulta", "queryid": "ID", "Value": "Tempo total (s)"})],
        sort_by="Tempo total (s)",
        overrides=[col("Tempo total (s)", unit="s", decimals=2, cell=CELL_GAUGE, minv=0),
                   col("ID", width=170)],
        no_value="pg_stat_statements não habilitado"))
    d.add(
        timeseries("Top 10 consultas por chamadas por segundo",
                   "As 10 consultas mais executadas, identificadas pelo queryid (o texto está na tabela acima).",
                   [prom('topk(10, sum by (queryid) (rate(pg_stat_statements_calls_total{job="postgres"}[$__rate_interval])))',
                         "{{queryid}}")],
                   unit="ops", decimals=2),
        timeseries("Top 10 consultas por tempo gasto por segundo",
                   "Segundos de banco gastos por segundo em cada consulta; é a mesma ideia do tempo total, ao longo do tempo.",
                   [prom('topk(10, sum by (queryid) (rate(pg_stat_statements_seconds_total{job="postgres"}[$__rate_interval])))',
                         "{{queryid}}")],
                   unit="short", decimals=3),
    )

    d.row("Consultas JDBC da API")
    d.add(
        timeseries("Consultas JDBC por segundo",
                   "Consultas SQL executadas pela aplicação por segundo; só aparece quando a instrumentação JDBC está ligada.",
                   [prom('sum(rate(%s[$__rate_interval]))' % api("jdbc_query_seconds_count"), "consultas")],
                   unit="ops", decimals=2, legend="hidden"),
        timeseries("Duração das consultas JDBC",
                   "Tempo médio e máximo de cada consulta SQL vista pela aplicação; o máximo aponta consultas pontualmente lentas.",
                   [prom('sum(rate(%s[$__rate_interval])) / sum(rate(%s[$__rate_interval]))'
                         % (api("jdbc_query_seconds_sum"), api("jdbc_query_seconds_count")), "média"),
                    prom('max(%s)' % api("jdbc_query_seconds_max"), "máximo")],
                   unit="s", decimals=3),
    )
    return d


# ==========================================================================
# 6. JVM e executores
# ==========================================================================
INST = 'instance=~"$instance"'


def jv(name, *extra):
    return api(name, INST, *extra)


def jvm():
    d = Dash("gsb-jvm", "GeoSegBar · JVM e executores",
             "Saúde da JVM da API: memória, coleta de lixo, threads, CPU, Tomcat, executores assíncronos e cliente Redis.",
             variables=[var_label("instance", "Instância", 'up{job="geosegbar-api"}')],
             deploy=True)

    d.row("Resumo")
    d.add(
        stat("Tempo no ar",
             "Há quanto tempo cada instância da API está rodando sem reiniciar; reinício recente aparece como valor baixo.",
             [prom('max by (instance) (%s)' % jv("process_uptime_seconds"), "{{instance}}")],
             w=6, unit="s", decimals=1, graph="none", thresholds=BLUE),
        stat("Heap usado",
             "Fração do heap máximo da JVM em uso agora; perto de 100% a coleta de lixo trabalha sem parar.",
             [prom('sum(%s) / sum(%s)' % (jv("jvm_memory_used_bytes", 'area="heap"'),
                                          jv("jvm_memory_max_bytes", 'area="heap"')))],
             w=6, unit="percentunit", decimals=0, thresholds=th_ok(0.75, 0.9)),
        stat("CPU do processo",
             "Fração da CPU do servidor consumida pelo processo Java da API.",
             [prom('max(%s)' % jv("process_cpu_usage"))],
             w=6, unit="percentunit", decimals=0, thresholds=th_ok(0.7, 0.9)),
        stat("Threads vivas",
             "Total de threads existentes na JVM; crescimento contínuo sem queda sugere vazamento de threads.",
             [prom('sum(%s)' % jv("jvm_threads_live_threads"))],
             w=6, unit="short", decimals=0, thresholds=BLUE),
    )

    d.row("Memória")
    d.add(
        timeseries("Heap usado e máximo, por instância",
                   "Memória heap em uso contra o máximo de cada instância; o serrote é normal, a base subindo é vazamento.",
                   [prom('sum by (instance) (%s)' % jv("jvm_memory_used_bytes", 'area="heap"'), "{{instance}} · usado"),
                    prom('sum by (instance) (%s)' % jv("jvm_memory_max_bytes", 'area="heap"'), "{{instance}} · máximo")],
                   unit="bytes", decimals=1, minv=0,
                   overrides=[ov(".*máximo", [fixed("dark-red"), DASH_LINE], regex=True)]),
        timeseries("Heap após a coleta de lixo",
                   "Fração do heap ocupada por dados vivos depois do GC; é o melhor indicador de falta de memória real.",
                   [prom('max by (instance) (%s / %s)' % (jv("jvm_gc_live_data_size_bytes"),
                                                          jv("jvm_gc_max_data_size_bytes")), "{{instance}}")],
                   unit="percentunit", decimals=0, minv=0, maxv=1, thresholds=th_ok(0.7, 0.85), thr_style="dashed"),
    )
    d.add(
        timeseries("Memória não-heap",
                   "Memória fora do heap (metaspace, código compilado) por instância; deve se estabilizar após a subida.",
                   [prom('sum by (instance) (%s)' % jv("jvm_memory_used_bytes", 'area="nonheap"'), "{{instance}}")],
                   unit="bytes", decimals=1),
        timeseries("Memória por região",
                   "Uso de cada região da JVM (Eden, Old Gen, Metaspace...) somado entre instâncias; mostra onde a memória está indo.",
                   [prom('sum by (id) (%s)' % jv("jvm_memory_used_bytes"), "{{id}}")],
                   unit="bytes", decimals=1, stack=True),
    )

    d.row("Coleta de lixo (GC)")
    d.add(
        timeseries("Tempo em pausa de GC",
                   "Fração do tempo em que a aplicação ficou parada pelo GC; acima de 5% a API perde desempenho.",
                   [prom('sum by (gc) (rate(%s[$__rate_interval]))' % jv("jvm_gc_pause_seconds_sum"), "{{gc}}")],
                   w=8, unit="percentunit", decimals=2, thresholds=th_ok(0.02, 0.05), thr_style="dashed"),
        timeseries("Maior pausa de GC",
                   "Maior pausa individual de GC recente por coletor; pausas acima de 1 segundo travam requisições.",
                   [prom('max by (gc) (%s)' % jv("jvm_gc_pause_seconds_max"), "{{gc}}")],
                   w=8, unit="s", decimals=3),
        timeseries("Coletas de GC por segundo",
                   "Quantas coletas acontecem por segundo e de qual tipo; muitas coletas completas indicam heap pequeno demais.",
                   [prom('sum by (gc, action) (rate(%s[$__rate_interval]))' % jv("jvm_gc_pause_seconds_count"),
                         "{{gc}} · {{action}}")],
                   w=8, unit="ops", decimals=3),
    )

    d.row("Threads, classes e CPU")
    d.add(
        timeseries("Threads vivas por instância",
                   "Número de threads de cada instância; uma linha crescente sem parar sugere vazamento.",
                   [prom('sum by (instance) (%s)' % jv("jvm_threads_live_threads"), "{{instance}}")],
                   w=8, unit="short", decimals=0),
        timeseries("Threads por estado",
                   "Threads agrupadas por estado (runnable, waiting, blocked...); muitas em blocked indicam contenção de locks.",
                   [prom('sum by (state) (%s)' % jv("jvm_threads_states_threads"), "{{state}}")],
                   w=8, unit="short", decimals=0, stack=True),
        timeseries("Classes carregadas",
                   "Classes carregadas na JVM por instância; deve estabilizar após o início.",
                   [prom('sum by (instance) (%s)' % jv("jvm_classes_loaded_classes"), "{{instance}}")],
                   w=8, unit="short", decimals=0),
    )
    d.add(
        timeseries("CPU do processo e do sistema",
                   "CPU usada pelo processo Java e pelo sistema inteiro (0 a 100%); a diferença é o que outros processos consomem.",
                   [prom('max by (instance) (%s)' % jv("process_cpu_usage"), "{{instance}} · processo"),
                    prom('max by (instance) (%s)' % jv("system_cpu_usage"), "{{instance}} · sistema")],
                   unit="percentunit", decimals=0, minv=0, maxv=1),
        timeseries("Eventos de log por nível",
                   "Linhas de log por segundo e por nível; aumento de WARN ou ERROR após um deploy sinaliza regressão.",
                   [prom('sum by (level) (rate(%s[$__rate_interval]))' % jv("logback_events_total"), "{{level}}")],
                   unit="ops", decimals=3,
                   overrides=[ov("error", [fixed("red")]), ov("warn", [fixed("yellow")]),
                              ov("info", [fixed("blue")])]),
    )

    d.row("Tomcat")
    d.add(
        timeseries("Threads do Tomcat",
                   "Threads ocupadas, existentes e o máximo configurado; quando ocupadas encostam no máximo, requisições começam a esperar.",
                   [prom('sum(%s)' % jv("tomcat_threads_busy_threads", TOMCAT_API), "ocupadas"),
                    prom('sum(%s)' % jv("tomcat_threads_current_threads", TOMCAT_API), "existentes"),
                    prom('sum(%s)' % jv("tomcat_threads_config_max_threads", TOMCAT_API), "máximo")],
                   unit="short", decimals=0, minv=0,
                   overrides=[ov("máximo", [fixed("dark-red"), DASH_LINE])]),
        timeseries("Saturação do Tomcat",
                   "Threads ocupadas divididas pelo máximo; perto de 100% a API já não aceita trabalho novo.",
                   [prom('sum(%s) / sum(%s)' % (jv("tomcat_threads_busy_threads", TOMCAT_API),
                                                jv("tomcat_threads_config_max_threads", TOMCAT_API)), "saturação")],
                   unit="percentunit", decimals=0, minv=0, maxv=1, legend="hidden",
                   thresholds=th_ok(0.7, 0.9), thr_style="dashed"),
    )

    d.row("Executores assíncronos")
    d.add(
        timeseries("Threads ativas por executor",
                   "Threads trabalhando em cada executor (pool de tarefas assíncronas); encostar no tamanho do pool significa fila.",
                   [prom('sum by (name) (%s)' % jv("executor_active_threads"), "{{name}}")],
                   unit="short", decimals=0),
        timeseries("Tarefas na fila por executor",
                   "Tarefas aguardando uma thread livre em cada executor; fila crescendo é trabalho acumulando.",
                   [prom('sum by (name) (%s)' % jv("executor_queued_tasks"), "{{name}}")],
                   unit="short", decimals=0),
    )
    d.add(
        timeseries("Capacidade restante da fila",
                   "Vagas livres na fila de cada executor (menor valor entre as instâncias); chegando a zero, novas tarefas são rejeitadas.",
                   [prom('min by (name) (%s)' % jv("executor_queue_remaining_tasks"), "{{name}}")],
                   unit="short", decimals=0),
        timeseries("Tamanho do pool e tarefas concluídas",
                   "Tamanho atual do pool de cada executor e quantas tarefas ele conclui por segundo.",
                   [prom('sum by (name) (%s)' % jv("executor_pool_size_threads"), "{{name}} · threads"),
                    prom('sum by (name) (rate(%s[$__rate_interval]))' % jv("executor_completed_tasks_total"),
                         "{{name}} · concluídas/s")],
                   unit="short", decimals=2),
    )

    d.row("Cliente Redis (Lettuce)")
    d.add(
        timeseries("Latência média por comando",
                   "Tempo médio de cada comando Redis visto pela API; valores acima de poucos milissegundos indicam Redis lento ou rede congestionada.",
                   [prom('sum by (command) (rate(%s[$__rate_interval])) / sum by (command) (rate(%s[$__rate_interval]))'
                         % (jv("lettuce_command_completion_seconds_sum"),
                            jv("lettuce_command_completion_seconds_count")), "{{command}}")],
                   unit="s", decimals=4),
        timeseries("Comandos Redis por segundo",
                   "Quantos comandos Redis a API envia por segundo, por tipo de comando.",
                   [prom('sum by (command) (rate(%s[$__rate_interval]))' % jv("lettuce_command_completion_seconds_count"),
                         "{{command}}")],
                   unit="ops", decimals=2, stack=True),
    )
    return d


# ==========================================================================
# 7. Servidor e containers
# ==========================================================================
NODE = 'job="node"'
CADV = 'job="cadvisor", name!=""'
FS = 'job="node", fstype!~"tmpfs|overlay|squashfs|ramfs", mountpoint!~"/etc/.*"'


def host():
    d = Dash("gsb-host", "GeoSegBar · Servidor e containers",
             "Recursos do servidor (CPU, memória, disco, rede), consumo por container, sondas externas, backup e Redis.")

    d.row("Servidor agora")
    d.add(
        stat("CPU do servidor",
             "Fração da CPU em uso nos últimos 5 minutos, somando todos os núcleos.",
             [prom('1 - avg(rate(node_cpu_seconds_total{%s, mode="idle"}[5m]))' % NODE)],
             unit="percentunit", decimals=0, thresholds=th_ok(0.7, 0.9)),
        stat("Memória usada",
             "Fração da memória física que não está disponível para novos processos.",
             [prom('1 - node_memory_MemAvailable_bytes{%s} / node_memory_MemTotal_bytes{%s}' % (NODE, NODE))],
             unit="percentunit", decimals=0, thresholds=th_ok(0.8, 0.92)),
        stat("Disco / usado",
             "Fração do disco raiz já ocupada; perto de 100% o banco e os logs deixam de gravar.",
             [prom('1 - node_filesystem_avail_bytes{%s, mountpoint="/"} / node_filesystem_size_bytes{%s, mountpoint="/"}'
                   % (NODE, NODE))],
             unit="percentunit", decimals=0, thresholds=th_ok(0.75, 0.9)),
        stat("Espaço livre previsto em 24 h",
             "Projeção linear do espaço livre no disco raiz daqui a 24 horas, com base nas últimas 6; negativo significa que o disco enche antes.",
             [prom('predict_linear(node_filesystem_avail_bytes{%s, mountpoint="/"}[6h], 24*3600)' % NODE)],
             unit="decbytes", decimals=1, thresholds=th("red", ("yellow", 0), ("green", 5000000000)),
             graph="none"),
        stat("Servidor ligado há",
             "Tempo desde o último boot do servidor; valor baixo inesperado indica reinício.",
             [prom('time() - node_boot_time_seconds{%s}' % NODE)],
             unit="s", decimals=1, graph="none", thresholds=BLUE),
        stat("Último backup bem-sucedido",
             "Há quanto tempo o backup do banco terminou com sucesso; vermelho se passar de 26 horas.",
             [prom('time() - geosegbar_backup_last_success_timestamp_seconds')],
             unit="s", decimals=1, thresholds=th("green", ("yellow", 86400), ("red", 93600)),
             color_mode="background", graph="none", no_value="sem backup registrado"),
    )
    d.add(
        timeseries("CPU do servidor ao longo do tempo",
                   "Uso total de CPU (1 menos o tempo ocioso) e a parcela por modo; sistema ou iowait altos indicam gargalo de disco ou kernel.",
                   [prom('1 - avg(rate(node_cpu_seconds_total{%s, mode="idle"}[$__rate_interval]))' % NODE, "total"),
                    prom('avg by (mode) (rate(node_cpu_seconds_total{%s, mode!="idle"}[$__rate_interval]))' % NODE,
                         "{{mode}}")],
                   unit="percentunit", decimals=0, minv=0, maxv=1,
                   overrides=[ov("total", [fixed("red"), ("custom.fillOpacity", 0), ("custom.lineWidth", 2)])]),
        timeseries("Carga do sistema",
                   "Load average de 1, 5 e 15 minutos contra o número de núcleos; carga acima da linha de CPUs significa processos na fila.",
                   [prom('node_load1{%s}' % NODE, "load 1 min"),
                    prom('node_load5{%s}' % NODE, "load 5 min"),
                    prom('node_load15{%s}' % NODE, "load 15 min"),
                    prom('count(node_cpu_seconds_total{%s, mode="idle"})' % NODE, "CPUs")],
                   unit="short", decimals=2, minv=0,
                   overrides=[ov("CPUs", [fixed("dark-red"), DASH_LINE])]),
    )
    d.add(
        timeseries("Memória usada ao longo do tempo",
                   "Fração da memória física em uso (sem contar cache reaproveitável); acima de 90% o kernel pode matar processos.",
                   [prom('1 - node_memory_MemAvailable_bytes{%s} / node_memory_MemTotal_bytes{%s}' % (NODE, NODE),
                         "usada")],
                   unit="percentunit", decimals=0, minv=0, maxv=1, legend="hidden",
                   thresholds=th_ok(0.8, 0.92), thr_style="dashed"),
        timeseries("Memória disponível e swap em uso",
                   "Memória livre para novos processos e quanto de swap está sendo usado; swap crescendo indica falta de RAM.",
                   [prom('node_memory_MemAvailable_bytes{%s}' % NODE, "memória disponível"),
                    prom('node_memory_SwapTotal_bytes{%s} - node_memory_SwapFree_bytes{%s}' % (NODE, NODE),
                         "swap em uso")],
                   unit="bytes", decimals=1, minv=0),
    )
    d.add(
        timeseries("Uso de disco por ponto de montagem",
                   "Fração ocupada de cada disco real (sem tmpfs nem overlay); acima de 90% é urgente.",
                   [prom('1 - node_filesystem_avail_bytes{%s} / node_filesystem_size_bytes{%s}' % (FS, FS),
                         "{{mountpoint}}")],
                   unit="percentunit", decimals=0, minv=0, maxv=1,
                   thresholds=th_ok(0.75, 0.9), thr_style="dashed"),
        timeseries("Leitura e escrita em disco",
                   "Bytes por segundo lidos e gravados nos discos; picos longos de escrita costumam ser backup ou vacuum.",
                   [prom('sum(rate(node_disk_read_bytes_total{%s}[$__rate_interval]))' % NODE, "leitura"),
                    prom('sum(rate(node_disk_written_bytes_total{%s}[$__rate_interval]))' % NODE, "escrita")],
                   unit="Bps", decimals=1),
    )
    d.add(
        timeseries("Ocupação do disco (I/O)",
                   "Fração do tempo em que cada disco ficou ocupado com operações; perto de 100% o disco é o gargalo.",
                   [prom('rate(node_disk_io_time_seconds_total{%s}[$__rate_interval])' % NODE, "{{device}}")],
                   unit="percentunit", decimals=0, minv=0, maxv=1),
        timeseries("Rede do servidor",
                   "Tráfego de rede recebido (acima do zero) e enviado (abaixo do zero), sem contar interfaces virtuais.",
                   [prom('sum(rate(node_network_receive_bytes_total{%s, device!~"lo|veth.*|docker.*|br-.*"}[$__rate_interval]))' % NODE,
                         "recebido"),
                    prom('sum(rate(node_network_transmit_bytes_total{%s, device!~"lo|veth.*|docker.*|br-.*"}[$__rate_interval]))' % NODE,
                         "enviado")],
                   unit="Bps", decimals=1,
                   overrides=[ov("enviado", [("custom.transform", "negative-Y")])]),
    )

    d.row("Containers")
    d.add(
        timeseries("CPU por container",
                   "Núcleos de CPU consumidos por cada container (top 15); 1 equivale a um núcleo inteiro.",
                   [prom('topk(15, sum by (name) (rate(container_cpu_usage_seconds_total{%s}[$__rate_interval])))' % CADV,
                         "{{name}}")],
                   unit="short", decimals=2, minv=0),
        timeseries("Memória por container",
                   "Memória em uso (working set) de cada container (top 15); é o valor que o kernel usa para decidir matar por falta de memória.",
                   [prom('topk(15, sum by (name) (container_memory_working_set_bytes{%s}))' % CADV, "{{name}}")],
                   unit="bytes", decimals=1, minv=0),
    )
    d.add(
        timeseries("Memória contra o limite do container",
                   "Fração do limite de memória já usada por container que tem limite configurado; perto de 100% o container é morto (OOM).",
                   [prom('topk(15, sum by (name) (container_memory_working_set_bytes{%s}) / (sum by (name) (container_spec_memory_limit_bytes{%s}) > 0))'
                         % (CADV, CADV), "{{name}}")],
                   unit="percentunit", decimals=0, minv=0, maxv=1,
                   thresholds=th_ok(0.8, 0.95), thr_style="dashed"),
        timeseries("Reinícios de container na última hora",
                   "Quantas vezes cada container reiniciou na última hora; qualquer barra além de um deploy é um problema.",
                   [prom('sum by (name) (changes(container_start_time_seconds{%s}[1h]))' % CADV, "{{name}}")],
                   unit="short", decimals=0, bars=True),
    )
    d.add(
        timeseries("Rede recebida por container",
                   "Bytes por segundo recebidos por container (top 10).",
                   [prom('topk(10, sum by (name) (rate(container_network_receive_bytes_total{%s}[$__rate_interval])))' % CADV,
                         "{{name}}")],
                   unit="Bps", decimals=1),
        timeseries("Rede enviada por container",
                   "Bytes por segundo enviados por container (top 10).",
                   [prom('topk(10, sum by (name) (rate(container_network_transmit_bytes_total{%s}[$__rate_interval])))' % CADV,
                         "{{name}}")],
                   unit="Bps", decimals=1),
    )

    d.row("Sondas externas (blackbox)")
    d.add(table(
        "Estado das sondas",
        "Cada linha é um endereço testado de fora; confira se responde, quanto demora, qual código HTTP devolve e quantos dias faltam para o certificado vencer.",
        [prom('max by (instance) (probe_success{job="blackbox"})', instant=True, fmt="table"),
         prom('max by (instance) (probe_duration_seconds{job="blackbox"})', instant=True, fmt="table"),
         prom('max by (instance) (probe_http_status_code{job="blackbox"})', instant=True, fmt="table"),
         prom('min by (instance) (((probe_ssl_earliest_cert_expiry{job="blackbox"} > 0) - time()) / 86400)',
              instant=True, fmt="table")],
        h=7,
        transformations=[merge(),
                         organize(exclude=["Time"],
                                  order=["instance", "Value #A", "Value #B", "Value #C", "Value #D"],
                                  rename={"instance": "Endereço", "Value #A": "Resposta",
                                          "Value #B": "Duração", "Value #C": "Código HTTP",
                                          "Value #D": "Certificado (dias)"})],
        overrides=[
            col("Resposta", mappings=map_ok_fail("OK", "FALHA"), cell=CELL_BG,
                thresholds=th("red", ("green", 1))),
            col("Duração", unit="s", decimals=3, cell=CELL_TEXT, thresholds=th_ok(1, 3)),
            col("Código HTTP", unit="none", decimals=0),
            col("Certificado (dias)", unit="d", decimals=0, cell=CELL_TEXT,
                thresholds=th_low(21, 30)),
        ],
        no_value="nenhuma sonda configurada"))
    d.add(
        timeseries("Duração das sondas",
                   "Tempo total de cada sonda externa; subida sustentada indica lentidão vista pelo usuário.",
                   [prom('probe_duration_seconds{job="blackbox"}', "{{instance}}")],
                   unit="s", decimals=3),
        bargauge("Dias até o certificado TLS vencer",
                 "Dias restantes do certificado de cada endereço; amarelo abaixo de 30 dias e vermelho abaixo de 14.",
                 [prom('min by (instance) (((probe_ssl_earliest_cert_expiry{job="blackbox"} > 0) - time()) / 86400)',
                       "{{instance}}", instant=True)],
                 unit="d", decimals=0, minv=0, maxv=90, thresholds=th_low(14, 30)),
    )

    d.row("Redis")
    d.add(
        stat("Redis no ar",
             "Resultado da coleta do exporter do Redis; NO AR significa que o Redis respondeu.",
             [prom('min(redis_up{job="redis"})')],
             w=4, h=7, mappings=map_ok_fail(), thresholds=th("red", ("green", 1)),
             color_mode="background", graph="none"),
        timeseries("Memória do Redis",
                   "Memória usada pelo Redis e o limite configurado (zero quando não há limite).",
                   [prom('redis_memory_used_bytes{job="redis"}', "usada"),
                    prom('redis_memory_max_bytes{job="redis"} > 0', "limite")],
                   w=5, h=7, unit="bytes", decimals=1, minv=0,
                   overrides=[ov("limite", [fixed("dark-red"), DASH_LINE])]),
        timeseries("Clientes conectados",
                   "Conexões abertas no Redis; crescimento contínuo sugere conexões vazando na aplicação.",
                   [prom('redis_connected_clients{job="redis"}', "clientes")],
                   w=5, h=7, unit="short", decimals=0, legend="hidden"),
        timeseries("Comandos por segundo",
                   "Comandos processados pelo Redis por segundo.",
                   [prom('rate(redis_commands_processed_total{job="redis"}[$__rate_interval])', "comandos")],
                   w=5, h=7, unit="ops", decimals=1, legend="hidden"),
        timeseries("Taxa de acerto do cache",
                   "Fração das leituras de chave que encontraram o valor; queda sustentada indica cache ineficaz ou expirando cedo.",
                   [prom('rate(redis_keyspace_hits_total{job="redis"}[$__rate_interval]) / (rate(redis_keyspace_hits_total{job="redis"}[$__rate_interval]) + rate(redis_keyspace_misses_total{job="redis"}[$__rate_interval]))',
                         "acertos")],
                   w=5, h=7, unit="percentunit", decimals=0, minv=0, maxv=1, legend="hidden"),
    )

    d.row("Kernel")
    d.add(logs("Mortes por falta de memória (OOM)",
               "Linhas do kernel que mencionam falta de memória ou OOM killer; aparecem quando o servidor matou um processo para sobreviver.",
               '{job="kernel"} |~ "(?i)out of memory|oom"', h=7))
    return d


# ==========================================================================
# 8. Segurança
# ==========================================================================
def security():
    d = Dash("gsb-security", "GeoSegBar · Segurança",
             "Sinais de ataque e abuso: logins, MFA, redefinição de senha, bypassKey, bloqueios HTTP, ModSecurity e SSH.")

    d.row("Resumo da janela")
    d.add(
        stat("Uso da bypassKey (métrica)",
             "Quantas vezes a chave de bypass da redefinição de senha foi usada na janela, segundo o contador; qualquer valor acima de zero é vermelho.",
             [prom('sum(increase(%s[$__range])) or vector(0)' % api("geosegbar_auth_bypass_key_total"), instant=True)],
             unit="short", decimals=0, thresholds=th("green", ("red", 1)),
             color_mode="background", graph="none"),
        stat("Uso da bypassKey (logs)",
             "Mesma checagem feita pelos logs da API, que pega também o primeiro uso após reinício; vermelho se houver qualquer linha.",
             [loki('sum(count_over_time({service="api"} |= "[SECURITY] bypassKey" [$__range]))', instant=True)],
             unit="short", decimals=0, thresholds=th("green", ("red", 1)),
             color_mode="background", graph="none", no_value="0"),
        stat("Logins com credencial inválida",
             "Tentativas de login com senha ou usuário errados na janela; salto grande sugere força bruta.",
             [prom('sum(increase(%s[$__range])) or vector(0)'
                   % api("geosegbar_auth_login_total", 'result="invalid_credentials"'), instant=True)],
             unit="short", decimals=0, thresholds=th("green", ("yellow", 20), ("red", 200)), graph="none"),
        stat("Respostas 401, 403 e 429",
             "Total de respostas de acesso negado ou limite excedido na janela.",
             [prom('sum(increase(%s[$__range])) or vector(0)' % http("count", 'status=~"401|403|429"'), instant=True)],
             unit="short", decimals=0, thresholds=th("green", ("yellow", 100), ("red", 1000)), graph="none"),
        stat("Eventos do ModSecurity",
             "Quantidade de linhas do firewall de aplicação (ModSecurity) no log do Apache na janela.",
             [loki('sum(count_over_time({job="apache-modsec"}[$__range]))', instant=True)],
             unit="short", decimals=0, thresholds=th("green", ("yellow", 10), ("red", 100)),
             graph="none", no_value="0"),
        stat("Falhas de SSH",
             "Tentativas de SSH rejeitadas (chave inválida ou usuário inexistente) na janela; é ruído constante na internet, mas picos importam.",
             [loki('sum(count_over_time({job="ssh"} |~ "Failed|Invalid user" [$__range]))', instant=True)],
             unit="short", decimals=0, thresholds=th("green", ("yellow", 100), ("red", 1000)),
             graph="none", no_value="0"),
    )

    d.row("Autenticação")
    d.add(
        timeseries("Logins por resultado",
                   "Logins por minuto separados por resultado; invalid_credentials em alta com success baixo é sinal de ataque.",
                   [prom('sum by (result) (rate(%s[$__rate_interval])) * 60' % api("geosegbar_auth_login_total"),
                         "{{result}}")],
                   unit="opm", decimals=2, stack=True,
                   overrides=[ov("success", [fixed("green")]), ov("invalid_credentials", [fixed("orange")]),
                              ov("error", [fixed("red")]), ov("mfa_required", [fixed("blue")]),
                              ov("account_unavailable", [fixed("yellow")])]),
        timeseries("MFA por resultado",
                   "Verificações do segundo fator por minuto e resultado; muitos invalid_code seguidos indicam tentativa de adivinhar o código.",
                   [prom('sum by (result) (rate(%s[$__rate_interval])) * 60' % api("geosegbar_auth_mfa_total"),
                         "{{result}}")],
                   unit="opm", decimals=2, stack=True,
                   overrides=[ov("success", [fixed("green")]), ov("invalid_code", [fixed("orange")]),
                              ov("expired_code", [fixed("yellow")]), ov("error", [fixed("red")])]),
    )
    d.add(
        timeseries("Redefinições de senha por etapa",
                   "Etapas da redefinição de senha por minuto; muitas em requested sem completed indicam abuso do fluxo.",
                   [prom('sum by (step) (rate(%s[$__rate_interval])) * 60' % api("geosegbar_auth_password_reset_total"),
                         "{{step}}")],
                   unit="opm", decimals=2, stack=True,
                   overrides=[ov("failed", [fixed("red")]), ov("completed", [fixed("green")])]),
        timeseries("bypassKey aceita e rejeitada",
                   "Uso da chave de bypass da redefinição de senha, por resultado; qualquer barra de accepted precisa de explicação.",
                   [prom('sum by (result) (increase(%s[$__rate_interval]))' % api("geosegbar_auth_bypass_key_total"),
                         "{{result}}")],
                   unit="short", decimals=0, bars=True,
                   overrides=[ov("accepted", [fixed("red")]), ov("rejected", [fixed("orange")])]),
    )
    d.add(
        timeseries("Renovações de sessão",
                   "Renovações de token por minuto (POST /user/refresh-token), fora do contador de login; failure alto e contínuo costuma ser app com token vencido em loop.",
                   [prom('sum by (result) (rate(%s[$__rate_interval])) * 60' % api("geosegbar_auth_session_refresh_total"),
                         "{{result}}")],
                   unit="opm", decimals=2, stack=True,
                   overrides=[ov("success", [fixed("green")]), ov("failure", [fixed("orange")])]),
    )

    d.row("Bloqueios HTTP")
    d.add(
        timeseries("401, 403 e 429 por segundo",
                   "Respostas de não autenticado, proibido e limite excedido; picos de 401 ou 429 costumam ser varredura automatizada.",
                   [prom('sum by (status) (rate(%s[$__rate_interval]))' % http("count", 'status=~"401|403|429"'),
                         "{{status}}")],
                   unit="reqps", decimals=3, stack=True),
        table("Top 15 IPs com erro 4xx (nginx)",
              "IPs que mais receberam respostas 4xx do nginx na janela; um IP muito acima dos demais é candidato a bloqueio.",
              [loki('topk(15, sum by (client_ip) (count_over_time(%s | status >= 400 | status < 500 [$__range])))' % NGJ("status", "client_ip"),
                    instant=True)],
              transformations=[labels_to_fields(), merge(),
                              organize(exclude=["Time"], order=["client_ip", "Value"],
                                       rename={"client_ip": "IP do cliente", "Value": "Respostas 4xx"})],
              sort_by="Respostas 4xx", w=12, h=8,
              overrides=[col("Respostas 4xx", unit="short", decimals=0, cell=CELL_GAUGE, minv=0,
                             thresholds=th("yellow", ("red", 100)))]),
    )
    d.add(logs("Eventos [SECURITY] da API",
               "Linhas de log marcadas como SECURITY pela API, entre elas o uso da bypassKey; leia cada uma.",
               '{service="api"} |= "[SECURITY]"', h=9))

    d.row("Firewall de aplicação (ModSecurity)")
    d.add(
        timeseries("Eventos do ModSecurity ao longo do tempo",
                   "Linhas do ModSecurity no log de erro do Apache em cada intervalo; picos indicam varredura ou ataque bloqueado.",
                   [loki('sum(count_over_time({job="apache-modsec"}[$__interval]))', "eventos")],
                   w=24, h=6, unit="short", decimals=0, bars=True, legend="hidden"),
    )
    d.add(logs("Log do ModSecurity",
               "Últimas linhas do ModSecurity, com a regra acionada e o IP de origem.",
               '{job="apache-modsec"}', h=9))

    d.row("SSH")
    d.add(
        timeseries("Falhas de SSH ao longo do tempo",
                   "Tentativas de SSH rejeitadas em cada intervalo (chave inválida ou usuário inexistente).",
                   [loki('sum(count_over_time({job="ssh"} |~ "Failed|Invalid user" [$__interval]))', "falhas")],
                   unit="short", decimals=0, bars=True, legend="hidden",
                   overrides=[ov("falhas", [fixed("orange")])]),
        logs("Logins SSH aceitos",
             "Quem entrou no servidor por SSH: cada linha deve corresponder a uma pessoa conhecida.",
             '{job="ssh"} |= "Accepted"', w=12, h=8),
    )
    d.add(logs("Falhas de SSH recentes",
               "Últimas tentativas de SSH rejeitadas, com usuário e IP de origem.",
               '{job="ssh"} |~ "Failed|Invalid user"', h=8))
    return d


# ==========================================================================
# 9. Jobs e negócio
# ==========================================================================
def jobs():
    d = Dash("gsb-jobs", "GeoSegBar · Jobs e negócio",
             "Tarefas agendadas, coleta da ANA, e-mails, S3, uploads e indicadores de negócio (checklists e leituras).",
             deploy=True)

    sc = "tasks_scheduled_execution_seconds_"
    by = "sum by (code_namespace, code_function)"
    d.row("Tarefas agendadas (@Scheduled)")
    d.add(table(
        "Tarefas agendadas",
        "Cada linha é um método @Scheduled; confira quantas vezes rodou, quantas falhou e quanto tempo levou na janela.",
        [prom('%s (increase(%s[$__range]))' % (by, api(sc + "count")), instant=True, fmt="table"),
         prom('(%s (increase(%s[$__range]))) or (%s (increase(%s[$__range])) * 0)'
              % (by, api(sc + "count", 'outcome="ERROR"'), by, api(sc + "count")), instant=True, fmt="table"),
         prom('%s (increase(%s[$__range])) / %s (increase(%s[$__range]))'
              % (by, api(sc + "sum"), by, api(sc + "count")), instant=True, fmt="table"),
         prom('max by (code_namespace, code_function) (max_over_time(%s[$__range]))' % api(sc + "max"),
              instant=True, fmt="table")],
        h=9,
        transformations=[merge(),
                         organize(exclude=["Time"],
                                  order=["code_namespace", "code_function", "Value #A", "Value #B", "Value #C", "Value #D"],
                                  rename={"code_namespace": "Classe", "code_function": "Método",
                                          "Value #A": "Execuções", "Value #B": "Erros",
                                          "Value #C": "Duração média", "Value #D": "Duração máxima"})],
        sort_by="Erros",
        overrides=[
            col("Execuções", unit="short", decimals=0),
            col("Erros", unit="short", decimals=0, cell=CELL_BG, thresholds=th("green", ("red", 1))),
            col("Duração média", unit="s", decimals=2),
            col("Duração máxima", unit="s", decimals=2, cell=CELL_TEXT, thresholds=th_ok(60, 300)),
        ],
        no_value="nenhuma tarefa agendada executou na janela"))
    d.add(
        timeseries("Falhas dos jobs",
                   "Falhas por job em cada intervalo, contadas na auditoria (os jobs capturam a própria exceção, então a métrica do @Scheduled não as vê) e, à parte, exceções que escaparam do método; barras repetidas no mesmo job indicam job quebrado.",
                   [prom('sum by (task) (increase(%s[$__rate_interval]))' % api("geosegbar_job_runs_total", 'result="failure"'),
                         "{{task}}"),
                    prom('sum by (code_function) (increase(%s[$__rate_interval]))' % api(sc + "count", 'outcome="ERROR"'),
                         "{{code_function}} (exceção)")],
                   unit="short", decimals=0, bars=True, stack=True),
        timeseries("Duração máxima das tarefas",
                   "Maior duração recente de cada método @Scheduled; crescimento ao longo dos dias indica job ficando mais pesado.",
                   [prom('max by (code_function) (%s)' % api(sc + "max"), "{{code_function}}")],
                   unit="s", decimals=2),
    )

    d.row("Coleta da ANA")
    d.add(
        stat("Última coleta com sucesso",
             "Quando a coleta da ANA terminou com sucesso pela última vez (últimos 7 dias); vazio significa nenhuma coleta com sucesso na semana.",
             [prom('max(max_over_time(%s[7d])) * 1000' % api("geosegbar_ana_collection_last_success_timestamp_seconds"))],
             w=6, h=8, unit="dateTimeFromNow", graph="none", thresholds=BLUE, no_value="sem coleta em 7 dias"),
        timeseries("Coletas da ANA por resultado",
                   "Execuções da coleta em cada intervalo, separadas em sucesso e falha.",
                   [prom('sum by (result) (increase(%s[$__rate_interval]))' % api("geosegbar_ana_collection_runs_total"),
                         "{{result}}")],
                   w=9, unit="short", decimals=0, bars=True, stack=True,
                   overrides=[ov("success", [fixed("green")]), ov("failure", [fixed("red")])]),
        timeseries("Estações coletadas por resultado",
                   "Estações da ANA processadas em cada intervalo, por resultado; falhas isoladas podem ser da própria estação.",
                   [prom('sum by (result) (increase(%s[$__rate_interval]))' % api("geosegbar_ana_station_collections_total"),
                         "{{result}}")],
                   w=9, unit="short", decimals=0, bars=True, stack=True,
                   overrides=[ov("success", [fixed("green")]), ov("failure", [fixed("red")])]),
    )
    d.add(
        timeseries("Fila de jobs históricos",
                   "Tamanho da fila de processamento de dados históricos; uma linha que só cresce indica processamento parado.",
                   [prom('sum(%s)' % api("geosegbar_historical_jobs_queue_size"), "na fila")],
                   unit="short", decimals=0, legend="hidden"),
        logs("Logs da ANA",
             "Últimas linhas de log da API que mencionam ANA ou hidrotelemetria; use para entender uma falha de coleta.",
             '{service="api"} |~ `(?i)hidrotelem|\\bANA\\b`', w=12, h=8, max_lines=50),
    )

    d.row("E-mail e S3")
    d.add(
        timeseries("E-mails enviados por modelo",
                   "E-mails por modelo e resultado em cada intervalo; error none é envio com sucesso, qualquer outro valor é falha.",
                   [prom('sum by (template, error) (increase(%s[$__rate_interval]))' % api("geosegbar_email_seconds_count"),
                         "{{template}} · {{error}}")],
                   unit="short", decimals=0, bars=True, stack=True,
                   overrides=[ov(".*· none", [fixed("green")], regex=True)]),
        timeseries("Tempo de envio de e-mail",
                   "Tempo médio de cada envio por modelo; demora alta aponta lentidão do servidor de e-mail.",
                   [prom('sum by (template) (rate(%s[$__rate_interval])) / sum by (template) (rate(%s[$__rate_interval]))'
                         % (api("geosegbar_email_seconds_sum"), api("geosegbar_email_seconds_count")), "{{template}}")],
                   unit="s", decimals=2),
    )
    d.add(
        timeseries("Operações no S3",
                   "Chamadas ao S3 por operação e erro em cada intervalo; error none é sucesso.",
                   [prom('sum by (operation, error) (increase(%s[$__rate_interval]))' % api("geosegbar_s3_seconds_count"),
                         "{{operation}} · {{error}}")],
                   unit="short", decimals=0, bars=True, stack=True,
                   overrides=[ov(".*· none", [fixed("green")], regex=True)]),
        timeseries("Latência média no S3",
                   "Tempo médio de cada operação no S3; subida isolada de uma operação sugere objeto grande ou rede lenta.",
                   [prom('sum by (operation) (rate(%s[$__rate_interval])) / sum by (operation) (rate(%s[$__rate_interval]))'
                         % (api("geosegbar_s3_seconds_sum"), api("geosegbar_s3_seconds_count")), "{{operation}}")],
                   unit="s", decimals=3),
    )

    d.row("Uploads e uso do sistema")
    d.add(
        timeseries("Uploads iniciados (URL pré-assinada)",
                   "Quantas URLs de upload a API emitiu em cada intervalo, por tipo de arquivo.",
                   [prom('sum by (kind) (increase(%s[$__rate_interval]))' % api("geosegbar_upload_presigned_total"),
                         "{{kind}}")],
                   unit="short", decimals=0, bars=True, stack=True),
        timeseries("Uploads concluídos",
                   "Uploads finalizados por tipo e resultado; muitos expired ou failure em relação aos iniciados indicam problema no envio pelo app.",
                   [prom('sum by (kind, result) (increase(%s[$__rate_interval]))' % api("geosegbar_upload_completed_total"),
                         "{{kind}} · {{result}}")],
                   unit="short", decimals=0, bars=True, stack=True,
                   overrides=[ov(".*· success", [fixed("green")], regex=True),
                              ov(".*· failure", [fixed("red")], regex=True),
                              ov(".*· expired", [fixed("orange")], regex=True)]),
    )
    d.add(
        timeseries("Checklists enviados por hora",
                   "Checklists enviados na hora anterior a cada ponto (janela móvel); queda forte em horário comercial indica problema no app.",
                   [prom('sum(increase(%s[1h]))' % api("geosegbar_checklist_submitted_total"), "checklists")],
                   unit="short", decimals=0, legend="hidden"),
        timeseries("Leituras de instrumentos criadas por hora",
                   "Leituras registradas na hora anterior a cada ponto (janela móvel); mostra o ritmo real de uso do sistema.",
                   [prom('sum(increase(%s[1h]))' % api("geosegbar_readings_recorded_total"), "leituras")],
                   unit="short", decimals=0, legend="hidden"),
    )
    return d


# ==========================================================================
# 10. Saúde do monitoramento
# ==========================================================================
def monitoring():
    d = Dash("gsb-monitoring", "GeoSegBar · Saúde do monitoramento",
             "O monitoramento vigiando a si mesmo: alvos, Prometheus, Alertmanager, Loki, Tempo e alertas disparando.")

    d.row("Resumo")
    d.add(
        stat("Alvos fora do ar",
             "Quantidade de alvos que o Prometheus não consegue raspar; qualquer valor acima de zero é ponto cego.",
             [prom('count(up == 0) or vector(0)')],
             w=6, h=4, unit="short", decimals=0, thresholds=th("green", ("red", 1)),
             color_mode="background", graph="none"),
        stat("Alertas disparando",
             "Alertas em estado firing agora, sem contar o Watchdog.",
             [prom('count(ALERTS{alertstate="firing", alertname!="Watchdog"}) or vector(0)')],
             w=6, h=4, unit="short", decimals=0, thresholds=th("green", ("red", 1)),
             color_mode="background", graph="none"),
        stat("Alertas pendentes",
             "Alertas cuja condição já é verdadeira mas ainda aguardam o tempo mínimo (for) para disparar.",
             [prom('count(ALERTS{alertstate="pending"}) or vector(0)')],
             w=6, h=4, unit="short", decimals=0, thresholds=th("green", ("yellow", 1)),
             color_mode="background", graph="none"),
        stat("Falhas em regras (janela)",
             "Avaliações de regras do Prometheus que falharam na janela; regra quebrada significa alerta que nunca dispara.",
             [prom('sum(increase(prometheus_rule_evaluation_failures_total[$__range])) or vector(0)', instant=True)],
             w=6, h=4, unit="short", decimals=0, thresholds=th("green", ("red", 1)),
             color_mode="background", graph="none"),
    )

    d.row("Alvos e alertas")
    d.add(
        table("Estado dos alvos",
              "Cada linha é um alvo de coleta; NO AR significa que o Prometheus conseguiu raspar na última tentativa e FORA que falhou.",
              [prom('max by (job, instance) (up)', instant=True, fmt="table")],
              w=12, h=10,
              transformations=[organize(exclude=["Time"], order=["job", "instance", "Value"],
                                        rename={"job": "Alvo", "instance": "Instância", "Value": "Estado"})],
              sort_by="Estado", sort_desc=False,
              overrides=[col("Estado", mappings=map_ok_fail(), cell=CELL_BG, thresholds=th("red", ("green", 1)),
                             width=110)]),
        table("Lista de alertas disparando",
              "Alertas em estado firing agora, com severidade e alvo; vazia quando tudo está saudável.",
              [prom('ALERTS{alertstate="firing"}', instant=True, fmt="table")],
              w=12, h=10,
              transformations=[organize(include=["alertname", "severity", "job", "instance"],
                                        order=["alertname", "severity", "job", "instance"],
                                        rename={"alertname": "Alerta", "severity": "Severidade",
                                                "job": "Alvo", "instance": "Instância"})],
              no_value="nenhum alerta disparando"),
    )
    d.add(timeseries("Histórico de alertas",
                     "Quantidade de alertas em disparo ao longo do tempo, por nome; mostra quando cada problema começou e terminou.",
                     [prom('sum by (alertname) (ALERTS{alertstate="firing"})', "{{alertname}}")],
                     w=24, h=7, unit="short", decimals=0, stack=True))

    d.row("Prometheus")
    d.add(
        timeseries("Séries ativas",
                   "Quantidade de séries temporais na memória do Prometheus; crescimento rápido indica explosão de cardinalidade.",
                   [prom('prometheus_tsdb_head_series{job="prometheus"}', "séries")],
                   w=8, unit="short", decimals=0, legend="hidden"),
        timeseries("Armazenamento em disco",
                   "Espaço ocupado pelos blocos de dados do Prometheus; sobe até a retenção configurada e estabiliza.",
                   [prom('prometheus_tsdb_storage_blocks_bytes{job="prometheus"}', "blocos")],
                   w=8, unit="decbytes", decimals=1, legend="hidden"),
        timeseries("Falhas na avaliação de regras",
                   "Avaliações de regras que falharam em cada intervalo; qualquer barra indica regra de alerta ou gravação com erro.",
                   [prom('sum(increase(prometheus_rule_evaluation_failures_total[$__rate_interval]))',
                         "falhas")],
                   w=8, unit="short", decimals=0, bars=True, legend="hidden",
                   overrides=[ov("falhas", [fixed("red")])]),
    )

    d.row("Alertmanager")
    d.add(
        timeseries("Notificações enviadas",
                   "Notificações entregues em cada intervalo, por canal (integração); sem barras quando nenhum alerta disparou.",
                   [prom('sum by (integration) (increase(alertmanager_notifications_total[$__rate_interval]))',
                         "{{integration}}")],
                   unit="short", decimals=0, bars=True, stack=True),
        timeseries("Notificações com falha",
                   "Notificações que o Alertmanager não conseguiu entregar, por canal; qualquer barra significa alerta que ninguém recebeu.",
                   [prom('sum by (integration) (increase(alertmanager_notifications_failed_total[$__rate_interval]))',
                         "{{integration}}")],
                   unit="short", decimals=0, bars=True, stack=True,
                   overrides=[ov(".*", [fixed("red")], regex=True)]),
    )

    d.row("Loki e Tempo")
    d.add(
        timeseries("Logs recebidos pelo Loki",
                   "Bytes de log recebidos por segundo; salto repentino indica serviço gerando log em excesso.",
                   [prom('sum(rate(loki_distributor_bytes_received_total[$__rate_interval]))', "bytes/s")],
                   w=8, unit="Bps", decimals=1, legend="hidden"),
        timeseries("Streams ativos no Loki",
                   "Quantidade de fluxos de log na memória do ingester; crescimento contínuo indica labels demais.",
                   [prom('sum(loki_ingester_memory_streams)', "streams")],
                   w=8, unit="short", decimals=0, legend="hidden"),
        timeseries("Spans recebidos pelo Tempo",
                   "Spans de trace recebidos por segundo; zero com a API em uso significa que o tracing parou.",
                   [prom('sum(rate(tempo_distributor_spans_received_total[$__rate_interval]))', "spans/s")],
                   w=8, unit="ops", decimals=2, legend="hidden"),
    )
    return d


# --------------------------------------------------------------------------
# Registro e execução
# --------------------------------------------------------------------------
DASHBOARDS = [
    ("overview.json", overview),
    ("endpoints.json", endpoints),
    ("errors.json", errors),
    ("traffic.json", traffic),
    ("database.json", database),
    ("jvm.json", jvm),
    ("host.json", host),
    ("security.json", security),
    ("jobs.json", jobs),
    ("monitoring.json", monitoring),
]


def render(obj):
    return json.dumps(obj, indent=2, ensure_ascii=False) + "\n"


def main(argv=None):
    ap = argparse.ArgumentParser(description="Gera os dashboards do Grafana do GeoSegBar.")
    ap.add_argument("--out", default=DEFAULT_OUT, help="pasta de saída (padrão: monitoring/grafana/dashboards)")
    args = ap.parse_args(argv)

    out_dir = os.path.abspath(args.out)
    os.makedirs(out_dir, exist_ok=True)
    expected = set()
    for filename, factory in DASHBOARDS:
        dash = factory().build()
        expected.add(filename)
        with open(os.path.join(out_dir, filename), "w", encoding="utf-8", newline="\n") as fh:
            fh.write(render(dash))
        n = sum(1 for p in dash["panels"] if p["type"] != "row")
        print("escrito %-16s uid=%-16s painéis=%d" % (filename, dash["uid"], n))

    extras = sorted(f for f in os.listdir(out_dir) if f.endswith(".json") and f not in expected)
    for f in extras:
        print("aviso: %s não é gerado por este script" % f, file=sys.stderr)
    print("%d dashboards em %s" % (len(DASHBOARDS), out_dir))
    return 0


if __name__ == "__main__":
    sys.exit(main())
