package com.ubicafe.app.ui.comun;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.ubicafe.app.R;
import com.ubicafe.app.databinding.ItemDatoBinding;
import com.ubicafe.app.util.UiUtils;
import com.ubicafe.app.modelo.ParDato;

/**
 * RELLENA LA FICHA TÉCNICA
 * ---------------------------------------------------------------
 * Los datos del censo vienen como pares de "etiqueta, valor" porque cada
 * sección respondió cosas distintas. Esta clase los convierte en filas y
 * descarta los vacíos: de los 213 lugares, 138 no respondieron su
 * sección de detalle, y una ficha llena de guiones parece un error de la
 * app en vez de un dato que no se registró.
 */
public final class FichaTecnica {

    private FichaTecnica() {
        // Clase de utilidades: no se instancia.
    }

    /**
     * Agrega los pares con valor al contenedor. Si no queda ninguno,
     * el contenedor queda vacío y la pantalla muestra un mensaje
     * explicativo en vez de un bloque en blanco.
     *
     * @return cuántos pares se pintaron
     */
    public static int agregar(LinearLayout contenedor, Iterable<ParDato> pares) {
        Context contexto = contenedor.getContext();
        LayoutInflater inflador = LayoutInflater.from(contexto);
        int agregados = 0;

        for (ParDato par : pares) {
            if (par == null || par.valor == null || par.valor.trim().isEmpty()) {
                continue;
            }
            View fila = inflador.inflate(R.layout.item_dato, contenedor, false);
            ((TextView) fila.findViewById(R.id.texto_etiqueta)).setText(par.etiqueta);
            ((TextView) fila.findViewById(R.id.texto_valor)).setText(par.valor);
            contenedor.addView(fila);
            agregados++;
        }
        return agregados;
    }

    /**
     * Muestra un aviso de que esa sección no se respondió. Se usa en
     * lugar de esconder el bloque: si no, el usuario no puede
     * distinguir "no respondió" de "esto no existe".
     */
    public static void mostrarAviso(LinearLayout contenedor, String mensaje) {
        Context contexto = contenedor.getContext();
        TextView aviso = new TextView(contexto);
        aviso.setText(mensaje);
        aviso.setTextSize(14);
        aviso.setTextColor(ContextCompat.getColor(contexto, R.color.texto_tenue));
        aviso.setPadding(0, UiUtils.dp(contenedor, 8), 0, 0);
        contenedor.addView(aviso);
    }

    /**
     * Rellena una fila de la ficha técnica. Si no hay valor, la oculta
     * entera: una fila con guion parece un dato vacío, y aquí lo que
     * falta es una respuesta de la encuesta.
     */
    public static void pintar(ItemDatoBinding fila, String etiqueta, String valor) {
        if (valor == null || valor.trim().isEmpty()) {
            fila.getRoot().setVisibility(View.GONE);
            return;
        }
        fila.getRoot().setVisibility(View.VISIBLE);
        fila.textoEtiqueta.setText(etiqueta);
        fila.textoValor.setText(valor);
    }

    /**
     * El encabezado de una sección de la ficha ("Como tostaduría"),
     * con el mismo formato en todas las pantallas.
     */
    public static TextView encabezado(Context contexto, int titulo) {
        TextView encabezado = new TextView(contexto);
        encabezado.setText(titulo);
        encabezado.setTextColor(ContextCompat.getColor(contexto, R.color.verde_oscuro));
        encabezado.setTypeface(android.graphics.Typeface.create(
                "sans-serif", android.graphics.Typeface.BOLD));
        int margen = Math.round(18 * contexto.getResources().getDisplayMetrics().density);
        encabezado.setPadding(0, margen, 0, 4);
        return encabezado;
    }

    /** Oculta un bloque entero de la ficha. */
    public static void ocultar(View bloque) {
        bloque.setVisibility(View.GONE);
    }

    /** Igual, para una fila de la ficha técnica. */
    public static void ocultar(ItemDatoBinding fila) {
        fila.getRoot().setVisibility(View.GONE);
    }
}
