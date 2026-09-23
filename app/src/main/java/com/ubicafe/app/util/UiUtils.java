package com.ubicafe.app.util;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.ubicafe.app.R;

/**
 * Utilidades de interfaz compartidas por varias pantallas.
 * Aquí solo caben funciones generales y reutilizables (nada de lógica de negocio).
 */
public class UiUtils {

    private UiUtils() {
        // Prevent instanciación: es una clase de utilidades.
    }

    /**
     * Crea un "chip" (filtro pill de 34dp) dentro de una fila horizontal.
     * En reposo es blanco con texto principal; al seleccionarse queda verde.
     */
    public static TextView crearChip(ViewGroup fila, String texto) {
        TextView chip = new TextView(fila.getContext());
        chip.setText(texto);
        chip.setTextSize(13);
        chip.setTypeface(android.graphics.Typeface.create(
                "sans-serif-medium", android.graphics.Typeface.NORMAL));
        chip.setAllCaps(false);
        chip.setTextColor(fila.getContext().getColor(R.color.texto_principal));
        chip.setBackground(ContextCompat.getDrawable(
                fila.getContext(), R.drawable.fondo_chip_no_seleccionado));
        chip.setPadding(
                dp(fila, 12), 0, dp(fila, 12), 0);
        chip.setGravity(android.view.Gravity.CENTER);

        ViewGroup.LayoutParams parametros = new ViewGroup.MarginLayoutParams(
                dp(fila, 0), dp(fila, 34));
        ((ViewGroup.MarginLayoutParams) parametros).width =
                ViewGroup.LayoutParams.WRAP_CONTENT;
        ((ViewGroup.MarginLayoutParams) parametros).rightMargin = dp(fila, 8);
        chip.setLayoutParams(parametros);

        fila.addView(chip);
        return chip;
    }

    /** Cambia el aspecto del chip: seleccionado (relleno verde) o reposo (blanco). */
    public static void marcarChipSeleccionado(TextView chip, boolean seleccionado) {
        if (seleccionado) {
            chip.setBackground(ContextCompat.getDrawable(
                    chip.getContext(), R.drawable.fondo_chip_seleccionado));
            chip.setTextColor(chip.getContext().getColor(R.color.texto_sobre_verde));
        } else {
            chip.setBackground(ContextCompat.getDrawable(
                    chip.getContext(), R.drawable.fondo_chip_no_seleccionado));
            chip.setTextColor(chip.getContext().getColor(R.color.texto_principal));
        }
    }

    /** Convierte un valor dp a píxeles (para los tamaños en código). */
    public static int dp(View vista, int valorDp) {
        return Math.round(valorDp * vista.getResources().getDisplayMetrics().density);
    }
}