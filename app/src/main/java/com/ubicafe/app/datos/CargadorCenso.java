package com.ubicafe.app.datos;

import android.content.Context;
import android.util.Log;

import com.ubicafe.app.modelo.CafeVariedad;
import com.ubicafe.app.modelo.DetalleCafeteria;
import com.ubicafe.app.modelo.DetalleMarca;
import com.ubicafe.app.modelo.DetalleProductor;
import com.ubicafe.app.modelo.DetalleTostaderia;
import com.ubicafe.app.modelo.Entidad;
import com.ubicafe.app.modelo.Marca;
import com.ubicafe.app.modelo.Rol;
import com.ubicafe.app.modelo.Sucursal;
import com.ubicafe.app.util.Texto;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * LEE EL CENSO DE LOS ASSETS
 * ---------------------------------------------------------------
 * Lee una sola vez el archivo assets/censo_ubicafe.json, lo convierte a
 * objetos y lo deja cacheado en memoria. El archivo lo genera
 * tools/generar_censo.py a partir del Excel del censo.
 *
 * Por qué JSON en assets y no una base de datos: la app es solo
 * lectora. Nadie escribe desde el teléfono, así que no hay consultas
 * SQL que valga la pena indexar ni esquemas que migrar. El archivo
 * pesa 180 KB, se lee en una fracción de segundo y ocupa un megabyte
 * de memoria. Una base de datos añadiría dependencias, código y
 * complejidad a cambio de nada.
 *
 * Cuando exista un servidor, este es el ÚNICO archivo que hay que
 * reescribir:RepositorioDatos y todas las pantallas siguen igual.
 *
 * El parseo se hace una vez y se recuerda el resultado. Es seguro
 * repetir la llamada desde cualquier hilo: el trabajo real solo ocurre
 * la primera vez.
 */
public final class CargadorCenso {

    private static final String ARCHIVO = "censo_ubicafe.json";
    private static final String ETIQUETA = "CargadorCenso";

    /** Los datos ya leídos. null mientras no se haya cargado. */
    private static volatile Censo censo;

    private CargadorCenso() {
        // Clase de servicios: se accede por los métodos estáticos.
    }

    /**
     * El censo en memoria, o null si no se pudo leer.
     *
     * Se synchronize en lugar de dejar que dos hilos compitan por
     * leerlo: el archivo son 180 KB y parsearlo dos veces al arrancar
     * sería tirar trabajo a la basura. Es una espera de milisegundos
     * dentro del Splash, no un bloqueo en una pantalla.
     */
    public static Censo cargar(Context contexto) {
        Censo actual = censo;
        if (actual != null) {
            return actual;
        }
        synchronized (CargadorCenso.class) {
            if (censo == null) {
                censo = leer(contexto.getApplicationContext());
            }
            return censo;
        }
    }

    /** Descarta los datos cacheados. Solo lo usan las pruebas. */
    static void olvidar() {
        synchronized (CargadorCenso.class) {
            censo = null;
        }
    }

    private static Censo leer(Context contexto) {
        try (InputStream entrada = contexto.getAssets().open(ARCHIVO)) {
            return new Censo(new JSONObject(leerTexto(entrada)));
        } catch (IOException | JSONException error) {
            // Se registra y se devuelve null. La interfaz muestra su
            // estado de error con un botón de reintento, que vuelve a
            // llamar a este método.
            Log.e(ETIQUETA, "No se pudo leer " + ARCHIVO + " de los assets", error);
            return null;
        }
    }

    /** Lee el asset entero a memoria. Son 180 KB: caben de sobra. */
    private static String leerTexto(InputStream entrada) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream(ARCHIVO.length() * 4);
        byte[] trozo = new byte[8192];
        int leidos;
        while ((leidos = entrada.read(trozo)) != -1) {
            buffer.write(trozo, 0, leidos);
        }
        return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
    }

    // ------------------------------------------------------------------
    // El censo ya parseado, con sus índices listos para consultar.
    // ------------------------------------------------------------------

    /**
     * Los datos del censo y los índices que hacen rapides las consultas.
     *
     * Sin índices habría que recorrer las 213 entidades en cada filtro de
     * chip y en cada pulsación del buscador. Con ellos, todo es una
     * lectura de mapa.
     */
    public static final class Censo {

        public final String fuente;
        public final int anio;
        public final List<Entidad> entidades;
        public final List<Marca> marcas;
        public final List<CafeVariedad> variedades;
        public final Map<String, Entidad> porId;
        public final Map<Rol, List<Entidad>> porRol;
        public final Map<String, List<Entidad>> porMacrodistrito;
        public final Map<String, List<Entidad>> porMarcaAsociada;
        public final Map<String, List<Sucursal>> sucursalesPorMarca;
        public final Map<String, Marca> marcaPorClave;
        /** Para cada local, la cadena a la que pertenece. La inversa del anterior. */
        public final Map<String, Marca> marcaDeLocal;

        Censo(JSONObject raiz) throws JSONException {
            JSONObject meta = raiz.getJSONObject("meta");
            this.fuente = meta.optString("fuente", "");
            this.anio = meta.optInt("anio", 0);

            this.entidades = leerEntidades(raiz.getJSONArray("entidades"));
            this.variedades = leerVariedades(raiz.getJSONArray("variedades"));

            Map<String, Marca> marcasPorClave = new LinkedHashMap<>();
            List<Marca> listaMarcas = leerMarcas(raiz.getJSONArray("marcas"), marcasPorClave);
            this.marcas = Collections.unmodifiableList(listaMarcas);
            this.marcaPorClave = Collections.unmodifiableMap(marcasPorClave);

            this.porId = indexarPorId(this.entidades);
            this.porRol = indexarPorRol(this.entidades);
            this.porMacrodistrito = indexarPorMacrodistrito(this.entidades);
            this.porMarcaAsociada = indexarPorMarca(this.entidades);
            this.sucursalesPorMarca = indexarSucursales(this.marcas);
            this.marcaDeLocal = indexarMarcaDeLocal(this.marcas);
        }

        private static List<Entidad> leerEntidades(JSONArray arreglo) throws JSONException {
            List<Entidad> lista = new ArrayList<>(arreglo.length());
            for (int i = 0; i < arreglo.length(); i++) {
                JSONObject objeto = arreglo.getJSONObject(i);
                lista.add(new Entidad(
                        objeto.optString("id", ""),
                        objeto.optString("nombre", ""),
                        objeto.optString("direccion", ""),
                        objeto.optString("mapaUrl", ""),
                        objeto.optString("macrodistrito", ""),
                        objeto.optDouble("lat", 0),
                        objeto.optDouble("lng", 0),
                        leerRoles(objeto.optJSONArray("roles")),
                        leerRol(objeto.optString("rolPrincipal", "OTRO")),
                        objeto.optString("marcaAsociada", ""),
                        objeto.optString("nota", ""),
                        objeto.isNull("precisionGps") ? null : objeto.optDouble("precisionGps"),
                        objeto.optString("notaUbicacion", ""),
                        leerDetalleCafeteria(objeto.optJSONObject("detalleCafeteria")),
                        leerDetalleMarca(objeto.optJSONObject("detalleMarca")),
                        leerDetalleTostaderia(objeto.optJSONObject("detalleTostaderia")),
                        leerDetalleProductor(objeto.optJSONObject("detalleProductor"))));
            }
            return Collections.unmodifiableList(lista);
        }

        private static List<Marca> leerMarcas(JSONArray arreglo,
                                               Map<String, Marca> porClave)
                throws JSONException {
            List<Marca> lista = new ArrayList<>(arreglo.length());
            for (int i = 0; i < arreglo.length(); i++) {
                JSONObject objeto = arreglo.getJSONObject(i);
                String nombre = objeto.optString("nombre", "");
                Marca marca = new Marca(
                        nombre,
                        leerDetalleMarca(objeto),
                        objeto.optString("nota", ""),
                        leerSucursales(nombre, objeto.optJSONArray("sucursales")));
                lista.add(marca);
                porClave.put(Texto.clave(nombre), marca);
            }
            return lista;
        }

        private static List<Sucursal> leerSucursales(String marca, JSONArray arreglo)
                throws JSONException {
            if (arreglo == null) {
                return new ArrayList<>();
            }
            List<Sucursal> lista = new ArrayList<>(arreglo.length());
            for (int i = 0; i < arreglo.length(); i++) {
                JSONObject objeto = arreglo.getJSONObject(i);
                lista.add(new Sucursal(
                        marca,
                        objeto.optString("entidadId", ""),
                        objeto.optString("nombre", ""),
                        objeto.optString("macrodistrito", ""),
                        objeto.optString("direccion", ""),
                        objeto.optString("mapaUrl", ""),
                        objeto.optDouble("lat", 0),
                        objeto.optDouble("lng", 0)));
            }
            return lista;
        }

        private static List<CafeVariedad> leerVariedades(JSONArray arreglo)
                throws JSONException {
            List<CafeVariedad> lista = new ArrayList<>(arreglo.length());
            for (int i = 0; i < arreglo.length(); i++) {
                JSONObject objeto = arreglo.getJSONObject(i);
                lista.add(new CafeVariedad(
                        objeto.optString("nombre", ""),
                        objeto.optString("variedadesDeclaradas", ""),
                        objeto.optString("region", ""),
                        objeto.optString("marca", ""),
                        objeto.optString("contexto", "")));
            }
            return Collections.unmodifiableList(lista);
        }

        private static Set<Rol> leerRoles(JSONArray arreglo) throws JSONException {
            Set<Rol> roles = EnumSet.noneOf(Rol.class);
            if (arreglo != null) {
                for (int i = 0; i < arreglo.length(); i++) {
                    roles.add(leerRol(arreglo.optString(i, "OTRO")));
                }
            }
            return roles;
        }

        private static Rol leerRol(String nombre) {
            for (Rol rol : Rol.values()) {
                if (rol.name().equalsIgnoreCase(nombre)) {
                    return rol;
                }
            }
            // Un rol desconocido en el JSON no puede tumbar la app: se
            // trata como "otros", que es lo que el censo llama a lo que
            // no se pudo clasificar.
            return Rol.OTRO;
        }

        private static DetalleCafeteria leerDetalleCafeteria(JSONObject objeto) {
            if (objeto == null) {
                return null;
            }
            return new DetalleCafeteria(
                    objeto.optString("anioApertura", ""),
                    objeto.optString("mesas", ""),
                    objeto.optString("capacidad", ""),
                    objeto.optString("baristas", ""),
                    objeto.optString("tipoEstablecimiento", ""),
                    objeto.optString("tipoApp", ""),
                    objeto.optString("direccion", ""),
                    objeto.optString("mapaUrl", ""));
        }

        private static DetalleMarca leerDetalleMarca(JSONObject objeto) {
            if (objeto == null) {
                return null;
            }
            return new DetalleMarca(
                    objeto.optString("esNacional", ""),
                    objeto.optString("anioCreacion", ""),
                    objeto.optString("lugarCreacion", ""),
                    objeto.optString("registradaSenapi", ""),
                    objeto.optString("segmento", ""),
                    objeto.optString("nProductos", ""),
                    objeto.optString("municipioOrigen", ""),
                    objeto.optString("canalComercializacion", ""),
                    objeto.optString("cobertura", ""));
        }

        private static DetalleTostaderia leerDetalleTostaderia(JSONObject objeto) {
            if (objeto == null) {
                return null;
            }
            return new DetalleTostaderia(
                    objeto.optString("anioInicio", ""),
                    objeto.optString("kgTostadosMes", ""),
                    objeto.optString("compraCafe", ""),
                    objeto.optString("regionesOrigen", ""),
                    objeto.optString("tiposTueste", ""),
                    objeto.optString("direccion", ""),
                    objeto.optString("mapaUrl", ""));
        }

        private static DetalleProductor leerDetalleProductor(JSONObject objeto) {
            if (objeto == null) {
                return null;
            }
            return new DetalleProductor(
                    objeto.optString("comunidadMunicipio", ""),
                    objeto.optString("aniosProduciendo", ""),
                    objeto.optString("especies", ""),
                    objeto.optString("variedades", ""),
                    objeto.optString("produccionUltimaCosecha", ""),
                    objeto.optString("notasCata", ""),
                    objeto.optString("vendeA", ""),
                    objeto.optString("direccion", ""),
                    objeto.optString("mapaUrl", ""));
        }

        // -- índices ---------------------------------------------------

        private static Map<String, Entidad> indexarPorId(List<Entidad> entidades) {
            Map<String, Entidad> indice = new LinkedHashMap<>();
            for (Entidad entidad : entidades) {
                indice.put(entidad.id, entidad);
            }
            return Collections.unmodifiableMap(indice);
        }

        private static Map<Rol, List<Entidad>> indexarPorRol(List<Entidad> entidades) {
            Map<Rol, List<Entidad>> indice = new LinkedHashMap<>();
            for (Rol rol : Rol.values()) {
                indice.put(rol, new ArrayList<Entidad>());
            }
            for (Entidad entidad : entidades) {
                for (Rol rol : entidad.roles) {
                    indice.get(rol).add(entidad);
                }
            }
            for (Map.Entry<Rol, List<Entidad>> entrada : indice.entrySet()) {
                entrada.setValue(Collections.unmodifiableList(entrada.getValue()));
            }
            return Collections.unmodifiableMap(indice);
        }

        private static Map<String, List<Entidad>> indexarPorMacrodistrito(
                List<Entidad> entidades) {
            Map<String, List<Entidad>> indice = new LinkedHashMap<>();
            for (Entidad entidad : entidades) {
                String clave = entidad.macrodistrito.isEmpty()
                        ? "" : entidad.macrodistrito;
                List<Entidad> lista = indice.get(clave);
                if (lista == null) {
                    lista = new ArrayList<>();
                    indice.put(clave, lista);
                }
                lista.add(entidad);
            }
            return cerrar(indice);
        }

        private static Map<String, List<Entidad>> indexarPorMarca(List<Entidad> entidades) {
            Map<String, List<Entidad>> indice = new LinkedHashMap<>();
            for (Entidad entidad : entidades) {
                if (entidad.marcaAsociada.isEmpty()) {
                    continue;
                }
                List<Entidad> lista = indice.get(Texto.clave(entidad.marcaAsociada));
                if (lista == null) {
                    lista = new ArrayList<>();
                    indice.put(Texto.clave(entidad.marcaAsociada), lista);
                }
                lista.add(entidad);
            }
            return cerrar(indice);
        }

        private static Map<String, List<Sucursal>> indexarSucursales(List<Marca> marcas) {
            Map<String, List<Sucursal>> indice = new LinkedHashMap<>();
            for (Marca marca : marcas) {
                indice.put(Texto.clave(marca.nombre), marca.sucursales);
            }
            return Collections.unmodifiableMap(indice);
        }

        /**
         * Índice inverso: para un local, ¿de qué cadena es? Lo necesita
         * la ficha de una cafetería para decir "es parte de Alexander
         * Coffee" y abrir la lista completa de esa cadena. Como los 78
         * puntos de venta apuntan a 78 entidades distintas, no hay
         * ambigüedad: un local pertenece a una sola cadena.
         */
        private static Map<String, Marca> indexarMarcaDeLocal(List<Marca> marcas) {
            Map<String, Marca> indice = new LinkedHashMap<>();
            for (Marca marca : marcas) {
                for (Sucursal sucursal : marca.sucursales) {
                    if (!sucursal.entidadId.isEmpty()) {
                        indice.put(sucursal.entidadId, marca);
                    }
                }
            }
            return Collections.unmodifiableMap(indice);
        }

        private static Map<String, List<Entidad>> cerrar(Map<String, List<Entidad>> indice) {
            for (Map.Entry<String, List<Entidad>> entrada : indice.entrySet()) {
                entrada.setValue(Collections.unmodifiableList(entrada.getValue()));
            }
            return Collections.unmodifiableMap(indice);
        }
    }
}
