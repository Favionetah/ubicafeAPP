package com.ubicafe.app.datos.room;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Una de las 40 cadenas de café del censo.
 *
 * A diferencia de los locales, la ficha de la cadena (si es nacional,
 * cuándo se creó, dónde, cuántos productos tiene...) sí es única y sí
 * se guarda en la misma tabla, con las columnas en lugar de en una tabla
 * aparte. La razón es que no hay una tabla "detalle de cadena" separada
 * que consultar: la ficha siempre se lee junto al nombre de la cadena,
 * entonces partirla en dos solo añadiría un JOIN a cada lectura.
 *
 * La clave primaria es el nombre tal cual lo escribió el censo, con su
 * tilde y su mayúscula incluidos. Se guarda además "nombre_clave", la
 * versión sin tildes y en minúsculas, porque hay dos cadenas que solo se
 * diferencian en eso ("Café Chulumani" y "Café chulumani") y las
 * búsquedas por marca tienen que encontrar las dos.
 */
@Entity(
        tableName = "marcas",
        indices = {@Index("nombre_clave")})
public class FilaMarca {

    @PrimaryKey
    @ColumnInfo(name = "nombre")
    @NonNull
    public String nombre;

    @ColumnInfo(name = "nombre_clave")
    public String nombreClave;

    @ColumnInfo(name = "es_nacional")
    public String esNacional;

    @ColumnInfo(name = "anio_creacion")
    public String anioCreacion;

    @ColumnInfo(name = "lugar_creacion")
    public String lugarCreacion;

    @ColumnInfo(name = "registrada_senapi")
    public String registradaSenapi;

    @ColumnInfo(name = "segmento")
    public String segmento;

    @ColumnInfo(name = "n_productos")
    public String nProductos;

    @ColumnInfo(name = "municipio_origen")
    public String municipioOrigen;

    @ColumnInfo(name = "canal_comercializacion")
    public String canalComercializacion;

    @ColumnInfo(name = "cobertura")
    public String cobertura;

    @ColumnInfo(name = "nota")
    public String nota;

    /**
     * Ruta de la foto de la cadena en assets, o null. Las marcas sin foto
     * muestran el círculo con la inicial, que es lo que se veía antes.
     */
    @ColumnInfo(name = "foto")
    public String foto;

    @ColumnInfo(name = "orden")
    public int orden;

    public FilaMarca() {
    }
}
