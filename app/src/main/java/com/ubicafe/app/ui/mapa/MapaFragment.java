package com.ubicafe.app.ui.mapa;

import android.Manifest;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.Fragment;

import com.ubicafe.app.R;
import com.ubicafe.app.modelo.Rol;
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
 * PESTAÑA DE MAPA (P4)
 * ---------------------------------------------------------------
 * El mapa del censo con los 213 lugares, cada pin con el color de su
 * papel. Los chips filtran por categoría sin volver a leer los datos:
 * solo se oculta o se muestra cada pin, que es lo que hace fluido
 * cambiar de categoría.
 */
public class MapaFragment extends Fragment {

    /**
     * El permiso se pide al tocar el subtítulo de la cabecera, no al
     * abrir el mapa: un permiso pedido sin que la persona esté buscando
     * su ubicación se deniega por costumbre, y el mapa funciona igual
     * sin él.
     */
    private final ActivityResultLauncher<String> pedirUbicacion =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                    concedido -> {
                        Geolocalizador.recordarDecision(requireContext(), concedido);
                        View raiz = getView();
                        if (raiz != null) {
                            agregarPosicionDelUsuario();
                            actualizarSubtitulo(raiz);
                        }
                    });

    private final List<MarcadorMapa> marcadores = new ArrayList<>();
    private final List<Marker> pines = new ArrayList<>();
    private final List<TextView> chips = new ArrayList<>();

    private MapView mapa;
    private Marker pinUsuario;
    private MarcadorMapa seleccionado;
    private Rol categoriaSeleccionada;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflador, @Nullable ViewGroup contenedor,
                             @Nullable Bundle estado) {
        ConfiguracionMapa.inicializar(requireContext());
        return inflador.inflate(R.layout.fragment_mapa, contenedor, false);
    }

    @Override
    public void onViewCreated(@NonNull View vista, @Nullable Bundle estado) {
        super.onViewCreated(vista, estado);

        mapa = vista.findViewById(R.id.mapa_vista);
        mapa.setMultiTouchControls(true);

        marcadores.addAll(MarcadoresMapa.todos());
        crearPines();
        agregarPosicionDelUsuario();

        mapa.getController().setZoom(13.0);
        mapa.getController().setCenter(new GeoPoint(
                ConfiguracionMapa.LAT_LA_PAZ, ConfiguracionMapa.LNG_LA_PAZ));

        crearFiltros(vista);
        actualizarSubtitulo(vista);

        if (!marcadores.isEmpty()) {
            seleccionado = marcadores.get(0);
            mostrarInformacion(vista, seleccionado);
        }

        vista.findViewById(R.id.btn_ver_informacion)
                .setOnClickListener(v -> abrirDetalleDe(seleccionado));
    }

    /**
     * La cabecera dice cuántos lugares hay y, debajo, si conocemos la
     * posición de la persona. Sin permiso se ofrece activarla en el
     * sitio donde se necesita, no en un menú aparte.
     */
    private void actualizarSubtitulo(View vista) {
        int total = marcadores.size();
        ((TextView) vista.findViewById(R.id.texto_ubicacion))
                .setText(getResources().getQuantityString(
                        R.plurals.mapa_subtitulo, total, total));

        boolean conPosicion = Geolocalizador.hayPosicion(requireContext());
        TextView estado = vista.findViewById(R.id.texto_mi_ubicacion);
        estado.setText(conPosicion
                ? getString(R.string.mapa_ubicacion_activada)
                : getString(R.string.mapa_ubicacion_no_disponible));
        estado.setClickable(!conPosicion);
        estado.setOnClickListener(conPosicion ? null
                : v -> pedirUbicacion.launch(
                        Manifest.permission.ACCESS_COARSE_LOCATION));
    }

    /** Un pin por lugar, cada uno con el color de su papel. */
    private void crearPines() {
        for (MarcadorMapa dato : marcadores) {
            Marker pin = new Marker(mapa);
            pin.setPosition(new GeoPoint(dato.lat, dato.lng));
            pin.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            pin.setIcon(pinDeColor(dato.color));
            pin.setTitle(dato.nombre);
            pin.setRelatedObject(dato);
            pin.setOnMarkerClickListener((marcador, overlay) -> {
                seleccionado = (MarcadorMapa) marcador.getRelatedObject();
                if (getView() != null) {
                    mostrarInformacion(getView(), seleccionado);
                }
                return true;
            });
            mapa.getOverlays().add(pin);
            pines.add(pin);
        }
    }

    /**
     * El punto de la persona, si autorizó la ubicación. No se inventa
     * uno: sin permiso, el mapa no muestra ningún punto azul.
     *
     * Se quita el anterior antes de poner el nuevo porque se vuelve a
     * llamar cuando llega el permiso, y sin esto quedaría un pin azul
     * por cada permiso concedido.
     */
    private void agregarPosicionDelUsuario() {
        if (pinUsuario != null) {
            mapa.getOverlays().remove(pinUsuario);
            pinUsuario = null;
        }
        if (!Geolocalizador.tienePermiso(requireContext())) {
            return;
        }
        android.location.Location posicion =
                Geolocalizador.ultimaPosicion(requireContext());
        if (posicion == null) {
            return;
        }
        pinUsuario = new Marker(mapa);
        pinUsuario.setPosition(new GeoPoint(posicion.getLatitude(), posicion.getLongitude()));
        pinUsuario.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        pinUsuario.setIcon(pinDeColor(ContextCompat.getColor(
                requireContext(), R.color.azul_ubicacion)));
        pinUsuario.setTitle(getString(R.string.inicio_cerca_de_ti));
        mapa.getOverlays().add(pinUsuario);
        mapa.invalidate();
    }

    private Drawable pinDeColor(int color) {
        Drawable pin = ContextCompat.getDrawable(requireContext(), R.drawable.ic_map_pin);
        Drawable tenido = DrawableCompat.wrap(pin).mutate();
        DrawableCompat.setTint(tenido, color);
        return tenido;
    }

    /** Los chips de categoría. Null = todas. */
    private void crearFiltros(View vista) {
        List<Rol> opciones = new ArrayList<>();
        opciones.add(null);
        opciones.add(Rol.CAFETERIA);
        opciones.add(Rol.MARCA);
        opciones.add(Rol.TOSTADURIA);
        opciones.add(Rol.PRODUCTOR);
        opciones.add(Rol.TIENDA);

        LinearLayout fila = vista.findViewById(R.id.fila_filtros);
        for (Rol rol : opciones) {
            String etiqueta = rol == null
                    ? getString(R.string.mapa_filtro_todos)
                    : rol.etiquetaCorta();
            TextView chip = UiUtils.crearChip(fila, etiqueta);
            chip.setOnClickListener(v -> {
                categoriaSeleccionada = rol;
                refrescarChips();
                aplicarFiltro();
            });
            chips.add(chip);
        }
        refrescarChips();
    }

    private void refrescarChips() {
        int indice = 0;
        for (TextView chip : chips) {
            Rol[] opciones = {null, Rol.CAFETERIA, Rol.MARCA, Rol.TOSTADURIA,
                    Rol.PRODUCTOR, Rol.TIENDA};
            boolean elegido = opciones[indice] == categoriaSeleccionada;
            UiUtils.marcarChipSeleccionado(chip, elegido);
            indice++;
        }
    }

    /**
     * Muestra u oculta los pines de la categoría. No se reconstruye el
     * mapa: 213 pins nuevos tardarían más que un simple cambio de
     * visibilidad.
     */
    private void aplicarFiltro() {
        for (int i = 0; i < marcadores.size(); i++) {
            boolean visible = categoriaSeleccionada == null
                    || marcadores.get(i).rol == categoriaSeleccionada;
            pines.get(i).setVisible(visible);
        }
        mapa.invalidate();

        MarcadorMapa primero = primeroVisible();
        if (primero == null) {
            mostrarPanelVacio();
            return;
        }
        seleccionado = primero;
        if (getView() != null) {
            mostrarInformacion(getView(), primero);
        }
    }

    private MarcadorMapa primeroVisible() {
        for (int i = 0; i < marcadores.size(); i++) {
            boolean visible = categoriaSeleccionada == null
                    || marcadores.get(i).rol == categoriaSeleccionada;
            if (visible) {
                return marcadores.get(i);
            }
        }
        return null;
    }

    /** Rellena el panel inferior con el marcador que se tocó. */
    private void mostrarInformacion(View vista, MarcadorMapa marcador) {
        vista.findViewById(R.id.btn_ver_informacion).setVisibility(View.VISIBLE);
        ((TextView) vista.findViewById(R.id.texto_marcador_nombre)).setText(marcador.nombre);
        ((TextView) vista.findViewById(R.id.texto_marcador_tipo))
                .setText(marcador.rol.etiqueta());
        ((TextView) vista.findViewById(R.id.texto_marcador_direccion))
                .setText(marcador.ubicacionAvisada
                        ? getString(R.string.detalle_ubicacion_no_verificada)
                        : marcador.zona);
    }

    private void mostrarPanelVacio() {
        View vista = getView();
        if (vista == null) {
            return;
        }
        vista.findViewById(R.id.btn_ver_informacion).setVisibility(View.GONE);
        ((TextView) vista.findViewById(R.id.texto_marcador_nombre))
                .setText(R.string.lista_vacia_titulo);
        ((TextView) vista.findViewById(R.id.texto_marcador_tipo)).setText("");
        ((TextView) vista.findViewById(R.id.texto_marcador_direccion))
                .setText(R.string.lista_vacia_detalle);
    }

    /** Lleva a la ficha que corresponde al papel del pin. */
    private void abrirDetalleDe(MarcadorMapa marcador) {
        if (marcador == null) {
            return;
        }
        String marca = MarcadoresMapa.marcaDe(marcador.idEntidad);
        Intent intento;
        if (marcador.rol == Rol.TIENDA && marca != null) {
            intento = new Intent(requireContext(), DetalleMarcaActivity.class);
            intento.putExtra(DetalleMarcaActivity.EXTRA_NOMBRE_MARCA, marca);
        } else {
            switch (marcador.rol) {
                case TOSTADURIA:
                    intento = new Intent(requireContext(), DetalleTostaderiaActivity.class);
                    break;
                case PRODUCTOR:
                    intento = new Intent(requireContext(), DetalleProductorActivity.class);
                    break;
                default:
                    intento = new Intent(requireContext(), DetalleCafeteriaActivity.class);
                    break;
            }
            intento.putExtra(DetalleCafeteriaActivity.EXTRA_ID_ENTIDAD, marcador.idEntidad);
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
        // La vista se recrea al volver a la pestaña: sin limpiarlos,
        // los marcadores de la vista anterior se accumulate.
        marcadores.clear();
        pines.clear();
        chips.clear();
        pinUsuario = null;
        super.onDestroyView();
    }
}
