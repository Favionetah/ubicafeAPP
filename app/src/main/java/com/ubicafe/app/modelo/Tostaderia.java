package com.ubicafe.app.modelo;

/**
 * Una tostaduría (café 100% boliviano).
 *  - region: zona donde está (La Paz, Yungas o Tarija) para el filtro.
 *  - lat/lng: coordenadas para el mapa de OpenStreetMap.
 */
public class Tostaderia {

    public final String nombre;
    public final String region;         // La Paz | Yungas | Tarija
    public final String direccion;
    public final String horarios;
    public final String descripcion;
    public final String contacto;
    public final int colorMarca;        // color de la tarjeta placeholder
    public final char inicial;
    public final double lat;
    public final double lng;

    public Tostaderia(String nombre, String region, String direccion, String horarios,
                      String descripcion, String contacto, int colorMarca, char inicial,
                      double lat, double lng) {
        this.nombre = nombre;
        this.region = region;
        this.direccion = direccion;
        this.horarios = horarios;
        this.descripcion = descripcion;
        this.contacto = contacto;
        this.colorMarca = colorMarca;
        this.inicial = inicial;
        this.lat = lat;
        this.lng = lng;
    }
}