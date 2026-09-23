package com.ubicafe.app.ui.puntosventa;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.ubicafe.app.R;
import com.ubicafe.app.datos.RepositorioDatos;
import com.ubicafe.app.modelo.MarcaCafe;
import com.ubicafe.app.modelo.PuntoVenta;
import com.ubicafe.app.ui.mapa.MapaActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * PUNTOS DE VENTA (P12).
 * "Marcas censadas" se puede filtrar con el buscador; al elegir una marca
 * se resalta y se muestra el detalle con sus puntos de venta.
 */
public class PuntosVentaActivity extends AppCompatActivity {

    /** Marcas que tienen al menos un punto de venta (registradas para el censo). */
    private final List<MarcaCafe> marcas = new ArrayList<>();
    private MarcaCafe seleccionada;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_puntos_venta);

        ((TextView) findViewById(R.id.texto_titulo)).setText(getString(R.string.puntos_titulo));
        ((TextView) findViewById(R.id.texto_subtitulo))
                .setText(getString(R.string.puntos_subtitulo));

        findViewById(R.id.btn_volver).setOnClickListener(v -> finish());
        findViewById(R.id.btn_ver_mapa)
                .setOnClickListener(v -> startActivity(new Intent(this, MapaActivity.class)));

        // Solo entran las marcas con puntos de venta en el censo.
        for (MarcaCafe marca : RepositorioDatos.obtenerMarcas()) {
            if (RepositorioDatos.contarPuntosDeVentaDe(marca.nombre) > 0) {
                marcas.add(marca);
            }
        }
        if (!marcas.isEmpty()) {
            seleccionada = marcas.get(0);
        }

        rozarLista();

        EditText campo = findViewById(R.id.campo_busqueda);
        campo.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable s) { }

            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {
                rozarLista();
            }
        });
    }

    /** Reconstruye la lista de marcas según el texto del buscador. */
    private void rozarLista() {
        EditText campo = findViewById(R.id.campo_busqueda);
        String texto = campo.getText().toString().trim().toLowerCase(Locale.getDefault());

        LinearLayout contenedor = findViewById(R.id.lista_marcas);
        contenedor.removeAllViews();

        if (seleccionada != null && !seleccionada.nombre.toLowerCase(Locale.getDefault())
                .contains(texto)
                && !texto.isEmpty()) {
            // La selección dejó de ser visible con el filtro: no resaltar nada.
            pintarDetalle(null);
        }

        for (MarcaCafe marca : marcas) {
            if (!marca.nombre.toLowerCase(Locale.getDefault()).contains(texto)) {
                continue;
            }
            contenedor.addView(crearFilaMarca(marca, contenedor));
        }

        if (seleccionada != null) {
            pintarDetalle(seleccionada);
        }
    }

    /** Crea una fila de "Marcas censadas" con su selección marcada. */
    private View crearFilaMarca(MarcaCafe marca, LinearLayout contenedor) {
        View fila = LayoutInflater.from(this)
                .inflate(R.layout.item_marca_resumen, contenedor, false);

        ((TextView) fila.findViewById(R.id.texto_nombre)).setText(marca.nombre);
        ((TextView) fila.findViewById(R.id.texto_conteo))
                .setText(getResources().getQuantityString(R.plurals.unidad_puntos_venta,
                        RepositorioDatos.contarPuntosDeVentaDe(marca.nombre),
                        RepositorioDatos.contarPuntosDeVentaDe(marca.nombre)));

        boolean estáSeleccionada = seleccionada != null
                && seleccionada.nombre.equalsIgnoreCase(marca.nombre);
        pintarFilaSeleccion(fila, estáSeleccionada);

        fila.setOnClickListener(v -> {
            seleccionada = marca;
            rozarLista();
        });
        return fila;
    }

    private void pintarFilaSeleccion(View fila, boolean seleccionada) {
        fila.setBackground(ContextCompat.getDrawable(this,
                seleccionada ? R.drawable.fondo_fila_brand_seleccionado
                        : R.drawable.fondo_fila_brand_no_seleccionado));
        TextView nombre = fila.findViewById(R.id.texto_nombre);
        TextView conteo = fila.findViewById(R.id.texto_conteo);
        int colorTexto = getColor(seleccionada ? R.color.texto_sobre_verde : R.color.texto_principal);
        int colorConteo = getColor(seleccionada ? R.color.fondo_crema : R.color.texto_secundario);
        nombre.setTextColor(colorTexto);
        conteo.setTextColor(colorConteo);
    }

    /** Pinta el detalle de la marca elegida con sus puntos de venta. */
    private void pintarDetalle(MarcaCafe marca) {
        TextView textoNombre = findViewById(R.id.texto_detalle_nombre);
        TextView textoDestacado = findViewById(R.id.texto_destacado);
        LinearLayout contenedor = findViewById(R.id.contenedor_puntos);

        if (marca == null) {
            textoNombre.setText("");
            textoDestacado.setVisibility(View.GONE);
            contenedor.removeAllViews();
            return;
        }

        textoNombre.setText(marca.nombre);
        textoDestacado.setVisibility(marca.esNacional ? View.VISIBLE : View.GONE);

        contenedor.removeAllViews();
        for (PuntoVenta punto : RepositorioDatos.obtenerPuntosDeVentaDe(marca.nombre)) {
            contenedor.addView(crearFilaPunto(punto));
        }
    }

    /** Fila "Punto verde + Local / Barrio" dentro del detalle. */
    private View crearFilaPunto(PuntoVenta punto) {
        LinearLayout fila = new LinearLayout(this);
        fila.setOrientation(LinearLayout.HORIZONTAL);
        fila.setGravity(android.view.Gravity.CENTER_VERTICAL);

        LinearLayout.LayoutParams anchoPunto = new LinearLayout.LayoutParams(dp(8), dp(8));
        View viñeta = new View(this);
        viñeta.setBackgroundResource(R.drawable.fondo_punto_verde);
        fila.addView(viñeta, anchoPunto);

        TextView texto = new TextView(this);
        texto.setText(punto.local + " / " + punto.barrio);
        texto.setTextSize(14);
        texto.setTypeface(android.graphics.Typeface.create(
                "sans-serif", android.graphics.Typeface.NORMAL));
        texto.setTextColor(getColor(R.color.texto_principal));
        LinearLayout.LayoutParams parametros =
                new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
        parametros.leftMargin = dp(10);
        parametros.topMargin = dp(6);
        parametros.bottomMargin = dp(6);
        fila.addView(texto, parametros);
        return fila;
    }

    private int dp(int valor) {
        return Math.round(valor * getResources().getDisplayMetrics().density);
    }
}