package com.ubicafe.app.util;

import java.text.Normalizer;
import java.util.Locale;

/**
 * NORMALIZACIÓN DE TEXTO PARA BUSCAR
 * ---------------------------------------------------------------
 * La persona escribe "cafe" y tiene que encontrar "Café Berna".
 * Escribe "geisha" y tiene que encontrar "GEISHA". Y escribe "BOLIVIAN"
 * y tiene que encontrar "Bolivian Cofee".
 *
 * Para que eso funcione sin casos especiales, se comparan las palabras
 * ya normalizadas: sin mayúsculas, sin tildes y sin signos. La clave
 * nunca se muestra; solo se usa para emparejar.
 */
public final class Texto {

    private Texto() {
        // Clase de utilidades: no se instancia.
    }

    /**
     * Reduce un texto a su forma comparable: "Café Copacabana" y
     * "CAFE-COPACABANA" producen la misma clave.
     */
    public static String clave(String texto) {
        if (texto == null || texto.isEmpty()) {
            return "";
        }
        // NFD separa la "é" en "e" + tilde combinante; al quitar los
        // signos combinantes queda la "e" sola.
        String sinTildes = Normalizer.normalize(texto, Normalizer.Form.NFD);
        StringBuilder limpio = new StringBuilder(sinTildes.length());
        for (int i = 0; i < sinTildes.length(); i++) {
            char caracter = sinTildes.charAt(i);
            int tipo = Character.getType(caracter);
            if (tipo != Character.NON_SPACING_MARK) {
                limpio.append(Character.toLowerCase(caracter));
            }
        }
        return limpio.toString().trim();
    }

    /**
     * La clave de una búsqueda con espacios normalizados, para poder
     * buscar por varias palabras a la vez: "cafe origen" tiene que
     * encontrar "Café de Origen".
     */
    public static String claveDeConsulta(String consulta) {
        if (consulta == null) {
            return "";
        }
        return clave(consulta).replaceAll("\\s+", " ").trim();
    }

    /**
     * true si el texto contiene la consulta, ignorando mayúsculas y
     * tildes. La consulta ya viene normalizada desde claveDeConsulta.
     */
    public static boolean contiene(String texto, String consultaClave) {
        if (consultaClave.isEmpty()) {
            return true;
        }
        return clave(texto).contains(consultaClave);
    }

    /** Convierte a mayúscula la primera letra, para títulos y etiquetas. */
    public static String inicialMayuscula(String texto) {
        if (texto == null || texto.isEmpty()) {
            return "";
        }
        return texto.substring(0, 1).toUpperCase(Locale.getDefault())
                + texto.substring(1);
    }

    /**
     * Une las partes con comas, saltándose las vacías. Se usa para
     * armar "Cafetería · Marca" y "Boliviano · Yungas" sin dejar
     * comas colgando cuando falta un dato.
     */
    public static String unir(String separador, Iterable<String> partes) {
        StringBuilder texto = new StringBuilder();
        for (String parte : partes) {
            if (parte == null || parte.trim().isEmpty()) {
                continue;
            }
            if (texto.length() > 0) {
                texto.append(separador);
            }
            texto.append(parte.trim());
        }
        return texto.toString();
    }
}
