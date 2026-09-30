package com.ubicafe.app.ui.mapa;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONObject;
import org.osmdroid.util.GeoPoint;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * CALCULAR LA RUTA EN COCHE (P3)
 * ---------------------------------------------------------------
 * Pide a OSRM el trayecto entre la posición de la persona y el local, y
 * lo devuelve como una lista de puntos lista para dibujar en osmdroid.
 *
 * POR QUÉ OSRM Y NO GOOGLE MAPS
 * Google Maps SDK pide una clave de API y una cuenta de facturación, y
 * además manda a un servidor de Google las coordenadas de cada persona
 * que usa la app. OSRM es un proyecto abierto: el servidor público se
 * puede usar sin clave ni registro, y la consulta se hace a máquina.
 * Para una app de un censo con 213 locales, esa es la opción que no
 * obliga a registrar nada.
 *
 * POR QUÉ UNA CONEXIÓN A MANO Y NO UNA LIBRERÍA
 * Una librería de rutas (Mapbox, osrm-android) trae su propio cliente
 * HTTP, su modelo de datos y a veces un parser de polyline. Aquí solo
 * hace falta una llamada GET y un campo de la respuesta, así que
 * HttpURLConnection y org.json, que ya vienen en Android, bastan. Una
 * dependencia menos que actualizar.
 *
 * NUNCA EN EL HILO PRINCIPAL
 * La consulta tarda entre 200 ms y un par de segundos, y en una ciudad
 * de La Paz con cutoff o sin datos puede ser más. Bloquear el hilo
 * principal con eso es la definición de una app que se congela, así que
 * todo va en un hilo propio y el resultado vuelve al principal.
 */
public final class CalculadoraRuta {

    private static final String ETIQUETA = "UbiCafe.Ruta";

    /**
     * Servidor público de OSRM. Es el de la demo del proyecto, pensado
     * para pruebas: aguanta poco tráfico y no hay garantía de
     * disponibilidad. Si algún día la app tiene usuarios de verdad, lo
     * propio se despliega en un servidor propio y solo cambia esta
     * constante.
     */
    private static final String BASE =
            "https://router.project-osrm.org/route/v1/driving/";

    /**
     * El servicio público pide identificarse. No es por los datos: es
     * para que quien lo opera sepa quién llama y pueda avisar si hace
     * falta.
     */
    private static final String AGENTE =
            "UbiCafe/1.0 (censo del cafe de Bolivia; appopensource)";

    private static final int ESPERA_MILISEGUNDOS = 8000;

    /** Avisa cuando la ruta está lista. Siempre en el hilo principal. */
    public interface AlCalcular {
        void rutaCalculada(Ruta ruta);
    }

    /** Un hilo para todas las consultas: se piden de a una. */
    private static final ExecutorService HILO = Executors.newSingleThreadExecutor();

    private static final Handler PRINCIPAL = new Handler(Looper.getMainLooper());

    private CalculadoraRuta() {
        // Clase de servicios: se accede por los métodos estáticos.
    }

    /**
     * Pide la ruta y avisa por el callback. No lanza nunca: si algo
     * falla, avisa con la línea recta para que el destino se siga
     * viendo.
     */
    public static void calcular(double latOrigen, double lngOrigen,
                                double latDestino, double lngDestino,
                                AlCalcular alCalcular) {
        HILO.execute(() -> {
            Ruta ruta;
            try {
                ruta = pedirRuta(latOrigen, lngOrigen, latDestino, lngDestino);
            } catch (Exception problema) {
                // Se dibuja el segmento directo y la pantalla avisa de
                // que no es el trayecto real. Lo que se distingue es el
                // motivo, porque no es lo mismo no tener cobertura que
                // tener el servicio caído: lo primero se arregla
                // encendiendo los datos, lo segundo no.
                boolean sinConexion = problema instanceof IOException;
                Log.w(ETIQUETA, sinConexion
                        ? "Sin conexión con OSRM; se dibuja la línea recta"
                        : "OSRM falló; se dibuja la línea recta", problema);
                ruta = Ruta.enLineaRecta(
                        DecodificadorPolyline.recta(new GeoPoint(latOrigen, lngOrigen),
                                new GeoPoint(latDestino, latDestino)),
                        distanciaRecta(latOrigen, lngOrigen, latDestino, latDestino),
                        sinConexion);
            }
            Ruta definitiva = ruta;
            PRINCIPAL.post(() -> alCalcular.rutaCalculada(definitiva));
        });
    }

    /** La consulta a OSRM y su respuesta. Solo se llama desde el hilo. */
    private static Ruta pedirRuta(double latOrigen, double lngOrigen,
                                  double latDestino, double lngDestino) throws Exception {
        // El orden de OSRM es longitud primero, al revés que una
        // coordenada normal, y el separador de los dos puntos es ';'.
        String url = String.format(Locale.US,
                "%s%.6f,%.6f;%.6f,%.6f?overview=full&geometries=polyline",
                BASE, lngOrigen, latOrigen, lngDestino, latDestino);

        HttpURLConnection conexion = (HttpURLConnection) new URL(url).openConnection();
        try {
            conexion.setRequestMethod("GET");
            conexion.setConnectTimeout(ESPERA_MILISEGUNDOS);
            conexion.setReadTimeout(ESPERA_MILISEGUNDOS);
            conexion.setRequestProperty("User-Agent", AGENTE);

            int codigo = conexion.getResponseCode();
            if (codigo != HttpURLConnection.HTTP_OK) {
                throw new IllegalStateException("OSRM respondió " + codigo);
            }

            String cuerpo;
            try (BufferedReader lector = new BufferedReader(new InputStreamReader(
                    conexion.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder texto = new StringBuilder();
                String linea;
                while ((linea = lector.readLine()) != null) {
                    texto.append(linea);
                }
                cuerpo = texto.toString();
            }
            return leerRuta(cuerpo, latOrigen, lngOrigen, latDestino, lngDestino);
        } finally {
            conexion.disconnect();
        }
    }

    /**
     * Saca de la respuesta de OSRM la geometría, la distancia y el
     * tiempo. Si algo no cuadra, devuelve la línea recta en vez de
     * fallar: es preferible un trayecto aproximado a un mapa en blanco.
     */
    private static Ruta leerRuta(String cuerpo, double latOrigen, double lngOrigen,
                                 double latDestino, double lngDestino) throws Exception {
        List<GeoPoint> recta = DecodificadorPolyline.recta(
                new GeoPoint(latOrigen, lngOrigen), new GeoPoint(latDestino, lngDestino));
        double enLineaRecta = distanciaRecta(latOrigen, lngOrigen, latDestino, lngDestino);

        JSONObject respuesta = new JSONObject(cuerpo);
        // OSRM no usa el código HTTP para decir que no encontró ruta: lo
        // dice en el cuerpo, con code = "NoRoute".
        if (!"Ok".equalsIgnoreCase(respuesta.optString("code", ""))) {
            return Ruta.enLineaRecta(recta, enLineaRecta, false);
        }

        if (!respuesta.has("routes") || respuesta.getJSONArray("routes").length() == 0) {
            return Ruta.enLineaRecta(recta, enLineaRecta, false);
        }

        JSONObject primera = respuesta.getJSONArray("routes").getJSONObject(0);
        List<GeoPoint> puntos = DecodificadorPolyline.aPuntos(primera.optString("geometry", ""));
        if (puntos.size() < 2) {
            return Ruta.enLineaRecta(recta, enLineaRecta, false);
        }

        return Ruta.desdeOsrm(puntos,
                primera.optDouble("distance", enLineaRecta),
                primera.optDouble("duration", 0));
    }

    /**
     * La distancia en línea recta entre dos puntos, en metros. Se usa
     * para el plan B, y coincide con lo que dice Geolocalizador: mismo
     * cálculo, mismos números.
     */
    private static double distanciaRecta(double lat1, double lng1,
                                         double lat2, double lng2) {
        return com.ubicafe.app.util.Distancia.entre(lat1, lng1, lat2, lng2) * 1000.0;
    }
}
