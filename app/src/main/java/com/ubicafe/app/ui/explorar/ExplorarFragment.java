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
import com.ubicafe.app.modelo.CafeOrigen;
import com.ubicafe.app.modelo.Cafeteria;
import com.ubicafe.app.ui.origen.FichaOrigenActivity;

/**
 * Pestaña de EXPLORAR (P5).
 * Muestra variedades paceñas, regiones cafeteras y una guía del café.
 * Sin métricas ni cifras (según alcance aprobado).
 */
public class ExplorarFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflador, @Nullable ViewGroup contenedor,
                             @Nullable Bundle estado) {
        return inflador.inflate(R.layout.fragment_explorar, contenedor, false);
    }

    @Override
    public void onViewCreated(@NonNull View vista, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(vista, savedInstanceState);

        rellenarVariedades(vista);
        rellenarRegiones(vista);
        rellenarGuia(vista);
    }

    /** Variedades paceñas: una fila por cada café de origen → Ficha de Origen. */
    private void rellenarVariedades(View vista) {
        LinearLayout contenedor = vista.findViewById(R.id.contenedor_variedades);
        for (CafeOrigen cafe : RepositorioDatos.obtenerCafesDeOrigen()) {
            View tarjeta = getLayoutInflater()
                    .inflate(R.layout.item_cafe_origen, contenedor, false);

            ((TextView) tarjeta.findViewById(R.id.texto_variedad)).setText(cafe.variedad);
            ((TextView) tarjeta.findViewById(R.id.texto_origen)).setText(
                    nombreMarcaDe(cafe.variedad) + " · " + cafe.origen);
            ((TextView) tarjeta.findViewById(R.id.texto_notas)).setText(cafe.aroma);

            tarjeta.setOnClickListener(v -> {
                Intent intento = new Intent(requireContext(), FichaOrigenActivity.class);
                intento.putExtra(FichaOrigenActivity.EXTRA_VARIEDAD, cafe.variedad);
                intento.putExtra(FichaOrigenActivity.EXTRA_NOMBRE_MARCA,
                        nombreMarcaDe(cafe.variedad));
                startActivity(intento);
            });
            contenedor.addView(tarjeta);
        }
    }

    /** Regiones cafeteras: filas estáticas de las zonas de cultivo. */
    private void rellenarRegiones(View vista) {
        LinearLayout contenedor = vista.findViewById(R.id.contenedor_regiones);
        String[][] regiones = {
                {"Caranavi", "Cafetales de altura entre 1.400 y 1.800 m s. n. m."},
                {"Yungas", "Café de sombra bajo bosque nativo entre 1.200 y 1.500 m."},
                {"La Paz", "La capital reúne tostadores y cafeterías de especialidad."}
        };
        for (String[] region : regiones) {
            View fila = getLayoutInflater().inflate(R.layout.item_region, contenedor, false);
            ((TextView) fila.findViewById(R.id.texto_titulo)).setText(region[0]);
            ((TextView) fila.findViewById(R.id.texto_descripcion)).setText(region[1]);
            contenedor.addView(fila);
        }
    }

    /** Guía del café: tres tarjetas informativas estáticas. */
    private void rellenarGuia(View vista) {
        LinearLayout contenedor = vista.findViewById(R.id.contenedor_guia);
        String[] titulos = {
                getString(R.string.explorar_guia_card1_titulo),
                getString(R.string.explorar_guia_card2_titulo),
                getString(R.string.explorar_guia_card3_titulo)
        };
        String[] descripciones = {
                getString(R.string.explorar_guia_card1_txt),
                getString(R.string.explorar_guia_card2_txt),
                getString(R.string.explorar_guia_card3_txt)
        };
        for (int i = 0; i < titulos.length; i++) {
            View tarjeta = getLayoutInflater().inflate(R.layout.item_card_info, contenedor, false);
            ((TextView) tarjeta.findViewById(R.id.texto_titulo)).setText(titulos[i]);
            ((TextView) tarjeta.findViewById(R.id.texto_descripcion)).setText(descripciones[i]);
            contenedor.addView(tarjeta);
        }
    }

    /** Nombre de la marca que ofrece una variedad (para "TYPICA · Caranavi"). */
    private String nombreMarcaDe(String variedad) {
        for (Cafeteria cafeteria : RepositorioDatos.obtenerCafeterias()) {
            if (cafeteria.marca == null) {
                continue;
            }
            for (CafeOrigen cafe : cafeteria.fichaOrigen) {
                if (cafeteria.marca != null
                        && cafe.variedad.equalsIgnoreCase(variedad)) {
                    return cafeteria.marca;
                }
            }
        }
        return getString(R.string.categoria_cafeterias);
    }
}