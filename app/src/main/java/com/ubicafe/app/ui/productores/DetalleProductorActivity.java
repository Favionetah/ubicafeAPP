package com.ubicafe.app.ui.productores;

import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.ubicafe.app.R;
import com.ubicafe.app.datos.RepositorioDatos;
import com.ubicafe.app.modelo.Productor;

/**
 * DETALLE DE UN PRODUCTOR (finca cafetalera) (P9b).
 * Muestra la descripción de la finca y sus datos de registro.
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

        ((TextView) findViewById(R.id.texto_titulo)).setText(productor.nombreFinca);
        ((TextView) findViewById(R.id.texto_subtitulo)).setText(productor.nombreFamilia);
        ((TextView) findViewById(R.id.texto_descripcion)).setText(productor.descripcion);

        LinearLayout contenedor = findViewById(R.id.contenedor_ficha);
        agregarCampo(contenedor, getString(R.string.productor_campo_origen), productor.origen);
        agregarCampo(contenedor, getString(R.string.productor_campo_altitud),
                productor.altitud + " m s. n. m.");
        agregarCampo(contenedor, getString(R.string.productor_campo_familia),
                productor.nombreFamilia);
        agregarCampo(contenedor, getString(R.string.productor_campo_cafes),
                getResources().getQuantityString(
                        R.plurals.unidad_cafes, productor.numCafes, productor.numCafes));
    }

    private void agregarCampo(LinearLayout contenedor, String etiqueta, String valor) {
        ViewGroup fila = (ViewGroup) getLayoutInflater()
                .inflate(R.layout.item_par_info, contenedor, false);
        ((TextView) fila.findViewById(R.id.texto_etiqueta)).setText(etiqueta);
        ((TextView) fila.findViewById(R.id.texto_valor)).setText(valor);
        contenedor.addView(fila);
    }
}