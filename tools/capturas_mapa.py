# -*- coding: utf-8 -*-
"""
Compositor de capturas del mapa de UbiCafe para la guía .docx.

Reproduce las pantallas reales de la app (fragment_mapa y activity_mapa)
usando:
  - teselas REALES de OpenStreetMap (tile.openstreetmap.org),
  - los datos REALES de ubicafe.db (206 cafeterías, cadenas, etc.),
  - los colores y tipografía exactos del proyecto (colors.xml, res/font).

Uso:  python tools/capturas_mapa.py  (Python 3.11 de Windows con Pillow)
Salida: docs/figuras/*.png
"""
import json
import math
import os
import sqlite3
import urllib.request

from PIL import Image, ImageDraw, ImageFont

RAIZ = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RUTA_DB = os.path.join(RAIZ, "ubicafe.db")
RUTA_FIGURAS = os.path.join(RAIZ, "docs", "figuras")
TILE_CACHE = os.path.join(os.environ.get("TEMP", RAIZ), "opencode", "osm_tiles")
FUENTES = os.path.join(RAIZ, "app", "src", "main", "res", "font")

UA = "UbiCafe/1.0 (guia-docente)"

# --- Paleta exacta de colors.xml ------------------------------------------
FONDO_CREMA = (0xF8, 0xF4, 0xEA)
FONDO_BLANCO = (0xFF, 0xFF, 0xFF)
SUPERFICIE = (0xF1, 0xEB, 0xDD)
VERDE_OSCURO = (0x16, 0x4E, 0x3B)
TEXTO_PRINCIPAL = (0x26, 0x33, 0x2E)
TEXTO_SECUNDARIO = (0x4E, 0x5A, 0x55)
TEXTO_TENUE = (0x78, 0x81, 0x7D)
TEXTO_SOBRE_VERDE = (0xFF, 0xFF, 0xFF)
CAFE_ACCENT = (0x76, 0x51, 0x3D)
CARAMELO_CLARO = (0xDD, 0xA1, 0x5E)
VERDE_MEDIO = (0x6B, 0x8F, 0x71)
AZUL_UBICACION = (0x2E, 0x6D, 0xB4)
DIVISOR = (0xE0, 0xE1, 0xDC)

# Chip "Todos"/"Cafeterías" seleccionado = verde al 10% sobre el fondo
def mezclar(c1, c2, alpha):
    return tuple(round(c1[i] * alpha + c2[i] * (1 - alpha)) for i in range(3))

CHIP_SEL_BG = mezclar(VERDE_OSCURO, FONDO_CREMA, 0.10)

# --- Tipografía (las mismas fuentes que usa la app) ------------------------
_cache_fuentes = {}
def fuente(nombre, tam_px):
    clave = (nombre, tam_px)
    if clave not in _cache_fuentes:
        ruta = os.path.join(FUENTES, nombre)
        _cache_fuentes[clave] = ImageFont.truetype(ruta, tam_px)
    return _cache_fuentes[clave]

def texto(dib, xy, s, font_name, size, fill, anchor="la"):
    di = dib
    di.text(xy, s, font=fuente(font_name, size), fill=fill, anchor=anchor)

# --- Mercator --------------------------------------------------------------
def world_px(lat, lng, zp):
    n = 2 ** zp
    w = 256 * n
    x = (lng + 180.0) / 360.0 * w
    r = math.radians(lat)
    y = (1.0 - math.log(math.tan(r) + 1.0 / math.cos(r)) / math.pi) / 2.0 * w
    return x, y

def descargar_tesela(z, x, y):
    carpeta = os.path.join(TILE_CACHE, str(z))
    os.makedirs(carpeta, exist_ok=True)
    archivo = os.path.join(carpeta, f"{x}_{y}.png")
    if not os.path.exists(archivo):
        url = f"https://tile.openstreetmap.org/{z}/{x}/{y}.png"
        req = urllib.request.Request(url, headers={"User-Agent": UA})
        with urllib.request.urlopen(req, timeout=20) as r:
            datos = r.read()
        with open(archivo, "wb") as f:
            f.write(datos)
    return Image.open(archivo).convert("RGB")

def mapa_osm(centro, zp, ancho, alto):
    """Recorta el mapa de OSM (teselas de zoom Zp) de ancho×alto píxeles."""
    cx, cy = world_px(centro[0], centro[1], zp)
    izq = cx - ancho / 2.0
    arr = cy - alto / 2.0
    lienzo = Image.new("RGB", (ancho, alto), FONDO_CREMA)
    tx0 = int(math.floor(izq / 256.0))
    tx1 = int(math.floor((izq + ancho) / 256.0))
    ty0 = int(math.floor(arr / 256.0))
    ty1 = int(math.floor((arr + alto) / 256.0))
    for ty in range(ty0, ty1 + 1):
        for tx in range(tx0, tx1 + 1):
            try:
                tesela = descargar_tesela(zp, tx, ty)
            except Exception:
                continue
            px = int(round(tx * 256 - izq))
            py = int(round(ty * 256 - arr))
            lienzo.paste(tesela, (px, py))
    # Atribución de OpenStreetMap (buena práctica real)
    d = ImageDraw.Draw(lienzo)
    f = fuente("nunito_sans_medium.ttf", 22)
    et = "© OpenStreetMap contributors"
    ancho_et = d.textlength(et, font=f)
    d.rectangle([ancho - ancho_et - 12, alto - 30, ancho - 2, alto - 2],
                fill=mezclar((255, 255, 255), FONDO_CREMA, 0.85))
    d.text((ancho - ancho_et - 6, alto - 24), et, font=f, fill=TEXTO_TENUE)
    return lienzo, (izq, arr)

# --- Mapa base con píxeles de mundo ya calculados --------------------------
def mapa_con_puntos(centro, zp, ancho, alto, puntos):
    lienzo, (izq, arr) = mapa_osm(centro, zp, ancho, alto)
    dib = ImageDraw.Draw(lienzo, "RGBA")
    cuenta = 0
    for lat, lng, color in puntos:
        x, y = world_px(lat, lng, zp)
        px_ = x - izq
        py_ = y - arr
        if 0 <= px_ <= ancho and 0 <= py_ <= alto:
            pin(dib, px_, py_, color)
            cuenta += 1
    return lienzo, cuenta

def pin(dib, cx, cy, color):
    r = 13
    cabeza = (cx, cy - 24)
    # sombra suave
    dib.ellipse([cx - 16, cy - 4, cx + 16, cy + 4], fill=(0, 0, 0, 40))
    # cuerpo tipo gota: círculo + triángulo
    dib.ellipse([cabeza[0] - r, cabeza[1] - r, cabeza[0] + r, cabeza[1] + r], fill=color)
    dib.polygon([(cx - r, cabeza[1] + 2), (cx + r, cabeza[1] + 2), (cx, cy)], fill=color)
    # bisel y orificio blancos
    dib.ellipse([cabeza[0] - r, cabeza[1] - r, cabeza[0] + r, cabeza[1] + r],
                outline=(255, 255, 255, 200), width=2)
    dib.ellipse([cx - 5, cabeza[1] - 5, cx + 5, cabeza[1] + 5], fill=(255, 255, 255, 230))

# --- Componentes de UI -----------------------------------------------------
def chip(dib, x, y, etiqueta, seleccionado, h=68):
    f = fuente("nunito_sans_semi_bold.ttf", 26)
    ancho = dib.textlength(etiqueta, font=f) + 48
    bg = CHIP_SEL_BG if seleccionado else FONDO_BLANCO
    borde = None if seleccionado else DIVISOR
    dib.rounded_rectangle([x, y, x + ancho, y + h], radius=h // 2, fill=bg,
                          outline=borde, width=2 if borde else 0)
    color = VERDE_OSCURO if seleccionado else TEXTO_PRINCIPAL
    dib.text((x + 24, y + h / 2 - 1), etiqueta, font=f, fill=color, anchor="lm")
    return x + ancho + 16

def boton_verde(dib, x, y, ancho_, h, etiqueta):
    dib.rounded_rectangle([x, y, x + ancho_, y + h], radius=16, fill=VERDE_OSCURO)
    f = fuente("nunito_sans_semi_bold.ttf", 30)
    dib.text((x + ancho_ / 2, y + h / 2), etiqueta, font=f,
             fill=TEXTO_SOBRE_VERDE, anchor="mm")

def tarjeta(dib, x0, y0, x1, y1, fondo=FONDO_BLANCO, radio=36, sombra=True):
    if sombra:
        dib.rounded_rectangle([x0, y0 + 6, x1, y1 + 8], radius=radio,
                              fill=(0, 0, 0, 18))
    dib.rounded_rectangle([x0, y0, x1, y1], radius=radio, fill=fondo)

def estrella(dib, cx, cy, r=13):
    pts = []
    for i in range(10):
        ang = math.pi / 2 + i * math.pi / 5
        rr = r if i % 2 == 0 else r * 0.45
        pts.append((cx + rr * math.cos(ang), cy + rr * math.sin(ang)))
    dib.polygon(pts, fill=CARAMELO_CLARO)

def cabeza_fragment(dib, ancho):
    """Encabezado del fragment_mapa: 'Mapa' + 'Achumani, La Paz' + chips."""
    texto(dib, (40, 40), "Mapa", "nunito_extra_bold.ttf", 48, VERDE_OSCURO)
    texto(dib, (40, 96), "Achumani, La Paz", "nunito_sans_medium.ttf", 26, TEXTO_TENUE)

def fila_chips(dib, x, y, seleccionado):
    for i, nombre in enumerate(["Todos", "Cafeterías", "Tostadurías",
                                "Productores", "Puntos de venta"]):
        x = chip(dib, x, y, nombre, seleccionado == i)

def barra_titulo(dib, titulo, subtitulo=None):
    """Cabecera de activity_mapa (barra_titulo.xml)."""
    cx, cy = 72, 72
    dib.ellipse([cx - 40, cy - 40, cx + 40, cy + 40], fill=FONDO_BLANCO)
    dib.ellipse([cx - 40, cy - 40, cx + 40, cy + 40], outline=DIVISOR, width=2)
    # chevron izquierdo
    f = fuente("nunito_extra_bold.ttf", 48)
    dib.text((cx, cy), "<", font=f, fill=TEXTO_PRINCIPAL, anchor="mm")
    texto(dib, (132, 36), titulo, "nunito_extra_bold.ttf", 48, VERDE_OSCURO)
    if subtitulo:
        texto(dib, (132, 92), subtitulo, "nunito_sans_medium.ttf", 26, TEXTO_TENUE)

def tarjeta_fragment(dib, ancho, alto, nombre, tipo, dir_, rating=True):
    """Panel inferior del fragment_mapa (info del marcador)."""
    ms = 32
    x0, x1 = ms, ancho - ms
    y1 = alto - ms
    y0 = y1 - 470
    tarjeta(dib, x0, y0, x1, y1)
    x = x0 + 32
    texto(dib, (x, y0 + 34), nombre, "nunito_bold.ttf", 40, TEXTO_PRINCIPAL)
    texto(dib, (x, y0 + 84), tipo, "nunito_sans_medium.ttf", 26, TEXTO_SECUNDARIO)
    texto(dib, (x, y0 + 126), dir_, "nunito_sans_medium.ttf", 30, TEXTO_PRINCIPAL)
    if rating:
        estrella(dib, x + 16, y0 + 172)
        texto(dib, (x + 44, y0 + 172), "4.9", "nunito_sans_bold.ttf", 40,
              TEXTO_PRINCIPAL, anchor="lm")
        texto(dib, (x + 100, y0 + 172), "Rating", "nunito_sans_medium.ttf",
              26, TEXTO_TENUE, anchor="lm")
    boton_verde(dib, x, y1 - 32 - 96, ancho - x - 32, 96, "Ver información")

def tarjeta_activity(dib, ancho, alto, titulo, sub, subtitulo_header=None):
    """Tarjeta inferior de activity_mapa (CardView)."""
    ms = 32
    x0, x1 = ms, ancho - ms
    y1 = alto - 32
    y0 = y1 - 380
    tarjeta(dib, x0, y0, x1, y1, radio=36)
    x = x0 + 32
    texto(dib, (x, y0 + 32), titulo, "nunito_bold.ttf", 34, TEXTO_PRINCIPAL)
    texto(dib, (x, y0 + 80), sub, "nunito_sans_medium.ttf", 26, TEXTO_SECUNDARIO)
    boton_verde(dib, x, y1 - 32 - 96, ancho - x - 32, 96, "Ver información")

# --- Datos ----------------------------------------------------------------:
def cargar_datos():
    con = sqlite3.connect(RUTA_DB)
    cur = con.cursor()
    cafes = cur.execute(
        "SELECT nombre, zona, lat, lng FROM cafeterias "
        "WHERE lat IS NOT NULL AND lng IS NOT NULL").fetchall()
    tost = cur.execute(
        "SELECT nombre, region, lat, lng FROM tostadurias "
        "WHERE lat IS NOT NULL AND lng IS NOT NULL").fetchall()
    prod = cur.execute(
        "SELECT nombre_finca, origen, lat, lng FROM productores "
        "WHERE lat IS NOT NULL AND lng IS NOT NULL").fetchall()
    pv = cur.execute(
        "SELECT marca, local, barrio, lat, lng FROM puntos_de_venta "
        "WHERE lat IS NOT NULL AND lng IS NOT NULL").fetchall()
    con.close()
    return cafes, tost, prod, pv

def elegir(cafes, subcadenas, zona=None):
    for nombre, z, lat, lng in cafes:
        if zona and z != zona:
            continue
        if any(s in nombre.lower() for s in subcadenas):
            return nombre, z, lat, lng
    return None

# --- Figuras ---------------------------------------------------------------
def figura_principal(cafes, tost, prod, pv, seleccion=None):
    ancho, alto = 1200, 2400
    lienzo = Image.new("RGB", (ancho, alto), FONDO_CREMA)
    dib = ImageDraw.Draw(lienzo)
    cabeza_fragment(dib, ancho)
    fila_chips(dib, 40, 130, seleccion)
    y0 = 228
    y1 = alto - 32 - 470 - 32  # deja margen para la tarjeta
    region_h = y1 - y0

    if seleccion is None:  # Todos
        puntos = ([(c[2], c[3], VERDE_OSCURO) for c in cafes]
                  + [(t[2], t[3], CAFE_ACCENT) for t in tost]
                  + [(p[2], p[3], VERDE_MEDIO) for p in prod]
                  + [(v[3], v[4], CARAMELO_CLARO) for v in pv]
                  + [(-16.535, -68.070, AZUL_UBICACION)])
    else:
        puntos = [(c[2], c[3], VERDE_OSCURO) for c in cafes]

    centro = (-16.500, -68.120)
    mapa, _ = mapa_con_puntos(centro, 13, ancho, region_h, puntos)
    lienzo.paste(mapa, (0, y0))

    nombre, z, lat, lng = elegir(cafes, ["typica"],
                                 "Sur") or cafes[0][:2] + (cafes[0][2], cafes[0][3])
    tarjeta_fragment(dib, ancho, alto, nombre, "Cafetería · " + z, z)
    return lienzo

def figura_cerca_de_ti(cafes):
    sel = elegir(cafes, ["typica", "tuta", "la muela", "café del migrante",
                         "veirut", "musa", "au cafe", "unicafe"])
    if not sel:
        sel = next(c for c in cafes if c[1] == "Sur" and c[2] > -16.55)
    nombre, z, lat, lng = sel
    ancho, alto = 1200, 2100
    lienzo = Image.new("RGB", (ancho, alto), FONDO_CREMA)
    dib = ImageDraw.Draw(lienzo)
    barra_titulo(dib, "Mapa")
    y0 = 144
    y1 = alto - 32 - 380 - 32
    region_h = y1 - y0
    mapa, _ = mapa_con_puntos((lat, lng), 16, ancho, region_h,
                              [(lat, lng, VERDE_OSCURO)])
    lienzo.paste(mapa, (0, y0))
    tarjeta_activity(dib, ancho, alto, nombre, "Cafeterías · " + z)
    return lienzo

def figura_puntos_marca(pv, marca="TYPICA"):
    puntos = [v for v in pv if v[0].lower() == marca.lower()]
    if not puntos:
        puntos = pv[:5]
    lat_c = sum(p[3] for p in puntos) / len(puntos)
    lng_c = sum(p[4] for p in puntos) / len(puntos)
    ancho, alto = 1200, 2100
    lienzo = Image.new("RGB", (ancho, alto), FONDO_CREMA)
    dib = ImageDraw.Draw(lienzo)
    barra_titulo(dib, "Mapa", marca + " · Puntos de venta")
    y0 = 144
    y1 = alto - 32 - 380 - 32
    region_h = y1 - y0
    mapa, _ = mapa_con_puntos((lat_c, lng_c), 14, ancho, region_h,
                              [(p[3], p[4], CARAMELO_CLARO) for p in puntos])
    lienzo.paste(mapa, (0, y0))
    local = puntos[0][1] or puntos[0][2]
    tarjeta_activity(dib, ancho, alto, marca + " · " + local,
                     "Puntos de venta · " + (puntos[0][2] or "La Paz"))
    return lienzo

def main():
    os.makedirs(RUTA_FIGURAS, exist_ok=True)
    cafes, tost, prod, pv = cargar_datos()
    figuras = [
        ("figura_mapa_todos.png", figura_principal(cafes, tost, prod, pv, None)),
        ("figura_mapa_cafeterias.png",
         figura_principal(cafes, tost, prod, pv, "Cafeterías")),
        ("figura_mapa_cerca_de_ti.png", figura_cerca_de_ti(cafes)),
        ("figura_mapa_puntos_venta.png", figura_puntos_marca(pv, "TYPICA")),
    ]
    for nombre, img in figuras:
        ruta = os.path.join(RUTA_FIGURAS, nombre)
        img.save(ruta, "PNG")
        print("OK", ruta, img.size)

if __name__ == "__main__":
    main()