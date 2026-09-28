package com.ubicafe.app.modelo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * CIFRAS PÚBLICAS DEL SECTOR
 * ---------------------------------------------------------------
 * El conteo de todo lo que hay en el censo, calculado una vez al
 * cargar los datos. La interfaz nunca escribe un número a mano: pide
 * estos totales, que salen del contenido real del censo.
 *
 * Existe un bloque de precio en el diseño original, pero el censo no
 * preguntó precios en ninguna de sus dos hojas, así que aquí no existe
 * ese campo y la app no puede mostrarlo. Cuando exista el dato, se
 * añade a esta clase y al JSON, no a un layout.
 */
public class Estadisticas {

    public final int totalEntidades;
    public final Map<Rol, Integer> porRol;
    public final Map<Rol, Integer> porRolPrincipal;
    public final Map<String, Integer> porMacrodistrito;

    public final int cafeterias;
    public final int marcas;
    public final int puntosDeVenta;
    public final int tostadurias;
    public final int productores;
    public final int variedades;

    public final int conCoordenadas;
    public final int conDetalleCafeteria;
    public final int conDetalleMarca;
    public final int conDetalleTostaderia;
    public final int conDetalleProductor;
    public final int conVariasFichas;

    public Estadisticas(List<Entidad> entidades, List<Marca> marcas,
                        List<CafeVariedad> variedades) {
        this.totalEntidades = entidades.size();
        this.marcas = marcas.size();
        this.variedades = variedades.size();

        Map<Rol, Integer> roles = new LinkedHashMap<>();
        Map<Rol, Integer> principales = new LinkedHashMap<>();
        Map<String, Integer> macrodistritos = new LinkedHashMap<>();
        for (Rol rol : Rol.values()) {
            roles.put(rol, 0);
            principales.put(rol, 0);
        }

        int coordenadas = 0;
        int detalleCafeteria = 0;
        int detalleMarca = 0;
        int detalleTostaderia = 0;
        int detalleProductor = 0;
        int variasFichas = 0;

        for (Entidad entidad : entidades) {
            for (Rol rol : entidad.roles) {
                roles.put(rol, roles.get(rol) + 1);
            }
            principales.put(entidad.rolPrincipal, principales.get(entidad.rolPrincipal) + 1);

            String macrodistrito = entidad.macrodistrito.isEmpty()
                    ? "Sin macrodistrito" : entidad.macrodistrito;
            macrodistritos.put(macrodistrito, macrodistritos.getOrDefault(macrodistrito, 0) + 1);

            if (entidad.tieneCoordenadas()) {
                coordenadas++;
            }
            if (entidad.detalleCafeteria != null && !entidad.detalleCafeteria.estaVacia()) {
                detalleCafeteria++;
            }
            if (entidad.detalleMarca != null && !entidad.detalleMarca.estaVacia()) {
                detalleMarca++;
            }
            if (entidad.detalleTostaderia != null && !entidad.detalleTostaderia.estaVacia()) {
                detalleTostaderia++;
            }
            if (entidad.detalleProductor != null && !entidad.detalleProductor.estaVacia()) {
                detalleProductor++;
            }
            if (entidad.cantidadDeFichas() > 1) {
                variasFichas++;
            }
        }

        this.porRol = Collections.unmodifiableMap(roles);
        this.porRolPrincipal = Collections.unmodifiableMap(principales);
        this.porMacrodistrito = Collections.unmodifiableMap(macrodistritos);

        this.cafeterias = roles.get(Rol.CAFETERIA);
        this.productores = roles.get(Rol.PRODUCTOR);
        this.tostadurias = roles.get(Rol.TOSTADURIA);
        // Los puntos de venta son las sucursales de las cadenas, no las
        // entidades con rol de tienda: son dos cosas distintas y el censo
        // las cuenta por separado (78 sucursales y 4 tiendas).
        this.puntosDeVenta = sumaSucursales(marcas);
        this.conCoordenadas = coordenadas;
        this.conDetalleCafeteria = detalleCafeteria;
        this.conDetalleMarca = detalleMarca;
        this.conDetalleTostaderia = detalleTostaderia;
        this.conDetalleProductor = detalleProductor;
        this.conVariasFichas = variasFichas;
    }

    private static int sumaSucursales(List<Marca> marcas) {
        int total = 0;
        for (Marca marca : marcas) {
            total += marca.sucursales.size();
        }
        return total;
    }

    /** Los macrodistritos de más a menos poblados, para el gráfico de barras. */
    public List<String> macrodistritosOrdenados() {
        List<String> zonas = new ArrayList<>(porMacrodistrito.keySet());
        Collections.sort(zonas, (a, b) -> Integer.compare(
                porMacrodistrito.get(b), porMacrodistrito.get(a)));
        return zonas;
    }

    public int lugaresDe(String macrodistrito) {
        return porMacrodistrito.getOrDefault(macrodistrito, 0);
    }

    /** El mayor conteo de macrodistrito, para escalar el gráfico. */
    public int maximoMacrodistrito() {
        int maximo = 0;
        for (Integer cantidad : porMacrodistrito.values()) {
            maximo = Math.max(maximo, cantidad);
        }
        return maximo;
    }
}
