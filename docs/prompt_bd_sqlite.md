# Prompt v2 — Construir ubicafe.db desde la encuesta Excel

Pegar este prompt en Claude (Claude Code) junto con el archivo
`CAFETERÍAS ENCUESTA PARA APP.xlsx`.

---

Eres un ingeniero de datos de la app Android "UbiCafe" (Java + XML, mapa OpenStreetMap, paquete com.ubicafe.app).
La app cataloga cafeterías de especialidad, marcas, tostadurías, productores y puntos de venta de La Paz. Hoy los datos están hardcodeados en DatosEjemplo.java (9 cafeterías de ejemplo). Necesito que rescates los datos REALES del Excel adjunto y construyas una BASE DE DATOS SQLite funcional y lista para consumir.

## 1. ARCHIVO DE ENTRADA (adjunto)
Archivo: CAFETERÍAS ENCUESTA PARA APP.xlsx

- Hoja "MAPA" (la curada, 214 filas = 213 registros). Columnas EXACTAS:
  A = Nombre del emprendimiento · B = Macrodistrito · C = Dirección · D = Latitud · E = Longitud ·
  F = "Rubro (todos)" · G = "Rubro principal" · H = "Revisar ubicación".
  ES LA FUENTE PRINCIPAL para nombre, zona, dirección, coordenadas y categoría.

- Hoja "DATOS LIMPIOS" (214 filas = 213 registros, mismo orden de filas que "MAPA"). Para enriquecer:
  - B = nombre de emprendimiento · C = macrodistrito · D = dirección · E = latitud · F = longitud
  - I = "5. Su negocio está vinculado…" (respuesta texto, ej. "Tengo una cafetería", "Tengo una tostaduría")
  - J, K, L, M = banderas 0/1 por opción de esa pregunta (valida su significado con I)
  - AJ = "2.1 Nombre de la cafetería" · AK = "2.2 Año de apertura" · AL = "2.3 Número de sucursales"
  - AP = "2.7 Tipo de establecimiento" · BC = "2.8 Criterios de cafetería especial" · BU = preparaciones/métodos
  - DD = variedad/café declarado. OJO: puede traer variedad ("Catuai", "Criolla", "Typica y Catuai rojo"),
    proceso ("Natural y lavado", "Semi-lavado"), ruido ("1.0", "Tradicional") o vacío: usa solo valores útiles de variedad.

Lee el Excel con Python (openpyxl/pandas) en la sesión de Claude Code. NUNCA inventes valores: todo dato debe provenir de una celda; lo vacío va como NULL.

## 2. ENTREGABLES
1. `ubicafe.db` — base SQLite poblada (UTF-8, con acentos correctos).
2. `esquema.sql` — el script CREATE TABLE exacto que usaste (reproducible).
3. `datos_descartados.log` — filas excluidas y el motivo (sin GPS, incompleta, duplicada, fuera de La Paz).
4. Resumen final con conteos y 5 ejemplos reales.

## 3. ESQUEMA EXACTO (nombres y tipos OBLIGATORIOS)
```sql
PRAGMA foreign_keys = ON;

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
```
Nombres de tabla/columna EXACTOS. `etiquetas` guárdala como texto JSON de arreglo, p. ej. '["Cafetería","Tostaduría"]'. `tipo` ∈ {"cafeOrigen","marcaNacional","clasica","restaurante","tostaderia","cowork"} (mapea desde AP/I; si no hay dato, "clasica"). `inicial` = primera letra del nombre.

## 4. REGLAS DE CARGA
1. La hoja "MAPA" manda para categoría (col G), zona (col B), dirección (col C) y GPS (cols D, E). Si un registro no está ahí, usa "DATOS LIMPIOS" (I + banderas J–M).
2. Lat/lng SOLO de celdas, dentro de La Paz: -16.6 ≤ lat ≤ -16.35 y -68.25 ≤ lng ≤ -68.0. Fuera de rango, duplicados para direcciones distintas, o con "Revisar ubicación" (col H) = Sí → inserta igual pero con revisar_ubicacion=1 (o NULL si no hay dato); NUNCA inventes coordenadas.
3. Normaliza zona (macrodistrito) a la lista canónica {Centro, Sur, Cotahuma, Max Paredes, Periférica, Mallasa}; corrige variantes (espacios, mayúsculas, "Casco Viejo"→Centro). Si la dirección permite inferir barrio (Sopocachi, Miraflores, San Antonio, Villa Fátima, Obrajes, Achumani, Calacoto…) llénalo en `barrio`; `zona` siempre es macrodistrito canónico.
4. Cadenas reales de La Paz (Alexander, TYPICA, Roaster, Juan Valdez, Rosa negra, Mugen Coffee, etc.): agrupa por nombre normalizado (minúsculas, sin "café"/"coffee"/"coffe" ni acentos) → una fila en `marcas`, y un `cafeterias` o `puntos_de_venta` por cada sede con sus GPS. Liga con `marca_id`. La col AL (nº de sucursales) ayuda a confirmar cadenas.
5. Deduplica: mismo nombre + misma dirección + mismos GPS = 1 registro. Añade la sede al `cafeteria_cafes_origen` cuando la fila declare variedad en DD (deduplica variedad en `cafes_origen` por variedad+origen).
6. Productores: filas cuyo rubro principal (G) o I sea cafetal/finca. Altitud solo si hay dato explícito (col G/H de DATOS LIMPIOS o rango declarado); si no, NULL.
7. Horarios: solo si el Excel los trae explícitamente (en esta encuesta normalmente no hay → NULL). Contacto idem.

## 5. VERIFICACIÓN OBLIGATORIA (ejecuta y muéstrame los resultados)
- `PRAGMA integrity_check;` → debe devolver `ok`.
- `SELECT 'cafeterias', COUNT(*) FROM cafeterias UNION ALL ...` para las 6 tablas.
- Conteos esperados orientativos: cafeterías ~199, tostadurías ~25, marcas ~27, productores ~13, puntos_de_venta ≥6.
- "Cero inventos": cualquier texto que no salga textual del Excel (p. ej. descripciones de marcas) queda explícitamente anotado en el log.
- Muestra: nº por colección, cuántos con revisar_ubicacion=1, cadenas detectadas con su nº de sedes, y 5 filas reales de cafeterías.