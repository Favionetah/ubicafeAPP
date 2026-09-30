package com.ubicafe.app.ui.mapa;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import com.ubicafe.app.R;

/**
 * Los dos iconos que usa un pin del mapa.
 *
 * El pin normal se tiñe con el color del papel que representa. El
 * seleccionado es ese mismo pin un 42% más grande y con un disco blanco
 * detrás de la cabeza: se distingue de un vistazo sin perder el color que
 * dice qué tipo de lugar es, que es información y no decoración.
 *
 * Las dos capas del pin seleccionado comparten viewport y tamaño, así que
 * alinean solas y no hay que compensar nada con insets.
 *
 * Vive aquí y no en cada pantalla porque MapaFragment y MapaActivity
 * necesitan exactamente los mismos dos iconos.
 */
public final class PinesMapa {

    private PinesMapa() {
    }

    /** El pin del color indicado, del tamaño normal. */
    public static Drawable pin(Context contexto, int color) {
        return teñir(contexto, R.drawable.ic_map_pin, color);
    }

    /** El pin del color indicado, en grande y con el disco blanco detrás. */
    public static Drawable pinSeleccionado(Context contexto, int color) {
        Drawable disco = ContextCompat.getDrawable(contexto, R.drawable.fondo_halo_marcador);
        Drawable pin = teñir(contexto, R.drawable.ic_map_pin_grande, color);
        return new LayerDrawable(new Drawable[]{disco, pin});
    }

    private static Drawable teñir(Context contexto, int drawable, int color) {
        Drawable pin = ContextCompat.getDrawable(contexto, drawable);
        Drawable tenido = DrawableCompat.wrap(pin).mutate();
        DrawableCompat.setTint(tenido, color);
        return tenido;
    }
}
