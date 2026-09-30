package com.ubicafe.app;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.ubicafe.app.datos.RepositorioDatos;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ARRANQUE DE LA APP
 * ---------------------------------------------------------------
 * La base de datos se abre y, si es la primera vez, se llena desde el
 * JSON de los assets. Eso implica abrir SQLite, leer 180 KB y escribir
 * 9 tablas, así que se hace en un hilo aparte al abrir la app. La Splash
 * espera a que termine antes de pasar a las pestañas, de modo que
 * ninguna pantalla llega a pintar una lista vacía por un dato que
 * todavía no estaba.
 *
 * En los arranques siguientes no hay trabajo: SembradorCenso solo
 * comprueba el sello de la tabla meta contra el JSON, que es una
 * lectura de un milisegundo.
 *
 * Quien necesite esperar usa esperarCenso(callback), que se invoca en
 * el hilo principal tanto si el censo ya estaba listo como si acaba de
 * terminar de sembrarse. No se bloquea el hilo principal con un latch:
 * eso congelaría la animación de la Splash justo mientras espera.
 *
 * Ninguna pantalla lee el JSON ni la base: todas pasan por
 * RepositorioDatos.
 */
public class UbiCafeApp extends Application {

    private static final String ETIQUETA = "UbiCafe";

    /** Avisa de que el censo terminó de leerse, bien o mal. */
    public interface AlCargarCenso {
        void censoCargado(boolean disponible);
    }

    private ExecutorService lector;
    private volatile boolean cargaTerminada;
    private volatile boolean censoDisponible;

    /** El hilo principal, para avisar a las pantallas desde el hilo del lector. */
    private final Handler principal = new Handler(Looper.getMainLooper());

    /** Los que esperan, y a los que hay que avisar en cuanto haya datos. */
    private final List<AlCargarCenso> esperando = new ArrayList<>();

    @Override
    public void onCreate() {
        super.onCreate();

        RepositorioDatos.iniciar(this);
        lector = Executors.newSingleThreadExecutor();
        lector.execute(this::cargarCenso);
    }

    private void cargarCenso() {
        boolean disponible = false;
        try {
            // hayDatos() abre la base y, si hace falta, la llena desde
            // el JSON: es el punto de entrada que las pantallas también
            // usan, así que al terminar la app y las pantallas trabajan
            // sobre la misma base ya sembrada.
            disponible = RepositorioDatos.hayDatos();
        } catch (RuntimeException problema) {
            // Un JSON corrupto no debe tumbar la app al abrir: se avisa
            // y las pantallas mostrarán su estado de error.
            Log.e(ETIQUETA, "No se pudo leer el censo", problema);
        }
        censoDisponible = disponible;
        cargaTerminada = true;
        avisarEnEspera();
    }

    /**
     * Avisa a quien espera, siempre en el hilo principal: los que
     * esperan son Activities y Fragments, y un listener que repinta
     * vistas desde el hilo del lector las lanzaría al crash. Por eso
     * el posted y no una llamada directa, aunque la carga haya
     * terminado.
     */
    private void avisarEnEspera() {
        final List<AlCargarCenso> copia;
        synchronized (esperando) {
            if (esperando.isEmpty()) {
                return;
            }
            copia = new ArrayList<>(esperando);
            esperando.clear();
        }
        principal.post(() -> {
            for (AlCargarCenso oyente : copia) {
                oyente.censoCargado(censoDisponible);
            }
        });
    }

    /**
     * Avisa cuando el censo esté disponible. Si ya lo está, avisa de
     * inmediato; si no, cuando termine de leerse.
     *
     * El oyente siempre recibe la llamada en el hilo principal, dé
     * donde venga: quien espera son pantallas, y repintar una vista
     * desde el hilo del lector la revienta.
     */
    public void esperarCenso(AlCargarCenso oyente) {
        synchronized (esperando) {
            // Se comprueba dentro del candado, no antes: la carga
            // termina en otro hilo y podría haberse completado entre
            // la prueba y aquí. Si se comprueba antes y se avisa fuera
            // del candado, el oyente queda en la lista y nadie le
            // avisa nunca.
            if (!cargaTerminada) {
                esperando.add(oyente);
                return;
            }
        }
        principal.post(() -> oyente.censoCargado(censoDisponible));
    }

    @Override
    public void onTerminate() {
        if (lector != null) {
            lector.shutdownNow();
        }
        super.onTerminate();
    }
}
