#!/usr/bin/env python3
"""
Siembra por la API los datos de demostración del ciclo de abastecimiento (HD-06).

Crea, con nombres únicos por corrida (sufijo de fecha y hora):
  * catálogo: una categoría, un template y dos variantes;
  * red: dos CD (uno casi sin stock, para ver el fallback de HU-45) y dos tiendas
    con su Almacén y su Bodega, con prioridad CD-sin-stock -> CD-principal;
  * inventario inicial en CDs, Almacenes y Bodegas;
  * umbral de reposición interna (3/6) y nivel de reposición al CD (5/20) por variante;
  * un usuario por rol operativo, todos con la contraseña de demo que se indique.

Todo pasa por los endpoints reales, con las mismas reglas que un usuario. No usa la base
de datos directamente ni crea nada que la API no permita. Solo la biblioteca estándar.

Uso:
    python3 tools/demo/sembrar_demo.py --api http://localhost:8080 --email admin@...
Las contraseñas (la del admin y la de los usuarios de demo) se piden por consola y no
quedan en el historial. Si el admin tiene MFA, se pide el código. También se aceptan
las variables TOKEN (un access token ya emitido) y DEMO_PASSWORD.

Al final imprime los ids y los emails creados. Ninguna contraseña se imprime ni se
guarda: quien siembra comparte la contraseña de demo con el equipo por otro medio.
"""
from __future__ import annotations

import argparse
import getpass
import json
import os
import sys
import urllib.error
import urllib.request
from datetime import datetime

ROLES_DEMO = [
    # (rol, ¿opera en las tiendas de demo?)
    ("VENDEDOR", True),
    ("SUPERVISOR_TIENDA", True),
    ("PLANIFICADOR_ABASTECIMIENTO", True),
    ("OPERADOR_CD", False),
    ("COORDINADOR_TRANSPORTE", False),
    ("ENCARGADO_BODEGA", True),
]


class Api:
    def __init__(self, base: str, token: str | None = None):
        self.base = base.rstrip("/")
        self.token = token

    def llamar(self, metodo: str, ruta: str, cuerpo: dict | None = None):
        datos = None if cuerpo is None else json.dumps(cuerpo).encode("utf-8")
        peticion = urllib.request.Request(self.base + ruta, data=datos, method=metodo)
        peticion.add_header("Content-Type", "application/json")
        peticion.add_header("Accept", "application/json")
        if self.token:
            peticion.add_header("Authorization", "Bearer " + self.token)
        try:
            with urllib.request.urlopen(peticion, timeout=30) as respuesta:
                texto = respuesta.read().decode("utf-8")
                return json.loads(texto) if texto else None
        except urllib.error.HTTPError as error:
            detalle = error.read().decode("utf-8", "replace")
            sys.exit(f"ERROR {error.code} en {metodo} {ruta}: {detalle}")
        except urllib.error.URLError as error:
            sys.exit(f"No se pudo conectar con {self.base}: {error.reason}")

    def post(self, ruta, cuerpo=None):
        return self.llamar("POST", ruta, cuerpo)

    def put(self, ruta, cuerpo):
        return self.llamar("PUT", ruta, cuerpo)

    def get(self, ruta):
        return self.llamar("GET", ruta)


def iniciar_sesion(api: Api, email: str) -> str:
    password = getpass.getpass(f"Contraseña de {email}: ")
    respuesta = api.post("/api/v1/auth/login", {"email": email, "password": password})
    if respuesta.get("mfaRequerido"):
        codigo = input("Código MFA (TOTP o de respaldo): ").strip()
        respuesta = api.post("/api/v1/auth/login/mfa",
                             {"mfaChallengeToken": respuesta["mfaChallengeToken"], "codigo": codigo})
    return respuesta["accessToken"]


def contrasena_demo() -> str:
    """La pide dos veces por consola, o la toma de DEMO_PASSWORD. La API exige 12+ caracteres y no filtrada."""
    password = os.environ.get("DEMO_PASSWORD")
    if not password:
        password = getpass.getpass("Contraseña para los usuarios de demo (12+ caracteres): ")
        if getpass.getpass("Repítela: ") != password:
            sys.exit("Las contraseñas no coinciden")
    if len(password) < 12:
        sys.exit("La contraseña de demo debe tener al menos 12 caracteres")
    return password


def sembrar(api: Api, sufijo: str, password: str) -> dict:
    s = {"sufijo": sufijo}

    categoria = api.post("/api/v1/categorias", {"nombre": f"Camisas demo {sufijo}"})
    template = api.post("/api/v1/templates", {
        "nombre": f"Camisa Oxford demo {sufijo}", "temporada": "2026-2", "proveedor": "Textiles Andinos",
        "precioBase": 89900, "categoriaId": categoria["id"]})
    s["variantes"] = []
    for color, talla in (("AZ", "M"), ("BL", "L")):
        sku = f"DEMO-{sufijo}-{color}-{talla}"
        variante = api.post("/api/v1/variantes",
                            {"sku": sku, "talla": talla, "color": color, "templateId": template["id"]})
        s["variantes"].append({"id": variante["id"], "sku": sku})

    cd_principal = api.post("/api/v1/centros-distribucion",
                            {"nombre": f"CD Demo Principal {sufijo}", "ubicacion": "Bogotá"})
    cd_respaldo = api.post("/api/v1/centros-distribucion",
                           {"nombre": f"CD Demo Sin Stock {sufijo}", "ubicacion": "Cali"})
    # Crear el CD no devuelve su nodo; la consulta por id sí.
    s["cds"] = {clave: {"id": cd["id"], "nodoId": api.get(f"/api/v1/centros-distribucion/{cd['id']}")["nodo"]["id"]}
                for clave, cd in (("principal", cd_principal), ("sinStock", cd_respaldo))}

    s["tiendas"] = []
    for nombre, ubicacion in (("Centro", "Medellín"), ("Norte", "Barranquilla")):
        tienda = api.post("/api/v1/tiendas", {"nombre": f"Tienda Demo {nombre} {sufijo}", "ubicacion": ubicacion})
        # Primero el CD sin stock: la orden muestra el intento descartado y cae al principal (HU-45).
        api.put(f"/api/v1/tiendas/{tienda['id']}/prioridad-cd", {"cdIds": [cd_respaldo["id"], cd_principal["id"]]})
        s["tiendas"].append({"id": tienda["id"], "nombre": tienda["nombre"],
                             "almacenId": tienda["almacenId"], "bodegaId": tienda["bodegaId"]})

    def cargar(nodo_id: str, cantidad: int):
        api.post(f"/api/v1/nodos/{nodo_id}/inventario/carga-inicial",
                 {"items": [{"sku": v["sku"], "cantidad": cantidad} for v in s["variantes"]]})

    cargar(s["cds"]["principal"]["nodoId"], 200)
    cargar(s["cds"]["sinStock"]["nodoId"], 3)
    for tienda in s["tiendas"]:
        cargar(tienda["almacenId"], 2)   # por debajo del umbral 3: la primera venta dispara el traspaso
        cargar(tienda["bodegaId"], 6)    # tras el traspaso queda bajo el nivel 5: nace la recomendación

    # Los niveles van después de la carga: crearlos no evalúa nada (D-57).
    for v in s["variantes"]:
        api.post("/api/v1/umbrales-reposicion",
                 {"varianteId": v["id"], "cantidadMinima": 3, "cantidadObjetivo": 6})
        api.post("/api/v1/niveles-reposicion-cd",
                 {"varianteId": v["id"], "cantidadMinima": 5, "cantidadObjetivo": 20})

    roles = {rol["nombre"]: rol["id"] for rol in api.get("/api/v1/roles")}
    s["usuarios"] = []
    for nombre_rol, por_tienda in ROLES_DEMO:
        if nombre_rol not in roles:
            sys.exit(f"Falta el rol {nombre_rol}: ¿está aplicada la migración V34?")
        email = f"demo.{nombre_rol.lower().replace('_', '.')}.{sufijo}@supplychain.local"
        usuario = api.post("/api/v1/usuarios", {"email": email, "nombreCompleto": f"Demo {nombre_rol.title()}",
                                                "password": password})
        api.post(f"/api/v1/usuarios/{usuario['id']}/roles", {"rolId": roles[nombre_rol]})
        if por_tienda:
            for tienda in s["tiendas"]:
                api.post(f"/api/v1/usuarios/{usuario['id']}/tiendas", {"tiendaId": tienda["id"]})
        s["usuarios"].append({"rol": nombre_rol, "email": email})
    return s


def main():
    parser = argparse.ArgumentParser(description="Siembra los datos de demostración por la API (HD-06).")
    parser.add_argument("--api", default="http://localhost:8080", help="URL base de la API")
    parser.add_argument("--email", help="Email de un ADMIN (si no se define la variable TOKEN)")
    parser.add_argument("--salida", help="Archivo JSON donde guardar los ids y emails (por defecto, solo consola)")
    args = parser.parse_args()

    api = Api(args.api, os.environ.get("TOKEN"))
    if not api.token:
        if not args.email:
            parser.error("indica --email de un ADMIN o define TOKEN")
        api.token = iniciar_sesion(api, args.email)
    password = contrasena_demo()

    sembrado = sembrar(api, datetime.now().strftime("%m%d%H%M%S"), password)
    texto = json.dumps(sembrado, indent=2, ensure_ascii=False)
    if args.salida:
        with open(args.salida, "w", encoding="utf-8") as archivo:
            archivo.write(texto + "\n")
        print(f"Datos sembrados; ids y emails en {args.salida}")
    else:
        print(texto)


if __name__ == "__main__":
    main()
