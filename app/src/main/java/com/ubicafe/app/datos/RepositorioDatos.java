package com.ubicafe.app.datos;

import android.annotation.SuppressLint;
import android.content.Context;

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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * PUNTO ÚNICO DE ACCESO A LOS DATOS
 * ---------------------------------------------------------------
 * Ninguna pantalla lee el JSON ni conoce al CargadorCenso: todas pasan
 * por aquí. Es la razón por la que, el día que haya un servidor, solo
 * haya que reescribir CargadorCenso y ni una pantalla cambie.
 *
 * Es una clase estática a propósito. El censo es de solo lectura, así
 * que no hay estado que sincronizar ni que proteger.
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
     * Deja el repositorio con acceso a los assets. Lo llama el
     * Application al arrancar, o el Splash si no hay una clase
     * Application propia. Hasta entonces, las consultas devuelven
     * listas vacías en vez de fallar.
     */
    public static void iniciar(Context contextoAplicacion) {
        contexto = contextoAplicacion.getApplicationContext();
    }

    /**
     * true si el censo está disponible. Las pantallas lo consultan antes
     * de pintar, para poder mostrar estado de carga o de error en vez de
     * una lista vacía sin explicación.
     */
    public static boolean hayDatos() {
        return censo() != null;
    }

    /** Los datos crudos, o null si todavía no se han cargado. */
    private static CargadorCenso.Censo censo() {
        return contexto == null ? null : CargadorCenso.cargar(contexto);
    }

    /**
     * Copia defensiva. El spec la pide explícitamente y el motivo es
     * concreto: una pantalla que ordene su lista in situ deformaría el
     * índice interno y la siguiente pantalla abriría con el orden roto.
     * Con 213 entidades, copiar cuesta menos que un milisegundo.
     */
    private static <T> List<T> copia(List<T> original) {
        return new ArrayList<>(original);
    }

    // ------------------------------------------------------------------
    // Entidades
    // ------------------------------------------------------------------

    /** Los 213 lugares del censo, en el orden en que los registró. */
    public static List<Entidad> obtenerEntidades() {
        CargadorCenso.Censo censo = censo();
        return censo == null ? Collections.<Entidad>emptyList() : copia(censo.entidades);
    }

    /**
     * Los lugares que tienen un papel concreto, sin importar cuál sea su
     * rol principal. Devuelve 189 cafeterías, 25 marcas, 13 tostadurías,
     * 12 productores, 6 tiendas y 17 otros, y un lugar con dos papeles
     * aparece en las dos listas.
     */
    public static List<Entidad> obtenerEntidadesPorRol(Rol rol) {
        CargadorCenso.Censo censo = censo();
        if (censo == null) {
            return Collections.emptyList();
        }
        List<Entidad> resultado = censo.porRol.get(rol);
        return resultado == null ? Collections.<Entidad>emptyList() : copia(resultado);
    }

    /**
     * Los lugares cuyo papel principal es este. A diferencia del
     * anterior, no hay repeticiones: sirve para el mapa y para los
     * listados, donde cada lugar debe aparecer una sola vez.
     */
    public static List<Entidad> obtenerPrincipalesPorRol(Rol rol) {
        List<Entidad> resultado = new ArrayList<>();
        for (Entidad entidad : obtenerEntidades()) {
            if (entidad.rolPrincipal == rol) {
                resultado.add(entidad);
            }
        }
        return resultado;
    }

    public static List<Entidad> obtenerEntidadesPorMacrodistrito(String macrodistrito) {
        CargadorCenso.Censo censo = censo();
        if (censo == null || macrodistrito == null || macrodistrito.isEmpty()) {
            return obtenerEntidades();
        }
        List<Entidad> resultado = censo.porMacrodistrito.get(macrodistrito);
        return resultado == null ? Collections.<Entidad>emptyList() : copia(resultado);
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
        CargadorCenso.Censo censo = censo();
        return censo == null ? null : censo.porId.get(id);
    }

    /** Una entidad por su nombre, ignorando mayúsculas y tildes. */
    public static Entidad obtenerEntidadPorNombre(String nombre) {
        String clave = Texto.clave(nombre);
        for (Entidad entidad : obtenerEntidades()) {
            if (Texto.clave(entidad.nombre).equals(clave)) {
                return entidad;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------
    // Marcas y puntos de venta
    // ------------------------------------------------------------------

    public static List<Marca> obtenerMarcas() {
        CargadorCenso.Censo censo = censo();
        return censo == null ? Collections.<Marca>emptyList() : copia(censo.marcas);
    }

    /** Una marca por su nombre, ignorando mayúsculas y tildes. */
    public static Marca obtenerMarca(String nombre) {
        CargadorCenso.Censo censo = censo();
        return censo == null ? null : censo.marcaPorClave.get(Texto.clave(nombre));
    }

    /** La cadena a la que pertenece un local, o null si es independiente. */
    public static Marca obtenerMarcaDe(String entidadId) {
        CargadorCenso.Censo censo = censo();
        return censo == null ? null : censo.marcaDeLocal.get(entidadId);
    }

    /** Los 78 puntos de venta del censo, con la marca a la que pertenecen. */
    public static List<Sucursal> obtenerSucursales() {
        List<Sucursal> todas = new ArrayList<>();
        for (Marca marca : obtenerMarcas()) {
            for (Sucursal sucursal : marca.sucursales) {
                todas.add(sucursal);
            }
        }
        return todas;
    }

    public static List<Sucursal> obtenerSucursalesDe(String marca) {
        CargadorCenso.Censo censo = censo();
        if (censo == null || marca == null || marca.isEmpty()) {
            return Collections.emptyList();
        }
        List<Sucursal> resultado = censo.sucursalesPorMarca.get(Texto.clave(marca));
        return resultado == null ? Collections.<Sucursal>emptyList() : copia(resultado);
    }

    /**
     * Los locales de una cadena, pero como entidades del mapa, para que
     * "ver en el mapa" pueda dibujar marcadores.
     */
    public static List<Entidad> obtenerEntidadesDeMarca(String marca) {
        CargadorCenso.Censo censo = censo();
        if (censo == null) {
            return Collections.emptyList();
        }
        List<Entidad> resultado = censo.porMarcaAsociada.get(Texto.clave(marca));
        return resultado == null ? Collections.<Entidad>emptyList() : copia(resultado);
    }

    /** Cuántos locales tiene una cadena. */
    public static int contarSucursalesDe(String marca) {
        return obtenerSucursalesDe(marca).size();
    }

    /**
     * Los locales de una entidad concreta, por su identificador. El spec
     * pide este método para las fichas de detalle, donde ya se tiene el
     * id y no el nombre de la cadena.
     */
    public static List<Sucursal> obtenerSucursalesDeEntidad(String entidadId) {
        List<Sucursal> resultado = new ArrayList<>();
        for (Sucursal sucursal : obtenerSucursales()) {
            if (sucursal.entidadId.equals(entidadId)) {
                resultado.add(sucursal);
            }
        }
        return resultado;
    }

    // ------------------------------------------------------------------
    // Cafés de origen
    // ------------------------------------------------------------------

    public static List<CafeVariedad> obtenerVariedades() {
        CargadorCenso.Censo censo = censo();
        return censo == null ? Collections.<CafeVariedad>emptyList() : copia(censo.variedades);
    }

    /** Los cafés de origen de una marca concreta. */
    public static List<CafeVariedad> obtenerVariedadesDe(String marca) {
        String clave = Texto.clave(marca);
        List<CafeVariedad> resultado = new ArrayList<>();
        for (CafeVariedad variedad : obtenerVariedades()) {
            if (Texto.clave(variedad.marca).equals(clave)) {
                resultado.add(variedad);
            }
        }
        return resultado;
    }

    public static CafeVariedad obtenerVariedadPorNombre(String nombre) {
        String clave = Texto.clave(nombre);
        for (CafeVariedad variedad : obtenerVariedades()) {
            if (Texto.clave(variedad.nombre).equals(clave)) {
                return variedad;
            }
        }
        return null;
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
        Map<String, Map<String, Integer>> porClave = new LinkedHashMap<>();
        for (CafeVariedad variedad : obtenerVariedades()) {
            if (variedad.region.isEmpty()) {
                continue;
            }
            String clave = Texto.clave(variedad.region);
            porClave.computeIfAbsent(clave, k -> new LinkedHashMap<>())
                    .merge(variedad.region, 1, Integer::sum);
        }

        Map<String, Integer> regiones = new LinkedHashMap<>();
        for (Map<String, Integer> grafias : porClave.values()) {
            int total = 0;
            String masUsada = "";
            for (Map.Entry<String, Integer> grafia : grafias.entrySet()) {
                total += grafia.getValue();
                if (grafia.getValue() > grafias.get(masUsada)) {
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
     * La comparación recorre todos los lugares, menos de 300 en total.
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
     */
    public static List<Entidad> obtenerEntidadesCercanas(double lat, double lng, int limite) {
        List<Entidad> candidatas = new ArrayList<>();
        for (Entidad entidad : obtenerEntidades()) {
            if (entidad.tieneCoordenadas()) {
                candidatas.add(entidad);
            }
        }
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
        CargadorCenso.Censo censo = censo();
        return censo == null ? "" : censo.fuente;
    }

    public static int obtenerAnioCenso() {
        CargadorCenso.Censo censo = censo();
        return censo == null ? 0 : censo.anio;
    }
}
