package com.ubicafe.app.modelo;

/**
 * Un productor (finca cafetalera) registrado en la app.
 * Ej.: Finca El Cóndor, de la familia Mamani en Caranavi.
 */
public class Productor {

    public final String nombreFinca;    // nombre de la finca
    public final String nombreFamilia;  // familia o persona que produce
    public final String origen;         // ej. "Caranavi, La Paz"
    public final int numCafes;          // cuántos cafés produce/exhíbe
    public final int altitud;           // altitud media de la finca
    public final String descripcion;    // texto corto del perfil
    public final int colorMarca;        // color de la tarjeta placeholder
    public final char inicial;          // inicial para la tarjeta
    public final double lat;            // coordenadas para el mapa de OpenStreetMap
    public final double lng;

    public Productor(String nombreFinca, String nombreFamilia, String origen,
                     int numCafes, int altitud, String descripcion,
                     int colorMarca, char inicial, double lat, double lng) {
        this.nombreFinca = nombreFinca;
        this.nombreFamilia = nombreFamilia;
        this.origen = origen;
        this.numCafes = numCafes;
        this.altitud = altitud;
        this.descripcion = descripcion;
        this.colorMarca = colorMarca;
        this.inicial = inicial;
this.lat = lat;
        this.lng = lng;
    }
}