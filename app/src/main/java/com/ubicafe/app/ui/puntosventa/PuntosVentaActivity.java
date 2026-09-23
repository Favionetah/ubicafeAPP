package com.ubicafe.app.ui.puntosventa;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ubicafe.app.R;
import com.ubicafe.app.datos.RepositorioDatos;
import com.ubicafe.app.modelo.MarcaCafe;
import com.ubicafe.app.modelo.PuntoVenta;

import java.util.List;

/**
 * PUNTOS DE VENTA.
 * Primero muestra un resumen por marca ("Marcas censadas")
 * y luego la lista completa de todos los puntos de venta.
 */
public class PuntosVentaActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_puntos_venta);

        ((TextView) findViewById(R.id.texto_titulo)).setText(getString(R.string.puntos_titulo));

        findViewById(R.id.btn_volver).setOnClickListener(v -> finish());

        llenarMarcasCensadas();
        llenarListaPuntos();
    }

    /** Muestra cuántos puntos de venta tiene cada marca registrada. */
    private void llenarMarcasCensadas() {
        LinearLayout contenedor = findViewById(R.id.contenedor_marcas_censadas);

        for (MarcaCafe marca : RepositorioDatos.obtenerMarcas()) {
            int cantidad = RepositorioDatos.contarPuntosDeVentaDe(marca.nombre);
            if (cantidad == 0) {
                continue; // marcas sin puntos de venta no se muestran en el censo
            }

            View fila = LayoutInflater.from(this)
                    .inflate(R.layout.item_marca_resumen, contenedor, false);
            ((TextView) fila.findViewById(R.id.texto_nombre)).setText(marca.nombre);
            ((TextView) fila.findViewById(R.id.texto_conteo)).setText(
                    getResources().getQuantityString(R.plurals.unidad_puntos_venta, cantidad, cantidad));
            contenedor.addView(fila);
        }
    }

    private void llenarListaPuntos() {
        List<PuntoVenta> puntos = RepositorioDatos.obtenerPuntosDeVenta();
        RecyclerView lista = findViewById(R.id.lista);
        lista.setLayoutManager(new LinearLayoutManager(this));
        lista.setAdapter(new AdaptadorPuntoVenta(puntos));
    }
}