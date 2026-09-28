package com.ubicafe.app.modelo;

import java.util.ArrayList;
import java.util.List;

/**
 * FICHA DE UNA MARCA DE CAFÉ
 * ---------------------------------------------------------------
 * Datos que el censo recogió de una marca. La mayoría están vacíos: de
 * las 40 marcas del catálogo, solo "es nacional" tiene respuesta en
 * todas, y el canal de comercialización y la cobertura, en cuatro. La
 * ficha tiene que poder mostrarse casi vacía sin romper la pantalla.
 */
public class DetalleMarca implements FichaDetalle {

    public final String esNacional;
    public final String anioCreacion;
    public final String lugarCreacion;
    public final String registradaSenapi;
    public final String segmento;
    public final String nProductos;
    public final String municipioOrigen;
    public final String canalComercializacion;
    public final String cobertura;

    public DetalleMarca(String esNacional, String anioCreacion, String lugarCreacion,
                        String registradaSenapi, String segmento, String nProductos,
                        String municipioOrigen, String canalComercializacion,
                        String cobertura) {
        this.esNacional = DetalleCafeteria.vacioONulo(esNacional);
        this.anioCreacion = DetalleCafeteria.vacioONulo(anioCreacion);
        this.lugarCreacion = DetalleCafeteria.vacioONulo(lugarCreacion);
        this.registradaSenapi = DetalleCafeteria.vacioONulo(registradaSenapi);
        this.segmento = DetalleCafeteria.vacioONulo(segmento);
        this.nProductos = DetalleCafeteria.vacioONulo(nProductos);
        this.municipioOrigen = DetalleCafeteria.vacioONulo(municipioOrigen);
        this.canalComercializacion = DetalleCafeteria.vacioONulo(canalComercializacion);
        this.cobertura = DetalleCafeteria.vacioONulo(cobertura);
    }

    /** La marca es de Bolivia si el censo lo respondió afirmativamente. */
    public boolean esNacional() {
        return "Sí".equalsIgnoreCase(esNacional);
    }

    /** true si la marca viene de fuera del país. */
    public boolean esImportada() {
        return "No".equalsIgnoreCase(esNacional);
    }

    @Override
    public boolean estaVacia() {
        return anioCreacion.isEmpty() && lugarCreacion.isEmpty()
                && registradaSenapi.isEmpty() && segmento.isEmpty()
                && nProductos.isEmpty() && municipioOrigen.isEmpty()
                && canalComercializacion.isEmpty() && cobertura.isEmpty();
    }

    @Override
    public List<ParDato> pares() {
        List<ParDato> pares = new ArrayList<>();
        DetalleCafeteria.agregar(pares, "Canal de comercialización", canalComercializacion);
        DetalleCafeteria.agregar(pares, "Cobertura", cobertura);
        DetalleCafeteria.agregar(pares, "Segmento", segmento);
        DetalleCafeteria.agregar(pares, "N. de productos", nProductos);
        DetalleCafeteria.agregar(pares, "Año de creación", anioCreacion);
        DetalleCafeteria.agregar(pares, "Lugar de creación", lugarCreacion);
        DetalleCafeteria.agregar(pares, "Registrada en SENAPI", registradaSenapi);
        DetalleCafeteria.agregar(pares, "Municipio o región de origen", municipioOrigen);
        return pares;
    }
}
