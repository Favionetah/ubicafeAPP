package com.ubicafe.app.modelo;

import java.util.ArrayList;
import java.util.List;

/**
 * FICHA DE UN PRODUCTOR
 * ---------------------------------------------------------------
 * La huella de quien cultiva: comunidad, años produciendo, especies,
 * variedades, producción de la última cosecha, notas de cata y a quién
 * le vende.
 *
 * Solo hay 8 fichas completas de las 12 entidades con rol de productor,
 * y es el conjunto más desigual del censo: hay productores con
 * diecinueve años de datos y otros con un solo campo. Por eso todos los
 * bloques son opcionales.
 *
 * OJO: las notas de cata llegan en mayúsculas y separadas por comas
 * ("SABOR DE CANELA, CHOCOLATE, UVA"). Se guardan tal cual; pasarlas a
 * lista de chips es responsabilidad de la interfaz, no del dato.
 */
public class DetalleProductor implements FichaDetalle {

    public final String comunidadMunicipio;
    public final String aniosProduciendo;
    public final String especies;
    public final String variedades;
    public final String produccionUltimaCosecha;
    public final String notasCata;
    public final String vendeA;
    public final String direccion;
    public final String mapaUrl;

    public DetalleProductor(String comunidadMunicipio, String aniosProduciendo,
                            String especies, String variedades,
                            String produccionUltimaCosecha, String notasCata,
                            String vendeA, String direccion, String mapaUrl) {
        this.comunidadMunicipio = DetalleCafeteria.vacioONulo(comunidadMunicipio);
        this.aniosProduciendo = DetalleCafeteria.vacioONulo(aniosProduciendo);
        this.especies = DetalleCafeteria.vacioONulo(especies);
        this.variedades = DetalleCafeteria.vacioONulo(variedades);
        this.produccionUltimaCosecha = DetalleCafeteria.vacioONulo(produccionUltimaCosecha);
        this.notasCata = DetalleCafeteria.vacioONulo(notasCata);
        this.vendeA = DetalleCafeteria.vacioONulo(vendeA);
        this.direccion = DetalleCafeteria.vacioONulo(direccion);
        this.mapaUrl = DetalleCafeteria.vacioONulo(mapaUrl);
    }

    @Override
    public boolean estaVacia() {
        return comunidadMunicipio.isEmpty() && aniosProduciendo.isEmpty()
                && especies.isEmpty() && variedades.isEmpty()
                && produccionUltimaCosecha.isEmpty() && notasCata.isEmpty()
                && vendeA.isEmpty();
    }

    @Override
    public List<ParDato> pares() {
        List<ParDato> pares = new ArrayList<>();
        DetalleCafeteria.agregar(pares, "Comunidad o municipio", comunidadMunicipio);
        DetalleCafeteria.agregar(pares, "Años produciendo café", aniosProduciendo);
        DetalleCafeteria.agregar(pares, "Especies", especies);
        DetalleCafeteria.agregar(pares, "Variedades", variedades);
        DetalleCafeteria.agregar(pares, "Producción de la última cosecha",
                produccionUltimaCosecha);
        DetalleCafeteria.agregar(pares, "¿A quién vende?", vendeA);
        return pares;
    }

    /**
     * Las notas de cata separadas en items para pintarlas como chips.
     * "SABOR DE CANELA, CHOCOLATE, UVA" -> ["Canela", "Chocolate", "Uva"]
     *
     * El censo escribe estas notas siempre en mayúsculas y a veces con
     * coletillas del cuestionario, así que se normaliza el formato sin
     * tocar el contenido.
     */
    public List<String> notasComoLista() {
        List<String> notas = new ArrayList<>();
        for (String parte : notasCata.split("[,;]")) {
            String limpia = parte.trim();
            if (limpia.isEmpty()) {
                continue;
            }
            String minuscula = limpia.toLowerCase(java.util.Locale.ROOT);
            String sinColetilla = minuscula.startsWith("sabor de ")
                    ? minuscula.substring("sabor de ".length()) : minuscula;
            notas.add(primeraMayuscula(sinColetilla));
        }
        return notas;
    }

    private static String primeraMayuscula(String texto) {
        if (texto.isEmpty()) {
            return texto;
        }
        return texto.substring(0, 1).toUpperCase(java.util.Locale.ROOT) + texto.substring(1);
    }

    /** Las especies separadas, para mostrarlas como lista corta. */
    public List<String> especiesComoLista() {
        List<String> resultado = new ArrayList<>();
        for (String parte : especies.split("[,;]")) {
            String limpia = parte.trim();
            if (!limpia.isEmpty()) {
                resultado.add(limpia);
            }
        }
        return resultado;
    }
}
