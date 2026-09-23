package com.ubicafe.app.ui.mapa;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.ubicafe.app.R;
import com.ubicafe.app.datos.RepositorioDatos;
import com.ubicafe.app.modelo.Cafeteria;
import com.ubicafe.app.modelo.PuntoVenta;
import com.ubicafe.app.ui.cafeterias.DetalleCafeteriaActivity;
import com.ubicafe.app.ui.marcas.DetalleMarcaActivity;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.util.ArrayList;
import java.util.List;

/**
 * MAPA "CERCA DE TI" (pantalla completa).
 * Mapa real de OpenStreetMap (osmdroid). Según cómo se abrió:
 *  - desde "Cómo llegar"  → centra en esa cafetería (EXTRA de DetalleCafeteria).
 *  - desde una marca      → muestra los puntos de venta de esa marca
 *                           (EXTRA de DetalleMarca / Puntos de venta).
 *  - sin extras           → todas las cafeterías.
 */
public class MapaActivity extends AppCompatActivity {

    private final List<MarcadorMapa> marcadores = new ArrayList<>();
    private final List<Marker> marcadoresEnMapa = new ArrayList<>();

    private MapView mapa;
    private MarcadorMapa seleccionado;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ConfiguracionMapa.inicializar(this);
        setContentView(R.layout.activity_mapa);

        ((TextView) findViewById(R.id.texto_titulo)).setText(getString(R.string.mapa_titulo));

        findViewById(R.id.btn_volver).setOnClickListener(v -> finish());

        mapa = findViewById(R.id.mapa_vista);
        mapa.setMultiTouchControls(true);

        String nombreCafeteria = getIntent().getStringExtra(
                DetalleCafeteriaActivity.EXTRA_NOMBRE_CAFETERIA);
        String nombreMarca = getIntent().getStringExtra(
                DetalleMarcaActivity.EXTRA_NOMBRE_MARCA);

        construirSegunContexto(nombreCafeteria, nombreMarca);
        desplegarMarcadores();

        if (marcadores.isEmpty()) {
            return;
        }

        // Centro y zoom según el contexto.
        if (nombreCafeteria != null) {
            mapa.getController().setZoom(15.0);
            mapa.getController().setCenter(new GeoPoint(marcadores.get(0).lat,
                    marcadores.get(0).lng));
        } else {
            mapa.getController().setZoom(13.0);
            mapa.getController().setCenter(new GeoPoint(ConfiguracionMapa.LAT_LA_PAZ,
                    ConfiguracionMapa.LNG_LA_PAZ));
        }

        mostrarInformacion(marcadores.get(0));

        findViewById(R.id.btn_ver_informacion)
                .setOnClickListener(v -> abrirDetalleDe(seleccionado));
    }

    /** Carga los puntos según el origen de la pantalla. */
    private void construirSegunContexto(String nombreCafeteria, String nombreMarca) {
        marcadores.clear();

        if (nombreCafeteria != null) {
            Cafeteria cafeteria = RepositorioDatos.obtenerCafeteria(nombreCafeteria);
            if (cafeteria != null) {
                int color = getColor(R.color.verde_oscuro);
                marcadores.add(new MarcadorMapa(cafeteria.nombre, cafeteria.zona,
                        cafeteria.lat, cafeteria.lng, MarcadorMapa.TIPO_CAFETERIA,
                        color, cafeteria.nombre));
            }
            return;
        }

        if (nombreMarca != null) {
            int color = getColor(R.color.caramelo_claro);
            for (PuntoVenta punto : RepositorioDatos.obtenerPuntosDeVentaDe(nombreMarca)) {
                marcadores.add(new MarcadorMapa(punto.marca + " · " + punto.local,
                        punto.barrio, punto.lat, punto.lng,
                        MarcadorMapa.TIPO_PUNTO_VENTA, color, punto.marca));
            }
            ((TextView) findViewById(R.id.texto_subtitulo))
                    .setText(nombreMarca + " · " + getString(R.string.puntos_subtitulo));
            return;
        }

        int color = getColor(R.color.verde_oscuro);
        for (Cafeteria cafeteria : RepositorioDatos.obtenerCafeterias()) {
            marcadores.add(new MarcadorMapa(cafeteria.nombre, cafeteria.zona,
                    cafeteria.lat, cafeteria.lng, MarcadorMapa.TIPO_CAFETERIA,
                    color, cafeteria.nombre));
        }
    }

    /** Coloca el pin de cada punto sobre el mapa. */
    private void desplegarMarcadores() {
        for (MarcadorMapa dato : marcadores) {
            Marker marcador = new Marker(mapa);
            marcador.setPosition(new GeoPoint(dato.lat, dato.lng));
            marcador.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            marcador.setIcon(pinDeColor(dato.color));
            marcador.setRelatedObject(dato);
            marcador.setOnMarkerClickListener((mk, overlay) -> {
                seleccionado = (MarcadorMapa) mk.getRelatedObject();
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
        android.graphics.drawable.Drawable teñido =
                androidx.core.graphics.drawable.DrawableCompat.wrap(pin).mutate();
        androidx.core.graphics.drawable.DrawableCompat.setTint(teñido, color);
        return teñido;
    }

    /** Pinta la tarjeta inferior con el punto elegido. */
    private void mostrarInformacion(MarcadorMapa marcador) {
        ((TextView) findViewById(R.id.texto_marcador_titulo)).setText(marcador.nombre);
        ((TextView) findViewById(R.id.texto_marcador_zona))
                .setText(marcador.tipo + " · " + marcador.zona);
    }

    /** Abre el detalle del punto según su tipo. */
    private void abrirDetalleDe(MarcadorMapa marcador) {
        if (marcador == null) {
            return;
        }
        Intent intento;
        if (marcador.tipo.equals(MarcadorMapa.TIPO_PUNTO_VENTA)) {
            intento = new Intent(this, DetalleMarcaActivity.class);
            intento.putExtra(DetalleMarcaActivity.EXTRA_NOMBRE_MARCA, marcador.nombreDetalle);
        } else {
            intento = new Intent(this, DetalleCafeteriaActivity.class);
            intento.putExtra(DetalleCafeteriaActivity.EXTRA_NOMBRE_CAFETERIA,
                    marcador.nombreDetalle);
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