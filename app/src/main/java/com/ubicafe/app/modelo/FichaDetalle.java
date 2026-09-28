package com.ubicafe.app.modelo;

import java.util.List;

/**
 * FICHA DE DETALLE DE UN LUGAR
 * ---------------------------------------------------------------
 * El censo tiene cuatro secciones de detalle, cada una con sus propias
 * preguntas. Todas se muestran igual en la interfaz: un encabezado y
 * una lista de "etiqueta, valor". Esta interfaz es lo que permite que
 * la ficha de la app sea una sola en vez de cuatro, y que al añadir una
 * quinta sección no obligue a tocar las pantallas.
 */
public interface FichaDetalle {

    /** Los pares con dato. Los vacíos no se incluyen. */
    List<ParDato> pares();

    /** true si la sección no tiene nada que mostrar. */
    boolean estaVacia();
}
