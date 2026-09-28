package com.ubicafe.app.modelo;

/**
 * UN CAFÉ DE ORIGEN O VARIEDAD
 * ---------------------------------------------------------------
 * El censo registró 66 referencias a cafés concretos: "Geisha", "Catuai",
 * "Caturá" y otras, con la región de donde vienen y la marca que las
 * trabaja. No es un producto con precio, porque el censo no preguntó
 * precios: es una relación entre una variedad, un lugar de origen y una
 * marca.
 *
 * La columna "contexto" del censo dice por qué aparece cada una, y
 * conviene conservarlo porque explica la relación:
 *   "Cafetería que compra esta variedad"
 *   "Región de origen de la marca"
 *   "Tostaduría que compra por región"
 *   "Productor que cultiva"
 */
public class CafeVariedad {

    public final String nombre;
    public final String variedadesDeclaradas;
    public final String region;
    public final String marca;
    public final String contexto;

    public CafeVariedad(String nombre, String variedadesDeclaradas, String region,
                        String marca, String contexto) {
        this.nombre = nombre == null ? "" : nombre.trim();
        this.variedadesDeclaradas = variedadesDeclaradas == null ? "" : variedadesDeclaradas.trim();
        this.region = region == null ? "" : region.trim();
        this.marca = marca == null ? "" : marca.trim();
        this.contexto = contexto == null ? "" : contexto.trim();
    }

    public char inicial() {
        return nombre.isEmpty() ? '?' : Character.toUpperCase(nombre.charAt(0));
    }

    /** "Catuai, Caturá, Geisha" separado en items para pintarlos como chips. */
    public java.util.List<String> variedadesComoLista() {
        java.util.List<String> lista = new java.util.ArrayList<>();
        for (String parte : variedadesDeclaradas.split("[,;/]")) {
            String limpia = parte.trim();
            if (!limpia.isEmpty()) {
                lista.add(limpia);
            }
        }
        return lista;
    }
}
