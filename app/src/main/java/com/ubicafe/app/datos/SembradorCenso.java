package com.ubicafe.app.datos;

import android.content.Context;
import android.util.Log;

import com.ubicafe.app.datos.room.DaoDetalles;
import com.ubicafe.app.datos.room.DaoEntidades;
import com.ubicafe.app.datos.room.DaoMarcas;
import com.ubicafe.app.datos.room.DaoMeta;
import com.ubicafe.app.datos.room.DaoRoles;
import com.ubicafe.app.datos.room.DaoVariedades;
import com.ubicafe.app.datos.room.FilaDetalleCafeteria;
import com.ubicafe.app.datos.room.FilaDetalleMarcaEntidad;
import com.ubicafe.app.datos.room.FilaDetalleProductor;
import com.ubicafe.app.datos.room.FilaDetalleTostaderia;
import com.ubicafe.app.datos.room.FilaEntidad;
import com.ubicafe.app.datos.room.FilaMarca;
import com.ubicafe.app.datos.room.FilaMeta;
import com.ubicafe.app.datos.room.FilaRol;
import com.ubicafe.app.datos.room.FilaSucursal;
import com.ubicafe.app.datos.room.FilaVariedad;
import com.ubicafe.app.datos.room.Traductor;
import com.ubicafe.app.datos.room.UbiCafeBaseDatos;
import com.ubicafe.app.util.Texto;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

/**
 * LLENA LA BASE DE DATOS LA PRIMERA VEZ
 * ---------------------------------------------------------------
 * El censo sigue llegando como un archivo JSON en los assets, que es lo
 * que produce tools/generar_censo.py desde el Excel. EsteSeeder es el
 * puente: lee el JSON una única vez, lo pasa a las tablas de Room y a
 * partir de ahí la app lee de la base, no del archivo.
 *
 * Por qué no se copia el .db ya lleno dentro de los assets: un archivo
 * .db es un binario que no se puede revisar en un diff, y cualquier
 * cambio en el censo obligaría a regenerarlo además de regenerar el
 * JSON. Con el JSON como única fuente, hay un solo artefacto que
 * mantener y la base siempre está coherente con él.
 *
 * CUÁNDO SE EJECUTA
 * Solo la primera vez. Después la base se lee de ella misma. Si el JSON
 * cambia (por ejemplo, el censo del año que viene), el SELLO guardado en
 * la tabla meta deja de coincidir, se detecta al arrancar y se vuelve a
 * llenar. Ese es el motivo de guardar el sello: sin él, quien actualizase
 * el JSON vería las listas viejas y no sabría por qué.
 *
 * Va en una transacción: o se escriben las 10 tablas enteras, o no se
 * escribe ninguna. Un fallo a mitad (el teléfono se apaga, la app se
 * mata) deja la base como estaba, no con 200 lugares y sin marcas.
 */
public final class SembradorCenso {

    private static final String ETIQUETA = "SembradorCenso";
    private static final String ARCHIVO_CENSO = "censo_ubicafe.json";
    private static final String ARCHIVO_FOTOS = "imagenes.json";
    private static final String CARPETA_FOTOS = "fotos/";

    private SembradorCenso() {
        // Clase de servicios: se accede por los métodos estáticos.
    }

    /**
     * Ya se comprobó en este proceso que la base está al día. Se evita
     * repetir la consulta a la tabla meta en cada llamada al repositorio:
     * las pantallas piden los datos muchas veces y el sello del JSON no
     * puede cambiar mientras la app está abierta (los assets son de
     * solo lectura).
     */
    private static volatile boolean verificadoEnEsteProceso;

    /**
     * Llena la base si hace falta, y devuelve si el censo quedó
     * disponible. Se puede llamar en cada arranque: si ya está, no hace
     * nada más que una lectura de la tabla meta.
     */
    public static synchronized boolean sembrarSiFalta(Context contexto) {
        if (verificadoEnEsteProceso) {
            return true;
        }
        try {
            UbiCafeBaseDatos base = UbiCafeBaseDatos.obtener(contexto);

            // El JSON se lee una sola vez: 180 KB no son nada, pero
            // parsearlo dos veces por arranque es tirar trabajo.
            String contenido = textoDe(contexto, ARCHIVO_CENSO);
            JSONObject raiz = new JSONObject(contenido);
            String sello = calcularSello(contenido);

            FilaMeta meta = base.meta().leer();
            if (meta != null && sello.equals(meta.sello)) {
                verificadoEnEsteProceso = true;
                return true;
            }
            llenar(base, contexto, raiz, sello);
            verificadoEnEsteProceso = true;
            return true;
        } catch (IOException | JSONException | RuntimeException problema) {
            // Un JSON corrupto o una base bloqueada no deben tumbar la app
            // al abrir: se avisa y las pantallas mostrarán su estado de
            // error con un botón de reintento. verificadoEnEsteProceso
            // sigue en false, así que el próximo intento reintenta.
            Log.e(ETIQUETA, "No se pudo llenar la base con " + ARCHIVO_CENSO, problema);
            return false;
        }
    }

    /**
     * El sello del JSON con el que se llenó la base: la huella SHA-256 de
     * su contenido, en hexadecimal.
     *
     * Antes esto era "tamaño en bytes + número de registros", y estaba
     * mal: un cambio de texto que no altere ni el tamaño ni el total
     * (por ejemplo corregir una dirección mal escrita, o cambiar el
     * nombre de una marca por otro de la misma longitud) dejaba el
     * sello igual y la app seguía enseñando el dato viejo sin avisar.
     * Con la huella, cualquier diferencia de un solo carácter cambia el
     * sello y obliga a volver a sembrar.
     *
     * SHA-256 está disponible en todas las versiones de Android que
     * soporta la app, así que si algún día fallara sería un problema
     * del sistema, no de este código.
     */
    private static String calcularSello(String contenido) {
        byte[] huella;
        try {
            huella = MessageDigest.getInstance("SHA-256")
                    .digest(contenido.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException imposible) {
            throw new IllegalStateException("Android sin SHA-256", imposible);
        }
        StringBuilder hexadecimal = new StringBuilder(huella.length * 2);
        for (byte octeto : huella) {
            hexadecimal.append(Character.forDigit((octeto >> 4) & 0xF, 16));
            hexadecimal.append(Character.forDigit(octeto & 0xF, 16));
        }
        return hexadecimal.toString();
    }

    private static void llenar(UbiCafeBaseDatos base, Context contexto,
                               JSONObject raiz, String sello)
            throws IOException, JSONException {

        JSONObject meta = raiz.getJSONObject("meta");
        JSONObject fotos = leerFotos(contexto);

        DaoEntidades daoEntidades = base.entidades();
        DaoRoles daoRoles = base.roles();
        DaoDetalles daoDetalles = base.detalles();
        DaoMarcas daoMarcas = base.marcas();
        DaoVariedades daoVariedades = base.variedades();
        DaoMeta daoMeta = base.meta();

        // Antes de tocar la base se lee y se convierte TODO el JSON en
        // memoria. Es a propósito: si algo del JSON viniera roto, el
        // fallo saldría aquí, antes de borrar nada, y la base de la
        // instalación anterior se quedaría intacta. Si se borrara
        // primero y se parseara después, un solo campo mal formado
        // dejaría la app con la lista vacía y sin forma de recuperarse
        // sin reinstalar.
        int orden = 0;
        int ordenSucursal = 0;
        JSONArray entidades = raiz.getJSONArray("entidades");
        List<FilaEntidad> filasEntidades = new ArrayList<>(entidades.length());
        List<FilaRol> filasRoles = new ArrayList<>(entidades.length());
        List<FilaDetalleCafeteria> cafeterias = new ArrayList<>();
        List<FilaDetalleMarcaEntidad> marcasEntidad = new ArrayList<>();
        List<FilaDetalleTostaderia> tostadurias = new ArrayList<>();
        List<FilaDetalleProductor> productores = new ArrayList<>();

        for (int i = 0; i < entidades.length(); i++) {
            JSONObject objeto = entidades.getJSONObject(i);
            String id = objeto.optString("id", "");
            filasEntidades.add(filaEntidad(objeto, orden, fotoDeEntidad(fotos, id)));
            orden++;

            JSONArray roles = objeto.optJSONArray("roles");
            if (roles != null) {
                for (int j = 0; j < roles.length(); j++) {
                    filasRoles.add(new FilaRol(id, normalizarRol(roles.optString(j, "OTRO"))));
                }
            }
            agregarCafeteria(cafeterias, id, objeto.optJSONObject("detalleCafeteria"));
            agregarMarcaEntidad(marcasEntidad, id, objeto.optJSONObject("detalleMarca"));
            agregarTostaduria(tostadurias, id, objeto.optJSONObject("detalleTostaderia"));
            agregarProductor(productores, id, objeto.optJSONObject("detalleProductor"));
        }

        List<FilaMarca> filasMarcas = new ArrayList<>();
        List<FilaSucursal> filasSucursales = new ArrayList<>();
        JSONArray marcas = raiz.getJSONArray("marcas");
        for (int i = 0; i < marcas.length(); i++) {
            JSONObject objeto = marcas.getJSONObject(i);
            String nombre = objeto.optString("nombre", "");
            filasMarcas.add(filaMarca(objeto, i, fotoDeMarca(fotos, nombre)));
            agregarSucursales(filasSucursales, nombre, objeto.optJSONArray("sucursales"),
                    ordenSucursal);
            ordenSucursal += objeto.optJSONArray("sucursales") == null
                    ? 0 : objeto.optJSONArray("sucursales").length();
        }

        List<FilaVariedad> filasVariedades = new ArrayList<>();
        JSONArray variedades = raiz.getJSONArray("variedades");
        for (int i = 0; i < variedades.length(); i++) {
            filasVariedades.add(filaVariedad(variedades.getJSONObject(i), i));
        }

        final FilaMeta filaMeta = new FilaMeta();
        filaMeta.id = 1;
        filaMeta.fuente = meta.optString("fuente", "");
        filaMeta.anio = meta.optInt("anio", 0);
        filaMeta.totalRegistros = meta.optInt("totalRegistros", 0);
        filaMeta.sello = sello;
        filaMeta.sembrado = new java.text.SimpleDateFormat(
                "yyyy-MM-dd HH:mm", java.util.Locale.US).format(new java.util.Date());

        // Una sola transacción para vaciar y rellenar. El borrado va
        // primero porque, si el JSON nuevo trae menos entidades que el
        // viejo, si no quedarían filas de la versión anterior mezcladas
        // con las nuevas. Y estar todo en la misma transacción es lo que
        // hace que sea "o todo o nada": si el teléfono se apaga en
        // mitad de la escritura, SQLite deshace el borrado también.
        base.runInTransaction(new Runnable() {
            @Override
            public void run() {
                daoEntidades.borrar();
                daoRoles.borrar();
                daoDetalles.borrarCafeterias();
                daoDetalles.borrarMarcasEntidad();
                daoDetalles.borrarTostaderias();
                daoDetalles.borrarProductores();
                daoDetalles.borrarSucursales();
                daoMarcas.borrar();
                daoVariedades.borrar();
                daoMeta.borrar();

                daoEntidades.insertarTodas(filasEntidades);
                daoRoles.insertarTodos(filasRoles);
                daoDetalles.insertarCafeterias(cafeterias);
                daoDetalles.insertarMarcasEntidad(marcasEntidad);
                daoDetalles.insertarTostaderias(tostadurias);
                daoDetalles.insertarProductores(productores);
                daoDetalles.insertarSucursales(filasSucursales);
                daoMarcas.insertarTodas(filasMarcas);
                daoVariedades.insertarTodas(filasVariedades);
                daoMeta.guardar(filaMeta);
            }
        });

        // El resumen sale por logcat a propósito: es la forma de
        // comprobar, desde un terminal, que la base se llenó con lo
        // mismo que dice el JSON. El comando está en la guía de
        // verificación: adb logcat | grep SembradorCenso
        Log.i(ETIQUETA, "Base llena con el JSON " + ARCHIVO_CENSO
                + " -> " + filasEntidades.size() + " lugares, "
                + filasMarcas.size() + " marcas, "
                + cafeterias.size() + " fichas de cafetería, "
                + marcasEntidad.size() + " fichas de marca, "
                + tostadurias.size() + " fichas de tostaduría, "
                + productores.size() + " fichas de productor, "
                + filasSucursales.size() + " sucursales, "
                + filasVariedades.size() + " variedades, "
                + filasRoles.size() + " filas de roles, "
                + "sello " + sello);
    }

    // ------------------------------------------------------------------
    // Del JSON a las filas
    // ------------------------------------------------------------------

    private static FilaEntidad filaEntidad(JSONObject objeto, int orden, String foto) {
        FilaEntidad fila = new FilaEntidad();
        fila.id = objeto.optString("id", "");
        fila.nombre = objeto.optString("nombre", "");
        fila.nombreClave = Texto.clave(fila.nombre);
        fila.direccion = objeto.optString("direccion", "");
        fila.mapaUrl = objeto.optString("mapaUrl", "");
        fila.macrodistrito = objeto.optString("macrodistrito", "");
        fila.lat = objeto.optDouble("lat", 0);
        fila.lng = objeto.optDouble("lng", 0);
        fila.rolPrincipal = normalizarRol(objeto.optString("rolPrincipal", "OTRO"));
        fila.marcaAsociada = objeto.optString("marcaAsociada", "");
        fila.marcaAsociadaClave = Texto.clave(fila.marcaAsociada);
        fila.nota = objeto.optString("nota", "");
        fila.precisionGps = objeto.isNull("precisionGps")
                ? null : objeto.optDouble("precisionGps", 0);
        fila.notaUbicacion = objeto.optString("notaUbicacion", "");
        fila.foto = foto;
        fila.orden = orden;
        return fila;
    }

    private static FilaMarca filaMarca(JSONObject objeto, int orden, String foto) {
        FilaMarca fila = new FilaMarca();
        fila.nombre = objeto.optString("nombre", "");
        fila.nombreClave = Texto.clave(fila.nombre);
        fila.esNacional = objeto.optString("esNacional", "");
        fila.anioCreacion = objeto.optString("anioCreacion", "");
        fila.lugarCreacion = objeto.optString("lugarCreacion", "");
        fila.registradaSenapi = objeto.optString("registradaSenapi", "");
        fila.segmento = objeto.optString("segmento", "");
        fila.nProductos = objeto.optString("nProductos", "");
        fila.municipioOrigen = objeto.optString("municipioOrigen", "");
        fila.canalComercializacion = objeto.optString("canalComercializacion", "");
        fila.cobertura = objeto.optString("cobertura", "");
        fila.nota = objeto.optString("nota", "");
        fila.foto = foto;
        fila.orden = orden;
        return fila;
    }

    private static void agregarSucursales(List<FilaSucursal> destino, String marca,
                                           JSONArray arreglo, int ordenInicial)
            throws JSONException {
        if (arreglo == null) {
            return;
        }
        String clave = Texto.clave(marca);
        for (int i = 0; i < arreglo.length(); i++) {
            JSONObject objeto = arreglo.getJSONObject(i);
            FilaSucursal fila = new FilaSucursal();
            fila.entidadId = objeto.optString("entidadId", "");
            fila.nombre = objeto.optString("nombre", "");
            fila.macrodistrito = objeto.optString("macrodistrito", "");
            fila.direccion = objeto.optString("direccion", "");
            fila.mapaUrl = objeto.optString("mapaUrl", "");
            fila.lat = objeto.optDouble("lat", 0);
            fila.lng = objeto.optDouble("lng", 0);
            fila.marca = marca;
            fila.marcaClave = clave;
            fila.orden = ordenInicial + i;
            destino.add(fila);
        }
    }

    private static FilaVariedad filaVariedad(JSONObject objeto, int orden) {
        FilaVariedad fila = new FilaVariedad();
        fila.nombre = objeto.optString("nombre", "");
        fila.nombreClave = Texto.clave(fila.nombre);
        fila.variedadesDeclaradas = objeto.optString("variedadesDeclaradas", "");
        fila.region = objeto.optString("region", "");
        fila.regionClave = Texto.clave(fila.region);
        fila.marca = objeto.optString("marca", "");
        fila.marcaClave = Texto.clave(fila.marca);
        fila.contexto = objeto.optString("contexto", "");
        fila.orden = orden;
        return fila;
    }

    private static void agregarCafeteria(List<FilaDetalleCafeteria> destino, String id,
                                        JSONObject objeto) throws JSONException {
        if (objeto == null) {
            return;
        }
        FilaDetalleCafeteria fila = new FilaDetalleCafeteria();
        fila.entidadId = id;
        fila.anioApertura = objeto.optString("anioApertura", "");
        fila.mesas = objeto.optString("mesas", "");
        fila.capacidad = objeto.optString("capacidad", "");
        fila.baristas = objeto.optString("baristas", "");
        fila.tipoEstablecimiento = objeto.optString("tipoEstablecimiento", "");
        fila.tipoApp = objeto.optString("tipoApp", "");
        fila.direccion = objeto.optString("direccion", "");
        fila.mapaUrl = objeto.optString("mapaUrl", "");
        destino.add(fila);
    }

    private static void agregarMarcaEntidad(List<FilaDetalleMarcaEntidad> destino, String id,
                                            JSONObject objeto) throws JSONException {
        if (objeto == null) {
            return;
        }
        FilaDetalleMarcaEntidad fila = new FilaDetalleMarcaEntidad();
        fila.entidadId = id;
        fila.esNacional = objeto.optString("esNacional", "");
        fila.anioCreacion = objeto.optString("anioCreacion", "");
        fila.lugarCreacion = objeto.optString("lugarCreacion", "");
        fila.registradaSenapi = objeto.optString("registradaSenapi", "");
        fila.segmento = objeto.optString("segmento", "");
        fila.nProductos = objeto.optString("nProductos", "");
        fila.municipioOrigen = objeto.optString("municipioOrigen", "");
        fila.canalComercializacion = objeto.optString("canalComercializacion", "");
        fila.cobertura = objeto.optString("cobertura", "");
        destino.add(fila);
    }

    private static void agregarTostaduria(List<FilaDetalleTostaderia> destino, String id,
                                          JSONObject objeto) throws JSONException {
        if (objeto == null) {
            return;
        }
        FilaDetalleTostaderia fila = new FilaDetalleTostaderia();
        fila.entidadId = id;
        fila.anioInicio = objeto.optString("anioInicio", "");
        fila.kgTostadosMes = objeto.optString("kgTostadosMes", "");
        fila.compraCafe = objeto.optString("compraCafe", "");
        fila.regionesOrigen = objeto.optString("regionesOrigen", "");
        fila.tiposTueste = objeto.optString("tiposTueste", "");
        fila.direccion = objeto.optString("direccion", "");
        fila.mapaUrl = objeto.optString("mapaUrl", "");
        destino.add(fila);
    }

    private static void agregarProductor(List<FilaDetalleProductor> destino, String id,
                                         JSONObject objeto) throws JSONException {
        if (objeto == null) {
            return;
        }
        FilaDetalleProductor fila = new FilaDetalleProductor();
        fila.entidadId = id;
        fila.comunidadMunicipio = objeto.optString("comunidadMunicipio", "");
        fila.aniosProduciendo = objeto.optString("aniosProduciendo", "");
        fila.especies = objeto.optString("especies", "");
        fila.variedades = objeto.optString("variedades", "");
        fila.produccionUltimaCosecha = objeto.optString("produccionUltimaCosecha", "");
        fila.notasCata = objeto.optString("notasCata", "");
        fila.vendeA = objeto.optString("vendeA", "");
        fila.direccion = objeto.optString("direccion", "");
        fila.mapaUrl = objeto.optString("mapaUrl", "");
        destino.add(fila);
    }

    // ------------------------------------------------------------------
    // Las fotos
    // ------------------------------------------------------------------

    /**
     * La ruta de la foto de un local, o "" si no tiene. Sale de
     * imagenes.json, que es el manifiesto que deja tools/generar_imagenes.py:
     * para cada lugar guarda el nombre del archivo dentro de
     * imagenesCafes/ o imagenesTostadurias/.
     *
     * En la tabla solo se guarda la ruta. Los 23 MB de imágenes van sueltos
     * en los assets y se cargan cuando se pintan.
     */
    private static String fotoDeEntidad(JSONObject fotos, String id) {
        String archivo = archivoDe(fotos, "cafes", id);
        if (!archivo.isEmpty()) {
            return CARPETA_FOTOS + "cafes/" + archivo;
        }
        archivo = archivoDe(fotos, "tostadurias", id);
        if (!archivo.isEmpty()) {
            return CARPETA_FOTOS + "tostadurias/" + archivo;
        }
        return "";
    }

    /** La ruta de la foto de una marca, o "" si no tiene. */
    private static String fotoDeMarca(JSONObject fotos, String nombre) {
        JSONObject seccion = fotos.optJSONObject("marcas");
        if (seccion == null) {
            return "";
        }
        JSONObject entrada = seccion.optJSONObject(Texto.clave(nombre));
        if (entrada == null) {
            return "";
        }
        String archivo = entrada.optString("archivo", "");
        return archivo.isEmpty() ? "" : CARPETA_FOTOS + "marcas/" + archivo;
    }

    private static String archivoDe(JSONObject fotos, String seccion, String id) {
        JSONObject grupo = fotos.optJSONObject(seccion);
        if (grupo == null || id.isEmpty()) {
            return "";
        }
        JSONObject entrada = grupo.optJSONObject(id);
        return entrada == null ? "" : entrada.optString("archivo", "");
    }

    private static JSONObject leerFotos(Context contexto) {
        try {
            return new JSONObject(textoDe(contexto, ARCHIVO_FOTOS));
        } catch (IOException | JSONException problema) {
            // Sin manifiesto de fotos la app sigue funcionando: lo único
            // que se pierde es la imagen, y cada pantalla tiene su
            // respaldo (el ícono del rol, la inicial, el fondo con
            // degradado). Es preferible a dejar la base sin llenar.
            Log.w(ETIQUETA, "Sin " + ARCHIVO_FOTOS + ": los locales irán sin foto", problema);
            return new JSONObject();
        }
    }

    /**
     * El rol tal como se guarda en la base: el nombre del enum. Un rol
     * desconocido se guarda como OTRO, que es como lo clasificaba el
     * censo el JSON original.
     */
    private static String normalizarRol(String nombre) {
        return Traductor.rol(nombre).name();
    }

    // ------------------------------------------------------------------
    // Lectura de assets
    // ------------------------------------------------------------------

    private static String textoDe(Context contexto, String nombre) throws IOException {
        try (InputStream entrada = contexto.getAssets().open(nombre)) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream(4096);
            byte[] trozo = new byte[8192];
            int leidos;
            while ((leidos = entrada.read(trozo)) != -1) {
                buffer.write(trozo, 0, leidos);
            }
            return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
        }
    }
}
