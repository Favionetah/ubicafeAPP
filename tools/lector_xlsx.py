"""
LECTOR DE XLSX SIN DEPENDENCIAS
=======================================================================
Lee hojas de cálculo de un .xlsx usando solo la biblioteca estándar de
Python. Evita depender de openpyxl o pandas para poder regenerar el
censo en cualquier máquina.

Un .xlsx es un ZIP con XML dentro. Necesitamos tres cosas:
  xl/workbook.xml              -> nombres y orden de las hojas
  xl/worksheets/sheetN.xml     -> las celdas
  xl/sharedStrings.xml         -> el texto compartido (opcional)

Las celdas se guardan como índices numéricos ("<v>3</v>") apuntando a
sharedStrings, o como texto en línea cuando el archivo lo permite.
Este lector cubre ambos casos.
"""

import re
import zipfile
import xml.etree.ElementTree as ET

NS = "{http://schemas.openxmlformats.org/spreadsheetml/2006/main}"


class LectorXlsx:
    """Lee un .xlsx y expone sus hojas como listas de diccionarios."""

    def __init__(self, ruta):
        self._zip = zipfile.ZipFile(ruta)
        self._compartidas = self._leer_compartidas()
        self._orden_hojas = self._leer_orden_hojas()

    # -- piezas internas ------------------------------------------------

    def _leer_compartidas(self):
        """Devuelve la lista de textos compartidos del libro."""
        if "xl/sharedStrings.xml" not in self._zip.namelist():
            return []
        raiz = ET.fromstring(self._zip.read("xl/sharedStrings.xml"))
        textos = []
        for si in raiz.findall(NS + "si"):
            # Un texto puede estar repartido en varias partes <r><t>.
            textos.append("".join(t.text or "" for t in si.iter(NS + "t")))
        return textos

    def _leer_orden_hojas(self):
        """Devuelve [(nombre_visible, ruta_xml), ...] en el orden del libro."""
        raiz = ET.fromstring(self._zip.read("xl/workbook.xml"))
        relaciones = ET.fromstring(self._zip.read("xl/_rels/workbook.xml.rels"))
        destinos = {r.get("Id"): r.get("Target") for r in relaciones}

        hojas = []
        for hoja in raiz.iter(NS + "sheet"):
            rid = hoja.get(
                "{http://schemas.openxmlformats.org/officeDocument/2006/relationships}id"
            )
            destino = destinos.get(rid, "")
            # El destino puede venir como "worksheets/sheet2.xml" o como
            # "/xl/worksheets/sheet2.xml". Dentro del ZIP siempre es relativo.
            destino = destino.lstrip("/")
            if not destino.startswith("xl/"):
                destino = "xl/" + destino
            hojas.append((hoja.get("name"), destino))
        return hojas

    @staticmethod
    def _letra_a_indice(letra):
        """'A' -> 0, 'B' -> 1, ... 'Z' -> 25, 'AA' -> 26."""
        indice = 0
        for caracter in letra:
            indice = indice * 26 + (ord(caracter) - 64)
        return indice - 1

    def _leer_celda(self, celda):
        """Devuelve el texto de una celda <c>."""
        en_linea = celda.find(NS + "is")
        if en_linea is not None:
            return "".join(t.text or "" for t in en_linea.iter(NS + "t"))
        valor = celda.find(NS + "v")
        if valor is None:
            return ""
        if celda.get("t") == "s":
            return self._compartidas[int(valor.text)]
        return valor.text or ""

    # -- API pública ----------------------------------------------------

    def hojas(self):
        """Lista los nombres de hoja disponibles."""
        return [nombre for nombre, _ in self._orden_hojas]

    def leer(self, nombre_hoja):
        """
        Devuelve (encabezados, filas) de la hoja indicada.

        encabezados: lista de str, en el orden real de columnas.
        filas:       lista de dict {encabezado: texto sin espacios sobrantes}.
                     Si una columna se llama "Col" y dos filas comparten
                     nombre, la segunda sobrescribe a la primera; por eso
                     conviene que los encabezados sean únicos.
        """
        ruta = None
        objetivo = normalizar(nombre_hoja)
        for nombre, destino in self._orden_hojas:
            if normalizar(nombre) == objetivo:
                ruta = destino
                break
        if ruta is None:
            raise KeyError(
                "No existe la hoja %r. Disponibles: %s"
                % (nombre_hoja, ", ".join(self.hojas()))
            )

        raiz = ET.fromstring(self._zip.read(ruta))
        filas_xml = raiz.find(NS + "sheetData").findall(NS + "row")
        if not filas_xml:
            return [], []

        # Índice de columna -> texto, para la fila de encabezados.
        crudos = []
        for fila in filas_xml:
            celdas = {}
            for celda in fila.findall(NS + "c"):
                referencia = celda.get("r", "")
                letra = "".join(c for c in referencia if c.isalpha())
                if letra:
                    celdas[letra] = self._leer_celda(celda)
            crudos.append(celdas)

        if not crudos:
            return [], []

        columnas = sorted(
            {letra for fila in crudos for letra in fila},
            key=LectorXlsx._letra_a_indice,
        )
        encabezados = [crudos[0].get(columna, "") or columna for columna in columnas]

        filas = []
        for cruda in crudos[1:]:
            fila = {}
            for indice, columna in enumerate(columnas):
                valor = (cruda.get(columna) or "").strip()
                clave = encabezados[indice]
                # Choca de nombres: se conserva el primero y se descarta
                # el repetido, que casi siempre es una columna de cálculo.
                if clave not in fila:
                    fila[clave] = valor
            filas.append(fila)
        return encabezados, filas


def normalizar(texto):
    """
    Reduce un texto a una clave comparable: sin mayúsculas, sin
    tildes, sin signos y sin espacios.

    Sirve para emparejar "Café Copacabana", "CAFE COPACABANA" y
    "cafe-copacabana" como la misma entidad. Se usa solo para cruzar
    datos, nunca para mostrar en pantalla: el nombre que ve el usuario
    es siempre el original.
    """
    import unicodedata

    plano = unicodedata.normalize("NFD", (texto or "").lower())
    sin_tildes = "".join(c for c in plano if unicodedata.category(c) != "Mn")
    return re.sub(r"[^a-z0-9]", "", sin_tildes)
