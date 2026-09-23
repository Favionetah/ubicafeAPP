package com.ubicafe.app.ui.busqueda;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

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
 * Pestaña BÚSQUEDA.
 * Busca en toda la app (cafés, marcas y cafeterías) mientras el usuario escribe.
 */
public class BusquedaFragment extends Fragment {

    private TextView textoVacioInicial;
    private TextView tituloCafes, tituloMarcas, tituloEstablecimientos;
    private LinearLayout listaCafes, listaMarcas, listaEstablecimientos;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflador, @Nullable ViewGroup contenedor,
                             @Nullable Bundle estado) {
        return inflador.inflate(R.layout.fragment_busqueda, contenedor, false);
    }

    @Override
    public void onViewCreated(@NonNull View vista, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(vista, savedInstanceState);

        textoVacioInicial = vista.findViewById(R.id.texto_vacio_inicial);
        tituloCafes = vista.findViewById(R.id.titulo_cafes);
        tituloMarcas = vista.findViewById(R.id.titulo_marcas);
        tituloEstablecimientos = vista.findViewById(R.id.titulo_establecimientos);
        listaCafes = vista.findViewById(R.id.lista_cafes);
        listaMarcas = vista.findViewById(R.id.lista_marcas);
        listaEstablecimientos = vista.findViewById(R.id.lista_establecimientos);

        EditText campo = vista.findViewById(R.id.campo_busqueda);

        // Al escribir, se busca automáticamente.
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

        // El botón "Cancelar" vuelve a la pestaña anterior (cierra el teclado).
        vista.findViewById(R.id.btn_cancelar).setOnClickListener(v -> {
            campo.setText("");
            campo.clearFocus();
        });
    }

    /** Lanza la búsqueda y pinta los resultados en sus secciones. */
    private void buscar(String texto) {
        ResultadoBusqueda resultado = RepositorioDatos.buscar(texto);

        // Mensaje inicial mientras no hay nada escrito.
        textoVacioInicial.setVisibility(texto.trim().isEmpty() ? View.VISIBLE : View.GONE);

        llenarSeccion(listaCafes, tituloCafes, resultado.cafesDeEspecialidad.size());
        llenarSeccion(listaMarcas, tituloMarcas, resultado.marcasRegistradas.size());
        llenarSeccion(listaEstablecimientos, tituloEstablecimientos, resultado.establecimientos.size());

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
    }

    // ---------- Construcción de filas ----------

    private void agregarFilaCafe(CafeOrigen cafe) {
        View fila = inflarFila(listaCafes);
        ((TextView) fila.findViewById(R.id.texto_nombre)).setText(cafe.variedad);
        ((TextView) fila.findViewById(R.id.texto_subtitulo))
                .setText(getString(R.string.ficha_campo_origen) + ": " + cafe.origen);
        pintarInicial(fila, cafe.variedad, R.color.cafe_accent);

        fila.setOnClickListener(v -> abrirFichaOrigen(cafe, ""));
        listaCafes.addView(fila);
    }

    private void agregarFilaMarca(MarcaCafe marca) {
        View fila = inflarFila(listaMarcas);
        ((TextView) fila.findViewById(R.id.texto_nombre)).setText(marca.nombre);
        ((TextView) fila.findViewById(R.id.texto_subtitulo))
                .setText(getString(R.string.marca_nacional_registrada)
                        + " · " + android.text.TextUtils.join(", ", marca.etiquetas));
        pintarInicial(fila, marca.nombre, marca.colorMarca);

        fila.setOnClickListener(v -> {
            Intent intento = new Intent(requireContext(), DetalleMarcaActivity.class);
            intento.putExtra(DetalleMarcaActivity.EXTRA_NOMBRE_MARCA, marca.nombre);
            startActivity(intento);
        });
        listaMarcas.addView(fila);
    }

    private void agregarFilaCafeteria(Cafeteria cafeteria) {
        View fila = inflarFila(listaEstablecimientos);
        ((TextView) fila.findViewById(R.id.texto_nombre)).setText(cafeteria.nombre);
        ((TextView) fila.findViewById(R.id.texto_subtitulo))
                .setText(cafeteria.zona + " · " + cafeteria.tipo);
        pintarInicial(fila, cafeteria.nombre, R.color.verde_oscuro);

        fila.setOnClickListener(v -> {
            Intent intento = new Intent(requireContext(), DetalleCafeteriaActivity.class);
            intento.putExtra(DetalleCafeteriaActivity.EXTRA_NOMBRE_CAFETERIA, cafeteria.nombre);
            startActivity(intento);
        });
        listaEstablecimientos.addView(fila);
    }

    // ---------- Utilidades ----------

    private View inflarFila(LinearLayout contenedor) {
        return getLayoutInflater().inflate(R.layout.item_resultado_busqueda, contenedor, false);
    }

    /** Pinta el cuadrito de color y su letra inicial. */
    private void pintarInicial(View fila, String nombre, int colorRes) {
        LinearLayout boxColor = fila.findViewById(R.id.box_color);
        TextView textoInicial = fila.findViewById(R.id.texto_inicial);

        GradientDrawable cuadro = new GradientDrawable();
        cuadro.setShape(GradientDrawable.RECTANGLE);
        cuadro.setCornerRadius(12f);
        cuadro.setColor(getResources().getColor(colorRes, requireContext().getTheme()));
        boxColor.setBackground(cuadro);

        textoInicial.setText(nombre.substring(0, 1));
    }

    private void abrirFichaOrigen(CafeOrigen cafe, String nombreMarca) {
        Intent intento = new Intent(requireContext(), FichaOrigenActivity.class);
        intento.putExtra(FichaOrigenActivity.EXTRA_VARIEDAD, cafe.variedad);
        intento.putExtra(FichaOrigenActivity.EXTRA_NOMBRE_MARCA, nombreMarca);
        startActivity(intento);
    }
}