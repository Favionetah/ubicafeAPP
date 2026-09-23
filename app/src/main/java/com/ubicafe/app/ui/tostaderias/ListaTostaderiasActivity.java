package com.ubicafe.app.ui.tostaderias;

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
import com.ubicafe.app.modelo.Tostaderia;
import com.ubicafe.app.util.UiUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * LISTA DE TOSTADURÍAS.
 * Muestra las tostadurías con búsqueda por nombre y filtro por región
 * (La Paz | Yungas | Tarija).
 */
public class ListaTostaderiasActivity extends AppCompatActivity {

    private final List<Tostaderia> tostaduriasMostradas = new ArrayList<>();
    private AdaptadorTostaderia adaptador;

    private String regionSeleccionada = ""; // "" = todas
    private final List<TextView> chips = new ArrayList<>();

    // Regiones disponibles como filtros (en el mismo orden en que se muestran).
    private final String[] regiones = {"", "La Paz", "Yungas", "Tarija"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lista_tostaderias);

        ((TextView) findViewById(R.id.texto_titulo))
                .setText(getString(R.string.categoria_tostaderias));

        findViewById(R.id.btn_volver).setOnClickListener(v -> finish());

        adaptador = new AdaptadorTostaderia(tostaduriasMostradas);
        RecyclerView lista = findViewById(R.id.lista);
        lista.setLayoutManager(new LinearLayoutManager(this));
        lista.setAdapter(adaptador);

        crearFiltros();
        aplicarFiltros();

        EditText campoBusqueda = findViewById(R.id.campo_busqueda);
        campoBusqueda.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable s) { }

            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {
                aplicarFiltros();
            }
        });
    }

    private void crearFiltros() {
        LinearLayout fila = findViewById(R.id.fila_filtros);
        for (int i = 0; i < regiones.length; i++) {
            String texto = regiones[i].isEmpty() ? getString(R.string.cafeterias_filtro_todas)
                    : regiones[i];
            TextView chip = UiUtils.crearChip(fila, texto);
            final int posicion = i;
            chip.setOnClickListener(v -> {
                regionSeleccionada = regiones[posicion];
                refrescarChips();
                aplicarFiltros();
            });
            chips.add(chip);
        }
        refrescarChips();
    }

    private void refrescarChips() {
        for (int i = 0; i < chips.size(); i++) {
            boolean seleccionado = regiones[i].equals(regionSeleccionada);
            UiUtils.marcarChipSeleccionado(chips.get(i), seleccionado);
        }
    }

    private void aplicarFiltros() {
        EditText campo = findViewById(R.id.campo_busqueda);
        String textoBusqueda = campo.getText().toString().toLowerCase().trim();

        List<Tostaderia> resultado = new ArrayList<>();
        for (Tostaderia tostaduría : RepositorioDatos.obtenerTostaderiasPorRegion(regionSeleccionada)) {
            if (tostaduría.nombre.toLowerCase().contains(textoBusqueda)) {
                resultado.add(tostaduría);
            }
        }
        adaptador.actualizar(resultado);
    }
}