package com.ubicafe.app.modelo;

import java.util.List;

/**
 * Una marca de café nacional que se vende en La Paz.
 *
 *  - nombre:         ej. "TYPICA".
 *  - etiquetas:      roles de la marca (Cafetería, Tostaduría, Productor).
 *  - esNacional:     true si es una marca nacional registrada.
 *  - descripcion:    texto corto para el detalle.
 *  - cafésOrigen:    lista de cafés de especialidad de la marca.
 *  - colorMarca:     color para la tarjeta (placeholder cuando no hay foto).
 *  - inicial:        letra que se muestra en la tarjeta placeholder.
 */
public class MarcaCafe {

    public final String nombre;
    public final List<String> etiquetas;
    public final boolean esNacional;
    public final String descripcion;
    public final List<CafeOrigen> cafésOrigen;
    public final int colorMarca;
    public final char inicial;

    public MarcaCafe(String nombre, List<String> etiquetas, boolean esNacional,
                     String descripcion, List<CafeOrigen> cafésOrigen,
                     int colorMarca, char inicial) {
        this.nombre = nombre;
        this.etiquetas = etiquetas;
        this.esNacional = esNacional;
        this.descripcion = descripcion;
        this.cafésOrigen = cafésOrigen;
        this.colorMarca = colorMarca;
        this.inicial = inicial;
    }
}