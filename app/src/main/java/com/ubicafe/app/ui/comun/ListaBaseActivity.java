package com.ubicafe.app.ui.comun;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;

import com.ubicafe.app.R;
import com.ubicafe.app.databinding.ActivityListaRolBinding;
import com.ubicafe.app.util.Texto;
import com.ubicafe.app.util.UiUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * PANTALLA BASE DE LOS CINCO LISTADOS
 * ---------------------------------------------------------------
 * Cafeterías, marcas, tostadurías, productores y otros comparten el
 * mismo esqueleto: cabecera con volver, buscador, fila de filtros,
 * contador de resultados y lista. Este esqueleto vive aquí una sola vez
 * y cada listado solo dice qué datos mostrar y a qué pantalla lleva
 * cada tarjeta.
 *
 * El buscador espera 200 ms antes de filtrar. Sin esa espera, cada
 * tecla recorra las 213 entidades y la lista parpadea mientras se
 * escribe.
 */
public abstract class ListaBaseActivity extends AppCompatActivity {

    /** Filtro a dejar seleccionado al abrir, enviado por otra pantalla. */
    public static final String EXTRA_FILTRO = "filtro_inicial";

    /** Milisegundos de espera antes de filtrar, para no filtrar por tecla. */
    private static final long ESPERA_BUSQUEDA_MS = 200;

    protected ActivityListaRolBinding ui;

    private final List<TextView> chips = new ArrayList<>();
    private final List<String> valoresFiltro = new ArrayList<>();
    private final Handler manejador = new Handler(Looper.getMainLooper());
    private Runnable busquedaPendiente;

    private String filtroSeleccionado = "";

    @Override
    protected void onCreate(@Nullable Bundle estado) {
        super.onCreate(estado);
        ui = ActivityListaRolBinding.inflate(getLayoutInflater());
        setContentView(ui.getRoot());

        ui.barra.textoTitulo.setText(titulo());
        ui.barra.textoSubtitulo.setText(subtitulo());
        ui.barra.btnVolver.setOnClickListener(v -> finish());
        ui.campoBusqueda.setHint(placeholderBusqueda());

        crearFiltros();
        conectarBuscador();
        refrescar();
    }

    // ------------------------------------------------------------------
    // Lo que define cada variante
    // ------------------------------------------------------------------

    protected abstract String titulo();

    protected abstract String subtitulo();

    @StringRes
    protected abstract int placeholderBusqueda();

    /**
     * Los valores de los chips, en el orden en que se muestran, después
     * de "Todas" y de los filtros de agrupación. Si devuelve una lista
     * vacía y no hay agrupación, la fila de filtros se oculta.
     */
    protected abstract List<String> valoresDeFiltros();

    /**
     * Los chips que cambian de qué grupo de valores se muestra. Las
     * tostadurías alternan entre regiones de origen y macrodistritos; los
     * puntos de venta, entre cadenas y macrodistritos. Vacío por
     * defecto: en los listados de cafeterías, marcas y productores no
     * hace falta, porque sus valores son de una sola clase.
     */
    protected List<String> filtrosDeAgrupacion() {
        return Collections.emptyList();
    }

    /**
     * Se llama al tocar un chip. Sirve para cambiar de grupo de valores:
     * quien lo use llama a reconstruirChips() y devuelve true.
     *
     * @return true si los chips se reconstruyeron
     */
    protected boolean alSeleccionarFiltro(String valor) {
        return false;
    }

    /** Etiqueta de un valor de filtro. Por defecto, el valor mismo. */
    protected String etiquetaDeFiltro(String valor) {
        return valor.isEmpty() ? getString(R.string.cafeterias_filtro_todas) : valor;
    }

    /** Vuelve a calcular y pintar la lista con lo que hay Typed. */
    protected abstract void refrescar();

    // ------------------------------------------------------------------
    // Filtros
    // ------------------------------------------------------------------

    /**
     * Vuelve a pintar la fila de chips. Se llama al abrir y cada vez
     * que un listado cambia de grupo de valores, porque los chips
     * anteriores ya no filtran lo mismo.
     */
    protected void reconstruirChips() {
        ui.filaFiltros.removeAllViews();
        chips.clear();
        valoresFiltro.clear();
        valoresFiltro.add("");
        valoresFiltro.addAll(filtrosDeAgrupacion());
        valoresFiltro.addAll(valoresDeFiltros());

        if (valoresFiltro.size() <= 1) {
            ui.scrollFiltros.setVisibility(View.GONE);
            return;
        }
        ui.scrollFiltros.setVisibility(View.VISIBLE);

        // Si el filtro elegido ya no está entre los valores (pasó de
        // "Caranavi" a macrodistritos, por ejemplo), se vuelve a
        // "Todas": dejarlo puesto filtraría por algo que no existe.
        if (!filtroSeleccionado.isEmpty()
                && !contieneIgnorandoTildes(valoresFiltro, filtroSeleccionado)) {
            filtroSeleccionado = "";
        }

        for (int i = 0; i < valoresFiltro.size(); i++) {
            final String valor = valoresFiltro.get(i);
            TextView chip = UiUtils.crearChip(ui.filaFiltros, etiquetaDeFiltro(valor));
            chip.setOnClickListener(v -> {
                filtroSeleccionado = valor;
                if (alSeleccionarFiltro(valor)) {
                    reconstruirChips();
                } else {
                    marcarChips();
                }
                refrescar();
            });
            chips.add(chip);
        }
        marcarChips();
    }

    private void crearFiltros() {
        // Un filtro que no existe en esta variante se ignora en vez de
        // dejar la lista en un estado que el usuario no puede deshacer.
        String filtroInicial = getIntent().getStringExtra(EXTRA_FILTRO);
        if (filtroInicial != null) {
            filtroSeleccionado = filtroInicial;
        }
        reconstruirChips();
    }

    private void marcarChips() {
        for (int i = 0; i < chips.size(); i++) {
            boolean elegido = valoresFiltro.get(i).equals(filtroSeleccionado);
            UiUtils.marcarChipSeleccionado(chips.get(i), elegido);
        }
    }

    private static boolean contieneIgnorandoTildes(List<String> valores, String buscado) {
        String clave = Texto.clave(buscado);
        for (String valor : valores) {
            if (Texto.clave(valor).equals(clave)) {
                return true;
            }
        }
        return false;
    }

    /** El filtro activo. "" significa "sin filtro". */
    protected String filtroActual() {
        return filtroSeleccionado;
    }

    /** El texto escrito, ya normalizado para comparar. */
    protected String consultaActual() {
        return Texto.claveDeConsulta(ui.campoBusqueda.getText().toString());
    }

    // ------------------------------------------------------------------
    // Buscador
    // ------------------------------------------------------------------

    private void conectarBuscador() {
        ui.campoBusqueda.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }

            @Override
            public void afterTextChanged(Editable s) {
                programarRefresco(ESPERA_BUSQUEDA_MS);
            }
        });

        // El botón "buscar" del teclado filtra al instante, sin esperar.
        ui.campoBusqueda.setOnEditorActionListener((vista, accion, evento) -> {
            if (accion == EditorInfo.IME_ACTION_SEARCH) {
                programarRefresco(0);
                return true;
            }
            return false;
        });
    }

    /**
     * Programa el filtrado para dentro de unos milisegundos. Cada tecla
     * nueva cancela la espera anterior, así que solo se filtra una vez
     * cuando la persona deja de escribir. Con 0 se filtra ya.
     */
    private void programarRefresco(long esperaMs) {
        if (busquedaPendiente != null) {
            manejador.removeCallbacks(busquedaPendiente);
        }
        busquedaPendiente = this::refrescar;
        manejador.postDelayed(busquedaPendiente, esperaMs);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (busquedaPendiente != null) {
            manejador.removeCallbacks(busquedaPendiente);
        }
    }

    // ------------------------------------------------------------------
    // Estado de la lista
    // ------------------------------------------------------------------

    /**
     * Pinta el contador y muestra u oculta el estado vacío. Cada listado
     * lo llama al final de refrescar().
     */
    protected void mostrarResultados(int cantidad, boolean hayFiltroActivo) {
        ui.textoConteo.setText(getResources().getQuantityString(
                R.plurals.conteo_varios, cantidad, cantidad));

        mostrarVacio(cantidad == 0, hayFiltroActivo);
    }

    /**
     * Muestra u oculta el estado vacío. El texto cambia según el motivo:
     * si hay un filtro o una búsqueda activa no se encontró nada, y si
     * no hay nada en absoluto, el censo no lo registró.
     */
    protected void mostrarVacio(boolean vacio, boolean hayFiltroActivo) {
        ui.vacio.contenedorVacio.setVisibility(vacio ? View.VISIBLE : View.GONE);
        ui.lista.setVisibility(vacio ? View.GONE : View.VISIBLE);

        if (vacio) {
            ui.vacio.textoVacioTitulo.setText(hayFiltroActivo
                    ? R.string.buscar_sin_resultados_titulo
                    : R.string.lista_vacia_titulo);
            ui.vacio.textoVacioDetalle.setText(hayFiltroActivo
                    ? R.string.buscar_vacio_detalle
                    : R.string.lista_vacia_detalle);
        }
    }
}
