package com.ubicafe.app.datos.room;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

/**
 * Consultas de los 213 lugares del censo.
 *
 * Cada @Query es SQL de verdad que SQLite ejecuta, y el procesador de
 * Room lo compila al hacer el build: si escribiera mal una columna, el
 * build fallaría con el nombre de la tabla y de la columna, en vez de
 * fallar en el teléfono al abrir una lista.
 */
@Dao
public abstract class DaoEntidades {

    // -- escritura (solo la usa el sembrador) ------------------------

    @Insert
    public abstract void insertarTodas(List<FilaEntidad> filas);

    @Query("DELETE FROM entidades")
    public abstract void borrar();

    // -- lecturas ----------------------------------------------------

    /** Los 213, en el orden del JSON. */
    @Query("SELECT * FROM entidades ORDER BY orden")
    public abstract List<FilaEntidad> todas();

    /**
     * La consulta más usada de la app: se llama 7 veces, una por cada
     * pantalla que abre una ficha. Con clave primaria, es un índice.
     */
    @Query("SELECT * FROM entidades WHERE id = :id")
    public abstract FilaEntidad porId(String id);

    /** La búsqueda por nombre normalizado, que es como se llama desde la UI. */
    @Query("SELECT * FROM entidades WHERE nombre_clave = :clave ORDER BY orden LIMIT 1")
    public abstract FilaEntidad porNombreClave(String clave);

    /**
     * Los lugares de un papel, usando la tabla de cruce. Con el
     * multirrol del censo, un local aparece en varias listas a propósito:
     * Typica sale en las 189 cafeterías y también en las 13 tostadurías.
     */
    @Query("SELECT e.* FROM entidades e "
            + "INNER JOIN entidades_roles r ON r.entidad_id = e.id "
            + "WHERE r.rol = :rol ORDER BY e.orden")
    public abstract List<FilaEntidad> porRol(String rol);

    @Query("SELECT * FROM entidades WHERE rol_principal = :rol ORDER BY orden")
    public abstract List<FilaEntidad> porRolPrincipal(String rol);

    @Query("SELECT * FROM entidades WHERE macrodistrito = :macrodistrito ORDER BY orden")
    public abstract List<FilaEntidad> porMacrodistrito(String macrodistrito);

    @Query("SELECT * FROM entidades WHERE marca_asociada_clave = :clave ORDER BY orden")
    public abstract List<FilaEntidad> porMarca(String clave);

    /** Los lugares que sí se pueden poner en el mapa. */
    @Query("SELECT * FROM entidades WHERE lat != 0 OR lng != 0 ORDER BY orden")
    public abstract List<FilaEntidad> conCoordenadas();

    @Query("SELECT COUNT(*) FROM entidades")
    public abstract int contar();

    @Query("SELECT COUNT(*) FROM entidades WHERE lat != 0 OR lng != 0")
    public abstract int contarConCoordenadas();

    @Query("SELECT COUNT(*) FROM entidades WHERE rol_principal = :rol")
    public abstract int contarPorRolPrincipal(String rol);
}
