package com.ubicafe.app.ui.productores;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.ubicafe.app.R;
import com.ubicafe.app.datos.RepositorioDatos;
import com.ubicafe.app.modelo.Productor;

/**
 * LISTA DE PRODUCTORES (P11).
 * Muestra todas las fincas cafetaleras registradas y una CTA final.
 */
public class ListaProductoresActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lista_productores);

        ((TextView) findViewById(R.id.texto_titulo)).setText(getString(R.string.productores_titulo));
        ((TextView) findViewById(R.id.texto_subtitulo))
                .setText(getString(R.string.productores_subtitulo));

        findViewById(R.id.btn_volver).setOnClickListener(v -> finish());

        llenarProductores();
    }

    private void llenarProductores() {
        LinearLayout contenedor = findViewById(R.id.lista_productores);
        for (Productor productor : RepositorioDatos.obtenerProductores()) {
            View tarjeta = getLayoutInflater()
                    .inflate(R.layout.item_productor, contenedor, false);

            ((TextView) tarjeta.findViewById(R.id.texto_nombre))
                    .setText(productor.nombreFinca);
            ((TextView) tarjeta.findViewById(R.id.texto_subtitulo)).setText(productor.origen);
            ((TextView) tarjeta.findViewById(R.id.texto_conteo))
                    .setText(getResources().getQuantityString(
                            R.plurals.unidad_cafes, productor.numCafes, productor.numCafes));

            tarjeta.setOnClickListener(v -> {
                Intent intento = new Intent(this, DetalleProductorActivity.class);
                intento.putExtra(DetalleProductorActivity.EXTRA_NOMBRE_FINCA,
                        productor.nombreFinca);
                startActivity(intento);
            });
            contenedor.addView(tarjeta);
        }
    }
}