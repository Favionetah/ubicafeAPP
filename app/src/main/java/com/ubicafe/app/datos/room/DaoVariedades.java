package com.ubicafe.app.datos.room;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

/** Consultas de las 66 variedades de café de origen. */
@Dao
public abstract class DaoVariedades {

    @Insert
    public abstract void insertarTodas(List<FilaVariedad> filas);

    @Query("DELETE FROM variedades")
    public abstract void borrar();

    @Query("SELECT * FROM variedades ORDER BY orden")
    public abstract List<FilaVariedad> todas();

    /**
     * El nombre está repetido seis veces en el censo (Café Santa,
     * Coffee Brothers, Typica...), así que la búsqueda por nombre
     * devuelve varias y quien la llama se queda con la primera.
     */
    @Query("SELECT * FROM variedades WHERE nombre_clave = :clave ORDER BY orden")
    public abstract List<FilaVariedad> porNombreClave(String clave);

    @Query("SELECT * FROM variedades WHERE marca_clave = :clave ORDER BY orden")
    public abstract List<FilaVariedad> porMarca(String clave);

    @Query("SELECT COUNT(*) FROM variedades")
    public abstract int contar();
}
