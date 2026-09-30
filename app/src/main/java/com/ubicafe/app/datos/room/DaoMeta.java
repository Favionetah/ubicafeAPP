package com.ubicafe.app.datos.room;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

/** Lectura y escritura de la fila de metadatos y del sello del JSON. */
@Dao
public abstract class DaoMeta {

    @Insert
    public abstract void guardar(FilaMeta fila);

    @Query("SELECT * FROM meta WHERE id = 1")
    public abstract FilaMeta leer();

    @Query("DELETE FROM meta")
    public abstract void borrar();
}
