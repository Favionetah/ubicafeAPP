package com.ubicafe.app.modelo;

import java.util.ArrayList;
import java.util.List;

/**
 * RESULTADO DE LA BÚSQUEDA GLOBAL
 * ---------------------------------------------------------------
 * Agrupa los hallazgos en las cinco secciones en que se muestran en
 * pantalla, cada una con su contador. La búsqueda no devuelve una lista
 * única porque "cafe" tiene que dar cafeterías, marcas, tostadurías y
 * productores a la vez, ordenados por sección y no mezclados.
 *
 * Todos los campos son listas inmutables y ya vienen ordenadas: quien
 * llama no necesita reordenar nada.
 */
public class ResultadoBusqueda {

    public final List<CafeVariedad> cafesDeOrigen;
    public final List<Marca> marcas;
    public final List<Entidad> establecimientos;
    public final List<Entidad> productores;
    public final List<Entidad> tostadurias;

    public ResultadoBusqueda(List<CafeVariedad> cafesDeOrigen, List<Marca> marcas,
                             List<Entidad> establecimientos, List<Entidad> productores,
                             List<Entidad> tostadurias) {
        this.cafesDeOrigen = cafesDeOrigen;
        this.marcas = marcas;
        this.establecimientos = establecimientos;
        this.productores = productores;
        this.tostadurias = tostadurias;
    }

    /** Un resultado sin nada que mostrar, para el estado vacío. */
    public static ResultadoBusqueda vacio() {
        return new ResultadoBusqueda(new ArrayList<CafeVariedad>(), new ArrayList<Marca>(),
                new ArrayList<Entidad>(), new ArrayList<Entidad>(), new ArrayList<Entidad>());
    }

    /** Total de hallazgos, para el subtítulo de la pantalla. */
    public int total() {
        return cafesDeOrigen.size() + marcas.size() + establecimientos.size()
                + productores.size() + tostadurias.size();
    }

    public boolean estaVacio() {
        return total() == 0;
    }

    /** true si esta sección tiene resultados y merece su encabezado. */
    public boolean hayCafes() {
        return !cafesDeOrigen.isEmpty();
    }

    public boolean hayMarcas() {
        return !marcas.isEmpty();
    }

    public boolean hayEstablecimientos() {
        return !establecimientos.isEmpty();
    }

    public boolean hayProductores() {
        return !productores.isEmpty();
    }

    public boolean hayTostadurias() {
        return !tostadurias.isEmpty();
    }
}
