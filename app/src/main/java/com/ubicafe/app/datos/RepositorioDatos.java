package com.ubicafe.app.datos;

import com.ubicafe.app.modelo.CafeOrigen;
import com.ubicafe.app.modelo.Cafeteria;
import com.ubicafe.app.modelo.MarcaCafe;
import com.ubicafe.app.modelo.Productor;
import com.ubicafe.app.modelo.PuntoVenta;
import com.ubicafe.app.modelo.ResultadoBusqueda;
import com.ubicafe.app.modelo.Tostaderia;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * PUNTO ÚNICO DE ACCESO A LOS DATOS.
 * ---------------------------------------------------------------
 * Las pantallas NUNCA leen DatosEjemplo directamente: siempre pasan
 * por aquí. Así, cuando exista una API o una base de datos, solo hay
 * que cambiar el interior de estos métodos y las pantallas seguirán
 * funcionando igual.
 * ---------------------------------------------------------------
 */
public class RepositorioDatos {

    // ---- CAFETERÍAS -------------------------------------------------

    public static List<Cafeteria> obtenerCafeterias() {
        return DatosEjemplo.obtenerCafeterias();
    }

    /** Filtra por zona (ej. "Centro", "Sur", "Sopocachi"). Vacío = todas. */
    public static List<Cafeteria> obtenerCafeteriasPorZona(String zona) {
        List<Cafeteria> resultado = new ArrayList<>();
        for (Cafeteria c : obtenerCafeterias()) {
            if (zona == null || zona.isEmpty() || c.zona.equalsIgnoreCase(zona)) {
                resultado.add(c);
            }
        }
        return resultado;
    }

    /** Filtra por tipo (Café de origen / Marca nacional / Clásica). */
    public static List<Cafeteria> obtenerCafeteriasPorTipo(String tipo) {
        List<Cafeteria> resultado = new ArrayList<>();
        for (Cafeteria c : obtenerCafeterias()) {
            if (tipo == null || tipo.isEmpty() || c.tipo.equals(tipo)) {
                resultado.add(c);
            }
        }
        return resultado;
    }

    /** Busca una cafetería por su nombre (sin importar mayúsculas). */
    public static Cafeteria obtenerCafeteria(String nombre) {
        for (Cafeteria cafeteria : obtenerCafeterias()) {
            if (cafeteria.nombre.equalsIgnoreCase(nombre)) {
                return cafeteria;
            }
        }
        return null;
    }

    /** Todos los cafés de especialidad de origen registrados. */
    public static List<CafeOrigen> obtenerCafesDeOrigen() {
        return DatosEjemplo.obtenerCafesDeOrigen();
    }

    /** Busca un café de especialidad por su variedad. */
    public static CafeOrigen obtenerCafePorVariedad(String variedad) {
        for (CafeOrigen cafe : DatosEjemplo.obtenerCafesDeOrigen()) {
            if (cafe.variedad.equalsIgnoreCase(variedad)) {
                return cafe;
            }
        }
        return null;
    }

    // ---- MARCAS ------------------------------------------------------

    public static List<MarcaCafe> obtenerMarcas() {
        return DatosEjemplo.obtenerMarcas();
    }

    /** Busca una marca por su nombre exacto (sin importar mayúsculas). */
    public static MarcaCafe obtenerMarca(String nombre) {
        for (MarcaCafe marca : obtenerMarcas()) {
            if (marca.nombre.equalsIgnoreCase(nombre)) {
                return marca;
            }
        }
        return null;
    }

    /** Número de puntos de venta que tiene una marca (se calcula de los registros). */
    public static int contarPuntosDeVentaDe(String nombreMarca) {
        int contador = 0;
        for (PuntoVenta p : obtenerPuntosDeVenta()) {
            if (p.marca.equalsIgnoreCase(nombreMarca)) {
                contador++;
            }
        }
        return contador;
    }

    // ---- TOSTADURÍAS -------------------------------------------------

    public static List<Tostaderia> obtenerTostaderias() {
        return DatosEjemplo.obtenerTostaderias();
    }

    /** Filtra por región (La Paz | Yungas | Tarija). Vacío = todas. */
    public static List<Tostaderia> obtenerTostaderiasPorRegion(String region) {
        List<Tostaderia> resultado = new ArrayList<>();
        for (Tostaderia t : obtenerTostaderias()) {
            if (region == null || region.isEmpty() || t.region.equalsIgnoreCase(region)) {
                resultado.add(t);
            }
        }
        return resultado;
    }

    /** Busca una tostaduría por su nombre. */
    public static Tostaderia obtenerTostaderia(String nombre) {
        for (Tostaderia tostaderia : obtenerTostaderias()) {
            if (tostaderia.nombre.equalsIgnoreCase(nombre)) {
                return tostaderia;
            }
        }
        return null;
    }

    // ---- PRODUCTORES -------------------------------------------------

    public static List<Productor> obtenerProductores() {
        return DatosEjemplo.obtenerProductores();
    }

    /** Busca un productor por el nombre de su finca. */
    public static Productor obtenerProductor(String nombreFinca) {
        for (Productor productor : obtenerProductores()) {
            if (productor.nombreFinca.equalsIgnoreCase(nombreFinca)) {
                return productor;
            }
        }
        return null;
    }

    // ---- PUNTOS DE VENTA ---------------------------------------------

    public static List<PuntoVenta> obtenerPuntosDeVenta() {
        return DatosEjemplo.obtenerPuntosDeVenta();
    }

    public static List<PuntoVenta> obtenerPuntosDeVentaDe(String nombreMarca) {
        List<PuntoVenta> resultado = new ArrayList<>();
        for (PuntoVenta p : obtenerPuntosDeVenta()) {
            if (p.marca.equalsIgnoreCase(nombreMarca)) {
                resultado.add(p);
            }
        }
        return resultado;
    }

    // ---- ECOSISTEMA (cifras del dashboard) ---------------------------

    public static int contarLugares() {
        return obtenerCafeterias().size() + obtenerPuntosDeVenta().size();
    }

    public static int contarMarcas() {
        return obtenerMarcas().size();
    }

    public static int contarTostaderias() {
        return obtenerTostaderias().size();
    }

    public static int contarProductores() {
        return obtenerProductores().size();
    }

    // ---- BÚSQUEDA GLOBAL ----------------------------------------------

    /**
     * Busca un texto en cafés, marcas y cafeterías.
     * Compara sin distinguir mayúsculas ni acentos.
     */
    public static ResultadoBusqueda buscar(String texto) {
        ResultadoBusqueda resultado = new ResultadoBusqueda();
        if (texto == null || texto.trim().isEmpty()) {
            return resultado;
        }

        String consulta = normalizar(texto);

        // Coincidencias en cafés de especialidad
        for (CafeOrigen cafe : DatosEjemplo.obtenerCafesDeOrigen()) {
            if (normalizar(cafe.variedad).contains(consulta)) {
                resultado.cafesDeEspecialidad.add(cafe);
            }
        }

        // Coincidencias en marcas registradas
        for (MarcaCafe marca : obtenerMarcas()) {
            if (normalizar(marca.nombre).contains(consulta)) {
                resultado.marcasRegistradas.add(marca);
            }
        }

        // Coincidencias en cafeterías (por nombre de local o de marca)
        for (Cafeteria cafeteria : obtenerCafeterias()) {
            String nombreLocal = cafeteria.marca == null ? cafeteria.nombre
                    : cafeteria.nombre + " " + cafeteria.marca;
            if (normalizar(nombreLocal).contains(consulta)) {
                resultado.establecimientos.add(cafeteria);
            }
        }
        return resultado;
    }

    // ---- UTILIDAD INTERNA ----------------------------------------------

    /** Quita acentos y pasa a minúsculas para comparar fácil. */
    private static String normalizar(String texto) {
        return java.text.Normalizer.normalize(texto, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.getDefault());
    }
}