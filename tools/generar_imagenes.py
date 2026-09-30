# -*- coding: utf-8 -*-
"""
Generador de las carpetas de imagenes del censo de UbiCafe.

Recorre los 242 registros de la base (189 cafeterías, 40 marcas y 13
tostadurías) y deja una foto para cada uno en el proyecto:

    imagenesCafes/         189 fotos
    imagenesMarcas/         40 fotos
    imagenesTostadurias/    13 fotos
    imagenes.json           manifiesto id -> archivo

DE DÓNDE SALE CADA FOTO, y por qué está en el manifiesto
---------------------------------------------------------
Dos orígenes, y la diferencia se guarda en el campo "origen":

  "propia"    La foto sale de la web del propio negocio. Se busca con
              DuckDuckGo y solo se aceptan dominios que sean del negocio;
              se descartan instagram, facebook y los agregadores.
  "generica"  No se encontró foto del negocio, así que se usa una foto
              real de café de Wikimedia Commons. Es un marcador de
              posición, NO es el local: por eso queda anotado como tal
              y no como "propia".

Lo normal es que casi todo salga "generica": la mayoría de los negocios
del censo no tienen web propia y sus fotos están en redes sociales, que
no dejan descargarlas. El script lo reporta al final en vez de
disimularlo.

Las fotos genéricas se descargan de Wikimedia Commons y llevan licencia
libre con autor. Cada una queda anotada en "credito" y "url" para poder
acreditarla.

Uso:
    python tools/generar_imagenes.py pool       # solo el banco de fotos
    python tools/generar_imagenes.py buscar     # busca foto propia de cada uno
    python tools/generar_imagenes.py ensamblar  # arma carpetas + manifiesto
    python tools/generar_imagenes.py todo        # las tres, en orden

Se puede interrumpir y reanudar: el banco y los resultados de búsqueda
se guardan en tools/_cache_imagenes/ y no se vuelven a descargar.
"""
import hashlib
import html
import json
import os
import re
import sys
import time
import unicodedata
import urllib.error
import urllib.parse
import urllib.request

from PIL import Image

RAIZ = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CENSO = os.path.join(RAIZ, "app", "src", "main", "assets", "censo_ubicafe.json")
CACHE = os.path.join(RAIZ, "tools", "_cache_imagenes")
POOL_DIR = os.path.join(CACHE, "pool")
CACHE_BUSQUEDA = os.path.join(CACHE, "busquedas.json")
MANIFIESTO = os.path.join(RAIZ, "imagenes.json")

CARPETA_CAFES = os.path.join(RAIZ, "imagenesCafes")
CARPETA_MARCAS = os.path.join(RAIZ, "imagenesMarcas")
CARPETA_TOSTADURIAS = os.path.join(RAIZ, "imagenesTostadurias")

TAMANO = (1024, 768)
TAMANO_MIN_POOL = 20  # con menos que esto el relleno queda demasiado repetido

# Sin esto no bajan nada de Wikimedia: responden 403 sin User-Agent.
UA = "UbiCafe/1.0 (proyecto educativo del censo cafetero de La Paz; contacto: ubicafe@example.org)"

# Segundos entre búsquedas. El buscador tira captcha a quien va rápido.
PAUSA_BUSQUEDA = 3.0
PAUSA_POOL = 1.0

# Fotografías que no sirven como marcador genérico: son de una cadena
# concreta, así que no pueden representar a otro local.
MARCAS_COMERCIALES = [
    "starbucks", "costa", "dunkin", "mcdonald", "mcdonalds", "nescafe",
    "nespresso", "nestle", "illy", "lavazza", "segafredo", "jacobs",
    "nero", "tim hortons", "peets", "caffe nero", "krispy kreme",
    "starbox", "the coffeebean", "carlsberg",
]

# Dominios que nunca son la web del negocio, aunque aparezcan primeros.
DOMINIOS_DESCARTADOS = [
    "instagram.com", "facebook.com", "fb.com", "twitter.com", "x.com",
    "tiktok.com", "youtube.com", "sluurpy.com", "tripadvisor.com",
    "tripadvisor.es", "google.com", "maps.app.goo.gl", "goo.gl",
    "wikipedia.org", "amazon.com", "ebay.com", "menu", "delivery",
    "justo.com", "rappi.com", "ubereats.com", "doordash", "yelp",
    "opinion.com.bo", "pinterest", "linkedin.com", "foursquare",
    "apple.com", "play.google", "qr-code", "pdf", "linkedin",
]


# ---------------------------------------------------------------- utilidades

def pedir(url, timeout=25):
    """GET con User-Agent. Devuelve los bytes o None."""
    peticion = urllib.request.Request(url, headers={
        "User-Agent": UA,
        "Accept-Language": "es,en;q=0.8",
    })
    try:
        with urllib.request.urlopen(peticion, timeout=timeout) as r:
            return r.read()
    except (urllib.error.URLError, urllib.error.HTTPError, OSError, ValueError):
        return None


def pedir_json(url, timeout=25):
    crudo = pedir(url, timeout)
    if not crudo:
        return None
    try:
        return json.loads(crudo.decode("utf-8", "ignore"))
    except (ValueError, UnicodeDecodeError):
        return None


def slug(texto, largo=32):
    """'Café con Pan' -> 'cafe_con_pan'. Sin acentos, sin ñ, minúsculas."""
    t = unicodedata.normalize("NFKD", texto or "")
    t = t.encode("ascii", "ignore").decode()
    t = re.sub(r"[^a-z0-9]+", "_", t.lower()).strip("_")
    return t[:largo].rstrip("_")


def clave(texto):
    """
    La clave con la que la app empareja un nombre con su foto.

    Tiene que ser EXACTAMENTE lo que hace Texto.clave() en Java: quitar
    los signos combinantes con NFD, pasar a minúsculas y recortar. Si
    aquí se dejaran las tildes, 'Café con Pan' se guardaría como
    'café con pan' y la app, que busca 'cafe con pan', no encontraría
    nada: la marca se quedaría sin foto sin avisar. Por eso la función
    existe y no se usa un .lower() a pelo.
    """
    t = unicodedata.normalize("NFD", texto or "")
    t = "".join(c for c in t if unicodedata.category(c) != "Mn")
    return t.lower().strip()


PREFIJOS_DIRECCION = re.compile(
    r"^(?:calle|av|avda|avda\.|avenida|plaza|sobre|direccion|dirección|calle)\.?\s+",
    re.IGNORECASE)
CORTE_DIRECCION = re.compile(
    r"(?:\s+(?:n\s*°|n°|#|n\.?°|entre|esquina|local\s+\d)\s+)", re.IGNORECASE)


def calle_de(direccion, macrodistrito):
    """
    Saca el sufijo del archivo de una dirección tipo
    'Av. 20 de octubre' -> '20_de_octubre'.

    Si la dirección está vacía o es un link de Google Maps (pasa en
    algunas tostadurías), cae al macrodistrito, porque inventar una calle
    a partir de un link no sirve de nada.
    """
    t = (direccion or "").strip()
    if t and "http" not in t.lower():
        t = CORTE_DIRECCION.split(t)[0]
        t = PREFIJOS_DIRECCION.sub("", t).strip()
        t = slug(t, 24)
        if t:
            return t
    return slug(macrodistrito, 20) or "lapaz"


def unicos(nombres):
    """
    Si dos fichas dependen del mismo nombre, a la segunda se le pega _2.
    Devuelve la lista de nombres ya sin colisiones.
    """
    vistos = set()
    salida = []
    for n in nombres:
        base, i = n, 2
        while base in vistos:
            base = "%s_%d" % (n, i)
            i += 1
        vistos.add(base)
        salida.append(base)
    return salida


def recortar(origen, destino):
    """
    Center-crop a 4:3 y deja la imagen en 1024x768. Devuelve False si el
    archivo no era una imagen válida, para que el pool descarte lo roto.
    """
    try:
        with Image.open(origen) as im:
            im = im.convert("RGB")
            ancho, alto = im.size
            objetivo = TAMANO[0] / TAMANO[1]
            actual = ancho / alto
            if actual > objetivo:          # muy ancha: recorta a los lados
                nuevo = int(alto * objetivo)
                izquierda = (ancho - nuevo) // 2
                im = im.crop((izquierda, 0, izquierda + nuevo, alto))
            elif actual < objetivo:        # muy alta: recorta arriba y abajo
                nuevo = int(ancho / objetivo)
                arriba = (alto - nuevo) // 2
                im = im.crop((0, arriba, ancho, arriba + nuevo))
            im = im.resize(TAMANO, Image.LANCZOS)
            im.save(destino, "JPEG", quality=80, optimize=True)
        return True
    except Exception as e:  # noqa: BLE001 - el pool descarta lo que sea
        print("      (descartada: %s)" % e.__class__.__name__)
        return False


def sin_texto_sin_marcas(titulo):
    t = titulo.lower()
    return not any(m in t for m in MARCAS_COMERCIALES)


def limpiar_html(bruto):
    """Quita scripts y estilos antes de buscar URLs de imágenes."""
    s = bruto.decode("utf-8", "ignore")
    s = re.sub(r"<script[^>]*>.*?</script>", " ", s, flags=re.S | re.I)
    s = re.sub(r"<style[^>]*>.*?</style>", " ", s, flags=re.S | re.I)
    return s


# ------------------------------------------------------------------- lectura

def cargar_censo():
    with open(CENSO, encoding="utf-8") as f:
        return json.load(f)


def construir_fichas(censo):
    """
    Devuelve las 242 fichas listas, con su nombre de archivo ya resuelto.

    Cada ficha: id, nombre, zona, calle, carpeta, clave y archivo.
    """
    entidades = censo["entidades"]
    marcas = censo["marcas"]

    def es(ent, rol):
        return rol in (ent.get("roles") or [])

    # Cafés: el nombre de archivo es nombre + calle de la dirección.
    cafes = [e for e in entidades if es(e, "CAFETERIA")]
    nombres = unicos([
        "%s_%s" % (slug(e["nombre"]), calle_de(e.get("direccion"), e.get("macrodistrito")))
        for e in cafes])
    fichas = [{
        "id": e["id"],
        "nombre": e["nombre"],
        "zona": e.get("macrodistrito") or "",
        "direccion": e.get("direccion") or "",
        "carpeta": "cafes",
        "clave": e["id"],
        "archivo": "%s.jpg" % n,
    } for e, n in zip(cafes, nombres)]

    # Tostadurías: mismo criterio. 13 fichas.
    tosts = [e for e in entidades if es(e, "TOSTADURIA")]
    nombres_t = unicos([
        "%s_%s" % (slug(e["nombre"]), calle_de(e.get("direccion"), e.get("macrodistrito")))
        for e in tosts])
    fichas += [{
        "id": e["id"],
        "nombre": e["nombre"],
        "zona": e.get("macrodistrito") or "",
        "direccion": e.get("direccion") or "",
        "carpeta": "tostadurias",
        "clave": e["id"],
        "archivo": "%s.jpg" % n,
    } for e, n in zip(tosts, nombres_t)]

    # Marcas: una ficha por marca, no por local. Una marca no tiene
    # sucursal propia, así que el sufijo solo se pone si la ficha dice de
    # dónde es (municipio de origen o cobertura). Si no lo dice, va el
    # nombre solo: un 'sin_nombre' inventado no aporta nada.
    nombres_m = unicos([
        ("%s_%s" % (slug(m["nombre"]),
                    slug(m.get("municipioOrigen") or m.get("cobertura"), 20)))
        if (m.get("municipioOrigen") or m.get("cobertura"))
        else slug(m["nombre"])
        for m in marcas])
    fichas += [{
        "id": "marca-" + slug(m["nombre"], 40),
        "nombre": m["nombre"],
        "zona": m.get("cobertura") or m.get("municipioOrigen") or "",
        "direccion": "",
        "carpeta": "marcas",
        "clave": clave(m["nombre"]),
        "archivo": "%s.jpg" % n.rstrip("_"),
    } for m, n in zip(marcas, nombres_m)]

    return fichas


# ------------------------------------------------------- banco de genéricas

CONSULTAS_POOL = [
    "coffee cup latte art",
    "cappuccino cup",
    "espresso cup",
    "coffee shop interior",
    "coffee beans roasted",
    "café Bolivia",
    "coffee cup table",
    "barista portafilter",
    "coffee grinder",
    "latte glass",
]


def licencia_permitida(valor):
    v = (valor or "").lower()
    return any(x in v for x in ["cc0", "cc by", "cc-by", "public domain", "pd-", "attribution"])


def bajar_pool(minimo=TAMANO_MIN_POOL):
    """
    Descarga fotos reales de café de Wikimedia Commons con licencia libre
    y las deja recortadas en tools/_cache_imagenes/pool/.
    """
    os.makedirs(POOL_DIR, exist_ok=True)
    ya = [f for f in os.listdir(POOL_DIR) if f.endswith(".jpg")]
    print("Banco de genéricas: %d ya descargadas" % len(ya))

    creditos = {}
    ruta_creditos = os.path.join(CACHE, "pool_creditos.json")
    if os.path.exists(ruta_creditos):
        with open(ruta_creditos, encoding="utf-8") as f:
            creditos = json.load(f)

    indice = 1
    for consulta in CONSULTAS_POOL:
        if len(ya) >= 60:
            break
        params = {
            "action": "query", "format": "json", "generator": "search",
            "gsrsearch": "filetype:bitmap %s" % consulta,
            "gsrnamespace": "6", "gsrlimit": "25",
            "prop": "imageinfo", "iiprop": "url|size|extmetadata",
            "iiurlwidth": "1400",
        }
        datos = pedir_json("https://commons.wikimedia.org/w/api.php?"
                           + urllib.parse.urlencode(params))
        if not datos or "query" not in datos:
            print("  (consulta '%s' sin respuesta)" % consulta)
            time.sleep(PAUSA_POOL)
            continue

        for pagina in datos["query"].get("pages", {}).values():
            if len(ya) >= 60:
                break
            info = (pagina.get("imageinfo") or [{}])[0]
            meta = info.get("extmetadata", {})
            titulo = pagina.get("title", "")
            licencia = meta.get("LicenseShortName", {}).get("value", "")
            url = info.get("thumburl") or info.get("url")
            if not url or not licencia_permitida(licencia):
                continue
            if not sin_texto_sin_marcas(titulo):
                continue

            nombre = "pool_%02d.jpg" % indice
            destino = os.path.join(POOL_DIR, nombre)
            indice += 1
            crudo = pedir(url)
            if not crudo or len(crudo) < 12000:
                print("      (descarga fallida o muy chica: %s)" % titulo[:40])
                continue
            temporal = destino + ".orig"
            with open(temporal, "wb") as f:
                f.write(crudo)
            if not recortar(temporal, destino):
                os.remove(temporal)
                continue
            os.remove(temporal)
            ya.append(nombre)
            creditos[nombre] = {
                "url": url,
                "titulo": titulo,
                "licencia": licencia,
                "autor": limpiar_texto(meta.get("Artist", {}).get("value", "")),
            }
            print("      + %s  %s  (%s)" % (nombre, titulo[:44], licencia))

        with open(ruta_creditos, "w", encoding="utf-8") as f:
            json.dump(creditos, f, ensure_ascii=False, indent=1)
        time.sleep(PAUSA_POOL)

    print("Banco de genéricas: %d fotos en total" % len(ya))
    if len(ya) < minimo:
        print("AVISO: el banco quedó corto (%d). El relleno se va a repetir "
              "mucho." % len(ya))
    return creditos


def limpiar_texto(html_bruto):
    s = re.sub(r"<[^>]+>", " ", html_bruto or "")
    return html.unescape(re.sub(r"\s+", " ", s)).strip()[:90]


def archivos_pool():
    if not os.path.isdir(POOL_DIR):
        return []
    return sorted(f for f in os.listdir(POOL_DIR) if f.endswith(".jpg"))


# ------------------------------------------------- búsqueda de foto propia

def consulta_ddg(texto):
    """Busca en DuckDuckGo y devuelve la lista de dominios encontrados."""
    url = "https://html.duckduckgo.com/html/?" + urllib.parse.urlencode({"q": texto})
    crudo = pedir(url)
    if not crudo:
        return None
    s = crudo.decode("utf-8", "ignore")
    # Si aparece un captcha hay que parar, no seguir golpeando.
    if re.search(r"anomaly|captcha|unusual traffic|blocked", s, re.I):
        return "CAPTCHA"
    destinos = []
    for m in re.finditer(r"uddg=([^&\"]+)", s):
        u = urllib.parse.unquote(m.group(1))
        if u.startswith("http"):
            destinos.append(u)
    vistos, ordenados = set(), []
    for u in destinos:
        host = urllib.parse.urlparse(u).netloc.lower()
        if host in vistos:
            continue
        if any(d in u.lower() for d in DOMINIOS_DESCARTADOS):
            continue
        vistos.add(host)
        ordenados.append(u)
    return ordenados


def imagenes_de_la_pagina(url):
    """Saca la mejor foto de una página: og:image primero, si no el mayor <img>."""
    crudo = pedir(url)
    if not crudo:
        return None
    base = urllib.parse.urlparse(url)
    candidatas = []

    og = re.search(rb'property=["\']og:image["\'][^>]+content=["\']([^"\']+)',
                   crudo, re.I)
    if not og:
        og = re.search(rb'content=["\']([^"\']+)["\'][^>]+property=["\']og:image["\']',
                       crudo, re.I)
    if og:
        candidatas.append((100, og.group(1).decode("utf-8", "ignore")))

    s = limpiar_html(crudo)
    for m in re.finditer(r'<img[^>]+src=["\']([^"\']+)["\']', s, re.I):
        src = m.group(1)
        if src.startswith("data:"):
            continue
        if any(x in src.lower() for x in ["logo", "icon", "sprite", "avatar",
                                          "favicon", "badge", "banner_sitio"]):
            continue
        candidatas.append((10, src))
    for m in re.finditer(r'background-image\s*:\s*url\(["\']?([^"\')]+)', s, re.I):
        candidatas.append((5, m.group(1)))

    for _, src in candidatas:
        if src.startswith("//"):
            src = "https:" + src
        elif src.startswith("/"):
            src = "%s://%s%s" % (base.scheme, base.netloc, src)
        elif not src.startswith("http"):
            continue
        if not src.lower().split("?")[0].endswith(
                (".jpg", ".jpeg", ".png", ".webp")):
            continue
        return src
    return None


def buscar_foto_propia(ficha):
    """
    Intenta la foto del propio negocio. Devuelve (url, pagina) o (None, None).

    Se busca por nombre y zona, y se reintenta con la calle si la primera
    ronda no da nada, porque hay fichas cuya dirección es un link de Maps
    y en la dirección no hay nada que buscar.
    """
    intentos = ['"%s" %s La Paz Bolivia café' % (ficha["nombre"], ficha["zona"])]
    calle = calle_de(ficha["direccion"], ficha["zona"])
    if calle and calle not in (ficha["nombre"].lower(),):
        intentos.append('"%s" %s La Paz' % (ficha["nombre"], calle))
    intentos.append('%s La Paz Bolivia' % ficha["nombre"])

    for consulta in intentos:
        resultado = consulta_ddg(consulta)
        if resultado == "CAPTCHA":
            print("      (el buscador pidió captcha, se corta la búsqueda)")
            return None, "CAPTCHA"
        time.sleep(PAUSA_BUSQUEDA)
        if not resultado:
            continue
        for url in resultado[:4]:
            imagen = imagenes_de_la_pagina(url)
            if not imagen:
                continue
            time.sleep(0.6)
            if pedir(imagen, timeout=20):
                return imagen, url
    return None, None


def fase_buscar():
    fichas = construir_fichas(cargar_censo())
    print("Buscando foto propia de %d fichas" % len(fichas))

    if os.path.exists(CACHE_BUSQUEDA):
        with open(CACHE_BUSQUEDA, encoding="utf-8") as f:
            guardadas = json.load(f)
    else:
        guardadas = {}

    os.makedirs(CACHE, exist_ok=True)
    propias, genericas, captcha = 0, 0, False

    for i, ficha in enumerate(fichas, 1):
        clave = ficha["clave"]
        if clave in guardadas:
            propias += 1 if guardadas[clave].get("url") else 0
            genericas += 0 if guardadas[clave].get("url") else 1
            continue
        if captcha:
            guardadas[clave] = {"url": None}
            continue

        url, pagina = buscar_foto_propia(ficha)
        if pagina == "CAPTCHA":
            captcha = True
        guardadas[clave] = {"url": url, "pagina": pagina}
        if url:
            propias += 1
        else:
            genericas += 1

        if i % 10 == 0 or url:
            print("  %3d/%d  propias:%d  sin foto:%d%s"
                  % (i, len(fichas), propias, genericas,
                     "  (detenido por captcha)" if captcha else ""))
            with open(CACHE_BUSQUEDA, "w", encoding="utf-8") as f:
                json.dump(guardadas, f, ensure_ascii=False, indent=1)

    with open(CACHE_BUSQUEDA, "w", encoding="utf-8") as f:
        json.dump(guardadas, f, ensure_ascii=False, indent=1)

    print("Búsqueda terminada: propias %d, sin foto %d" % (propias, genericas))
    if captcha:
        print("AVISO: el buscador cortó con captcha, quedaron sin buscar. "
              "Vuelve a correr 'buscar' más tarde para completarlo.")
    return guardadas


# ---------------------------------------------------------------- ensamblado

def elegir_del_pool(nombre, archivos):
    """Elige foto de relleno de forma estable: el mismo nombre, la misma foto."""
    if not archivos:
        return None
    h = int(hashlib.md5(nombre.encode("utf-8")).hexdigest(), 16)
    return archivos[h % len(archivos)]


def fase_ensamblar():
    fichas = construir_fichas(cargar_censo())
    del_pool_disponibles = archivos_pool()
    creditos = {}
    if os.path.exists(os.path.join(CACHE, "pool_creditos.json")):
        with open(os.path.join(CACHE, "pool_creditos.json"), encoding="utf-8") as f:
            creditos = json.load(f)

    busquedas = {}
    if os.path.exists(CACHE_BUSQUEDA):
        with open(CACHE_BUSQUEDA, encoding="utf-8") as f:
            busquedas = json.load(f)

    for carpeta in (CARPETA_CAFES, CARPETA_MARCAS, CARPETA_TOSTADURIAS):
        os.makedirs(carpeta, exist_ok=True)

    creditos_manifiesto = {}
    origen = {"propia": 0, "generica": 0, "sin_foto": 0}
    faltantes = []

    for ficha in fichas:
        carpeta = {
            "cafes": CARPETA_CAFES,
            "marcas": CARPETA_MARCAS,
            "tostadurias": CARPETA_TOSTADURIAS,
        }[ficha["carpeta"]]
        destino = os.path.join(carpeta, ficha["archivo"])
        registro = busquedas.get(ficha["clave"], {}).get("url")

        if registro:
            crudo = pedir(registro, timeout=30)
            temporal = destino + ".orig"
            ok = False
            if crudo and len(crudo) > 12000:
                with open(temporal, "wb") as f:
                    f.write(crudo)
                ok = recortar(temporal, destino)
            if os.path.exists(temporal):
                os.remove(temporal)
            if ok:
                origen["propia"] += 1
                creditos_manifiesto[ficha["archivo"]] = {
                    "origen": "propia", "url": registro,
                    "pagina": busquedas.get(ficha["clave"], {}).get("pagina", ""),
                }
                continue
            # Se anunció foto pero no se pudo descargar: cae al pool.
            registro = None

        del_pool = elegir_del_pool(ficha["archivo"], del_pool_disponibles)
        if del_pool and recortar(os.path.join(POOL_DIR, del_pool), destino):
            origen["generica"] += 1
            c = creditos.get(del_pool, {})
            creditos_manifiesto[ficha["archivo"]] = {
                "origen": "generica", "pool": del_pool,
                "url": c.get("url", ""), "credito": c.get("autor", ""),
                "licencia": c.get("licencia", ""),
            }
            continue

        origen["sin_foto"] += 1
        faltantes.append(ficha["nombre"])

    manifiesto = {
        "generado": time.strftime("%Y-%m-%d"),
        "tamano": "%dx%d" % TAMANO,
        "nota": ("origen 'propia' = foto de la web del negocio. "
                 "origen 'generica' = foto de café de Wikimedia Commons usada "
                 "como marcador de posición porque no se encontró la del "
                 "negocio. Las 'generica' NO son el local."),
        "resumen": origen,
        "cafes": {}, "marcas": {}, "tostadurias": {},
        "creditos": creditos_manifiesto,
    }

    for ficha in fichas:
        seccion = {
            "cafes": "cafes", "marcas": "marcas", "tostadurias": "tostadurias",
        }[ficha["carpeta"]]
        manifiesto[seccion][ficha["clave"]] = {
            "archivo": ficha["archivo"],
            "nombre": ficha["nombre"],
            "zona": ficha["zona"],
        }

    with open(MANIFIESTO, "w", encoding="utf-8") as f:
        json.dump(manifiesto, f, ensure_ascii=False, indent=1)

    print("Carpetas armadas.")
    print("  propias:   %d" % origen["propia"])
    print("  genéricas: %d" % origen["generica"])
    print("  sin foto:  %d" % origen["sin_foto"])
    if faltantes:
        print("  las que faltaron:", ", ".join(faltantes[:15]))
    return manifiesto


# -------------------------------------------------------------------- main

def main():
    paso = sys.argv[1] if len(sys.argv) > 1 else "todo"
    if paso in ("todo", "pool"):
        bajar_pool()
    if paso in ("todo", "buscar"):
        fase_buscar()
    if paso in ("todo", "ensamblar"):
        fase_ensamblar()
    if paso == "nombres":
        fichas = construir_fichas(cargar_censo())
        print("%d fichas" % len(fichas))
        for f in fichas:
            print("  %-11s %-8s %s" % (f["carpeta"], f["zona"][:8], f["archivo"]))


if __name__ == "__main__":
    main()
