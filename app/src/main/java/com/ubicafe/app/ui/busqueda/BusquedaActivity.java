package com.ubicafe.app.ui.busqueda;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.ubicafe.app.R;
import com.ubicafe.app.datos.RepositorioDatos;
import com.ubicafe.app.modelo.CafeOrigen;
import com.ubicafe.app.modelo.Cafeteria;
import com.ubicafe.app.modelo.MarcaCafe;
import com.ubicafe.app.modelo.ResultadoBusqueda;
import com.ubicafe.app.ui.cafeterias.DetalleCafeteriaActivity;
import com.ubicafe.app.ui.marcas.DetalleMarcaActivity;
import com.ubicafe.app.ui.origen.FichaOrigenActivity;

/**
 * BÚSQUEDA GLOBAL (P10).
 * Busca en toda la app (cafés, marcas y cafeterías) mientras se escribe.
 */
public class BusquedaActivity extends AppCompatActivity {

    private TextView textoVacioInicial;
    private TextView tituloCafes, tituloMarcas, tituloEstablecimientos;
    private LinearLayout listaCafes, listaMarcas, listaEstablecimientos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_busqueda);

        textoVacioInicial = findViewById(R.id.texto_vacio_inicial);
        tituloCafes = findViewById(R.id.titulo_cafes);
        tituloMarcas = findViewById(R.id.titulo_marcas);
        tituloEstablecimientos = findViewById(R.id.titulo_establecimientos);
        tituloCafes.setTag(R.string.seccion_cafes_especialidad);
        tituloMarcas.setTag(R.string.seccion_marcas_registradas);
        tituloEstablecimientos.setTag(R.string.seccion_establecimientos);
        listaCafes = findViewById(R.id.lista_cafes);
        listaMarcas = findViewById(R.id.lista_marcas);
        listaEstablecimientos = findViewById(R.id.lista_establecimientos);

        EditText campo = findViewById(R.id.campo_busqueda);
        campo.requestFocus();
        campo.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }

            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {
                buscar(s.toString());
            }

            @Override public void afterTextChanged(Editable s) { }
        });

        // El botón "Buscar" del teclado también dispara la búsqueda.
        campo.setOnEditorActionListener((view, accionId, evento) -> {
            if (accionId == EditorInfo.IME_ACTION_SEARCH) {
                buscar(campo.getText().toString());
                return true;
            }
            return false;
        });

        findViewById(R.id.btn_cancelar).setOnClickListener(v -> finish());
    }

    /** Lanza la búsqueda y pinta los resultados en sus secciones. */
    private void buscar(String texto) {
        ResultadoBusqueda resultado = RepositorioDatos.buscar(texto);

        textoVacioInicial.setVisibility(texto.trim().isEmpty() ? View.VISIBLE : View.GONE);

        llenarSeccion(listaCafes, tituloCafes, resultado.cafesDeEspecialidad.size());
        llenarSeccion(listaMarcas, tituloMarcas, resultado.marcasRegistradas.size());
        llenarSeccion(listaEstablecimientos, tituloEstablecimientos,
                resultado.establecimientos.size());

        listaCafes.removeAllViews();
        listaMarcas.removeAllViews();
        listaEstablecimientos.removeAllViews();

        for (CafeOrigen cafe : resultado.cafesDeEspecialidad) {
            agregarFilaCafe(cafe);
        }
        for (MarcaCafe marca : resultado.marcasRegistradas) {
            agregarFilaMarca(marca);
        }
        for (Cafeteria cafeteria : resultado.establecimientos) {
            agregarFilaCafeteria(cafeteria);
        }
    }

    /** Muestra u oculta el título de una sección según cuántos resultados tiene. */
    private void llenarSeccion(LinearLayout lista, TextView titulo, int cantidad) {
        boolean hayResultados = cantidad > 0;
        lista.setVisibility(hayResultados ? View.VISIBLE : View.GONE);
        titulo.setVisibility(hayResultados ? View.VISIBLE : View.GONE);
        titulo.setText(getString(R.string.seccion_conteo,
                getString((Integer) titulo.getTag()), cantidad));
    }

    // ---------- Construcción de filas ----------

    private void agregarFilaCafe(CafeOrigen cafe) {
        View fila = inflarFila(listaCafes);
        ((TextView) fila.findViewById(R.id.texto_nombre)).setText(cafe.variedad);
        ((TextView) fila.findViewById(R.id.texto_subtitulo)).setText(cafe.origen);

        fila.setOnClickListener(v -> abrirFichaOrigen(cafe, ""));
        listaCafes.addView(fila);
    }

    private void agregarFilaMarca(MarcaCafe marca) {
        View fila = inflarFila(listaMarcas);
        ((TextView) fila.findViewById(R.id.texto_nombre)).setText(marca.nombre);
        ((TextView) fila.findViewById(R.id.texto_subtitulo))
                .setText(getString(R.string.marca_nacional_registrada)
                        + " · " + android.text.TextUtils.join(", ", marca.etiquetas));

        fila.setOnClickListener(v -> {
            Intent intento = new Intent(this, DetalleMarcaActivity.class);
            intento.putExtra(DetalleMarcaActivity.EXTRA_NOMBRE_MARCA, marca.nombre);
            startActivity(intento);
        });
        listaMarcas.addView(fila);
    }

    private void agregarFilaCafeteria(Cafeteria cafeteria) {
        View fila = inflarFila(listaEstablecimientos);
        ((TextView) fila.findViewById(R.id.texto_nombre)).setText(cafeteria.nombre);
        ((TextView) fila.findViewById(R.id.texto_subtitulo))
                .setText(cafeteria.direccion + " · " + cafeteria.zona);

        fila.setOnClickListener(v -> {
            Intent intento = new Intent(this, DetalleCafeteriaActivity.class);
            intento.putExtra(DetalleCafeteriaActivity.EXTRA_NOMBRE_CAFETERIA, cafeteria.nombre);
            startActivity(intento);
        });
        listaEstablecimientos.addView(fila);
    }

    // ---------- Utilidades ----------

    private View inflarFila(LinearLayout contenedor) {
        return getLayoutInflater().inflate(R.layout.item_resultado_busqueda, contenedor, false);
    }

    private void abrirFichaOrigen(CafeOrigen cafe, String nombreMarca) {
        Intent intento = new Intent(this, FichaOrigenActivity.class);
        intento.putExtra(FichaOrigenActivity.EXTRA_VARIEDAD, cafe.variedad);
        intento.putExtra(FichaOrigenActivity.EXTRA_NOMBRE_MARCA, nombreMarca);
        startActivity(intento);
    }
}