package com.ubicafe.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.ubicafe.app.R;

/**
 * Pantalla de bienvenida.
 * Solo muestra el logo un momento y luego pasa al onboarding.
 */
public class SplashActivity extends AppCompatActivity {

    private static final int TIEMPO_MOSTRADO_MS = 1500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Esperar un poco, mostrar el onboarding y luego la pantalla principal.
        new Handler(Looper.getMainLooper()).postDelayed(this::abrirPróximo, TIEMPO_MOSTRADO_MS);
    }

    private void abrirPróximo() {
        startActivity(new Intent(this, OnboardingActivity.class));
        finish();
    }
}