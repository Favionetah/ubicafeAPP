package com.ubicafe.app.datos.room;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

/**
 * Consultas de las 4 fichas opcionales y de los 78 puntos de venta.
 *
 * OJO CON LAS CONSULTAS "IN (...)"
 * Un IN de Room no es una lista: se escribe como un solo parámetro y
 * Room lo convierte en un signo de interrogación por cada elemento. Y
 * SQLite admite como máximo 999 parámetros por sentencia (y algunos
 * SuggestedQueryLimits lo bajan a 500). Hoy el censo tiene 213 lugares,
 * así que las consultas más anchas que se hacen son de 213, y queda
 * margen de sobra.
 *
 * Si algún día el censo pasara de 999 locales, estas consultas de
 * "todas" empezarían a fallar en tiempo de ejecución con un
 * IllegalStateException de "too many SQL variables". La solución es
 * trocear la lista en trozos de 500 y unir los resultados en el
 * traductor, no subir el límite: es un tope del motor, no de Room.
 */
@Dao
public abstract class DaoDetalles {

    @Insert
    public abstract void insertarCafeterias(List<FilaDetalleCafeteria> filas);

    @Insert
    public abstract void insertarMarcasEntidad(List<FilaDetalleMarcaEntidad> filas);

    @Insert
    public abstract void insertarTostaderias(List<FilaDetalleTostaderia> filas);

    @Insert
    public abstract void insertarProductores(List<FilaDetalleProductor> filas);

    @Insert
    public abstract void insertarSucursales(List<FilaSucursal> filas);

    @Query("DELETE FROM detalle_cafeteria")
    public abstract void borrarCafeterias();

    @Query("DELETE FROM detalle_marca_entidad")
    public abstract void borrarMarcasEntidad();

    @Query("DELETE FROM detalle_tostaderia")
    public abstract void borrarTostaderias();

    @Query("DELETE FROM detalle_productor")
    public abstract void borrarProductores();

    @Query("DELETE FROM sucursales")
    public abstract void borrarSucursales();

    // -- lecturas de ficha -------------------------------------------

    @Query("SELECT * FROM detalle_cafeteria WHERE entidad_id = :id")
    public abstract FilaDetalleCafeteria cafeteriaDe(String id);

    @Query("SELECT * FROM detalle_cafeteria")
    public abstract List<FilaDetalleCafeteria> todasCafeterias();

    @Query("SELECT * FROM detalle_cafeteria WHERE entidad_id IN (:ids)")
    public abstract List<FilaDetalleCafeteria> cafeteriasDe(List<String> ids);

    @Query("SELECT * FROM detalle_marca_entidad WHERE entidad_id = :id")
    public abstract FilaDetalleMarcaEntidad marcaDe(String id);

    @Query("SELECT * FROM detalle_marca_entidad")
    public abstract List<FilaDetalleMarcaEntidad> todasMarcasEntidad();

    @Query("SELECT * FROM detalle_marca_entidad WHERE entidad_id IN (:ids)")
    public abstract List<FilaDetalleMarcaEntidad> marcasEntidadDe(List<String> ids);

    @Query("SELECT * FROM detalle_tostaderia WHERE entidad_id = :id")
    public abstract FilaDetalleTostaderia tostaduriaDe(String id);

    @Query("SELECT * FROM detalle_tostaderia")
    public abstract List<FilaDetalleTostaderia> todasTostaderias();

    @Query("SELECT * FROM detalle_tostaderia WHERE entidad_id IN (:ids)")
    public abstract List<FilaDetalleTostaderia> tostaduriasDe(List<String> ids);

    @Query("SELECT * FROM detalle_productor WHERE entidad_id = :id")
    public abstract FilaDetalleProductor productorDe(String id);

    @Query("SELECT * FROM detalle_productor")
    public abstract List<FilaDetalleProductor> todosProductores();

    @Query("SELECT * FROM detalle_productor WHERE entidad_id IN (:ids)")
    public abstract List<FilaDetalleProductor> productoresDe(List<String> ids);

    // -- lecturas de puntos de venta --------------------------------

    @Query("SELECT * FROM sucursales ORDER BY orden")
    public abstract List<FilaSucursal> todasSucursales();

    @Query("SELECT * FROM sucursales WHERE marca_clave = :clave ORDER BY orden")
    public abstract List<FilaSucursal> sucursalesDeMarca(String clave);

    @Query("SELECT * FROM sucursales WHERE entidad_id = :id LIMIT 1")
    public abstract FilaSucursal sucursalDeEntidad(String id);

    @Query("SELECT COUNT(*) FROM sucursales WHERE marca_clave = :clave")
    public abstract int contarSucursalesDeMarca(String clave);

    @Query("SELECT COUNT(*) FROM detalle_cafeteria")
    public abstract int contarFichasCafeteria();

    @Query("SELECT COUNT(*) FROM detalle_marca_entidad")
    public abstract int contarFichasMarca();

    @Query("SELECT COUNT(*) FROM detalle_tostaderia")
    public abstract int contarFichasTostaderia();

    @Query("SELECT COUNT(*) FROM detalle_productor")
    public abstract int contarFichasProductor();
}
