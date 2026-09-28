package com.ubicafe.app.ui.mapa;

import com.ubicafe.app.datos.RepositorioDatos;
import com.ubicafe.app.modelo.Entidad;
import com.ubicafe.app.modelo.Marca;
import com.ubicafe.app.modelo.Rol;
import com.ubicafe.app.modelo.Sucursal;

import java.util.ArrayList;
import java.util.List;

/**
 * CONSTRUCCIÓN DE MARCADORES
 * ---------------------------------------------------------------
 * Un solo lugar decide qué se dibuja en el mapa, y lo hace siempre
 * igual. Hay tres formas de abrir el mapa y cada una usa un criterio
 * distinto:
 *
 *  - todos los lugares del censo;
 *  - los locales de una cadena;
 *  - el recorrido de una categoría (cafeterías, tostadurías...).
 *
 * Se omiten los lugares sin coordenadas. No se pueden dibujar, y meter
 * un marcador en (0,0) los mandaría todos al mar de Guinea.
 */
public final class MarcadoresMapa {

    private MarcadoresMapa() {
        // Clase de utilidades: no se instancia.
    }

    /** Los marcadores de toda una categoría, o de todos si el rol es null. */
    public static List<MarcadorMapa> porCategoria(Rol rol, int colorPorRol) {
        List<MarcadorMapa> marcadores = new ArrayList<>();
        for (Entidad lugar : RepositorioDatos.obtenerEntidades()) {
            if (!lugar.tieneCoordenadas()) {
                continue;
            }
            if (rol != null && !lugar.es(rol)) {
                continue;
            }
            marcadores.add(desdeEntidad(lugar, colorPorRol));
        }
        return marcadores;
    }

    /** Un marcador por cada lugar del censo que se puede dibujar. */
    public static List<MarcadorMapa> todos() {
        List<MarcadorMapa> marcadores = new ArrayList<>();
        for (Entidad lugar : RepositorioDatos.obtenerEntidades()) {
            if (lugar.tieneCoordenadas()) {
                marcadores.add(desdeEntidad(lugar, 0));
            }
        }
        return marcadores;
    }

    /**
     * Los locales de una cadena. Cada local es un marcador propio: son
     * direcciones distintas y quien busca "Typica" quiere el local que
     * tiene cerca, no la sede central.
     */
    public static List<MarcadorMapa> deMarca(String nombreMarca, int color) {
        List<MarcadorMapa> marcadores = new ArrayList<>();
        for (Sucursal sucursal : RepositorioDatos.obtenerSucursalesDe(nombreMarca)) {
            if (!sucursal.tieneCoordenadas()) {
                continue;
            }
            marcadores.add(new MarcadorMapa(
                    sucursal.nombre.isEmpty() ? sucursal.marca : sucursal.nombre,
                    sucursal.macrodistrito,
                    sucursal.lat, sucursal.lng,
                    Rol.TIENDA, color, sucursal.entidadId, false));
        }
        return marcadores;
    }

    /** Un marcador de un lugar concreto, para centrar el mapa en él. */
    public static MarcadorMapa desdeEntidad(Entidad lugar, int color) {
        return new MarcadorMapa(
                lugar.nombre, lugar.macrodistrito, lugar.lat, lugar.lng,
                lugar.rolPrincipal,
                color == 0 ? colorDe(lugar.rolPrincipal) : color,
                lugar.id,
                tieneUbicacionAvisada(lugar));
    }

    /**
     * true si el censo Mungió dejó dudas sobre la posición: o lo
     * escribió en el aviso, o no sabe a qué distancia quedó el punto.
     */
    public static boolean tieneUbicacionAvisada(Entidad lugar) {
        return !lugar.notaUbicacion.isEmpty() || lugar.precisionGps == null;
    }

    /**
     * El color de un pin por su papel. El azul se reserva para la
     * posición de la persona, así que no se usa aquí.
     */
    public static int colorDe(Rol rol) {
        switch (rol) {
            case CAFETERIA:
                return 0xFF164E3B;
            case MARCA:
                return 0xFF76513D;
            case TOSTADURIA:
                return 0xFF76513D;
            case PRODUCTOR:
                return 0xFF6B8F71;
            case TIENDA:
                return 0xFF1C4E3B;
            default:
                return 0xFF78817D;
        }
    }

    /**
     * La marca a la que pertenece un local, o null si es independiente.
     * La ficha del mapa usa esto para saber si "Ver información" debe
     * llevar a la cadena o al local.
     */
    public static String marcaDe(String entidadId) {
        Marca marca = RepositorioDatos.obtenerMarcaDe(entidadId);
        return marca == null ? null : marca.nombre;
    }
}
