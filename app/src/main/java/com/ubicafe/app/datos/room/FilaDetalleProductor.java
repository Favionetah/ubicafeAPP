package com.ubicafe.app.datos.room;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * La ficha de productor de un local. Solo 12 de los 213 la tienen.
 *
 * Los campos de listas libres (especies, variedades, notas de cata) se
 * guardan tal cual llegan del JSON, con comas y todo. Ahí el censo escribe
 * texto, no un catálogo cerrado de valores permitidos, así que separarlo
 * en otra tabla sería inventarle una estructura al dato.
 */
@Entity(tableName = "detalle_productor")
public class FilaDetalleProductor {

    @PrimaryKey
    @ColumnInfo(name = "entidad_id")
    @NonNull
    public String entidadId;

    @ColumnInfo(name = "comunidad_municipio")
    public String comunidadMunicipio;

    @ColumnInfo(name = "anios_produciendo")
    public String aniosProduciendo;

    @ColumnInfo(name = "especies")
    public String especies;

    @ColumnInfo(name = "variedades")
    public String variedades;

    @ColumnInfo(name = "produccion_ultima_cosecha")
    public String produccionUltimaCosecha;

    @ColumnInfo(name = "notas_cata")
    public String notasCata;

    @ColumnInfo(name = "vende_a")
    public String vendeA;

    @ColumnInfo(name = "direccion")
    public String direccion;

    @ColumnInfo(name = "mapa_url")
    public String mapaUrl;

    public FilaDetalleProductor() {
    }
}
