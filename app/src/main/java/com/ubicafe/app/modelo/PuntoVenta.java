package com.ubicafe.app.modelo;

/**
 * Un punto de venta de una marca en la ciudad.
 * Ej.: "TYPICA - Achumani" vende la marca TYPICA.
 *  - lat/lng: coordenadas reales (usadas por el futuro Google Maps).
 */
public class PuntoVenta {

    public final String marca;      // marca que se vende (ej. "TYPICA")
    public final String local;      // nombre del local (ej. "Achumani")
    public final String barrio;     // zona de La Paz
    public final String direccion;
    public final double lat;
    public final double lng;

    public PuntoVenta(String marca, String local, String barrio,
                      String direccion, double lat, double lng) {
        this.marca = marca;
        this.local = local;
        this.barrio = barrio;
        this.direccion = direccion;
        this.lat = lat;
        this.lng = lng;
    }
}