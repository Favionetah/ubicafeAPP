package com.ubicafe.app.ui.productores;

import android.os.Bundle;
import android.graphics.drawable.GradientDrawable;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.ubicafe.app.R;
import com.ubicafe.app.datos.RepositorioDatos;
import com.ubicafe.app.modelo.Productor;

/**
 * DETALLE DE UN PRODUCTOR (finca cafetalera).
 * Muestra la finca, la familia, sus cifras y una descripción.
 */
public class DetalleProductorActivity extends AppCompatActivity {

    public static final String EXTRA_NOMBRE_FINCA = "nombre_finca";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalle_productor);

        findViewById(R.id.btn_volver).setOnClickListener(v -> finish());

        String nombreFinca = getIntent().getStringExtra(EXTRA_NOMBRE_FINCA);
        Productor productor = RepositorioDatos.obtenerProductor(nombreFinca);

        if (productor == null) {
            finish();
            return;
        }

        ((TextView) findViewById(R.id.texto_titulo)).setText(getString(R.string.productores_titulo));
        ((TextView) findViewById(R.id.texto_finca)).setText(productor.nombreFinca);
        ((TextView) findViewById(R.id.texto_familia)).setText(productor.nombreFamilia);
        ((TextView) findViewById(R.id.texto_origen)).setText(productor.origen);
        ((TextView) findViewById(R.id.texto_descripcion)).setText(productor.descripcion);

        ((TextView) findViewById(R.id.texto_num_cafes))
                .setText(String.valueOf(productor.numCafes));
        ((TextView) findViewById(R.id.texto_etiqueta_cafes))
                .setText(getResources().getQuantityString(
                        R.plurals.unidad_cafes, productor.numCafes, productor.numCafes));
        ((TextView) findViewById(R.id.texto_num_altitud))
                .setText(productor.altitud + " m");

        // Color de las tarjetas de cifra
        pintarContenedor(R.id.box_cifra_cafes, productor.colorMarca);
        pintarContenedor(R.id.box_cifra_altitud, productor.colorMarca);
    }

    /** Aplica un color de fondo suave con el color de la finca. */
    private void pintarContenedor(int idContenedor, int colorRes) {
        LinearLayout contenedor = findViewById(idContenedor);
        GradientDrawable fondo = new GradientDrawable();
        fondo.setShape(GradientDrawable.RECTANGLE);
        fondo.setCornerRadius(18f);
        fondo.setColor(getColor(colorRes));
        contenedor.setBackground(fondo);
    }
}