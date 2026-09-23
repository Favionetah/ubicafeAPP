package com.ubicafe.app.modelo;

import java.util.List;

/**
 * Una cafetería registrada en La Paz.
 * Campo a campo:
 *  - nombre:     nombre del local (ej. "TYPICA - Achumani").
 *  - zona:        barrio o macrodistrito (ej. "Sur", "Centro", "Sopocachi").
 *  - direccion:   dirección callejera para el detalle.
 *  - horario:     texto corto de horarios (ej. "Lun-Sáb 8:00-20:00").
 *  - tipo:        clasificación (CAFE_ORIGEN, MARCA_NACIONAL o CLASICA).
 *  - marca:       marca asociada (ej. "TYPICA") o null.
 *  - fichaOrigen: cafés de especialidad que sirve, o lista vacía.
 *  - lat / lng:   coordenadas para el mapa (futuro Google Maps).
 */
public class Cafeteria {

    /** Tipos de cafetería (filtro semejante al diseño). */
    public static final String TIPO_CAFE_ORIGEN = "Café de origen";
    public static final String TIPO_MARCA_NACIONAL = "Marca nacional";
    public static final String TIPO_CLASICA = "Clásica";

    public final String nombre;
    public final String zona;
    public final String direccion;
    public final String horario;
    public final String tipo;
    public final String marca;
    public final List<CafeOrigen> fichaOrigen;
    public final double lat;
    public final double lng;

    public Cafeteria(String nombre, String zona, String direccion, String horario,
                     String tipo, String marca, List<CafeOrigen> fichaOrigen,
                     double lat, double lng) {
        this.nombre = nombre;
        this.zona = zona;
        this.direccion = direccion;
        this.horario = horario;
        this.tipo = tipo;
        this.marca = marca;
        this.fichaOrigen = fichaOrigen;
        this.lat = lat;
        this.lng = lng;
    }
}