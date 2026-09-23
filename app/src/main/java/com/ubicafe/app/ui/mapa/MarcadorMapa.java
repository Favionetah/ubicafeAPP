package com.ubicafe.app.ui.mapa;

/**
 * Un punto sobre el mapa de OpenStreetMap.
 * Guarda el nombre que se muestra, sus coordenadas reales (lat/lng) y
 * la categoría a la que pertenece (cafetería, tostaduría, productor o
 * punto de venta) para filtrar marcadores y abrir el detalle correcto.
 */
public class MarcadorMapa {

    /** Tipos de marcador (coinciden con los filtros del mapa). */
    public static final String TIPO_CAFETERIA = "Cafeterías";
    public static final String TIPO_TOSTADERIA = "Tostadurías";
    public static final String TIPO_PRODUCTOR = "Productores";
    public static final String TIPO_PUNTO_VENTA = "Puntos de venta";

    public final String nombre;
    public final String zona;        // zona/barrio para la leyenda
    public final double lat;
    public final double lng;
    public final String tipo;        // una de las constantes TIPO_*
    public final int color;          // color del marcador
    /** Nombre de la entidad que abre al pulsar "Ver información". */
    public final String nombreDetalle;

    public MarcadorMapa(String nombre, String zona, double lat, double lng,
                        String tipo, int color, String nombreDetalle) {
        this.nombre = nombre;
        this.zona = zona;
        this.lat = lat;
        this.lng = lng;
        this.tipo = tipo;
        this.color = color;
        this.nombreDetalle = nombreDetalle;
    }
}