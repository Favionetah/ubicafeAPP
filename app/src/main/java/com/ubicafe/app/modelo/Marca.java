package com.ubicafe.app.modelo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * UNA MARCA DE CAFÉ Y SUS SUCURSALES
 * ---------------------------------------------------------------
 * Una cadena o un nombre comercial del censo. Hay 40, y 78 sucursales
 * repartidas entre ellas. La marca no es un lugar: no tiene dirección ni
 * coordenadas propias, así que su ficha se construye a partir de sus
 * sucursales y de los pocos campos que el censo respondió.
 */
public class Marca {

    public final String nombre;
    public final DetalleMarca detalle;
    public final String nota;
    public final List<Sucursal> sucursales;

    /**
     * La foto de la cadena, como ruta dentro de los assets
     * ("fotos/marcas/typica.jpg"), o "" si no tiene. Misma decisión que
     * en Entidad: la base guarda el nombre, los bytes van en los assets.
     */
    public final String foto;

    public Marca(String nombre, DetalleMarca detalle, String nota,
                 List<Sucursal> sucursales, String foto) {
        this.nombre = nombre == null ? "" : nombre.trim();
        this.detalle = detalle;
        this.nota = nota == null ? "" : nota.trim();
        this.sucursales = Collections.unmodifiableList(sucursales);
        this.foto = foto == null ? "" : foto;
    }

    public boolean tieneSucursales() {
        return !sucursales.isEmpty();
    }

    /** Los macrodistritos donde la marca tiene local, sin repetir. */
    public List<String> zonasDondeOpera() {
        LinkedHashSet<String> zonas = new LinkedHashSet<>();
        for (Sucursal sucursal : sucursales) {
            if (!sucursal.macrodistrito.isEmpty()) {
                zonas.add(sucursal.macrodistrito);
            }
        }
        return new ArrayList<>(zonas);
    }

    public char inicial() {
        return nombre.isEmpty() ? '?' : Character.toUpperCase(nombre.charAt(0));
    }
}
