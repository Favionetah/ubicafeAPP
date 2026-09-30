package com.ubicafe.app.ui.comun;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.ubicafe.app.R;
import com.ubicafe.app.datos.RepositorioDatos;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * CRÉDITOS Y FUENTES
 * ---------------------------------------------------------------
 * Contesta lo que alguien se preguntaría al ver una foto en la app: de
 * dónde salió, quién la hizo y con qué permiso.
 *
 * POR QUÉ EXISTE Y NO ES UN ADORNO
 * Las imágenes vienen de Wikimedia Commons. Más de 200 usan licencias
 * CC BY o CC BY-SA, y esas licencias tienen una obligación concreta:
 * nombrar al autor y decir qué licencia se aplica. Dejar el manifiesto
 * escondido en los assets no cumple eso, por mucho que el archivo esté
 * ahí dentro.
 *
 * LAS CIFRAS NO ESTÁN ESCRITAS A MANO
 * Se leen de imagenes.json, el mismo archivo que usa el sembrador, así
 * que esta pantalla no puede quedarse diciendo "242" si el próximo censo
 * trae 250. Si el manifiesto no se puede leer, la pantalla lo dice en
 * lugar de mostrar un cero.
 */
public class CreditosActivity extends AppCompatActivity {

    private static final String ARCHIVO_IMAGENES = "imagenes.json";

    /** Abre la pantalla desde cualquier parte de la app. */
    public static void abrir(Context contexto) {
        contexto.startActivity(new Intent(contexto, CreditosActivity.class));
    }

    @Override
    protected void onCreate(@Nullable Bundle estado) {
        super.onCreate(estado);
        setContentView(R.layout.activity_creditos);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.creditos_titulo);
        }

        pintarDatos();
        pintarImagenes(leerManifiesto());
    }

    /** De dónde salen los lugares: el censo, con su fecha y su alcance. */
    private void pintarDatos() {
        StringBuilder texto = new StringBuilder();
        texto.append(getString(R.string.creditos_datos_texto));

        // Las cifras salen de la base y no del JSON suelto: así el texto
        // describe lo que la app está enseñando de verdad.
        String fuente = RepositorioDatos.obtenerFuente();
        int anio = RepositorioDatos.obtenerAnioCenso();
        if (!fuente.isEmpty() || anio > 0) {
            texto.append("\n\n").append(getString(
                    R.string.creditos_datos_origen, fuente, anio));
        }

        Map<String, Integer> conteo = RepositorioDatos.contarFilas();
        Integer lugares = conteo.get("entidades");
        Integer marcas = conteo.get("marcas");
        if (lugares != null && marcas != null) {
            texto.append("\n\n").append(getString(
                    R.string.creditos_datos_conteo, lugares, marcas));
        }
        ((TextView) findViewById(R.id.texto_datos)).setText(texto.toString());
    }

    /**
     * Rellena el resumen, la lista de autores y la de licencias con lo
     * que declara el manifiesto. Si el manifiesto no está, se avisa en
     * vez de mostrar una pantalla vacía sin explicación.
     */
    private void pintarImagenes(JSONObject manifiesto) {
        TextView resumen = findViewById(R.id.texto_resumen_imagenes);
        TextView version = findViewById(R.id.texto_version);

        if (manifiesto == null) {
            resumen.setText(R.string.creditos_sin_manifiesto);
            version.setVisibility(View.GONE);
            return;
        }

        int total = contarImagenes(manifiesto);

        resumen.setText(getString(R.string.creditos_imagenes_texto, total));

        version.setText(getString(R.string.creditos_version,
                manifiesto.optString("generado", "?"), total));

        JSONObject creditos = manifiesto.optJSONObject("creditos");
        if (creditos == null || creditos.length() == 0) {
            return;
        }

        // Se cuentan y se agrupan los autores y las licencias. Un mapa
        // conserva el orden de entrada, así que la lista sale en el mismo
        // orden en que el generador las escribió, sin ordenar por
        // criterio critères que podrían cambiar entre versiones.
        Map<String, Integer> autores = new LinkedHashMap<>();
        Map<String, Integer> licencias = new LinkedHashMap<>();
        java.util.Iterator<String> claves = creditos.keys();
        while (claves.hasNext()) {
            JSONObject entrada = creditos.optJSONObject(claves.next());
            if (entrada == null) {
                continue;
            }
            String autor = entrada.optString("credito", "").trim();
            String licencia = entrada.optString("licencia", "").trim();
            contarEn(autores, autor.isEmpty()
                    ? getString(R.string.creditos_desconocido) : autor);
            contarEn(licencias, licencia.isEmpty()
                    ? getString(R.string.creditos_desconocido) : licencia);
        }

        agregarFilas(R.id.lista_autores, autores, R.plurals.creditos_fila_autor);
        agregarFilas(R.id.lista_licencias, licencias, R.plurals.creditos_fila_licencia);
    }

    private void contarEn(Map<String, Integer> mapa, String clave) {
        Integer anterior = mapa.get(clave);
        mapa.put(clave, anterior == null ? 1 : anterior + 1);
    }

    /**
     * Una fila por entrada del mapa. Las imágenes y los autores se
     * muestran como texto plano y no como enlaces: no se puede abrir un
     * navegador dentro de la app sin sacar al usuario de ella, y un
     * enlace que no hace nada es peor que no ponerlo.
     */
    private void agregarFilas(int idContenedor, Map<String, Integer> entradas,
                              int plural) {
        LinearLayout lista = findViewById(idContenedor);
        LayoutInflater inflador = LayoutInflater.from(this);
        for (Map.Entry<String, Integer> entrada : entradas.entrySet()) {
            TextView fila = (TextView) inflador.inflate(
                    android.R.layout.simple_list_item_1, lista, false);
            int cantidad = entrada.getValue();
            fila.setText(getResources().getQuantityString(plural, cantidad,
                    cantidad, entrada.getKey()));
            fila.setTextColor(getResources().getColor(R.color.texto_secundario, getTheme()));
            lista.addView(fila);
        }
    }

    /** El total de archivos del manifiesto, contando las tres carpetas. */
    private int contarImagenes(JSONObject manifiesto) {
        int total = 0;
        for (String seccion : new String[]{"cafes", "marcas", "tostadurias"}) {
            JSONObject grupo = manifiesto.optJSONObject(seccion);
            if (grupo != null) {
                total += grupo.length();
            }
        }
        return total;
    }

    /**
     * El manifiesto de las imágenes, o null si no se puede leer. No
     * propaga la excepción: que falte un archivo tiene que dejar la app
     * en pie, y esta pantalla se puede abrir sin él.
     */
    private JSONObject leerManifiesto() {
        try (InputStream entrada = getAssets().open(ARCHIVO_IMAGENES)) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream(4096);
            byte[] trozo = new byte[8192];
            int leidos;
            while ((leidos = entrada.read(trozo)) != -1) {
                buffer.write(trozo, 0, leidos);
            }
            return new JSONObject(new String(buffer.toByteArray(), StandardCharsets.UTF_8));
        } catch (IOException | org.json.JSONException problema) {
            return null;
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
