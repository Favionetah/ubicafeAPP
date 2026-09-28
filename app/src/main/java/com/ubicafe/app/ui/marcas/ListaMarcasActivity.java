package com.ubicafe.app.ui.marcas;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.ubicafe.app.R;
import com.ubicafe.app.datos.RepositorioDatos;
import com.ubicafe.app.modelo.Marca;
import com.ubicafe.app.ui.comun.AdaptadorMarca;
import com.ubicafe.app.ui.comun.ListaBaseActivity;
import com.ubicafe.app.util.Texto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * LISTA DE MARCAS (P13)
 * ---------------------------------------------------------------
 * Las 40 cadenas y marcas del censo. El filtro no es geográfico: lo
 * interesante de una marca es si tiene many franchises o una sola, así
 * que se agrupa por cobertura ("La Paz", "Nacional", "Bolivia") en vez
 * de por macrodistrito.
 */
public class ListaMarcasActivity extends ListaBaseActivity {

    private List<String> coberturas = Collections.emptyList();
    private AdaptadorMarca adaptador;

    @Override
    protected void onCreate(@Nullable Bundle estado) {
        coberturas = new ArrayList<>(recolectarCoberturas());
        super.onCreate(estado);
    }

    /** Las coberturas que de verdad respondieron las marcas. */
    private static java.util.TreeSet<String> recolectarCoberturas() {
        java.util.TreeSet<String> encontradas =
                new java.util.TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (Marca marca : RepositorioDatos.obtenerMarcas()) {
            if (marca.detalle != null && !marca.detalle.cobertura.isEmpty()) {
                encontradas.add(marca.detalle.cobertura);
            }
        }
        return encontradas;
    }

    @Override
    protected String titulo() {
        return getString(R.string.marcas_titulo);
    }

    @Override
    protected String subtitulo() {
        return getString(R.string.marcas_subtitulo);
    }

    @Override
    protected int placeholderBusqueda() {
        return R.string.buscar_marca;
    }

    @Override
    protected String etiquetaDeFiltro(String valor) {
        if (valor.equals(FILTRO_TODAS_LAS_MARCAS)) {
            return getString(R.string.marcas_filtro_todas);
        }
        return valor;
    }

    /** Chip que se refiere a la lista completa, no a una cobertura. */
    private static final String FILTRO_TODAS_LAS_MARCAS = "";

    @Override
    protected List<String> valoresDeFiltros() {
        List<String> valores = new ArrayList<>();
        valores.add(FILTRO_TODAS_LAS_MARCAS);
        valores.addAll(coberturas);
        return valores;
    }

    @Override
    protected void refrescar() {
        if (adaptador == null) {
            adaptador = new AdaptadorMarca(Collections.<Marca>emptyList(),
                    marca -> DetalleMarcaActivity.abrir(this, marca));
            ui.lista.setLayoutManager(new LinearLayoutManager(this));
            ui.lista.setAdapter(adaptador);
        }

        String cobertura = filtroActual();
        String consulta = consultaActual();

        List<Marca> resultados = new ArrayList<>();
        for (Marca marca : RepositorioDatos.obtenerMarcas()) {
            if (!cobertura.isEmpty() && !tieneCobertura(marca, cobertura)) {
                continue;
            }
            if (!consulta.isEmpty() && !coincide(marca, consulta)) {
                continue;
            }
            resultados.add(marca);
        }

        adaptador.setItems(resultados);
        int total = resultados.size();
        ui.textoConteo.setText(getResources().getQuantityString(
                R.plurals.conteo_marcas_varios, total, total));
        mostrarVacio(resultados.isEmpty(), !cobertura.isEmpty() || !consulta.isEmpty());
    }

    private static boolean tieneCobertura(Marca marca, String cobertura) {
        return marca.detalle != null
                && Texto.clave(marca.detalle.cobertura).equals(Texto.clave(cobertura));
    }

    private static boolean coincide(Marca marca, String consulta) {
        if (Texto.clave(marca.nombre).contains(consulta)
                || Texto.clave(marca.nota).contains(consulta)) {
            return true;
        }
        for (com.ubicafe.app.modelo.Sucursal sucursal : marca.sucursales) {
            if (Texto.clave(sucursal.nombre).contains(consulta)
                    || Texto.clave(sucursal.direccion).contains(consulta)
                    || Texto.clave(sucursal.macrodistrito).contains(consulta)) {
                return true;
            }
        }
        return marca.detalle != null
                && Texto.clave(marca.detalle.segmento).contains(consulta);
    }
}
