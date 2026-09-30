package com.ubicafe.app.datos.room;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * La fila única con los metadatos del censo: de dónde viene y en qué año.
 *
 * Sirve para dos cosas. La primera es pintar "Censo del sector cafetero
 * de La Paz, 2026" al pie de la app. La segunda, y la importante, es
 * guardar el SELLO del JSON con el que se llenó la base.
 *
 * El sello es el total de registros más el tamaño en bytes del
 * assets/censo_ubicafe.json. Si algún día se regenera el censo con
 * tools/generar_censo.py, el sello cambia, la app lo detecta al arrancar
 * y vuelve a llenar la base. Sin esto habría que borrar los datos de la
 * app a mano cada actualización, y quien actualizara el censo se
 * encontraría con listas viejas sin saber por qué.
 */
@Entity(tableName = "meta")
public class FilaMeta {

    /** Siempre 1: esta tabla tiene una sola fila y punto. */
    @PrimaryKey
    @ColumnInfo(name = "id")
    public int id;

    @ColumnInfo(name = "fuente")
    public String fuente;

    @ColumnInfo(name = "anio")
    public int anio;

    @ColumnInfo(name = "total_registros")
    public int totalRegistros;

    @ColumnInfo(name = "sello")
    public String sello;

    @ColumnInfo(name = "sembrado")
    public String sembrado;

    public FilaMeta() {
    }
}
