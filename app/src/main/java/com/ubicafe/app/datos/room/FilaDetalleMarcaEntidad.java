package com.ubicafe.app.datos.room;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * La ficha de marca de un local: 78 de los 213 la tienen.
 *
 * Se separa de la ficha de la cadena (FilaMarca) aunque tengan los mismos
 * campos, porque son cosas distintas: esta es la ficha que el censo
 * escribió de UN local que además pertenece a una cadena, y la otra es la
 * ficha de la cadena como empresa. Alexander Coffee tiene ficha como
 * cadena y además cada uno de sus locales tiene la suya. Unir las dos en
 * una tabla obligaría a mezclar en la misma fila datos que no son del
 * mismo tipo.
 */
@Entity(tableName = "detalle_marca_entidad")
public class FilaDetalleMarcaEntidad {

    @PrimaryKey
    @ColumnInfo(name = "entidad_id")
    @NonNull
    public String entidadId;

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

    public FilaDetalleMarcaEntidad() {
    }
}
