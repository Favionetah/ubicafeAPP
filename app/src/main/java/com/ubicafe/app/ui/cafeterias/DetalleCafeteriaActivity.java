package com.ubicafe.app.ui.cafeterias;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.ubicafe.app.R;
import com.ubicafe.app.datos.RepositorioDatos;
import com.ubicafe.app.modelo.Cafeteria;
import com.ubicafe.app.ui.mapa.MapaActivity;

/**
 * DETALLE DE UNA CAFETERÍA (P7).
 * Tarjeta informativa: foto, nombre, rating/distancia, descripción,
 * horarios, ubicación y botón "Cómo llegar".
 */
public class DetalleCafeteriaActivity extends AppCompatActivity {

    /** Nombre de la cafetería que se quiere ver (se pasa al abrir esta pantalla). */
    public static final String EXTRA_NOMBRE_CAFETERIA = "nombre_cafeteria";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalle_cafeteria);

        findViewById(R.id.btn_volver).setOnClickListener(v -> finish());

        // Obtener la cafetería elegida.
        String nombre = getIntent().getStringExtra(EXTRA_NOMBRE_CAFETERIA);
        Cafeteria cafeteria = RepositorioDatos.obtenerCafeteria(nombre);

        if (cafeteria == null) {
            finish();
            return;
        }

        // Encabezado sobre la foto
        String marca = cafeteria.marca == null ? cafeteria.nombre : cafeteria.marca;
        ((TextView) findViewById(R.id.texto_nombre)).setText(marca);
        ((TextView) findViewById(R.id.texto_tipo)).setText(cafeteria.tipo);

        // Rating y distancia
        ((TextView) findViewById(R.id.texto_rating)).setText("4.9 (240)");
        ((TextView) findViewById(R.id.texto_distancia)).setText("A 450 m");

        // Descripción
        ((TextView) findViewById(R.id.texto_descripcion))
                .setText(descripcionDe(cafeteria));

        // Horarios y ubicación
        ((TextView) findViewById(R.id.texto_horarios)).setText(cafeteria.horario);
        ((TextView) findViewById(R.id.texto_ubicacion)).setText(
                cafeteria.direccion + "\n" + cafeteria.zona + ", La Paz");

        // Cómo llegar
        findViewById(R.id.btn_como_llegar).setOnClickListener(
                v -> startActivity(new Intent(this, MapaActivity.class)));
    }

    /** Texto breve según el tipo de local. */
    private String descripcionDe(Cafeteria cafeteria) {
        if (cafeteria.marca != null) {
            return getString(R.string.cafeteria_descripcion_marca, cafeteria.marca,
                    cafeteria.zona);
        }
        return getString(R.string.cafeteria_descripcion_clasica, cafeteria.zona);
    }
}