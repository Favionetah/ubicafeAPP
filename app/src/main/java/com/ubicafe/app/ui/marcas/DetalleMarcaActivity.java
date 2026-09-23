package com.ubicafe.app.ui.marcas;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.ubicafe.app.R;
import com.ubicafe.app.datos.RepositorioDatos;
import com.ubicafe.app.modelo.CafeOrigen;
import com.ubicafe.app.modelo.MarcaCafe;
import com.ubicafe.app.ui.mapa.MapaActivity;
import com.ubicafe.app.ui.origen.FichaOrigenActivity;

/**
 * DETALLE DE UNA MARCA DE CAFÉ (P8).
 * Muestra las etiquetas de la marca, sus cafés de origen y sus puntos de venta.
 */
public class DetalleMarcaActivity extends AppCompatActivity {

    public static final String EXTRA_NOMBRE_MARCA = "nombre_marca";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalle_marca);

        findViewById(R.id.btn_volver).setOnClickListener(v -> finish());

        String nombre = getIntent().getStringExtra(EXTRA_NOMBRE_MARCA);
        MarcaCafe marca = RepositorioDatos.obtenerMarca(nombre);

        if (marca == null) {
            finish();
            return;
        }

        // Cabecera con el nombre de la marca y su estado de registro.
        ((TextView) findViewById(R.id.texto_titulo)).setText(marca.nombre);
        ((TextView) findViewById(R.id.texto_subtitulo))
                .setText(getString(R.string.marca_nacional_registrada));

        llenarEtiquetas(marca);
        llenarCafes(marca);
        llenarDondeEncontrar(marca);
    }

    /** Etiquetas de la marca como chips pega: Cafetería · Tostaduría · Productor. */
    private void llenarEtiquetas(MarcaCafe marca) {
        LinearLayout fila = findViewById(R.id.fila_etiquetas);
        for (String etiqueta : marca.etiquetas) {
            TextView chip = new TextView(this);
            chip.setText(etiqueta);
            chip.setTextSize(12);
            chip.setTypeface(android.graphics.Typeface.create(
                    "sans-serif-medium", android.graphics.Typeface.NORMAL));
            chip.setTextColor(getColor(R.color.verde_oscuro));
            chip.setBackgroundResource(R.drawable.fondo_badge_zona);
            chip.setPadding(
                    dp(12), dp(5), dp(12), dp(5));
            chip.setGravity(Gravity.CENTER);

            LinearLayout.LayoutParams parametros =
                    new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT);
            parametros.rightMargin = dp(8);
            fila.addView(chip, parametros);
        }
    }

    /** Lista de cafés de especialidad de la marca. */
    private void llenarCafes(MarcaCafe marca) {
        LinearLayout contenedorCafes = findViewById(R.id.contenedor_cafes);
        for (CafeOrigen cafe : marca.cafésOrigen) {
            ViewGroup tarjeta = (ViewGroup) getLayoutInflater()
                    .inflate(R.layout.item_cafe_origen, contenedorCafes, false);

            ((TextView) tarjeta.findViewById(R.id.texto_variedad)).setText(cafe.variedad);
            ((TextView) tarjeta.findViewById(R.id.texto_origen))
                    .setText(cafe.origen + " · " + cafe.altitud + " m");
            ((TextView) tarjeta.findViewById(R.id.texto_notas)).setText(cafe.aroma);

            tarjeta.setOnClickListener(v -> {
                Intent intento = new Intent(this, FichaOrigenActivity.class);
                intento.putExtra(FichaOrigenActivity.EXTRA_VARIEDAD, cafe.variedad);
                intento.putExtra(FichaOrigenActivity.EXTRA_NOMBRE_MARCA, marca.nombre);
                startActivity(intento);
            });
            contenedorCafes.addView(tarjeta);
        }
    }

    /** ¿Dónde encontrar la marca? + acceso al mapa de puntos de venta. */
    private void llenarDondeEncontrar(MarcaCafe marca) {
        int puntos = RepositorioDatos.contarPuntosDeVentaDe(marca.nombre);
        int cafeterias = 0;
        for (com.ubicafe.app.modelo.Cafeteria c : RepositorioDatos.obtenerCafeterias()) {
            if (marca.nombre.equalsIgnoreCase(c.marca)) {
                cafeterias++;
            }
        }
        int total = puntos + cafeterias;

        ((TextView) findViewById(R.id.texto_descripcion))
                .setText(getString(R.string.marca_puntos_descripcion, marca.nombre, total));

        findViewById(R.id.btn_ver_puntos_mapa)
                .setOnClickListener(v -> startActivity(new Intent(this, MapaActivity.class)));
    }

    private int dp(int valor) {
        return Math.round(valor * getResources().getDisplayMetrics().density);
    }
}