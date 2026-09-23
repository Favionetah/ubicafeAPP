package com.ubicafe.app.modelo;

import java.util.ArrayList;
import java.util.List;

/**
 * Resultado de la búsqueda global.
 * Agrupa los hallazgos en tres secciones, igual que en el diseño:
 *  - cafés de especialidad
 *  - marcas registradas
 *  - establecimientos (cafeterías que coinciden)
 */
public class ResultadoBusqueda {

    public final List<CafeOrigen> cafesDeEspecialidad;
    public final List<MarcaCafe> marcasRegistradas;
    public final List<Cafeteria> establecimientos;

    public ResultadoBusqueda() {
        cafesDeEspecialidad = new ArrayList<>();
        marcasRegistradas = new ArrayList<>();
        establecimientos = new ArrayList<>();
    }

    /** true si ninguna de las tres secciones tiene resultados. */
    public boolean estaVacio() {
        return cafesDeEspecialidad.isEmpty()
                && marcasRegistradas.isEmpty()
                && establecimientos.isEmpty();
    }
}