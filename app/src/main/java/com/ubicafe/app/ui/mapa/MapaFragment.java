package com.ubicafe.app.ui.mapa;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.Fragment;

import com.ubicafe.app.R;
import com.ubicafe.app.datos.RepositorioDatos;
import com.ubicafe.app.modelo.Cafeteria;
import com.ubicafe.app.modelo.Productor;
import com.ubicafe.app.modelo.PuntoVenta;
import com.ubicafe.app.modelo.Tostaderia;
import com.ubicafe.app.ui.cafeterias.DetalleCafeteriaActivity;
import com.ubicafe.app.ui.marcas.DetalleMarcaActivity;
import com.ubicafe.app.ui.productores.DetalleProductorActivity;
import com.ubicafe.app.ui.tostaderias.DetalleTostaderiaActivity;
import com.ubicafe.app.util.UiUtils;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.util.ArrayList;
import java.util.List;

/**
 * Pestaña de MAPA (P4).
 * Mapa real de OpenStreetMap (osmdroid) con los marcadores de
 * cafeterías, tostadurías, productores y puntos de venta. Los chips
 * filtran los marcadores visibles y "Ver información" abre el detalle
 * correcto según el tipo del marcador elegido.
 */
public class MapaFragment extends Fragment {

    private final List<MarcadorMapa> marcadores = new ArrayList<>();
    private final List<Marker> marcadoresEnMapa = new ArrayList<>();
    private final List<TextView> chips = new ArrayList<>();

    private MapView mapa;
    private MarcadorMapa seleccionado;
    private String tipoSeleccionado = ""; // "" = todos

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflador, @Nullable ViewGroup contenedor,
                             @Nullable Bundle estado) {
        ConfiguracionMapa.inicializar(requireContext());
        return inflador.inflate(R.layout.fragment_mapa, contenedor, false);
    }

    @Override
    public void onViewCreated(@NonNull View vista, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(vista, savedInstanceState);

        mapa = vista.findViewById(R.id.mapa_vista);
        mapa.setMultiTouchControls(true);

        construirMarcadores();
        agregarMarcadoresAlMapa();
        agregarUbicacionDelUsuario();

        // Mapa centrado en La Paz.
        mapa.getController().setZoom(13.0);
        mapa.getController().setCenter(new GeoPoint(ConfiguracionMapa.LAT_LA_PAZ,
                ConfiguracionMapa.LNG_LA_PAZ));

        crearFiltros(vista);

        if (!marcadores.isEmpty()) {
            mostrarInformacion(vista, marcadores.get(0));
        }

        vista.findViewById(R.id.btn_ver_informacion)
                .setOnClickListener(v -> abrirDetalleDe(seleccionado));
    }

    /** Carga un punto por cada cafetería, tostaduría, productor y punto de venta. */
    private void construirMarcadores() {
        marcadores.clear();

        int colorCafeteria = requireContext().getColor(R.color.verde_oscuro);
        for (Cafeteria c : RepositorioDatos.obtenerCafeterias()) {
            marcadores.add(new MarcadorMapa(c.nombre, c.zona, c.lat, c.lng,
                    MarcadorMapa.TIPO_CAFETERIA, colorCafeteria, c.nombre));
        }

        int colorTostaderia = requireContext().getColor(R.color.cafe_accent);
        for (Tostaderia t : RepositorioDatos.obtenerTostaderias()) {
            marcadores.add(new MarcadorMapa(t.nombre, t.region, t.lat, t.lng,
                    MarcadorMapa.TIPO_TOSTADERIA, colorTostaderia, t.nombre));
        }

        int colorProductor = requireContext().getColor(R.color.verde_medio);
        for (Productor p : RepositorioDatos.obtenerProductores()) {
            marcadores.add(new MarcadorMapa(p.nombreFinca, p.origen, p.lat, p.lng,
                    MarcadorMapa.TIPO_PRODUCTOR, colorProductor, p.nombreFinca));
        }

        int colorPuntoVenta = requireContext().getColor(R.color.caramelo_claro);
        for (PuntoVenta pv : RepositorioDatos.obtenerPuntosDeVenta()) {
            marcadores.add(new MarcadorMapa(pv.marca + " · " + pv.local, pv.barrio,
                    pv.lat, pv.lng, MarcadorMapa.TIPO_PUNTO_VENTA, colorPuntoVenta, pv.marca));
        }
    }

    /** Dibuja cada punto como un marcador de osmdroid sobre el mapa. */
    private void agregarMarcadoresAlMapa() {
        for (MarcadorMapa dato : marcadores) {
            Marker marcador = new Marker(mapa);
            marcador.setPosition(new GeoPoint(dato.lat, dato.lng));
            marcador.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            marcador.setIcon(pinDeColor(dato.color));
            marcador.setRelatedObject(dato);
            marcador.setOnMarkerClickListener((mk, overlay) -> {
                seleccionado = (MarcadorMapa) mk.getRelatedObject();
                mostrarInformacion(requireView(), seleccionado);
                return true;
            });
            mapa.getOverlays().add(marcador);
            marcadoresEnMapa.add(marcador);
        }
    }

    /** Punto fijo "tu ubicación" (Achumani) sin permisos de GPS. */
    private void agregarUbicacionDelUsuario() {
        Marker usuario = new Marker(mapa);
        usuario.setPosition(new GeoPoint(-16.535, -68.070));
        usuario.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        usuario.setIcon(pinDeColor(requireContext().getColor(R.color.azul_ubicacion)));
        usuario.setTitle("Achumani, La Paz");
        usuario.setOnMarkerClickListener((mk, overlay) -> {
            seleccionado = null;
            mostrarInformacionDeUbicacion(requireView());
            return true;
        });
        mapa.getOverlays().add(usuario);
    }

    /** Pin del mismo vector, teñido con el color de su categoría. */
    private Drawable pinDeColor(int color) {
        Drawable pin = ContextCompat.getDrawable(requireContext(), R.drawable.ic_map_pin);
        Drawable teñido = DrawableCompat.wrap(pin).mutate();
        DrawableCompat.setTint(teñido, color);
        return teñido;
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
                aplicarFiltro();
            });
            chips.add(chip);
        }
        refrescarChips();
    }

    private void refrescarChips() {
        for (TextView chip : chips) {
            boolean seleccionado = chip.getText().toString().equals(
                    tipoSeleccionado.isEmpty() ? getString(R.string.mapa_filtro_todos)
                            : tipoSeleccionado);
            UiUtils.marcarChipSeleccionado(chip, seleccionado);
        }
    }

    /** Muestra solo los marcadores de la categoría elegida. */
    private void aplicarFiltro() {
        for (int i = 0; i < marcadores.size(); i++) {
            boolean visible = tipoSeleccionado.isEmpty()
                    || marcadores.get(i).tipo.equals(tipoSeleccionado);
            marcadoresEnMapa.get(i).setVisible(visible);
        }
        mapa.invalidate();

        // Panel: mostrar el primer marcador visible o un aviso.
        if (tipoSeleccionado.isEmpty()) {
            if (!marcadores.isEmpty()) {
                mostrarInformacion(requireView(), marcadores.get(0));
            }
        } else {
            for (int i = 0; i < marcadores.size(); i++) {
                if (marcadores.get(i).tipo.equals(tipoSeleccionado)) {
                    mostrarInformacion(requireView(), marcadores.get(i));
                    return;
                }
            }
            mostrarInformacionDeUbicacion(requireView());
        }
    }

    /** Rellena el panel inferior con el marcador elegido. */
    private void mostrarInformacion(View vista, MarcadorMapa marcador) {
        vista.findViewById(R.id.btn_ver_informacion).setVisibility(View.VISIBLE);
        ((TextView) vista.findViewById(R.id.texto_marcador_nombre)).setText(marcador.nombre);
        ((TextView) vista.findViewById(R.id.texto_marcador_tipo))
                .setText(marcador.tipo + " · " + marcador.zona);
        ((TextView) vista.findViewById(R.id.texto_marcador_direccion)).setText(marcador.zona);
        vista.findViewById(R.id.fila_rating).setVisibility(
                MarcadorMapa.TIPO_CAFETERIA.equals(marcador.tipo) ? View.VISIBLE : View.GONE);
        if (MarcadorMapa.TIPO_CAFETERIA.equals(marcador.tipo)) {
            ((TextView) vista.findViewById(R.id.texto_marcador_rating)).setText("4.9");
        }
    }

    /** Panel informativo para la ubicación fija del usuario. */
    private void mostrarInformacionDeUbicacion(View vista) {
        vista.findViewById(R.id.btn_ver_informacion).setVisibility(View.GONE);
        ((TextView) vista.findViewById(R.id.texto_marcador_nombre)).setText("Tu ubicación");
        ((TextView) vista.findViewById(R.id.texto_marcador_tipo))
                .setText(getString(R.string.categoria_puntos_venta) + " · Achumani");
        ((TextView) vista.findViewById(R.id.texto_marcador_direccion))
                .setText("Achumani, La Paz (ubicación de ejemplo)");
        vista.findViewById(R.id.fila_rating).setVisibility(View.GONE);
    }

    /** Abre el detalle que corresponde al tipo del marcador seleccionado. */
    private void abrirDetalleDe(MarcadorMapa marcador) {
        if (marcador == null) {
            return;
        }
        Intent intento;
        switch (marcador.tipo) {
            case MarcadorMapa.TIPO_CAFETERIA:
                intento = new Intent(requireContext(), DetalleCafeteriaActivity.class);
                intento.putExtra(DetalleCafeteriaActivity.EXTRA_NOMBRE_CAFETERIA,
                        marcador.nombreDetalle);
                break;
            case MarcadorMapa.TIPO_TOSTADERIA:
                intento = new Intent(requireContext(), DetalleTostaderiaActivity.class);
                intento.putExtra(DetalleTostaderiaActivity.EXTRA_NOMBRE_TOSTADERIA,
                        marcador.nombreDetalle);
                break;
            case MarcadorMapa.TIPO_PRODUCTOR:
                intento = new Intent(requireContext(), DetalleProductorActivity.class);
                intento.putExtra(DetalleProductorActivity.EXTRA_NOMBRE_FINCA,
                        marcador.nombreDetalle);
                break;
            case MarcadorMapa.TIPO_PUNTO_VENTA:
                intento = new Intent(requireContext(), DetalleMarcaActivity.class);
                intento.putExtra(DetalleMarcaActivity.EXTRA_NOMBRE_MARCA,
                        marcador.nombreDetalle);
                break;
            default:
                return;
        }
        startActivity(intento);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapa != null) {
            mapa.onResume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mapa != null) {
            mapa.onPause();
        }
    }

    @Override
    public void onDestroyView() {
        if (mapa != null) {
            mapa.onDetach();
            mapa = null;
        }
        super.onDestroyView();
    }
}