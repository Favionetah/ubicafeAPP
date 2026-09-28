package com.ubicafe.app.ui.ecosistema;

import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.ubicafe.app.R;
import com.ubicafe.app.datos.RepositorioDatos;
import com.ubicafe.app.modelo.Entidad;
import com.ubicafe.app.modelo.Estadisticas;
import com.ubicafe.app.modelo.Rol;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * PESTAÑA ECOSISTEMA
 * ---------------------------------------------------------------
 * Las cuatro cifras del censo y la distribución de lugares por
 * macrodistrito. Los números salen de Estadisticas, que cuenta las
 * entidades reales: 213 lugares, de los cuales 189 son cafeterías.
 * Nada aquí está escrito a mano, así que cambiar el censo no obliga a
 * cambiar esta pantalla.
 */
public class EcosistemaFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflador, @Nullable ViewGroup contenedor,
                             @Nullable Bundle estado) {
        return inflador.inflate(R.layout.fragment_ecosistema, contenedor, false);
    }

    @Override
    public void onViewCreated(@NonNull View vista, @Nullable Bundle estado) {
        super.onViewCreated(vista, estado);

        Estadisticas datos = RepositorioDatos.obtenerEstadisticas();

        llenarCifra(vista, R.id.cifra_lugares, datos.totalEntidades,
                getString(R.string.ecosistema_lugares), R.color.verde_oscuro);
        llenarCifra(vista, R.id.cifra_marcas, datos.marcas,
                getString(R.string.ecosistema_marcas), R.color.cafe_accent);
        llenarCifra(vista, R.id.cifra_tostaderias, datos.tostadurias,
                getString(R.string.ecosistema_tostaderias), R.color.verde_claro);
        llenarCifra(vista, R.id.cifra_productores, datos.productores,
                getString(R.string.ecosistema_productores), R.color.caramelo_claro);
        llenarCifra(vista, R.id.cifra_cafeterias, datos.cafeterias,
                getString(R.string.ecosistema_cafeterias), R.color.verde_claro);
        llenarCifra(vista, R.id.cifra_puntos_venta, datos.puntosDeVenta,
                getString(R.string.ecosistema_puntos_venta), R.color.cafe_accent);
        llenarCifra(vista, R.id.cifra_variedades, datos.variedades,
                getString(R.string.ecosistema_variedades), R.color.caramelo_claro);
        llenarCifra(vista, R.id.cifra_sucursales,
                RepositorioDatos.obtenerSucursales().size(),
                getString(R.string.ecosistema_sucursales), R.color.verde_oscuro);

        llenarMacrodistritos(vista);
        pintarNota(vista, datos);
    }

    /** Rellena una de las tarjetas de cifra con número, etiqueta y su raya de color. */
    private void llenarCifra(View vista, int idTarjeta, int numero, String etiqueta,
                             int colorRes) {
        View tarjeta = vista.findViewById(idTarjeta);
        ((TextView) tarjeta.findViewById(R.id.texto_numero)).setText(String.valueOf(numero));
        ((TextView) tarjeta.findViewById(R.id.texto_etiqueta)).setText(etiqueta);

        GradientDrawable raya = new GradientDrawable();
        raya.setShape(GradientDrawable.RECTANGLE);
        raya.setCornerRadius(20f);
        raya.setColor(ContextCompat.getColor(requireContext(), colorRes));
        tarjeta.findViewById(R.id.barra_color).setBackground(raya);
    }

    /**
     * La nota de abajo. Cifras y fuente salen del JSON, no del diseño:
     * el rango de precios que pedía la maqueta no va porque el censo no
     * registró ninguno, y poner "Bs. 15 — Bs. 95" sería inventar el
     * dato más citado de la pantalla.
     */
    private void pintarNota(View vista, Estadisticas datos) {
        TextView nota = vista.findViewById(R.id.texto_nota_metodologica);
        int anio = RepositorioDatos.obtenerAnioCenso();
        String fuente = RepositorioDatos.obtenerFuente();

        if (fuente.isEmpty() || anio <= 0) {
            nota.setVisibility(View.GONE);
            return;
        }
        nota.setText(getString(R.string.ecosistema_nota_completa,
                getString(R.string.ecosistema_nota_cifras,
                        datos.totalEntidades, datos.cafeterias, datos.conDetalleCafeteria),
                fuente, anio));
    }

    /**
     * Las cafeterías por macrodistrito, con la barra proporcional a la
     * más poblada. Solo cuenta cafeterías: un productor en el norte no es
     * un café por el centro, y mezclar roles haría la barra mentirosa.
     */
    private void llenarMacrodistritos(View vista) {
        Map<String, Integer> conteoPorZona =
                RepositorioDatos.obtenerConteoPorMacrodistrito(Rol.CAFETERIA);
        // De mayor a menor: la barra más larga arriba es la que da
        // referencia, y si empiece por el Centro la comparación se hace
        // con la última, que es la más corta.
        List<Map.Entry<String, Integer>> zonas = new ArrayList<>(conteoPorZona.entrySet());
        zonas.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));

        int maximo = 1;
        for (int n : conteoPorZona.values()) {
            maximo = Math.max(maximo, n);
        }

        LinearLayout contenedor = vista.findViewById(R.id.contenedor_macrodistritos);
        contenedor.removeAllViews();

        LayoutInflater inflador = LayoutInflater.from(requireContext());
        for (Map.Entry<String, Integer> zona : zonas) {
            View fila = inflador.inflate(R.layout.item_macrodistrito, contenedor, false);
            ((TextView) fila.findViewById(R.id.texto_nombre)).setText(zona.getKey());
            ((TextView) fila.findViewById(R.id.texto_conteo)).setText(
                    getResources().getQuantityString(R.plurals.ecosistema_cafeterias_de,
                            zona.getValue(), zona.getValue()));

            ProgressBar barra = fila.findViewById(R.id.barra_progreso);
            barra.setMax(maximo);
            barra.setProgress(zona.getValue());

            contenedor.addView(fila);
        }
    }
}
