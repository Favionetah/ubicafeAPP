package com.ubicafe.app.ui.productores;

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
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;

/**
 * LISTA DE PRODUCTORES (P6)
 * ---------------------------------------------------------------
 * Los 12 lugares del censo que tienen cafetales. Un productor no está
 * en un macrodistrito: vive en su comunidad, fuera de la ciudad, así que
 * el filtro es por comunidad o municipio y no por zona urbana. Filtrar
 * por macrodistrito aquí sería mezclar dos cosas distintas.
 */
public class ListaProductoresActivity extends ListaBaseActivity {

    private List<String> comunidades = Collections.emptyList();
    private AdaptadorEntidad adaptador;

    @Override
    protected void onCreate(@Nullable Bundle estado) {
        comunidades = new ArrayList<>(recolectarComunidades());
        super.onCreate(estado);
    }

    /** Las comunidades y municipios que de verdad respondieron. */
    private static TreeSet<String> recolectarComunidades() {
        TreeSet<String> encontradas = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (Entidad lugar : RepositorioDatos.obtenerEntidadesPorRol(Rol.PRODUCTOR)) {
            if (lugar.detalleProductor == null) {
                continue;
            }
            addSeparado(encontradas, lugar.detalleProductor.comunidadMunicipio);
        }
        return encontradas;
    }

    /** El censo guardaba "Coroico / Ichu" en un solo campo: se parte. */
    private static void addSeparado(TreeSet<String> destino, String campo) {
        for (String parte : campo.split("[,;/]| - ")) {
            String limpia = parte.trim();
            if (!limpia.isEmpty()) {
                destino.add(limpia);
            }
        }
    }

    @Override
    protected String titulo() {
        return getString(R.string.productores_titulo);
    }

    @Override
    protected String subtitulo() {
        return getString(R.string.productores_subtitulo);
    }

    @Override
    protected int placeholderBusqueda() {
        return R.string.buscar_productor;
    }

    @Override
    protected List<String> valoresDeFiltros() {
        List<String> valores = new ArrayList<>();
        valores.add("");
        valores.addAll(comunidades);
        return valores;
    }

    @Override
    protected void refrescar() {
        if (adaptador == null) {
            adaptador = new AdaptadorEntidad(Collections.<Entidad>emptyList(),
                    lugar -> DetalleProductorActivity.abrir(this, lugar));
            ui.lista.setLayoutManager(new LinearLayoutManager(this));
            ui.lista.setAdapter(adaptador);
        }

        String comunidad = filtroActual();
        String consulta = consultaActual();

        List<Entidad> resultados = new ArrayList<>();
        for (Entidad lugar : RepositorioDatos.obtenerEntidadesPorRol(Rol.PRODUCTOR)) {
            if (!comunidad.isEmpty() && !perteneceAComunidad(lugar, comunidad)) {
                continue;
            }
            if (!consulta.isEmpty() && !coincide(lugar, consulta)) {
                continue;
            }
            resultados.add(lugar);
        }

        adaptador.setItems(resultados);
        mostrarResultados(resultados.size(), !comunidad.isEmpty() || !consulta.isEmpty());
    }

    private static boolean perteneceAComunidad(Entidad lugar, String comunidad) {
        if (lugar.detalleProductor == null) {
            return false;
        }
        return Texto.clave(lugar.detalleProductor.comunidadMunicipio)
                .equals(Texto.clave(comunidad));
    }

    private static boolean coincide(Entidad lugar, String consulta) {
        if (Texto.clave(lugar.nombre).contains(consulta)
                || Texto.clave(lugar.direccion).contains(consulta)) {
            return true;
        }
        return lugar.detalleProductor != null
                && (Texto.clave(lugar.detalleProductor.especies).contains(consulta)
                || Texto.clave(lugar.detalleProductor.variedades).contains(consulta)
                || Texto.clave(lugar.detalleProductor.comunidadMunicipio).contains(consulta));
    }
}
