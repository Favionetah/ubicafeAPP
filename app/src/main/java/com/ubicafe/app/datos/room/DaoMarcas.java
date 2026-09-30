package com.ubicafe.app.datos.room;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

/** Consultas de las 40 cadenas de café. */
@Dao
public abstract class DaoMarcas {

    @Insert
    public abstract void insertarTodas(List<FilaMarca> filas);

    @Query("DELETE FROM marcas")
    public abstract void borrar();

    @Query("SELECT * FROM marcas ORDER BY orden")
    public abstract List<FilaMarca> todas();

    @Query("SELECT * FROM marcas WHERE nombre_clave = :clave LIMIT 1")
    public abstract FilaMarca porClave(String clave);

    @Query("SELECT * FROM marcas WHERE nombre = :nombre LIMIT 1")
    public abstract FilaMarca porNombre(String nombre);

    @Query("SELECT COUNT(*) FROM marcas")
    public abstract int contar();
}
