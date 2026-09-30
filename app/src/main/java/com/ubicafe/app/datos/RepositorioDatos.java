package com.ubicafe.app.datos;

import android.annotation.SuppressLint;
import android.content.Context;

import com.ubicafe.app.datos.room.Traductor;
import com.ubicafe.app.datos.room.UbiCafeBaseDatos;
import com.ubicafe.app.modelo.CafeVariedad;
import com.ubicafe.app.modelo.Entidad;
import com.ubicafe.app.modelo.Estadisticas;
import com.ubicafe.app.modelo.Marca;
import com.ubicafe.app.modelo.ResultadoBusqueda;
import com.ubicafe.app.modelo.Rol;
import com.ubicafe.app.modelo.Sucursal;
import com.ubicafe.app.util.Distancia;
import com.ubicafe.app.util.Texto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * PUNTO ÚNICO DE ACCESO A LOS DATOS
 * ---------------------------------------------------------------
 * Ninguna pantalla habla con la base de datos ni sabe qué hay debajo:
 * todas pasan por aquí. Es la razón por la que, el día que haya un
 * servidor, solo haya que reescribir esta clase (o el DAO que usa) y ni
 * una pantalla cambie.
 *
 * QUÉ HAY DEBAJO AHORA
 * La base de datos es SQLite, a través de Room: las tablas están
 * declaradas con anotaciones en el paquete datos.room y las consultas son
 * SQL escrito en los DAO. Este JSON de assets ya no se lee para pintar
 * nada: SembradorCenso lo lee una única vez, al primer arranque, para
 * llenar las tablas, y a partir de ahí todo sale de la base.
 *
 * Se mantiene el patrón de listas de la versión anterior (copia
 * defensiva, y las listas que se devuelven no se tocan) porque las
 * pantallas ordenan y filtran lo que reciben. Copiar 213 objetos cuesta
 * microsegundos y evita que una pantalla que ordene su lista en situ
 * descoloque a la siguiente.
 *
 * Todas las listas que devuelve son copias. Quien llama puede ordenar o
 * filtrar su resultado sin que otras pantallas lo noterán, y los datos
 * de base no se deforman nunca.
 */
public final class RepositorioDatos {

    // Lint marca esto como fuga, pero se guarda el contexto de la
    // aplicación, no el de una Activity: ese no se destruye nunca, así
    // que no hay nada que collectar. Se silencia a mano para que el
    // aviso no tape los que sí importan.
    @SuppressLint("StaticFieldLeak")
    private static Context contexto;
    private static Estadisticas estadisticas;

    private RepositorioDatos() {
        // Clase de servicios: se accede por los métodos estáticos.
    }

    /**
     * Deja el repositorio con acceso a la base. Lo llama el
     * Application al arrancar, o el Splash si no hay una clase
     * Application propia. Hasta entonces, las consultas devuelven
     * listas vacías en vez de fallar.
     */
    public static void iniciar(Context contextoAplicacion) {
        contexto = contextoAplicacion.getApplicationContext();
    }

    // ------------------------------------------------------------------
    // La base
    // ------------------------------------------------------------------

    /**
     * true si el censo está disponible. Las pantallas lo consultan antes
     * de pintar, para poder mostrar estado de carga o de error en vez de
     * una lista vacía sin explicación.
     */
    public static boolean hayDatos() {
        if (contexto == null) {
            return false;
        }
        UbiCafeBaseDatos base = base();
        return base != null && base.entidades().contar() > 0;
    }

    /**
     * Abre la base y, si está vacía, la llena desde el JSON.
     *
     * SembradorCenso solo hace trabajo la primera vez: en los arranques
     * siguientes se limita a leer el sello de la tabla meta y ver que
     * coincide con el JSON de los assets, lo cual es una consulta de
     * un milisegundo.
     */
    private static UbiCafeBaseDatos base() {
        if (contexto == null) {
            return null;
        }
        if (!SembradorCenso.sembrarSiFalta(contexto)) {
            return null;
        }
        return UbiCafeBaseDatos.obtener(contexto);
    }

    /** La fila de metadatos, o null si todavía no hay nada sembrado. */
    private static com.ubicafe.app.datos.room.FilaMeta meta() {
        UbiCafeBaseDatos base = base();
        return base == null ? null : base.meta().leer();
    }

    /**
     * Copia defensiva. El spec la pide explícitamente y el motivo es
     * concreto: una pantalla que ordene su lista in situ deformaría el
     * resultado interno y la siguiente pantalla abriría con el orden
     * roto. Con 213 entidades, copiar cuesta menos que un milisegundo.
     */
    private static <T> List<T> copia(List<T> original) {
        return new ArrayList<>(original);
    }

    // ------------------------------------------------------------------
    // Entidades
    // ------------------------------------------------------------------

    /** Los 213 lugares del censo, en el orden en que los registró. */
    public static List<Entidad> obtenerEntidades() {
        UbiCafeBaseDatos base = base();
        if (base == null) {
            return Collections.emptyList();
        }
        return copia(Traductor.entidades(base, base.entidades().todas()));
    }

    /**
     * Los lugares que tienen un papel concreto, sin importar cuál sea su
     * rol principal. Devuelve 189 cafeterías, 25 marcas, 13 tostadurías,
     * 12 productores, 6 tiendas y 17 otros, y un lugar con dos papeles
     * aparece en las dos listas.
     *
     * La consulta es un INNER JOIN con la tabla de roles: por eso un
     * local con dos papeles sale dos veces, una en cada lista, sin
     * duplicar filas dentro de la misma.
     */
    public static List<Entidad> obtenerEntidadesPorRol(Rol rol) {
        UbiCafeBaseDatos base = base();
        if (base == null) {
            return Collections.emptyList();
        }
        return copia(Traductor.entidades(base, base.entidades().porRol(rol.name())));
    }

    /**
     * Los lugares cuyo papel principal es este. A diferencia del
     * anterior, no hay repeticiones: sirve para el mapa y para los
     * listados, donde cada lugar debe aparecer una sola vez.
     */
    public static List<Entidad> obtenerPrincipalesPorRol(Rol rol) {
        UbiCafeBaseDatos base = base();
        if (base == null) {
            return Collections.emptyList();
        }
        return copia(Traductor.entidades(base, base.entidades().porRolPrincipal(rol.name())));
    }

    public static List<Entidad> obtenerEntidadesPorMacrodistrito(String macrodistrito) {
        UbiCafeBaseDatos base = base();
        if (base == null) {
            return Collections.emptyList();
        }
        if (macrodistrito == null || macrodistrito.isEmpty()) {
            return obtenerEntidades();
        }
        return copia(Traductor.entidades(base, base.entidades().porMacrodistrito(macrodistrito)));
    }

    /**
     * Cuántos lugares de un rol hay en cada macrodistrito. Lo usa la
     * pestaña Ecosistema para dibujar las barras proporcionales. Se
     * cuenta sobre los datos y no sobre una lista escrita a mano, porque
     * el censo tiene 213 lugares en 20 zonas y cualquiera se equivoca al
     * contarlos a mano.
     */
    public static Map<String, Integer> obtenerConteoPorMacrodistrito(Rol rol) {
        Map<String, Integer> conteo = new LinkedHashMap<>();
        for (Entidad entidad : obtenerEntidadesPorRol(rol)) {
            if (entidad.macrodistrito.isEmpty()) {
                continue;
            }
            conteo.merge(entidad.macrodistrito, 1, Integer::sum);
        }
        return conteo;
    }

    /** Una entidad por su identificador, o null si no existe. */
    public static Entidad obtenerEntidad(String id) {
        UbiCafeBaseDatos base = base();
        if (base == null || id == null) {
            return null;
        }
        return Traductor.entidad(base, base.entidades().porId(id));
    }

    /** Una entidad por su nombre, ignorando mayúsculas y tildes. */
    public static Entidad obtenerEntidadPorNombre(String nombre) {
        UbiCafeBaseDatos base = base();
        if (base == null || nombre == null) {
            return null;
        }
        return Traductor.entidad(base, base.entidades().porNombreClave(Texto.clave(nombre)));
    }

    // ------------------------------------------------------------------
    // Marcas y puntos de venta
    // ------------------------------------------------------------------

    public static List<Marca> obtenerMarcas() {
        UbiCafeBaseDatos base = base();
        if (base == null) {
            return Collections.emptyList();
        }
        return copia(Traductor.marcas(
                base.marcas().todas(), base.detalles().todasSucursales()));
    }

    /** Una marca por su nombre, ignorando mayúsculas y tildes. */
    public static Marca obtenerMarca(String nombre) {
        UbiCafeBaseDatos base = base();
        if (base == null || nombre == null) {
            return null;
        }
        com.ubicafe.app.datos.room.FilaMarca fila =
                base.marcas().porClave(Texto.clave(nombre));
        if (fila == null) {
            return null;
        }
        List<Marca> una = Traductor.marcas(
                Collections.singletonList(fila), base.detalles().todasSucursales());
        return una.isEmpty() ? null : una.get(0);
    }

    /**
     * La cadena a la que pertenece un local, o null si es independiente.
     *
     * La inversa del índice anterior: se pregunta por el id del local y
     * se responde con la marca. La tabla de puntos de venta tiene el id
     * del local como clave primaria, así que es una lectura directa.
     */
    public static Marca obtenerMarcaDe(String entidadId) {
        UbiCafeBaseDatos base = base();
        if (base == null || entidadId == null) {
            return null;
        }
        com.ubicafe.app.datos.room.FilaSucursal sucursal =
                base.detalles().sucursalDeEntidad(entidadId);
        if (sucursal == null) {
            return null;
        }
        com.ubicafe.app.datos.room.FilaMarca fila = base.marcas().porClave(sucursal.marcaClave);
        if (fila == null) {
            return null;
        }
        List<Marca> una = Traductor.marcas(
                Collections.singletonList(fila), base.detalles().todasSucursales());
        return una.isEmpty() ? null : una.get(0);
    }

    /** Los 78 puntos de venta del censo, con la marca a la que pertenecen. */
    public static List<Sucursal> obtenerSucursales() {
        UbiCafeBaseDatos base = base();
        if (base == null) {
            return Collections.emptyList();
        }
        return Traductor.sucursales(base.detalles().todasSucursales());
    }

    public static List<Sucursal> obtenerSucursalesDe(String marca) {
        UbiCafeBaseDatos base = base();
        if (base == null || marca == null || marca.isEmpty()) {
            return Collections.emptyList();
        }
        return Traductor.sucursales(base.detalles().sucursalesDeMarca(Texto.clave(marca)));
    }

    /**
     * Los locales de una cadena, pero como entidades del mapa, para que
     * "ver en el mapa" pueda dibujar marcadores.
     */
    public static List<Entidad> obtenerEntidadesDeMarca(String marca) {
        UbiCafeBaseDatos base = base();
        if (base == null || marca == null) {
            return Collections.emptyList();
        }
        return copia(Traductor.entidades(base, base.entidades().porMarca(Texto.clave(marca))));
    }

    /** Cuántos locales tiene una cadena. */
    public static int contarSucursalesDe(String marca) {
        UbiCafeBaseDatos base = base();
        if (base == null || marca == null || marca.isEmpty()) {
            return 0;
        }
        // Cuenta en la base en vez de traer la lista y medirla: la
        // respuesta es un entero y no hace falta cargar 6 filas.
        return base.detalles().contarSucursalesDeMarca(Texto.clave(marca));
    }

    /**
     * Los locales de una entidad concreta, por su identificador. El spec
     * pide este método para las fichas de detalle, donde ya se tiene el
     * id y no el nombre de la cadena.
     */
    public static List<Sucursal> obtenerSucursalesDeEntidad(String entidadId) {
        UbiCafeBaseDatos base = base();
        if (base == null || entidadId == null) {
            return Collections.emptyList();
        }
        com.ubicafe.app.datos.room.FilaSucursal fila =
                base.detalles().sucursalDeEntidad(entidadId);
        return fila == null
                ? Collections.<Sucursal>emptyList()
                : Collections.singletonList(Traductor.sucursal(fila));
    }

    // ------------------------------------------------------------------
    // Cafés de origen
    // ------------------------------------------------------------------

    public static List<CafeVariedad> obtenerVariedades() {
        UbiCafeBaseDatos base = base();
        if (base == null) {
            return Collections.emptyList();
        }
        return copia(Traductor.variedades(base.variedades().todas()));
    }

    /** Los cafés de origen de una marca concreta. */
    public static List<CafeVariedad> obtenerVariedadesDe(String marca) {
        UbiCafeBaseDatos base = base();
        if (base == null || marca == null) {
            return Collections.emptyList();
        }
        return copia(Traductor.variedades(base.variedades().porMarca(Texto.clave(marca))));
    }

    public static CafeVariedad obtenerVariedadPorNombre(String nombre) {
        UbiCafeBaseDatos base = base();
        if (base == null || nombre == null) {
            return null;
        }
        List<com.ubicafe.app.datos.room.FilaVariedad> filas =
                base.variedades().porNombreClave(Texto.clave(nombre));
        if (filas.isEmpty()) {
            return null;
        }
        return Traductor.variedad(filas.get(0));
    }

    /**
     * Las regiones cafetaleras del censo con cuántas variedades se les
     * atribuyen, y en el orden en que aparecen.
     *
     * Se agrupa por clave sin tildes ni mayúsculas porque el censo
     * escribe la misma región de tres formas: "Caranavi",
     * "CARANAVI LOCALIDAD ILLAMANI - SAN JOSE" y "Caranavi, La Paz".
     * Aplanarlas a mano sería decidir qué cuenta como la misma región
     * sin saberlo, así que se cuentan tal cual y solo se agrupan
     * mayúsculas y tildes, que no cambian el nombre. De cada grupo se
     * muestra la grafía más repetida, que es la que se lee bien.
     *
     * Una región con una sola variedad no aparece: el campo es texto
     * libre ("Taypi Playa", "Max Paredes"), no un catálogo, y listar eso
     * como región cafetalera sería inventarle una categoría.
     */
    public static Map<String, Integer> obtenerRegionesCafetaleras() {
        UbiCafeBaseDatos base = base();
        if (base == null) {
            return Collections.emptyMap();
        }

        // La clave normalizada de cada región ya está en la base, en la
        // columna region_clave, calculada al sembrar. Antes se
        // normalizaba aquí, en cada llamada.
        Map<String, Map<String, Integer>> porClave = new LinkedHashMap<>();
        for (com.ubicafe.app.datos.room.FilaVariedad fila : base.variedades().todas()) {
            if (fila.region == null || fila.region.isEmpty()) {
                continue;
            }
            porClave.computeIfAbsent(fila.regionClave, k -> new LinkedHashMap<>())
                    .merge(fila.region, 1, Integer::sum);
        }

        Map<String, Integer> regiones = new LinkedHashMap<>();
        for (Map<String, Integer> grafias : porClave.values()) {
            int total = 0;
            String masUsada = null;
            for (Map.Entry<String, Integer> grafia : grafias.entrySet()) {
                total += grafia.getValue();
                if (masUsada == null || grafia.getValue() > grafias.get(masUsada)) {
                    masUsada = grafia.getKey();
                }
            }
            if (total >= 2) {
                regiones.put(masUsada, total);
            }
        }
        return regiones;
    }

    // ------------------------------------------------------------------
    // Búsqueda
    // ------------------------------------------------------------------

    /**
     * Busca en todo el censo, sin distinguir mayúsculas ni acentos.
     *
     * Devuelve los resultados agrupados por sección porque "cafe" tiene
     * que dar cafeterías, marcas, tostadurías y productores a la vez, y
     * mostrarlos mezclados no ayuda: cada tipo de dato se ve distinto.
     *
     * La comparación se hace en memoria y no con un "LIKE" de SQL, y es
     * a propósito. Un LIKE necesita preparar la consulta con los
     * comodines y no usaría ningún índice; además, el campo que se
     * busca está repartido entre el local, su macrodistrito y hasta
     * cuatro fichas, así que la alternativa sería unir nueve tablas.
     * Con 213 locales menos de 300 filas en total, traerlas y recorrerlas
     * es más rápido y más sencillo.
     *
     * Con un índice invertido se ganaría poco y se perdería la
     * tolerancia a erratas, que aquí conviene: el censo escribe
     * "Coffe", "Cofee" y "Coffe" para lo mismo, y quien busca "coffee"
     * debería encontrarlos.
     */
    public static ResultadoBusqueda buscar(String texto) {
        String consulta = Texto.claveDeConsulta(texto);
        if (consulta.isEmpty()) {
            return ResultadoBusqueda.vacio();
        }

        List<CafeVariedad> cafes = new ArrayList<>();
        List<Marca> marcas = new ArrayList<>();
        List<Entidad> establecimientos = new ArrayList<>();
        List<Entidad> productores = new ArrayList<>();
        List<Entidad> tostadurias = new ArrayList<>();

        for (Entidad entidad : obtenerEntidades()) {
            if (!coincide(entidad, consulta)) {
                continue;
            }
            // Un lugar va a la sección de su papel principal, para que
            // "Tostadurías" no muestre las 13 cafeterías que además
            // tuestan.
            switch (entidad.rolPrincipal) {
                case TOSTADURIA:
                    tostadurias.add(entidad);
                    break;
                case PRODUCTOR:
                    productores.add(entidad);
                    break;
                default:
                    establecimientos.add(entidad);
                    break;
            }
        }

        for (Marca marca : obtenerMarcas()) {
            if (Texto.clave(marca.nombre).contains(consulta)
                    || Texto.clave(marca.nota).contains(consulta)) {
                marcas.add(marca);
            }
        }

        for (CafeVariedad variedad : obtenerVariedades()) {
            if (Texto.clave(variedad.nombre).contains(consulta)
                    || Texto.clave(variedad.variedadesDeclaradas).contains(consulta)
                    || Texto.clave(variedad.region).contains(consulta)
                    || Texto.clave(variedad.marca).contains(consulta)) {
                cafes.add(variedad);
            }
        }

        return new ResultadoBusqueda(cafes, marcas, establecimientos,
                productores, tostadurias);
    }

    /**
     * true si el lugar encaja con la búsqueda. Se mira el nombre, la
     * dirección, el macrodistrito, la cadena a la que pertenece, el
     * nombre de sus fichas y las variedades y notas que registró.
     */
    private static boolean coincide(Entidad entidad, String consulta) {
        return Texto.clave(entidad.nombre).contains(consulta)
                || Texto.clave(entidad.direccion).contains(consulta)
                || Texto.clave(entidad.macrodistrito).contains(consulta)
                || Texto.clave(entidad.marcaAsociada).contains(consulta)
                || coincideDetalle(entidad, consulta);
    }

    private static boolean coincideDetalle(Entidad entidad, String consulta) {
        if (entidad.detalleCafeteria != null
                && (Texto.clave(entidad.detalleCafeteria.tipoEstablecimiento).contains(consulta)
                || Texto.clave(entidad.detalleCafeteria.tipoApp).contains(consulta))) {
            return true;
        }
        if (entidad.detalleTostaderia != null
                && (Texto.clave(entidad.detalleTostaderia.regionesOrigen).contains(consulta)
                || Texto.clave(entidad.detalleTostaderia.tiposTueste).contains(consulta))) {
            return true;
        }
        if (entidad.detalleProductor != null
                && (Texto.clave(entidad.detalleProductor.especies).contains(consulta)
                || Texto.clave(entidad.detalleProductor.variedades).contains(consulta)
                || Texto.clave(entidad.detalleProductor.comunidadMunicipio).contains(consulta))) {
            return true;
        }
        return entidad.detalleMarca != null
                && Texto.clave(entidad.detalleMarca.segmento).contains(consulta);
    }

    // ------------------------------------------------------------------
    // Cercanía y estadísticas
    // ------------------------------------------------------------------

    /**
     * Los lugares con coordenadas más cerca de un punto, ordenados por
     * distancia. Los que no tienen coordenadas se descartan: no se
     * puede ordenar por una distancia que no existe.
     *
     * La distancia se calcula en Java y no en SQL porque SQLite no trae
     * fórmula de haversine: se tendría que escribirla entera en la
     * consulta. Con 213 candidatos, mostrarlos ya filtrados por
     * coordenada y ordenarlos en memoria es lo más simple.
     */
    public static List<Entidad> obtenerEntidadesCercanas(double lat, double lng, int limite) {
        UbiCafeBaseDatos base = base();
        if (base == null) {
            return Collections.emptyList();
        }
        List<Entidad> candidatas = Traductor.entidades(base, base.entidades().conCoordenadas());
        Collections.sort(candidatas, (una, otra) -> Double.compare(
                Distancia.entre(una.lat, una.lng, lat, lng),
                Distancia.entre(otra.lat, otra.lng, lat, lng)));

        if (candidatas.size() <= limite) {
            return candidatas;
        }
        return copia(candidatas.subList(0, limite));
    }

    /**
     * Las cifras del sector, calculadas una vez y recordadas. Todas las
     * pantallas las piden; recorrer 213 entidades en cada llamada sería
     * tirar el cálculo para siempre.
     */
    public static Estadisticas obtenerEstadisticas() {
        if (estadisticas == null && hayDatos()) {
            estadisticas = new Estadisticas(obtenerEntidades(), obtenerMarcas(),
                    obtenerVariedades());
        }
        return estadisticas;
    }

    /** El texto de procedencia que se muestra al pie del ecosistema. */
    public static String obtenerFuente() {
        com.ubicafe.app.datos.room.FilaMeta meta = meta();
        return meta == null || meta.fuente == null ? "" : meta.fuente;
    }

    public static int obtenerAnioCenso() {
        com.ubicafe.app.datos.room.FilaMeta meta = meta();
        return meta == null ? 0 : meta.anio;
    }

    // ------------------------------------------------------------------
    // Solo para pruebas
    // ------------------------------------------------------------------

    /**
     * Las cifras que hay ahora mismo en la base, para comparar con lo que
     * dice el JSON. No lo usa ninguna pantalla: existe para poder
     * comprobar, desde un terminal o un test, que la base se llenó
     * igual que el archivo.
     */
    public static Map<String, Integer> contarFilas() {
        Map<String, Integer> filas = new HashMap<>();
        UbiCafeBaseDatos base = base();
        if (base == null) {
            return filas;
        }
        filas.put("entidades", base.entidades().contar());
        filas.put("entidades_roles", base.roles().contar());
        filas.put("marcas", base.marcas().contar());
        filas.put("variedades", base.variedades().contar());
        filas.put("sucursales", base.detalles().todasSucursales().size());
        filas.put("detalle_cafeteria", base.detalles().contarFichasCafeteria());
        filas.put("detalle_marca_entidad", base.detalles().contarFichasMarca());
        filas.put("detalle_tostaderia", base.detalles().contarFichasTostaderia());
        filas.put("detalle_productor", base.detalles().contarFichasProductor());
        return filas;
    }
}
