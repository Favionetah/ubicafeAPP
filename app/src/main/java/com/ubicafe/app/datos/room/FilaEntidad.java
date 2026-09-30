package com.ubicafe.app.datos.room;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Una fila de la tabla "entidades": los 213 lugares del censo.
 *
 * Es la foto de cómo queda el dato en la base de datos, no el objeto que
 * usa la interfaz. Se separa a propósito: la clase de la interfaz
 * (modelo.Entidad) tiene campos finales, funciones y objetos anidados, y
 * forzarla a hacer de tabla obligaría a llenarla de @Ignore y anotaciones
 * que no dice nada del censo. Aquí solo hay columnas, y elTraductor se
 * encarga de pasarlas al modelo.
 *
 * El campo "orden" no está en el JSON: es la posición que tenía la
 * entidad dentro del archivo. Sin él, SQLite devolvería las filas en un
 * orden arbitrario y las listas de la app saldrían desordenadas de una
 * actualización a otra.
 */
@Entity(
        tableName = "entidades",
        indices = {
                @Index("macrodistrito"),
                @Index("rol_principal"),
                @Index("marca_asociada_clave")
        })
public class FilaEntidad {

    @PrimaryKey
    @ColumnInfo(name = "id")
    @NonNull
    public String id;

    @ColumnInfo(name = "nombre")
    public String nombre;

    @ColumnInfo(name = "nombre_clave")
    public String nombreClave;

    @ColumnInfo(name = "direccion")
    public String direccion;

    @ColumnInfo(name = "mapa_url")
    public String mapaUrl;

    @ColumnInfo(name = "macrodistrito")
    public String macrodistrito;

    @ColumnInfo(name = "lat")
    public double lat;

    @ColumnInfo(name = "lng")
    public double lng;

    @ColumnInfo(name = "rol_principal")
    public String rolPrincipal;

    /**
     * La marca a la que pertenece, ya normalizada sin tildes y minúsculas.
     * Se guarda una copia normalizada además del nombre bueno porque las
     * consultas por cadena ("los locales de Alexander Coffee") se hacen
     * siempre contra la versión normalizada: es la única forma de que
     * "Café Chulumani" y "Café chulumani" den el mismo resultado.
     */
    @ColumnInfo(name = "marca_asociada_clave")
    public String marcaAsociadaClave;

    @ColumnInfo(name = "marca_asociada")
    public String marcaAsociada;

    @ColumnInfo(name = "nota")
    public String nota;

    /**
     * Nullable a propósito. El censo deja precisionGps en null cuando la
     * coordenada vino de una foto de la fachada o de un pin aproximado, y
     * meter un 0 ahí haría creer que la ubicación era exacta en el
     * centro del mapa. Room distingue bien el null de un 0.0.
     */
    @ColumnInfo(name = "precision_gps")
    public Double precisionGps;

    @ColumnInfo(name = "nota_ubicacion")
    public String notaUbicacion;

    /**
     * La foto del local, como ruta dentro de assets (por ejemplo
     * "fotos/cafes/cafe_berna_linares_947.jpg"), o null si no tiene.
     *
     * Aquí está la decisión de diseño importante: en la base solo vive el
     * NOMBRE del archivo, nunca el archivo entero. Meter las fotos como
     * bytes (BLOB) haría que la base ocupara 23 MB, que cada lectura
     * cargara la imagen completa en memoria aunque solo se quiera su
     * nombre, y que cambiar la foto de un local obligara a reescribir la
     * fila. Con la ruta, la foto se cambia sustituyendo un archivo.
     */
    @ColumnInfo(name = "foto")
    public String foto;

    @ColumnInfo(name = "orden")
    public int orden;

    public FilaEntidad() {
        // Room lo necesita para poder instanciar las filas.
    }
}
