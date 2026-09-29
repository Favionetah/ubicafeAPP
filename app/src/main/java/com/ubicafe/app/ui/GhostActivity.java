package com.ubicafe.app.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.ubicafe.app.R;

/**
 * PANTALLA SECRETA (ghost page).
 * Solo se abre al tocar el patrón Inicio·Inicio·Inicio·Mapa·Mapa·Mapa·
 * Explorar·Explorar·Explorar·Inicio·Mapa·Explorar
 * en la barra inferior de MainActivity.
 */
public class GhostActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ghost);

        ((TextView) findViewById(R.id.texto_titulo)).setText(R.string.ghost_titulo);
        ((TextView) findViewById(R.id.texto_subtitulo)).setVisibility(View.GONE);

        findViewById(R.id.btn_volver).setOnClickListener(v -> finish());
    }
}