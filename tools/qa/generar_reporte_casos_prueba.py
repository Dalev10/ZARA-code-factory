#!/usr/bin/env python3
"""
Genera un reporte HTML único, legible y navegable de los 56 casos de prueba
de Sprint 1 (Casos_de_Prueba_-_Sprint__1_EAP01FE.docx), a partir de:

  - tools/qa/casos_metadata.json   -> datos estáticos de cada caso (nombre,
    tipo, criterios de aceptación/rechazo, prerrequisitos, pasos, resultado
    esperado), extraídos una sola vez del documento original.
  - target/failsafe-reports/TEST-*.xml -> resultado real de la última
    ejecución de src/test/java/.../qa/*IT.java (JUnit 5 + REST Assured),
    generado por maven-failsafe-plugin.

No depende de librerías externas (solo la librería estándar de Python), para
no agregar otra herramienta más al proyecto. Se invoca automáticamente al
final de:

    ./mvnw verify -q -Pcasos-qa-sprint1

Uso manual: python3 tools/qa/generar_reporte_casos_prueba.py <target-dir>
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

CP_ID_RE = re.compile(r"cpHu(\d{2})(\d{3})")

DEFECTOS = [
    {
        "id": "D-01",
        "severidad": "Alta",
        "casos": ["CP-HU06-002", "CP-HU06-003"],
        "titulo": "Eliminar Tienda no valida si tiene una Bodega_Tienda asociada",
        "descripcion": (
            "TiendaService.desactivar() desactiva la Tienda sin comprobar si tiene una "
            "Bodega_Tienda asociada. Incumple AC-2 y AC-4 de HU-06."
        ),
        "nota": "Relacionado con PD-07 (pendiente de definición formal en el documento original).",
    },
    {
        "id": "D-02",
        "severidad": "Alta",
        "casos": ["CP-HU14-002"],
        "titulo": "Eliminar Centro de Distribución no valida dependencias",
        "descripcion": (
            "CentroDistribucionService.eliminarCentroDistribucion() no implementa ningún guard "
            "de dependencias: siempre elimina de forma incondicional. Incumple AC-2 y AC-3 de HU-14. "
            "Hallazgo de revisión de código (no reproducible en runtime en Sprint 1 por falta de un "
            "escenario real de dependencias)."
        ),
        "nota": "",
    },
    {
        "id": "D-03",
        "severidad": "Media",
        "casos": ["CP-HU10-003"],
        "titulo": "Eliminar Bodega_Tienda es un borrado físico (sin historial)",
        "descripcion": (
            "Tras eliminar una Bodega_Tienda, la consulta devuelve 404 de inmediato: no se conserva "
            "ningún rastro histórico. Incumple AC-5 de HU-10."
        ),
        "nota": "Relacionado con PD-08 (pendiente de definición formal en el documento original).",
    },
    {
        "id": "D-04",
        "severidad": "Media",
        "casos": ["CP-HU14-003"],
        "titulo": "Eliminar Centro de Distribución es un borrado físico (sin historial)",
        "descripcion": "Mismo patrón que D-03, aplicado a Centro de Distribución. Incumple AC-5 de HU-14.",
        "nota": "",
    },
]
DEFECTO_POR_CASO = {cp: d for d in DEFECTOS for cp in d["casos"]}


def cargar_metadata():
    datos = json.loads(METADATA_PATH.read_text(encoding="utf-8"))
    return {c["id"]: c for c in datos}


def limpiar_log(texto):
    if not texto:
        return None
    lineas = [l for l in texto.split("\n") if not l.startswith("###CASO-")]
    # Recorta el ruido de arranque de Spring (solo aparece en el primer caso de
    # cada clase, no aporta nada al detalle de ESE caso puntual).
    lineas = [l for l in lineas if not re.match(r"^\d{4}-\d{2}-\d{2}T.*(INFO|WARN)\s", l)]
    texto_limpio = "\n".join(lineas).strip("\n")
    return texto_limpio or None


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
            nombre_metodo = tc.get("name", "")
            m = CP_ID_RE.search(nombre_metodo)
            cp_id = f"CP-HU{m.group(1)}-{m.group(2)}" if m else nombre_metodo
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
            log = limpiar_log(system_out.text if system_out is not None else None)
            casos.append({
                "cpId": cp_id,
                "metodo": nombre_metodo,
                "clase": root.get("name", ""),
                "tiempo": tc.get("time", "0"),
                "estado": estado,
                "mensaje": mensaje,
                "log": log,
            })
    return casos


def hu_de(cp_id):
    m = re.match(r"CP-HU(\d{2})-", cp_id)
    return m.group(1) if m else "00"


def render_lista(items):
    if not items:
        return ""
    return "<ul>" + "".join(f"<li>{html.escape(i)}</li>" for i in items) + "</ul>"


ESTADO_INFO = {
    "APROBADO": {"clase": "ok", "icono": "&#10003;"},
    "FALLIDO": {"clase": "fail", "icono": "&#10007;"},
    "OMITIDO": {"clase": "skip", "icono": "&#9888;"},
}


def render_caso(resultado, meta):
    cp_id = resultado["cpId"]
    estado = resultado["estado"]
    info = ESTADO_INFO[estado]
    nombre = meta["nombre"] if meta else resultado["metodo"]
    tipo = meta["tipo"] if meta else ""
    defecto = DEFECTO_POR_CASO.get(cp_id)

    detalle_partes = []
    if meta:
        if meta.get("descripcion"):
            detalle_partes.append(f"<p><strong>Descripción:</strong> {html.escape(meta['descripcion'])}</p>")
        if meta.get("criteriosAceptacion"):
            detalle_partes.append(
                "<p><strong>Criterios de aceptación:</strong></p>" + render_lista(meta["criteriosAceptacion"])
            )
        if meta.get("criteriosRechazo"):
            detalle_partes.append(
                "<p><strong>Criterios de rechazo:</strong></p>" + render_lista(meta["criteriosRechazo"])
            )
        if meta.get("prerrequisitos"):
            detalle_partes.append("<p><strong>Prerrequisitos:</strong></p>" + render_lista(meta["prerrequisitos"]))
        if meta.get("pasos"):
            detalle_partes.append("<p><strong>Pasos:</strong></p>" + render_lista(meta["pasos"]))
        if meta.get("resultadoEsperado"):
            detalle_partes.append(
                f"<p><strong>Resultado esperado:</strong> {html.escape(meta['resultadoEsperado'])}</p>"
            )

    if estado == "OMITIDO":
        detalle_partes.append(f'<div class="motivo motivo-skip"><strong>Motivo por el que no se ejecuta:</strong> {html.escape(resultado["mensaje"])}</div>')
    elif estado == "FALLIDO":
        detalle_partes.append(f'<div class="motivo motivo-fail"><strong>Falló en:</strong> {html.escape(resultado["mensaje"])}</div>')
        if defecto:
            detalle_partes.append(
                f'<div class="motivo motivo-fail"><strong>Defecto {defecto["id"]} (severidad {defecto["severidad"]}):</strong> '
                f'{html.escape(defecto["titulo"])}</div>'
            )

    if resultado.get("log"):
        detalle_partes.append(
            "<p><strong>Detalle de lo ejecutado (petición/respuesta HTTP real):</strong></p>"
            f'<pre class="http-log">{html.escape(resultado["log"])}</pre>'
        )

    return f"""
    <details class="caso caso-{info['clase']}">
      <summary>
        <span class="badge badge-{info['clase']}">{info['icono']}</span>
        <span class="caso-id">{html.escape(cp_id)}</span>
        <span class="caso-nombre">{html.escape(nombre)}</span>
        <span class="caso-tipo">{html.escape(tipo)}</span>
        <span class="caso-tiempo">{resultado['tiempo']} s</span>
      </summary>
      <div class="caso-detalle">
        {''.join(detalle_partes)}
      </div>
    </details>
    """


def render_bug(defecto):
    casos_html = ", ".join(defecto["casos"])
    nota_html = f'<p class="bug-nota">{html.escape(defecto["nota"])}</p>' if defecto["nota"] else ""
    sev_clase = "alta" if defecto["severidad"] == "Alta" else "media"
    return f"""
    <div class="bug bug-{sev_clase}">
      <div class="bug-header">
        <span class="bug-id">{defecto['id']}</span>
        <span class="bug-sev bug-sev-{sev_clase}">Severidad {defecto['severidad']}</span>
      </div>
      <h4>{html.escape(defecto['titulo'])}</h4>
      <p>{html.escape(defecto['descripcion'])}</p>
      {nota_html}
      <p class="bug-casos"><strong>Casos:</strong> {html.escape(casos_html)}</p>
    </div>
    """


HU_TITULOS = {
    "01": "HU-01 · Registrar Usuario", "02": "HU-02 · Iniciar Sesión",
    "03": "HU-03 · Registrar Tienda/Almacén", "04": "HU-04 · Consultar Tienda/Almacén",
    "05": "HU-05 · Modificar Tienda/Almacén", "06": "HU-06 · Eliminar Tienda/Almacén",
    "07": "HU-07 · Registrar Bodega_Tienda", "08": "HU-08 · Consultar Bodega_Tienda",
    "09": "HU-09 · Modificar Bodega_Tienda", "10": "HU-10 · Eliminar Bodega_Tienda",
    "11": "HU-11 · Registrar Centro de Distribución", "12": "HU-12 · Consultar Centro de Distribución",
    "13": "HU-13 · Modificar Centro de Distribución", "14": "HU-14 · Eliminar Centro de Distribución",
    "15": "HU-15 · Registrar Producto", "16": "HU-16 · Consultar Producto",
    "17": "HU-17 · Modificar Producto", "18": "HU-18 · Eliminar Producto",
}

CSS = """
:root {
  --ok: #2e7d32; --ok-bg: #e8f5e9;
  --fail: #c62828; --fail-bg: #fdecea;
  --skip: #b8860b; --skip-bg: #fff8e1;
  --navy: #1a1a2e; --accent: #c0122f;
  --ink: #22242a; --muted: #6b7280; --line: #e5e7eb;
}
* { box-sizing: border-box; }
body {
  margin: 0; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Helvetica, Arial, sans-serif;
  color: var(--ink); background: #f4f5f7;
}
header.top {
  background: linear-gradient(120deg, var(--navy), #0d0d17 60%, var(--accent) 180%);
  color: #fff; padding: 36px 32px 28px;
}
header.top h1 { margin: 0 0 4px; font-size: 26px; letter-spacing: .3px; }
header.top .sub { margin: 0; opacity: .85; font-size: 15px; }
header.top .meta { margin-top: 10px; font-size: 13px; opacity: .7; }
main { max-width: 1080px; margin: -20px auto 60px; padding: 0 20px; }
.cards { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; margin-bottom: 28px; }
.card {
  background: #fff; border-radius: 12px; padding: 18px 20px; box-shadow: 0 2px 10px rgba(0,0,0,.08);
  border-top: 4px solid var(--line);
}
.card .num { font-size: 32px; font-weight: 700; line-height: 1; }
.card .label { margin-top: 6px; font-size: 13px; color: var(--muted); text-transform: uppercase; letter-spacing: .4px; }
.card.total { border-top-color: var(--navy); }
.card.ok { border-top-color: var(--ok); } .card.ok .num { color: var(--ok); }
.card.fail { border-top-color: var(--fail); } .card.fail .num { color: var(--fail); }
.card.skip { border-top-color: var(--skip); } .card.skip .num { color: var(--skip); }

section.bugs { margin-bottom: 32px; }
section.bugs h2, section.hu h2 { font-size: 18px; margin: 0 0 12px; }
.bug {
  background: #fff; border-radius: 10px; padding: 16px 18px; margin-bottom: 10px;
  border-left: 5px solid var(--fail); box-shadow: 0 1px 6px rgba(0,0,0,.06);
}
.bug-media { border-left-color: var(--skip); }
.bug-header { display: flex; align-items: center; gap: 10px; margin-bottom: 4px; }
.bug-id { font-weight: 700; color: var(--fail); }
.bug-sev { font-size: 11px; padding: 2px 8px; border-radius: 999px; font-weight: 600; }
.bug-sev-alta { background: var(--fail-bg); color: var(--fail); }
.bug-sev-media { background: var(--skip-bg); color: var(--skip); }
.bug h4 { margin: 4px 0; font-size: 15px; }
.bug p { margin: 4px 0; font-size: 14px; color: #333; }
.bug-nota { font-style: italic; color: var(--muted); font-size: 13px; }
.bug-casos { font-size: 13px; color: var(--muted); }

.hu-group { margin-bottom: 22px; }
.hu-group > summary {
  cursor: pointer; font-size: 16px; font-weight: 600; padding: 10px 14px;
  background: #fff; border-radius: 8px; box-shadow: 0 1px 6px rgba(0,0,0,.06); list-style: none;
  display: flex; align-items: center; justify-content: space-between;
}
.hu-group > summary::-webkit-details-marker { display: none; }
.hu-group > summary .hu-counts { font-size: 12px; color: var(--muted); font-weight: 400; }
.hu-body { padding: 10px 4px 0; }

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
.caso-tiempo { font-size: 11px; color: var(--muted); min-width: 50px; text-align: right; }
.caso-detalle { padding: 4px 18px 16px 46px; font-size: 14px; }
.caso-detalle p { margin: 8px 0 4px; }
.caso-detalle ul { margin: 2px 0 8px 18px; padding: 0; }
.caso-detalle li { margin: 2px 0; }
.motivo { padding: 8px 12px; border-radius: 6px; margin: 8px 0; font-size: 13px; }
.motivo-skip { background: var(--skip-bg); color: #7a5c00; }
.motivo-fail { background: var(--fail-bg); color: var(--fail); }
pre.http-log {
  background: #14151f; color: #d4d4d4; padding: 12px 14px; border-radius: 8px; overflow-x: auto;
  font-size: 12px; line-height: 1.5; max-height: 420px;
}
footer { text-align: center; color: var(--muted); font-size: 12px; padding: 20px; }
"""


def main():
    target_dir = Path(sys.argv[1]) if len(sys.argv) > 1 else REPO_ROOT / "target"
    metadata = cargar_metadata()
    resultados = parsear_resultados(target_dir)

    if not resultados:
        print("[reporte-qa] No se encontraron resultados en target/failsafe-reports; "
              "¿corriste './mvnw verify -Pcasos-qa-sprint1'?")
        return

    resultados.sort(key=lambda r: r["cpId"])
    total = len(resultados)
    aprobados = sum(1 for r in resultados if r["estado"] == "APROBADO")
    fallidos = sum(1 for r in resultados if r["estado"] == "FALLIDO")
    omitidos = sum(1 for r in resultados if r["estado"] == "OMITIDO")

    por_hu = OrderedDict()
    for r in resultados:
        por_hu.setdefault(hu_de(r["cpId"]), []).append(r)

    bugs_html = "".join(render_bug(d) for d in DEFECTOS)

    grupos_html = []
    for hu, items in por_hu.items():
        casos_html = "".join(render_caso(r, metadata.get(r["cpId"])) for r in items)
        n_ok = sum(1 for r in items if r["estado"] == "APROBADO")
        n_fail = sum(1 for r in items if r["estado"] == "FALLIDO")
        n_skip = sum(1 for r in items if r["estado"] == "OMITIDO")
        counts = f"{n_ok} aprobado(s) · {n_fail} fallido(s) · {n_skip} omitido(s)"
        titulo = HU_TITULOS.get(hu, f"HU-{hu}")
        grupos_html.append(f"""
        <details class="hu-group" open>
          <summary>{html.escape(titulo)} <span class="hu-counts">{counts}</span></summary>
          <div class="hu-body">{casos_html}</div>
        </details>
        """)

    fecha = datetime.now().strftime("%Y-%m-%d %H:%M")

    pagina = f"""<!DOCTYPE html>
<html lang="es">
<head>
<meta charset="UTF-8" />
<title>Equipo de Calidad: Casos de prueba: Caso ZARA Code Factory</title>
<meta name="viewport" content="width=device-width, initial-scale=1" />
<style>{CSS}</style>
</head>
<body>
<header class="top">
  <h1>Equipo de Calidad: Casos de prueba: Caso ZARA Code Factory</h1>
  <p class="sub">Equipo EAP01FE — Sistema de Optimización de Cadena de Suministro</p>
  <p class="meta">Generado automáticamente el {fecha} · fuente: Casos_de_Prueba_-_Sprint__1_EAP01FE.docx</p>
</header>
<main>
  <div class="cards">
    <div class="card total"><div class="num">{total}</div><div class="label">Total casos</div></div>
    <div class="card ok"><div class="num">{aprobados}</div><div class="label">Aprobados</div></div>
    <div class="card fail"><div class="num">{fallidos}</div><div class="label">Fallidos</div></div>
    <div class="card skip"><div class="num">{omitidos}</div><div class="label">Omitidos</div></div>
  </div>

  <section class="bugs">
    <h2>Defectos encontrados ({len(DEFECTOS)})</h2>
    {bugs_html}
  </section>

  <section class="hu">
    <h2>Detalle por historia de usuario (clic en un caso para ver la ejecución)</h2>
    {''.join(grupos_html)}
  </section>
</main>
<footer>Reporte generado por tools/qa/generar_reporte_casos_prueba.py</footer>
</body>
</html>
"""

    out_dir = target_dir / "reports"
    out_dir.mkdir(parents=True, exist_ok=True)
    out_file = out_dir / "casos-prueba-sprint1.html"
    out_file.write_text(pagina, encoding="utf-8")
    print(f"[reporte-qa] Reporte escrito en {out_file} "
          f"({total} casos: {aprobados} aprobados, {fallidos} fallidos, {omitidos} omitidos)")


if __name__ == "__main__":
    main()
