package com.ubicafe.app.ui.mapa;

import org.osmdroid.util.GeoPoint;

import java.util.ArrayList;
import java.util.List;

/**
 * LECTURA DE UNA POLYLINE DE OSRM
 * ---------------------------------------------------------------
 * Las rutas no vienen como una lista de puntos: OSRM las devuelve
 * comprimidas en una cadena con el formato "polyline5", que es un
 * invento de Google. Cada carácter codifica un desplazamiento, no una
 * coordenada, así que hay que ir acumulando.
 *
 * La idea es esta: se recorren los caracteres en orden. Cada uno da un
 * número pequeño, que se suma a una posición que ya conocíamos. Por eso
 * funciona: cada punto solo guarda su distancia con el anterior, y el
 * primer punto es la posición inicial. El algoritmo es corto, es el de
 * todo el mundo, y la ventaja para nosotros es que las coordenadas
 * llegan enteras con 5 decimales, unos 11 cm, más que suficiente para
 * dibujar una calle.
 *
 * CUIDADO CON EL SALTO DE BYTE
 * El valor de un carácter va de -63 a 63, y el signo no está en el
 * carácter sino en un sexto bit que se acumula hacia el siguiente. Por
 * eso el resultado se vaescalando con >> 1: mientras haya cinco shifts
 * sin resultado útil, el desplazamiento sigue siendo mayor que 32 bits y
 * hay que seguir leyendo. Ese detalle es el que hace que una ruta con
 * puntos muy separados (un tramo largo de una vez) no se dibuje
 * deformada.
 */
public final class DecodificadorPolyline {

    private DecodificadorPolyline() {
        // Clase de utilidades: no se instancia.
    }

    /**
     * Convierte la cadena de OSRM en puntos del mapa. Devuelve una lista
     * vacía si la cadena está vacía o es inválida, nunca falla: una ruta
     * ilegible es el mismo caso que no haber ruta.
     */
    public static List<GeoPoint> aPuntos(String codificada) {
        List<GeoPoint> puntos = new ArrayList<>();
        if (codificada == null || codificada.isEmpty()) {
            return puntos;
        }

        int indice = 0;
        int lat = 0;
        int lng = 0;

        while (indice < codificada.length()) {
            // Cada coordenada se lee en dos pasos: primero la latitud,
            // luego la longitud. Dentro de cada uno, los caracteres van
            // de cinco en cinco bits, con el sexto bit del último
            // diciendo si el número es negativo. El acumulador se
            // declara FUERA del bucle porque cada vuelta del interior
            // aporta cinco bits más del mismo número, no uno nuevo.
            int[] latitud = leerCoordenada(codificada, indice);
            if (latitud == null) {
                return puntos;
            }
            indice = latitud[1];
            lat += latitud[0];

            int[] longitud = leerCoordenada(codificada, indice);
            if (longitud == null) {
                return puntos;
            }
            indice = longitud[1];
            lng += longitud[0];

            puntos.add(new GeoPoint(lat / 1e5, lng / 1e5));
        }
        return puntos;
    }

    /**
     * Lee una coordenada de la cadena y devuelve {desplazamiento, índice
     * siguiente}. El desplazamiento viene ya con el signo puesto, para
     * que quien llame solo tenga que sumarlo a la posición anterior.
     *
     * Devuelve null si la cadena se acaba a mitad, que es la forma de
     * decir "esto no es una polyline válida" sin lanzar.
     */
    private static int[] leerCoordenada(String codificada, int desde) {
        int indice = desde;
        int acumulado = 0;
        int desplazamiento = 0;
        int caracter;

        do {
            if (indice >= codificada.length()) {
                return null;
            }
            caracter = codificada.charAt(indice++) - 63;
            acumulado |= (caracter & 0x1f) << desplazamiento;
            desplazamiento += 5;
            // Con 0x20 o más sigue habiendo bits por leer. El AND con
            // 0x1f deja solo los cinco últimos, y el bit 5 va aparte.
        } while (caracter >= 0x20);

        int valor = (acumulado & 1) != 0 ? ~(acumulado >> 1) : (acumulado >> 1);
        return new int[]{valor, indice};
    }

    /**
     * La línea recta entre dos puntos. Es el plan B: si no hay red o
     * OSRM falla, el destino se sigue viendo y se puede llegar a pie.
     */
    public static List<GeoPoint> recta(GeoPoint desde, GeoPoint hasta) {
        List<GeoPoint> puntos = new ArrayList<>(2);
        puntos.add(desde);
        puntos.add(hasta);
        return puntos;
    }
}
