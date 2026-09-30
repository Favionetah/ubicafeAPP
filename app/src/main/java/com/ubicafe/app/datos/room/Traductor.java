package com.ubicafe.app.datos.room;

import com.ubicafe.app.modelo.CafeVariedad;
import com.ubicafe.app.modelo.DetalleCafeteria;
import com.ubicafe.app.modelo.DetalleMarca;
import com.ubicafe.app.modelo.DetalleProductor;
import com.ubicafe.app.modelo.DetalleTostaderia;
import com.ubicafe.app.modelo.Entidad;
import com.ubicafe.app.modelo.Marca;
import com.ubicafe.app.modelo.Rol;
import com.ubicafe.app.modelo.Sucursal;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Pasa las filas de la base a los objetos que usa la interfaz.
 *
 * Vive aquí, y no en RepositorioDatos, porque es la única pieza que
 * conoce las dos formas del dato: cómo está en SQLite (columnas, tablas
 * de cruce) y cómo lo quiere la app (un Entidad con sus fichas y sus
 * roles dentro). Si mañana cambia el modelo de la base, se toca este
 * archivo y ningún otro.
 *
 * Todos los textos pasan por vacio() porque una columna TEXT de SQLite
 * puede venir a null aunque en la base se haya escrito una cadena.
 * SQLite no distingue "" de NULL, y la interfaz espera cadenas vacías.
 */
public final class Traductor {

    private Traductor() {
        // Clase de servicios: se accede por métodos estáticos.
    }

    private static String vacio(String texto) {
        return texto == null ? "" : texto;
    }

    /** Un rol del texto de la base, o OTRO si no se reconoce. */
    public static Rol rol(String nombre) {
        for (Rol rol : Rol.values()) {
            if (rol.name().equalsIgnoreCase(vacio(nombre))) {
                return rol;
            }
        }
        // Un rol desconocido en la base no puede tumbar la app: se
        // trata como "otros", que es lo que el censo llama a lo que no se
        // pudo clasificar.
        return Rol.OTRO;
    }

    // ------------------------------------------------------------------
    // Lugares
    // ------------------------------------------------------------------

    /**
     * Une las filas de un local con sus roles y sus fichas para devolver
     * el Entidad que pintan las pantallas.
     *
     * Los roles llegan como una lista de pares (id, rol) porque en la
     * base están en su propia tabla: un local con dos papeles ocupa dos
     * filas. Las fichas llegan ya indexadas por id del local para no
     * buscarlas una a una.
     */
    public static Entidad entidad(FilaEntidad fila,
                                   Set<Rol> roles,
                                   FilaDetalleCafeteria cafeteria,
                                   FilaDetalleMarcaEntidad marca,
                                   FilaDetalleTostaderia tostaduria,
                                   FilaDetalleProductor productor) {
        return new Entidad(
                vacio(fila.id),
                vacio(fila.nombre),
                vacio(fila.direccion),
                vacio(fila.mapaUrl),
                vacio(fila.macrodistrito),
                fila.lat,
                fila.lng,
                roles,
                rol(fila.rolPrincipal),
                vacio(fila.marcaAsociada),
                vacio(fila.nota),
                fila.precisionGps,
                vacio(fila.notaUbicacion),
                cafeteria == null ? null : new DetalleCafeteria(
                        vacio(cafeteria.anioApertura), vacio(cafeteria.mesas),
                        vacio(cafeteria.capacidad), vacio(cafeteria.baristas),
                        vacio(cafeteria.tipoEstablecimiento), vacio(cafeteria.tipoApp),
                        vacio(cafeteria.direccion), vacio(cafeteria.mapaUrl)),
                marca == null ? null : new DetalleMarca(
                        vacio(marca.esNacional), vacio(marca.anioCreacion),
                        vacio(marca.lugarCreacion), vacio(marca.registradaSenapi),
                        vacio(marca.segmento), vacio(marca.nProductos),
                        vacio(marca.municipioOrigen), vacio(marca.canalComercializacion),
                        vacio(marca.cobertura)),
                tostaduria == null ? null : new DetalleTostaderia(
                        vacio(tostaduria.anioInicio), vacio(tostaduria.kgTostadosMes),
                        vacio(tostaduria.compraCafe), vacio(tostaduria.regionesOrigen),
                        vacio(tostaduria.tiposTueste), vacio(tostaduria.direccion),
                        vacio(tostaduria.mapaUrl)),
                productor == null ? null : new DetalleProductor(
                        vacio(productor.comunidadMunicipio), vacio(productor.aniosProduciendo),
                        vacio(productor.especies), vacio(productor.variedades),
                        vacio(productor.produccionUltimaCosecha), vacio(productor.notasCata),
                        vacio(productor.vendeA), vacio(productor.direccion),
                        vacio(productor.mapaUrl)),
                vacio(fila.foto));
    }

    /** Reúne los roles de todas las entidades: id -> sus roles. */
    public static Map<String, Set<Rol>> rolesPorEntidad(List<FilaRol> filas) {
        Map<String, Set<Rol>> indice = new HashMap<>();
        for (FilaRol fila : filas) {
            String id = vacio(fila.entidadId);
            Set<Rol> roles = indice.get(id);
            if (roles == null) {
                roles = EnumSet.noneOf(Rol.class);
                indice.put(id, roles);
            }
            roles.add(rol(fila.rol));
        }
        return indice;
    }

    private static Map<String, FilaDetalleCafeteria> indexarCafeterias(
            List<FilaDetalleCafeteria> filas) {
        Map<String, FilaDetalleCafeteria> indice = new HashMap<>();
        for (FilaDetalleCafeteria fila : filas) {
            indice.put(vacio(fila.entidadId), fila);
        }
        return indice;
    }

    private static Map<String, FilaDetalleMarcaEntidad> indexarMarcas(
            List<FilaDetalleMarcaEntidad> filas) {
        Map<String, FilaDetalleMarcaEntidad> indice = new HashMap<>();
        for (FilaDetalleMarcaEntidad fila : filas) {
            indice.put(vacio(fila.entidadId), fila);
        }
        return indice;
    }

    private static Map<String, FilaDetalleTostaderia> indexarTostaderias(
            List<FilaDetalleTostaderia> filas) {
        Map<String, FilaDetalleTostaderia> indice = new HashMap<>();
        for (FilaDetalleTostaderia fila : filas) {
            indice.put(vacio(fila.entidadId), fila);
        }
        return indice;
    }

    private static Map<String, FilaDetalleProductor> indexarProductores(
            List<FilaDetalleProductor> filas) {
        Map<String, FilaDetalleProductor> indice = new HashMap<>();
        for (FilaDetalleProductor fila : filas) {
            indice.put(vacio(fila.entidadId), fila);
        }
        return indice;
    }

    /** Los ids de las filas, para las consultas "IN (...)" de las fichas. */
    public static List<String> ids(List<FilaEntidad> filas) {
        List<String> ids = new ArrayList<>(filas.size());
        for (FilaEntidad fila : filas) {
            ids.add(fila.id);
        }
        return ids;
    }

    /**
     * Convierte una tanda de locales consultando sus roles y sus fichas.
     * Todas las listas que salen de un DAO pasan por aquí, así que el
     * número de consultas por tanda es siempre el mismo y no depende de
     * cuántos locales haya pedido la pantalla.
     */
    public static List<Entidad> entidades(UbiCafeBaseDatos base, List<FilaEntidad> filas) {
        if (filas.isEmpty()) {
            return new ArrayList<>();
        }
        List<String> ids = ids(filas);
        Map<String, Set<Rol>> roles = rolesPorEntidad(base.roles().todos());
        Map<String, FilaDetalleCafeteria> cafeterias = indexarCafeterias(
                base.detalles().cafeteriasDe(ids));
        Map<String, FilaDetalleMarcaEntidad> marcas = indexarMarcas(
                base.detalles().marcasEntidadDe(ids));
        Map<String, FilaDetalleTostaderia> tostadurias = indexarTostaderias(
                base.detalles().tostaduriasDe(ids));
        Map<String, FilaDetalleProductor> productores = indexarProductores(
                base.detalles().productoresDe(ids));

        List<Entidad> lista = new ArrayList<>(filas.size());
        for (FilaEntidad fila : filas) {
            String id = fila.id;
            Set<Rol> delLugar = roles.get(id);
            lista.add(entidad(
                    fila,
                    delLugar == null ? EnumSet.noneOf(Rol.class) : delLugar,
                    cafeterias.get(id),
                    marcas.get(id),
                    tostadurias.get(id),
                    productores.get(id)));
        }
        return lista;
    }

    /** Un local por su id, con todo lo que tiene colgando. */
    public static Entidad entidad(UbiCafeBaseDatos base, FilaEntidad fila) {
        if (fila == null) {
            return null;
        }
        String id = fila.id;
        Set<Rol> roles = rolesPorEntidad(base.roles().todos()).get(id);
        return entidad(
                fila,
                roles == null ? EnumSet.noneOf(Rol.class) : roles,
                base.detalles().cafeteriaDe(id),
                base.detalles().marcaDe(id),
                base.detalles().tostaduriaDe(id),
                base.detalles().productorDe(id));
    }

    // ------------------------------------------------------------------
    // Marcas, sucursales y variedades
    // ------------------------------------------------------------------

    public static DetalleMarca detalle(FilaMarca fila) {
        return new DetalleMarca(
                vacio(fila.esNacional), vacio(fila.anioCreacion),
                vacio(fila.lugarCreacion), vacio(fila.registradaSenapi),
                vacio(fila.segmento), vacio(fila.nProductos),
                vacio(fila.municipioOrigen), vacio(fila.canalComercializacion),
                vacio(fila.cobertura));
    }

    public static Sucursal sucursal(FilaSucursal fila) {
        return new Sucursal(
                vacio(fila.marca), vacio(fila.entidadId), vacio(fila.nombre),
                vacio(fila.macrodistrito), vacio(fila.direccion), vacio(fila.mapaUrl),
                fila.lat, fila.lng);
    }

    /**
     * Las 40 marcas con sus 78 puntos de venta ya agrupados. La consulta
     * trae los dos Conjuntos y el agrupado se hace en memoria, porque en
     * SQLite el "GROUP BY con listas anidadas" no existe: las sucursales
     * son filas de su propia tabla y solo se pueden volver a pegar a su
     * cadena en el código.
     */
    public static List<Marca> marcas(List<FilaMarca> filas, List<FilaSucursal> sucursales) {
        Map<String, List<Sucursal>> porMarca = new HashMap<>();
        for (FilaSucursal fila : sucursales) {
            String clave = vacio(fila.marcaClave);
            List<Sucursal> lista = porMarca.get(clave);
            if (lista == null) {
                lista = new ArrayList<>();
                porMarca.put(clave, lista);
            }
            lista.add(sucursal(fila));
        }
        List<Marca> lista = new ArrayList<>(filas.size());
        for (FilaMarca fila : filas) {
            List<Sucursal> suyas = porMarca.get(vacio(fila.nombreClave));
            lista.add(new Marca(
                    vacio(fila.nombre), detalle(fila), vacio(fila.nota),
                    suyas == null ? new ArrayList<>() : suyas,
                    vacio(fila.foto)));
        }
        return lista;
    }

    public static List<Sucursal> sucursales(List<FilaSucursal> filas) {
        List<Sucursal> lista = new ArrayList<>(filas.size());
        for (FilaSucursal fila : filas) {
            lista.add(sucursal(fila));
        }
        return lista;
    }

    public static CafeVariedad variedad(FilaVariedad fila) {
        return new CafeVariedad(
                vacio(fila.nombre), vacio(fila.variedadesDeclaradas),
                vacio(fila.region), vacio(fila.marca), vacio(fila.contexto));
    }

    public static List<CafeVariedad> variedades(List<FilaVariedad> filas) {
        List<CafeVariedad> lista = new ArrayList<>(filas.size());
        for (FilaVariedad fila : filas) {
            lista.add(variedad(fila));
        }
        return lista;
    }
}
