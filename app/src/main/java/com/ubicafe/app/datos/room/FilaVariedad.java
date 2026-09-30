package com.ubicafe.app.datos.room;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Una de las 66 variedades de café de origen del censo.
 *
 * Aquí la clave primaria NO puede ser el nombre: hay seis nombres
 * repetidos (Café Santa, Coffee Brothers, Fortaleza Coffe, Nayra Qata,
 * Sin Fronteras y Typica aparecen dos veces cada uno, en regiones
 * distintas). Son variedades distintas que happen a llamarse igual, así
 * que la clave es un id numérico y el nombre lleva un índice normal para
 * poder buscarlas por él.
 */
@Entity(
        tableName = "variedades",
        indices = {
                @Index("nombre_clave"),
                @Index("region_clave")
        })
public class FilaVariedad {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    public long id;

    @ColumnInfo(name = "nombre")
    public String nombre;

    @ColumnInfo(name = "nombre_clave")
    public String nombreClave;

    @ColumnInfo(name = "variedades_declaradas")
    public String variedadesDeclaradas;

    @ColumnInfo(name = "region")
    public String region;

    /**
     * La región normalizada. Sirve para agrupar: el censo escribe
     * "Caranavi", "CARANAVI LOCALIDAD ILLAMANI - SAN JOSE" y
     * "Caranavi, La Paz" para lo mismo, y sin esta columna cada grafía
     * sería una región distinta en el gráfico de Explorar.
     */
    @ColumnInfo(name = "region_clave")
    public String regionClave;

    @ColumnInfo(name = "marca")
    public String marca;

    @ColumnInfo(name = "marca_clave")
    public String marcaClave;

    @ColumnInfo(name = "contexto")
    public String contexto;

    @ColumnInfo(name = "orden")
    public int orden;

    public FilaVariedad() {
    }
}
