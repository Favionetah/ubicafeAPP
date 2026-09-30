package com.ubicafe.app.ui.cafeterias;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.ubicafe.app.R;
import com.ubicafe.app.datos.RepositorioDatos;
import com.ubicafe.app.modelo.Entidad;
import com.ubicafe.app.modelo.Estadisticas;
import com.ubicafe.app.modelo.Rol;
import com.ubicafe.app.ui.comun.AdaptadorEntidad;
import com.ubicafe.app.ui.comun.ListaBaseActivity;
import com.ubicafe.app.util.Texto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * LISTA DE CAFETERÍAS (P6)
 * ---------------------------------------------------------------
 * Los 189 lugares del censo que venden café al público. Muestra también
 * los que además son tostaduría o marca, porque para quien busca un
 * lugar donde tomar un café es lo mismo.
 *
 * El filtro es por macrodistrito y sus valores salen del propio censo,
 * no de una lista escrita a mano: así no aparece un chip que no lleva
 * a ninguna parte.
 */
public class ListaCafeteriasActivity extends ListaBaseActivity {

    /** Alias del nombre que usan otras pantallas para abrir con filtro. */
    public static final String EXTRA_ZONA = EXTRA_FILTRO;

    private AdaptadorEntidad adaptador;

    @Override
    protected String titulo() {
        return getString(R.string.cafeterias_titulo);
    }

    @Override
    protected String subtitulo() {
        return getString(R.string.cafeterias_subtitulo);
    }

    @Override
    protected int placeholderBusqueda() {
        return R.string.buscar_cafeteria;
    }

    private List<String> macrodistritos = Collections.emptyList();

    @Override
    protected void onCreate(@Nullable Bundle estado) {
        // Los macrodistritos se piden antes de llamar a super, porque la
        // base los necesita para armar los chips durante su propio onCreate.
        Estadisticas estadisticas = RepositorioDatos.obtenerEstadisticas();
        if (estadisticas != null) {
            macrodistritos = estadisticas.macrodistritosOrdenados();
        }
        super.onCreate(estado);
    }

    @Override
    protected List<String> valoresDeFiltros() {
        List<String> valores = new ArrayList<>();
        valores.add("");
        valores.addAll(macrodistritos);
        return valores;
    }

    @Override
    protected void refrescar() {
        if (adaptador == null) {
            adaptador = new AdaptadorEntidad(Collections.<Entidad>emptyList(),
                    this::abrirDetalle);
            adaptador.setContexto(this);
            ui.lista.setLayoutManager(new LinearLayoutManager(this));
            ui.lista.setAdapter(adaptador);
        }

        String macrodistrito = filtroActual();
        String consulta = consultaActual();

        List<Entidad> resultados = new ArrayList<>();
        for (Entidad lugar : RepositorioDatos.obtenerEntidadesPorRol(Rol.CAFETERIA)) {
            if (!macrodistrito.isEmpty()
                    && !Texto.clave(lugar.macrodistrito).equals(Texto.clave(macrodistrito))) {
                continue;
            }
            if (!consulta.isEmpty() && !coincide(lugar, consulta)) {
                continue;
            }
            resultados.add(lugar);
        }

        adaptador.setItems(resultados);
        mostrarResultados(resultados.size(), !macrodistrito.isEmpty() || !consulta.isEmpty());
    }

    private static boolean coincide(Entidad lugar, String consulta) {
        return Texto.clave(lugar.nombre).contains(consulta)
                || Texto.clave(lugar.direccion).contains(consulta)
                || Texto.clave(lugar.macrodistrito).contains(consulta)
                || Texto.clave(lugar.marcaAsociada).contains(consulta);
    }

    private void abrirDetalle(Entidad lugar) {
        DetalleCafeteriaActivity.abrir(this, lugar);
    }
}
