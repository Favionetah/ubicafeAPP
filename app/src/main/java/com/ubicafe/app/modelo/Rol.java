package com.ubicafe.app.modelo;

import com.ubicafe.app.R;

/**
 * ROL QUE JUEGA UN LUGAR EN EL ECOSISTEMA DEL CAFÉ
 * ---------------------------------------------------------------
 * Un mismo lugar puede cumplir varios roles a la vez: hay cafeterías
 * que además tuestan, marcas que tienen fincas y tiendas que venden
 * café de otras cadenas. Por eso el rol no es una jerarquía sino un
 * conjunto, y cada entidad guarda el suyo principal aparte.
 *
 * El orden de las constantes es el orden en que se muestran los roles
 * en la ficha: primero lo que la persona ve, después lo más técnico.
 */
public enum Rol {

    CAFETERIA("Cafetería", R.drawable.ic_cafeteria, R.color.verde_oscuro),
    MARCA("Marca de café", R.drawable.ic_marca, R.color.cafe_accent),
    TOSTADURIA("Tostaduría", R.drawable.ic_tostaderia, R.color.caramelo_claro),
    PRODUCTOR("Productor", R.drawable.ic_productor, R.color.verde_medio),
    TIENDA("Tienda de café", R.drawable.ic_puntosventa, R.color.verde_claro),
    OTRO("Otros", R.drawable.ic_otro, R.color.texto_tenue);

    private final String etiqueta;
    private final int icono;
    private final int color;

    Rol(String etiqueta, int icono, int color) {
        this.etiqueta = etiqueta;
        this.icono = icono;
        this.color = color;
    }

    /** Nombre corto para chips y listas: "Cafetería", "Marca", "Tostaduría". */
    public String etiquetaCorta() {
        switch (this) {
            case MARCA:
                return "Marca";
            default:
                return etiqueta;
        }
    }

    public String etiqueta() {
        return etiqueta;
    }

    public int icono() {
        return icono;
    }

    public int color() {
        return color;
    }
}
