package com.ubicafe.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.ubicafe.app.R;

/**
 * ONBOARDING (P2).
 * Carrusel deslizable de 3 diapositivas. El usuario puede deslizar,
 * usar "Siguiente" para avanzar o "Omitir"/"Comenzar" para entrar.
 */
public class OnboardingActivity extends AppCompatActivity {

    private OnboardingAdapter adaptador;
    private Button botonAvanzar;
    private TextView botonOmitir;
    private LinearLayout indicador;
    private View[] puntosIndicador;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        adaptador = new OnboardingAdapter(this);

        ViewPager2 pager = findViewById(R.id.pager_onboarding);
        pager.setAdapter(adaptador);

        botonAvanzar = findViewById(R.id.btn_avanzar);
        botonOmitir = findViewById(R.id.btn_omitir);
        indicador = findViewById(R.id.indicador_paginas);
        crearPuntosIndicador(adaptador.obtenerCantidad());

        botonAvanzar.setOnClickListener(v -> avanzar(pager));
        botonOmitir.setOnClickListener(v -> entrar());

        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int posicion) {
                actualizarIndicador(posicion);
                actualizarBoton(posicion);
            }
        });

        actualizarIndicador(0);
        actualizarBoton(0);
    }

    /** Avanza una diapositiva; en la última, entra a la app. */
    private void avanzar(ViewPager2 pager) {
        int posicion = pager.getCurrentItem();
        if (adaptador.esUltima(posicion)) {
            entrar();
        } else {
            pager.setCurrentItem(posicion + 1, true);
        }
    }

    private void entrar() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    /** Crea N puntos y deja el primero como "activo". */
    private void crearPuntosIndicador(int cantidad) {
        puntosIndicador = new View[cantidad];
        for (int i = 0; i < cantidad; i++) {
            View punto = new View(this);
            LinearLayout.LayoutParams parametros =
                    new LinearLayout.LayoutParams(
                            i == 0 ? dp(20) : dp(6),
                            dp(6));
            if (i > 0) {
                parametros.leftMargin = dp(6);
            }
            punto.setLayoutParams(parametros);
            indicador.addView(punto);
            puntosIndicador[i] = punto;
        }
    }

    /** Pinta los puntos según la página activa (píldora verde vs punto gris). */
    private void actualizarIndicador(int posicion) {
        for (int i = 0; i < puntosIndicador.length; i++) {
            boolean activo = i == posicion;
            puntosIndicador[i].setBackgroundResource(activo
                    ? R.drawable.fondo_punto_activo
                    : R.drawable.fondo_punto_inactivo);

            ViewGroup.LayoutParams parametros = puntosIndicador[i].getLayoutParams();
            parametros.width = dp(activo ? 20 : 6);
            puntosIndicador[i].setLayoutParams(parametros);
        }
    }

    /** El botón principal dice "Comenzar" en la última página y "Siguiente" antes. */
    private void actualizarBoton(int posicion) {
        boolean ultima = adaptador.esUltima(posicion);
        botonAvanzar.setText(ultima
                ? getString(R.string.onboarding_comenzar)
                : getString(R.string.onboarding_siguiente));
        botonOmitir.setVisibility(ultima ? View.INVISIBLE : View.VISIBLE);
    }

    private int dp(int valor) {
        return Math.round(valor * getResources().getDisplayMetrics().density);
    }
}