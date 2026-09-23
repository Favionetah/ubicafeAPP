package com.ubicafe.app.ui.mapa;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.ubicafe.app.R;
import com.ubicafe.app.datos.RepositorioDatos;
import com.ubicafe.app.modelo.Cafeteria;
import com.ubicafe.app.ui.mapa.MapaLaPazView.AlSeleccionarMarcador;

import java.util.ArrayList;
import java.util.List;

/**
 * MAPA "CERCA DE TI".
 * ---------------------------------------------------------------
 * Por ahora muestra un plano estático estilizado de La Paz con
 * los cafés marcados. La información (nombres y coordenadas) ya
 * vive en los modelos, lista para Google Maps.
 *
 * >>> ESTA ES API DE GOOGLE MAPS <<<
 * ---------------------------------
 * Cuando se pueda conectar Google Maps:
 *   1. Agrega en app/build.gradle la dependencia:
 *        implementation 'com.google.android.gms:play-services-maps:19.0.0'
 *   2. Agrega en AndroidManifest.xml el meta-data con tu API key
 *      (la línea ya está marcada ahí con el comentario "API KEY").
 *   3. Reemplaza el cuerpo de mostrarConGoogleMaps() (abajo) usando
 *      las coordenadas lat/lng que ya están en cada casa.
 * ...y elimina la vista estática. Los datos NO cambian.
 * ---------------------------------------------------------------
 */
public class MapaActivity extends AppCompatActivity {

    private final List<MarcadorMapa> marcadores = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mapa);

        ((TextView) findViewById(R.id.texto_titulo)).setText(getString(R.string.mapa_titulo));

        findViewById(R.id.btn_volver).setOnClickListener(v -> finish());

        construirMarcadores();

        // Lienzo estático (reemplazable por Google Maps en el futuro).
        MapaLaPazView vistaMapa = findViewById(R.id.mapa_vista);
        vistaMapa.setMarcadores(marcadores);

        // Mostrar el primer marcador como "seleccionado" por defecto.
        if (!marcadores.isEmpty()) {
            mostrarInformacion(marcadores.get(0));
        }

        // Al tocar un marcador, actualizar el panel inferior.
        vistaMapa.setListenerAlSeleccionar(marcador -> mostrarInformacion(marcador));
    }

    /** Carga los marcadores a partir de las cafeterías registradas. */
    private void construirMarcadores() {
        marcadores.clear();
        int color = getColor(R.color.verde_oscuro);
        for (Cafeteria cafeteria : RepositorioDatos.obtenerCafeterias()) {
            marcadores.add(new MarcadorMapa(cafeteria.nombre, cafeteria.zona,
                    cafeteria.lat, cafeteria.lng, color));
        }
    }

    /** Pinta la tarjeta inferior con el marcador elegido. */
    private void mostrarInformacion(MarcadorMapa marcador) {
        ((TextView) findViewById(R.id.texto_marcador_titulo)).setText(marcador.nombre);
        ((TextView) findViewById(R.id.texto_marcador_zona))
                .setText(getString(R.string.mapa_leyenda) + " · " + marcador.zona);
    }

    /**
     * >>> ESTO ES PARA API DE GOOGLE MAPS <<<
     * ----------------------------------------
     * Este método se deja como única puerta de conexión al mapa real.
     * Cuando se agregue la dependencia, aquí se crea un SupportMapFragment
     * y se dibujan marcadores usando marcadores[i].lat / .lng.
     */
    public void mostrarConGoogleMaps() {
        // Ejemplo de lo que iría acá (requiere la dependencia play-services-maps):
        //
        // SupportMapFragment fragmento = new SupportMapFragment();
        // getSupportFragmentManager().beginTransaction()
        //         .replace(R.id.mapa_vista, fragmento).commit();
        // fragmento.getMapAsync(googleMap -> {
        //     for (MarcadorMapa marcador : marcadores) {
        //         com.google.android.gms.maps.model.LatLng posicion =
        //                 new com.google.android.gms.maps.model.LatLng(marcador.lat, marcador.lng);
        //         googleMap.addMarker(new com.google.android.gms.maps.model.MarkerOptions()
        //                 .position(posicion).title(marcador.nombre));
        //     }
        // });
    }
}