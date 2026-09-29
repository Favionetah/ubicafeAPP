#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Genera ubicafe.db a partir de CAFETERÍAS ENCUESTA PARA APP.xlsx.

Salidas:
- ubicafe.db        base SQLite con el esquema y los datos reales de la encuesta
- esquema.sql       script CREATE TABLE exacto usado
- datos_descartados.log  filas excluidas o marcadas con su motivo

Uso: python tools/generar_bd.py
"""
import json
import os
import re
import sqlite3
import unicodedata
from collections import Counter, OrderedDict

import openpyxl
from openpyxl.utils import column_index_from_string as col_idx

RUTA_XLSX = r"C:\Users\MT\AndroidStudioProjects\ubicafeAPP\CAFETERÍAS ENCUESTA PARA APP.xlsx"
SALIDA_DB = r"C:\Users\MT\AndroidStudioProjects\ubicafeAPP\ubicafe.db"
SALIDA_SQL = r"C:\Users\MT\AndroidStudioProjects\ubicafeAPP\esquema.sql"
SALIDA_LOG = r"C:\Users\MT\AndroidStudioProjects\ubicafeAPP\datos_descartados.log"

LAT_MIN, LAT_MAX = -16.60, -16.35
LNG_MIN, LNG_MAX = -68.25, -68.00

ZONAS_CANONICAS = {
    "centro": "Centro",
    "sur": "Sur",
    "cotahuma": "Cotahuma",
    "max paredes": "Max Paredes",
    "max paredes": "Max Paredes",
    "periferica": "Periférica",
    "periférica": "Periférica",
    "mallasa": "Mallasa",
}

BARRIOS = {
    "sopocachi": "Sopocachi",
    "miraflores": "Miraflores",
    "san antonio": "San Antonio",
    "villa fatima": "Villa Fátima",
    "illa fatima": "Villa Fátima",
    "obrajes": "Obrajes",
    "achumani": "Achumani",
    "calacoto": "Calacoto",
    "san miguel": "San Miguel",
    "la florida": "La Florida",
}

CADENAS = {
    "alexander": [r"alexander"],
    "typica": [r"typica", r"ty.?pica"],
    "juan valdez": [r"juan valdez"],
    "rosa negra": [r"rosa negra"],
    "corban": [r"corban"],
    "mugen": [r"^mugen"],
    "andean hills": [r"andean hills"],
    "copacabana": [r"copacabana"],
    "brosso": [r"brosso"],
    "rustik": [r"rustik"],
}

CADENAS_DISPLAY = {
    "alexander": "Alexander",
    "typica": "TYPICA",
    "juan valdez": "Juan Valdez",
    "rosa negra": "Rosa Negra",
    "corban": "CORBAN",
    "mugen": "Mugen Coffee",
    "andean hills": "Andean Hills",
    "copacabana": "Copacabana",
    "brosso": "Brosso",
    "rustik": "Rustik",
}

VARIEDADES_CONOCIDAS = (
    "typica", "típica", "caturra", "catimor", "geisha", "bourbon", "pacamara",
    "catuai", "catuái", "criolla", "maragogype", "garnica", "monda nova",
    "san bernardo", "honduras", "cacao",
)


def limpiar(texto):
    if texto is None:
        return ""
    return str(texto).replace("\n", " ").strip()


def sin_tildes(texto):
    s = unicodedata.normalize("NFKD", texto)
    return "".join(c for c in s if not unicodedata.combining(c))


def nombre_clave(nombre):
    s = limpiar(nombre).lower()
    s = sin_tildes(s)
    s = re.sub(r"\b(cafe|coffee|coffe|coffi|bar|shop|project|drinks and food)\b", " ", s)
    s = re.sub(r"[^a-z0-9]+", " ", s)
    return re.sub(r"\s+", " ", s).strip()


def direccion_clave(direccion):
    return sin_tildes(limpiar(direccion)).lower()


def cadena_de(nombre):
    clave = nombre_clave(nombre)
    for marca, patrones in CADENAS.items():
        for p in patrones:
            if re.search(p, clave):
                return marca
    return None


def zona_canonica(zona):
    c = sin_tildes(limpiar(zona)).lower().replace("  ", " ").strip()
    return ZONAS_CANONICAS.get(c, limpiar(zona) or None)


def barrio_de(direccion):
    d = sin_tildes(limpiar(direccion)).lower()
    for palabra, barrio in BARRIOS.items():
        if palabra in d:
            return barrio
    return None


def tipo_establecimiento(ap, marca_id):
    ap = limpiar(ap).lower()
    if not ap:
        return "marcaNacional" if marca_id else "clasica"
    if "especialidad" in ap:
        return "cafeOrigen"
    if "tostadur" in ap:
        return "tostaderia"
    if "cowork" in ap:
        return "cowork"
    if "comercial" in ap or "cadena" in ap:
        return "marcaNacional"
    if "restaurante" in ap:
        return "restaurante"
    return "clasica"


def extraer_variedades(texto):
    t = limpiar(texto).lower()
    if not t or t in ("1.0", "1", "tradicional", "grano", "natural y lavado",
                      "semi-lavado", "lavado", "natural", "hidratado"):
        return []
    encontradas = [v for v in VARIEDADES_CONOCIDAS if v in t]
    nombres = []
    for v in encontradas:
        nombre = v.capitalize()
        if v == "catuai":
            nombre = "Catuai"
        elif v == "typica" or v == "típica":
            nombre = "Typica"
        elif v == "criolla":
            nombre = "Criolla"
        elif v == "geisha":
            nombre = "Geisha"
        elif v == "pacamara":
            nombre = "Pacamara"
        if nombre not in nombres:
            nombres.append(nombre)
    return nombres


def construir_esquema_sql():
    return """PRAGMA foreign_keys = ON;

CREATE TABLE cafes_origen (
  _id         INTEGER PRIMARY KEY AUTOINCREMENT,
  variedad    TEXT NOT NULL,
  origen      TEXT,
  altitud     INTEGER,
  proceso     TEXT,
  tueste      TEXT,
  aroma       TEXT,
  descripcion TEXT
);

CREATE TABLE marcas (
  _id         INTEGER PRIMARY KEY AUTOINCREMENT,
  nombre      TEXT NOT NULL UNIQUE,
  etiquetas   TEXT,
  es_nacional INTEGER NOT NULL DEFAULT 1,
  descripcion TEXT,
  inicial     TEXT
);

CREATE TABLE cafeterias (
  _id              INTEGER PRIMARY KEY AUTOINCREMENT,
  nombre           TEXT NOT NULL,
  zona             TEXT,
  barrio           TEXT,
  direccion        TEXT,
  horario          TEXT,
  tipo             TEXT,
  marca_id         INTEGER REFERENCES marcas(_id),
  lat              REAL,
  lng              REAL,
  revisar_ubicacion INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE cafeteria_cafes_origen (
  cafeteria_id INTEGER REFERENCES cafeterias(_id),
  cafe_id      INTEGER REFERENCES cafes_origen(_id),
  PRIMARY KEY (cafeteria_id, cafe_id)
);

CREATE TABLE tostadurias (
  _id               INTEGER PRIMARY KEY AUTOINCREMENT,
  nombre            TEXT NOT NULL,
  region            TEXT,
  direccion         TEXT,
  horarios          TEXT,
  descripcion       TEXT,
  contacto          TEXT,
  lat               REAL,
  lng               REAL,
  revisar_ubicacion INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE productores (
  _id          INTEGER PRIMARY KEY AUTOINCREMENT,
  nombre_finca TEXT NOT NULL,
  familia      TEXT,
  origen       TEXT,
  num_cafes    INTEGER,
  altitud      INTEGER,
  descripcion  TEXT,
  lat          REAL,
  lng          REAL
);

CREATE TABLE puntos_de_venta (
  _id       INTEGER PRIMARY KEY AUTOINCREMENT,
  marca     TEXT NOT NULL,
  local     TEXT,
  barrio    TEXT,
  direccion TEXT,
  lat       REAL,
  lng       REAL
);
"""


def main():
    wb = openpyxl.load_workbook(RUTA_XLSX, read_only=True, data_only=True)

    mapa = list(wb["MAPA"].iter_rows(values_only=True))
    enc = list(wb["DATOS LIMPIOS"].iter_rows(values_only=True))
    mapa_rows = mapa[1:]
    enc_rows = enc[1:]

    enriquecimiento = {}
    for r in enc_rows:
        if r[1] is None:
            continue
        enriquecimiento[nombre_clave(r[1])] = {
            "AJ": r[col_idx("AJ") - 1],   # nombre formal cafetería
            "AK": r[col_idx("AK") - 1],   # año de apertura
            "AL": r[col_idx("AL") - 1],   # nº sucursales
            "AP": r[col_idx("AP") - 1],   # tipo de establecimiento
            "BC": r[col_idx("BC") - 1],   # criterios caf especial
            "BU": r[col_idx("BU") - 1],   # preparaciones
            "DD": r[col_idx("DD") - 1],   # variedad de café
            "GALT": r[col_idx("G") - 1],  # altitud
            "J": r[col_idx("J") - 1], "K": r[col_idx("K") - 1],
            "L": r[col_idx("L") - 1], "M": r[col_idx("M") - 1],
        }

    registros = []
    for fila, r in enumerate(mapa_rows, start=2):
        nombre = limpiar(r[0])
        zonas = limpiar(r[1])
        direccion = limpiar(r[2])
        lat = r[3]
        lng = r[4]
        rubro_todos = limpiar(r[5])
        rubro_principal = limpiar(r[6])
        revisar_nota = limpiar(r[7])

        try:
            lat = float(lat) if lat not in (None, "") else None
        except (TypeError, ValueError):
            lat = None
        try:
            lng = float(lng) if lng not in (None, "") else None
        except (TypeError, ValueError):
            lng = None

        registro = {
            "nombre": nombre,
            "zona": zona_canonica(zonas),
            "direccion": direccion,
            "lat": lat,
            "lng": lng,
            "rubro_todos": rubro_todos,
            "rubro_principal": rubro_principal,
            "revisar_nota": revisar_nota,
            "enri": enriquecimiento.get(nombre_clave(nombre)) or {},
            "fila": fila,
        }
        registros.append(registro)

    descartes = []
    for r in registros:
        coord = r["lat"] is not None and r["lng"] is not None
        en_rango = (coord and LAT_MIN <= r["lat"] <= LAT_MAX
                    and LNG_MIN <= r["lng"] <= LNG_MAX)
        r["_gps_ok"] = coord and en_rango
        r["_geo_motivo"] = None
        if not coord:
            r["_geo_motivo"] = "sin GPS"
        elif not en_rango:
            r["_geo_motivo"] = "coordenadas fuera del rango de La Paz"

    # ---- cafeterías / tostadurías / productores / marcas -------------
    cafeterias = []
    tostadurias = []
    productores = []
    marca_nombres = Counter()
    marca_etiquetas = {}

    def clave_marca_para(r):
        cad = cadena_de(r["nombre"])
        if cad:
            return cad
        if "Marca de café" in r["rubro_todos"]:
            return nombre_clave(r["nombre"]) or None
        return None

    for r in registros:
        cm = clave_marca_para(r)
        etiquetas = [t.strip() for t in r["rubro_todos"].split(",") if t.strip()]
        if cm:
            marca_nombres[cm] += 1
            marca_etiquetas.setdefault(cm, set()).update(etiquetas)

    for r in registros:
        nombre_cafeteria = limpiar(r["enri"].get("AJ")) or r["nombre"]
        marca_clave = clave_marca_para(r)
        motivo_revisar = 1 if (r["revisar_nota"] or not r["_gps_ok"]) else 0
        barrio = barrio_de(r["direccion"]) or barrio_de(r["nombre"])
        zona = r["zona"]
        tipo = tipo_establecimiento(r["enri"].get("AP"), marca_clave is not None)

        base = {
            "nombre": nombre_cafeteria,
            "zona": zona,
            "barrio": barrio,
            "direccion": r["direccion"],
            "lat": r["lat"],
            "lng": r["lng"],
            "revisar": motivo_revisar,
            "marca": marca_clave,
            "tipo": tipo,
            "dd": r["enri"].get("DD"),
        }

        if r["rubro_principal"] == "Tostaduría":
            tostadurias.append(dict(base))
        elif r["rubro_principal"] == "Marca de café":
            if "Cafetería" in r["rubro_todos"]:
                cafeterias.append(dict(base))
        else:
            if r["rubro_principal"] == "Sin especificar":
                descartes.append(f"fila {r['fila']} · '{r['nombre']}' · Sin especificar → clasificada como cafetería (heurística)")
            cafeterias.append(dict(base))

        if "Cafetales propios" in r["rubro_todos"]:
            productores.append({
                "nombreFinca": r["nombre"],
                "familia": None,
                "origen": zona or "La Paz",
                "altitud": (int(r["enri"].get("GALT"))
                            if isinstance(r["enri"].get("GALT"), (int, float)) and r["enri"].get("GALT") > 0 else None),
                "numCafes": None,
                "descripcion": None,
                "lat": r["lat"],
                "lng": r["lng"],
            })
            descartes.append(f"fila {r['fila']} · '{r['nombre']}' · cafetales propios incluido además como productor")

    # ---- marcas (cadenas + negocios que producen o son una marca) -------
    marcas = OrderedDict()
    for clave in marca_nombres:
        etiquetas = sorted(marca_etiquetas.get(clave, set())) or ["Cafetería", "Cadena"]
        nombre_marca = CADENAS_DISPLAY.get(clave, clave.title())
        marcas[clave] = {
            "nombre": nombre_marca,
            "etiquetas": etiquetas,
            "inicial": nombre_marca[0],
        }

    # ---- deduplicar cafeterías (mismo nombre+dirección+GPS) ------------
    vistos = set()
    cafeterias_ok = []
    for c in cafeterias:
        k = (nombre_clave(c["nombre"]), direccion_clave(c["direccion"] or ""))
        if k in vistos:
            descartes.append(f"cafetería duplicada: '{c['nombre']}' · {c['direccion']}")
            continue
        vistos.add(k)
        cafeterias_ok.append(c)
    cafeterias = cafeterias_ok

    # ---- puntos de venta (sedes de cadenas) ----------------------------
    puntos_venta = []
    for c in cafeterias:
        if c["marca"]:
            puntos_venta.append({
                "marca": marcas[c["marca"]]["nombre"],
                "local": c["nombre"],
                "barrio": c["barrio"],
                "direccion": c["direccion"],
                "lat": c["lat"],
                "lng": c["lng"],
            })

    # ---- cafés de origen (variedades declaradas) ---------------------
    cafes_nombres = OrderedDict()
    cafeteria_cafes = []
    for c in cafeterias:
        variedades = extraer_variedades(c["dd"])
        for v in variedades:
            if v not in cafes_nombres:
                cafes_nombres[v] = None
            cafeteria_cafes.append((cafeterias.index(c), v))

    # ---- construir SQLite ------------------------------------------------
    if os.path.exists(SALIDA_DB):
        os.remove(SALIDA_DB)
    con = sqlite3.connect(SALIDA_DB)
    con.executescript(construir_esquema_sql())
    cur = con.cursor()

    cafe_ids = {}
    for variedad in cafes_nombres:
        cur.execute(
            "INSERT INTO cafes_origen (variedad, origen, proceso) VALUES (?, ?, ?)",
            (variedad, "La Paz", None))
        cafe_ids[variedad] = cur.lastrowid

    marca_ids = {}
    for clave, m in marcas.items():
        cur.execute(
            "INSERT INTO marcas (nombre, etiquetas, es_nacional, descripcion, inicial) VALUES (?, ?, 1, ?, ?)",
            (m["nombre"], json.dumps(m["etiquetas"]), "", m["inicial"]))
        marca_ids[clave] = cur.lastrowid

    for c in cafeterias:
        cur.execute(
            "INSERT INTO cafeterias (nombre, zona, barrio, direccion, horario, tipo, marca_id, lat, lng, revisar_ubicacion) VALUES (?,?,?,?,?,?,?,?,?,?)",
            (c["nombre"], c["zona"], c["barrio"], c["direccion"] or None, None,
             c["tipo"], marca_ids.get(c["marca"]), c["lat"], c["lng"], c["revisar"]))

    cafeteria_ids = [cur.lastrowid]
    cur.execute("SELECT _id FROM cafeterias ORDER BY _id")
    ids = [row[0] for row in cur.fetchall()]
    for (pos, variedad) in cafeteria_cafes:
        cur.execute("INSERT OR IGNORE INTO cafeteria_cafes_origen (cafeteria_id, cafe_id) VALUES (?,?)",
                    (ids[pos], cafe_ids[variedad]))

    for t in tostadurias:
        cur.execute(
            "INSERT INTO tostadurias (nombre, region, direccion, horarios, descripcion, contacto, lat, lng, revisar_ubicacion) VALUES (?,?,?,?,?,?,?,?,?)",
            (t["nombre"], t["zona"], t["direccion"] or None, None, None, None,
             t["lat"], t["lng"], t["revisar"]))

    for p in productores:
        cur.execute(
            "INSERT INTO productores (nombre_finca, familia, origen, num_cafes, altitud, descripcion, lat, lng) VALUES (?,?,?,?,?,?,?,?)",
            (p["nombreFinca"], p["familia"], p["origen"], p["numCafes"],
             p["altitud"], p["descripcion"], p["lat"], p["lng"]))

    for pv in puntos_venta:
        cur.execute(
            "INSERT INTO puntos_de_venta (marca, local, barrio, direccion, lat, lng) VALUES (?,?,?,?,?,?)",
            (pv["marca"], pv["local"], pv["barrio"], pv["direccion"] or None,
             pv["lat"], pv["lng"]))

    con.commit()

    resumen = {
        "cafeterias": cur.execute("SELECT COUNT(*) FROM cafeterias").fetchone()[0],
        "marcas": cur.execute("SELECT COUNT(*) FROM marcas").fetchone()[0],
        "tostadurias": cur.execute("SELECT COUNT(*) FROM tostadurias").fetchone()[0],
        "productores": cur.execute("SELECT COUNT(*) FROM productores").fetchone()[0],
        "puntos_de_venta": cur.execute("SELECT COUNT(*) FROM puntos_de_venta").fetchone()[0],
        "cafes_origen": cur.execute("SELECT COUNT(*) FROM cafes_origen").fetchone()[0],
        "revisar": cur.execute("SELECT COUNT(*) FROM cafeterias WHERE revisar_ubicacion=1").fetchone()[0],
        "integrity": cur.execute("PRAGMA integrity_check").fetchone()[0],
    }
    con.close()

    with open(SALIDA_SQL, "w", encoding="utf-8") as f:
        f.write(construir_esquema_sql())

    with open(SALIDA_LOG, "w", encoding="utf-8") as f:
        f.write("Datos descartados o con anotaciones\n" + "=" * 50 + "\n")
        f.write("\n".join(descartes) + "\n")

    print(json.dumps(resumen, ensure_ascii=False, indent=2))
    print("cadenas =>", {k: v["nombre"] for k, v in marcas.items()})


if __name__ == "__main__":
    main()