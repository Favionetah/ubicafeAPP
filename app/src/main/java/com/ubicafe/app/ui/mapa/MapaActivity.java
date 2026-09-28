package com.ubicafe.app.ui.mapa;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.ubicafe.app.R;
import com.ubicafe.app.datos.RepositorioDatos;
import com.ubicafe.app.modelo.Entidad;
import com.ubicafe.app.modelo.Rol;
import com.ubicafe.app.ui.cafeterias.DetalleCafeteriaActivity;
import com.ubicafe.app.ui.marcas.DetalleMarcaActivity;
import com.ubicafe.app.ui.productores.DetalleProductorActivity;
import com.ubicafe.app.ui.tostaderias.DetalleTostaderiaActivity;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.util.ArrayList;
import java.util.List;

/**
 * MAPA A PANTALLA COMPLETA
 * ---------------------------------------------------------------
 * Se abre desde "Ver en el mapa" de cualquier ficha, y hay tres
 * motivos distintos: ver un lugar, ver los locales de una cadena, o
 * ver una categoría entera. Cada uno entra por un extra diferente.
 */
public class MapaActivity extends AppCompatActivity {

    /** Centra el mapa en un lugar y lo deja seleccionado. */
    public static final String EXTRA_CENTRAR_EN = "centrar_en";

    /** Muestra solo los locales de una cadena. */
    public static final String EXTRA_FILTRO_MARCA = "filtro_marca";

    /** Muestra solo una categoría. */
    public static final String EXTRA_CATEGORIA = "categoria";

    private final List<MarcadorMapa> marcadores = new ArrayList<>();
    private final List<Marker> marcadoresEnMapa = new ArrayList<>();

    private MapView mapa;
    private MarcadorMapa seleccionado;

    @Override
    protected void onCreate(@Nullable Bundle estado) {
        super.onCreate(estado);
        ConfiguracionMapa.inicializar(this);
        setContentView(R.layout.activity_mapa);

        ((TextView) findViewById(R.id.texto_marcador_titulo)).setVisibility(View.GONE);
        ((TextView) findViewById(R.id.texto_marcador_zona)).setVisibility(View.GONE);

        findViewById(R.id.btn_volver).setOnClickListener(v -> finish());

        mapa = findViewById(R.id.mapa_vista);
        mapa.setMultiTouchControls(true);

        String idLugar = getIntent().getStringExtra(EXTRA_CENTRAR_EN);
        String marca = getIntent().getStringExtra(EXTRA_FILTRO_MARCA);
        String categoria = getIntent().getStringExtra(EXTRA_CATEGORIA);

        if (idLugar != null) {
            mostrarLugar(idLugar);
        } else if (marca != null) {
            mostrarMarca(marca);
        } else {
            mostrarCategoria(categoria);
        }

        if (marcadores.isEmpty()) {
            mostrarAviso("Ninguno de estos lugares tiene coordenadas en el censo");
            return;
        }

        desplegarMarcadores();
        centrar(marcadores.get(0));
        seleccionado = marcadores.get(0);
        mostrarInformacion(seleccionado);

        findViewById(R.id.btn_ver_informacion)
                .setOnClickListener(v -> abrirDetalleDe(seleccionado));
    }

    /** Un solo lugar, centrado y con zoom de calle. */
    private void mostrarLugar(String id) {
        Entidad lugar = RepositorioDatos.obtenerEntidad(id);
        if (lugar == null) {
            return;
        }
        if (lugar.tieneCoordenadas()) {
            marcadores.add(MarcadoresMapa.desdeEntidad(lugar, 0));
        }
        mapa.getController().setZoom(16.0);
    }

    /** Los locales de una cadena, con un zoom que abarque la ciudad. */
    private void mostrarMarca(String nombreMarca) {
        marcadores.addAll(MarcadoresMapa.deMarca(nombreMarca,
                MarcadoresMapa.colorDe(Rol.TIENDA)));
    }

    /** Todos los lugares, o los de una categoría, según el extra. */
    private void mostrarCategoria(@Nullable String categoria) {
        if (categoria == null) {
            marcadores.addAll(MarcadoresMapa.todos());
            return;
        }
        for (Rol rol : Rol.values()) {
            if (rol.etiquetaCorta().equalsIgnoreCase(categoria)
                    || rol.name().equalsIgnoreCase(categoria)) {
                marcadores.addAll(MarcadoresMapa.porCategoria(rol,
                        MarcadoresMapa.colorDe(rol)));
                return;
            }
        }
        marcadores.addAll(MarcadoresMapa.todos());
    }

    /** Dibuja un pin por marcador, cada uno con el color de su papel. */
    private void desplegarMarcadores() {
        for (MarcadorMapa dato : marcadores) {
            Marker marcador = new Marker(mapa);
            marcador.setPosition(new GeoPoint(dato.lat, dato.lng));
            marcador.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            marcador.setIcon(pinDeColor(dato.color));
            marcador.setTitle(dato.nombre);
            marcador.setRelatedObject(dato);
            marcador.setOnMarkerClickListener((pin, overlay) -> {
                seleccionado = (MarcadorMapa) pin.getRelatedObject();
                mostrarInformacion(seleccionado);
                return true;
            });
            mapa.getOverlays().add(marcador);
            marcadoresEnMapa.add(marcador);
        }
    }

    private android.graphics.drawable.Drawable pinDeColor(int color) {
        android.graphics.drawable.Drawable pin =
                androidx.core.content.ContextCompat.getDrawable(this, R.drawable.ic_map_pin);
        android.graphics.drawable.Drawable tenido =
                androidx.core.graphics.drawable.DrawableCompat.wrap(pin).mutate();
        androidx.core.graphics.drawable.DrawableCompat.setTint(tenido, color);
        return tenido;
    }

    /**
     * Encuadra el mapa. Con un solo marcador se centra en él; con
     * varios, en el punto medio de todos, para que ninguno quede fuera.
     */
    private void centrar(MarcadorMapa primero) {
        if (marcadores.size() == 1) {
            mapa.getController().setZoom(15.0);
            mapa.getController().setCenter(new GeoPoint(primero.lat, primero.lng));
        } else {
            double lat = 0;
            double lng = 0;
            for (MarcadorMapa dato : marcadores) {
                lat += dato.lat;
                lng += dato.lng;
            }
            mapa.getController().setZoom(13.0);
            mapa.getController().setCenter(new GeoPoint(
                    lat / marcadores.size(), lng / marcadores.size()));
        }
    }

    /** Pinta el panel con el punto elegido y su aviso si lo tiene. */
    private void mostrarInformacion(MarcadorMapa marcador) {
        TextView titulo = findViewById(R.id.texto_marcador_titulo);
        TextView zona = findViewById(R.id.texto_marcador_zona);
        titulo.setVisibility(View.VISIBLE);
        zona.setVisibility(View.VISIBLE);

        titulo.setText(marcador.nombre);
        zona.setText(marcador.zona.isEmpty()
                ? marcador.rol.etiqueta()
                : marcador.rol.etiqueta() + " · " + marcador.zona);

        if (marcador.ubicacionAvisada) {
            Entidad lugar = RepositorioDatos.obtenerEntidad(marcador.idEntidad);
            String aviso = lugar == null ? "" : lugar.notaUbicacion;
            zona.setText(aviso.isEmpty()
                    ? zona.getText().toString() + " · "
                        + getString(R.string.detalle_ubicacion_no_verificada)
                    : zona.getText().toString() + " · " + aviso);
        }
    }

    private void mostrarAviso(String mensaje) {
        TextView titulo = findViewById(R.id.texto_marcador_titulo);
        titulo.setVisibility(View.VISIBLE);
        titulo.setText(mensaje);
        findViewById(R.id.btn_ver_informacion).setVisibility(View.GONE);
    }

    /** Abre la ficha que corresponde al papel del marcador. */
    private void abrirDetalleDe(MarcadorMapa marcador) {
        if (marcador == null) {
            return;
        }
        // Un local de una cadena tiene mejor ficha en la cadena: ahí
        // están sus demás locales.
        String marca = MarcadoresMapa.marcaDe(marcador.idEntidad);
        Intent intento;
        if (marcador.rol == Rol.TIENDA && marca != null) {
            intento = new Intent(this, DetalleMarcaActivity.class);
            intento.putExtra(DetalleMarcaActivity.EXTRA_NOMBRE_MARCA, marca);
        } else {
            switch (marcador.rol) {
                case TOSTADURIA:
                    intento = new Intent(this, DetalleTostaderiaActivity.class);
                    break;
                case PRODUCTOR:
                    intento = new Intent(this, DetalleProductorActivity.class);
                    break;
                default:
                    intento = new Intent(this, DetalleCafeteriaActivity.class);
                    break;
            }
            intento.putExtra(DetalleCafeteriaActivity.EXTRA_ID_ENTIDAD, marcador.idEntidad);
        }
        startActivity(intento);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapa != null) {
            mapa.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mapa != null) {
            mapa.onPause();
        }
    }

    @Override
    protected void onDestroy() {
        if (mapa != null) {
            mapa.onDetach();
            mapa = null;
        }
        super.onDestroy();
    }
}
