package com.ubicafe.app.modelo;

import com.ubicafe.app.util.Distancia;

/**
 * UNA SUCURSAL DE UNA CADENA
 * ---------------------------------------------------------------
 * Un punto de venta concreto: un local con su dirección, su
 * macrodistrito y sus coordenadas. El censo localizó 78, y cada uno
 * corresponde a una entidad distinta del mapa, incluso cuando
 * comparten nombre. "Alexander Coffe" aparece en tres sucursales de La
 * Paz, todas con el mismo nombre y tres direcciones diferentes.
 *
 * El vínculo con la Entidad va en entidadId, no en un objeto, para que
 * el mapa y la lista de sucursales compartan la misma fuente.
 */
public class Sucursal {

    public final String entidadId;
    public final String nombre;
    public final String macrodistrito;
    public final String direccion;
    public final String mapaUrl;
    public final double lat;
    public final double lng;

    /** La cadena a la que pertenece el local. Nunca vacía: siempre hay una. */
    public final String marca;

    public Sucursal(String marca, String entidadId, String nombre,
                    String macrodistrito, String direccion, String mapaUrl,
                    double lat, double lng) {
        this.marca = marca == null ? "" : marca.trim();
        this.entidadId = entidadId == null ? "" : entidadId;
        this.nombre = nombre == null ? "" : nombre.trim();
        this.macrodistrito = macrodistrito == null ? "" : macrodistrito.trim();
        this.direccion = direccion == null ? "" : direccion.trim();
        this.mapaUrl = mapaUrl == null ? "" : mapaUrl.trim();
        this.lat = lat;
        this.lng = lng;
    }

    public boolean tieneCoordenadas() {
        return lat != 0.0 || lng != 0.0;
    }

    public char inicial() {
        return nombre.isEmpty() ? '?' : Character.toUpperCase(nombre.charAt(0));
    }

    /** Distancia en kilómetros desde un punto, o Double.MAX_VALUE si no hay coordenadas. */
    public double distanciaA(double latUsuario, double lngUsuario) {
        return tieneCoordenadas()
                ? Distancia.entre(lat, lng, latUsuario, lngUsuario)
                : Double.MAX_VALUE;
    }
}
