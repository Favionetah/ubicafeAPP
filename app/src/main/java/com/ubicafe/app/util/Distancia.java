package com.ubicafe.app.util;

/**
 * DISTANCIA SOBRE LA TIERRA
 * ---------------------------------------------------------------
 * La app solo necesita saber si un lugar está a 450 metros o a 12
 * kilómetros para escribir "450 m" o "12 km" al lado del nombre. No
 * necesita precisión de agrimensura, así que se usa la fórmula de
 * Haversine, que es exacta para distancias cortas y no requiere
 * dependencias.
 *
 * En La Paz un grado de longitud mide unos 103 km, no 111, porque la
 * ciudad está a 16 grados de latitud. Se corrige con el coseno de la
 * latitud; sin esa corrección, las distancias en dirección este-oeste
 * salen infladas cerca de un 8 %.
 */
public final class Distancia {

    /** Radio medio de la Tierra en kilómetros. */
    private static final double RADIO_TIERRA_KM = 6371.0;

    private Distancia() {
        // Clase de utilidades: no se instancia.
    }

    /**
     * Distancia en kilómetros entre dos puntos, por la esfera de
     * Haversine. A 4,5 millones de km de radio, el error es de metros.
     */
    public static double entre(double lat1, double lng1, double lat2, double lng2) {
        double radLat1 = Math.toRadians(lat1);
        double radLat2 = Math.toRadians(lat2);
        double diferenciaLat = Math.toRadians(lat2 - lat1);
        double diferenciaLng = Math.toRadians(lng2 - lng1);

        double senoLat = Math.sin(diferenciaLat / 2);
        double senoLng = Math.sin(diferenciaLng / 2);
        double h = senoLat * senoLat
                + Math.cos(radLat1) * Math.cos(radLat2) * senoLng * senoLng;

        return 2 * RADIO_TIERRA_KM * Math.asin(Math.sqrt(h));
    }

    /** La distancia en el formato que ve la persona: "450 m", "1,2 km". */
    public static String formatear(double kilometros) {
        if (Double.isNaN(kilometros) || Double.isInfinite(kilometros)) {
            return "";
        }
        if (kilometros < 1.0) {
            return Math.round(kilometros * 1000) + " m";
        }
        // "1.0 km" sobra el decimal cuando la distancia es redonda.
        String texto = String.format(java.util.Locale.getDefault(), "%.1f", kilometros);
        if (texto.endsWith(".0")) {
            texto = texto.substring(0, texto.length() - 2);
        }
        return texto + " km";
    }
}
