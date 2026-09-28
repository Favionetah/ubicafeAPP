#!/usr/bin/env python3
"""
GENERADOR DE LA BASE DE DATOS DE UBICAFÉ
=======================================================================
Convierte el censo del sector cafetero de La Paz en el archivo que la
app lee: app/src/main/assets/censo_ubicafe.json

FUENTES
  UbiCafe_DatosReales.xlsx        fuente de verdad. Ocho hojas ya
                                   depuradas por quien hizo el censo.
  CAFETERÍAS ENCUESTA PARA APP.xlsx
                                   solo se usa la hoja "MAPA" para
                                   traer la marca de verificación de
                                   ubicación, y la hoja "DATOS LIMPIOS"
                                   para traer la precisión del GPS.

CÓMO SE CRUZAN LAS HOJAS
  Las hojas no comparten un identificador, así que el cruce es por
  nombre normalizado (sin tildes ni mayúsculas) y, cuando el nombre
  aparece repetido porque hay sucursales, se desempata con la
  distancia entre coordenadas. El resultado es 188/188 cafeterías,
  78/78 puntos de venta, 13/13 tostadurías y 12/12 productores.

GARANTÍAS
  El script NO escribe el JSON si algún conteo no cuadra con la hoja
  RESUMEN del propio censo. Un dato incompleto es preferible a un dato
  inventado.

USO
    python3 tools/generar_censo.py
"""

import json
import math
import os
import sys
from collections import Counter, defaultdict

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from lector_xlsx import LectorXlsx, normalizar  # noqa: E402

# --- rutas -------------------------------------------------------------

AQUI = os.path.dirname(os.path.abspath(__file__))
RAIZ = os.path.dirname(AQUI)
CENSO = os.path.join(RAIZ, "UbiCafe_DatosReales.xlsx")
ENCUESTA = os.path.join(RAIZ, "CAFETERÍAS ENCUESTA PARA APP.xlsx")
DESTINO = os.path.join(RAIZ, "app", "src", "main", "assets", "censo_ubicafe.json")
REPORTE = os.path.join(AQUI, "REPORTE_LIMPIEZA.md")

# --- valores que no son información real -------------------------------

# El censo tiene bastantes marcadores de "no hay dato". No son
# respuestas, son ausencia de respuesta, y la app debe ocultarlos igual
# que un campo vacío. Se recorren a vacío.
AUSENTES = {
    "", "-", "--", "n/a", "na", "nan", "null", "none", "no sabe", "nosabe",
    "no tiene", "ninguno", "sin dato", "sindato", "n/a.", "#n/a",
    "no respondio", "0", "0.0", "0,0", "false", "null",
}

# Respuestas afirmativas que el censo usa con distinta grafía.
VERDADEROS = {"si", "sí", "s", "yes", "1", "true", "x"}
FALSOS = {"no", "n", "0", "false", ""}

# Erratas del censo que corregimos para que la ficha se lea bien.
CORRECCIONES = {
    "coffe arabica": "Coffea arabica",
    "coffe arábica": "Coffea arabica",
    "catuaí rojo": "Catuaí rojo",
}

# Los roles tal como aparecen en la columna "Roles" del censo.
MAPA_ROLES = {
    "cafeteria": "CAFETERIA",
    "marca": "MARCA",
    "tostaduria": "TOSTADURIA",
    "productor": "PRODUCTOR",
    "tienda": "TIENDA",
    "otro": "OTRO",
}

# Conteos que deben cumplirse exactamente. Vienen de la hoja RESUMEN
# del mismo censo, que es la fuente de verdad declarada del trabajo.
ESPERADO = {
    "entidades": 213,
    "marcas": 40,
    "sucursales": 78,
    "tostadurias": 13,
    "productores": 12,
    "variedades": 66,
    "coordenadas_completas": 213,
    "roles": {
        "CAFETERIA": 189, "MARCA": 25, "OTRO": 17,
        "TOSTADURIA": 13, "PRODUCTOR": 12, "TIENDA": 6,
    },
    "rol_principal": {
        "CAFETERIA": 188, "OTRO": 17, "TIENDA": 4, "MARCA": 2, "TOSTADURIA": 2,
    },
    "macrodistritos": {
        "Centro": 121, "Sur": 49, "Cotahuma": 29,
        "Max Paredes": 9, "Periferica": 4, "Mallasa": 1,
    },
}

# Registro de todo lo que se tocó, para escribir el reporte.
CAMBIOS = []


def anotar(categoria, original, nuevo, donde):
    CAMBIOS.append({
        "categoria": categoria,
        "original": original,
        "nuevo": nuevo,
        "donde": donde,
    })


# --- limpieza de valores ----------------------------------------------

def limpiar(valor, donde=""):
    """
    Devuelve el valor listo para la app, o "" si el censo no tiene dato.

    Colapsa espacios y descarta los marcadores de ausencia de dato.
    """
    if valor is None:
        return ""
    texto = str(valor).strip()
    if not texto:
        return ""
    colapsado = " ".join(texto.split())
    clave = normalizar(colapsado)
    if clave in AUSENTES or colapsado in AUSENTES:
        anotar("campo sin dato real", colapsado, "(oculto)", donde)
        return ""
    for incorrecto, correcto in CORRECCIONES.items():
        if clave == normalizar(incorrecto):
            anotar("errata del censo", colapsado, correcto, donde)
            return correcto
    return colapsado


def limpiar_si_no(texto):
    """Como limpiar(), pero sin registrar el cambio en el reporte.

    Se usa en los 213 campos de conteo, donde los vacíos son la norma
    y anotarlos uno por uno solo produciría ruido.
    """
    if texto is None:
        return ""
    colapsado = " ".join(str(texto).split())
    if normalizar(colapsado) in AUSENTES or colapsado in AUSENTES:
        return ""
    return colapsado


def es_url(texto):
    return texto.lower().startswith(("http://", "https://"))


def numero(valor):
    """Convierte a float tolerando '25.0', '25,0' y 'no sabe'. Devuelve None."""
    texto = limpiar_si_no(valor)
    if not texto:
        return None
    try:
        return float(texto.replace(",", "."))
    except ValueError:
        return None


def entero(valor):
    cantidad = numero(valor)
    return int(cantidad) if cantidad is not None else None


def si_no(texto):
    """Normaliza una respuesta Sí/No del censo a 'Sí' o 'No'. '' si no aplica."""
    limpio = limpiar_si_no(texto)
    if not limpio:
        return ""
    clave = normalizar(limpio)
    if clave in {normalizar(v) for v in VERDADEROS}:
        return "Sí"
    if clave in {normalizar(v) for v in FALSOS}:
        return "No"
    return limpio


def km_entre(lat1, lng1, lat2, lng2):
    """Distancia aproximada en kilómetros. Suficiente para desempatar."""
    if None in (lat1, lng1, lat2, lng2):
        return float("inf")
    lat_med = math.radians((lat1 + lat2) / 2.0)
    return math.hypot((lat1 - lat2) * 111.0, (lng1 - lng2) * 111.0 * math.cos(lat_med))


# --- cruce de hojas ----------------------------------------------------

def emparejar(filas, columna_nombre, base, base_ya_usada, etiqueta):
    """
    Cruza una hoja de detalle contra la hoja base (CLASIFICACIÓN).

    Busca, en este orden:
      1. Mismo nombre normalizado a menos de 50 m de distancia.
      2. Solo por coordenadas, a menos de 30 m.

    La segunda vía rescata los casos en que el negocio puso el nombre de
    la cadena ("Bolivian Coffee") donde el censo puso el nombre local
    ("Bolivian Cofee"), o donde hay una errata ("Veirut" por "Beirut").
    Las coordenadas son únicas en el censo, así que el emparejamiento es
    fiable.

    Devuelve (pares, sin_cruce) donde pares mapea un índice de la fila
    de detalle al índice de la fila base.
    """
    pares = {}
    sin_cruce = []
    for indice, fila in enumerate(filas):
        nombre = normalizar(fila.get(columna_nombre, ""))
        lat, lng = numero(fila.get("Lat")), numero(fila.get("Lng"))

        mejor = None
        for i, candidata in enumerate(base):
            if i in base_ya_usada:
                continue
            if normalizar(candidata.get("Nombre", "")) != nombre:
                continue
            distancia = km_entre(
                numero(candidata.get("Lat")), numero(candidata.get("Lng")), lat, lng
            )
            if mejor is None or distancia < mejor[0]:
                mejor = (distancia, i)

        if mejor is None or mejor[0] > 0.05:
            por_coordenada = None
            for i, candidata in enumerate(base):
                if i in base_ya_usada:
                    continue
                distancia = km_entre(
                    numero(candidata.get("Lat")),
                    numero(candidata.get("Lng")),
                    lat, lng,
                )
                if distancia <= 0.03 and (por_coordenada is None or distancia < por_coordenada[0]):
                    por_coordenada = (distancia, i)
            if por_coordenada is not None:
                mejor = por_coordenada
                anotar(
                    "cruce por coordenadas",
                    fila.get(columna_nombre, ""),
                    base[mejor[1]].get("Nombre", ""),
                    etiqueta,
                )

        if mejor is not None and mejor[0] <= 0.05:
            base_ya_usada.add(mejor[1])
            pares[indice] = mejor[1]
        else:
            sin_cruce.append(fila)
    return pares, sin_cruce


# --- construcción ------------------------------------------------------

def construir():
    libro = LectorXlsx(CENSO)
    _, clasificacion = libro.leer("CLASIFICACION")
    _, cafeterias = libro.leer("CAFETERIAS")
    _, marcas = libro.leer("MARCAS")
    _, puntos = libro.leer("PUNTOS DE VENTA")
    _, tostadurias = libro.leer("TOSTADERÍAS")
    _, productores = libro.leer("PRODUCTORES")
    _, cafes_origen = libro.leer("CAFÉS DE ORIGEN")

    # -- ubicaciones dudosas, desde el otro Excel ----------------------
    # La hoja MAPA tiene exactamente las mismas 213 filas que
    # CLASIFICACION, en el mismo orden y con las mismas coordenadas, así
    # que se cruza por posición y no por nombre. Trae el motivo por el
    # que hay que revisar la ubicación ("precisión 2000m", "signo
    # corregido"), que es más útil que un simple sí o no.
    nota_ubicacion = {}
    precision = {}
    try:
        otra = LectorXlsx(ENCUESTA)
        _, mapa = otra.leer("MAPA")
        _, datos = otra.leer("DATOS LIMPIOS")
        if len(mapa) != len(clasificacion) or len(datos) != len(clasificacion):
            raise ValueError(
                "MAPA tiene %d filas y DATOS LIMPIOS %d; se esperaban %d"
                % (len(mapa), len(datos), len(clasificacion))
            )
        for indice, fila in enumerate(mapa):
            nota_ubicacion[indice] = limpiar_si_no(fila.get("Revisar ubicación", ""))
        for indice, fila in enumerate(datos):
            precision[indice] = numero(
                fila.get("_4. Coordenadas geográficas_precision")
            )
    except (KeyError, FileNotFoundError, ValueError) as error:
        print("Aviso: no se pudo leer el Excel de encuesta (%s)." % error)
        print("      Se genera el JSON sin los avisos de verificación de ubicación.")
        nota_ubicacion = {}
        precision = {}

    # -- 1. Entidades: la columna vertebral ---------------------------
    entidades = []
    for indice, fila in enumerate(clasificacion):
        nombre = limpiar(fila.get("Nombre", ""), "entidad %d" % indice)
        direccion = limpiar(fila.get("Dirección", ""), "entidad %s" % nombre)
        mapa_url = ""
        if es_url(direccion):
            mapa_url = direccion
            anotar("dirección que era un enlace", direccion, "(se conserva como mapaUrl)", nombre)
            direccion = ""

        lat, lng = numero(fila.get("Lat")), numero(fila.get("Lng"))

        roles = []
        for parte in fila.get("Roles", "").split(","):
            rol = MAPA_ROLES.get(normalizar(parte))
            if rol and rol not in roles:
                roles.append(rol)
        if not roles:
            roles = ["OTRO"]
            anotar("entidad sin rol declarado", nombre, "OTRO", "entidades")

        principal = MAPA_ROLES.get(
            normalizar(fila.get("Clasificación principal", "")), "OTRO"
        )
        if principal not in roles:
            roles.append(principal)

        # El orden de los roles fija el orden en que se muestran en la
        # ficha, así que se reordena a un orden estable y legible.
        orden = ["CAFETERIA", "MARCA", "TOSTADURIA", "PRODUCTOR", "TIENDA", "OTRO"]
        roles.sort(key=orden.index)

        entidades.append({
            "id": "ent-%03d" % (indice + 1),
            "nombre": nombre,
            "direccion": direccion,
            "mapaUrl": mapa_url,
            "macrodistrito": limpiar(fila.get("Macrodistrito", ""), nombre),
            "lat": lat if lat is not None else 0.0,
            "lng": lng if lng is not None else 0.0,
            "roles": roles,
            "rolPrincipal": principal,
            "marcaAsociada": limpiar(fila.get("Marca asociada", ""), nombre),
            "nota": limpiar(fila.get("Nota / motivo / fuente", ""), nombre),
            "precisionGps": precision.get(indice),
            "notaUbicacion": nota_ubicacion.get(indice, ""),
            "detalleCafeteria": None,
            "detalleMarca": None,
            "detalleTostaderia": None,
            "detalleProductor": None,
        })

    por_nombre = {e["id"]: e for e in entidades}

    # -- 2. Detalles por rol ------------------------------------------
    usados = set()
    pares, sin_cruce = emparejar(cafeterias, "Nombre", clasificacion, usados, "detalleCafeteria")
    for indice, base in pares.items():
        fila = cafeterias[indice]
        entidad = por_nombre["ent-%03d" % (base + 1)]
        direccion = limpiar(fila.get("Dirección", ""), entidad["nombre"])
        mapa_url = ""
        if es_url(direccion):
            mapa_url = direccion
            direccion = ""
        entidad["detalleCafeteria"] = {
            "anioApertura": limpiar(fila.get("Año de apertura", ""), entidad["nombre"]),
            "mesas": limpiar(fila.get("Mesas", ""), entidad["nombre"]),
            "capacidad": limpiar(fila.get("Capacidad (personas)", ""), entidad["nombre"]),
            "baristas": limpiar(fila.get("Baristas", ""), entidad["nombre"]),
            "tipoEstablecimiento": limpiar(fila.get("Tipo de establecimiento", ""), entidad["nombre"]),
            "tipoApp": limpiar(fila.get("Tipo (app)", ""), entidad["nombre"]),
            "direccion": direccion,
            "mapaUrl": mapa_url,
        }

    usados = set()
    pares, sin_cruce_t = emparejar(tostadurias, "Nombre", clasificacion, usados, "detalleTostaderia")
    for indice, base in pares.items():
        fila = tostadurias[indice]
        entidad = por_nombre["ent-%03d" % (base + 1)]
        direccion = limpiar(fila.get("Dirección", ""), entidad["nombre"])
        mapa_url = ""
        if es_url(direccion):
            mapa_url = direccion
            direccion = ""
        entidad["detalleTostaderia"] = {
            "anioInicio": limpiar(fila.get("Año inicio", ""), entidad["nombre"]),
            "kgTostadosMes": limpiar(fila.get("Kg tostados/mes", ""), entidad["nombre"]),
            "compraCafe": si_no(fila.get("Compra café", "")),
            "regionesOrigen": limpiar(fila.get("Regiones de origen", ""), entidad["nombre"]),
            "tiposTueste": limpiar(fila.get("Tipos de tueste", ""), entidad["nombre"]),
            "direccion": direccion,
            "mapaUrl": mapa_url,
        }

    usados = set()
    pares, sin_cruce_p = emparejar(productores, "Nombre", clasificacion, usados, "detalleProductor")
    for indice, base in pares.items():
        fila = productores[indice]
        entidad = por_nombre["ent-%03d" % (base + 1)]
        direccion = limpiar(fila.get("Dirección", ""), entidad["nombre"])
        mapa_url = ""
        if es_url(direccion):
            mapa_url = direccion
            direccion = ""
        entidad["detalleProductor"] = {
            "comunidadMunicipio": limpiar(fila.get("Comunidad / municipio", ""), entidad["nombre"]),
            "aniosProduciendo": limpiar(fila.get("Años produciendo café", ""), entidad["nombre"]),
            "especies": limpiar(fila.get("Especies", ""), entidad["nombre"]),
            "variedades": limpiar(fila.get("Variedades", ""), entidad["nombre"]),
            "produccionUltimaCosecha": limpiar(fila.get("Producción última cosecha", ""), entidad["nombre"]),
            "notasCata": limpiar(fila.get("Notas de cata", ""), entidad["nombre"]),
            "vendeA": limpiar(fila.get("¿A quién vende?", ""), entidad["nombre"]),
            "direccion": direccion,
            "mapaUrl": mapa_url,
        }

    # -- 3. Marcas y sus sucursales -----------------------------------
    indice_por_id = {e["id"]: e for e in entidades}

    catalogo_marcas = []
    clave_marca = {}
    for fila in marcas:
        nombre = limpiar(fila.get("Marca", ""), "marca")
        clave = normalizar(nombre)
        clave_marca[clave] = nombre
        catalogo_marcas.append({
            "nombre": nombre,
            "esNacional": si_no(fila.get("¿Es nacional?", "")),
            "anioCreacion": limpiar(fila.get("Año creación", ""), nombre),
            "lugarCreacion": limpiar(fila.get("Lugar de creación", ""), nombre),
            "registradaSenapi": limpiar(fila.get("Registrada SENAPI", ""), nombre),
            "segmento": limpiar(fila.get("Segmento", ""), nombre),
            "nProductos": limpiar(fila.get("N. productos", ""), nombre),
            "municipioOrigen": limpiar(fila.get("Municipio/región de origen", ""), nombre),
            "canalComercializacion": limpiar(fila.get("Canal de comercialización", ""), nombre),
            "cobertura": limpiar(fila.get("Cobertura", ""), nombre),
            "nota": limpiar(fila.get("Nota", ""), nombre),
            "sucursales": [],
        })

    # El local de un punto de venta es una sucursal concreta, con su
    # propia dirección y sus propias coordenadas. Como las cadenas
    # repiten nombre ("Alexander Coffe" aparece en tres calles
    # distintas), no basta con buscar por nombre: hay que asignar cada
    # punto a la entidad libre más cercana, como en las demás hojas.
    usados = set()
    pares_pdv, sin_cruce_pdv = emparejar(
        puntos, "Local", clasificacion, usados, "punto de venta"
    )

    for indice, fila in enumerate(puntos):
        marca = limpiar(fila.get("Marca", ""), "punto de venta")
        local = limpiar(fila.get("Local", ""), "punto de venta")
        direccion = limpiar(fila.get("Dirección", ""), local)
        mapa_url = ""
        if es_url(direccion):
            mapa_url = direccion
            direccion = ""
        lat, lng = numero(fila.get("Lat")), numero(fila.get("Lng"))

        entidad_id = ""
        if indice in pares_pdv:
            entidad_id = indice_por_id["ent-%03d" % (pares_pdv[indice] + 1)]["id"]

        sucursal = {
            "entidadId": entidad_id,
            "nombre": local,
            "macrodistrito": limpiar(fila.get("Barrio / Macrodistrito", ""), local),
            "direccion": direccion,
            "mapaUrl": mapa_url,
            "lat": lat if lat is not None else 0.0,
            "lng": lng if lng is not None else 0.0,
        }

        clave = normalizar(marca)
        encontrada = next(
            (m for m in catalogo_marcas if normalizar(m["nombre"]) == clave), None
        )
        if encontrada is None:
            # Tres marcas de la hoja MARCAS no son cadenas sino fincas o
            # lotes concretos. Se conservan como marca propia para no
            # perder el punto de venta.
            encontrada = {
                "nombre": marca, "esNacional": "", "anioCreacion": "",
                "lugarCreacion": "", "registradaSenapi": "", "segmento": "",
                "nProductos": "", "municipioOrigen": "",
                "canalComercializacion": "", "cobertura": "",
                "nota": "Aparece en el censo solo como local, sin ficha de marca.",
                "sucursales": [],
            }
            catalogo_marcas.append(encontrada)
            anotar("marca sin ficha", marca, "creada desde el punto de venta", "marcas")
        encontrada["sucursales"].append(sucursal)

    # El detalle de marca de una entidad, cuando esa entidad pertenece a
    # una cadena con ficha completa.
    for entidad in entidades:
        clave = normalizar(entidad["marcaAsociada"])
        if not clave:
            continue
        marca = next((m for m in catalogo_marcas if normalizar(m["nombre"]) == clave), None)
        if marca is None:
            continue
        detalle = {
            campo: marca[campo] for campo in (
                "esNacional", "anioCreacion", "lugarCreacion", "registradaSenapi",
                "segmento", "nProductos", "municipioOrigen",
                "canalComercializacion", "cobertura",
            )
        }
        if any(detalle.values()):
            entidad["detalleMarca"] = detalle

    # -- 4. Cafés de origen -------------------------------------------
    variedades = []
    for fila in cafes_origen:
        nombre = limpiar(fila.get("Café de origen / variedad", ""), "variedad")
        if not nombre:
            continue
        marca_rel = limpiar(fila.get("Marca relacionada", ""), nombre)
        variedades.append({
            "nombre": nombre,
            "variedadesDeclaradas": limpiar(fila.get("Variedades declaradas", ""), nombre),
            "region": limpiar(fila.get("Región / comunidad", ""), nombre),
            "marca": clave_marca.get(normalizar(marca_rel), marca_rel),
            "contexto": limpiar(fila.get("Contexto", ""), nombre),
        })

    documento = {
        "meta": {
            "fuente": "Censo del sector cafetero de La Paz",
            "anio": 2026,
            "totalRegistros": len(entidades),
            "coordenadasCompletas": sum(
                1 for e in entidades if e["lat"] or e["lng"]
            ),
            "sinPrecio": True,
        },
        "entidades": entidades,
        "marcas": catalogo_marcas,
        "variedades": variedades,
    }
    return documento, {
        "detalleCafeteria": sin_cruce,
        "detalleTostaderia": sin_cruce_t,
        "detalleProductor": sin_cruce_p,
        "punto de venta": sin_cruce_pdv,
    }


# --- validación --------------------------------------------------------

def validar(documento, sin_cruces):
    errores = []
    entidades = documento["entidades"]
    marcas = documento["marcas"]

    def comparar(etiqueta, obtenido, esperado):
        if obtenido != esperado:
            errores.append("%s: esperado %s, obtenido %s" % (etiqueta, esperado, obtenido))

    comparar("entidades", len(entidades), ESPERADO["entidades"])
    comparar(
        "entidades con coordenadas",
        sum(1 for e in entidades if e["lat"] or e["lng"]),
        ESPERADO["coordenadas_completas"],
    )
    # El JSON no lleva ya los roles duplicados en la hoja: "Tostaduría,
    # Cafetería" y "Cafetería, Tostaduría" son la misma entidad.
    total_sucursales = sum(len(m["sucursales"]) for m in marcas)
    comparar("marcas", len(marcas), ESPERADO["marcas"])
    comparar("puntos de venta", total_sucursales, ESPERADO["sucursales"])
    comparar("variedades", len(documento["variedades"]), ESPERADO["variedades"])
    comparar(
        "entidades con detalleCafeteria",
        sum(1 for e in entidades if e["detalleCafeteria"]),
        188,
    )
    comparar(
        "entidades con detalleTostaderia",
        sum(1 for e in entidades if e["detalleTostaderia"]),
        ESPERADO["tostadurias"],
    )
    comparar(
        "entidades con detalleProductor",
        sum(1 for e in entidades if e["detalleProductor"]),
        ESPERADO["productores"],
    )

    roles = Counter(r for e in entidades for r in e["roles"])
    for rol, esperado in ESPERADO["roles"].items():
        comparar("rol %s" % rol, roles.get(rol, 0), esperado)

    principales = Counter(e["rolPrincipal"] for e in entidades)
    for rol, esperado in ESPERADO["rol_principal"].items():
        comparar("rol principal %s" % rol, principales.get(rol, 0), esperado)

    macro = Counter(normalizar(e["macrodistrito"]) for e in entidades)
    for nombre, esperado in ESPERADO["macrodistritos"].items():
        comparar("macrodistrito %s" % nombre, macro.get(normalizar(nombre), 0), esperado)

    # Ningún identificador puede repetirse.
    ids = [e["id"] for e in entidades]
    if len(set(ids)) != len(ids):
        errores.append("hay identificadores de entidad repetidos")

    # Todo punto de venta debe apuntar a una entidad existente.
    validas = set(ids)
    huerfanos = [
        s for m in marcas for s in m["sucursales"] if s["entidadId"] not in validas
    ]
    if huerfanos:
        errores.append("%d puntos de venta sin entidad asociada" % len(huerfanos))

    # Ninguna entidad puede quedarse sin nombre ni coordenadas.
    sin_nombre = [e for e in entidades if not e["nombre"]]
    if sin_nombre:
        errores.append("%d entidades sin nombre" % len(sin_nombre))
    sin_coordenada = [e for e in entidades if not e["lat"] and not e["lng"]]
    if sin_coordenada:
        errores.append("%d entidades sin coordenadas" % len(sin_coordenada))

    # Una entidad no puede ser dos sucursales de la misma cadena a la
    # vez: si lo fuera, el mapa mostraría el mismo punto repetido en
    # lugar de dos locales en calles distintas.
    duplicadas = Counter(
        s["entidadId"] for m in marcas for s in m["sucursales"] if s["entidadId"]
    )
    repetidas = [k for k, v in duplicadas.items() if v > 1]
    if repetidas:
        errores.append(
            "%d entidades figuran como sucursal repetida de la misma cadena: %s"
            % (len(repetidas), ", ".join(repetidas[:6]))
        )

    # Cada punto de venta debe tener coordenadas propias para poder
    # dibujarse en el mapa de la marca.
    sin_coordenada_pdv = [
        s for m in marcas for s in m["sucursales"] if not s["lat"] and not s["lng"]
    ]
    if sin_coordenada_pdv:
        errores.append("%d puntos de venta sin coordenadas" % len(sin_coordenada_pdv))

    # Ninguna hoja de detalle puede quedar con filas sin cruzar: si
    # sobrara alguna, la app mostraría una ficha sin su entidad o una
    # entidad sin su ficha, y ambas cosas son datos perdidos.
    for etiqueta, filas in sin_cruces.items():
        if filas:
            errores.append(
                "%d filas de %s sin cruce: %s"
                % (len(filas), etiqueta,
                   ", ".join(f.get("Nombre", f.get("Local", "?")) for f in filas[:6]))
            )
    return errores


# --- reporte -----------------------------------------------------------

def escribir_reporte(documento, errores, sin_cruces):
    entidades = documento["entidades"]
    marcas = documento["marcas"]
    sucursales = [s for m in marcas for s in m["sucursales"]]
    lineas = []
    escribir = lineas.append

    escribir("# Reporte de limpieza del censo\n")
    escribir("Generado por `tools/generar_censo.py`. No editar a mano: el JSON se")
    escribir("reproduce con `python3 tools/generar_censo.py`.\n")

    escribir("## Resultado de la validación\n")
    if errores:
        escribir("**El JSON NO se generó porque hubo errores:**\n")
        for error in errores:
            escribir("- %s" % error)
    else:
        escribir("Todos los conteos coinciden con la hoja RESUMEN del censo y no")
        escribir("queda ninguna fila de detalle sin Entidad a la que pertenecer.\n")

    escribir("\n## Conteos\n")
    escribir("| Qué | Cuántos |")
    escribir("|---|---|")
    escribir("| Entidades en el mapa | %d |" % len(entidades))
    escribir("| Con ficha de cafetería | %d |"
             % sum(1 for e in entidades if e["detalleCafeteria"]))
    escribir("| Con ficha de tostaduría | %d |"
             % sum(1 for e in entidades if e["detalleTostaderia"]))
    escribir("| Con ficha de productor | %d |"
             % sum(1 for e in entidades if e["detalleProductor"]))
    escribir("| Con ficha de marca | %d |"
             % sum(1 for e in entidades if e["detalleMarca"]))
    escribir("| Marcas en el catálogo | %d |" % len(marcas))
    escribir("| Puntos de venta | %d |" % len(sucursales))
    escribir("| Cafés de origen | %d |" % len(documento["variedades"]))
    escribir("| Entidades de una sola ficha | %d |"
             % sum(1 for e in entidades if sum(
                 1 for d in (e["detalleCafeteria"], e["detalleMarca"],
                             e["detalleTostaderia"], e["detalleProductor"]) if d) == 1))
    escribir("| Entidades con dos o más fichas | %d |"
             % sum(1 for e in entidades if sum(
                 1 for d in (e["detalleCafeteria"], e["detalleMarca"],
                             e["detalleTostaderia"], e["detalleProductor"]) if d) > 1))
    escribir("| Direcciones ausentes | %d |"
             % sum(1 for e in entidades if not e["direccion"]))
    escribir("| Ubicaciones con aviso del censo | %d |"
             % sum(1 for e in entidades if e["notaUbicacion"]))
    escribir("| Puntos de venta sin entidad | %d |"
             % sum(1 for s in sucursales if not s["entidadId"]))

    escribir("\n## Cómo se cruzaron las hojas\n")
    escribir("Las hojas del censo no comparten identificador, así que el cruce es")
    escribir("por nombre normalizado y, cuando el nombre se repite porque hay")
    escribir("sucursales de una misma cadena, se desempata con la distancia entre")
    escribir("coordenadas, de modo que cada sucursal conserva su propia entidad.\n")
    escribir("| Hoja de detalle | Cruzadas |")
    escribir("|---|---|")
    for etiqueta, filas in sin_cruces.items():
        total = {"detalleCafeteria": 188, "detalleTostaderia": 13,
                 "detalleProductor": 12, "punto de venta": 78}[etiqueta]
        escribir("| %s | %d de %d |" % (etiqueta, total - len(filas), total))

    resumen = Counter(c["categoria"] for c in CAMBIOS)
    escribir("\n## Cambios aplicados\n")
    if not resumen:
        escribir("Ninguno.\n")
    for categoria, veces in resumen.most_common():
        escribir("### %s (%d)\n" % (categoria, veces))
        vistos = set()
        mostrados = 0
        for cambio in CAMBIOS:
            if cambio["categoria"] != categoria:
                continue
            firma = (cambio["original"], cambio["nuevo"])
            if firma in vistos:
                continue
            vistos.add(firma)
            escribir("- `%s` → `%s`%s"
                     % (cambio["original"] or "(vacío)", cambio["nuevo"],
                        ("  — %s" % cambio["donde"]) if cambio["donde"] else ""))
            mostrados += 1
            if mostrados >= 12:
                escribir("- _…y %d casos más_" % (veces - mostrados))
                break
        escribir("")

    avisos = Counter(e["notaUbicacion"] for e in entidades if e["notaUbicacion"])
    escribir("\n## Avisos de ubicación que dejó el propio censo\n")
    escribir("El censo marcó %d ubicaciones para revisión. La app las dibuja en el"
             % sum(avisos.values()))
    escribir("mapa igual que las demás, pero conserva el motivo en `notaUbicacion`.\n")
    for motivo, veces in avisos.most_common():
        escribir("- `%s` (%d)" % (motivo, veces))

    escribir("\n## Decisiones que conviene revisar\n")
    escribir("1. **No hay precios en el censo.** Ninguna de las dos hojas tiene una")
    escribir("   columna de precio, así que el JSON no lleva ninguno y la app no")
    escribir("   muestra bloque de precio. `meta.sinPrecio` lo deja explícito.")
    escribir("2. **La columna de altitud del Excel no se usó.** Es la altitud del GPS,")
    escribir("   o sea la altura de La Paz donde está el local, no la del cafetal.")
    escribir("3. **Los nombres se guardan tal cual los escribió el negocio.** No se")
    escribir("   corrigieron mayúsculas ni minúsculas para no inventar cómo se")
    escribir("   escribe cada marca. La búsqueda ignora mayúsculas y tildes.")
    escribir("4. **Casi todas las fichas de marca están vacías.** De los 40 campos")
    escribir("   del catálogo, solo `esNacional` está completo en las 40 marcas; el")
    escribir("   canal de comercialización y la cobertura, en 4. La ficha de marca")
    escribir("   tiene que poder mostrarse casi vacía sin romperse.")
    escribir("5. **La ficha de cafetería solo tiene datos duros en 66 de 188.** Las")
    escribir("   188 tienen `tipoApp`, pero año de apertura, mesas, capacidad y")
    escribir("   baristas solo están en 66. La hoja RESUMEN dice 69; la diferencia")
    escribir("   son tres encuestas marcadas como completadas con los campos en blanco.")

    with open(REPORTE, "w", encoding="utf-8") as archivo:
        archivo.write("\n".join(lineas) + "\n")


# --- main --------------------------------------------------------------

def main():
    if not os.path.exists(CENSO):
        print("No se encuentra el censo en %s" % CENSO)
        return 1

    documento, sin_cruces = construir()
    errores = validar(documento, sin_cruces)
    escribir_reporte(documento, errores, sin_cruces)

    if errores:
        print("El JSON NO se generó. La validación encontró %d problemas:" % len(errores))
        for error in errores:
            print("  - %s" % error)
        print("\nDetalles en %s" % REPORTE)
        return 1

    os.makedirs(os.path.dirname(DESTINO), exist_ok=True)
    with open(DESTINO, "w", encoding="utf-8") as archivo:
        json.dump(documento, archivo, ensure_ascii=False, separators=(",", ":"))

    peso = os.path.getsize(DESTINO) / 1024.0
    print("censo_ubicafe.json escrito: %d entidades, %d marcas, %d variedades (%.0f KB)"
          % (len(documento["entidades"]), len(documento["marcas"]),
             len(documento["variedades"]), peso))
    print("Validación correcta. Reporte en %s" % REPORTE)
    return 0


if __name__ == "__main__":
    sys.exit(main())
