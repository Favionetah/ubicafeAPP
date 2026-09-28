package com.ubicafe.app.ui.origen;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.ubicafe.app.R;
import com.ubicafe.app.datos.RepositorioDatos;
import com.ubicafe.app.databinding.ActivityFichaOrigenBinding;
import com.ubicafe.app.modelo.CafeVariedad;
import com.ubicafe.app.util.UiUtils;

import java.util.List;

/**
 * FICHA DE UN CAFÉ DE ORIGEN (P9a)
 * ---------------------------------------------------------------
 * El censo tiene 66 fichas de café de origen: origen, variedad, proceso
 * y notas de cata. No tiene altitud ni precio, así que esta pantalla no
 * los pide: una altitud sin dato real sería un número inventado en la
 * posición más visible de la ficha.
 */
public class FichaOrigenActivity extends AppCompatActivity {

    public static final String EXTRA_VARIEDAD = "variedad";
    public static final String EXTRA_NOMBRE_MARCA = "nombre_marca";

    private ActivityFichaOrigenBinding ui;

    @Override
    protected void onCreate(@Nullable Bundle estado) {
        super.onCreate(estado);
        ui = ActivityFichaOrigenBinding.inflate(getLayoutInflater());
        setContentView(ui.getRoot());

        ui.barra.btnVolver.setOnClickListener(v -> finish());

        String variedad = getIntent().getStringExtra(EXTRA_VARIEDAD);
        String nombreMarca = getIntent().getStringExtra(EXTRA_NOMBRE_MARCA);
        CafeVariedad cafe = variedad == null
                ? null
                : RepositorioDatos.obtenerVariedadPorNombre(variedad);

        if (cafe == null) {
            finish();
            return;
        }

        ui.barra.textoTitulo.setText(cafe.nombre);
        if (nombreMarca != null && !nombreMarca.isEmpty()) {
            ui.barra.textoSubtitulo.setText(getString(R.string.ficha_marca_formato, nombreMarca));
        } else if (!cafe.marca.isEmpty()) {
            ui.barra.textoSubtitulo.setText(getString(R.string.ficha_marca_formato, cafe.marca));
        } else {
            ui.barra.textoSubtitulo.setText(R.string.ficha_origen_titulo);
        }

        ui.textoInicialHero.setText(String.valueOf(cafe.inicial()));

        agregarCampo(ui.contenedorFicha, getString(R.string.ficha_campo_origen), cafe.region);
        agregarCampo(ui.contenedorFicha, getString(R.string.ficha_campo_variedad),
                cafe.variedadesDeclaradas);
        agregarCampo(ui.contenedorFicha, getString(R.string.ficha_campo_contexto), cafe.contexto);

        llenarVariedades(cafe.variedadesComoLista());
    }

    /** Agrega una fila "etiqueta: valor", o la salta si no hay valor. */
    private void agregarCampo(LinearLayout contenedor, String etiqueta, String valor) {
        if (valor == null || valor.trim().isEmpty()) {
            return;
        }
        ViewGroup fila = (ViewGroup) getLayoutInflater()
                .inflate(R.layout.item_par_info, contenedor, false);
        ((TextView) fila.findViewById(R.id.texto_etiqueta)).setText(etiqueta);
        ((TextView) fila.findViewById(R.id.texto_valor)).setText(valor);
        contenedor.addView(fila);
    }

    /**
     * Las variedades declaradas, como chips. Si el censo puso una sola,
     * sale un solo chip: no hace falta inventar una lista.
     */
    private void llenarVariedades(List<String> variedades) {
        if (variedades.isEmpty()) {
            ui.filaVariedades.setVisibility(View.GONE);
            ui.tituloVariedades.setVisibility(View.GONE);
            return;
        }
        for (String variedad : variedades) {
            TextView chip = new TextView(this);
            chip.setText(variedad);
            chip.setTextSize(12);
            chip.setTypeface(android.graphics.Typeface.create(
                    "sans-serif-medium", android.graphics.Typeface.NORMAL));
            chip.setTextColor(getColor(R.color.verde_oscuro));
            chip.setBackgroundResource(R.drawable.fondo_badge_zona);
            chip.setPadding(UiUtils.dp(ui.filaVariedades, 10),
                    UiUtils.dp(ui.filaVariedades, 4),
                    UiUtils.dp(ui.filaVariedades, 10),
                    UiUtils.dp(ui.filaVariedades, 4));
            chip.setGravity(Gravity.CENTER);

            LinearLayout.LayoutParams parametros =
                    new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT);
            parametros.rightMargin = UiUtils.dp(ui.filaVariedades, 8);
            ui.filaVariedades.addView(chip, parametros);
        }
    }
}
