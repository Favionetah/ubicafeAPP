package com.ubicafe.app.modelo;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * FICHA DE UNA TOSTADURÍA
 * ---------------------------------------------------------------
 * Datos de quien tuesta café. Son las 13 fichas más completas del censo:
 * año de inicio, kilos al mes, si compra café y de qué regiones vienen
 * los granos están respondidos en las trece. Los tipos de tueste, en
 * cambio, solo están en cuatro.
 *
 * La columna de kilos viene en formatos distintos ("10 KG", "300", "5 kg",
 * "no sabe"). Se conserva tal cual porque es la respuesta del negocio y
 * unificar unidades sería inventar.
 */
public class DetalleTostaderia implements FichaDetalle {

    public final String anioInicio;
    public final String kgTostadosMes;
    public final String compraCafe;
    public final String regionesOrigen;
    public final String tiposTueste;
    public final String direccion;
    public final String mapaUrl;

    public DetalleTostaderia(String anioInicio, String kgTostadosMes, String compraCafe,
                             String regionesOrigen, String tiposTueste,
                             String direccion, String mapaUrl) {
        this.anioInicio = DetalleCafeteria.vacioONulo(anioInicio);
        this.kgTostadosMes = DetalleCafeteria.vacioONulo(kgTostadosMes);
        this.compraCafe = DetalleCafeteria.vacioONulo(compraCafe);
        this.regionesOrigen = DetalleCafeteria.vacioONulo(regionesOrigen);
        this.tiposTueste = DetalleCafeteria.vacioONulo(tiposTueste);
        this.direccion = DetalleCafeteria.vacioONulo(direccion);
        this.mapaUrl = DetalleCafeteria.vacioONulo(mapaUrl);
    }

    public boolean compraCafe() {
        return "Sí".equalsIgnoreCase(compraCafe);
    }

    @Override
    public boolean estaVacia() {
        return anioInicio.isEmpty() && kgTostadosMes.isEmpty() && compraCafe.isEmpty()
                && regionesOrigen.isEmpty() && tiposTueste.isEmpty();
    }

    /**
     * Las regiones de origen separadas en una lista, para poder filtrar
     * por una sola. El censo las escribió unidas en un campo ("Caranavi,
     * Tarija"), así que hay que partirlas. Se ignoran las mayúsculas
     * repetidas para que "Yungas, Yungas" cuente una vez.
     */
    public List<String> regionesOrigenComoLista() {
        List<String> regiones = new ArrayList<>();
        Set<String> vistas = new LinkedHashSet<>();
        for (String parte : regionesOrigen.split("[,;/]| y | - ")) {
            String limpia = parte.trim();
            if (limpia.isEmpty()) {
                continue;
            }
            if (vistas.add(limpia.toLowerCase(java.util.Locale.ROOT))) {
                regiones.add(limpia);
            }
        }
        return regiones;
    }

    @Override
    public List<ParDato> pares() {
        List<ParDato> pares = new ArrayList<>();
        DetalleCafeteria.agregar(pares, "Año de inicio", anioInicio);
        DetalleCafeteria.agregar(pares, "Kilos tostados por mes", kgTostadosMes);
        DetalleCafeteria.agregar(pares, "Compra café", compraCafe);
        DetalleCafeteria.agregar(pares, "Regiones de origen", regionesOrigen);
        DetalleCafeteria.agregar(pares, "Tipos de tueste", tiposTueste);
        return pares;
    }
}
