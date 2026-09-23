package com.ubicafe.app.ui.marcas;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.ubicafe.app.R;
import com.ubicafe.app.datos.RepositorioDatos;
import com.ubicafe.app.modelo.MarcaCafe;

/**
 * LISTA DE MARCAS DE CAFÉ (P13).
 * Muestra todas las marcas registradas y una CTA final.
 */
public class ListaMarcasActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lista_marcas);

        ((TextView) findViewById(R.id.texto_titulo)).setText(getString(R.string.marcas_titulo));
        ((TextView) findViewById(R.id.texto_subtitulo))
                .setText(getString(R.string.marcas_subtitulo));

        findViewById(R.id.btn_volver).setOnClickListener(v -> finish());

        llenarMarcas();
    }

    private void llenarMarcas() {
        LinearLayout contenedor = findViewById(R.id.lista_marcas);
        for (MarcaCafe marca : RepositorioDatos.obtenerMarcas()) {
            View tarjeta = getLayoutInflater().inflate(R.layout.item_marca, contenedor, false);

            ((TextView) tarjeta.findViewById(R.id.texto_nombre)).setText(marca.nombre);
            ((TextView) tarjeta.findViewById(R.id.texto_etiquetas))
                    .setText(android.text.TextUtils.join(", ", marca.etiquetas));

            tarjeta.setOnClickListener(v -> {
                Intent intento = new Intent(this, DetalleMarcaActivity.class);
                intento.putExtra(DetalleMarcaActivity.EXTRA_NOMBRE_MARCA, marca.nombre);
                startActivity(intento);
            });
            contenedor.addView(tarjeta);
        }
    }
}