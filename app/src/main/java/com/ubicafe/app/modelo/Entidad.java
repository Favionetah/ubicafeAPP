package com.ubicafe.app.modelo;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * UN LUGAR DEL CENSO
 * ---------------------------------------------------------------
 * Esta es la unidad central de la app, no "la cafetería". El censo
 * registró a 213 lugares que pueden ser varias cosas a la vez: hay
 * 17 que son cafetería y tostaduría al mismo tiempo, y una que además
 * tiene marca y cafetales propios. Si el modelo tratara cada rol por
 * separado, esos 17 registros aparecerían duplicados o se perderían.
 *
 * Por eso una Entidad tiene un conjunto de roles y, aparte, cuál de
 * ellos manda: ese es el que decide a qué pantalla de detalle lleva y
 * qué color la representa.
 *
 * Las cuatro fichas de detalle pueden ser null. De los 213 lugares, 138
 * no respondieron la sección de detalle, así que la interfaz tiene que
 * poder trabajar con una Entidad que solo tiene nombre, dirección,
 * macrodistrito, coordenadas y roles.
 *
 * Todos los campos son inmutables: el censo no cambia mientras la app
 * está abierta, y un objeto inmutable se puede compartir entre hilos
 * sin riesgo.
 */
public class Entidad {

    public final String id;
    public final String nombre;
    public final String direccion;
    public final String mapaUrl;
    public final String macrodistrito;
    public final double lat;
    public final double lng;
    public final Set<Rol> roles;
    public final Rol rolPrincipal;
    public final String marcaAsociada;
    public final String nota;
    public final Double precisionGps;
    public final String notaUbicacion;

    public final DetalleCafeteria detalleCafeteria;
    public final DetalleMarca detalleMarca;
    public final DetalleTostaderia detalleTostaderia;
    public final DetalleProductor detalleProductor;

    /**
     * La foto del local, como ruta dentro de los assets
     * ("fotos/cafes/cafe_berna_linares_947.jpg"), o "" si no tiene.
     *
     * Es una ruta y no la foto entera a propósito: la base de datos
     * guarda el nombre del archivo y los bytes viven en los assets. Así
     * consultar el censo no carga 23 MB de imágenes en memoria, y cambiar
     * la foto de un local no obliga a reescribir su fila.
     */
    public final String foto;

    public Entidad(String id, String nombre, String direccion, String mapaUrl,
                   String macrodistrito, double lat, double lng, Set<Rol> roles,
                   Rol rolPrincipal, String marcaAsociada, String nota,
                   Double precisionGps, String notaUbicacion,
                   DetalleCafeteria detalleCafeteria, DetalleMarca detalleMarca,
                   DetalleTostaderia detalleTostaderia, DetalleProductor detalleProductor,
                   String foto) {
        this.id = id;
        this.nombre = nombre;
        this.direccion = vacioONulo(direccion);
        this.mapaUrl = vacioONulo(mapaUrl);
        this.macrodistrito = vacioONulo(macrodistrito);
        this.lat = lat;
        this.lng = lng;
        this.roles = Collections.unmodifiableSet(new LinkedHashSet<>(roles));
        this.rolPrincipal = rolPrincipal;
        this.marcaAsociada = vacioONulo(marcaAsociada);
        this.nota = vacioONulo(nota);
        this.precisionGps = precisionGps;
        this.notaUbicacion = vacioONulo(notaUbicacion);
        this.detalleCafeteria = detalleCafeteria;
        this.detalleMarca = detalleMarca;
        this.detalleTostaderia = detalleTostaderia;
        this.detalleProductor = detalleProductor;
        this.foto = foto == null ? "" : foto;
    }

    /** El lugar tiene coordenadas usables y puede dibujarse en el mapa. */
    public boolean tieneCoordenadas() {
        return lat != 0.0 || lng != 0.0;
    }

    public boolean es(Rol rol) {
        return roles.contains(rol);
    }

    /** true si el lugar lleva más de un papel en el ecosistema. */
    public boolean esMultirrol() {
        return roles.size() > 1;
    }

    /** Los roles en el orden en que se muestran: "Cafetería · Marca". */
    public String rolesComoTexto() {
        StringBuilder texto = new StringBuilder();
        for (Rol rol : roles) {
            if (texto.length() > 0) {
                texto.append(" · ");
            }
            texto.append(rol.etiquetaCorta());
        }
        return texto.toString();
    }

    /**
     * La primera letra del nombre, para el marcador de color de las
     * tarjetas cuando el censo no tiene foto.
     */
    public char inicial() {
        return nombre.isEmpty() ? '?' : Character.toUpperCase(nombre.charAt(0));
    }

    /**
     * Un color estable derivado del nombre, para que una cafetería
     * conserve el mismo tono en todas las pantallas. El tono se elige
     * de los verdes de la paleta de marca.
     */
    public int colorMarca() {
        int suma = 0;
        for (int i = 0; i < nombre.length(); i++) {
            suma += nombre.charAt(i);
        }
        int[] paleta = {
                0xFF6B8F71, 0xFF1C4E3B, 0xFF76513D, 0xFF4E7A5B, 0xFF5C7A6B, 0xFF8A6F4E
        };
        return paleta[Math.abs(suma) % paleta.length];
    }

    /** true si la ficha de cafetería no trae datos que mostrar. */
    public boolean sinDetalleCafeteria() {
        return detalleCafeteria == null || detalleCafeteria.estaVacia();
    }

    /** Las notas de cata del productor, o una lista vacía si no es productor. */
    public List<String> notasDeCata() {
        return detalleProductor == null
                ? Collections.<String>emptyList()
                : detalleProductor.notasComoLista();
    }

    /** Cuántas fichas de detalle trae la entidad, para saber qué bloques mostrar. */
    public int cantidadDeFichas() {
        int total = 0;
        if (detalleCafeteria != null && !detalleCafeteria.estaVacia()) {
            total++;
        }
        if (detalleMarca != null && !detalleMarca.estaVacia()) {
            total++;
        }
        if (detalleTostaderia != null && !detalleTostaderia.estaVacia()) {
            total++;
        }
        if (detalleProductor != null && !detalleProductor.estaVacia()) {
            total++;
        }
        return total;
    }

    private static String vacioONulo(String valor) {
        return valor == null ? "" : valor.trim();
    }
}
