package com.ubicafe.app.datos.room;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Uno de los 78 puntos de venta del censo.
 *
 * Un punto de venta es el local concreto de una cadena: "Alexander
 * Coffe", en Cotahuma, Av. 20 de octubre. La entidad_id es su clave
 * primaria porque cada uno de los 78 puntos corresponde a un local
 * distinto del censo, y así la ficha del local y su punto de venta se
 * emparejan sin ambigüedad.
 *
 * El nombre de la cadena se guarda dos veces: el bueno, para pintar, y
 * el normalizado, para filtrar. Consultar "¿qué locales tiene Alexander
 * Coffee?" con un LIKE sobre el nombre bonito fallaría con "alexander
 * coffee" en minúsculas, que es como lo escribe la otra pantalla.
 */
@Entity(
        tableName = "sucursales",
        indices = {
                @Index("marca_clave"),
                @Index("macrodistrito")
        })
public class FilaSucursal {

    @PrimaryKey
    @ColumnInfo(name = "entidad_id")
    @NonNull
    public String entidadId;

    @ColumnInfo(name = "nombre")
    public String nombre;

    @ColumnInfo(name = "macrodistrito")
    public String macrodistrito;

    @ColumnInfo(name = "direccion")
    public String direccion;

    @ColumnInfo(name = "mapa_url")
    public String mapaUrl;

    @ColumnInfo(name = "lat")
    public double lat;

    @ColumnInfo(name = "lng")
    public double lng;

    @ColumnInfo(name = "marca")
    public String marca;

    @ColumnInfo(name = "marca_clave")
    public String marcaClave;

    @ColumnInfo(name = "orden")
    public int orden;

    public FilaSucursal() {
    }
}
