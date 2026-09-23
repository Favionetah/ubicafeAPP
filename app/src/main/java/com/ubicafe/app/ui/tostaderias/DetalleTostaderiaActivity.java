package com.ubicafe.app.ui.tostaderias;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.ubicafe.app.R;
import com.ubicafe.app.datos.RepositorioDatos;
import com.ubicafe.app.modelo.Tostaderia;

/**
 * DETALLE DE UNA TOSTADURÍA (P9b).
 * Muestra descripción, horarios, ubicación y contacto.
 */
public class DetalleTostaderiaActivity extends AppCompatActivity {

    public static final String EXTRA_NOMBRE_TOSTADERIA = "nombre_tostaderia";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalle_tostaderia);

        findViewById(R.id.btn_volver).setOnClickListener(v -> finish());

        String nombre = getIntent().getStringExtra(EXTRA_NOMBRE_TOSTADERIA);
        Tostaderia tostaduría = RepositorioDatos.obtenerTostaderia(nombre);

        if (tostaduría == null) {
            finish();
            return;
        }

        ((TextView) findViewById(R.id.texto_titulo)).setText(tostaduría.nombre);
        ((TextView) findViewById(R.id.texto_subtitulo))
                .setText(getString(R.string.tostaderia_tipo_detalle, tostaduría.region));
        ((TextView) findViewById(R.id.texto_descripcion)).setText(tostaduría.descripcion);
        ((TextView) findViewById(R.id.texto_horarios)).setText(tostaduría.horarios);
        ((TextView) findViewById(R.id.texto_ubicacion)).setText(tostaduría.direccion);
        ((TextView) findViewById(R.id.texto_contacto)).setText(tostaduría.contacto);
    }
}