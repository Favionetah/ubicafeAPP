package com.ubicafe.app.ui.mapa;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;

import com.ubicafe.app.R;
import com.ubicafe.app.datos.RepositorioDatos;
import com.ubicafe.app.modelo.Cafeteria;
import com.ubicafe.app.ui.cafeterias.DetalleCafeteriaActivity;
import com.ubicafe.app.util.UiUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Pestaña de MAPA (P4).
 * Muestra el plano de La Paz con las cafeterías marcadas y un panel
 * inferior con los datos del marcador seleccionado.
 */
public class MapaFragment extends Fragment {

    private final List<MarcadorMapa> marcadores = new ArrayList<>();
    private final List<TextView> chips = new ArrayList<>();
    private String tipoSeleccionado = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflador, @Nullable ViewGroup contenedor,
                             @Nullable Bundle estado) {
        return inflador.inflate(R.layout.fragment_mapa, contenedor, false);
    }

    @Override
    public void onViewCreated(@NonNull View vista, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(vista, savedInstanceState);

        construirMarcadores(vista);
        crearFiltros(vista);

        MapaLaPazView vistaMapa = vista.findViewById(R.id.mapa_vista);
        vistaMapa.setMarcadores(marcadores);

        if (!marcadores.isEmpty()) {
            mostrarInformacion(vista, marcadores.get(0));
        }

        vistaMapa.setListenerAlSeleccionar(marcador -> mostrarInformacion(vista, marcador));

        vista.findViewById(R.id.btn_ver_informacion).setOnClickListener(
                v -> abrirDetalle(vista, tipoSeleccionadoExtra()));
    }

    private void construirMarcadores(View vista) {
        marcadores.clear();
        int color = getResources().getColor(R.color.verde_oscuro, requireContext().getTheme());
        for (Cafeteria cafeteria : RepositorioDatos.obtenerCafeterias()) {
            marcadores.add(new MarcadorMapa(cafeteria.nombre, cafeteria.zona,
                    cafeteria.lat, cafeteria.lng, color));
        }
    }

    private void crearFiltros(View vista) {
        int[] filtros = {R.string.mapa_filtro_todos, R.string.categoria_cafeterias,
                R.string.categoria_tostaderias, R.string.categoria_productores,
                R.string.categoria_puntos_venta};
        LinearLayout fila = vista.findViewById(R.id.fila_filtros);

        for (int i = 0; i < filtros.length; i++) {
            TextView chip = UiUtils.crearChip(fila, getString(filtros[i]));
            final int posicion = i;
            chip.setOnClickListener(v -> {
                tipoSeleccionado = posicion == 0 ? "" : getString(filtros[posicion]);
                refrescarChips();
            });
            chips.add(chip);
        }
        refrescarChips();
    }

    private void refrescarChips() {
        for (int i = 0; i < chips.size(); i++) {
            boolean seleccionado = (i == 0 && tipoSeleccionado.isEmpty())
                    || (i > 0 && tipoSeleccionado.equals(getString(obtenerFiltro(i))));
            UiUtils.marcarChipSeleccionado(chips.get(i), seleccionado);
        }
    }

    private int obtenerFiltro(int i) {
        switch (i) {
            case 1: return R.string.categoria_cafeterias;
            case 2: return R.string.categoria_tostaderias;
            case 3: return R.string.categoria_productores;
            default: return R.string.categoria_puntos_venta;
        }
    }

    /** Nombre asociado al filtro activo para abrir el detalle correcto al pulsar el botón. */
    private String tipoSeleccionadoExtra() {
        return marcadores.isEmpty() ? null : marcadores.get(0).nombre;
    }

    private void mostrarInformacion(View vista, MarcadorMapa marcador) {
        ((TextView) vista.findViewById(R.id.texto_marcador_nombre)).setText(marcador.nombre);
        ((TextView) vista.findViewById(R.id.texto_marcador_tipo))
                .setText(getString(R.string.categoria_cafeterias) + " · "
                        + marcador.zona);
        ((TextView) vista.findViewById(R.id.texto_marcador_direccion)).setText(marcador.zona);
        ((TextView) vista.findViewById(R.id.texto_marcador_rating)).setText("4.9");
    }

    /** Abre el detalle de la cafetería mostrada en el panel. */
    private void abrirDetalle(View vista, String nombre) {
        Cafeteria cafeteria = RepositorioDatos.obtenerCafeteria(nombre);
        if (cafeteria == null) {
            return;
        }
        Intent intento = new Intent(requireContext(), DetalleCafeteriaActivity.class);
        intento.putExtra(DetalleCafeteriaActivity.EXTRA_NOMBRE_CAFETERIA, cafeteria.nombre);
        startActivity(intento);
    }
}