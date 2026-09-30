package com.ubicafe.app.datos.room;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * La ficha de cafetería de un local, en su propia tabla.
 *
 * Son 4 fichas (cafetería, marca, tostaduría, productor) y ninguna está
 * presente en los 213 locales: 25 no tienen ficha de cafetería, 200 no
 * tienen ficha de marca, y así con las demás. Podrían vivir como columnas
 * sueltas en "entidades", pero entonces la tabla tendría 40 columnas de
 * las que casi todas siempre valdrían "", y anyadir un campo a una ficha
 * obligaría a una migración. En tablas separadas, un local sin ficha es
 * sencillamente una fila que no existe.
 */
@Entity(tableName = "detalle_cafeteria")
public class FilaDetalleCafeteria {

    /** El id del local. Es la clave primaria porque la ficha es 1 a 1. */
    @PrimaryKey
    @ColumnInfo(name = "entidad_id")
    @NonNull
    public String entidadId;

    @ColumnInfo(name = "anio_apertura")
    public String anioApertura;

    @ColumnInfo(name = "mesas")
    public String mesas;

    @ColumnInfo(name = "capacidad")
    public String capacidad;

    @ColumnInfo(name = "baristas")
    public String baristas;

    @ColumnInfo(name = "tipo_establecimiento")
    public String tipoEstablecimiento;

    @ColumnInfo(name = "tipo_app")
    public String tipoApp;

    @ColumnInfo(name = "direccion")
    public String direccion;

    @ColumnInfo(name = "mapa_url")
    public String mapaUrl;

    public FilaDetalleCafeteria() {
    }
}
