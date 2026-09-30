package com.ubicafe.app.datos.room;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;

/**
 * Un papel que tiene un lugar: la tabla de cruce del multirrol.
 *
 * El censo deja que un mismo local tenga varios roles a la vez: Typica es
 * cafetería y también tostaduría, y hay 25 locales marcados como marca.
 * Guardar los roles como una lista dentro de la tabla "entidades"
 * obligaría a separarlos con comas y a buscar con un LIKE que puede
 * encontrar falsos positivos. Con una tabla de cruce, "las 189
 * cafeterías" es un JOIN de dos tablas y el resultado es exacto.
 *
 * La clave primaria son las dos columnas juntas, porque un lugar no puede
 * tener el mismo rol dos veces.
 */
@Entity(
        tableName = "entidades_roles",
        primaryKeys = {"entidad_id", "rol"},
        indices = {@Index("rol"), @Index("entidad_id")})
public class FilaRol {

    @ColumnInfo(name = "entidad_id")
    @NonNull
    public String entidadId;

    @ColumnInfo(name = "rol")
    @NonNull
    public String rol;

    public FilaRol(String entidadId, String rol) {
        this.entidadId = entidadId;
        this.rol = rol;
    }
}
