package com.ubicafe.app.modelo;

/**
 * PAR ETIQUETA / VALOR
 * ---------------------------------------------------------------
 * El censo responde a muchas preguntas con una etiqueta y una respuesta
 * suelta, y la interfaz las pinta siempre igual: la etiqueta arriba en
 * verde, el valor debajo. Este tipo agrupa los dos para que las cuatro
 * fichas (cafetería, marca, tostaduría y productor) puedan compartir el
 * mismo componente de dibujo.
 */
public class ParDato {

    public final String etiqueta;
    public final String valor;

    public ParDato(String etiqueta, String valor) {
        this.etiqueta = etiqueta;
        this.valor = valor == null ? "" : valor.trim();
    }

    /** Un par sin valor no se dibuja: el censo no respondió esa pregunta. */
    public boolean tieneValor() {
        return !valor.isEmpty();
    }
}
