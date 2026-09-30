package com.ubicafe.app.ui.tostaderias;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.ubicafe.app.R;
import com.ubicafe.app.datos.RepositorioDatos;
import com.ubicafe.app.modelo.Entidad;
import com.ubicafe.app.modelo.Rol;
import com.ubicafe.app.ui.comun.AdaptadorEntidad;
import com.ubicafe.app.ui.comun.ListaBaseActivity;
import com.ubicafe.app.util.Texto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;

/**
 * LISTA DE TOSTADURÍAS (P6)
 * ---------------------------------------------------------------
 * Los 13 lugares del censo que tuestan café. También entra aquí el que
 * además tiene cafetería, porque el interés es el mismo: dónde se tuesta
 * el grano que se compra después.
 *
 * El filtro es por región de origen del café, no por macrodistrito: son
 * cosas distintas. La región dice de dónde viene el grano (Caranavi,
 * Yungas, Tarija); el macrodistrito dice en qué parte de la ciudad está
 * el local.
 */
public class ListaTostaderiasActivity extends ListaBaseActivity {

    /** Modo en que se leen los valores de los chips. */
    private static final String MODO_REGIONES = "__regiones__";
    private static final String MODO_MACRODISTRITOS = "__macrodistritos__";

    private List<String> regiones = Collections.emptyList();
    private AdaptadorEntidad adaptador;
    private String modo = MODO_MACRODISTRITOS;

    @Override
    protected void onCreate(@Nullable Bundle estado) {
        regiones = new ArrayList<>(recolectarRegiones());
        super.onCreate(estado);
    }

    /**
     * Las regiones que de verdad respondió alguna tostaduría. Se leen de
     * los datos y no de una constante para no ofrecer un filtro vacío.
     */
    private static TreeSet<String> recolectarRegiones() {
        TreeSet<String> encontradas = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (Entidad lugar : RepositorioDatos.obtenerEntidadesPorRol(Rol.TOSTADURIA)) {
            if (lugar.detalleTostaderia == null) {
                continue;
            }
            for (String region : lugar.detalleTostaderia.regionesOrigenComoLista()) {
                if (!region.isEmpty()) {
                    encontradas.add(region);
                }
            }
        }
        return encontradas;
    }

    @Override
    protected String titulo() {
        return getString(R.string.tostaderias_titulo);
    }

    @Override
    protected String subtitulo() {
        return getString(R.string.tostaderias_subtitulo);
    }

    @Override
    protected int placeholderBusqueda() {
        return R.string.buscar_tostaderia;
    }

    @Override
    protected String etiquetaDeFiltro(String valor) {
        if (valor.isEmpty()) {
            return getString(R.string.cafeterias_filtro_todas);
        }
        if (valor.equals(MODO_REGIONES)) {
            return getString(R.string.tostaderias_filtro_regiones);
        }
        if (valor.equals(MODO_MACRODISTRITOS)) {
            return getString(R.string.puntos_filtro_zonas);
        }
        return valor;
    }

    @Override
    protected List<String> filtrosDeAgrupacion() {
        return Arrays.asList(MODO_REGIONES, MODO_MACRODISTRITOS);
    }

    /**
     * Tocar "Regiones" o "Macrodistritos" cambia de qué clase de
     * valores muestran los chips siguientes. Se reconstruye la fila
     * porque los valores anteriores ya no aplican.
     */
    @Override
    protected boolean alSeleccionarFiltro(String valor) {
        if (valor.equals(MODO_REGIONES)) {
            modo = MODO_REGIONES;
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
        return modo.equals(MODO_REGIONES) ? new ArrayList<>(regiones) : macrodistritosConTostaderia();
    }

    /**
     * Solo las zonas donde hay alguna tostaduría. Un chip de macrodistrito
     * que sale a cero resultados parece un dato mal escrito, y de los 20
     * macrodistritos solo unos pocos tienen una.
     */
    private static List<String> macrodistritosConTostaderia() {
        TreeSet<String> zonas = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (Entidad lugar : RepositorioDatos.obtenerEntidadesPorRol(Rol.TOSTADURIA)) {
            if (!lugar.macrodistrito.isEmpty()) {
                zonas.add(lugar.macrodistrito);
            }
        }
        return new ArrayList<>(zonas);
    }

    @Override
    protected void refrescar() {
        if (adaptador == null) {
            adaptador = new AdaptadorEntidad(Collections.<Entidad>emptyList(),
                    lugar -> DetalleTostaderiaActivity.abrir(this, lugar));
            adaptador.setContexto(this);
            ui.lista.setLayoutManager(new LinearLayoutManager(this));
            ui.lista.setAdapter(adaptador);
        }

        String seleccion = filtroActual();
        String consulta = consultaActual();

        // Solo se ofrecen macrodistritos donde haya alguna tostaduría:
        // un chip de zona con cero resultados parece un dato mal escrito.
        List<Entidad> resultados = new ArrayList<>();
        for (Entidad lugar : RepositorioDatos.obtenerEntidadesPorRol(Rol.TOSTADURIA)) {
            if (!coincideFiltro(lugar, seleccion) || !coincideConsulta(lugar, consulta)) {
                continue;
            }
            resultados.add(lugar);
        }

        adaptador.setItems(resultados);
        mostrarResultados(resultados.size(), !seleccion.isEmpty() || !consulta.isEmpty());
    }

    /**
     * Igual que en los puntos de venta: la región se compara solo si el
     * modo es regiones y el macrodistrito solo si el modo es zonas.
     */
    private boolean coincideFiltro(Entidad lugar, String seleccion) {
        if (seleccion.isEmpty()) {
            return true;
        }
        if (seleccion.equals(MODO_REGIONES) || seleccion.equals(MODO_MACRODISTRITOS)) {
            return true;
        }
        if (modo.equals(MODO_REGIONES)) {
            return lugar.detalleTostaderia != null
                    && lugar.detalleTostaderia.regionesOrigenComoLista().stream()
                    .anyMatch(region -> Texto.clave(region).equals(Texto.clave(seleccion)));
        }
        return Texto.clave(lugar.macrodistrito).equals(Texto.clave(seleccion));
    }

    private static boolean coincideConsulta(Entidad lugar, String consulta) {
        if (consulta.isEmpty()) {
            return true;
        }
        if (Texto.clave(lugar.nombre).contains(consulta)
                || Texto.clave(lugar.direccion).contains(consulta)
                || Texto.clave(lugar.macrodistrito).contains(consulta)) {
            return true;
        }
        return lugar.detalleTostaderia != null
                && (Texto.clave(lugar.detalleTostaderia.regionesOrigen).contains(consulta)
                || Texto.clave(lugar.detalleTostaderia.tiposTueste).contains(consulta));
    }
}
