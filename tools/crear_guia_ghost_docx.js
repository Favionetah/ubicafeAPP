// Genera la guía en Word de cómo entrar a la Pantalla Fantasma de UbiCafe.
// Uso:  node tools/crear_guia_ghost_docx.js   (requiere el paquete 'docx' instalado)
const fs = require("fs");
const path = require("path");
const {
  Document, Packer, Paragraph, TextRun, Table, TableRow, TableCell,
  Footer, AlignmentType, HeadingLevel, BorderStyle, WidthType,
  ShadingType, VerticalAlign, PageNumber, PageBreak, ImageRun, LevelFormat,
} = require("docx");

const RAIZ = path.dirname(path.dirname(__filename));
const SALIDA = path.join(RAIZ, "docs", "Guia_Pantalla_Fantasma_UbiCafe.docx");
const FIG1 = path.join(RAIZ, "docs", "figuras", "ghost_01_pantalla_principal.png");
const FIG2 = path.join(RAIZ, "docs", "figuras", "ghost_02_pantalla_fantasma.png");

const VERDE = "164E3B";
const VERDE2 = "1C4E3B";
const TENUE = "78817D";

const estilos = {
  default: { document: { run: { font: "Arial", size: 22, color: "26332E" } } },
  paragraphStyles: [
    { id: "Title", name: "Title", basedOn: "Normal",
      run: { size: 52, bold: true, color: VERDE, font: "Arial" },
      paragraph: { spacing: { before: 240, after: 200 }, alignment: AlignmentType.CENTER } },
    { id: "Heading1", name: "Heading 1", basedOn: "Normal", next: "Normal", quickFormat: true,
      run: { size: 32, bold: true, color: VERDE, font: "Arial" },
      paragraph: { spacing: { before: 280, after: 130 }, outlineLevel: 0 } },
    { id: "Heading2", name: "Heading 2", basedOn: "Normal", next: "Normal", quickFormat: true,
      run: { size: 28, bold: true, color: VERDE2, font: "Arial" },
      paragraph: { spacing: { before: 200, after: 100 }, outlineLevel: 1 } },
    { id: "Subtitulo", name: "Subtitulo", basedOn: "Normal",
      run: { size: 24, italics: true, color: TENUE },
      paragraph: { alignment: AlignmentType.CENTER, spacing: { after: 240 } } },
  ],
};

const P = (t, opts = {}) => new Paragraph({
  spacing: { after: 120, line: 288 }, ...opts, children: [new TextRun(t)],
});
const PAr = (t, opts = {}) => new Paragraph({
  spacing: { after: 120, line: 288 }, ...opts,
  children: t.split("**").map((parte, i) =>
    new TextRun({ text: parte, bold: i % 2 === 1 })),
});
const H1 = (t) => new Paragraph({ heading: HeadingLevel.HEADING_1, children: [new TextRun(t)] });
const H2 = (t) => new Paragraph({ heading: HeadingLevel.HEADING_2, children: [new TextRun(t)] });

const borde = { style: BorderStyle.SINGLE, size: 1, color: "CCCCCC" };
const celdasBorde = { top: borde, bottom: borde, left: borde, right: borde };
const sombra = { fill: "F4F1E8", type: ShadingType.CLEAR };

function codigo(lineas) {
  return new Table({
    columnWidths: [9360],
    margins: { top: 140, bottom: 140, left: 180, right: 180 },
    rows: [new TableRow({ children: [new TableCell({
      borders: celdasBorde, width: { size: 9360, type: WidthType.DXA }, shading: sombra,
      children: lineas.map((l) => new Paragraph({
        spacing: { after: 0, line: 248 },
        children: [new TextRun({ text: l, font: "Consolas", size: 19, color: "1F2A26" })],
      })),
    })] })],
  });
}

function tabla(anchuras, cabecera, filas, resaltar = []) {
  const celdas = (valores, i) => valores.map((v, j) =>
    new TableCell({
      borders: celdasBorde, width: { size: anchuras[j], type: WidthType.DXA },
      shading: i === 0 ? { fill: "E4EADF", type: ShadingType.CLEAR }
        : resaltar.includes(i) ? { fill: "F4F1E8", type: ShadingType.CLEAR } : undefined,
      verticalAlign: VerticalAlign.CENTER,
      children: [new Paragraph({ children: [new TextRun({ text: v, bold: i === 0 })] })],
    }));
  const filasTabla = [new TableRow({ tableHeader: true, children: celdas(cabecera, 0) })];
  filas.forEach((f, i) => filasTabla.push(new TableRow({ children: celdas(f, i + 1) })));
  return new Table({
    columnWidths: anchuras,
    margins: { top: 80, bottom: 80, left: 150, right: 150 },
    rows: filasTabla,
  });
}

const pie = new Footer({
  children: [new Paragraph({
    alignment: AlignmentType.CENTER,
    children: [
      new TextRun("UbiCafe · Guía de la pantalla fantasma · "),
      new TextRun({ children: [PageNumber.CURRENT] }), new TextRun(" de "),
      new TextRun({ children: [PageNumber.TOTAL_PAGES] }),
    ],
  })],
});

const contenido = [];

// Portada
contenido.push(
  new Paragraph({ spacing: { before: 1100 }, alignment: AlignmentType.CENTER,
    children: [new TextRun({ text: "UBICAFÉ", bold: true, size: 72, color: VERDE })] }),
  new Paragraph({ alignment: AlignmentType.CENTER,
    children: [new TextRun({ text: "Café de La Paz", size: 30, color: TENUE })] }),
  new Paragraph({ spacing: { before: 360 }, alignment: AlignmentType.CENTER,
    children: [new TextRun({ text: "Guía: cómo entrar a la pantalla fantasma", bold: true, size: 40, color: "26332E" })] }),
  new Paragraph({ spacing: { before: 480 }, style: "Subtitulo",
    children: [new TextRun("De dónde sale, hacia dónde va, el patrón secreto y el código de la lógica de acceso")] }),
  new Paragraph({ spacing: { before: 40 }, style: "Subtitulo",
    children: [new TextRun("28 de septiembre de 2026")] }),
  new Paragraph({ children: [new PageBreak()] }),
);

// 1. De qué pantalla a cuál
contenido.push(
  H1("1. De qué pantalla a cuál"),
  PAr("La **pantalla fantasma** (GhostActivity, titulada «Desarrolladores») es una pantalla oculta: no aparece en ningún menú ni en la navegación normal de la app. Solamente se puede alcanzar **desde la pantalla principal** (MainActivity), que es la que tiene la barra de pestañas inferior con **Inicio · Mapa · Explorar**."),
  PAr("Para pasar de una a otra hay que tocar un **patrón exacto de 12 toques** en esa barra inferior. La tabla resume el viaje:"),
  tabla([2600, 6760], ["Pantalla de origen", "Pantalla de destino"],
    [
      ["MainActivity — pestaña principal con barra inferior Inicio · Mapa · Explorar", "GhostActivity — pantalla «Desarrolladores» con el isologo de la UPDS y los datos del equipo"],
    ]),
  P(""),
  PAr("La **Figura 1** muestra la pantalla de origen (la principal, con las tres pestañas). La **Figura 2** muestra la pantalla fantasma ya abierta, con el isologo de la Universidad Privada Domingo Savio y los datos del equipo desarrollador."),

  new Table({
    columnWidths: [4680, 4680],
    margins: { top: 80, bottom: 80, left: 150, right: 150 },
    rows: [new TableRow({ children: [
      new TableCell({ borders: celdasBorde, verticalAlign: VerticalAlign.CENTER,
        children: [
          new Paragraph({ alignment: AlignmentType.CENTER, spacing: { after: 60 },
            children: [new ImageRun({ data: fs.readFileSync(FIG1), type: "png",
              transformation: { width: 240, height: 516 } })] }),
          new Paragraph({ alignment: AlignmentType.CENTER, children: [new TextRun(
            { text: "Figura 1 · Pantalla de origen: la principal (Inicio) con la barra de pestañas.", italics: true, size: 18, color: TENUE })] }),
        ] }),
      new TableCell({ borders: celdasBorde, verticalAlign: VerticalAlign.CENTER,
        children: [
          new Paragraph({ alignment: AlignmentType.CENTER, spacing: { after: 60 },
            children: [new ImageRun({ data: fs.readFileSync(FIG2), type: "png",
              transformation: { width: 240, height: 516 } })] }),
          new Paragraph({ alignment: AlignmentType.CENTER, children: [new TextRun(
            { text: "Figura 2 · Pantalla de destino: la página fantasma ya abierta.", italics: true, size: 18, color: TENUE })] }),
        ] }),
    ] })],
  }),
);

// 2. Pasos (el patrón)
contenido.push(
  H1("2. Pasos para entrar (el patrón)"),
  PAr("**Paso 0.** Abre UbiCafe y quédate en la pantalla principal; idealmente con la pestaña **Inicio** seleccionada (si se abrió en otra, toca Inicio una vez). Después, desde la barra inferior, toca **en este orden exacto** cada una de las pestañas de la siguiente tabla:"),
  tabla([2600, 6760], ["Paso", "Pestaña a tocar"],
    [
      ["1", "Inicio"], ["2", "Inicio"], ["3", "Inicio"],
      ["4", "Mapa"], ["5", "Mapa"], ["6", "Mapa"],
      ["7", "Explorar"], ["8", "Explorar"], ["9", "Explorar"],
      ["10", "Inicio"], ["11", "Mapa"], ["12", "Explorar"],
    ]),
  P(""),
  PAr("En resumen, el patrón completo es: **Inicio · Inicio · Inicio · Mapa · Mapa · Mapa · Explorar · Explorar · Explorar · Inicio · Mapa · Explorar**."),
  PAr("Encadenado por pasos, se ve así: **Inicio→Inicio→Inicio→Mapa→Mapa→Mapa→Explorar→Explorar→Explorar→Inicio→Mapa→Explorar**."),
  PAr("Consideraciones a tener en cuenta:"),
  P(""),
  new Paragraph({ numbering: { reference: "viñetas", level: 0 }, spacing: { after: 100, line: 288 },
    children: [new TextRun({ text: "El orden exacto es crítico: cuenta también el que la secuencia termine en I·M·E (Inicio, Mapa, Explorar)." })] }),
  new Paragraph({ numbering: { reference: "viñetas", level: 0 }, spacing: { after: 100, line: 288 },
    children: [new TextRun({ text: "Volver a tocar la pestaña ya activa también cuenta como toque (por eso Inicio se toca 3 veces seguidas y funciona)." })] }),
  new Paragraph({ numbering: { reference: "viñetas", level: 0 }, spacing: { after: 100, line: 288 },
    children: [new TextRun({ text: "No hay que «reiniciar» si te equivocas: la app recuerda los últimos 12 toques y, en cuanto coinciden con el patrón, abre la pantalla." })] }),
  new Paragraph({ numbering: { reference: "viñetas", level: 0 }, spacing: { after: 100, line: 288 },
    children: [new TextRun({ text: "La pestaña que queda seleccionada es siempre la del último toque (Explorar). Para salir de la pantalla fantasma, usa Atrás." })] }),
);

// 3. Código vital
contenido.push(
  H1("3. Fragmentos de código vitales (la lógica del patrón)"),
  PAr("Toda la lógica vive en **MainActivity.java**. El detector guarda el id de cada toque en una lista y lo compara con el patrón; cuando coincide, lanza GhostActivity."),
  PAr("**3.1 El patrón.** Constante con los ids de las pestañas, en el orden exacto de los 12 toques:"),
  codigo([
    "private static final int[] PATRON_OCULTO = {",
    "        R.id.menu_inicio, R.id.menu_inicio, R.id.menu_inicio,",
    "        R.id.menu_mapa, R.id.menu_mapa, R.id.menu_mapa,",
    "        R.id.menu_explorar, R.id.menu_explorar, R.id.menu_explorar,",
    "        R.id.menu_inicio, R.id.menu_mapa, R.id.menu_explorar,",
    "};",
  ]),
  PAr("**3.2 El detector de toques.** Cada toque de la barra se acumula en una ventana de 12; al completarse el patrón, se limpia la ventana y se abre la pantalla fantasma:"),
  codigo([
    "/** Últimos toques de la barra inferior (ventana del tamaño del patrón). */",
    "private final List<Integer> ultimosToques = new ArrayList<>();",
    "",
    "private void registrarToque(int idOpcion) {",
    "    ultimosToques.add(idOpcion);",
    "    if (ultimosToques.size() > PATRON_OCULTO.length) {",
    "        ultimosToques.remove(0);",
    "    }",
    "    if (esPatronOculto()) {",
    "        ultimosToques.clear();",
    "        startActivity(new Intent(this, GhostActivity.class));",
    "    }",
    "}",
    "",
    "private boolean esPatronOculto() {",
    "    if (ultimosToques.size() != PATRON_OCULTO.length) {",
    "        return false;",
    "    }",
    "    for (int i = 0; i < PATRON_OCULTO.length; i++) {",
    "        if (ultimosToques.get(i) != PATRON_OCULTO[i]) {",
    "            return false;",
    "        }",
    "    }",
    "    return true;",
    "}",
  ]),
  PAr("**3.3 El cableado en onCreate.** El listener se registra después de seleccionar Inicio (para no «contaminar» la ventana con el arranque) y llama al detector en cada toque, antes de cambiar de fragmento:"),
  codigo([
    "navegacion.setOnItemSelectedListener(menuItem -> {",
    "    registrarToque(menuItem.getItemId());   // primero: detector del patrón",
    "",
    "    Fragment destino = seleccionarFragmento(menuItem.getItemId());",
    "    if (destino != null) {",
    "        cambiarFragmento(destino);",
    "    }",
    "    return true;",
    "});",
  ]),
  PAr("**3.4 La pantalla fantasma.** GhostActivity solo infla su layout, pone el título «Desarrolladores» y conecta el botón volver:"),
  codigo([
    "protected void onCreate(Bundle savedInstanceState) {",
    "    super.onCreate(savedInstanceState);",
    "    setContentView(R.layout.activity_ghost);",
    "",
    "    ((TextView) findViewById(R.id.texto_titulo)).setText(R.string.ghost_titulo);",
    "    ((TextView) findViewById(R.id.texto_subtitulo)).setVisibility(View.GONE);",
    "",
    "    findViewById(R.id.btn_volver).setOnClickListener(v -> finish());",
    "}",
  ]),
  PAr("**¿Por qué «re-tocar» la pestaña activa cuenta?** BottomNavigationView notifica el listener cada vez que se toca un elemento, aunque sea el mismo ya seleccionado. Por eso la secuencia Inicio×3 y Explorar×3 se detecta sin problema."),
);

const num = {
  config: [
    { reference: "viñetas", levels: [
      { level: 0, format: LevelFormat.BULLET, text: "•", alignment: AlignmentType.LEFT,
        style: { paragraph: { indent: { left: 600, hanging: 280 } } } }] },
  ],
};

const doc = new Document({
  styles: estilos,
  numbering: num,
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