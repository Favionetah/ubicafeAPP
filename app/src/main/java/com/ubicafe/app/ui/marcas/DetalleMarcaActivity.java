package com.ubicafe.app.ui.marcas;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.ubicafe.app.R;
import com.ubicafe.app.datos.RepositorioDatos;
import com.ubicafe.app.modelo.CafeOrigen;
import com.ubicafe.app.modelo.MarcaCafe;
import com.ubicafe.app.ui.origen.FichaOrigenActivity;

/**
 * DETALLE DE UNA MARCA DE CAFÉ.
 * Muestra el encabezado de la marca, su descripción y sus cafés de origen.
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

        ((TextView) findViewById(R.id.texto_titulo)).setText(getString(R.string.categoria_marcas));

        // Encabezado
        TextView textoNombre = findViewById(R.id.texto_nombre);
        textoNombre.setText(marca.nombre);
        ((TextView) findViewById(R.id.texto_etiquetas))
                .setText(android.text.TextUtils.join(" · ", marca.etiquetas));
        ((TextView) findViewById(R.id.texto_inicial)).setText(String.valueOf(marca.inicial));
        ((TextView) findViewById(R.id.texto_descripcion)).setText(marca.descripcion);

        // Cuadro de color de la marca
        GradientDrawable cuadro = new GradientDrawable();
        cuadro.setShape(GradientDrawable.RECTANGLE);
        cuadro.setCornerRadius(16f);
        cuadro.setColor(getColor(marca.colorMarca));
        findViewById(R.id.box_color).setBackground(cuadro);

        // Chip "Marca nacional registrada" solo si es nacional
        TextView textoRegistro = findViewById(R.id.texto_registro);
        if (!marca.esNacional) {
            textoRegistro.setVisibility(View.GONE);
        }

        // Lista de cafés de especialidad
        LinearLayout contenedorCafes = findViewById(R.id.contenedor_cafes);
        for (CafeOrigen cafe : marca.cafésOrigen) {
            View tarjeta = getLayoutInflater()
                    .inflate(R.layout.item_cafe_origen, contenedorCafes, false);
            ((TextView) tarjeta.findViewById(R.id.texto_variedad)).setText(cafe.variedad);
            ((TextView) tarjeta.findViewById(R.id.texto_origen))
                    .setText(cafe.origen + " - " + cafe.altitud + " m s.n.m.");
            ((TextView) tarjeta.findViewById(R.id.texto_inicial))
                    .setText(cafe.variedad.substring(0, 1));

            tarjeta.setOnClickListener(v -> {
                Intent intento = new Intent(this, FichaOrigenActivity.class);
                intento.putExtra(FichaOrigenActivity.EXTRA_VARIEDAD, cafe.variedad);
                intento.putExtra(FichaOrigenActivity.EXTRA_NOMBRE_MARCA, marca.nombre);
                startActivity(intento);
            });
            contenedorCafes.addView(tarjeta);
        }
    }
}