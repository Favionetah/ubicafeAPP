package com.ubicafe.app.ui.mapa;

import org.osmdroid.util.GeoPoint;

import java.util.Collections;
import java.util.List;

/**
 * UNA RUTA HASTA UN LOCAL
 * ---------------------------------------------------------------
 * Los datos que llegan de OSRM, más lo que la app necesita saber para
 * poder dibujarla: de dónde se salía, cuánto se anda en coche y cuánto
 * tarda.
 *
 * Se distingue entre una ruta real y la línea recta del plan B porque
 * la interfaz tiene que poder decirlo: un trayecto dibujado en línea
 * recta no son 4 kilómetros por la avenida, son 4 kilómetros en línea
 * recta, y decirlo sin más sería mentir.
 */
public final class Ruta {

    /** Por qué la ruta es lo que es. */
    public enum Origen {
        /** OSRM respondió con el trayecto real por las calles. */
        OSRM,
        /** No se pudo contactar con el servicio: se trazó el directo. */
        SIN_CONEXION,
        /** El servicio respondió con un error: se trazó el directo. */
        ERROR_SERVICIO,
        /** La persona aún no ha autorizado la ubicación. */
        SIN_UBICACION
    }

    public final List<GeoPoint> puntos;
    public final double distanciaMetros;
    public final double duracionSegundos;
    public final Origen origen;

    private Ruta(List<GeoPoint> puntos, double distanciaMetros,
                 double duracionSegundos, Origen origen) {
        this.puntos = Collections.unmodifiableList(puntos);
        this.distanciaMetros = distanciaMetros;
        this.duracionSegundos = duracionSegundos;
        this.origen = origen;
    }

    /** La ruta que devolvió OSRM por las calles. */
    public static Ruta desdeOsrm(List<GeoPoint> puntos, double distanciaMetros,
                                 double duracionSegundos) {
        return new Ruta(puntos, distanciaMetros, duracionSegundos, Origen.OSRM);
    }

    /**
     * El plan B: el segmento directo, con la distancia real en línea
     * recta. La duración se deja a cero porque no se puede saber cuánto
     * se tarda en un tramo sin calles.
     *
     * El motivo se guarda aparte porque el aviso al usuario cambia: "no
     * tienes conexión" y "el servicio de rutas está fallando" son
     * problemas distintos, con soluciones distintas, y decir siempre lo
     * primero manda a la gente a revisar el Wi-Fi cuando el fallo es
     * del servicio.
     */
    public static Ruta enLineaRecta(List<GeoPoint> puntos, double distanciaMetros,
                                    boolean sinConexion) {
        return new Ruta(puntos, distanciaMetros, 0,
                sinConexion ? Origen.SIN_CONEXION : Origen.ERROR_SERVICIO);
    }

    public boolean esReal() {
        return origen == Origen.OSRM;
    }

    /** Los kilómetros, que es como los muestra la app. */
    public double kilometros() {
        return distanciaMetros / 1000.0;
    }

    /** Los minutos de trayecto, redondeados hacia arriba y sin decimales. */
    public int minutos() {
        return (int) Math.ceil(duracionSegundos / 60.0);
    }
}
