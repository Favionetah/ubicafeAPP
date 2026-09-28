package com.ubicafe.app.ui.puntosventa;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.ubicafe.app.R;
import com.ubicafe.app.datos.RepositorioDatos;
import com.ubicafe.app.modelo.Sucursal;
import com.ubicafe.app.ui.comun.AdaptadorSucursal;
import com.ubicafe.app.ui.comun.ListaBaseActivity;
import com.ubicafe.app.ui.mapa.MapaActivity;
import com.ubicafe.app.ui.marcas.DetalleMarcaActivity;
import com.ubicafe.app.util.Texto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;

/**
 * PUNTOS DE VENTA (P12)
 * ---------------------------------------------------------------
 * Los 78 locales que el censo registró, no las marcas. La diferencia
 * importa: hay 40 marcas censadas pero solo 78 locales, así que la
 * lista de marcas y la de locales no son la misma pantalla.
 *
 * El filtro es por marca, porque "dónde compro Typica" es una pregunta
 * por cadena, y por macrodistrito, porque "dónde me queda cerca" es
 * una pregunta por zona.
 */
public class PuntosVentaActivity extends ListaBaseActivity {

    /** Modo en que se leen los valores de los chips. */
    private static final String MODO_MARCAS = "__marcas__";
    private static final String MODO_MACRODISTRITOS = "__macrodistritos__";

    private List<String> marcas = Collections.emptyList();
    private AdaptadorSucursal adaptador;
    private String modo = MODO_MARCAS;

    @Override
    protected void onCreate(@Nullable Bundle estado) {
        marcas = new ArrayList<>(recolectarMarcas());
        super.onCreate(estado);

        int total = RepositorioDatos.obtenerSucursales().size();
        ui.barra.textoSubtitulo.setText(getResources().getQuantityString(
                R.plurals.puntos_subtitulo, total, total));
    }

    /** Las cadenas que de verdad tienen al menos un local censado. */
    private static TreeSet<String> recolectarMarcas() {
        TreeSet<String> encontradas = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (Sucursal sucursal : RepositorioDatos.obtenerSucursales()) {
            if (!sucursal.marca.isEmpty()) {
                encontradas.add(sucursal.marca);
            }
        }
        return encontradas;
    }

    @Override
    protected String titulo() {
        return getString(R.string.puntos_titulo);
    }

    @Override
    protected String subtitulo() {
        return "";
    }

    @Override
    protected int placeholderBusqueda() {
        return R.string.buscar_punto_venta;
    }

    @Override
    protected String etiquetaDeFiltro(String valor) {
        if (valor.isEmpty()) {
            return getString(R.string.cafeterias_filtro_todas);
        }
        if (valor.equals(MODO_MARCAS)) {
            return getString(R.string.puntos_filtro_marcas);
        }
        if (valor.equals(MODO_MACRODISTRITOS)) {
            return getString(R.string.puntos_filtro_zonas);
        }
        return valor;
    }

    @Override
    protected List<String> filtrosDeAgrupacion() {
        return Arrays.asList(MODO_MARCAS, MODO_MACRODISTRITOS);
    }

    /**
     * Los chips siguientes muestran cadenas o macrodistritos según el
     * que se toque. Se reconstruye la fila porque los valores anteriores
     * dejan de aplicar.
     */
    @Override
    protected boolean alSeleccionarFiltro(String valor) {
        if (valor.equals(MODO_MARCAS)) {
            modo = MODO_MARCAS;
            return true;
        }
        if (valor.equals(MODO_MACRODISTRITOS)) {
            modo = MODO_MACRODISTRITOS;
            return true;
        }
        return false;
    }

    @Override
    protected List<String> valoresDeFiltros() {
        return modo.equals(MODO_MARCAS) ? new ArrayList<>(marcas) : zonasConLocales();
    }

    /**
     * Solo los macrodistritos con algún local censado. De los 20 que hay
     * en el censo, la mayoría no tiene ninguno, y un chip que siempre
     * sale a cero parece un dato mal escrito.
     */
    private static List<String> zonasConLocales() {
        TreeSet<String> zonas = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (Sucursal sucursal : RepositorioDatos.obtenerSucursales()) {
            if (!sucursal.macrodistrito.isEmpty()) {
                zonas.add(sucursal.macrodistrito);
            }
        }
        return new ArrayList<>(zonas);
    }

    @Override
    protected void refrescar() {
        if (adaptador == null) {
            adaptador = new AdaptadorSucursal(Collections.<Sucursal>emptyList(),
                    this::abrirMarcaDeLaSucursal);
            ui.lista.setLayoutManager(new LinearLayoutManager(this));
            ui.lista.setAdapter(adaptador);
        }

        String seleccion = filtroActual();
        String consulta = consultaActual();

        List<Sucursal> resultados = new ArrayList<>();
        for (Sucursal sucursal : RepositorioDatos.obtenerSucursales()) {
            if (!coincideFiltro(sucursal, seleccion) || !coincideConsulta(sucursal, consulta)) {
                continue;
            }
            resultados.add(sucursal);
        }

        adaptador.setItems(resultados);
        mostrarResultados(resultados.size(), !seleccion.isEmpty() || !consulta.isEmpty());
    }

    /**
     * Un valor se compara solo contra el campo del modo activo. Si no,
     * una cadena llamada igual que una zona filtraría por ambas, y
     * "Centro" con la lista en modo marcas mostraría locales de otras.
     */
    private boolean coincideFiltro(Sucursal sucursal, String seleccion) {
        if (seleccion.isEmpty() || seleccion.equals(MODO_MARCAS)
                || seleccion.equals(MODO_MACRODISTRITOS)) {
            return true;
        }
        return modo.equals(MODO_MARCAS)
                ? Texto.clave(sucursal.marca).equals(Texto.clave(seleccion))
                : Texto.clave(sucursal.macrodistrito).equals(Texto.clave(seleccion));
    }

    private static boolean coincideConsulta(Sucursal sucursal, String consulta) {
        return consulta.isEmpty()
                || Texto.clave(sucursal.nombre).contains(consulta)
                || Texto.clave(sucursal.marca).contains(consulta)
                || Texto.clave(sucursal.direccion).contains(consulta)
                || Texto.clave(sucursal.macrodistrito).contains(consulta);
    }

    /**
     * Tocar un local abre la ficha de la cadena a la que pertenece, que
     * es donde están el resto de sus locales y el botón de verlos en el
     * mapa. Un local suelto no tiene ficha propia en el censo.
     */
    private void abrirMarcaDeLaSucursal(Sucursal sucursal) {
        if (sucursal.marca.isEmpty()) {
            return;
        }
        Intent intento = new Intent(this, DetalleMarcaActivity.class);
        intento.putExtra(DetalleMarcaActivity.EXTRA_NOMBRE_MARCA, sucursal.marca);
        startActivity(intento);
    }

    /** Abre el mapa con el filtro de una cadena. */
    public static void abrirEnMapa(android.app.Activity origen, String marca) {
        Intent intento = new Intent(origen, MapaActivity.class);
        if (marca != null && !marca.isEmpty()) {
            intento.putExtra(MapaActivity.EXTRA_FILTRO_MARCA, marca);
        }
        origen.startActivity(intento);
    }
}
