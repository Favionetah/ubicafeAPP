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

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.ubicafe.app.R;
import com.ubicafe.app.datos.RepositorioDatos;
import com.ubicafe.app.modelo.CafeVariedad;
import com.ubicafe.app.modelo.Entidad;
import com.ubicafe.app.modelo.Marca;
import com.ubicafe.app.modelo.ResultadoBusqueda;
import com.ubicafe.app.ui.cafeterias.DetalleCafeteriaActivity;
import com.ubicafe.app.ui.marcas.DetalleMarcaActivity;
import com.ubicafe.app.ui.origen.FichaOrigenActivity;
import com.ubicafe.app.ui.productores.DetalleProductorActivity;
import com.ubicafe.app.ui.tostaderias.DetalleTostaderiaActivity;
import com.ubicafe.app.util.Texto;

/**
 * BÚSQUEDA GLOBAL (P10)
 * ---------------------------------------------------------------
 * Un solo campo para todo el censo. Los resultados se agrupan en cinco
 * secciones porque "cafe" devuelve cinco cosas a la vez, y cada tipo
 * lleva a una pantalla distinta: un café de origen a su ficha, una marca
 * a la suya, un lugar a su ficha de detalle.
 *
 * La comparación ignora mayúsculas y tildes, así que "geisha" encuentra
 * "GEISHA" y "cafe" encuentra "Café".
 */
public class BusquedaActivity extends AppCompatActivity {

    private TextView textoVacioInicial;
    private TextView tituloCafes, tituloMarcas, tituloEstablecimientos;
    private TextView tituloProductores, tituloTostaderias, textoSinResultados;
    private LinearLayout listaCafes, listaMarcas, listaEstablecimientos;
    private LinearLayout listaProductores, listaTostaderias;

    @Override
    protected void onCreate(@Nullable Bundle estado) {
        super.onCreate(estado);
        setContentView(R.layout.activity_busqueda);

        textoVacioInicial = findViewById(R.id.texto_vacio_inicial);
        textoSinResultados = findViewById(R.id.texto_sin_resultados);

        tituloCafes = findViewById(R.id.titulo_cafes);
        tituloMarcas = findViewById(R.id.titulo_marcas);
        tituloEstablecimientos = findViewById(R.id.titulo_establecimientos);
        tituloProductores = findViewById(R.id.titulo_productores);
        tituloTostaderias = findViewById(R.id.titulo_tostaderias);

        listaCafes = findViewById(R.id.lista_cafes);
        listaMarcas = findViewById(R.id.lista_marcas);
        listaEstablecimientos = findViewById(R.id.lista_establecimientos);
        listaProductores = findViewById(R.id.lista_productores);
        listaTostaderias = findViewById(R.id.lista_tostaderias);

        EditText campo = findViewById(R.id.campo_busqueda);
        campo.requestFocus();
        campo.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }

            @Override public void afterTextChanged(Editable s) { buscar(s.toString()); }
        });

        campo.setOnEditorActionListener((vista, accion, evento) -> {
            if (accion == EditorInfo.IME_ACTION_SEARCH) {
                buscar(campo.getText().toString());
                return true;
            }
            return false;
        });

        findViewById(R.id.btn_cancelar).setOnClickListener(v -> finish());
    }

    /** Lanza la búsqueda y pinta cada sección con sus resultados. */
    private void buscar(String texto) {
        boolean vacio = texto.trim().isEmpty();
        textoVacioInicial.setVisibility(vacio ? View.VISIBLE : View.GONE);
        if (vacio) {
            textoSinResultados.setVisibility(View.GONE);
            limpiarTodo();
            return;
        }

        ResultadoBusqueda resultado = RepositorioDatos.buscar(texto);

        listaCafes.removeAllViews();
        listaMarcas.removeAllViews();
        listaEstablecimientos.removeAllViews();
        listaProductores.removeAllViews();
        listaTostaderias.removeAllViews();

        for (CafeVariedad cafe : resultado.cafesDeOrigen) {
            agregarFilaCafe(listaCafes, cafe);
        }
        for (Marca marca : resultado.marcas) {
            agregarFilaMarca(listaMarcas, marca);
        }
        for (Entidad lugar : resultado.establecimientos) {
            agregarFilaLugar(listaEstablecimientos, lugar, DetalleCafeteriaActivity.class);
        }
        for (Entidad lugar : resultado.productores) {
            agregarFilaLugar(listaProductores, lugar, DetalleProductorActivity.class);
        }
        for (Entidad lugar : resultado.tostadurias) {
            agregarFilaLugar(listaTostaderias, lugar, DetalleTostaderiaActivity.class);
        }

        llenarSeccion(listaCafes, tituloCafes, resultado.cafesDeOrigen.size(),
                R.string.seccion_cafes_especialidad);
        llenarSeccion(listaMarcas, tituloMarcas, resultado.marcas.size(),
                R.string.seccion_marcas_registradas);
        llenarSeccion(listaEstablecimientos, tituloEstablecimientos,
                resultado.establecimientos.size(), R.string.seccion_establecimientos);
        llenarSeccion(listaProductores, tituloProductores, resultado.productores.size(),
                R.string.categoria_productores);
        llenarSeccion(listaTostaderias, tituloTostaderias, resultado.tostadurias.size(),
                R.string.categoria_tostaderias);

        textoSinResultados.setVisibility(
                resultado.estaVacio() ? View.VISIBLE : View.GONE);
    }

    private void limpiarTodo() {
        listaCafes.removeAllViews();
        listaMarcas.removeAllViews();
        listaEstablecimientos.removeAllViews();
        listaProductores.removeAllViews();
        listaTostaderias.removeAllViews();
        for (TextView titulo : new TextView[]{tituloCafes, tituloMarcas,
                tituloEstablecimientos, tituloProductores, tituloTostaderias}) {
            titulo.setVisibility(View.GONE);
        }
    }

    /** El título de una sección con su conteo, o la oculta si está vacía. */
    private void llenarSeccion(LinearLayout lista, TextView titulo, int cantidad,
                               int etiqueta) {
        boolean hayResultados = cantidad > 0;
        lista.setVisibility(hayResultados ? View.VISIBLE : View.GONE);
        titulo.setVisibility(hayResultados ? View.VISIBLE : View.GONE);
        if (hayResultados) {
            titulo.setText(getString(R.string.seccion_conteo,
                    getString(etiqueta), cantidad));
        }
    }

    // ---------- Filas ----------

    private void agregarFilaCafe(LinearLayout lista, CafeVariedad cafe) {
        View fila = inflarFila(lista);
        ((TextView) fila.findViewById(R.id.texto_nombre)).setText(cafe.nombre);
        ((TextView) fila.findViewById(R.id.texto_subtitulo))
                .setText(Texto.unir(" · ", java.util.Arrays.asList(
                        cafe.region, cafe.marca)));
        fila.setOnClickListener(v -> {
            Intent intento = new Intent(this, FichaOrigenActivity.class);
            intento.putExtra(FichaOrigenActivity.EXTRA_VARIEDAD, cafe.nombre);
            startActivity(intento);
        });
        lista.addView(fila);
    }

    private void agregarFilaMarca(LinearLayout lista, Marca marca) {
        View fila = inflarFila(lista);
        ((TextView) fila.findViewById(R.id.texto_nombre)).setText(marca.nombre);
        int locales = marca.sucursales.size();
        ((TextView) fila.findViewById(R.id.texto_subtitulo)).setText(marca.tieneSucursales()
                ? getResources().getQuantityString(
                        R.plurals.marca_puntos_descripcion, locales, locales)
                : getString(R.string.detalle_sin_sucursales));
        fila.setOnClickListener(v -> {
            Intent intento = new Intent(this, DetalleMarcaActivity.class);
            intento.putExtra(DetalleMarcaActivity.EXTRA_NOMBRE_MARCA, marca.nombre);
            startActivity(intento);
        });
        lista.addView(fila);
    }

    /**
     * Un lugar del censo. Todas las fichas aceptan el identificador, así
     * que la fila no necesita saber de qué tipo es más allá de la clase
     * de destino.
     */
    private void agregarFilaLugar(LinearLayout lista, Entidad lugar, Class<?> destino) {
        View fila = inflarFila(lista);
        ((TextView) fila.findViewById(R.id.texto_nombre)).setText(lugar.nombre);
        ((TextView) fila.findViewById(R.id.texto_subtitulo))
                .setText(Texto.unir(" · ", java.util.Arrays.asList(
                        lugar.rolesComoTexto(),
                        lugar.direccion.isEmpty() ? lugar.macrodistrito : lugar.direccion)));
        fila.setOnClickListener(v -> {
            Intent intento = new Intent(this, destino);
            intento.putExtra(DetalleCafeteriaActivity.EXTRA_ID_ENTIDAD, lugar.id);
            startActivity(intento);
        });
        lista.addView(fila);
    }

    private View inflarFila(LinearLayout contenedor) {
        return getLayoutInflater().inflate(R.layout.item_resultado_busqueda,
                contenedor, false);
    }
}
