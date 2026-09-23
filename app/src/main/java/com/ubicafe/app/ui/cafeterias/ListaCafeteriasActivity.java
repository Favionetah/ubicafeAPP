package com.ubicafe.app.ui.cafeterias;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ubicafe.app.R;
import com.ubicafe.app.datos.RepositorioDatos;
import com.ubicafe.app.modelo.Cafeteria;
import com.ubicafe.app.util.UiUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * LISTA DE CAFETERÍAS (P6).
 * Muestra todas las cafeterías con búsqueda por nombre y filtro por zona.
 * Recibe opcionalmente un macrodistrito (EXTRA_ZONA) desde la pantalla de
 * inicio para abrir con ese filtro preseleccionado.
 */
public class ListaCafeteriasActivity extends AppCompatActivity {

    /** Zona/macrodistrito a preseleccionar al abrir (ej. "Sur", "Sopocachi"). */
    public static final String EXTRA_ZONA = "zona_inicial";

    // Estado visible para el usuario.
    private final List<Cafeteria> cafeteriasMostradas = new ArrayList<>();
    private AdaptadorCafeteria adaptador;

    // Filtros seleccionados actualmente.
    private String zonaSeleccionada = "";  // "" = todas
    private final List<TextView> chips = new ArrayList<>();

    // Macrodistritos disponibles ("" = Todas, "Cerca" = sin geolocalización).
    private final String[] zonas = {"", "Cerca", "Centro", "Cotahuma", "Sur",
            "Sopocachi", "Miraflores", "San Antonio", "Mallasa"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lista_cafeterias);

        ((TextView) findViewById(R.id.texto_titulo)).setText(getString(R.string.cafeterias_titulo));
        ((TextView) findViewById(R.id.texto_subtitulo))
                .setText(getString(R.string.cafeterias_subtitulo));

        // Botón de volver
        findViewById(R.id.btn_volver).setOnClickListener(v -> finish());

        // Lista
        adaptador = new AdaptadorCafeteria(cafeteriasMostradas);
        RecyclerView lista = findViewById(R.id.lista);
        lista.setLayoutManager(new LinearLayoutManager(this));
        lista.setAdapter(adaptador);

        // Filtros por zona
        crearFiltros();

        // Si vino un macrodistrito desde Inicio, preseleccionarlo.
        String zonaInicial = getIntent().getStringExtra(EXTRA_ZONA);
        if (zonaInicial != null && !zonaInicial.isEmpty()) {
            seleccionarZonaInicial(zonaInicial);
        }

        aplicarFiltros();

        // Búsqueda por nombre
        EditText campoBusqueda = findViewById(R.id.campo_busqueda);
        campoBusqueda.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable s) { }

            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {
                aplicarFiltros();
            }
        });
    }

    /** Crea los chips horizontales de macrodistrito. */
    private void crearFiltros() {
        LinearLayout fila = findViewById(R.id.fila_filtros);
        for (int i = 0; i < zonas.length; i++) {
            String texto = zonas[i].isEmpty() ? getString(R.string.cafeterias_filtro_todas)
                    : zonas[i];
            TextView chip = UiUtils.crearChip(fila, texto);
            final int posicion = i;
            chip.setOnClickListener(v -> {
                zonaSeleccionada = zonas[posicion];
                refrescarChips();
                aplicarFiltros();
            });
            chips.add(chip);
        }
        refrescarChips();
    }

    /** El chip marcado es el de "Todos" salvo que haya otra zona elegida. */
    private void refrescarChips() {
        for (int i = 0; i < chips.size(); i++) {
            boolean seleccionado = zonas[i].equals(zonaSeleccionada);
            UiUtils.marcarChipSeleccionado(chips.get(i), seleccionado);
        }
    }

    /** Preselecciona el macrodistrito recibido desde otra pantalla. */
    private void seleccionarZonaInicial(String zona) {
        for (int i = 0; i < zonas.length; i++) {
            if (zonas[i].equalsIgnoreCase(zona)) {
                zonaSeleccionada = zonas[i];
                refrescarChips();
                return;
            }
        }
        // Zona sin chip propio: se agrega temporalmente como selección.
        zonaSeleccionada = zona;
    }

    /** Aplica simultáneamente el filtro de zona y el texto del buscador. */
    private void aplicarFiltros() {
        // "Cerca" y "Todos" muestran todas las zonas (aún sin geolocalización).
        String zona = (zonaSeleccionada.isEmpty()
                || zonaSeleccionada.equals(getString(R.string.cafeterias_filtro_cerca)))
                ? "" : zonaSeleccionada;

        EditText campo = findViewById(R.id.campo_busqueda);
        String textoBusqueda = campo.getText().toString().toLowerCase().trim();

        List<Cafeteria> resultado = new ArrayList<>();
        for (Cafeteria cafeteria : RepositorioDatos.obtenerCafeteriasPorZona(zona)) {
            if (cafeteria.nombre.toLowerCase().contains(textoBusqueda)) {
                resultado.add(cafeteria);
            }
        }
        adaptador.actualizar(resultado);
        mostrarVacio(resultado.isEmpty());
    }

    /** Muestra u oculta el mensaje de "sin resultados". */
    private void mostrarVacio(boolean vacio) {
        findViewById(R.id.texto_vacio).setVisibility(
                vacio ? View.VISIBLE : View.GONE);
        findViewById(R.id.lista).setVisibility(
                vacio ? View.GONE : View.VISIBLE);
    }
}