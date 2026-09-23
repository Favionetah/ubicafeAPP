package com.ubicafe.app.ui.origen;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.ubicafe.app.R;
import com.ubicafe.app.datos.RepositorioDatos;
import com.ubicafe.app.modelo.CafeOrigen;

/**
 * FICHA DE ORIGEN de un café de especialidad.
 * Muestra los detalles de la variedad: origen, altitud, proceso, tostado, aroma y notas.
 */
public class FichaOrigenActivity extends AppCompatActivity {

    public static final String EXTRA_VARIEDAD = "variedad";
    public static final String EXTRA_NOMBRE_MARCA = "nombre_marca";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ficha_origen);

        findViewById(R.id.btn_volver).setOnClickListener(v -> finish());

        String variedad = getIntent().getStringExtra(EXTRA_VARIEDAD);
        String nombreMarca = getIntent().getStringExtra(EXTRA_NOMBRE_MARCA);
        CafeOrigen cafe = RepositorioDatos.obtenerCafePorVariedad(variedad);

        if (cafe == null) {
            finish();
            return;
        }

        ((TextView) findViewById(R.id.texto_titulo)).setText(getString(R.string.ficha_origen_titulo));
        ((TextView) findViewById(R.id.texto_variedad)).setText(cafe.variedad);

        // Línea "Marca: X" solo cuando se conoce la marca (no en búsqueda genérica).
        TextView textoMarca = findViewById(R.id.texto_marca);
        if (nombreMarca == null || nombreMarca.isEmpty()) {
            textoMarca.setVisibility(View.GONE);
        } else {
            textoMarca.setText(getString(R.string.ficha_campo_marca) + ": " + nombreMarca);
        }

        // Campos de la ficha (etiqueta + valor)
        LinearLayout contenedor = findViewById(R.id.contenedor_ficha);
        agregarCampo(contenedor, getString(R.string.ficha_campo_origen), cafe.origen);
        agregarCampo(contenedor, getString(R.string.ficha_campo_altitud),
                cafe.altitud + " m s.n.m.");
        agregarCampo(contenedor, getString(R.string.ficha_campo_variedad), cafe.variedad);
        agregarCampo(contenedor, getString(R.string.ficha_campo_proceso), cafe.proceso);
        agregarCampo(contenedor, getString(R.string.ficha_campo_tostado), cafe.tostado);
        agregarCampo(contenedor, getString(R.string.ficha_campo_aroma), cafe.aroma);
        agregarCampo(contenedor, getString(R.string.ficha_campo_notas), cafe.notasCata);
    }

    private void agregarCampo(LinearLayout contenedor, String etiqueta, String valor) {
        View fila = getLayoutInflater().inflate(R.layout.item_par_info, contenedor, false);
        ((TextView) fila.findViewById(R.id.texto_etiqueta)).setText(etiqueta);
        ((TextView) fila.findViewById(R.id.texto_valor)).setText(valor);
        contenedor.addView(fila);
    }
}