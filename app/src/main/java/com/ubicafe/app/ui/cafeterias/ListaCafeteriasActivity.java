package com.ubicafe.app.ui.cafeterias;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
 */
public class ListaCafeteriasActivity extends AppCompatActivity {

    // Estado visible para el usuario.
    private final List<Cafeteria> cafeteriasMostradas = new ArrayList<>();
    private AdaptadorCafeteria adaptador;

    // Filtros seleccionados actualmente.
    private String zonaSeleccionada = "";  // "" = todas
    private final List<TextView> chips = new ArrayList<>();

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

    /** Crea los chips horizontales: Todos, Cerca, Centro, Cotahuma, Sur. */
    private void crearFiltros() {
        int[] zonas = {R.string.cafeterias_filtro_todas,
                R.string.cafeterias_filtro_cerca,
                R.string.cafeterias_filtro_centro,
                R.string.cafeterias_filtro_cotahuma,
                R.string.cafeterias_filtro_sur};
        LinearLayout fila = findViewById(R.id.fila_filtros);

        for (int i = 0; i < zonas.length; i++) {
            TextView chip = UiUtils.crearChip(fila, getString(zonas[i]));
            final int posicion = i;
            chip.setOnClickListener(v -> {
                zonaSeleccionada = posicion == 0 ? "" : getString(zonas[posicion]);
                refrescarChips();
                aplicarFiltros();
            });
            chips.add(chip);
        }
        refrescarChips();
    }

    private void refrescarChips() {
        for (int i = 0; i < chips.size(); i++) {
            boolean seleccionado = (i == 0 && zonaSeleccionada.isEmpty())
                    || (i > 0 && zonaSeleccionada.equals(getString(
                    i == 1 ? R.string.cafeterias_filtro_cerca
                            : i == 2 ? R.string.cafeterias_filtro_centro
                            : i == 3 ? R.string.cafeterias_filtro_cotahuma
                            : R.string.cafeterias_filtro_sur)));
            UiUtils.marcarChipSeleccionado(chips.get(i), seleccionado);
        }
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
    }
}