package com.ubicafe.app.ui.comun;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.ubicafe.app.R;
import com.ubicafe.app.modelo.Sucursal;
import com.ubicafe.app.ui.mapa.Geolocalizador;

import java.util.List;

/**
 * LISTA DE SUCURSALES DENTRO DE UNA FICHA
 * ---------------------------------------------------------------
 * La lista de locales aparece dentro de una pantalla que ya tiene su
 * propio scroll, así que no se anida un RecyclerView: eso que disables
 * el scroll interno y con 78 filas se nota. Se inflan las tarjetas
 * directamente en un LinearLayout. El precio es aceptable porque las
 * fichas muestran los locales de una sola cadena, y la mayor tiene 11.
 */
public final class ListaSucursales {

    /** Se abre al tocar un local. */
    public interface AlTocar {
        void onSucursalTocada(Sucursal sucursal);
    }

    private ListaSucursales() {
        // Clase de utilidades: no se instancia.
    }

    /**
     * Dibuja las tarjetas de los locales dentro del contenedor. Si no
     * hay ninguno, dice que no registró ninguno: una lista vacía sin
     * explicación parece un fallo de la app.
     *
     * @return cuántos locales se pintaron
     */
    public static int agregar(LinearLayout contenedor, List<Sucursal> sucursales,
                              AlTocar alTocar) {
        if (sucursales.isEmpty()) {
            FichaTecnica.mostrarAviso(contenedor,
                    contenedor.getContext().getString(R.string.detalle_sin_sucursales));
            return 0;
        }

        LayoutInflater inflador = LayoutInflater.from(contenedor.getContext());
        for (Sucursal sucursal : sucursales) {
            View tarjeta = inflador.inflate(R.layout.item_sucursal, contenedor, false);
            ViewGroup.LayoutParams parametros = tarjeta.getLayoutParams();
            parametros.width = ViewGroup.LayoutParams.MATCH_PARENT;
            ((ViewGroup.MarginLayoutParams) parametros).leftMargin = 0;
            ((ViewGroup.MarginLayoutParams) parametros).rightMargin = 0;
            tarjeta.setLayoutParams(parametros);

            pintar(tarjeta, sucursal, alTocar);
            contenedor.addView(tarjeta);
        }
        return sucursales.size();
    }

    private static void pintar(View tarjeta, Sucursal sucursal, AlTocar alTocar) {
        ((TextView) tarjeta.findViewById(R.id.texto_entidad)).setText(sucursal.marca);
        ((TextView) tarjeta.findViewById(R.id.texto_sucursal)).setText(sucursal.nombre);

        TextView direccion = tarjeta.findViewById(R.id.texto_direccion);
        if (sucursal.direccion.isEmpty()) {
            direccion.setVisibility(View.GONE);
        } else {
            direccion.setText(sucursal.direccion);
        }

        View pie = tarjeta.findViewById(R.id.fila_pie);
        if (!sucursal.tieneCoordenadas()) {
            pie.setVisibility(View.GONE);
        } else {
            double kilometros = Geolocalizador.kilometrosA(
                    tarjeta.getContext(), sucursal.lat, sucursal.lng);
            ((TextView) tarjeta.findViewById(R.id.texto_distancia)).setText(kilometros < 0
                    ? Geolocalizador.SIN_UBICACION
                    : Geolocalizador.formatear(kilometros));
        }

        tarjeta.setOnClickListener(v -> alTocar.onSucursalTocada(sucursal));
    }

    /** El encabezado "N sucursales registradas", ya pintado. */
    public static TextView encabezado(LinearLayout contenedor, int total) {
        TextView titulo = FichaTecnica.encabezado(contenedor.getContext(),
                R.string.detalle_sucursales_titulo);
        titulo.setText(contenedor.getResources().getQuantityString(
                R.plurals.detalle_sucursales, total, total));
        return titulo;
    }
}
