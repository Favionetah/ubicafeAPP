package com.ubicafe.app.modelo;

import java.util.ArrayList;
import java.util.List;

/**
 * FICHA DE UN ESTABLECIMIENTO
 * ---------------------------------------------------------------
 * Datos que solo existen para los lugares que tienen cafetería: año de
 * apertura, mesas, baristas y tipo de establecimiento.
 *
 * De los 188 lugares con rol de cafetería, solo 66 tienen estos campos
 * completos. El resto llega con la ficha casi vacía, así que los bloques
 * de la interfaz que la usan deben ocultarse cuando no hay dato, nunca
 * mostrar un guion.
 */
public class DetalleCafeteria implements FichaDetalle {

    public final String anioApertura;
    public final String mesas;
    public final String capacidad;
    public final String baristas;
    public final String tipoEstablecimiento;
    public final String tipoApp;
    public final String direccion;
    public final String mapaUrl;

    public DetalleCafeteria(String anioApertura, String mesas, String capacidad,
                            String baristas, String tipoEstablecimiento, String tipoApp,
                            String direccion, String mapaUrl) {
        this.anioApertura = vacioONulo(anioApertura);
        this.mesas = vacioONulo(mesas);
        this.capacidad = vacioONulo(capacidad);
        this.baristas = vacioONulo(baristas);
        this.tipoEstablecimiento = vacioONulo(tipoEstablecimiento);
        this.tipoApp = vacioONulo(tipoApp);
        this.direccion = vacioONulo(direccion);
        this.mapaUrl = vacioONulo(mapaUrl);
    }

    /**
     * true si no hay ningún dato duro que mostrar. Decide si la ficha
     * merece un bloque propio o si solo queda la tarjeta básica.
     */
    @Override
    public boolean estaVacia() {
        return anioApertura.isEmpty() && mesas.isEmpty() && capacidad.isEmpty()
                && baristas.isEmpty() && tipoEstablecimiento.isEmpty();
    }

    /** Los pares etiqueta/valor que sí tienen dato, en orden de lectura. */
    @Override
    public List<ParDato> pares() {
        List<ParDato> pares = new ArrayList<>();
        agregar(pares, "Año de apertura", anioApertura);
        agregar(pares, "Tipo de establecimiento", tipoEstablecimiento);
        agregar(pares, "Mesas", mesas);
        agregar(pares, "Capacidad", capacidad);
        agregar(pares, "Baristas", baristas);
        return pares;
    }

    static void agregar(List<ParDato> pares, String etiqueta, String valor) {
        ParDato par = new ParDato(etiqueta, valor);
        if (par.tieneValor()) {
            pares.add(par);
        }
    }

    static String vacioONulo(String valor) {
        return valor == null ? "" : valor.trim();
    }
}
