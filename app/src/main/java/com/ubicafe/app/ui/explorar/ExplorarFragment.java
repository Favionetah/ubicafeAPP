package com.ubicafe.app.ui.explorar;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.ubicafe.app.R;
import com.ubicafe.app.datos.RepositorioDatos;
import com.ubicafe.app.modelo.CafeVariedad;
import com.ubicafe.app.ui.origen.FichaOrigenActivity;

import java.util.List;
import java.util.Map;

/**
 * PESTAÑA DE EXPLORAR (P5)
 * ---------------------------------------------------------------
 * Tres bloques, todos sacados del censo:
 *   1. Las 66 variedades y cafés de origen, agrupados por región.
 *   2. Las regiones cafetaleras con el número de variedades que el
 *      censo les atribuye.
 *   3. Una guía breve de qué es el café de especialidad.
 *
 * No hay altitudes ni puntajes: el censo no los preguntó, y esta
 * pantalla es la que más tempted de inventarlos está, porque "cafetal
 * de altura" sin cifra parece pobre.
 */
public class ExplorarFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflador, @Nullable ViewGroup contenedor,
                             @Nullable Bundle estado) {
        return inflador.inflate(R.layout.fragment_explorar, contenedor, false);
    }

    @Override
    public void onViewCreated(@NonNull View vista, @Nullable Bundle estado) {
        super.onViewCreated(vista, estado);

        List<CafeVariedad> variedades = RepositorioDatos.obtenerVariedades();
        pintarVariedades(vista, variedades);
        pintarRegiones(vista);
        pintarGuia(vista);
    }

    /**
     * Una tarjeta por variedad. Muestra la región y la marca solo si el
     * censo las registró: 36 de las 66 no tienen región, y escribirlas
     * de todas sería inventar el dato más visible de la tarjeta.
     */
    private void pintarVariedades(View vista, List<CafeVariedad> variedades) {
        LinearLayout contenedor = vista.findViewById(R.id.contenedor_variedades);
        LayoutInflater inflador = LayoutInflater.from(contenedor.getContext());

        for (CafeVariedad variedad : variedades) {
            View tarjeta = inflador.inflate(R.layout.item_cafe_origen, contenedor, false);

            ((TextView) tarjeta.findViewById(R.id.texto_variedad)).setText(variedad.nombre);

            StringBuilder pie = new StringBuilder();
            if (!variedad.region.isEmpty()) {
                pie.append(variedad.region);
            }
            if (!variedad.marca.isEmpty()) {
                if (pie.length() > 0) {
                    pie.append(" · ");
                }
                pie.append(variedad.marca);
            }
            TextView origen = tarjeta.findViewById(R.id.texto_origen);
            if (pie.length() == 0) {
                origen.setVisibility(View.GONE);
            } else {
                origen.setText(pie);
            }

            // El contexto explica por qué aparece: "Cafetería que compra
            // esta variedad", "Productor que cultiva". Valdrá más que una
            // nota de cata inventada.
            TextView notas = tarjeta.findViewById(R.id.texto_notas);
            if (variedad.contexto.isEmpty()) {
                notas.setVisibility(View.GONE);
            } else {
                notas.setText(variedad.contexto);
            }

            tarjeta.setOnClickListener(v -> abrir(variedad));
            contenedor.addView(tarjeta);
        }
    }

    private void abrir(CafeVariedad variedad) {
        Intent intento = new Intent(requireContext(), FichaOrigenActivity.class);
        intento.putExtra(FichaOrigenActivity.EXTRA_VARIEDAD, variedad.nombre);
        if (!variedad.marca.isEmpty()) {
            intento.putExtra(FichaOrigenActivity.EXTRA_NOMBRE_MARCA, variedad.marca);
        }
        startActivity(intento);
    }

    /**
     * Las regiones con su conteo real. El filtro de una sola variedad
     * ya está en el repositorio, que es quien sabe qué grafías del
     * censo son la misma región.
     */
    private void pintarRegiones(View vista) {
        LinearLayout contenedor = vista.findViewById(R.id.contenedor_regiones);
        LayoutInflater inflador = LayoutInflater.from(contenedor.getContext());

        for (Map.Entry<String, Integer> region
                : RepositorioDatos.obtenerRegionesCafetaleras().entrySet()) {
            View fila = inflador.inflate(R.layout.item_region, contenedor, false);
            ((TextView) fila.findViewById(R.id.texto_titulo)).setText(region.getKey());
            int veces = region.getValue();
            ((TextView) fila.findViewById(R.id.texto_descripcion)).setText(
                    getResources().getQuantityString(R.plurals.explorar_region_conteo,
                            veces, veces));
            contenedor.addView(fila);
        }
    }

    /** Guía del café: tres tarjetas informativas estáticas. */
    private void pintarGuia(View vista) {
        LinearLayout contenedor = vista.findViewById(R.id.contenedor_guia);
        LayoutInflater inflador = LayoutInflater.from(contenedor.getContext());
        int[] titulos = {
                R.string.explorar_guia_card1_titulo,
                R.string.explorar_guia_card2_titulo,
                R.string.explorar_guia_card3_titulo
        };
        int[] textos = {
                R.string.explorar_guia_card1_txt,
                R.string.explorar_guia_card2_txt,
                R.string.explorar_guia_card3_txt
        };

        for (int i = 0; i < titulos.length; i++) {
            View tarjeta = inflador.inflate(R.layout.item_card_info, contenedor, false);
            ((TextView) tarjeta.findViewById(R.id.texto_titulo)).setText(titulos[i]);
            ((TextView) tarjeta.findViewById(R.id.texto_descripcion)).setText(textos[i]);
            contenedor.addView(tarjeta);
        }
    }
}
