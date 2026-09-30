package com.ubicafe.app.ui.inicio;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.ubicafe.app.R;
import com.ubicafe.app.datos.RepositorioDatos;
import com.ubicafe.app.modelo.Entidad;
import com.ubicafe.app.modelo.Estadisticas;
import com.ubicafe.app.ui.busqueda.BusquedaActivity;
import com.ubicafe.app.ui.cafeterias.DetalleCafeteriaActivity;
import com.ubicafe.app.ui.cafeterias.ListaCafeteriasActivity;
import com.ubicafe.app.ui.mapa.Geolocalizador;
import com.ubicafe.app.ui.mapa.MapaActivity;
import com.ubicafe.app.ui.marcas.ListaMarcasActivity;
import com.ubicafe.app.ui.productores.ListaProductoresActivity;
import com.ubicafe.app.ui.tostaderias.ListaTostaderiasActivity;
import com.ubicafe.app.util.CargadorFotos;
import com.ubicafe.app.util.Distancia;
import com.ubicafe.app.util.UiUtils;

import java.util.List;

/**
 * Pestaña de INICIO (P3)
 * ---------------------------------------------------------------
 * La pantalla de entrada muestra las cifras reales del censo y los
 * accesos a las categorías. No inventa reseñas ni distancias: si
 * la persona no activó la ubicación, la tarjeta "Cerca de ti" lo dice
 * en vez de mostrar "450 m" fijos.
 */
public class InicioFragment extends Fragment {

    /**
     * Pide la ubicación cuando la persona toca la tarjeta. Se pide
     * desde el toque y no al abrir la app: un permiso pedido sin
     * contexto se deniega por costumbre, y este no es necesario para
     * usar la app.
     */
    private final ActivityResultLauncher<String> pedirUbicacion =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                    concedido -> {
                        Geolocalizador.recordarDecision(requireContext(), concedido);
                        View raiz = getView();
                        if (raiz != null) {
                            prepararCercaDeTi(raiz);
                        }
                    });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflador, @Nullable ViewGroup contenedor,
                             @Nullable Bundle estado) {
        return inflador.inflate(R.layout.fragment_inicio, contenedor, false);
    }

    @Override
    public void onViewCreated(@NonNull View vista, @Nullable Bundle estado) {
        super.onViewCreated(vista, estado);

        prepararCategorias(vista);
        prepararBusqueda(vista);
        prepararCercaDeTi(vista);
        prepararMacrodistritos(vista);
        prepararBannerProductores(vista);
        pintarResumen(vista);
    }

    /** Tiles de categoría: cada uno abre su listado. */
    private void prepararCategorias(View vista) {
        vista.findViewById(R.id.tile_cafeterias)
                .setOnClickListener(v -> abrir(ListaCafeteriasActivity.class));
        vista.findViewById(R.id.tile_marcas)
                .setOnClickListener(v -> abrir(ListaMarcasActivity.class));
        vista.findViewById(R.id.tile_tostaderias)
                .setOnClickListener(v -> abrir(ListaTostaderiasActivity.class));
    }

    /** El buscador del inicio abre la búsqueda global (P10). */
    private void prepararBusqueda(View vista) {
        vista.findViewById(R.id.buscador_inicio)
                .setOnClickListener(v -> abrir(BusquedaActivity.class));
    }

    /**
     * "Cerca de ti": el lugar más cercano al que se registró la
     * ubicación. Sin permiso, no se pone ninguno: se explica cómo
     * activarlo y la tarjeta sigue levando al mapa.
     */
    private void prepararCercaDeTi(View vista) {
        View tarjeta = vista.findViewById(R.id.card_cerca_de_ti);
        TextView boton = vista.findViewById(R.id.btn_ver_informacion);
        Entidad cerca = obtenerMasCercano();

        if (cerca == null) {
            // Sin permiso la tarjeta no desaparece: se explica y se
            // ofrece el botón. Ocultarla dejaría a la persona sin
            // forma de activar la ubicación desde la app.
            ((TextView) vista.findViewById(R.id.texto_nombre_cerca))
                    .setText(R.string.inicio_cerca_sin_ubicacion);
            vista.findViewById(R.id.texto_tipo_cerca).setVisibility(View.GONE);
            vista.findViewById(R.id.texto_zona_cerca).setVisibility(View.GONE);
            vista.findViewById(R.id.foto_cerca).setVisibility(View.GONE);
            ((TextView) vista.findViewById(R.id.texto_distancia)).setText(
                    Geolocalizador.permisoDenegado(requireContext())
                            ? R.string.inicio_ubicacion_en_ajustes
                            : R.string.inicio_activar_ubicacion);

            boton.setText(R.string.inicio_activar_ubicacion_boton);
            View.OnClickListener activar = v ->
                    pedirUbicacion.launch(android.Manifest.permission.ACCESS_COARSE_LOCATION);
            boton.setOnClickListener(activar);
            tarjeta.setOnClickListener(activar);
            return;
        }

        // La foto del más cercano, si la tiene.
        ImageView foto = vista.findViewById(R.id.foto_cerca);
        if (cerca.foto.isEmpty()) {
            foto.setVisibility(View.GONE);
        } else {
            foto.setVisibility(View.VISIBLE);
            CargadorFotos.pintarRecortada(foto, requireContext(), cerca.foto,
                    cerca.rolPrincipal.icono());
        }

        ((TextView) vista.findViewById(R.id.texto_nombre_cerca)).setText(cerca.nombre);
        vista.findViewById(R.id.texto_tipo_cerca).setVisibility(View.VISIBLE);
        vista.findViewById(R.id.texto_zona_cerca).setVisibility(View.VISIBLE);
        ((TextView) vista.findViewById(R.id.texto_tipo_cerca)).setText(cerca.rolesComoTexto());
        ((TextView) vista.findViewById(R.id.texto_zona_cerca)).setText(cerca.macrodistrito.isEmpty()
                ? getString(R.string.app_ubicacion)
                : cerca.macrodistrito);

        double kilometros = Geolocalizador.kilometrosA(requireContext(), cerca.lat, cerca.lng);
        ((TextView) vista.findViewById(R.id.texto_distancia)).setText(kilometros < 0
                ? getString(R.string.inicio_activar_ubicacion)
                : getString(R.string.inicio_distancia, Distancia.formatear(kilometros)));

        boton.setText(R.string.inicio_categoria_ver_informacion);
        View.OnClickListener abrir = v -> DetalleCafeteriaActivity.abrir(requireContext(), cerca);
        tarjeta.setOnClickListener(abrir);
        boton.setOnClickListener(abrir);
    }

    /**
     * Los chips de macrodistrito salen del censo, no de una lista fija.
     * El de más lugares va primero, que es el más útil.
     */
    private void prepararMacrodistritos(View vista) {
        Estadisticas estadisticas = RepositorioDatos.obtenerEstadisticas();
        if (estadisticas == null) {
            return;
        }
        List<String> distritos = estadisticas.macrodistritosOrdenados();
        if (distritos.size() > 4) {
            distritos = distritos.subList(0, 4);
        }

        LinearLayout fila = vista.findViewById(R.id.fila_macrodistritos);
        for (String distrito : distritos) {
            TextView chip = UiUtils.crearChip(fila, distrito);
            chip.setOnClickListener(v -> abrirListaDe(distrito));
        }
    }

    /** Abre el listado de cafeterías con el macrodistrito preseleccionado. */
    private void abrirListaDe(String distrito) {
        Intent intento = new Intent(requireContext(), ListaCafeteriasActivity.class);
        intento.putExtra(ListaCafeteriasActivity.EXTRA_ZONA, distrito);
        startActivity(intento);
    }

    private void prepararBannerProductores(View vista) {
        vista.findViewById(R.id.banner_productores)
                .setOnClickListener(v -> abrir(ListaProductoresActivity.class));
    }

    /**
     * El resumen de abajo: las cifras del censo, tal cual. Están
     * calculadas en Estadisticas, no escritas a mano, para que no
     * puedan quedar desactualizadas si cambia el JSON.
     */
    private void pintarResumen(View vista) {
        Estadisticas e = RepositorioDatos.obtenerEstadisticas();
        if (e == null) {
            vista.findViewById(R.id.bloque_resumen).setVisibility(View.GONE);
            return;
        }
        ((TextView) vista.findViewById(R.id.texto_resumen))
                .setText(getString(R.string.inicio_resumen_censo,
                        e.totalEntidades, e.cafeterias, e.marcas,
                        e.tostadurias, e.productores, e.puntosDeVenta));
    }

    /** El lugar con coordenadas más cercano, o null si no hay posición. */
    private Entidad obtenerMasCercano() {
        if (!Geolocalizador.tienePermiso(requireContext())) {
            return null;
        }
        android.location.Location posicion =
                Geolocalizador.ultimaPosicion(requireContext());
        if (posicion == null) {
            return null;
        }
        List<Entidad> cercanas = RepositorioDatos.obtenerEntidadesCercanas(
                posicion.getLatitude(), posicion.getLongitude(), 1);
        return cercanas.isEmpty() ? null : cercanas.get(0);
    }

    private void abrir(Class<?> actividad) {
        startActivity(new Intent(requireContext(), actividad));
    }

    @Override
    public void onResume() {
        super.onResume();
        // La distancia depende de dónde esté la persona, así que se
        // recalcula al volver a esta pestaña.
        if (getView() != null) {
            prepararCercaDeTi(getView());
        }
    }
}
