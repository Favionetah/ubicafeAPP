package com.ubicafe.app.datos.room;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * La ficha de tostaduría de un local. Solo 13 de los 213 la tienen.
 *
 * Misma idea que FilaDetalleCafeteria: tabla aparte, clave primaria el
 * id del local porque la relación es 1 a 1.
 */
@Entity(tableName = "detalle_tostaderia")
public class FilaDetalleTostaderia {

    @PrimaryKey
    @ColumnInfo(name = "entidad_id")
    @NonNull
    public String entidadId;

    @ColumnInfo(name = "anio_inicio")
    public String anioInicio;

    @ColumnInfo(name = "kg_tostados_mes")
    public String kgTostadosMes;

    @ColumnInfo(name = "compra_cafe")
    public String compraCafe;

    @ColumnInfo(name = "regiones_origen")
    public String regionesOrigen;

    @ColumnInfo(name = "tipos_tueste")
    public String tiposTueste;

    @ColumnInfo(name = "direccion")
    public String direccion;

    @ColumnInfo(name = "mapa_url")
    public String mapaUrl;

    public FilaDetalleTostaderia() {
    }
}
