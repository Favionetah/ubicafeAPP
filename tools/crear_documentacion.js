// Genera la documentación técnica ampliada de UbiCafe (.docx).
// Uso:  node tools/crear_documentacion.js   (requiere el paquete 'docx' instalado)
const fs = require("fs");
const path = require("path");
const {
  Document, Packer, Paragraph, TextRun, Table, TableRow, TableCell,
  Footer, AlignmentType, LevelFormat, HeadingLevel,
  BorderStyle, WidthType, ShadingType, VerticalAlign, PageNumber, PageBreak,
  TableOfContents,
} = require("docx");

const RAIZ = path.dirname(path.dirname(__filename));
const SALIDA = path.join(RAIZ, "docs", "Documentacion_Tecnica_UbiCafe.docx");
const VERDE = "164E3B";
const VERDE2 = "1C4E3B";
const TENUE = "78817D";

// ---------- estilos ----------
const estilos = {
  default: { document: { run: { font: "Arial", size: 22, color: "26332E" } } },
  paragraphStyles: [
    { id: "Title", name: "Title", basedOn: "Normal",
      run: { size: 52, bold: true, color: VERDE, font: "Arial" },
      paragraph: { spacing: { before: 240, after: 200 }, alignment: AlignmentType.CENTER } },
    { id: "Heading1", name: "Heading 1", basedOn: "Normal", next: "Normal", quickFormat: true,
      run: { size: 32, bold: true, color: VERDE, font: "Arial" },
      paragraph: { spacing: { before: 285, after: 137 }, outlineLevel: 0 } },
    { id: "Heading2", name: "Heading 2", basedOn: "Normal", next: "Normal", quickFormat: true,
      run: { size: 28, bold: true, color: VERDE2, font: "Arial" },
      paragraph: { spacing: { before: 204, after: 103 }, outlineLevel: 1 } },
    { id: "Subtitulo", name: "Subtitulo", basedOn: "Normal",
      run: { size: 24, italics: true, color: TENUE },
      paragraph: { alignment: AlignmentType.CENTER, spacing: { after: 240 } } },
  ],
};

const numeracion = {
  config: [
    { reference: "viñetas", levels: [
      { level: 0, format: LevelFormat.BULLET, text: "•", alignment: AlignmentType.LEFT,
        style: { paragraph: { indent: { left: 600, hanging: 280 } } } }] },
    { reference: "pasos", levels: [
      { level: 0, format: LevelFormat.DECIMAL, text: "%1.", alignment: AlignmentType.LEFT,
        style: { paragraph: { indent: { left: 600, hanging: 320 } } } }] },
  ],
};

// ---------- helpers ----------
const P = (t, opts = {}) => new Paragraph({
  spacing: { after: 108, line: 280 }, ...opts, children: [new TextRun(t)],
});

const PAr = (t, opts = {}) => new Paragraph({
  spacing: { after: 108, line: 280 }, ...opts,
  children: t.split("**").map((parte, i) =>
    new TextRun({ text: parte, bold: i % 2 === 1 })),
});

const H1 = (t) => new Paragraph({ heading: HeadingLevel.HEADING_1, children: [new TextRun(t)] });
const H2 = (t) => new Paragraph({ heading: HeadingLevel.HEADING_2, children: [new TextRun(t)] });

const borde = { style: BorderStyle.SINGLE, size: 1, color: "CCCCCC" };
const celdasBorde = { top: borde, bottom: borde, left: borde, right: borde };
const sombra = { fill: "F4F1E8", type: ShadingType.CLEAR };

function codigo(lineas) {
  const parrafos = lineas.map((l) => new Paragraph({
    spacing: { after: 0, line: 240 },
    children: [new TextRun({ text: l, font: "Consolas", size: 19, color: "1F2A26" })],
  }));
  return new Table({
    columnWidths: [9360],
    margins: { top: 140, bottom: 140, left: 180, right: 180 },
    rows: [new TableRow({ children: [new TableCell({
      borders: celdasBorde, width: { size: 9360, type: WidthType.DXA }, shading: sombra,
      children: parrafos,
    })] })],
  });
}

function tabla(anchuras, cabecera, filas) {
  const celdas = (valores, anchoFila) => valores.map((v, i) =>
    new TableCell({
      borders: celdasBorde, width: { size: anchuras[i], type: WidthType.DXA },
      shading: anchoFila ? { fill: "E4EADF", type: ShadingType.CLEAR } : undefined,
      verticalAlign: VerticalAlign.CENTER,
      children: [new Paragraph({ children: [new TextRun({ text: v, bold: !!anchoFila })] })],
    }));
  const filasTabla = [new TableRow({ tableHeader: true, children: celdas(cabecera, true) })];
  filas.forEach((f) => filasTabla.push(new TableRow({ children: celdas(f, false) })));
  return new Table({
    columnWidths: anchuras,
    margins: { top: 80, bottom: 80, left: 150, right: 150 },
    rows: filasTabla,
  });
}

const espacio = () => new Paragraph({ spacing: { after: 120 }, children: [new TextRun("")] });

const Bul = (t, opts = {}) => new Paragraph({
  spacing: { after: 96, line: 276 }, ...opts,
  numbering: { reference: "viñetas", level: 0 },
  children: t.split("**").map((parte, i) => new TextRun({ text: parte, bold: i % 2 === 1 })),
});

const Paso = (t, opts = {}) => new Paragraph({
  spacing: { after: 108, line: 280 }, ...opts,
  numbering: { reference: "pasos", level: 0 },
  children: [new TextRun(t)],
});

const pie = new Footer({
  children: [new Paragraph({
    alignment: AlignmentType.CENTER,
    children: [
      new TextRun("UbiCafe · Documentación técnica · "),
      new TextRun({ children: [PageNumber.CURRENT] }), new TextRun(" de "),
      new TextRun({ children: [PageNumber.TOTAL_PAGES] }),
    ],
  })],
});

// ---------- contenido ----------
const contenido = [];

// Portada
contenido.push(
  new Paragraph({ spacing: { before: 1200 }, alignment: AlignmentType.CENTER,
    children: [new TextRun({ text: "UBICAFÉ", bold: true, size: 72, color: VERDE })] }),
  new Paragraph({ alignment: AlignmentType.CENTER,
    children: [new TextRun({ text: "Café de La Paz", size: 30, color: TENUE })] }),
  new Paragraph({ spacing: { before: 360 }, alignment: AlignmentType.CENTER,
    children: [new TextRun({ text: "Documentación técnica del desarrollo de la aplicación", bold: true, size: 40 })] }),
  new Paragraph({ alignment: AlignmentType.CENTER,
    children: [new TextRun({ text: "Android (Java + XML) · OpenStreetMap · SQLite", size: 30, color: VERDE2, bold: true })] }),
  new Paragraph({ spacing: { before: 480 }, style: "Subtitulo",
    children: [new TextRun("Pasos del proyecto, arquitectura, datos, código relevante, navegación e integración del mapa, con apéndices de referencia")] }),
  new Paragraph({ spacing: { before: 40 }, style: "Subtitulo",
    children: [new TextRun("27 de septiembre de 2026")] }),
  new Paragraph({ spacing: { before: 320 }, alignment: AlignmentType.CENTER,
    children: [new TextRun({ text: "Versión 2 · Edición ampliada (19 secciones y 4 apéndices)", size: 22, color: TENUE })] }),
  new Paragraph({ children: [new PageBreak()] }),
);

// Índice
contenido.push(
  H1("Índice"),
  new TableOfContents("Contenido", { hyperlink: true, headingStyleRange: "1-2" }),
  new Paragraph({ children: [new PageBreak()] }),
);

// 1. Presentación
contenido.push(
  H1("1. Presentación del proyecto"),
  PAr("**UbiCafe** es una aplicación Android que da a conocer el **ecosistema del café de La Paz**: cafeterías, marcas de café nacional, tostadurías, productores y puntos de venta. Cada lugar está censado con sus **coordenadas reales** (latitud y longitud) y se muestra tanto en listas y tarjetas como sobre un **mapa real**."),
  PAr("El proyecto no se limitó a la interfaz: incluye un **pipeline de datos** que convierte un censo (hojas de cálculo) en una **base de datos SQLite** normalizada, lista para alimentar la app en el futuro. Esta documentación explica todo el recorrido, desde el censo hasta la pantalla final."),

  H2("1.1 Para quién es este documento"),
  Bul("**Docentes y evaluadores:** para verificar que cada pantalla cumple la fase correspondiente y que el código sigue buenas prácticas."),
  Bul("**Estudiantes:** para entender, en lenguaje directo y con el código real del proyecto, cómo funciona cada pieza."),
  Bul("**Futuros mantenedores:** para localizar archivos, dependencias y decisiones de diseño en minutos."),

  H2("1.2 Objetivos de la aplicación"),
  Bul("Mostrar dónde tomar buen café en La Paz usando un mapa real e información censada."),
  Bul("Permitir **buscar** cafés de especialidad, marcas y cafeterías, sin importar acentos o mayúsculas."),
  Bul("Agrupar el ecosistema en categorías (cafeterías, marcas, tostadurías, productores, puntos de venta) y en macrodistritos."),
  Bul("Servir de base académica: arquitectura limpia, un único punto de acceso a datos y extensibilidad hacia una API o base de datos."),

  H2("1.3 Módulos y pantallas"),
  tabla([3300, 1600, 4460], ["Pantalla", "Fase", "Función"],
    [
      ["Splash / Onboarding", "P1–P2", "Carga inicial y presentación del producto (carrusel)."],
      ["Inicio", "P3", "Acceso a categorías, «Cerca de ti», macrodistritos y banner."],
      ["Mapa (pestaña y completo)", "P4", "OpenStreetMap con marcadores filtrables y detalle."],
      ["Explorar", "P5", "Variedades paceñas, regiones cafeteras y guía del café."],
      ["Listas y detalles", "P6–P8", "Cafeterías, marcas, tostadurías y productores."],
      ["Ficha de origen", "P9", "Ficha técnica y notas sensoriales de un café de especialidad."],
      ["Búsqueda global", "P10", "Resultados en tres secciones mientras se escribe."],
      ["Puntos de venta / Ecosistema", "P12", "Distribución por marca y cifras del sector."],
    ]),
);

// 2. Tecnologías
contenido.push(
  H1("2. Tecnologías y herramientas"),
  PAr("La app usa **Java 17** con vistas clásicas (**View + XML**, sin Compose), un enfoque estable, didáctico y con la mayor documentación del ecosistema Android. Las versiones mínima y objetivo son **minSdk 24** (Android 7.0) y **targetSdk 37**."),

  H2("2.1 Configuración del módulo (app/build.gradle)"),
  codigo([
    "android {",
    "    compileSdk 37",
    "    buildFeatures { viewBinding = true }",
    "",
    "    defaultConfig {",
    "        applicationId \"com.ubicafe.app\"",
    "        minSdk 24",
    "        targetSdk 37",
    "        versionCode 1",
    "    }",
    "}",
  ]),
  PAr("**ViewBinding** activado: cada layout genera una clase de enlace (por ejemplo `ActivityMainBinding`) que permite acceder a las vistas de forma segura y con autocompletado."),

  H2("2.2 Dependencias y su papel"),
  tabla([4300, 5060], ["Librería", "Por qué está en el proyecto"],
    [
      ["androidx.appcompat:appcompat", "Activitys compatibles hacia atrás y temas Material (AppCompatActivity)."],
      ["com.google.android.material:material", "BottomNavigationView, Button con esquinas y tema MaterialComponents."],
      ["androidx.constraintlayout:constraintlayout", "Layouts flexibles para las tarjetas del Inicio."],
      ["androidx.recyclerview:recyclerview", "Listas eficientes: cafeterías, marcas, tostadurías, productores."],
      ["androidx.cardview:cardview", "Tarjetas redondeadas con elevación (tarjetas de lista y del mapa)."],
      ["androidx.viewpager2:viewpager2", "Carrusel deslizable del Onboarding."],
      ["org.osmdroid:osmdroid-android:6.1.20", "Mapa real de OpenStreetMap sin API key."],
    ]),

  H2("2.3 Tema, colores y tipografía"),
  PAr("El tema hereda de `Theme.MaterialComponents.Light.NoActionBar`. Define la identidad visual de la app:"),
  codigo([
    "<!-- res/values/themes.xml -->",
    "<style name=\"TemaUbiCafe\" parent=\"Theme.MaterialComponents.Light.NoActionBar\">",
    "    <item name=\"colorPrimary\">@color/verde_oscuro</item>",
    "    <item name=\"colorSecondary\">@color/cafe_accent</item>",
    "    <item name=\"android:colorBackground\">@color/fondo_crema</item>",
    "    <item name=\"android:fontFamily\">@font/nunito_sans_family</item>",
    "    <item name=\"android:windowLightStatusBar\">true</item>",
    "</style>",
  ]),
  PAr("La paleta central, definida en `res/values/colors.xml`, se reutiliza en todas las pantallas y en los pines del mapa:"),
  tabla([2600, 4200, 2560], ["Color", "Hex", "Uso"],
    [
      ["verde_oscuro / verde_claro", "#164E3B / #1C4E3B", "Títulos, botones y marca principal."],
      ["fondo_crema / fondo_blanco", "#F8F4EA / #FFFFFF", "Fondo general y tarjetas."],
      ["texto_principal / texto_secundario", "#26332E / #4E5A55", "Textos y subtítulos."],
      ["texto_tenue", "#78817D", "Notas menores y subtítulos de cabecera."],
      ["cafe_accent / caramelo_claro", "#76513D / #DDA15E", "Acentos cafeteros y pines de puntos de venta."],
      ["verde_medio", "#6B8F71", "Pines de productores en el mapa."],
      ["azul_ubicacion", "#2E6DB4", "Pin de la ubicación del usuario."],
      ["divisor / verde_tenue", "#E0E1DC / #164E3B10", "Separadores y fondos suaves."],
    ]),
  PAr("La tipografía es **Nunito** (títulos) y **Nunito Sans** (texto), empaquetada como fuentes locales en `res/font/` con sus familias XML (`nunito_family.xml`, `nunito_sans_family.xml`) para los pesos: regular, medium, semi_bold, bold, extra_bold y black."),
);

// 3. Arquitectura
contenido.push(
  H1("3. Arquitectura de la aplicación"),
  PAr("El código se organiza en **tres capas** separadas por paquetes. La regla de oro es que **ninguna pantalla sabe cómo se guardan los datos**: siempre pregunta a `RepositorioDatos`."),

  H2("3.1 Capas y paquetes"),
  tabla([1560, 3900, 3900], ["Paquete", "Contenido", "Responsabilidad"],
    [
      ["modelo", "Cafeteria, MarcaCafe, CafeOrigen, Productor, PuntoVenta, Tostaderia y ResultadoBusqueda", "Clases de datos puras: campos finales + constantes de tipo."],
      ["datos", "RepositorioDatos, DatosEjemplo", "Único acceso a los datos; hoy datos locales, mañana SQLite o API."],
      ["ui", "Activities, Fragments, adaptadores y util/UiUtils", "Presentación. Nunca lee DatosEjemplo directamente."],
    ]),

  H2("3.2 El punto único de acceso a los datos"),
  PAr("`RepositorioDatos` expone métodos estáticos por entidad. Cada método delega hoy en `DatosEjemplo`, pero su contrato está pensado para cambiarse sin tocar las pantallas:"),
  codigo([
    "// datos/RepositorioDatos.java",
    "public class RepositorioDatos {",
    "    public static List<Cafeteria> obtenerCafeterias() {",
    "        return DatosEjemplo.obtenerCafeterias();  // → SQLiteOpenHelper o API",
    "    }",
    "    public static List<Cafeteria> obtenerCafeteriasPorZona(String zona)",
    "    public static List<Cafeteria> obtenerCafeteriasPorTipo(String tipo)",
    "    public static Cafeteria obtenerCafeteria(String nombre)",
    "    public static MarcaCafe obtenerMarca(String nombre)",
    "    public static int contarPuntosDeVentaDe(String nombreMarca)",
    "    public static ResultadoBusqueda buscar(String texto)",
    "}",
  ]),
  PAr("El propio comentario de cabecera de la clase lo resume: **«Las pantallas NUNCA leen DatosEjemplo directamente»**. Esta decisión es la que permite migrar a una base de datos real sin reescribir la interfaz."),

  H2("3.3 Modelos de datos"),
  PAr("Cada modelo agrupa sus campos públicos finales e inmutables, con constantes para los valores cerrados. Los principales:"),
  tabla([2600, 6760], ["Clase", "Campos clave y uso"],
    [
      ["Cafeteria", "nombre, zona, direccion, horario, tipo (CAFE_ORIGEN/MARCA_NACIONAL/CLASICA), marca (o null), fichaOrigen, lat, lng."],
      ["MarcaCafe", "nombre, etiquetas (Cafetería/Tostaduría/Productor), esNacional, descripcion, cafésOrigen, colorMarca, inicial."],
      ["CafeOrigen", "variedad (GEISHA…), origen, altitud, proceso, tostado, aroma, notasCata."],
      ["Productor", "nombreFinca, nombreFamilia, origen, numCafes, altitud, descripcion, lat, lng."],
      ["Tostaderia", "nombre, region (La Paz/Yungas/Tarija), direccion, horarios, contacto, lat, lng."],
      ["PuntoVenta", "marca, local, barrio, direccion, lat, lng."],
    ]),
  PAr("Incluido en palabras del propio `Cafeteria`: «lat/lng: coordenadas para el mapa (futuro Google Maps)». Hoy son datos reales del censo y ya alimentan a OpenStreetMap."),

  H2("3.4 Objetos auxiliares de la capa de presentación"),
  PAr("No todo es negocio: dos clases de apoyo cargan datos en vuelo."),
  Bul("**ResultadoBusqueda** (modelo): tres listas (cafés, marcas, establecimientos) y `estaVacio()` que consulta las tres."),
  Bul("**MarcadorMapa** (ui/mapa): lo que se dibuja sobre el mapa — nombre visible, zona, coordenadas, `tipo`, color y `nombreDetalle`, que decide qué pantalla abre «Ver información»."),
);

// 4. Del censo a la app
contenido.push(
  H1("4. Los datos: del censo a la app"),
  PAr("Toda la información que muestra UbiCafe proviene de un **censo real** del café en La Paz, recogido en hojas de cálculo. El reto fue convertir esas hojas en una base de datos limpia y auditable."),

  H2("4.1 Fuentes del dato"),
  Bul("Hoja de **encuesta**: registros de cafeterías, marcas, tostadurías, productores y puntos de venta."),
  Bul("Hoja **mapa**: coordenadas (lat/lng) asociadas a cada local, junto con un flag para revisar la posición."),

  H2("4.2 Pipeline de normalización"),
  PAr("`tools/generar_bd.py` lee las hojas y produce en un solo paso la base y los informes de auditoría:"),
  tabla([3100, 6260], ["Paso", "Qué hace"],
    [
      ["1. Lectura", "Abre el censo (hojas de cálculo) y arma los registros en Python."],
      ["2. Normalización", "Cada clave de entidad se recorre una vez, la variedad «Típica» pasa a «Typica», se deduplican marcas y se calculan los vínculos cafeteria-café."],
      ["3. Carga", "Inserta todo en `ubicafe.db` aplicando el esquema relacional."],
      ["4. Auditoría", "Escribe `datos_descartados.log` con los registros omitidos o incompletos."],
      ["5. Verificación", "Revisa integridad referencial y conteos por tabla."],
    ]),

  H2("4.3 Esquema SQLite"),
  tabla([3400, 5960], ["Tabla", "Contenido y relación"],
    [
      ["cafes_origen", "Fichas de variedad: variedad, origen, altitud, proceso, tueste, aroma, descripción."],
      ["marcas", "nombre único, etiquetas, es_nacional, descripción, inicial."],
      ["cafeterias", "Local con zona, barrio, dirección, horario, tipo, marca_id (FK) y lat/lng."],
      ["cafeteria_cafes_origen", "Vínculo N:N entre cafeterías y cafés (PK compuesta)."],
      ["tostadurias / productores", "Cada uno con sus campos de perfil y coordenadas."],
      ["puntos_de_venta", "marca (texto), local, barrio y coordenadas."],
      ["revisar_ubicacion", "Flag en cafeterías/tostadurías para las 32 posiciones a validar."],
    ]),
  PAr("El **esquema completo** se reproduce en el Apéndice A, listo para usarse con `SQLiteOpenHelper` o cualquier ORM ligero."),

  H2("4.4 Control de calidad"),
  Bul("Conteos verificados por tabla: 206 cafeterías, 29 marcas, 3 tostadurías, 14 productores, 55 puntos de venta, 5 cafés, 17 vínculos."),
  Bul("32 cafeterías marcadas en `revisar_ubicacion` por tener coordenadas dudosas o faltantes."),
  Bul("`datos_descartados.log` documenta cada omisión para auditar después."),
  PAr("Así, la base es **la fuente de verdad** del censo y el puente natural hacia la integración real en la app."),
);

// 5. Flujo de pantallas
contenido.push(
  H1("5. Flujo de pantallas (navegación)"),
  PAr("La navegación mezcla dos estilos coherentes entre sí: una **secuencia de arranque** fija y un **árbol basado en Intents explícitos** con extras, más **pestañas** dentro de la MainActivity."),

  H2("5.1 Secuencia de arranque"),
  PAr("`SplashActivity` espera **1,5 segundos** con un `Handler` (sin hilos peligrosos) y abre `OnboardingActivity`. Ese carrusel nunca se salta por ahora: se decide entrar al final."),
  codigo([
    "// SplashActivity.java",
    "new Handler(Looper.getMainLooper()).postDelayed(() -> {",
    "    startActivity(new Intent(this, OnboardingActivity.class));",
    "    finish();",
    "}, 1500);   // 1,5 segundos de carga",
  ]),

  H2("5.2 Pestañas principales"),
  PAr("`MainActivity` monta las **tres pestañas** mediante un `BottomNavigationView`, definido en `res/menu/menu_principal.xml` (iconos `ic_house`, `ic_map_pin`, `ic_database_search`), y reemplaza un fragmento en un único contenedor:"),
  codigo([
    "<!-- res/menu/menu_principal.xml -->",
    "<menu xmlns:android=\"http://schemas.android.com/apk/res/android\">",
    "    <item android:id=\"@+id/menu_inicio\"     android:icon=\"@drawable/ic_house\"",
    "          android:title=\"@string/tab_inicio\" />",
    "    <item android:id=\"@+id/menu_mapa\"       android:icon=\"@drawable/ic_map_pin\"",
    "          android:title=\"@string/tab_mapa\" />",
    "    <item android:id=\"@+id/menu_explorar\"   android:icon=\"@drawable/ic_database_search\"",
    "          android:title=\"@string/tab_explorar\" />",
    "</menu>",
  ]),
  codigo([
    "// MainActivity.java",
    "navegacion.setOnItemSelectedListener(item -> {",
    "    Fragment destino = seleccionarFragmento(item.getItemId());",
    "    cambiarFragmento(destino);   // replace(R.id.contenedor_fragmentos, destino)",
    "    return true;",
    "});",
  ]),

  H2("5.3 Árbol completo de navegación"),
  PAr("El resto usa **Intents explícitos con extras**. La tabla resume cada salto y el dato que transporta:"),
  tabla([2500, 3700, 3160], ["Origen", "Destino", "Cómo se pasa"],
    [
      ["Inicio (tile/banner)", "ListaCafeterias / Marcas / Tostaderias / Productores / PuntosVenta", "Sin extras (categoría fija)."],
      ["Inicio (chip macrodistrito)", "ListaCafeteriasActivity", "EXTRA_ZONA = \"Sur\", \"Centro\"…"],
      ["Inicio (Cerca de ti)", "DetalleCafeteriaActivity", "EXTRA_NOMBRE_CAFETERIA"],
      ["ListaCafeterias (fila)", "DetalleCafeteriaActivity", "EXTRA_NOMBRE_CAFETERIA"],
      ["DetalleCafeteria (Cómo llegar)", "MapaActivity", "EXTRA_NOMBRE_CAFETERIA (centra el mapa)"],
      ["DetalleMarca (Ver en mapa)", "MapaActivity", "EXTRA_NOMBRE_MARCA (puntos de venta)"],
      ["DetalleMarca (café de especialidad)", "FichaOrigenActivity", "EXTRA_VARIEDAD + EXTRA_NOMBRE_MARCA"],
      ["Explorar (variedad)", "FichaOrigenActivity", "EXTRA_VARIEDAD + EXTRA_NOMBRE_MARCA"],
      ["PuntosVenta (Ver en mapa)", "MapaActivity", "EXTRA_NOMBRE_MARCA de la marca elegida"],
      ["Pestaña Mapa (Ver información)", "Detalle según el tipo del marcador", "Extra del lugar según MarcadorMapa.tipo"],
      ["Búsqueda (resultado)", "DetalleMarca / DetalleCafeteria / FichaOrigen", "Extra del lugar elegido"],
    ]),

  H2("5.4 Contratos entre pantallas"),
  PAr("Cada destino declara sus extras como **constantes públicas** para no acoplar pantallas entre sí. Quien abre, escribe la clave; quien recibe, la lee con `getIntent().getStringExtra(...)`:"),
  tabla([3300, 3200, 2860], ["Constante", "Clase que la declara", "Valor (clave)"],
    [
      ["EXTRA_NOMBRE_CAFETERIA", "DetalleCafeteriaActivity", "\"nombre_cafeteria\""],
      ["EXTRA_ZONA", "ListaCafeteriasActivity", "\"zona_inicial\""],
      ["EXTRA_NOMBRE_MARCA", "DetalleMarcaActivity", "\"nombre_marca\""],
      ["EXTRA_VARIEDAD / EXTRA_NOMBRE_MARCA", "FichaOrigenActivity", "\"variedad\" / \"nombre_marca\""],
      ["EXTRA_NOMBRE_TOSTADERIA / EXTRA_NOMBRE_FINCA", "DetalleTostaderia / DetalleProductor", "Nombre del registro"],
    ]),
  PAr("Patrón repetido en todos los detalles: **recibir el extra → buscar el objeto en el repositorio → si no existe, cerrar**. Esto impide abrir una pantalla con un identificador inválido."),

  H2("5.5 Ciclo de vida y estado"),
  PAr("Los fragmentos no conservan estado entre visitas: `MainActivity` crea uno nuevo en cada cambio de pestaña y cada fragmento **recalcula todo en `onViewCreated`**. Ventaja: el estado nunca queda desincronizado; coste: se reelabora al volver a una pestaña. Es la decisión correcta mientras los datos sean locales."),
);

// 6. Código relevante explicado
contenido.push(
  H1("6. Código relevante explicado"),
  PAr("Esta sección recorre el código pantalla por pantalla, con los fragmentos que mejor explican cada decisión."),

  H2("6.1 Onboarding (ViewPager2 + indicador por código)"),
  PAr("`OnboardingActivity` configura el adaptador y registra un callback de página. El texto del botón cambia entre «Siguiente» y «Comenzar», y los **puntos indicadores se dibujan por código** (no en XML), creciendo el activo a una píldora de 20 dp:"),
  codigo([
    "// OnboardingActivity.java",
    "private void avanzar(ViewPager2 pager) {",
    "    if (adaptador.esUltima(pager.getCurrentItem())) { entrar(); }",
    "    else { pager.setCurrentItem(pager.getCurrentItem() + 1, true); }",
    "}",
    "private void entrar() {",
    "    startActivity(new Intent(this, MainActivity.class));",
    "    finish();",
    "}",
    "// onPageSelected → actualizarIndicador(posicion) usa dp(20) vs dp(6).",
  ]),
  PAr("El adaptador `OnboardingAdapter` usa la interfaz más simple: un `ViewPager2` con `RecyclerView.Adapter` clásico. Cada `Pagina` guarda título y descripción de recursos de texto."),

  H2("6.2 Inicio: una pantalla que se arma sola"),
  PAr("`InicioFragment.onViewCreated` delega en cinco preparadores en orden: categorías, búsqueda, «Cerca de ti», macrodistritos y banner. La tarjeta «Cerca de ti» usa la **primera cafetería del repositorio** y oculta la tarjeta si no hay datos:"),
  codigo([
    "// InicioFragment.java",
    "prepararCategorias(vista);  prepararBusqueda(vista);",
    "prepararCercaDeTi(vista);   prepararMacrodistritos(vista);",
    "prepararBannerProductores(vista);",
    "",
    "private void prepararCercaDeTi(View vista) {",
    "    Cafeteria destacada = RepositorioDatos.obtenerCafeterias().isEmpty()",
    "            ? null : RepositorioDatos.obtenerCafeterias().get(0);",
    "    if (destacada == null) {",
    "        vista.findViewById(R.id.card_cerca_de_ti).setVisibility(View.GONE);",
    "        return;",
    "    }",
    "    ((TextView) vista.findViewById(R.id.texto_nombre_cerca)).setText(destacada.nombre);",
    "}",
  ]),

  H2("6.3 Lista de cafeterías: RecyclerView + chips + búsqueda"),
  PAr("`ListaCafeteriasActivity` combina tres ideas (chips de macrodistrito, buscador y estado vacío) en un único método de filtrado reutilizado en cada evento:"),
  codigo([
    "// ListaCafeteriasActivity.java",
    "private void aplicarFiltros() {",
    "    String zona = zonaSeleccionada.isEmpty()",
    "            || zonaSeleccionada.equals(getString(R.string.cafeterias_filtro_cerca))",
    "            ? \"\" : zonaSeleccionada;",
    "    String texto = campo.getText().toString().toLowerCase().trim();",
    "    List<Cafeteria> resultado = new ArrayList<>();",
    "    for (Cafeteria c : RepositorioDatos.obtenerCafeteriasPorZona(zona)) {",
    "        if (c.nombre.toLowerCase().contains(texto)) resultado.add(c);",
    "    }",
    "    adaptador.actualizar(resultado);",
    "    mostrarVacio(resultado.isEmpty());",
    "}",
  ]),
  PAr("`AdaptadorCafeteria` infla `item_cafeteria` y muestra **nombre de marca si existe, si no el nombre del local**; al tocar, abre el detalle pasando solo el nombre. El buscador dispara `aplicarFiltros()` con un `TextWatcher`."),

  H2("6.4 Detalle de cafetería: patrón guarda y «Cómo llegar»"),
  PAr("`DetalleCafeteriaActivity` lee el extra y **se cierra sola** si el registro no existe. Su botón «Cómo llegar» abre `MapaActivity` reutilizando la misma constante:"),
  codigo([
    "// DetalleCafeteriaActivity.java",
    "String nombre = getIntent().getStringExtra(EXTRA_NOMBRE_CAFETERIA);",
    "Cafeteria cafeteria = RepositorioDatos.obtenerCafeteria(nombre);",
    "if (cafeteria == null) { finish(); return; }   // guarda contra ids inválidos",
    "",
    "findViewById(R.id.btn_como_llegar).setOnClickListener(v -> {",
    "    Intent intento = new Intent(this, MapaActivity.class);",
    "    intento.putExtra(EXTRA_NOMBRE_CAFETERIA, cafeteria.nombre);",
    "    startActivity(intento);",
    "});",
  ]),

  H2("6.5 Detalle de marca: etiquetas, cafés y puntos de venta"),
  PAr("`DetalleMarcaActivity` pinta las **etiquetas** (Cafetería · Tostaduría · Productor) como chips creados por código, infla las tarjetas de **cafés de origen**, y calcula **cuántos puntos de venta** tiene sumando los `PuntoVenta` censados más las cafeterías de la misma marca:"),
  codigo([
    "int puntos = RepositorioDatos.contarPuntosDeVentaDe(marca.nombre);",
    "int cafeterias = 0;",
    "for (Cafeteria c : RepositorioDatos.obtenerCafeterias()) {",
    "    if (marca.nombre.equalsIgnoreCase(c.marca)) cafeterias++;",
    "}",
    "int total = puntos + cafeterias;",
  ]),

  H2("6.6 Ficha de origen y notas como chips"),
  PAr("`FichaOrigenActivity` muestra los campos técnicos como pares etiqueta/valor y convierte el campo de **notas sensoriales en chips**, separando el texto por comas, punto y coma o « y »:"),
  codigo([
    "for (String nota : aroma.split(\"[,;]| y |\\\\s+\")) {",
    "    if (nota.trim().isEmpty()) continue;",
    "    TextView chip = new TextView(this);",
    "    chip.setText(nota.trim());",
    "    chip.setTextSize(12);",
    "    chip.setBackgroundResource(R.drawable.fondo_badge_zona);",
    "    fila.addView(chip);",
    "}",
  ]),

  H2("6.7 Búsqueda global sin acentos"),
  PAr("`RepositorioDatos.buscar()` recorre cafés, marcas y cafeterías comparando texto **normalizado a NFD**: quita acentos y pasa a minúsculas, de modo que «pyp» encuentra «TYPICA» y «típica» también:"),
  codigo([
    "private static String normalizar(String texto) {",
    "    return java.text.Normalizer.normalize(texto, Normalizer.Form.NFD)",
    "            .replaceAll(\"\\\\p{M}\", \"\").toLowerCase(Locale.getDefault());",
    "}",
  ]),
  PAr("`BusquedaActivity` pinta los resultados en tres secciones; cada sección se oculta si queda vacía y muestra su contador «… (n)»:"),
  codigo([
    "private void llenarSeccion(LinearLayout lista, TextView titulo, int cantidad) {",
    "    boolean hay = cantidad > 0;",
    "    lista.setVisibility(hay ? View.VISIBLE : View.GONE);",
    "    titulo.setVisibility(hay ? View.VISIBLE : View.GONE);",
    "    titulo.setText(getString(R.string.seccion_conteo,",
    "            getString((Integer) titulo.getTag()), cantidad));",
    "}",
  ]),

  H2("6.8 Puntos de venta: selección con detalle lateral"),
  PAr("`PuntosVentaActivity` filtra **las marcas que tienen puntos de venta**, permite elegir una y muestra su detalle. Reutiliza `getQuantityString` para el plural correcto («1 punto de venta» / «n puntos de venta»):"),
  codigo([
    "for (MarcaCafe marca : RepositorioDatos.obtenerMarcas()) {",
    "    if (RepositorioDatos.contarPuntosDeVentaDe(marca.nombre) > 0) {",
    "        marcas.add(marca);",
    "    }",
    "}",
    "...",
    "getResources().getQuantityString(R.plurals.unidad_puntos_venta, n, n)",
  ]),

  H2("6.9 Ecosistema: cifras y barras de proporción"),
  PAr("`EcosistemaFragment` rellena cuatro tarjetas con los `contar*()` del repositorio y dibuja la distribución por macrodistrito como **barras de progreso proporcionales** al máximo:"),
  codigo([
    "// EcosistemaFragment.java",
    "Map<String, Integer> conteo = new LinkedHashMap<>();",
    "for (Cafeteria c : RepositorioDatos.obtenerCafeterias()) {",
    "    conteo.put(c.zona, conteo.getOrDefault(c.zona, 0) + 1);",
    "}",
    "int maximo = 1; for (int n : conteo.values()) maximo = Math.max(maximo, n);",
    "// ...en cada fila: barra.setProgress(n * 100 / maximo)",
  ]),

  H2("6.10 Utilidades de interfaz (UiUtils)"),
  PAr("Los chips pill (alto 34 dp, texto 13 sp) se crean una sola vez y comparten aspecto: `UiUtils.crearChip()` construye el `TextView` y `marcarChipSeleccionado()` alterna entre relleno verde y blanco. Esto evita duplicar la lógica en listas y en el mapa."),
);

// 7. Pantallas a fondo: layouts
contenido.push(
  H1("7. Pantallas a fondo: layouts y recursos"),
  PAr("La interfaz es **XML + estilos reutilizables**. Tres piezas merecen explicación por su uso transversal."),

  H2("7.1 Cabecera reutilizable (barra_titulo.xml)"),
  PAr("Todas las pantallas de detalle y listas comparten una cabecera: botón circular blanco de **40 dp** con chevron y dos textos. Se incluye con `<include layout=\"@layout/barra_titulo\" />`:"),
  codigo([
    "<!-- res/layout/barra_titulo.xml -->",
    "<LinearLayout ... android:layout_height=\"72dp\">",
    "    <ImageButton android:id=\"@+id/btn_volver\"",
    "        android:layout_width=\"40dp\" android:layout_height=\"40dp\"",
    "        android:background=\"@drawable/fondo_circulo_blanco\"",
    "        android:src=\"@drawable/ic_chevron_left\"",
    "        app:tint=\"@color/texto_principal\" />",
    "    <TextView android:id=\"@+id/texto_titulo\" style=\"@style/TituloHeader\" />",
    "    <TextView android:id=\"@+id/texto_subtitulo\" style=\"@style/SubtituloHeader\" />",
    "</LinearLayout>",
  ]),

  H2("7.2 El mapa como vista (fragment_mapa.xml y activity_mapa.xml)"),
  PAr("El `MapView` de osmdroid es una vista más: se declara con su espacio de nombres y tres atributos clave (`tilesource`, `zoom`, `multiTouchControls`). En la pestaña se coloca bajo una fila de filtros y sobre un panel inferior:"),
  codigo([
    "<!-- fragment_mapa.xml (resumido) -->",
    "<HorizontalScrollView>",
    "    <LinearLayout android:id=\"@+id/fila_filtros\" android:orientation=\"horizontal\" />",
    "</HorizontalScrollView>",
    "<FrameLayout android:layout_weight=\"1\">",
    "    <org.osmdroid.views.MapView",
    "        android:id=\"@+id/mapa_vista\"",
    "        android:layout_width=\"match_parent\" android:layout_height=\"match_parent\"",
    "        osmdroid:tilesource=\"MAPNIK\" osmdroid:zoom=\"13\"",
    "        osmdroid:multiTouchControls=\"true\" />",
    "    <LinearLayout ... android:layout_gravity=\"bottom\" >",
    "        <!-- nombre, tipo, dirección, rating y botón Ver información -->",
    "    </LinearLayout>",
    "</FrameLayout>",
  ]),
  PAr("`activity_mapa.xml` reutiliza `barra_titulo` y una **CardView** inferior (radio 18 dp) para el punto elegido. Los dos layouts demuestran que el mismo componente se adapta a pestaña y a pantalla completa."),

  H2("7.3 Tarjeta de lista (item_cafeteria.xml)"),
  PAr("`item_cafeteria` es una CardView de 12 dp con foto placeholder (130 dp), nombre Nunito Extra Bold, badge de zona y fila distancia/rating; los `tools:text` previsualizan el diseño en el editor."),

  H2("7.4 Tarjeta de café de especialidad (item_cafe_origen.xml)"),
  PAr("Se reutiliza en `DetalleMarcaActivity` y en `ExplorarFragment`: variedad verde Extra Bold, origen y aroma en tonos tenues, y un chevron que lleva a la Ficha de Origen."),

  H2("7.5 Estilos y fuentes"),
  tabla([3200, 3300, 2860], ["Estilo", "Base", "Uso"],
    [
      ["TituloHeader", "Nunito 800 · 24sp · verde", "Títulos de pantalla (cabeceras y títulos de sección)."],
      ["SubtituloHeader", "Nunito Sans 500 · 13sp · tenue", "Subtítulos y ubicaciones."],
      ["BadgeZona", "Nunito Sans Bold · 12sp", "Chapa de zona sobre las tarjetas."],
      ["FiltroChip", "pill 34dp · 13sp", "Filtros de lista y del mapa (color lo aplica UiUtils)."],
      ["BotonVerde / BotonVerde12 / BotonVerdePill", "Material button", "Acciones: esquinas 8, 12 y píldora."],
    ]),
);

// 8. Mapas
contenido.push(
  H1("8. Mapas: integración con OpenStreetMap (osmdroid)"),
  PAr("El mapa es el módulo estrella (P4). Se eligió **OpenStreetMap** a través de **osmdroid** frente a Google Maps por una razón práctica: sin API key (los permisos se reducen a INTERNET), sin costes de facturación y con datos abiertos."),

  H2("8.1 Configuración (ConfiguracionMapa.java)"),
  PAr("`inicializar()` carga las preferencias propias de osmdroid en `SharedPreferences(\"osmdroid\")` y fija el **user-agent**, requisito de la política de OpenStreetMap para identificar apps:"),
  codigo([
    "// ui/mapa/ConfiguracionMapa.java",
    "public static final String TOKEN_PROVEEDOR = \"\";     // teselas públicas",
    "public static final double LAT_LA_PAZ = -16.500;      // centro de La Paz",
    "public static final double LNG_LA_PAZ = -68.120;",
    "",
    "public static void inicializar(Context contexto) {",
    "    SharedPreferences prefs = contexto.getSharedPreferences(",
    "            \"osmdroid\", Context.MODE_PRIVATE);",
    "    Configuration.getInstance().load(contexto, prefs);",
    "    Configuration.getInstance().setUserAgentValue(",
    "            \"UbiCafe/1.0 (\" + contexto.getPackageName() + \")\");",
    "}",
  ]),

  H2("8.2 Marcadores por categoría"),
  PAr("`MapaFragment.construirMarcadores()` recorre cada lista del repositorio y crea un `MarcadorMapa`. Cada categoría tiene su **color de pin** (un mismo vector `ic_map_pin` teñido con `DrawableCompat`):"),
  tabla([3300, 6060], ["Categoría", "Color (R.color)"],
    [
      ["Cafeterías", "verde_oscuro (#164E3B)"],
      ["Tostadurías", "cafe_accent (#76513D)"],
      ["Productores", "verde_medio (#6B8F71)"],
      ["Puntos de venta", "caramelo_claro (#DDA15E)"],
      ["Ubicación del usuario", "azul_ubicacion (#2E6DB4) — punto fijo en Achumani (-16.535, -68.070)"],
    ]),

  H2("8.3 Colocar y filtrar marcadores"),
  codigo([
    "// MapaFragment.java",
    "for (MarcadorMapa dato : marcadores) {",
    "    Marker marcador = new Marker(mapa);",
    "    marcador.setPosition(new GeoPoint(dato.lat, dato.lng));",
    "    marcador.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);",
    "    marcador.setIcon(pinDeColor(dato.color));",
    "    marcador.setRelatedObject(dato);",
    "    marcador.setOnMarkerClickListener((mk, ov) -> {",
    "        seleccionado = (MarcadorMapa) mk.getRelatedObject();",
    "        mostrarInformacion(requireView(), seleccionado);",
    "        return true;",
    "    });",
    "    mapa.getOverlays().add(marcador);",
    "}",
  ]),
  PAr("Los chips filtran con `setVisible()` y redibujan con `invalidate()`; tras filtrar, el panel inferior muestra el primer marcador visible de la categoría o un aviso de «Tu ubicación»:"),
  codigo([
    "private void aplicarFiltro() {",
    "    for (int i = 0; i < marcadores.size(); i++) {",
    "        boolean visible = tipoSeleccionado.isEmpty()",
    "                || marcadores.get(i).tipo.equals(tipoSeleccionado);",
    "        marcadoresEnMapa.get(i).setVisible(visible);",
    "    }",
    "    mapa.invalidate();",
    "}",
  ]),

  H2("8.4 El mapa «Cerca de ti» según el contexto (MapaActivity)"),
  PAr("`MapaActivity` decide qué dibujar según los extras recibidos:"),
  codigo([
    "// MapaActivity.java",
    "if (nombreCafeteria != null) {",
    "    // una sola cafetería, zoom 15 centrado en ella",
    "    mapa.getController().setZoom(15.0);",
    "    mapa.getController().setCenter(new GeoPoint(m.lat, m.lng));",
    "} else if (nombreMarca != null) {",
    "    // puntos de venta de la marca, zoom 13 centrado en La Paz",
    "} else {",
    "    // todas las cafeterías, zoom 13 centrado en La Paz",
    "}",
  ]),
  PAr("«Ver información» abre el detalle correcto: `DetalleMarcaActivity` si el marcador es un punto de venta, o `DetalleCafeteriaActivity` en los demás casos (el `switch` del fragmento distingue además tostadurías y productores)."),

  H2("8.5 Ciclo de vida del mapa (obligatorio)"),
  codigo([
    "protected void onResume()  { super.onResume();  if (mapa != null) mapa.onResume(); }",
    "protected void onPause()   { super.onPause();   if (mapa != null) mapa.onPause(); }",
    "protected void onDestroy() { if (mapa != null) { mapa.onDetach(); mapa = null; }",
    "    super.onDestroy(); }",
  ]),
  PAr("El trío `onResume/onPause/onDetach` sincroniza la vida del mapa con la de la pantalla. Omitirlo provoca hilos colgados y fugas de memoria. El fragmento hace lo propio con `onResume/onPause/onDestroyView`."),

  H2("8.6 Cambiar de proveedor sin tocar la app"),
  PAr("`MapaFragment` y `MapaActivity` no conocen el proveedor. Para usar teselas propias basta completar la configuración:"),
  codigo([
    "// En el futuro: proveedor propio (MapTiler, Mapbox, institucional…)",
    "TOKEN_PROVEEDOR = \"xxxx\";",
    "// Configuration.getInstance().setUserAgentValue(\"UbiCafe/1.0\");",
    "// mapa.setTileSource(fuenteConUrlYTamanoPropios);",
    "// El resto (marcadores, filtros, detalle) NO cambia.",
  ]),
  PAr("La guía detallada de esta integración vive en `docs/Guia_OpenStreetMap_UbiCafe.docx`."),
);

// 9. Compilar
contenido.push(
  H1("9. Cómo compilar y ejecutar"),
  Paso("Abrir el proyecto en **Android Studio**: File → Open → carpeta raíz de UbiCafe. Esperar a la sincronización de Gradle."),
  Paso("Comprobar que existe un **SDK** con API 24+ y un **dispositivo/emulador** con Android 7.0 o superior."),
  Paso("Ejecutar **Build → Make Project** (Ctrl+F9). Con `gradlew.bat assembleDebug` el resultado es `app/build/outputs/apk/debug/app-debug.apk`."),
  Paso("Instalar en un dispositivo: `adb install -r app/build/outputs/apk/debug/app-debug.apk`."),
  Paso("Para el mapa, el dispositivo necesita **Internet** (las teselas se descargan al vuelo); en un aula sin red, el mapa se verá gris."),
);

// 10. Problemas frecuentes
contenido.push(
  H1("10. Problemas frecuentes y soluciones"),
  tabla([4500, 4860], ["Problema", "Solución"],
    [
      ["El mapa se ve en blanco o gris", "Revisar el permiso INTERNET y el user-agent; el emulador debe tener red y llegar a tile.openstreetmap.org."],
      ["«Couldn't get connection factory client»", "Falta el permiso de red o el servidor de teselas está bloqueado."],
      ["El mapa se corta al rotar la pantalla", "Reconfigurar en onResume y guardar el estado en las preferencias de osmdroid."],
      ["Marcadores duplicados", "No llamar al agregado de marcadores en onCreate y en onResume a la vez."],
      ["El zoom con dos dedos no responde", "Falta multiTouchControls=true (XML o setMultiTouchControls(true))."],
      ["Los chips no se marcan al tocar", "Actualizar el estado y refrescar con marcarChipSeleccionado()."],
      ["Buscar no encuentra acentos", "El buscador compara texto normalizado a NFD (RepositorioDatos)."],
      ["Detalle en blanco al abrir", "El extra no encontró registro; el detalle hace finish() a propósito."],
      ["Gradle no baja librerías", "Verificar conexión y repositorios Maven en settings.gradle."],
      ["El TOC de esta guía no muestra los títulos", "Abrir en Word y actualizar el índice con F9."],
    ]),
);

// 11. Buenas prácticas
contenido.push(
  H1("11. Buenas prácticas aplicadas y mejoras futuras"),
  H2("11.1 Ya aplicado"),
  Bul("**Un solo punto de acceso a datos** (RepositorioDatos) y modelos inmutables con constantes de tipo."),
  Bul("**Extra → lookup → finish** en todos los detalles: nunca se pinta un registro inexistente."),
  Bul("**Comparaciones sin acentos** (NFD) en búsqueda y filtros."),
  Bul("**Recursos reutilizables**: chips, badges, tarjetas y cabecera (`include layout`)."),
  Bul("**Vistas programáticas mínimas**: solo lo que se repite (indicador, chips, notas, filas)."),
  Bul("**Vida del mapa sincronizada** con onResume/onPause/onDetach."),
  H2("11.2 Mejoras futuras"),
  Bul("Migrar `RepositorioDatos` a **SQLite** (`ubicafe.db`) o a una **API REST**, sin tocar pantallas."),
  Bul("**Geolocalización real** (GPS) para «Cerca de ti»; hoy el punto azul es fijo en Achumani."),
  Bul("Revisar los **32 registros** de `revisar_ubicacion` y completar coordenadas."),
  Bul("Terminar el dashboard **Ecosistema** y publicarlo en una pestaña."),
  Bul("Navegación de fragmentos sin recrearlos cada vez (show/hide o Navigation Component)."),
);

// 12. Estructura del proyecto
contenido.push(
  H1("12. Estructura del proyecto"),
  PAr("El árbol resumido ubica cada pieza en segundos:"),
  codigo([
    "ubicafeAPP/",
    "├── app/src/main/",
    "│   ├── java/com/ubicafe/app/",
    "│   │   ├── modelo/      · 7 clases de datos puras",
    "│   │   ├── datos/       · RepositorioDatos + DatosEjemplo",
    "│   │   ├── ui/          · Splash, Onboarding(+Adapter), Main",
    "│   │   ├── ui/{inicio,mapa,explorar,ecosistema,busqueda,origen,",
    "│   │       puntosventa,cafeterias,marcas,tostaderias,productores}",
    "│   │   └── util/        · UiUtils.java",
    "│   ├── res/",
    "│   │   ├── layout/      · 34 layouts (activity_*/fragment_*/item_*)",
    "│   │   ├── menu/        · menu_principal.xml",
    "│   │   ├── values/      · colors.xml, strings.xml, styles.xml, themes.xml",
    "│   │   ├── drawable/    · chips, badges, pines e iconos",
    "│   │   └── font/        · Nunito y Nunito Sans (9 archivos)",
    "│   └── AndroidManifest.xml",
    "├── tools/               · generar_bd.py, capturas_mapa.py,",
    "│                          crear_guia_docx.js, crear_documentacion.js",
    "├── docs/                · guías y figuras (Guia_OpenStreetMap_UbiCafe.docx, figuras/)",
    "├── ubicafe.db · esquema.sql · datos_descartados.log",
    "└── build.gradle · settings.gradle",
  ]),
);

// 13. Fases
contenido.push(
  H1("13. Fases de desarrollo (hitos del proyecto)"),
  PAr("Cada pantalla lleva anotada su fase (`Px`) en el comentario de su clase. El orden resume cómo se construyó la app:"),
  tabla([1400, 7960], ["Fase", "Entregable"],
    [
      ["P1", "Planteamiento: actividad principal, Splash y arranque."],
      ["P2", "Onboarding: carrusel de 3 diapositivas con avanzar/omitir."],
      ["P3", "Inicio: grid de categorías, «Cerca de ti», chips y banner."],
      ["P4", "Mapa: OpenStreetMap/osmdroid con marcadores y filtros."],
      ["P5", "Explorar: variedades, regiones y guía del café."],
      ["P6–P8", "Listas y detalles: cafeterías, marcas, tostadurías, productores."],
      ["P9", "Ficha de origen del café de especialidad."],
      ["P10", "Búsqueda global (cafés, marcas, cafeterías)."],
      ["P12", "Puntos de venta y Ecosistema (cifras + barras)."],
      ["P13–P14", "Consistencia visual, ajustes y documentación."],
    ]),
  PAr("Y, en paralelo, el **pipeline de datos**: censo → `generar_bd.py` → `ubicafe.db` → futura integración con `RepositorioDatos`."),
);

// 14. Glosario
contenido.push(
  H1("14. Glosario rápido"),
  tabla([3000, 6360], ["Término", "Significado"],
    [
      ["Activity / Fragment", "Pantalla completa / porción reutilizable (pestañas)."],
      ["Adapter + RecyclerView", "Patrón que convierte una lista de objetos en tarjetas."],
      ["Extra (Intent)", "Parámetro que viaja con un Intent entre pantallas."],
      ["ViewBinding", "Clases de enlace de vistas del layout, sin findViewById."],
      ["Tesela / tilesource", "Recuadro de imagen del mapa que se descarga al navegar."],
      ["GeoPoint / Marker", "Coordenada (lat/lng) / pin dibujado en osmdroid."],
      ["minSdk / targetSdk", "Versión mínima soportada (24) y objetivo (37)."],
      ["NFD / normalizar", "Descomponer acentos y pasar a minúsculas para comparar."],
      ["Censo / pipeline", "Recolección de datos y proceso que los normaliza."],
    ]),
);

// 15. Conclusión
contenido.push(
  H1("15. Conclusión"),
  PAr("UbiCafe se construyó por fases y con una **separación clara de responsabilidades**: pantallas que solo piden datos al repositorio, modelos inmutables que describen el censo y una extensión de mapas que, gracias a **osmdroid/OpenStreetMap**, funciona sin API key ni costes."),
  PAr("La **base de datos SQLite** ya generada es la puerta natural para crecer sin reescribir la interfaz: una migración en `RepositorioDatos` y el censo completo alimenta todas las listas y el mapa. El resultado es una app fácil de compilar, de explicar y de ampliar, lista para evaluar y para evolucionar."),
);

// 16. Apéndice A — Esquema SQLite
contenido.push(
  new Paragraph({ pageBreakBefore: true, heading: HeadingLevel.HEADING_1, children: [new TextRun("16. Apéndice A — Esquema SQLite (esquema.sql)")] }),
  PAr("El esquema completo usado por `generar_bd.py` para crear `ubicafe.db`:"),
  codigo([
    "PRAGMA foreign_keys = ON;",
    "",
    "CREATE TABLE cafes_origen (",
    "  _id INTEGER PRIMARY KEY AUTOINCREMENT,",
    "  variedad TEXT NOT NULL, origen TEXT, altitud INTEGER,",
    "  proceso TEXT, tueste TEXT, aroma TEXT, descripcion TEXT);",
    "",
    "CREATE TABLE marcas (",
    "  _id INTEGER PRIMARY KEY AUTOINCREMENT,",
    "  nombre TEXT NOT NULL UNIQUE, etiquetas TEXT,",
    "  es_nacional INTEGER NOT NULL DEFAULT 1,",
    "  descripcion TEXT, inicial TEXT);",
    "",
    "CREATE TABLE cafeterias (",
    "  _id INTEGER PRIMARY KEY AUTOINCREMENT,",
    "  nombre TEXT NOT NULL, zona TEXT, barrio TEXT, direccion TEXT,",
    "  horario TEXT, tipo TEXT,",
    "  marca_id INTEGER REFERENCES marcas(_id),",
    "  lat REAL, lng REAL,",
    "  revisar_ubicacion INTEGER NOT NULL DEFAULT 0);",
    "",
    "CREATE TABLE cafeteria_cafes_origen (",
    "  cafeteria_id INTEGER REFERENCES cafeterias(_id),",
    "  cafe_id INTEGER REFERENCES cafes_origen(_id),",
    "  PRIMARY KEY (cafeteria_id, cafe_id));",
    "",
    "CREATE TABLE tostadurias (",
    "  _id INTEGER PRIMARY KEY AUTOINCREMENT,",
    "  nombre TEXT NOT NULL, region TEXT, direccion TEXT, horarios TEXT,",
    "  descripcion TEXT, contacto TEXT,",
    "  lat REAL, lng REAL, revisar_ubicacion INTEGER NOT NULL DEFAULT 0);",
    "",
    "CREATE TABLE productores (",
    "  _id INTEGER PRIMARY KEY AUTOINCREMENT,",
    "  nombre_finca TEXT NOT NULL, familia TEXT, origen TEXT,",
    "  num_cafes INTEGER, altitud INTEGER, descripcion TEXT,",
    "  lat REAL, lng REAL);",
    "",
    "CREATE TABLE puntos_de_venta (",
    "  _id INTEGER PRIMARY KEY AUTOINCREMENT,",
    "  marca TEXT NOT NULL, local TEXT, barrio TEXT, direccion TEXT,",
    "  lat REAL, lng REAL);",
  ]),
);

// 17. Apéndice B — Inventario de archivos
contenido.push(
  H1("17. Apéndice B — Inventario de archivos fuente"),
  PAr("Todos los archivos Java del proyecto (34) con su paquete y propósito:"),
  tabla([2000, 7360], ["Ruta (com.ubicafe.app)", "Propósito"],
    [
      ["SplashActivity", "Pantalla de carga que entra al Onboarding."],
      ["OnboardingActivity / OnboardingAdapter", "Carrusel de presentación (3 páginas)."],
      ["MainActivity", "Pestañas inferiores y cambio de fragmento."],
      ["inicio/InicioFragment", "Home: categorías, cerca de ti, chips, banner."],
      ["explorar/ExplorarFragment", "Variedades, regiones y guía del café."],
      ["mapa/ConfiguracionMapa", "User-agent y preferencias de osmdroid."],
      ["mapa/MarcadorMapa", "Dato que se dibuja sobre el mapa."],
      ["mapa/MapaFragment", "Pestaña Mapa con filtros y panel inferior."],
      ["mapa/MapaActivity", "Mapa completo según contexto (cafetería/marca/todas)."],
      ["busqueda/BusquedaActivity", "Búsqueda global en tres secciones."],
      ["origen/FichaOrigenActivity", "Ficha técnica y notas sensoriales."],
      ["ecosistema/EcosistemaFragment", "Cifras y barras por macrodistrito."],
      ["puntosventa/PuntosVentaActivity", "Marcas con puntos de venta + detalle."],
      ["cafeterias/ListaCafeteriasActivity", "Lista con chips de zona y buscador."],
      ["cafeterias/DetalleCafeteriaActivity", "Tarjeta de cafetería y «Cómo llegar»."],
      ["cafeterias/AdaptadorCafeteria", "Convierte cafeterías en tarjetas."],
      ["marcas/ListaMarcas · DetalleMarca", "Marcas y su detalle (etiquetas, cafés, puntos)."],
      ["tostaderias/Lista · Detalle · Adaptador", "Tostadurías y filtro por región."],
      ["productores/Lista · Detalle", "Productores (fincas)."],
      ["datos/RepositorioDatos · DatosEjemplo", "Único acceso a los datos y datos locales."],
      ["modelo/* (7 clases)", "Cafeteria, MarcaCafe, CafeOrigen, Productor, PuntoVenta, Tostaderia, ResultadoBusqueda."],
      ["util/UiUtils", "Chips pill reutilizables."],
    ]),
);

// 18. Apéndice C — Datos de ejemplo
contenido.push(
  H1("18. Apéndice C — Conjunto de datos de ejemplo (DatosEjemplo)"),
  PAr("`DatosEjemplo` simula el censo mientras no haya base de datos. Resumen de su contenido, tal como está en el código:"),

  H2("18.1 Cafés de especialidad"),
  tabla([1800, 3000, 1400, 3160], ["Variedad", "Origen", "Altitud", "Aroma"],
    [
      ["GEISHA", "Caranavi, La Paz", "1.600 m", "Jazmín, frutas tropicales"],
      ["CATURRA", "Caranavi, La Paz", "1.500 m", "Manzana verde, miel"],
      ["TYPICA", "Coroico, Yungas", "1.450 m", "Cacao, nuez"],
      ["CATIMOR", "Chulumani, Yungas", "1.400 m", "Durazno, miel de caña"],
      ["BOURBON", "Caranavi, La Paz", "1.580 m", "Fresa, cítricos"],
      ["PACAMARA", "Yungas - La Paz", "1.550 m", "Flores blancas, mermelada"],
    ]),

  H2("18.2 Marcas"),
  tabla([2300, 3000, 4060], ["Marca", "Etiquetas", "Cafés de la marca"],
    [
      ["TYPICA", "Cafetería · Tostaduría · Productor", "GEISHA, CATURRA"],
      ["Roaster", "Cafetería · Tostaduría · Productor", "TYPICA, CATIMOR"],
      ["Mugen", "Cafetería · Tostaduría · Productor", "BOURBON"],
      ["Café Yungas", "Productor", "CATIMOR, TYPICA"],
      ["Café de la Finca", "Productor", "PACAMARA"],
    ]),

  H2("18.3 Cafeterías (muestra)"),
  tabla([3800, 1700, 3860], ["Cafetería", "Zona", "Tipo"],
    [
      ["TYPICA - Achumani / San Miguel / Miraflores", "Sur / Sur / Miraflores", "Café de origen"],
      ["Roaster - Sopocachi", "Sopocachi", "Marca nacional"],
      ["Mugen - Centro · Café Yungas - Camacho", "Centro", "Marca nacional"],
      ["Café de la Finca - Calacoto", "Sur", "Marca nacional"],
      ["La Casona Café · Altura Café", "Centro / Sur", "Clásica"],
    ]),

  H2("18.4 Productores, tostadurías y puntos de venta"),
  Bul("**Productores:** Finca El Cóndor (Caranavi), Finca La Primavera (Coroico), Finca Alto Sajama (Caranavi)."),
  Bul("**Tostadurías:** Tostaduría ROC y Torrefactora del Alto (La Paz), CoffeeLab La Paz, Tostaduría Yungas y Tostaduría del Sur (Tarija)."),
  Bul("**Puntos de venta:** 3 de TYPICA, 5 de Café Yungas y 2 de Café de la Finca (10 en total)."),
);

// 19. Apéndice D — Pruebas manuales
contenido.push(
  H1("19. Apéndice D — Lista de verificación de pruebas manuales"),
  tabla([4680, 4680], ["Probar", "Resultado esperado"],
    [
      ["Arranque de la app", "Splash 1,5 s → Onboarding; avanzar/omitir entran a Inicio."],
      ["Onboarding", "Puntos activos crecen; último botón dice «Comenzar»."],
      ["Pestañas", "Inicio/Mapa/Explorar se alternan sin fallos."],
      ["Mapa (sin red)", "Se ve gris; con red aparecen las teselas y los pines."],
      ["Filtros del mapa", "Solo quedan visibles los marcadores de la categoría."],
      ["Cómo llegar", "El mapa se centra en esa cafetería con zoom 15."],
      ["Lista: buscar y filtrar", "Resultados al instante; mensaje «sin resultados» si no hay."],
      ["Detalle inválido", "La pantalla se cierra sola (patrón guarda)."],
      ["Búsqueda global", "Encuentra variedades, marcas y cafeterías con o sin acentos."],
      ["Ficha de origen", "Notas sensoriales aparecen como chips."],
      ["Puntos de venta", "Solo marcas con puntos; selección muestra su detalle."],
      ["Ecosistema", "Cifras y barras proporcionales por macrodistrito."],
    ]),
  PAr("Con estas pruebas, cada fase P1–P14 queda verificada de extremo a extremo: dato → lista → detalle → mapa."),
);

// Documento final
const doc = new Document({
  styles: estilos,
  numbering: numeracion,
  sections: [{
    properties: { page: { margin: { top: 1440, right: 1440, bottom: 1440, left: 1440 } } },
    footers: { default: pie },
    children: contenido,
  }],
});

Packer.toBuffer(doc).then((buf) => {
  fs.writeFileSync(SALIDA, buf);
  console.log("OK", SALIDA, (buf.length / 1024).toFixed(1) + " KB");
});