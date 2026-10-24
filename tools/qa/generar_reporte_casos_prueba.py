#!/usr/bin/env python3
"""Genera target/reports/casos-prueba.html a partir de los resultados de Failsafe.

Cruza dos fuentes:
  - target/failsafe-reports: resultado de cada escenario (aprobado, fallido, omitido).
  - qa/features/**/*.feature: sprint, funcionalidad, etiquetas y pasos Gherkin de cada caso.
"""
import html
import json
import re
import sys
import xml.etree.ElementTree as ET
from collections import OrderedDict
from datetime import datetime
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
METADATA_PATH = Path(__file__).resolve().parent / "casos_metadata.json"
FEATURES_DIR = REPO_ROOT / "src/test/java/com/codefactory/supplychain/qa/features"

CP_ID_RE = re.compile(r"(?:CP-HU|cpHu)(\d{2})-?(\d{3})", re.IGNORECASE)

VERDE = "\033[92m"
ROJO = "\033[91m"
AMARILLO = "\033[93m"
NEGRITA = "\033[1m"
GRIS = "\033[90m"
FIN = "\033[0m"

TIPOS_DE_PRUEBA = OrderedDict([
    ("flujo-principal", "Flujo principal"),
    ("ruta-negativa", "Ruta negativa"),
    ("caso-borde", "Caso borde"),
    ("seguridad", "Seguridad"),
    ("fiabilidad", "Fiabilidad"),
    ("interoperabilidad", "Interoperabilidad"),
    ("integracion", "Integración"),
    ("integridad-de-datos", "Integridad de datos"),
])

ESTADO_INFO = {
    "APROBADO": {"clase": "ok", "icono": "&#10003;", "texto": "Aprobado"},
    "FALLIDO": {"clase": "fail", "icono": "&#10007;", "texto": "Fallido"},
    "OMITIDO": {"clase": "skip", "icono": "&#9888;", "texto": "Omitido"},
    "SIN_EJECUTAR": {"clase": "none", "icono": "&#8211;", "texto": "Sin ejecutar"},
}


def imprimir_resumen_consola(total, aprobados, fallidos, omitidos, resultados, out_file):
    ancho = 58
    linea = "=" * ancho
    print()
    print(f"{NEGRITA}{linea}{FIN}")
    print(f"{NEGRITA}  RESUMEN — {total} Casos de Prueba (QA){FIN}")
    print(linea)
    print(f"  Total de casos    : {total}")
    print(f"  {VERDE}Aprobados         : {aprobados}{FIN}")
    color_f = ROJO if fallidos > 0 else GRIS
    print(f"  {color_f}Fallidos          : {fallidos}{FIN}")
    color_s = AMARILLO if omitidos > 0 else GRIS
    print(f"  {color_s}Omitidos (skip)   : {omitidos}{FIN}")
    print(linea)

    if fallidos > 0:
        print(f"  {ROJO}{NEGRITA}Casos fallidos (defectos):{FIN}")
        for r in resultados:
            if r["estado"] == "FALLIDO":
                print(f"  {ROJO}  - {r['metodo']}{FIN}")
        print(linea)

    resultado = f"{ROJO}HAY FALLAS{FIN}" if fallidos > 0 else f"{VERDE}TODO OK{FIN}"
    print(f"  Resultado: {resultado}")
    print(f"  Reporte HTML: {out_file}")
    print(f"{linea}\n")


# ---------------------------------------------------------------------- fuentes de datos

def cargar_metadata():
    datos = json.loads(METADATA_PATH.read_text(encoding="utf-8"))
    return {c["id"]: c for c in datos}


def parsear_features():
    """Devuelve {cpId: {sprint, archivo, linea, caracteristica, etiquetas, pasos}}."""
    escenarios = {}
    if not FEATURES_DIR.is_dir():
        return escenarios
    for archivo in sorted(FEATURES_DIR.rglob("*.feature")):
        relativo = archivo.relative_to(FEATURES_DIR).as_posix()
        sprint_m = re.match(r"sprint(\d+)/", relativo)
        sprint = int(sprint_m.group(1)) if sprint_m else 0
        caracteristica, antecedentes = "", []
        etiquetas_pendientes, actual, en_antecedentes = [], None, False
        for numero, linea in enumerate(archivo.read_text(encoding="utf-8").splitlines(), start=1):
            texto = linea.strip()
            if not texto or texto.startswith("#"):
                continue
            if texto.startswith("@"):
                etiquetas_pendientes += [t.lstrip("@") for t in texto.split()]
                actual = None
                continue
            if texto.startswith("Característica:"):
                caracteristica = texto.split(":", 1)[1].strip()
                etiquetas_pendientes = []
                continue
            if texto.startswith("Antecedentes:"):
                en_antecedentes, actual = True, None
                continue
            if texto.startswith(("Escenario:", "Esquema del escenario:")):
                en_antecedentes = False
                m = CP_ID_RE.search(texto)
                if not m:
                    etiquetas_pendientes, actual = [], None
                    continue
                cp_id = f"CP-HU{m.group(1)}-{m.group(2)}"
                actual = {
                    "sprint": sprint,
                    "archivo": relativo,
                    "linea": numero,
                    "caracteristica": caracteristica,
                    "etiquetas": etiquetas_pendientes,
                    "pasos": list(antecedentes),
                }
                escenarios[cp_id] = actual
                etiquetas_pendientes = []
                continue
            if en_antecedentes:
                antecedentes.append(texto)
            elif actual is not None:
                actual["pasos"].append(texto)
    return escenarios


ANSI_RE = re.compile(r"(?:\x1b|&amp;#27;|&#27;|&amp#27;|#27;)\[[0-9;]*m")
LOG_RE = re.compile(r"^\d{2,4}[-:]\d{2}[-:]\d{2}[T .]")
GLUE_RE = re.compile(r"\s+# (?:com[./]codefactory|classpath:)\S*.*$")
TOKEN_RE = re.compile(r"(Bearer\s+)[A-Za-z0-9._~+/=-]{12,}")


def limpiar_salida(texto):
    """Deja solo el escenario impreso por Cucumber, sin logs de Spring ni colores."""
    if not texto:
        return None
    texto = ANSI_RE.sub("", texto)
    lineas = texto.split("\n")
    inicio = next((i for i, l in enumerate(lineas) if l.lstrip().startswith(("@", "Escenario:"))), None)
    if inicio is None:
        return None
    limpias = [GLUE_RE.sub("", l).rstrip() for l in lineas[inicio:] if not LOG_RE.match(l)]
    resultado = TOKEN_RE.sub(r"\1<token>", "\n".join(limpias)).strip("\n")
    return resultado or None


def parsear_resultados(target_dir: Path):
    reportes_dir = target_dir / "failsafe-reports"
    casos = []
    if not reportes_dir.is_dir():
        return casos
    for xml_file in sorted(reportes_dir.glob("TEST-*.xml")):
        try:
            root = ET.parse(xml_file).getroot()
        except ET.ParseError:
            continue
        for tc in root.findall("testcase"):
            nombre = tc.get("name", "")
            m = CP_ID_RE.search(nombre)
            cp_id = f"CP-HU{m.group(1)}-{m.group(2)}" if m else nombre
            failure = tc.find("failure")
            error = tc.find("error")
            skipped = tc.find("skipped")
            if failure is not None or error is not None:
                estado = "FALLIDO"
                mensaje = (failure if failure is not None else error).get("message", "")
            elif skipped is not None:
                estado = "OMITIDO"
                mensaje = skipped.get("message", "")
            else:
                estado = "APROBADO"
                mensaje = ""
            system_out = tc.find("system-out")
            casos.append({
                "cpId": cp_id,
                "metodo": nombre,
                "tiempo": tc.get("time", "0"),
                "estado": estado,
                "mensaje": (mensaje or "").strip(),
                "salida": limpiar_salida(system_out.text if system_out is not None else None),
            })
    return casos


# ---------------------------------------------------------------------- utilidades

def sprint_de(cp_id, escenario, meta):
    if escenario:
        return escenario["sprint"]
    if meta and meta.get("sprint"):
        return meta["sprint"]
    return 1


def tipo_de_prueba(etiquetas):
    for clave in TIPOS_DE_PRUEBA:
        if clave in etiquetas:
            return clave
    return None


def polaridad(etiquetas, meta):
    if "positivo" in etiquetas:
        return "Positivo"
    if "negativo" in etiquetas:
        return "Negativo"
    return meta.get("tipo", "") if meta else ""


def nombre_de(resultado, meta):
    nombre = resultado["metodo"]
    m = re.match(r"CP-HU\d{2}-\d{3}\s*[—-]\s*(.*)", nombre)
    if m:
        return m.group(1)
    return meta["nombre"] if meta else nombre


def funcionalidad_de(escenario, meta):
    if escenario and escenario["caracteristica"]:
        return escenario["caracteristica"]
    if meta and meta.get("modulo"):
        return meta["modulo"]
    return "Sin funcionalidad"


def primera_linea(texto):
    """Resumen del fallo: omite el encabezado genérico de RestAssured ("1 expectation failed.")."""
    lineas = [l.strip() for l in (texto or "").split("\n") if l.strip()]
    utiles = [l for l in lineas if not re.match(r"^\d+ expectations? failed\.?$", l)]
    return " ".join(utiles[:2]) if utiles else (lineas[0] if lineas else "")


def conteo(items):
    return {
        "total": len(items),
        "ok": sum(1 for r in items if r["estado"] == "APROBADO"),
        "fail": sum(1 for r in items if r["estado"] == "FALLIDO"),
        "skip": sum(1 for r in items if r["estado"] == "OMITIDO"),
    }


def porcentaje(c):
    ejecutados = c["ok"] + c["fail"]
    return f"{(100 * c['ok'] / ejecutados):.0f} %" if ejecutados else "—"


# ---------------------------------------------------------------------- secciones HTML

def render_filas_resumen(grupos):
    filas = []
    for etiqueta, c in grupos:
        filas.append(
            f"<tr><td>{html.escape(etiqueta)}</td><td class='num'>{c['total']}</td>"
            f"<td class='num ok'>{c['ok']}</td><td class='num fail'>{c['fail']}</td>"
            f"<td class='num skip'>{c['skip']}</td><td class='num'>{porcentaje(c)}</td></tr>")
    return "".join(filas)


def tabla_resumen(titulo, grupos):
    return f"""
    <div class="panel">
      <h3>{html.escape(titulo)}</h3>
      <table class="resumen">
        <thead><tr><th></th><th>Total</th><th>Aprob.</th><th>Fall.</th><th>Omit.</th><th>% aprob.</th></tr></thead>
        <tbody>{render_filas_resumen(grupos)}</tbody>
      </table>
    </div>"""


def render_fallo(resultado, meta, escenario):
    nombre = nombre_de(resultado, meta)
    funcionalidad = funcionalidad_de(escenario, meta)
    ubicacion = f'{escenario["archivo"]}:{escenario["linea"]}' if escenario else ""
    sprint = f'Sprint {escenario["sprint"]}' if escenario else ""
    return f"""
    <div class="bug">
      <div class="bug-header">
        <a class="bug-id" href="#{resultado['cpId']}">{html.escape(resultado['cpId'])}</a>
        <span class="bug-sev">{html.escape(sprint)}</span>
        <span class="bug-func">{html.escape(funcionalidad)}</span>
      </div>
      <h4>{html.escape(nombre)}</h4>
      <p><strong>Falló en:</strong> {html.escape(primera_linea(resultado['mensaje']) or 'Sin mensaje de error')}</p>
      <p class="bug-casos">{html.escape(ubicacion)}</p>
    </div>
    """


def render_caso(resultado, meta, escenario):
    cp_id = resultado["cpId"]
    estado = resultado["estado"]
    info = ESTADO_INFO[estado]
    etiquetas = escenario["etiquetas"] if escenario else []
    tipo = tipo_de_prueba(etiquetas)
    detalle = []

    chips = [polaridad(etiquetas, meta)]
    if tipo:
        chips.append(TIPOS_DE_PRUEBA[tipo])
    if "no-funcional" in etiquetas:
        chips.append("No funcional")
    elif "funcional" in etiquetas:
        chips.append("Funcional")
    detalle.append('<p class="tags">' + "".join(
        f'<span class="tag">{html.escape(c)}</span>' for c in chips if c) + "</p>")

    if meta and meta.get("descripcion"):
        detalle.append(f"<p><strong>Descripción:</strong> {html.escape(meta['descripcion'])}</p>")
    if escenario and escenario["pasos"]:
        detalle.append("<p><strong>Escenario (Gherkin):</strong></p>"
                       f'<pre class="gherkin">{html.escape(chr(10).join(escenario["pasos"]))}</pre>')
        detalle.append(f'<p class="fuente">{html.escape(escenario["archivo"])}:{escenario["linea"]}</p>')

    if estado == "OMITIDO":
        motivo = resultado["mensaje"] or ("Funcionalidad aún no implementada (@omitido)"
                                          if "omitido" in etiquetas else "Excluido de esta ejecución")
        detalle.append(f'<div class="motivo motivo-skip"><strong>Motivo por el que no se ejecuta:</strong> '
                       f'{html.escape(motivo)}</div>')
    elif estado == "FALLIDO":
        detalle.append(f'<div class="motivo motivo-fail"><strong>Falló en:</strong> '
                       f'{html.escape(primera_linea(resultado["mensaje"]))}</div>')
        if resultado.get("salida"):
            detalle.append("<p><strong>Salida de la ejecución:</strong></p>"
                           f'<pre class="http-log">{html.escape(resultado["salida"][:8000])}</pre>')

    return f"""
    <details class="caso caso-{info['clase']}" id="{cp_id}">
      <summary>
        <span class="badge badge-{info['clase']}">{info['icono']}</span>
        <span class="caso-id">{html.escape(cp_id)}</span>
        <span class="caso-nombre">{html.escape(nombre_de(resultado, meta))}</span>
        <span class="caso-tipo">{html.escape(TIPOS_DE_PRUEBA.get(tipo, '') if tipo else '')}</span>
        <span class="caso-tiempo">{resultado['tiempo']} s</span>
      </summary>
      <div class="caso-detalle">
        {''.join(detalle)}
      </div>
    </details>
    """


CSS = """
:root {
  --ok: #2e7d32; --ok-bg: #e8f5e9;
  --fail: #c62828; --fail-bg: #fdecea;
  --skip: #b8860b; --skip-bg: #fff8e1;
  --info: #1d4ed8;
  --navy: #1a1a2e; --accent: #c0122f;
  --ink: #22242a; --muted: #6b7280; --line: #e5e7eb;
}
* { box-sizing: border-box; }
body {
  margin: 0; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Helvetica, Arial, sans-serif;
  color: var(--ink); background: #f4f5f7;
}
a { color: var(--info); }
header.top {
  background: linear-gradient(120deg, var(--navy), #0d0d17 60%, var(--accent) 180%);
  color: #fff; padding: 36px 32px 28px;
}
header.top h1 { margin: 0 0 4px; font-size: 26px; letter-spacing: .3px; }
header.top .sub { margin: 0; opacity: .85; font-size: 15px; }
header.top .meta { margin-top: 10px; font-size: 13px; opacity: .7; }
main { max-width: 1080px; margin: -20px auto 60px; padding: 0 20px; }
nav.indice { display: flex; flex-wrap: wrap; gap: 8px; margin: 0 0 24px; }
nav.indice a {
  background: #fff; border-radius: 999px; padding: 6px 14px; font-size: 13px; text-decoration: none;
  color: var(--navy); box-shadow: 0 1px 4px rgba(0,0,0,.06);
}
.cards { display: grid; grid-template-columns: repeat(5, 1fr); gap: 14px; margin-bottom: 18px; }
.card {
  background: #fff; border-radius: 12px; padding: 18px 20px; box-shadow: 0 2px 10px rgba(0,0,0,.08);
  border-top: 4px solid var(--line);
}
.card .num { font-size: 32px; font-weight: 700; line-height: 1; font-variant-numeric: tabular-nums; }
.card .label { margin-top: 6px; font-size: 13px; color: var(--muted); text-transform: uppercase; letter-spacing: .4px; }
.card.total { border-top-color: var(--navy); }
.card.ok { border-top-color: var(--ok); } .card.ok .num { color: var(--ok); }
.card.fail { border-top-color: var(--fail); } .card.fail .num { color: var(--fail); }
.card.skip { border-top-color: var(--skip); } .card.skip .num { color: var(--skip); }
.card.rate { border-top-color: var(--info); } .card.rate .num { color: var(--info); }

.resumenes { display: grid; grid-template-columns: repeat(2, 1fr); gap: 14px; margin-bottom: 32px; }
.panel { background: #fff; border-radius: 10px; padding: 14px 18px; box-shadow: 0 1px 6px rgba(0,0,0,.06); }
.panel h3 { margin: 0 0 8px; font-size: 14px; }
table { width: 100%; border-collapse: collapse; font-size: 13px; }
table th { text-align: left; color: var(--muted); font-weight: 600; padding: 6px 8px; border-bottom: 1px solid var(--line); }
table td { padding: 6px 8px; border-bottom: 1px solid var(--line); vertical-align: top; }
table.resumen th:not(:first-child), td.num { text-align: right; font-variant-numeric: tabular-nums; }
td.ok { color: var(--ok); } td.fail { color: var(--fail); } td.skip { color: var(--skip); }

section { margin-bottom: 32px; }
section h2 { font-size: 18px; margin: 0 0 12px; }
.intro { font-size: 14px; color: var(--muted); margin: -4px 0 12px; }
.bug {
  background: #fff; border-radius: 10px; padding: 16px 18px; margin-bottom: 10px;
  border-left: 5px solid var(--fail); box-shadow: 0 1px 6px rgba(0,0,0,.06);
}
.bug-header { display: flex; align-items: center; gap: 10px; margin-bottom: 4px; flex-wrap: wrap; }
.bug-id { font-weight: 700; color: var(--fail); text-decoration: none; }
.bug-sev { font-size: 11px; padding: 2px 8px; border-radius: 999px; font-weight: 600; background: var(--fail-bg); color: var(--fail); }
.bug-func { font-size: 12px; color: var(--muted); }
.bug h4 { margin: 4px 0; font-size: 15px; }
.bug p { margin: 4px 0; font-size: 14px; color: #333; }
.sin-fallos { background: #fff; border-radius: 10px; padding: 16px 18px; color: var(--muted); }
.bug-casos { font-size: 12px; color: var(--muted); font-family: ui-monospace, Menlo, monospace; }

.grupo { margin-bottom: 14px; }
.grupo > summary {
  cursor: pointer; font-size: 15px; font-weight: 600; padding: 10px 14px;
  background: #fff; border-radius: 8px; box-shadow: 0 1px 6px rgba(0,0,0,.06); list-style: none;
  display: flex; align-items: center; justify-content: space-between; gap: 12px;
}
.grupo > summary::-webkit-details-marker { display: none; }
.grupo-conteo { font-size: 12px; color: var(--muted); font-weight: 400; margin-left: 6px; }
.grupo-cuerpo { padding: 10px 4px 0; }
.sprint-titulo { font-size: 16px; margin: 22px 0 10px; color: var(--navy); }

details.caso {
  background: #fff; border-radius: 8px; margin: 8px 0; box-shadow: 0 1px 4px rgba(0,0,0,.05);
  border-left: 4px solid var(--line);
}
details.caso-ok { border-left-color: var(--ok); }
details.caso-fail { border-left-color: var(--fail); }
details.caso-skip { border-left-color: var(--skip); }
details.caso > summary {
  cursor: pointer; list-style: none; padding: 10px 14px; display: flex; align-items: center; gap: 10px;
}
details.caso > summary::-webkit-details-marker { display: none; }
.badge {
  display: inline-flex; align-items: center; justify-content: center; width: 22px; height: 22px;
  border-radius: 50%; font-size: 13px; font-weight: 700; flex-shrink: 0;
}
.badge-ok { background: var(--ok-bg); color: var(--ok); }
.badge-fail { background: var(--fail-bg); color: var(--fail); }
.badge-skip { background: var(--skip-bg); color: var(--skip); }
.caso-id { font-weight: 700; font-size: 13px; color: var(--navy); min-width: 88px; }
.caso-nombre { flex: 1; font-size: 14px; }
.caso-tipo { font-size: 11px; color: var(--muted); border: 1px solid var(--line); border-radius: 999px; padding: 1px 8px; }
.caso-tipo:empty { display: none; }
.caso-tiempo { font-size: 11px; color: var(--muted); min-width: 50px; text-align: right; }
.caso-detalle { padding: 4px 18px 16px 46px; font-size: 14px; }
.caso-detalle p { margin: 8px 0 4px; }
.tags { display: flex; gap: 6px; flex-wrap: wrap; }
.tag { font-size: 11px; color: var(--navy); background: #eef0f6; border-radius: 999px; padding: 2px 8px; }
.fuente { font-size: 12px; color: var(--muted); font-family: ui-monospace, Menlo, monospace; }
.motivo { padding: 8px 12px; border-radius: 6px; margin: 8px 0; font-size: 13px; }
.motivo-skip { background: var(--skip-bg); color: #7a5c00; }
.motivo-fail { background: var(--fail-bg); color: var(--fail); }
pre.gherkin {
  background: #f7f8fa; border: 1px solid var(--line); padding: 10px 12px; border-radius: 8px;
  font-size: 12px; line-height: 1.55; overflow-x: auto; margin: 4px 0;
}
pre.http-log {
  background: #14151f; color: #d4d4d4; padding: 12px 14px; border-radius: 8px; overflow-x: auto;
  font-size: 12px; line-height: 1.5; max-height: 420px;
}
footer { text-align: center; color: var(--muted); font-size: 12px; padding: 20px; }
@media (max-width: 760px) {
  main { padding: 0 16px; }
  .cards { grid-template-columns: repeat(2, 1fr); }
  .resumenes { grid-template-columns: 1fr; }
  .caso-tiempo, .caso-tipo { display: none; }
  .caso-detalle { padding-left: 18px; }
  }
"""


def main():
    target_dir = Path(sys.argv[1]) if len(sys.argv) > 1 else REPO_ROOT / "target"
    metadata = cargar_metadata()
    escenarios = parsear_features()
    resultados = parsear_resultados(target_dir)

    if not resultados:
        print("[reporte-qa] No se encontraron resultados en target/failsafe-reports; "
              "¿corriste './mvnw clean -P casos-qa verify'?")
        return

    resultados.sort(key=lambda r: r["cpId"])
    total = conteo(resultados)

    # Resúmenes por sprint, tipo de prueba y polaridad
    por_sprint = OrderedDict()
    por_tipo = OrderedDict((clave, []) for clave in TIPOS_DE_PRUEBA)
    for r in resultados:
        escenario = escenarios.get(r["cpId"])
        meta = metadata.get(r["cpId"])
        por_sprint.setdefault(sprint_de(r["cpId"], escenario, meta), []).append(r)
        tipo = tipo_de_prueba(escenario["etiquetas"]) if escenario else None
        if tipo:
            por_tipo[tipo].append(r)
    grupos_sprint = [(f"Sprint {s}", conteo(items)) for s, items in sorted(por_sprint.items())]
    grupos_sprint.append(("Total", total))
    grupos_tipo = [(TIPOS_DE_PRUEBA[t], conteo(items)) for t, items in por_tipo.items() if items]

    fallos = [r for r in resultados if r["estado"] == "FALLIDO"]
    bugs_html = "".join(render_fallo(r, metadata.get(r["cpId"]), escenarios.get(r["cpId"])) for r in fallos)
    if not bugs_html:
        bugs_html = '<p class="sin-fallos">No se registraron fallos en esta ejecución.</p>'

    # Detalle por sprint y por funcionalidad
    detalle_html = []
    for sprint, items_sprint in sorted(por_sprint.items()):
        por_funcionalidad = OrderedDict()
        for r in items_sprint:
            funcionalidad = funcionalidad_de(escenarios.get(r["cpId"]), metadata.get(r["cpId"]))
            por_funcionalidad.setdefault(funcionalidad, []).append(r)
        grupos_html = []
        for funcionalidad, items in por_funcionalidad.items():
            c = conteo(items)
            casos_html = "".join(render_caso(r, metadata.get(r["cpId"]), escenarios.get(r["cpId"])) for r in items)
            abierto = " open" if c["fail"] else ""
            grupos_html.append(f"""
        <details class="grupo"{abierto}>
          <summary>{html.escape(funcionalidad)}
            <span class="grupo-conteo">{c['ok']} aprobado(s) · {c['fail']} fallido(s) · {c['skip']} omitido(s)</span>
          </summary>
          <div class="grupo-cuerpo">{casos_html}</div>
        </details>""")
        cs = conteo(items_sprint)
        detalle_html.append(
            f'<h3 class="sprint-titulo" id="sprint-{sprint}">Sprint {sprint} '
            f'<span class="grupo-conteo">{cs["total"]} casos · {porcentaje(cs)} de aprobación</span></h3>'
            + "".join(grupos_html))

    fecha = datetime.now().strftime("%Y-%m-%d %H:%M")
    sprints_txt = " y ".join(str(s) for s in sorted(por_sprint))
    nav_sprints = "".join(f'<a href="#sprint-{s}">Detalle Sprint {s}</a>' for s in sorted(por_sprint))

    pagina = f"""<!DOCTYPE html>
<html lang="es">
<head>
<meta charset="UTF-8" />
<title>Casos de prueba ZARA</title>
<meta name="viewport" content="width=device-width, initial-scale=1" />
<style>{CSS}</style>
</head>
<body>
<header class="top">
  <h1>Equipo de Calidad: Casos de prueba: Caso ZARA Code Factory</h1>
  <p class="sub">Equipo EAP01FE — Sistema de Optimización de Cadena de Suministro · Sprints {sprints_txt}</p>
  <p class="meta">Generado automáticamente el {fecha} · fuente: escenarios Gherkin de qa/features ejecutados con Cucumber sobre la API real</p>
</header>
<main>
  <div class="cards">
    <div class="card total"><div class="num">{total['total']}</div><div class="label">Total casos</div></div>
    <div class="card ok"><div class="num">{total['ok']}</div><div class="label">Aprobados</div></div>
    <div class="card fail"><div class="num">{total['fail']}</div><div class="label">Fallidos</div></div>
    <div class="card skip"><div class="num">{total['skip']}</div><div class="label">Omitidos</div></div>
    <div class="card rate"><div class="num">{porcentaje(total)}</div><div class="label">Aprobación</div></div>
  </div>

  <nav class="indice">
    <a href="#defectos">Defectos</a>{nav_sprints}
  </nav>

  <div class="resumenes">
    {tabla_resumen("Resultados por sprint", grupos_sprint)}
    {tabla_resumen("Resultados por tipo de prueba", grupos_tipo)}
  </div>

  <section class="bugs" id="defectos">
    <h2>Defectos encontrados ({len(fallos)})</h2>
    {bugs_html}
  </section>

  <section class="detalle">
    <h2>Detalle por funcionalidad</h2>
    <p class="intro">Clic en un caso para ver su escenario Gherkin y el resultado de la ejecución.</p>
    {''.join(detalle_html)}
  </section>
</main>
<footer>Reporte generado por tools/qa/generar_reporte_casos_prueba.py</footer>
</body>
</html>
"""

    out_dir = target_dir / "reports"
    out_dir.mkdir(parents=True, exist_ok=True)
    out_file = out_dir / "casos-prueba.html"
    out_file.write_text(pagina, encoding="utf-8")
    imprimir_resumen_consola(total["total"], total["ok"], total["fail"], total["skip"], resultados, out_file)


if __name__ == "__main__":
    main()
