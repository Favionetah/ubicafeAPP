package com.ubicafe.app.ui.mapa;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.core.content.ContextCompat;

import com.ubicafe.app.util.Distancia;

/**
 * GEOLOCALIZACIÓN
 * ---------------------------------------------------------------
 * "Cerca de ti" es la única parte de la app que necesita saber dónde
 * está la persona, y es opcional: sin permiso, la app sigue completa y
 * solo deja de mostrar distancias.
 *
 * Se usa ACCESS_COARSE_LOCATION a propósito para las distancias. El
 * censo no necesita la calle exacta para decir "a 450 m"; pedirla sería
 * pedir más de la cuenta.
 *
 * La excepción es "Cómo llegar". Ahí sí hace falta la ubicación
 * precisa, porque una ruta calculada desde un punto kilometer de
 * distancia es una ruta que no lleva a ningún sitio. Por eso ese botón
 * pide ACCESS_FINE_LOCATION, y por eso el botón de la ficha dice que
 * hace falta activarla si no la hay.
 */
public final class Geolocalizador {

    private static final String ETIQUETA = "UbiCafe.Ubicacion";

    /** Si la persona ya denegó el permiso de ubicación. */
    private static final String PREFS_DENEGADO = "ubicacion_denegada";

    /**
     * Cuánto se espera una posición fresca antes de conformarse con la
     * última conocida. Diez segundos es lo que tarda un GPS en interiores
     * con cielo parcial; más que eso, la pantalla de "buscando tu
     * ubicación" se hace eterna, y quien espera la pasa peor con una
     * ruta medio aproximada que con un mensaje claro.
     */
    private static final int SEGUNDOS_ESPERA = 10;

    private Geolocalizador() {
        // Clase de utilidades: no se instancia.
    }

    /**
     * true si ya se le concedió el permiso de ubicación aproximada.
     * Con este basta para ordenar "Cerca de ti": no hace falta saber en
     * qué calle está la persona, solo en qué zona.
     */
    public static boolean tienePermiso(Context contexto) {
        return ContextCompat.checkSelfPermission(contexto,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * true si tenemos la ubicación precisa, que es lo que hace falta
     * para calcular una ruta: con la aproximada, el punto de partida
     * puede estar a un kilómetro del sitio real y el trayecto se
     * calcularía desde el sitio equivocado.
     *
     * Si solo se concedió la aproximada, esto devuelve false. Es
     * correcto: es preferible decir que hace falta el permiso exacto a
     * dibujar una ruta que sale de la esquina equivocada.
     */
    public static boolean tienePermisoPreciso(Context contexto) {
        return ContextCompat.checkSelfPermission(contexto,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Los dos permisos que hacen falta para calcular una ruta. Se piden
     * juntos porque en Android 12 y posteriores granting uno concede
     * aproximadamente el otro, y pedir los dos de una vez evita el
     * segundo diálogo.
     */
    public static String[] permisosDeRuta() {
        return new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        };
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

    /**
     * Pide una lectura de ubicación AHORA y avisa por el Callable.
     *
     * "Cómo llegar" no puede conformarse con ultimaPosicion(): esa
     * devuelve el último punto que el teléfono tenga guardado, que puede
     * ser de hace media hora y estar a un kilómetro de donde está la
     * persona. Una ruta calculada desde ahí no lleva a ningún sitio, y
     * quien la ve no tiene forma de saber que el error es nuestro. Por
     * eso aquí se pide un fix de verdad.
     *
     * Se cubren las dos APIs porque minSdk es 24: getCurrentLocation es
     * de Android 11 (API 30) y por debajo se usa requestSingleUpdate,
     * que está obsoleta pero sigue funcionando.
     *
     * Si en SEGUNDOS_ESPERA no llega nada, se entrega la última posición
     * conocida aunque sea vieja: es mejor un punto aproximado que dejar
     * el mapa a medias, y el panel deja claro que no es una lectura
     * fresca. Si tampoco hay ninguna, se avisa por sinPosicion().
     */
    public static Cancelacion pedirPosicionActual(Context contexto,
                                                  EscuchaPosicion escucha) {
        LocationManager gestor =
                (LocationManager) contexto.getSystemService(Context.LOCATION_SERVICE);
        if (gestor == null || !tienePermisoPreciso(contexto)) {
            // Sin permiso exacto no se puede calcular una ruta, y no
            // tiene sentido quedarse esperando una.
            escucha.sinPosicion();
            return new Cancelacion(null);
        }

        String proveedor = proveedorVivo(gestor, true);
        if (proveedor == null) {
            proveedor = proveedorVivo(gestor, false);
        }
        if (proveedor == null) {
            // GPS y red apagados: no hay de dónde sacar un punto.
            escucha.sinPosicion();
            return new Cancelacion(null);
        }
        return new Peticion(gestor, proveedor, escucha).pedir();
    }

    /**
     * El primer proveedor encendido. Se prueban de más a menos preciso,
     * al revés que en ultimaPosicion(): aquí se quiere la mejor lectura
     * posible aunque gaste batería, porque es una sola petición para una
     * ruta y no un seguimiento continuo.
     */
    private static String proveedorVivo(LocationManager gestor, boolean gps) {
        String[] candidatos = gps
                ? new String[]{LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER}
                : new String[]{LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER};
        for (String proveedor : candidatos) {
            if (gestor.isProviderEnabled(proveedor)) {
                return proveedor;
            }
        }
        return null;
    }

    /** Para recibir el resultado de pedirPosicionActual. */
    public interface EscuchaPosicion {
        void conPosicion(Location posicion);
        void sinPosicion();
    }

    /**
     * La petición en sí. Es una clase y no un método largo porque tiene
     * que vivir enterita en memoria hasta que llegue el resultado: si
     * fuera un método, el oyente que se registra en el sistema se
     *.collectaría y la respuesta se perdería sin avisar.
     */
    private static final class Peticion implements LocationListener {

        private final LocationManager gestor;
        private final String proveedor;
        private final EscuchaPosicion escucha;
        private final Handler reloj;
        private final Runnable alAgotarseElTiempo;
        private boolean yaRespondido;

        Peticion(LocationManager gestor, String proveedor, EscuchaPosicion escucha) {
            this.gestor = gestor;
            this.proveedor = proveedor;
            this.escucha = escucha;
            this.reloj = new Handler(Looper.getMainLooper());
            this.alAgotarseElTiempo = new Runnable() {
                @Override
                public void run() {
                    // Se acabó la espera. Se entrega lo último que
                    // hubiera aunque sea viejo, porque un punto viejo
                    // orienta; si no hay ninguno, se avisa.
                    Location ultima = null;
                    try {
                        ultima = gestor.getLastKnownLocation(proveedor);
                    } catch (SecurityException revocado) {
                        ultima = null;
                    }
                    if (ultima != null) {
                        responder(ultima);
                    } else {
                        sinPosicion();
                    }
                }
            };
        }

        Cancelacion pedir() {
            reloj.postDelayed(alAgotarseElTiempo, SEGUNDOS_ESPERA * 1000L);
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    // La API moderna: no hay que registrarse como oyente
                    // y se cancela sola. El Executor corre la respuesta en
                    // el hilo principal, que es donde se pinta el mapa.
                    gestor.getCurrentLocation(proveedor, null, reloj::post, this::recibir);
                } else {
                    gestor.requestSingleUpdate(proveedor, this, Looper.getMainLooper());
                }
            } catch (SecurityException revocado) {
                // El permiso se quitó entre el chequeo y la llamada.
                reloj.removeCallbacks(alAgotarseElTiempo);
                escucha.sinPosicion();
            }
            return new Cancelacion(this);
        }

        /** Solo responde la primera vez: el fix y el reloj pueden tocar a la vez. */
        private void responder(Location posicion) {
            if (yaRespondido) {
                return;
            }
            yaRespondido = true;
            soltar();
            escucha.conPosicion(posicion);
        }

        private void sinPosicion() {
            if (yaRespondido) {
                return;
            }
            yaRespondido = true;
            soltar();
            escucha.sinPosicion();
        }

        private void recibir(Location posicion) {
            if (posicion != null) {
                responder(posicion);
            }
        }

        /** Quita el oyente y el reloj: si no, seguirían vivos sin nadie que los espere. */
        private void soltar() {
            reloj.removeCallbacks(alAgotarseElTiempo);
            try {
                gestor.removeUpdates(this);
            } catch (SecurityException revocado) {
                // Nada que hacer.
            }
        }

        @Override
        public void onLocationChanged(Location posicion) {
            recibir(posicion);
        }

        // Los tres de abajo dejaron de ser obligatorios en la API 30, pero
        // en Android 7 y 8 el sistema sí los llama, así que se dejan vacíos
        // para no comerse un AbstractMethodError en un teléfono viejo.
        @Override
        public void onStatusChanged(String proveedor, int estado, Bundle extras) {
        }

        @Override
        public void onProviderEnabled(String proveedor) {
        }

        @Override
        public void onProviderDisabled(String proveedor) {
        }
    }

    /**
     * Lo que hay que llamar para dejar de esperar la posición. La
     * pantalla la guarda y la llama en onDestroy: una petición que nadie
     * cancela sigue despertando el teléfono con la app cerrada.
     */
    public static final class Cancelacion {

        private final Peticion peticion;

        Cancelacion(Peticion peticion) {
            this.peticion = peticion;
        }

        public void cancelar() {
            if (peticion == null) {
                return;
            }
            peticion.yaRespondido = true;
            peticion.soltar();
        }
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
