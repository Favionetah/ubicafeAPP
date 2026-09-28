package com.ubicafe.app.ui.mapa;

import com.ubicafe.app.modelo.Rol;

/**
 * Un punto sobre el mapa de OpenStreetMap.
 * Guarda el nombre que se muestra, sus coordenadas reales y el papel
 * que tiene en el censo, que es lo que decide el color del pin y a qué
 * ficha lleva "Ver información".
 *
 * Los roles son los del modelo, no cadenas propias: así el mapa y las
 * listas no pueden contradecirse sobre qué es una tostaduría.
 */
public class MarcadorMapa {

    public final String nombre;
    public final String zona;
    public final double lat;
    public final double lng;
    public final Rol rol;
    public final int color;

    /** Identificador de la entidad que abre al pulsar "Ver información". */
    public final String idEntidad;

    /** true si el censo advirtió que su posición es dudosa. */
    public final boolean ubicacionAvisada;

    public MarcadorMapa(String nombre, String zona, double lat, double lng,
                        Rol rol, int color, String idEntidad,
                        boolean ubicacionAvisada) {
        this.nombre = nombre;
        this.zona = zona;
        this.lat = lat;
        this.lng = lng;
        this.rol = rol;
        this.color = color;
        this.idEntidad = idEntidad;
        this.ubicacionAvisada = ubicacionAvisada;
    }
}
