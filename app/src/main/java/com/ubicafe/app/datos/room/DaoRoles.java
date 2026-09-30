package com.ubicafe.app.datos.room;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

/** Lectura y escritura de la tabla de cruce de roles. */
@Dao
public abstract class DaoRoles {

    @Insert
    public abstract void insertarTodos(List<FilaRol> filas);

    @Query("DELETE FROM entidades_roles")
    public abstract void borrar();

    /**
     * Trae todos los roles de golpe en lugar de uno por entidad. Son 213
     * entidades y 246 filas: pedir 213 consultas individuales para armar
     * los multiroles sería 213 viajes de ida y vuelta a SQLite para
     * terminar con lo mismo en memoria.
     */
    @Query("SELECT * FROM entidades_roles")
    public abstract List<FilaRol> todos();

    @Query("SELECT COUNT(*) FROM entidades_roles")
    public abstract int contar();
}
