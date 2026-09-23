package com.ubicafe.app.ui.mapa;

/**
 * Un punto sobre el mapa estático de La Paz.
 * Guarda el nombre que se muestra y sus coordenadas reales (lat/lng),
 * para que en el futuro Google Maps use exactamente estos mismos datos.
 */
public class MarcadorMapa {

    public final String nombre;
    public final String zona;      // zona/barrio para la leyenda
    public final double lat;
    public final double lng;
    public final int color;        // color del marcador

    public MarcadorMapa(String nombre, String zona, double lat, double lng, int color) {
        this.nombre = nombre;
        this.zona = zona;
        this.lat = lat;
        this.lng = lng;
        this.color = color;
    }
}