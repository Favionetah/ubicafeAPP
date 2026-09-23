package com.ubicafe.app.ui.inicio;

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
import com.ubicafe.app.modelo.Cafeteria;
import com.ubicafe.app.ui.busqueda.BusquedaActivity;
import com.ubicafe.app.ui.cafeterias.DetalleCafeteriaActivity;
import com.ubicafe.app.ui.cafeterias.ListaCafeteriasActivity;
import com.ubicafe.app.ui.marcas.ListaMarcasActivity;
import com.ubicafe.app.ui.productores.ListaProductoresActivity;
import com.ubicafe.app.ui.puntosventa.PuntosVentaActivity;
import com.ubicafe.app.ui.tostaderias.ListaTostaderiasActivity;
import com.ubicafe.app.util.UiUtils;

/**
 * Pestaña de INICIO (P3).
 * - Grid "Explora por categoría" (2x2).
 * - Tarjeta destacada "Cerca de ti".
 * - Chips de macrodistritos.
 * - Banner final de productores.
 */
public class InicioFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflador, @Nullable ViewGroup contenedor,
                             @Nullable Bundle estado) {
        return inflador.inflate(R.layout.fragment_inicio, contenedor, false);
    }

    @Override
    public void onViewCreated(@NonNull View vista, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(vista, savedInstanceState);

        prepararCategorias(vista);
        prepararBusqueda(vista);
        prepararCercaDeTi(vista);
        prepararMacrodistritos(vista);
        prepararBannerProductores(vista);
    }

    /** Tiles 2x2: cada categoría abre su listado. */
    private void prepararCategorias(View vista) {
        vista.findViewById(R.id.tile_cafeterias)
                .setOnClickListener(v -> abrir(ListaCafeteriasActivity.class));
        vista.findViewById(R.id.tile_marcas)
                .setOnClickListener(v -> abrir(ListaMarcasActivity.class));
        vista.findViewById(R.id.tile_tostaderias)
                .setOnClickListener(v -> abrir(ListaTostaderiasActivity.class));
        vista.findViewById(R.id.tile_puntos_venta)
                .setOnClickListener(v -> abrir(PuntosVentaActivity.class));
    }

    /** El buscador del Home abre la búsqueda global (P10). */
    private void prepararBusqueda(View vista) {
        vista.findViewById(R.id.buscador_inicio)
                .setOnClickListener(v -> abrir(BusquedaActivity.class));
    }

    /** Tarjeta "Cerca de ti": muestra la primera cafetería y abre su detalle. */
    private void prepararCercaDeTi(View vista) {
        Cafeteria destacada = obtenerPrimera();
        if (destacada == null) {
            vista.findViewById(R.id.card_cerca_de_ti).setVisibility(View.GONE);
            return;
        }

        ((TextView) vista.findViewById(R.id.texto_nombre_cerca)).setText(destacada.nombre);
        ((TextView) vista.findViewById(R.id.texto_tipo_cerca)).setText(destacada.tipo);
        ((TextView) vista.findViewById(R.id.texto_zona_cerca)).setText(destacada.zona);
        ((TextView) vista.findViewById(R.id.texto_distancia))
                .setText(getString(R.string.inicio_distancia, "450 m"));
        ((TextView) vista.findViewById(R.id.texto_rating))
                .setText(getString(R.string.inicio_rating, "4.9", "240 opiniones"));

        View.OnClickListener abrir = v -> {
            Intent intento = new Intent(requireContext(), DetalleCafeteriaActivity.class);
            intento.putExtra(DetalleCafeteriaActivity.EXTRA_NOMBRE_CAFETERIA, destacada.nombre);
            startActivity(intento);
        };
        vista.findViewById(R.id.card_cerca_de_ti).setOnClickListener(abrir);
        vista.findViewById(R.id.btn_ver_informacion).setOnClickListener(abrir);
    }

    /** Chips de macrodistrito: al tocar, abre el listado de cafeterías. */
    private void prepararMacrodistritos(View vista) {
        String[] distritos = {"Sur", "Centro", "Cotahuma", "Mallasa", "Sopocachi", "San Antonio"};
        LinearLayout fila = vista.findViewById(R.id.fila_macrodistritos);

        for (int i = 0; i < distritos.length; i++) {
            TextView chip = UiUtils.crearChip(fila, distritos[i]);
            UiUtils.marcarChipSeleccionado(chip, i == 0);
            chip.setOnClickListener(v -> abrir(ListaCafeteriasActivity.class));
        }
    }

    /** Banner final de productores. */
    private void prepararBannerProductores(View vista) {
        vista.findViewById(R.id.banner_productores)
                .setOnClickListener(v -> abrir(ListaProductoresActivity.class));
    }

    /** Primera cafetería registrada, para la tarjeta "Cerca de ti". */
    private Cafeteria obtenerPrimera() {
        return RepositorioDatos.obtenerCafeterias().isEmpty()
                ? null : RepositorioDatos.obtenerCafeterias().get(0);
    }

    private void abrir(Class<?> actividad) {
        startActivity(new Intent(requireContext(), actividad));
    }
}