package com.ubicafe.app.ui.ecosistema;

import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.ubicafe.app.R;
import com.ubicafe.app.datos.RepositorioDatos;
import com.ubicafe.app.modelo.Cafeteria;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Pestaña ECOSISTEMA.
 * Muestra el resumen del café en La Paz: cifras globales
 * y distribución de cafeterías por macrodistrito.
 */
public class EcosistemaFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflador, @Nullable ViewGroup contenedor,
                             @Nullable Bundle estado) {
        return inflador.inflate(R.layout.fragment_ecosistema, contenedor, false);
    }

    @Override
    public void onViewCreated(@NonNull View vista, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(vista, savedInstanceState);

        // 1. Cifras globales (las 4 tarjetas)
        llenarCifra(vista, R.id.cifra_lugares, RepositorioDatos.contarLugares(),
                getString(R.string.ecosistema_lugares), R.color.verde_oscuro);
        llenarCifra(vista, R.id.cifra_marcas, RepositorioDatos.contarMarcas(),
                getString(R.string.ecosistema_marcas), R.color.cafe_accent);
        llenarCifra(vista, R.id.cifra_tostaderias, RepositorioDatos.contarTostaderias(),
                getString(R.string.ecosistema_tostaderias), R.color.verde_claro);
        llenarCifra(vista, R.id.cifra_productores, RepositorioDatos.contarProductores(),
                getString(R.string.ecosistema_productores), R.color.caramelo_claro);

        // 2. Distribución por macrodistrito
        llenarMacrodistritos(vista);
    }

    /** Rellena una de las tarjetas de cifra con número, etiqueta y su raya de color. */
    private void llenarCifra(View vista, int idTarjeta, int numero, String etiqueta, int colorRes) {
        View tarjeta = vista.findViewById(idTarjeta);
        TextView numeroTexto = tarjeta.findViewById(R.id.texto_numero);
        TextView etiquetaTexto = tarjeta.findViewById(R.id.texto_etiqueta);
        View barraColor = tarjeta.findViewById(R.id.barra_color);

        numeroTexto.setText(String.valueOf(numero));
        etiquetaTexto.setText(etiqueta);

        // Barra de color decorativa
        GradientDrawable raya = new GradientDrawable();
        raya.setShape(GradientDrawable.RECTANGLE);
        raya.setCornerRadius(20f);
        raya.setColor(getResources().getColor(colorRes, requireContext().getTheme()));
        barraColor.setBackground(raya);
    }

    /** Agrupa las cafeterías por zona y dibuja una barra de proporción por cada una. */
    private void llenarMacrodistritos(View vista) {
        Map<String, Integer> conteoPorZona = new LinkedHashMap<>();
        for (Cafeteria cafeteria : RepositorioDatos.obtenerCafeterias()) {
            String zona = cafeteria.zona;
            conteoPorZona.put(zona, conteoPorZona.getOrDefault(zona, 0) + 1);
        }

        // La barra más larga sirve de referencia para las demás.
        int maximo = 1;
        for (int n : conteoPorZona.values()) {
            maximo = Math.max(maximo, n);
        }

        LinearLayout contenedor = vista.findViewById(R.id.contenedor_macrodistritos);
        contenedor.removeAllViews();

        LayoutInflater inflador = getLayoutInflater();
        for (Map.Entry<String, Integer> entrada : conteoPorZona.entrySet()) {
            View fila = inflador.inflate(R.layout.item_macrodistrito, contenedor, false);
            ((TextView) fila.findViewById(R.id.texto_nombre)).setText(entrada.getKey());
            ((TextView) fila.findViewById(R.id.texto_conteo))
                    .setText(entrada.getKey() + " · " + entrada.getValue() + " cafés");

            ProgressBar barra = fila.findViewById(R.id.barra_progreso);
            barra.setProgress(Math.round(entrada.getValue() * 100f / maximo));

            contenedor.addView(fila);
        }
    }
}