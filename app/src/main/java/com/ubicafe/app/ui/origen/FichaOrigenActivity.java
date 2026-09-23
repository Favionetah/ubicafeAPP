package com.ubicafe.app.ui.origen;

import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.ubicafe.app.R;
import com.ubicafe.app.datos.RepositorioDatos;
import com.ubicafe.app.modelo.CafeOrigen;

/**
 * FICHA DE ORIGEN de un café de especialidad (P9a).
 * Muestra los datos de la ficha técnica, las notas sensoriales
 * y la presentación con precio observado.
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

        // Cabecera: variedad + marca (o "Ficha de Origen" cuando no se conoce).
        ((TextView) findViewById(R.id.texto_titulo)).setText(cafe.variedad);
        TextView subtitulo = findViewById(R.id.texto_subtitulo);
        if (nombreMarca != null && !nombreMarca.isEmpty()) {
            subtitulo.setText(getString(R.string.ficha_marca_formato, nombreMarca));
        } else {
            subtitulo.setText(getString(R.string.ficha_origen_titulo));
        }

        ((TextView) findViewById(R.id.texto_inicial_hero))
                .setText(String.valueOf(cafe.variedad.charAt(0)));

        LinearLayout contenedor = findViewById(R.id.contenedor_ficha);
        agregarCampo(contenedor, getString(R.string.ficha_campo_origen), cafe.origen);
        agregarCampo(contenedor, getString(R.string.ficha_campo_altitud),
                cafe.altitud + " m s. n. m.");
        agregarCampo(contenedor, getString(R.string.ficha_campo_variedad), cafe.variedad);
        agregarCampo(contenedor, getString(R.string.ficha_campo_proceso), cafe.proceso);

        llenarNotas(cafe.aroma);
    }

    private void agregarCampo(LinearLayout contenedor, String etiqueta, String valor) {
        ViewGroup fila = (ViewGroup) getLayoutInflater()
                .inflate(R.layout.item_par_info, contenedor, false);
        ((TextView) fila.findViewById(R.id.texto_etiqueta)).setText(etiqueta);
        ((TextView) fila.findViewById(R.id.texto_valor)).setText(valor);
        contenedor.addView(fila);
    }

    /** Notas sensoriales como chips (separadas por coma o "y" en el texto). */
    private void llenarNotas(String aroma) {
        LinearLayout fila = findViewById(R.id.fila_notas);
        for (String nota : aroma.split("[,;]| y |\\s+")) {
            if (nota.trim().isEmpty()) {
                continue;
            }
            TextView chip = new TextView(this);
            chip.setText(nota.trim());
            chip.setTextSize(12);
            chip.setTypeface(android.graphics.Typeface.create(
                    "sans-serif-medium", android.graphics.Typeface.NORMAL));
            chip.setTextColor(getColor(R.color.verde_oscuro));
            chip.setBackgroundResource(R.drawable.fondo_badge_zona);
            chip.setPadding(dp(10), dp(4), dp(10), dp(4));
            chip.setGravity(Gravity.CENTER);

            LinearLayout.LayoutParams parametros =
                    new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT);
            parametros.rightMargin = dp(8);
            fila.addView(chip, parametros);
        }
    }

    private int dp(int valor) {
        return Math.round(valor * getResources().getDisplayMetrics().density);
    }
}