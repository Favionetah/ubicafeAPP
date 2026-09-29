// Genera la guía .docx "Incorporación de OpenStreetMap (osmdroid) en UbiCafe".
// Uso:  node tools/crear_guia_docx.js   (requiere el paquete 'docx' instalado)
const fs = require("fs");
const path = require("path");
const {
  Document, Packer, Paragraph, TextRun, Table, TableRow, TableCell,
  ImageRun, Header, Footer, AlignmentType, LevelFormat, HeadingLevel,
  BorderStyle, WidthType, ShadingType, VerticalAlign, PageNumber, PageBreak,
  TableOfContents,
} = require("docx");

const RAIZ = path.dirname(path.dirname(__filename));
const FIGURAS = path.join(RAIZ, "docs", "figuras");
const SALIDA = path.join(RAIZ, "docs", "Guia_OpenStreetMap_UbiCafe.docx");
const VERDE = "164E3B";
const VERDE2 = "1C4E3B";
const TENUE = "78817D";

// ---------- estilos ----------
const estilos = {
  default: {
    document: { run: { font: "Arial", size: 22, color: "26332E" } },
  },
  paragraphStyles: [
    { id: "Title", name: "Title", basedOn: "Normal",
      run: { size: 52, bold: true, color: VERDE, font: "Arial" },
      paragraph: { spacing: { before: 240, after: 200 }, alignment: AlignmentType.CENTER } },
    { id: "Heading1", name: "Heading 1", basedOn: "Normal", next: "Normal", quickFormat: true,
      run: { size: 32, bold: true, color: VERDE, font: "Arial" },
      paragraph: { spacing: { before: 320, after: 160 }, outlineLevel: 0 } },
    { id: "Heading2", name: "Heading 2", basedOn: "Normal", next: "Normal", quickFormat: true,
      run: { size: 28, bold: true, color: VERDE2, font: "Arial" },
      paragraph: { spacing: { before: 240, after: 120 }, outlineLevel: 1 } },
    { id: "Subtitulo", name: "Subtitulo", basedOn: "Normal",
      run: { size: 24, italics: true, color: TENUE },
      paragraph: { alignment: AlignmentType.CENTER, spacing: { after: 240 } } },
    { id: "LeyendaFigura", name: "Leyenda Figura", basedOn: "Normal",
      run: { size: 20, italics: true, color: "4E5A55" },
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
        style: { paragraph: { indent: { left: 600, hanging: 300 } } } }] },
  ],
};

// ---------- helpers ----------
const P = (t, opts = {}) => new Paragraph({
  spacing: { after: 140, line: 300 },
  ...opts,
  children: [new TextRun(t)],
});

const PCon = (runs, opts = {}) => new Paragraph({
  spacing: { after: 140, line: 300 },
  ...opts,
  children: runs,
});

// Párrafo con una o más marcas **negrita** inline
const PAr = (t, opts = {}) => new Paragraph({
  spacing: { after: 140, line: 300 },
  ...opts,
  children: t.split("**").map((parte, i) =>
    new TextRun({ text: parte, bold: i % 2 === 1 })),
});

const Coh = (t) => new Paragraph({
  heading: HeadingLevel.HEADING_1,
  pageBreakBefore: t.startsWith("2.") ? false : undefined,
  children: [new TextRun(t)],
});
// usamos pageBreakBefore explicitamente abajo según convenga

const borde = { style: BorderStyle.SINGLE, size: 1, color: "CCCCCC" };
const celdasBorde = { top: borde, bottom: borde, left: borde, right: borde };
const sombra = { fill: "F4F1E8", type: ShadingType.CLEAR };

// Bloque de código como tabla de una celda
function codigo(lineas) {
  const parrafos = lineas.map((l) => new Paragraph({
    spacing: { after: 0, line: 260 },
    children: [new TextRun({ text: l, font: "Consolas", size: 19, color: "1F2A26" })],
  }));
  return new Table({
    columnWidths: [9360],
    margins: { top: 140, bottom: 140, left: 180, right: 180 },
    rows: [new TableRow({
      children: [new TableCell({
        borders: celdasBorde,
        width: { size: 9360, type: WidthType.DXA },
        shading: sombra,
        children: parrafos.length ? parrafos : [new Paragraph({ children: [] })],
      })],
    })],
  });
}

const espacio = () => new Paragraph({ spacing: { after: 120 }, children: [new TextRun("")] });

// Figura con captura
function imagen(nombreArchivo, anchoPx, captura) {
  const img = fs.readFileSync(path.join(FIGURAS, nombreArchivo));
  const alto = Math.round((anchoPx * 2100) / 1200);
  return [
    new Paragraph({
      alignment: AlignmentType.CENTER,
      spacing: { before: 160, after: 60 },
      children: [new ImageRun({
        type: "png", data: img,
        transformation: { width: anchoPx, height: alto },
        altText: { title: captura, description: captura, name: captura },
      })],
    }),
    new Paragraph({ style: "LeyendaFigura", children: [new TextRun(captura)] }),
  ];
}

// Cabecera / pie
const pie = new Footer({
  children: [new Paragraph({
    alignment: AlignmentType.CENTER,
    children: [
      new TextRun("UbiCafe · Guía de integración del mapa · "),
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
    children: [new TextRun({ text: "Incorporación del mapa en la aplicación", bold: true, size: 40 })] }),
  new Paragraph({ alignment: AlignmentType.CENTER,
    children: [new TextRun({ text: "OpenStreetMap (osmdroid) en lugar de Google Maps", size: 32, color: VERDE2, bold: true })] }),
  new Paragraph({ spacing: { before: 480 }, style: "Subtitulo",
    children: [new TextRun("Guía técnica paso a paso · Proyecto Android · Módulo Mapa (P4)")] }),
  new Paragraph({ spacing: { before: 40 }, style: "Subtitulo",
    children: [new TextRun("27 de septiembre de 2026")] }),
  new Paragraph({ children: [new PageBreak()] }),
);

// Índice
contenido.push(
  new Paragraph({ heading: HeadingLevel.HEADING_1, children: [new TextRun("Índice")] }),
  new TableOfContents("Contenido", { hyperlink: true, headingStyleRange: "1-2" }),
  new Paragraph({ children: [new PageBreak()] }),
);

// 1. Introducción
contenido.push(
  new Paragraph({ heading: HeadingLevel.HEADING_1, children: [new TextRun("1. Introducción y objetivo")] }),
  PAr("UbiCafe es una aplicación Android que muestra el **ecosistema del café de La Paz**: cafeterías, marcas, tostadurías, productores y puntos de venta, todos con coordenadas reales obtenidas en un censo. Uno de sus módulos centrales (P4) es el **mapa**, la pantalla desde la que el usuario explora visualmente esa información."),
  PAr("El objetivo de esta guía es explicar, paso a paso, **cómo se integró un mapa real en el proyecto**. Aunque el título más habitual es «la API de Google Maps», en UbiCafe se eligió **OpenStreetMap a través de la librería osmdroid**, una alternativa gratuita, sin API key y con datos abiertos, ideal para un proyecto académico o de bajo presupuesto."),
  PAr("Al terminar esta guía sabrás: qué piezas necesita la app para mostrar el mapa, **cómo funciona cada fragmento de código**, cómo se colocan y filtran los marcadores, y cómo cambiar de proveedor de teselas si algún día se desea usar otro servicio."),
);

// 2. Comparativa
contenido.push(
  new Paragraph({ heading: HeadingLevel.HEADING_1, children: [new TextRun("2. ¿Google Maps u OpenStreetMap?")] }),
  PAr("La decisión más importante al añadir mapas es **elegir el proveedor**. Google Maps ofrece el servicio más conocido, pero en un proyecto académico suele imponer requisitos que pueden complicar la entrega:"),
  new Table({
    columnWidths: [1560, 3900, 3900],
    margins: { top: 80, bottom: 80, left: 150, right: 150 },
    rows: [
      new TableRow({ tableHeader: true, children: [
        new TableCell({ borders: celdasBorde, width: { size: 1560, type: WidthType.DXA },
          shading: { fill: "E4EADF", type: ShadingType.CLEAR },
          verticalAlign: VerticalAlign.CENTER,
          children: [new Paragraph({ children: [new TextRun({ text: "Criterio", bold: true })] })] }),
        new TableCell({ borders: celdasBorde, width: { size: 3900, type: WidthType.DXA },
          shading: { fill: "E4EADF", type: ShadingType.CLEAR },
          verticalAlign: VerticalAlign.CENTER,
          children: [new Paragraph({ children: [new TextRun({ text: "Google Maps SDK", bold: true })] })] }),
        new TableCell({ borders: celdasBorde, width: { size: 3900, type: WidthType.DXA },
          shading: { fill: "E4EADF", type: ShadingType.CLEAR },
          verticalAlign: VerticalAlign.CENTER,
          children: [new Paragraph({ children: [new TextRun({ text: "OpenStreetMap (osmdroid)", bold: true })] })] }),
      ]}),
      ...[
        ["API key", "Obligatoria, ligada a una cuenta de Google y a la facturación", "No requiere clave: teselas públicas de openstreetmap.org"],
        ["Coste", "Tiene límites gratuitos; después se factura", "Gratuito (se recomienda un proveedor propio a gran escala)"],
        ["Datos", "Dependen de la cobertura de Google", "Datos abiertos y editables por la comunidad"],
        ["License/uso", "Términos de Google Maps Platform", "ODbL (OpenStreetMap) + licencia de osmdroid (Apache 2.0)"],
        ["Permisos en Android", "Además de INTERNET, la resolución de la clave", "Solo INTERNET (las teselas se descargan al vuelo)"],
        ["Configuración", "Google Play Services + claves por BUILD_TYPE", "Un fichero de configuración de 30 líneas"],
      ].map((f) => new TableRow({ children: f.map((c) =>
        new TableCell({ borders: celdasBorde, width: c.length > 20 ? { size: 3900, type: WidthType.DXA } : { size: 1560, type: WidthType.DXA },
          children: [new Paragraph({ children: [new TextRun(c)] })] })) })),
    ],
  }),
  PAr("**Conclusión:** para UbiCafe, la ausencia de API key y de costes hace que OpenStreetMap sea la opción más limpia para enseñar y ejecutar el proyecto. El resto de la guía usa `osmdroid-android:6.1.20`."),
);

// 3. Requisitos
contenido.push(
  new Paragraph({ heading: HeadingLevel.HEADING_1, children: [new TextRun("3. Requisitos previos")] }),
  new Paragraph({ numbering: { reference: "viñetas", level: 0 }, children: [new TextRun({ text: "Android Studio (o Gradle) con un proyecto Android en Java/Kotlin. UbiCafe usa Java 17 y vistas clásicas (View + XML).", })] }),
  new Paragraph({ numbering: { reference: "viñetas", level: 0 }, children: [new TextRun({ text: "Internet en el dispositivo/emulador para descargar las teselas del mapa en tiempo real.", })] }),
  new Paragraph({ numbering: { reference: "viñetas", level: 0 }, children: [new TextRun({ text: "Un teléfono o emulador con Android 7 (API 24) o superior, que es el valor de minSdk del proyecto.", })] }),
  new Paragraph({ numbering: { reference: "viñetas", level: 0 }, children: [new TextRun({ text: "Conocimientos básicos de Gradle, XML de layouts y Java.", })] }),
  PAr("Los pasos 1 a 7 corresponden a ficheros reales dentro de `app/src/main/`. El código que verás es exactamente el del proyecto."),
);

// 4. Paso 1
contenido.push(
  new Paragraph({ heading: HeadingLevel.HEADING_1, children: [new TextRun("4. Paso 1 — Añadir la librería osmdroid")] }),
  PAr("El primer paso es decirle a Gradle que el proyecto necesita la librería. En `app/build.gradle`, dentro del bloque `dependencies`, se añade una única línea:"),
  codigo([
    "// app/build.gradle",
    "dependencies {",
    "    ...",
    "    // osmdroid: mapa real de OpenStreetMap (P4).",
    "    implementation 'org.osmdroid:osmdroid-android:6.1.20'",
    "}",
  ]),
  PAr("Tras guardar, Gradle descarga la librería automáticamente. osmdroid trae `MapView`, `Marker`, `GeoPoint` y las clases de configuración que usaremos en los siguientes pasos."),
);

// 5. Paso 2
contenido.push(
  new Paragraph({ heading: HeadingLevel.HEADING_1, children: [new TextRun("5. Paso 2 — Permisos de red (sin API key)")] }),
  PAr("El mapa descarga recuadros (teselas) de imagen desde Internet mientras el usuario se desplaza. Por eso la app necesita el permiso de red en `app/src/main/AndroidManifest.xml`:"),
  codigo([
    "<!-- app/src/main/AndroidManifest.xml -->",
    "<uses-permission android:name=\"android.permission.INTERNET\" />",
    "<uses-permission android:name=\"android.permission.ACCESS_NETWORK_STATE\" />",
  ]),
  PAr("**Detalle importante:** a diferencia de Google Maps, **no se necesita ninguna API key**. Las teselas públicas de OpenStreetMap se sirven sin autenticación, únicamente con un *user-agent* identificable (lo configuramos en el Paso 3). La Activity del mapa se declara como cualquier otra pantalla:"),
  codigo(["<activity android:name=\".ui.mapa.MapaActivity\" />"]),
);

// 6. Paso 3
contenido.push(
  new Paragraph({ heading: HeadingLevel.HEADING_1, children: [new TextRun("6. Paso 3 — Configurar osmdroid al arrancar")] }),
  PAr("osmdroid necesita que su objeto de configuración global se cargue una vez. En UbiCafe lo centralizamos en la clase `ConfiguracionMapa`:"),
  codigo([
    "// app/src/main/java/com/ubicafe/app/ui/mapa/ConfiguracionMapa.java",
    "public final class ConfiguracionMapa {",
    "    public static final String TOKEN_PROVEEDOR = \"\";",
    "    public static final double LAT_LA_PAZ = -16.500;",
    "    public static final double LNG_LA_PAZ = -68.120;",
    "",
    "    public static void inicializar(Context contexto) {",
    "        SharedPreferences preferencias = contexto.getSharedPreferences(",
    "                \"osmdroid\", Context.MODE_PRIVATE);",
    "        Configuration.getInstance().load(contexto, preferencias);",
    "        Configuration.getInstance().setUserAgentValue(",
    "                \"UbiCafe/1.0 (\" + contexto.getPackageName() + \")\");",
    "    }",
    "}",
  ]),
  PAr("Qué ocurre aquí, en lenguaje normal:"),
  new Paragraph({ numbering: { reference: "viñetas", level: 0 }, children: [new TextRun({ text: "Se guarda el estado del mapa (zoom, posición) en las preferencias de Android para conservarlas entre sesiones.", })] }),
  new Paragraph({ numbering: { reference: "viñetas", level: 0 }, children: [new TextRun({ text: "Se declara un user-agent que identifica a la app: OpenStreetMap pide que las aplicaciones se identifiquen para poder mantener el servicio.", })] }),
  new Paragraph({ numbering: { reference: "viñetas", level: 0 }, children: [new TextRun({ text: "Las constantes de La Paz sirven para centrar el mapa por defecto en cada pantalla.", })] }),
  PAr("`inicializar(...)` se llama desde `onCreate` de cada Activity y desde `onCreateView` del fragmento que usa el mapa."),
);

// 7. Paso 4
contenido.push(
  new Paragraph({ heading: HeadingLevel.HEADING_1, children: [new TextRun("7. Paso 4 — Añadir MapView al layout")] }),
  PAr("El mapa es un componente de vista más. En el XML del layout se coloca `org.osmdroid.views.MapView` dentro de un contenedor. Tendrá su propio espacio del nombre `osmdroid`:"),
  codigo([
    "<!-- app/src/main/res/layout/fragment_mapa.xml -->",
    "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"",
    "    xmlns:osmdroid=\"http://schemas.osmdroid.org/map\"",
    "    android:layout_width=\"match_parent\"",
    "    android:layout_height=\"match_parent\"",
    "    android:orientation=\"vertical\">",
    "",
    "    <org.osmdroid.views.MapView",
    "        android:id=\"@+id/mapa_vista\"",
    "        android:layout_width=\"match_parent\"",
    "        android:layout_height=\"match_parent\"",
    "        osmdroid:tilesource=\"MAPNIK\"",
    "        osmdroid:zoom=\"13\"",
    "        osmdroid:multiTouchControls=\"true\" />",
    "</LinearLayout>",
  ]),
  PAr("Los tres atributos clave:"),
  new Paragraph({ numbering: { reference: "viñetas", level: 0 }, children: [new TextRun({ text: "tilesource=\"MAPNIK\": el estilo del mapa (el aspecto clásico de OpenStreetMap). Se puede cambiar por otras fuentes de teselas.", })] }),
  new Paragraph({ numbering: { reference: "viñetas", level: 0 }, children: [new TextRun({ text: "zoom=\"13\": nivel de acercamiento inicial, adecuado para zoom de ciudad.", })] }),
  new Paragraph({ numbering: { reference: "viñetas", level: 0 }, children: [new TextRun({ text: "multiTouchControls=\"true\": habilita pellizcar para hacer zoom en pantallas táctiles.", })] }),
);

// 8. Paso 5
contenido.push(
  new Paragraph({ heading: HeadingLevel.HEADING_1, children: [new TextRun("8. Paso 5 — Colocar marcadores sobre el mapa")] }),
  PAr("Cada cafetería, tostaduría, productor y punto de venta se convierte en un **marcador** (pin). Los datos provienen de `RepositorioDatos` y las coordenadas son reales (latitud y longitud del censo). El bucle es el mismo para todos:"),
  codigo([
    "// app/src/main/java/com/ubicafe/app/ui/mapa/MapaFragment.java",
    "import org.osmdroid.util.GeoPoint;",
    "import org.osmdroid.views.overlay.Marker;",
    "",
    "for (Cafeteria dato : RepositorioDatos.obtenerCafeterias()) {",
    "    Marker marcador = new Marker(mapa);",
    "    marcador.setPosition(new GeoPoint(dato.lat, dato.lng));",
    "    marcador.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);",
    "    marcador.setIcon(pinDeColor(colorCafeteria));",
    "    marcador.setRelatedObject(dato);",       // guarda el objeto tocado
    "    marcador.setOnMarkerClickListener((mk, overlay) -> {",
    "        seleccionado = (MarcadorMapa) mk.getRelatedObject();",
    "        mostrarInformacion(requireView(), seleccionado);",
    "        return true;",
    "    });",
    "    mapa.getOverlays().add(marcador);",
    "}",
  ]),
  PAr("Cómo se lee:"),
  new Paragraph({ numbering: { reference: "viñetas", level: 0 }, children: [new TextRun({ text: "`GeoPoint(lat, lng)` convierte las coordenadas en un punto del mapa.", })] }),
  new Paragraph({ numbering: { reference: "viñetas", level: 0 }, children: [new TextRun({ text: "`setAnchor` fija el anclaje del pin (su punta toca el punto exacto).", })] }),
  new Paragraph({ numbering: { reference: "viñetas", level: 0 }, children: [new TextRun({ text: "`setIcon` pinta el pin; cada categoría usa su color de marca (verde cafetería, café tostaduría…).", })] }),
  new Paragraph({ numbering: { reference: "viñetas", level: 0 }, children: [new TextRun({ text: "`setOnMarkerClickListener` detecta el toque y muestra la tarjeta inferior con la información.", })] }),
  new Paragraph({ numbering: { reference: "viñetas", level: 0 }, children: [new TextRun({ text: "`setRelatedObject` guarda el dato original junto al marcador para saber qué pantalla abrir después.", })] }),
  PAr("El mismo vector de pin se tiñe con el color de cada categoría, como un sello de cera:"),
  codigo([
    "private Drawable pinDeColor(int color) {",
    "    Drawable pin = ContextCompat.getDrawable(ctx, R.drawable.ic_map_pin);",
    "    Drawable tenido = DrawableCompat.wrap(pin).mutate();",
    "    DrawableCompat.setTint(tenido, color);",
    "    return tenido;",
    "}",
  ]),
);

// 9. Paso 6
contenido.push(
  new Paragraph({ heading: HeadingLevel.HEADING_1, children: [new TextRun("9. Paso 6 — Inicializar el mapa y centrarlo")] }),
  PAr("La pantalla completa `MapaActivity` (el «mapa de cerca de ti») muestra lo mismo, pero en pantalla entera. Para que el mapa funcione es imprescindible enlazar su ciclo de vida con el de la Activity:"),
  codigo([
    "// app/src/main/java/com/ubicafe/app/ui/mapa/MapaActivity.java",
    "protected void onCreate(Bundle savedInstanceState) {",
    "    super.onCreate(savedInstanceState);",
    "    ConfiguracionMapa.inicializar(this);   // 1. configuración de osmdroid",
    "    setContentView(R.layout.activity_mapa);",
    "    mapa = findViewById(R.id.mapa_vista);",
    "    mapa.setMultiTouchControls(true);      // 2. zoom con gestos",
    "    // 3. centrar el mapa en La Paz.",
    "    mapa.getController().setZoom(13.0);",
    "    mapa.getController().setCenter(",
    "        new GeoPoint(ConfiguracionMapa.LAT_LA_PAZ, ConfiguracionMapa.LNG_LA_PAZ));",
    "}",
    "",
    "protected void onResume() { super.onResume(); if (mapa != null) mapa.onResume(); }",
    "protected void onPause()  { super.onPause();  if (mapa != null) mapa.onPause(); }",
    "protected void onDestroy() {",
    "    if (mapa != null) { mapa.onDetach(); mapa = null; }",
    "    super.onDestroy();",
    "}",
  ]),
  PAr("El trío `onResume` / `onPause` / `onDetach` es **obligatorio**: el mapa descarga teselas y consume recursos, y así su vida queda sincronizada con la pantalla. Si se omite, la app puede quedarse con hilos colgados."),
  PAr("Además, esta pantalla es «inteligente» según cómo se abrió: si llego desde *Cómo llegar* de una cafetería, centra en esa cafetería con zoom 15; si llego desde una marca, pinta sus puntos de venta; y sin contexto, muestra todas las cafeterías."),
  codigo([
    "if (nombreCafeteria != null) {",
    "    mapa.getController().setZoom(15.0);",
    "    mapa.getController().setCenter(new GeoPoint(cafeteria.lat, cafeteria.lng));",
    "} else {",
    "    mapa.getController().setZoom(13.0);",
    "    mapa.getController().setCenter(new GeoPoint(",
    "        ConfiguracionMapa.LAT_LA_PAZ, ConfiguracionMapa.LNG_LA_PAZ));",
    "}",
  ]),
);

// 10. Paso 7
contenido.push(
  new Paragraph({ heading: HeadingLevel.HEADING_1, children: [new TextRun("10. Paso 7 — Filtros y panel de información")] }),
  PAr("La pestaña Mapa incluye **chips de filtro** (Todos, Cafeterías, Tostadurías, Productores, Puntos de venta). Al tocar un chip se ocultan o muestran los marcadores de esa categoría recorriendo las listas que se guardaron al colocarlos:"),
  codigo([
    "private void aplicarFiltro() {",
    "    for (int i = 0; i < marcadores.size(); i++) {",
    "        boolean visible = tipoSeleccionado.isEmpty()",
    "                || marcadores.get(i).tipo.equals(tipoSeleccionado);",
    "        marcadoresEnMapa.get(i).setVisible(visible);",
    "    }",
    "    mapa.invalidate();   // redibuja el mapa",
    "}",
  ]),
  PAr("Cuando el usuario toca un marcador, la tarjeta inferior muestra su nombre, categoría y zona. El botón **«Ver información»** abre la pantalla de detalle correcta según el tipo: cafetería, tostaduría, productor o marca. Todo se decide con la constante `tipo` que cada `MarcadorMapa` lleva consigo."),
);

// 11. Paso 8
contenido.push(
  new Paragraph({ heading: HeadingLevel.HEADING_1, children: [new TextRun("11. Paso 8 — Ejecutar la aplicación y ver el resultado")] }),
  PAr("Con estos pasos el mapa ya aparece. Al ejecutar la app, la pestaña **Mapa** muestra la ciudad de La Paz con las teselas de OpenStreetMap y los marcadores de todas las categorías. Las siguientes imágenes reproducen fielmente las pantallas reales usando las coordenadas del censo y el mapa auténtico de OpenStreetMap."),
  new Paragraph({ pageBreakBefore: true, heading: HeadingLevel.HEADING_2, children: [new TextRun("11.1 Pestaña Mapa con todos los marcadores")] }),
  PAr("La pantalla `fragment_mapa` con los filtros, el pin azul marcando la ubicación de ejemplo y la tarjeta inferior con la información del primer lugar."),
  ...imagen("figura_mapa_todos.png", 200, "Figura 1. Pestaña Mapa: OpenStreetMap con cafeterías (verde), tostadurías (café), productores (verde medio) y puntos de venta (caramelo), además de la ubicación azul."),
  new Paragraph({ heading: HeadingLevel.HEADING_2, children: [new TextRun("11.2 Filtrando solo cafeterías")] }),
  ...imagen("figura_mapa_cafeterias.png", 200, "Figura 2. Al tocar el chip «Cafeterías» solo quedan visibles los marcadores verdes; el resto se oculta sin recargar el mapa."),
  new Paragraph({ pageBreakBefore: true, heading: HeadingLevel.HEADING_2, children: [new TextRun("11.3 «Cómo llegar» a una cafetería")] }),
  PAr("Desde el detalle de una cafetería, «Cómo llegar» abre `MapaActivity` centrada en ese establecimiento con zoom 15: un único pin verde y la tarjeta inferior con la información."),
  ...imagen("figura_mapa_cerca_de_ti.png", 200, "Figura 3. Pantalla completa «Cómo llegar»: el mapa se centra en la cafetería elegida (zoom 15) con su pin verde."),
  new Paragraph({ heading: HeadingLevel.HEADING_2, children: [new TextRun("11.4 Puntos de venta de una marca")] }),
  PAr("Desde el detalle de una marca, «Ver puntos en mapa» dibuja únicamente los puntos de venta de esa marca. Aquí se muestran las sedes de TYPICA con pines color caramelo:"),
  ...imagen("figura_mapa_puntos_venta.png", 200, "Figura 4. Mapa de una marca (TYPICA): sus puntos de venta con pines caramelo sobre el mapa de OpenStreetMap."),
);

// 12. Cambiar proveedor
contenido.push(
  new Paragraph({ pageBreakBefore: true, heading: HeadingLevel.HEADING_1, children: [new TextRun("12. ¿Y si queremos usar otro proveedor de mapas?")] }),
  PAr("Una de las ventajas de esta arquitectura es que **casi todo el trabajo ya está hecho**: el código de marcadores, filtros y detalles no depende del proveedor. Para cambiar de OpenStreetMap a un servicio de teselas propio (por ejemplo MapTiler, Mapbox o un servidor institucional) solo se toca la configuración:"),
  codigo([
    "// ConfiguracionMapa.java — dato preparado para el futuro",
    "public static final String TOKEN_PROVEEDOR = \"\";   // ← aquí iría el token",
    "",
    "// Si algún día se cuenta con un proveedor propio:",
    "//   Configuration.getInstance().setUserAgentValue(\"UbiCafe/1.0\");",
    "//   mapa.setTileSource(nuevaFuenteConUrlYTamanoDeTeselas);",
    "// El resto de la aplicación (marcadores, filtros, detalles) no cambia.",
  ]),
  PAr("Para volver a la idea original de Google Maps API, la equivalencia sería: sustituir `MapView` de osmdroid por el `MapView` de Google Play Services, declarar la API key en el `AndroidManifest.xml` y traducir `Marker`/`GeoPoint` a las clases equivalentes. La lógica de negocio de UbiCafe permanecería intacta."),
);

// 13. Problemas frecuentes
contenido.push(
  new Paragraph({ heading: HeadingLevel.HEADING_1, children: [new TextRun("13. Problemas frecuentes y soluciones")] }),
  new Table({
    columnWidths: [4680, 4680],
    margins: { top: 80, bottom: 80, left: 150, right: 150 },
    rows: [
      new TableRow({ tableHeader: true, children: [
        new TableCell({ borders: celdasBorde, width: { size: 4680, type: WidthType.DXA }, shading: { fill: "E4EADF", type: ShadingType.CLEAR }, verticalAlign: VerticalAlign.CENTER, children: [new Paragraph({ children: [new TextRun({ text: "Problema", bold: true })] })] }),
        new TableCell({ borders: celdasBorde, width: { size: 4680, type: WidthType.DXA }, shading: { fill: "E4EADF", type: ShadingType.CLEAR }, verticalAlign: VerticalAlign.CENTER, children: [new Paragraph({ children: [new TextRun({ text: "Solución", bold: true })] })] }),
      ]}),
      ...[
        ["El mapa se ve en blanco o gris", "Revisa el permiso INTERNET y el user-agent (Paso 2 y 3); los emuladores sin red no cargan teselas."],
        ["«Couldn't get connection factory client» en los logs", "Falta el permiso de red, o el dispositivo no tiene acceso a tile.openstreetmap.org."],
        ["El mapa se corta al rotar la pantalla", "Añade android:configChanges o guarda el estado (SharedPreferences del Paso 3) y vuelve a centrar en onResume."],
        ["Marcadores doblados (duplicados)", "Se agregaron dos veces al mismo overlay; revisa que no llames a agregarMarcadores() en onCreate y onResume a la vez."],
        ["El zoom con dos dedos no responde", "Falta multiTouchControls=true en el XML o en setMultiTouchControls(true)."],
        ["Quiero teselas con otro estilo", "Cambia tilesource a otra fuente (por ejemplo OTMSource o un proveedor propio) en ConfiguracionMapa."],
      ].map((f) => new TableRow({ children: f.map((c) =>
        new TableCell({ borders: celdasBorde, width: { size: 4680, type: WidthType.DXA }, children: [new Paragraph({ children: [new TextRun(c)] })] })) })),
    ],
  }),
);

// 14. Conclusión
contenido.push(
  new Paragraph({ heading: HeadingLevel.HEADING_1, children: [new TextRun("14. Conclusión")] }),
  PAr("Integrar un mapa real en UbiCafe se redujo a: una dependencia en Gradle, dos permisos en el manifiesto, una configuración inicial y la colocación de marcadores con las coordenadas del censo. Elegir **OpenStreetMap con osmdroid** evitó la API key y los costes de Google Maps sin perder las funciones que la app necesita: zoom, gestos, filtros y apertura del detalle del lugar."),
  PAr("Los archivos implicados viven en `app/src/main/java/com/ubicafe/app/ui/mapa/` (`MapaFragment.java`, `MapaActivity.java`, `ConfiguracionMapa.java`) y en los layouts `fragment_mapa.xml` y `activity_mapa.xml`. Con esta guía y esos ficheros, cualquier persona (o docente) puede reproducir y ampliar el módulo de mapas."),
);

// Documento final
const doc = new Document({
  styles: estilos,
  numbering: numeracion,
  sections: [{
    properties: {
      page: { margin: { top: 1440, right: 1440, bottom: 1440, left: 1440 } },
    },
    footers: { default: pie },
    children: contenido,
  }],
});

Packer.toBuffer(doc).then((buf) => {
  fs.writeFileSync(SALIDA, buf);
  console.log("OK", SALIDA, (buf.length / 1024).toFixed(1) + " KB");
});