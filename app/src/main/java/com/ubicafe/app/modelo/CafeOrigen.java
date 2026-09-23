package com.ubicafe.app.modelo;

/**
 * Un café de especialidad con su "ficha de origen".
 * Es la información que ves en las tarjetas del detalle:
 * variedad (GEISHA, CATURRA…), de dónde viene, a qué altura,
 * cómo se procesa y qué sabor tiene.
 */
public class CafeOrigen {

    public final String variedad;      // ej. "GEISHA"
    public final String origen;        // ej. "Caranavi, La Paz"
    public final int altitud;          // metros sobre el nivel del mar
    public final String proceso;       // ej. "Lavado"
    public final String tostado;       // ej. "Medio"
    public final String aroma;         // ej. "Jazmín, frutas tropicales"
    public final String notasCata;     // ej. "Floral, dulce, cuerpo sedoso"

    public CafeOrigen(String variedad, String origen, int altitud, String proceso,
                      String tostado, String aroma, String notasCata) {
        this.variedad = variedad;
        this.origen = origen;
        this.altitud = altitud;
        this.proceso = proceso;
        this.tostado = tostado;
        this.aroma = aroma;
        this.notasCata = notasCata;
    }
}