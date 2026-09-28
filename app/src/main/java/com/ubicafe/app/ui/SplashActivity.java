package com.ubicafe.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.ubicafe.app.R;
import com.ubicafe.app.UbiCafeApp;

/**
 * PANTALLA DE CARGA (P1)
 * ---------------------------------------------------------------
 * Muestra el logo mientras se lee el censo y, como mucho, un segundo y
 * medio para que el logo se vea. Las dos cosas a la vez: si el JSON
 * tarda, la pantalla espera, y si tarda poco, la marca igual se muestra
 * el tiempo acordado. Pasar a las pestañas con el censo a medio leer
 * haría que la primera lista saliera vacía.
 */
public class SplashActivity extends AppCompatActivity {

    private static final long TIEMPO_MOSTRADO_MS = 1500;

    /**
     * Red de seguridad. Si el censo no llega en este tiempo se pasa igual:
     * las pantallas ya saben mostrar su propio estado de error, y una
     * pantalla de carga que no acaba nunca es peor que un mensaje.
     */
    private static final long ESPERA_MAXIMA_MS = 6000;

    private final Handler manejador = new Handler(Looper.getMainLooper());

    private long inicio;
    private boolean censoRespondido;
    private boolean abriendo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        inicio = System.currentTimeMillis();

        manejador.postDelayed(this::abrir, ESPERA_MAXIMA_MS);
        ((UbiCafeApp) getApplication()).esperarCenso(disponible -> {
            censoRespondido = true;
            intentarAbrir();
        });
    }

    /**
     * Se pasa a la siguiente pantalla cuando ya se cumplió el tiempo de
     * marca y el censo llegó, o cuando se agotó la espera máxima.
     */
    private void intentarAbrir() {
        if (abriendo) {
            return;
        }
        if (!censoRespondido && System.currentTimeMillis() - inicio < ESPERA_MAXIMA_MS) {
            return;
        }
        manejador.postDelayed(this::abrir,
                Math.max(0, TIEMPO_MOSTRADO_MS - (System.currentTimeMillis() - inicio)));
    }

    private void abrir() {
        if (abriendo || isFinishing() || isDestroyed()) {
            return;
        }
        // El timeout y el callback compiten por pasar a la siguiente
        // pantalla. Sin esta bandera, el que llegara segundo abriría
        // una segunda Activity encima de la primera.
        abriendo = true;
        startActivity(new Intent(this, OnboardingActivity.class));
        finish();
    }
}
