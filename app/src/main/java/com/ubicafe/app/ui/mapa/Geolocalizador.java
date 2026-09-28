package com.ubicafe.app.ui.mapa;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;

import androidx.core.content.ContextCompat;

import com.ubicafe.app.util.Distancia;

/**
 * GEOLOCALIZACIÓN
 * ---------------------------------------------------------------
 * "Cerca de ti" es la única parte de la app que necesita saber dónde
 * está la persona, y es opcional: sin permiso, la app sigue completa y
 * solo deja de mostrar distancias.
 *
 * Se usa ACCESS_COARSE_LOCATION a propósito. El censo no necesita la
 * calle exacta para decir "a 450 m"; pedirla sería pedir más de la
 * cuenta.
 */
public final class Geolocalizador {

    private static final String ETIQUETA = "UbiCafe.Ubicacion";

    /** Si la persona ya denegó el permiso de ubicación. */
    private static final String PREFS_DENEGADO = "ubicacion_denegada";

    private Geolocalizador() {
        // Clase de utilidades: no se instancia.
    }

    /** true si ya se le concedió el permiso de ubicación aproximada. */
    public static boolean tienePermiso(Context contexto) {
        return ContextCompat.checkSelfPermission(contexto,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * true si se le pidió el permiso alguna vez y lo denegó. Android
     * vuelve a preguntar igual, pero conviene poder decir "vuelve a
     * activarlo en Ajustes" en vez de insistir con un diálogo que la
     * persona ya rechazó dos veces.
     */
    public static boolean permisoDenegado(Context contexto) {
        return !tienePermiso(contexto)
                && preferencias(contexto).getBoolean(PREFS_DENEGADO, false);
    }

    /**
     * Registra la respuesta al permiso: si lo denegó, se apunta para
     * poder ajustar el texto y no volver a pedirlo a lo loco.
     */
    public static void recordarDecision(Context contexto, boolean concedido) {
        if (concedido) {
            return;
        }
        preferencias(contexto).edit().putBoolean(PREFS_DENEGADO, true).apply();
    }

    /**
     * La última posición conocida del dispositivo, o null si no hay
     * permiso, no hay proveedor encendido o nunca se obtuvo una lectura.
     * Nunca lanza: degradar en silencio es lo correcto aquí.
     */
    public static Location ultimaPosicion(Context contexto) {
        if (!tienePermiso(contexto)) {
            return null;
        }
        LocationManager gestor =
                (LocationManager) contexto.getSystemService(Context.LOCATION_SERVICE);
        if (gestor == null) {
            return null;
        }
        try {
            // Se prueban los proveedores de menos a más preciso. La
            // red es la única que no gasta batería, así que va primera.
            for (String proveedor : new String[]{
                    LocationManager.NETWORK_PROVIDER,
                    LocationManager.GPS_PROVIDER,
                    LocationManager.PASSIVE_PROVIDER}) {
                if (!gestor.isProviderEnabled(proveedor)) {
                    continue;
                }
                Location posicion = gestor.getLastKnownLocation(proveedor);
                if (posicion != null) {
                    return posicion;
                }
            }
        } catch (SecurityException sinPermiso) {
            // El permiso se revocó entre el chequeo y la llamada.
            return null;
        }
        return null;
    }

    /** true si ya sabemos dónde está la persona y podemos calcular distancias. */
    public static boolean hayPosicion(Context contexto) {
        return ultimaPosicion(contexto) != null;
    }

    /**
     * La distancia de un punto del censo, ya formateada ("A 450 m de ti"),
     * o el texto de estado cuando no hay ubicación. Nunca devuelve null
     * para que las tarjetas no tengan que decidir nada.
     */
    public static String distanciaATexto(Context contexto, double lat, double lng) {
        Location posicion = ultimaPosicion(contexto);
        if (posicion == null) {
            return SIN_UBICACION;
        }
        return Distancia.formatear(Distancia.entre(posicion.getLatitude(),
                posicion.getLongitude(), lat, lng));
    }

    /**
     * Los kilómetros de un punto, o -1 si todavía no se sabe dónde está
     * la persona. El -1 hace de bandera: ninguna distancia real es
     * negativa.
     */
    public static double kilometrosA(Context contexto, double lat, double lng) {
        Location posicion = ultimaPosicion(contexto);
        if (posicion == null) {
            return -1;
        }
        return Distancia.entre(posicion.getLatitude(), posicion.getLongitude(), lat, lng);
    }

    private static android.content.SharedPreferences preferencias(Context contexto) {
        return contexto.getApplicationContext()
                .getSharedPreferences("ubicafe", android.content.Context.MODE_PRIVATE);
    }

    /** Texto para cuando no hay permiso o posición: se invita a activarla. */
    public static final String SIN_UBICACION = "Activa la ubicación para ver distancias";

    /** La distancia de un lugar ya con el preposición, para las tarjetas. */
    public static String formatear(double kilometros) {
        return "A " + Distancia.formatear(kilometros) + " de ti";
    }
}
