#!/usr/bin/env python3
"""
Imprime un resumen corto y coloreado en consola al final de "mvn test" /
"mvn verify", leyendo los .txt que maven-surefire-plugin ya deja en
target/surefire-reports/ (no agrega ningún archivo nuevo, solo lee lo que
Surefire genera de por sí).

Uso: python3 tools/resumen_pruebas_unitarias.py <reports-dir>
"""
import re
import sys
from pathlib import Path

VERDE = "\033[92m"
ROJO = "\033[91m"
AMARILLO = "\033[93m"
NEGRITA = "\033[1m"
GRIS = "\033[90m"
FIN = "\033[0m"

PATRON = re.compile(
    r"Tests run: (\d+), Failures: (\d+), Errors: (\d+), Skipped: (\d+).*-- in (\S+)"
)


def main():
    reports_dir = Path(sys.argv[1]) if len(sys.argv) > 1 else Path("target/surefire-reports")
    archivos = sorted(reports_dir.glob("*.txt")) if reports_dir.is_dir() else []

    if not archivos:
        print(f"{AMARILLO}[resumen] No se encontraron reportes en {reports_dir}{FIN}")
        return

    total = fallas = errores = omitidas = 0
    clases_con_problemas = []

    for archivo in archivos:
        m = PATRON.search(archivo.read_text(encoding="utf-8", errors="ignore"))
        if not m:
            continue
        t, f, e, s, clase = int(m.group(1)), int(m.group(2)), int(m.group(3)), int(m.group(4)), m.group(5)
        total += t
        fallas += f
        errores += e
        omitidas += s
        if f > 0 or e > 0:
            clases_con_problemas.append((clase, f, e))

    pasaron = total - fallas - errores - omitidas
    hay_problemas = (fallas + errores) > 0

    ancho = 58
    linea = "=" * ancho
    print()
    print(f"{NEGRITA}{linea}{FIN}")
    print(f"{NEGRITA}  RESUMEN — Pruebas unitarias / integración{FIN}")
    print(f"{linea}")
    print(f"  Clases analizadas : {len(archivos)}")
    print(f"  Total de pruebas  : {total}")
    print(f"  {VERDE}Pasaron           : {pasaron}{FIN}")
    color_f = ROJO if (fallas + errores) > 0 else GRIS
    print(f"  {color_f}Fallaron          : {fallas + errores}{FIN}")
    color_s = AMARILLO if omitidas > 0 else GRIS
    print(f"  {color_s}Omitidas (skip)   : {omitidas}{FIN}")
    print(linea)

    if clases_con_problemas:
        print(f"  {ROJO}{NEGRITA}Clases con fallas/errores:{FIN}")
        for clase, f, e in clases_con_problemas:
            print(f"  {ROJO}  - {clase}  (fallas={f}, errores={e}){FIN}")
        print(linea)

    resultado = f"{ROJO}HAY FALLAS{FIN}" if hay_problemas else f"{VERDE}TODO OK{FIN}"
    print(f"  Resultado: {resultado}")
    print(f"{linea}\n")


if __name__ == "__main__":
    main()
