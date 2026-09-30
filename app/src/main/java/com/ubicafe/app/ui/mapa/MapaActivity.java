package com.ubicafe.app.ui.mapa;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.ubicafe.app.R;
import com.ubicafe.app.datos.RepositorioDatos;
import com.ubicafe.app.modelo.Entidad;
import com.ubicafe.app.modelo.Rol;
import com.ubicafe.app.ui.cafeterias.DetalleCafeteriaActivity;
import com.ubicafe.app.ui.marcas.DetalleMarcaActivity;
import com.ubicafe.app.ui.productores.DetalleProductorActivity;
import com.ubicafe.app.ui.tostaderias.DetalleTostaderiaActivity;
import com.ubicafe.app.util.Distancia;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

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

    /**
     * Dibuja la ruta hasta este lugar. Viene del botón "Cómo llegar" de
     * la ficha. No se mezclan con EXTRA_CENTRAR_EN a propósito: ver un
     * lugar en el mapa y calcular cómo llegar a él son dos cosas
     * distintas, y quien solo quería ver el pin no quiere un panel de
     * ruta encima.
     */
    public static final String EXTRA_RUTA_A = "ruta_a";

    private final List<MarcadorMapa> marcadores = new ArrayList<>();
    private final List<Marker> marcadoresEnMapa = new ArrayList<>();

    private MapView mapa;
    private Marker pinSeleccionado;
    private MarcadorMapa seleccionado;

    /** El lugar al que se va a calcular la ruta, o null si no hay ruta. */
    private Entidad destinoRuta;
    /** La línea del trayecto, o null si todavía no se ha calculado. */
    private Polyline lineaRuta;
    private Marker pinUsuario;
    /** La espera de la posición, para poder cancelarla al cerrar. */
    private Geolocalizador.Cancelacion peticionUbicacion;

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

        String idRuta = getIntent().getStringExtra(EXTRA_RUTA_A);
        if (idRuta != null) {
            mostrarRuta(idRuta);
            return;
        }

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
        seleccionar(marcadores.get(0), false);

        findViewById(R.id.btn_ver_informacion)
                .setOnClickListener(v -> abrirDetalleDe(seleccionado));
    }

    /**
     * Entra en modo ruta: el destino con su pin, la posición de la
     * persona, el trayecto entre los dos y el panel con distancia y
     * tiempo.
     *
     * Si no sabemos dónde está la persona, se dice en el panel y se
     * deja el pin del local. Se podría abrir el mapa de la aplicación
     * de Maps con la ruta ya hecha, pero eso saca de la app a otra y
     * quien no la tiene instalada se queda sin nada; aquí al menos
     * siempre se ve a dónde ir.
     */
    private void mostrarRuta(String idDestino) {
        destinoRuta = RepositorioDatos.obtenerEntidad(idDestino);
        if (destinoRuta == null || !destinoRuta.tieneCoordenadas()) {
            mostrarAviso("Este lugar no tiene coordenadas, así que no se puede calcular la ruta");
            return;
        }

        pinSeleccionado = new Marker(mapa);
        pinSeleccionado.setPosition(
                new GeoPoint(destinoRuta.lat, destinoRuta.lng));
        pinSeleccionado.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        pinSeleccionado.setIcon(PinesMapa.pinSeleccionado(this,
                MarcadoresMapa.colorDe(destinoRuta.rolPrincipal)));
        pinSeleccionado.setTitle(destinoRuta.nombre);
        mapa.getOverlays().add(pinSeleccionado);

        // El panel de abajo estorba: en modo ruta la información útil
        // está arriba y el botón de abrir la ficha ya no hace falta,
        // porque se viene de esa ficha.
        View panelAbajo = findViewById(R.id.panel_inferior);
        if (panelAbajo != null) {
            panelAbajo.setVisibility(View.GONE);
        }

        android.location.Location ultima = Geolocalizador.ultimaPosicion(this);
        mostrarPanelRuta(destinoRuta.nombre,
                ultima == null ? getString(R.string.ruta_buscando_ubicacion)
                        : getString(R.string.ruta_calculando), "", true);
        mapa.getController().setCenter(
                new GeoPoint(destinoRuta.lat, destinoRuta.lng));

        // Se pide una posición de verdad, no la última guardada: esa
        // puede ser de hace media hora y la ruta saldría desde el sitio
        // equivocado. Mientras llega, el panel dice que está buscando.
        peticionUbicacion = Geolocalizador.pedirPosicionActual(this,
                new Geolocalizador.EscuchaPosicion() {
                    @Override
                    public void conPosicion(android.location.Location posicion) {
                        calcularRutaDesde(posicion);
                    }

                    @Override
                    public void sinPosicion() {
                        // El aviso lleva delante la razón de por qué se
                        // pide la ubicación, incluida la promesa de que no
                        // se guarda: es el único sitio donde alguien va a
                        // leerla.
                        mostrarPanelRuta(destinoRuta.nombre,
                                getString(R.string.ruta_sin_posicion,
                                        getString(R.string.como_llegar_permiso_texto)),
                                "", true);
                    }
                });
    }

    /** Con un punto de partida confirmado, ya se puede pedir el trayecto. */
    private void calcularRutaDesde(android.location.Location posicion) {
        if (isFinishing() || destinoRuta == null) {
            return;
        }
        GeoPoint desde = new GeoPoint(posicion.getLatitude(), posicion.getLongitude());
        agregarPinUsuario(desde);
        mostrarPanelRuta(destinoRuta.nombre, getString(R.string.ruta_calculando), "", true);

        CalculadoraRuta.calcular(desde.getLatitude(), desde.getLongitude(),
                destinoRuta.lat, destinoRuta.lng, this::pintarRuta);
    }

    /** El punto azul de la persona, con el mismo pin que usa el mapa. */
    private void agregarPinUsuario(GeoPoint posicion) {
        pinUsuario = new Marker(mapa);
        pinUsuario.setPosition(posicion);
        pinUsuario.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        pinUsuario.setIcon(PinesMapa.pin(this, ContextCompat.getColor(
                this, R.color.azul_ubicacion)));
        pinUsuario.setTitle(getString(R.string.ruta_origen_desconocido));
        mapa.getOverlays().add(pinUsuario);
    }

    /** Dibuja la línea del trayecto y enquadra origen y destino. */
    private void pintarRuta(Ruta ruta) {
        if (ruta.puntos.size() < 2) {
            return;
        }
        if (lineaRuta != null) {
            mapa.getOverlays().remove(lineaRuta);
        }
        lineaRuta = new Polyline();
        lineaRuta.setPoints(ruta.puntos);
        lineaRuta.setColor(ContextCompat.getColor(this, R.color.azul_ubicacion));
        // setWidth en píxeles, y no en dp: el grosor de una línea se
        // mide en pantalla, no en densidad. Con 5 px se ve una línea
        // fina pero legible sobre las teselas, que es lo que se busca:
        // la ruta se lee de un vistazo, no compite con los pines.
        lineaRuta.setWidth(5);
        // Los puntos de la ruta van debajo de los pines, para que el
        // destino quede tapado por su pin y se vea dónde acaba.
        mapa.getOverlays().add(0, lineaRuta);
        mapa.invalidate();

        // ruta_distancia ya dice "A 1,2 km en coche", así que aquí no se
        // repite el modo: pondría "en coche · coche".
        String resumen = getString(R.string.ruta_distancia,
                Distancia.formatear(ruta.kilometros()));
        if (ruta.duracionSegundos > 0) {
            resumen = resumen + " · " + getString(R.string.ruta_duracion,
                    pluralMinutos(ruta.minutos()));
        }
        // El aviso depende de por qué se cayó al plan B: sin cobertura
        // no es lo mismo que un servicio caído, y la diferencia le dice
        // a quien lee si tiene que revisar sus datos o solo esperar.
        String aviso = "";
        if (ruta.origen == Ruta.Origen.SIN_CONEXION) {
            aviso = getString(R.string.ruta_sin_conexion);
        } else if (ruta.origen == Ruta.Origen.ERROR_SERVICIO) {
            aviso = getString(R.string.ruta_error);
        }
        mostrarPanelRuta(destinoRuta.nombre, resumen, aviso, true);

        enCuadrarRuta(ruta);
    }

    /**
     * "5 minutos" o "1 minuto", en plural correcto. Se hace aquí y no
     * con un plurail porque el dato no sale de un recurso sino de un
     * cálculo, y el recurso solo aceptaría un entero.
     */
    private String pluralMinutos(int minutos) {
        return getResources().getQuantityString(R.plurals.ruta_minutos, minutos, minutos);
    }

    /**
     * Encuadra el trayecto entero. El zoom se calcula con el margen del
     * recuadro y un tope: si el trayecto es de 200 metros, un zoom de
     * ciudad dejaría la ruta como una raya; y si son 30 kilómetros, un
     * zoom de calle no la mostraría entera.
     */
    private void enCuadrarRuta(Ruta ruta) {
        double latMin = Double.MAX_VALUE;
        double latMax = -Double.MAX_VALUE;
        double lngMin = Double.MAX_VALUE;
        double lngMax = -Double.MAX_VALUE;
        for (GeoPoint punto : ruta.puntos) {
            latMin = Math.min(latMin, punto.getLatitude());
            latMax = Math.max(latMax, punto.getLatitude());
            lngMin = Math.min(lngMin, punto.getLongitude());
            lngMax = Math.max(lngMax, punto.getLongitude());
        }
        double centroLat = (latMin + latMax) / 2;
        double centroLng = (lngMin + lngMax) / 2;

        // 1,8 de margen: el 0,8 de cada lado evita que los extremos
        // queden pegados al borde de la pantalla.
        double zoom = zoomPara((latMax - latMin) * 1.8, (lngMax - lngMin) * 1.8, centroLat);
        mapa.getController().setZoom(zoom);
        mapa.getController().setCenter(new GeoPoint(centroLat, centroLng));
    }

    /**
     * El zoom que hace caber un recuadro de esas medidas. Es el mismo
     * criterio que usan los mapas: los grados de longitud se estiran
     * por el coseno de la latitud, porque cerca de los polos un grado de
     * longitud es mucho más corto que uno de latitud. Sin esto, el
     * trayecto en La Paz saldría descentrado.
     */
    private double zoomPara(double gradosLat, double gradosLng, double latCentro) {
        double coseno = Math.cos(Math.toRadians(latCentro));
        if (coseno < 0.1) {
            coseno = 0.1;
        }
        double mayor = Math.max(gradosLat, gradosLng * coseno);
        if (mayor <= 0) {
            return 16.0;
        }
        // 256 px de tesela: 2^zoom * 256 cubre el recuadro en grados.
        double zoom = Math.log(256.0 / (mayor * 360.0)) / Math.log(2.0);
        // Entre 12 y 17: más cerrado que 12 y la ruta no se lee, más
        // abierto que 17 y aparece media ciudad.
        return Math.max(12.0, Math.min(17.0, zoom));
    }

    /** Rellena el panel de arriba con el destino, el resumen y el aviso. */
    private void mostrarPanelRuta(String destino, String resumen, String aviso,
                                  boolean visible) {
        View panel = findViewById(R.id.panel_ruta);
        panel.setVisibility(visible ? View.VISIBLE : View.GONE);
        if (!visible) {
            return;
        }
        ((TextView) findViewById(R.id.texto_ruta_destino))
                .setText(getString(R.string.ruta_titulo, destino));
        ((TextView) findViewById(R.id.texto_ruta_resumen)).setText(resumen);

        TextView textoAviso = findViewById(R.id.texto_ruta_aviso);
        textoAviso.setText(aviso);
        textoAviso.setVisibility(aviso.isEmpty() ? View.GONE : View.VISIBLE);
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
            marcador.setIcon(PinesMapa.pin(this, dato.color));
            marcador.setTitle(dato.nombre);
            marcador.setRelatedObject(dato);
            marcador.setOnMarkerClickListener((pin, overlay) -> {
                seleccionar((MarcadorMapa) pin.getRelatedObject(), true);
                return true;
            });
            mapa.getOverlays().add(marcador);
            marcadoresEnMapa.add(marcador);
        }
    }

    /**
     * Deja un marcador como el elegido: pin con halo, panel de abajo con
     * sus datos y, si viene de un toque, el mapa desplazado hasta él. Al
     * abrir la pantalla se elige el primero sin desplazar, porque de eso
     * ya se encarga centrar().
     */
    private void seleccionar(MarcadorMapa marcador, boolean desplazar) {
        seleccionado = marcador;
        resaltar(pinDeMarcador(marcador));
        mostrarInformacion(marcador);
        if (desplazar) {
            mapa.getController().animateTo(new GeoPoint(marcador.lat, marcador.lng));
        }
    }

    /** El pin del mapa que corresponde a un marcador. */
    private Marker pinDeMarcador(MarcadorMapa marcador) {
        for (int i = 0; i < marcadores.size(); i++) {
            if (marcadores.get(i) == marcador) {
                return marcadoresEnMapa.get(i);
            }
        }
        return null;
    }

    /**
     * Pone el halo en un pin y se lo quita al anterior. Acepta null para
     * dejar el mapa sin ningún pin resaltado.
     */
    private void resaltar(@Nullable Marker pin) {
        if (pin == pinSeleccionado) {
            return;
        }
        if (pinSeleccionado != null) {
            pinSeleccionado.setIcon(PinesMapa.pin(this,
                    ((MarcadorMapa) pinSeleccionado.getRelatedObject()).color));
        }
        pinSeleccionado = pin;
        if (pin != null) {
            pin.setIcon(PinesMapa.pinSeleccionado(this,
                    ((MarcadorMapa) pin.getRelatedObject()).color));
        }
        if (mapa != null) {
            mapa.invalidate();
        }
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
        // Sin esto, la petición de posición seguiría viva con la
        // pantalla cerrada, despertando el GPS para nada.
        if (peticionUbicacion != null) {
            peticionUbicacion.cancelar();
            peticionUbicacion = null;
        }
        if (mapa != null) {
            mapa.onDetach();
            mapa = null;
        }
        pinSeleccionado = null;
        seleccionado = null;
        super.onDestroy();
    }
}
