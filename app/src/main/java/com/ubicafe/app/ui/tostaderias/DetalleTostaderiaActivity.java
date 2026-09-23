package com.ubicafe.app.ui.tostaderias;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.ubicafe.app.R;
import com.ubicafe.app.datos.RepositorioDatos;
import com.ubicafe.app.modelo.Tostaderia;

/**
 * DETALLE DE UNA TOSTADURÍA.
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

        ((TextView) findViewById(R.id.texto_titulo))
                .setText(getString(R.string.categoria_tostaderias));
        ((TextView) findViewById(R.id.texto_nombre)).setText(tostaduría.nombre);
        ((TextView) findViewById(R.id.texto_region)).setText(tostaduría.region);
        ((TextView) findViewById(R.id.texto_descripcion)).setText(tostaduría.descripcion);

        // Tarjeta con horarios, ubicación y contacto
        LinearLayout contenedor = findViewById(R.id.contenedor_info);
        agregarCampo(contenedor, getString(R.string.tostaderia_horarios), tostaduría.horarios);
        agregarCampo(contenedor, getString(R.string.tostaderia_ubicacion), tostaduría.direccion);
        agregarCampo(contenedor, getString(R.string.tostaderia_contacto), tostaduría.contacto);
    }

    private void agregarCampo(LinearLayout contenedor, String etiqueta, String valor) {
        View fila = getLayoutInflater().inflate(R.layout.item_par_info, contenedor, false);
        ((TextView) fila.findViewById(R.id.texto_etiqueta)).setText(etiqueta);
        ((TextView) fila.findViewById(R.id.texto_valor)).setText(valor);
        contenedor.addView(fila);
    }
}