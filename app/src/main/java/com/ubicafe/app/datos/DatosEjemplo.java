package com.ubicafe.app.datos;

import com.ubicafe.app.R;
import com.ubicafe.app.modelo.CafeOrigen;
import com.ubicafe.app.modelo.Cafeteria;
import com.ubicafe.app.modelo.MarcaCafe;
import com.ubicafe.app.modelo.Productor;
import com.ubicafe.app.modelo.PuntoVenta;
import com.ubicafe.app.modelo.Tostaderia;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * DATOS DE EJEMPLO.
 * ---------------------------------------------------------------
 * Esta clase contiene TODA la información que muestra la app
 * (cafeterías, marcas, productores, tostadurías y puntos de venta).
 *
 * Por ahora son datos locales (sin internet). Cuando haya una API
 * o base de datos, solo se reescribe esta clase (o RepositorioDatos)
 * sin tocar ninguna pantalla.
 * ---------------------------------------------------------------
 *
 * Formato de escritura de cada bloque:
 *   // 1. CAFÉS DE ORIGEN (las fichas de especialidad reutilizables)
 */
public class DatosEjemplo {

    // ------------------------------------------------------------------
    // 1. CAFÉS DE ORIGEN (fichas de variedades de especialidad)
    // ------------------------------------------------------------------
    private static final CafeOrigen GEISHA = new CafeOrigen(
            "GEISHA", "Caranavi, La Paz", 1600, "Lavado", "Medio",
            "Jazmín, frutas tropicales",
            "Floral, dulce, cuerpo sedoso");

    private static final CafeOrigen CATURRA = new CafeOrigen(
            "CATURRA", "Caranavi, La Paz", 1500, "Lavado", "Medio",
            "Manzana verde, miel",
            "Cítrico, caramelizado, acidez brillante");

    private static final CafeOrigen TYPICA_YUNGAS = new CafeOrigen(
            "TYPICA", "Coroico, Yungas", 1450, "Semi-lavado", "Oscuro",
            "Cacao, nuez",
            "Chocolatoso, cuerpo cremoso");

    private static final CafeOrigen CATIMOR = new CafeOrigen(
            "CATIMOR", "Chulumani, Yungas", 1400, "Lavado", "Medio",
            "Durazno, miel de caña",
            "Frutal, dulce y redondo");

    private static final CafeOrigen BOURBON = new CafeOrigen(
            "BOURBON", "Caranavi, La Paz", 1580, "Natural", "Medio-claro",
            "Fresa, cítricos",
            "Frutado, vinoso, final suave");

    private static final CafeOrigen PACAMARA = new CafeOrigen(
            "PACAMARA", "Yungas - La Paz", 1550, "Lavado", "Medio",
            "Flores blancas, mermelada",
            "Dulce intenso, cuerpo medio-alto");

    private static final List<CafeOrigen> TODO_LOS_CAFES = new ArrayList<>(Arrays.asList(
            GEISHA, CATURRA, TYPICA_YUNGAS, CATIMOR, BOURBON, PACAMARA));

    // ------------------------------------------------------------------
    // 2. MARCAS DE CAFÉ NACIONAL
    // ------------------------------------------------------------------
    private static final List<MarcaCafe> MARCAS = new ArrayList<>(Arrays.asList(
            new MarcaCafe("TYPICA",
                    Arrays.asList("Cafetería", "Tostaduría", "Productor"), true,
                    "Marca paceña que impulsa el café boliviano de especialidad.",
                    Arrays.asList(GEISHA, CATURRA),
                    R.color.verde_oscuro, 'T'),

            new MarcaCafe("Roaster",
                    Arrays.asList("Cafetería", "Tostaduría", "Productor"), true,
                    "Tostaduría artesanal de granos seleccionados en los Yungas.",
                    Arrays.asList(TYPICA_YUNGAS, CATIMOR),
                    R.color.cafe_accent, 'R'),

            new MarcaCafe("Mugen",
                    Arrays.asList("Cafetería", "Tostaduría", "Productor"), true,
                    "Café de especialidad con toques japoneses en La Paz.",
                    Arrays.asList(BOURBON),
                    R.color.verde_claro, 'M'),

            new MarcaCafe("Café Yungas",
                    Arrays.asList("Productor"), true,
                    "Productora yngueña con distribución en toda la ciudad.",
                    Arrays.asList(CATIMOR, TYPICA_YUNGAS),
                    R.color.caramelo_claro, 'Y'),

            new MarcaCafe("Café de la Finca",
                    Arrays.asList("Productor"), true,
                    "Café de finca directa, de la tierra al vaso.",
                    Arrays.asList(PACAMARA),
                    R.color.texto_secundario, 'F')));

    // ------------------------------------------------------------------
    // 3. CAFETERÍAS DE LA PAZ
    // ------------------------------------------------------------------
    private static final List<Cafeteria> CAFETERIAS = new ArrayList<>(Arrays.asList(
            new Cafeteria("TYPICA - Achumani", "Sur", "Av. Costanera, Achumani",
                    "Lun-Dom 8:00-21:00", Cafeteria.TIPO_CAFE_ORIGEN, "TYPICA",
                    Arrays.asList(GEISHA, CATURRA), -16.535, -68.070),

            new Cafeteria("TYPICA - San Miguel", "Sur", "Av. Ballivián, San Miguel",
                    "Lun-Dom 8:00-21:00", Cafeteria.TIPO_CAFE_ORIGEN, "TYPICA",
                    Arrays.asList(GEISHA), -16.521, -68.083),

            new Cafeteria("TYPICA - Miraflores", "Miraflores", "Av. Busch, Miraflores",
                    "Lun-Dom 8:30-20:30", Cafeteria.TIPO_CAFE_ORIGEN, "TYPICA",
                    Arrays.asList(CATURRA), -16.501, -68.113),

            new Cafeteria("Roaster - Sopocachi", "Sopocachi", "Av. 6 de Agosto",
                    "Lun-Sáb 9:00-21:00", Cafeteria.TIPO_MARCA_NACIONAL, "Roaster",
                    Arrays.asList(TYPICA_YUNGAS, CATIMOR), -16.512, -68.124),

            new Cafeteria("Mugen - Centro", "Centro", "Calle Sagárnaga",
                    "Lun-Dom 9:00-22:00", Cafeteria.TIPO_MARCA_NACIONAL, "Mugen",
                    Arrays.asList(BOURBON), -16.495, -68.133),

            new Cafeteria("Café Yungas - Camacho", "Centro", "Av. Camacho esq. Colón",
                    "Lun-Sáb 8:00-19:30", Cafeteria.TIPO_MARCA_NACIONAL, "Café Yungas",
                    Arrays.asList(CATIMOR), -16.497, -68.137),

            new Cafeteria("Café de la Finca - Calacoto", "Sur", "Av. Costanera, Calacoto",
                    "Lun-Dom 9:00-20:00", Cafeteria.TIPO_MARCA_NACIONAL, "Café de la Finca",
                    Arrays.asList(PACAMARA), -16.538, -68.076),

            new Cafeteria("La Casona Café", "Centro", "Calle Jaén",
                    "Lun-Dom 10:00-20:00", Cafeteria.TIPO_CLASICA, null,
                    new ArrayList<>(), -16.490, -68.131),

            new Cafeteria("Altura Café", "Sur", "Av. La Florida",
                    "Lun-Sáb 8:30-19:00", Cafeteria.TIPO_CLASICA, null,
                    new ArrayList<>(), -16.527, -68.055)));

    // ------------------------------------------------------------------
    // 4. PRODUCTORES (fincas cafetaleras)
    // ------------------------------------------------------------------
    private static final List<Productor> PRODUCTORES = new ArrayList<>(Arrays.asList(
            new Productor("Finca El Cóndor", "Familia Mamani", "Caranavi, La Paz",
                    3, 1600, "Productor de origen de las laderas del río Yara.",
                    R.color.verde_oscuro, 'C', -15.833, -67.567),

            new Productor("Finca La Primavera", "Familia Ramírez", "Coroico, Yungas",
                    4, 1500, "Cafetales bajo sombra de árboles nativos.",
                    R.color.cafe_accent, 'P', -16.190, -67.727),

            new Productor("Finca Alto Sajama", "Familia Choque", "Caranavi, La Paz",
                    2, 1700, "Finca de altura con recolección 100% manual.",
                    R.color.verde_claro, 'A', -15.800, -67.600)));

    // ------------------------------------------------------------------
    // 5. TOSTADURÍAS (regiones: La Paz, Yungas, Tarija)
    // ------------------------------------------------------------------
    private static final List<Tostaderia> TOSTADERIAS = new ArrayList<>(Arrays.asList(
            new Tostaderia("Tostaduría ROC", "La Paz", "Zona Sopocachi",
                    "Lun-Vie 9:00-18:00",
                    "Tostamos en pequeños lotes para resaltar el origen de cada grano boliviano.",
                    "info@roc.bo", R.color.cafe_accent, 'R', -16.512, -68.124),

            new Tostaderia("Torrefactora del Alto", "La Paz", "Av. Periférica",
                    "Lun-Sáb 8:00-17:00",
                    "Tostadora tradicional de la ciudad, especialista en blends paceños.",
                    "contacto@altocafe.bo", R.color.verde_oscuro, 'A', -16.505, -68.155),

            new Tostaderia("CoffeeLab La Paz", "La Paz", "Zona San Jorge",
                    "Lun-Sáb 9:30-19:00",
                    "Laboratorio de tostado y catación para cafés de especialidad.",
                    "hola@coffeelab.bo", R.color.texto_secundario, 'C', -16.513, -68.120),

            new Tostaderia("Tostaduría Yungas", "Yungas", "Coroico",
                    "Lun-Dom 8:00-18:00",
                    "Tueste yngueño con granos 100% yungueños, directo de la región.",
                    "ventas@tostyungas.bo", R.color.caramelo_claro, 'Y', -16.190, -67.727),

            new Tostaderia("Tostaduría del Sur", "Tarija", "Calle Bolívar, Tarija",
                    "Lun-Sáb 9:00-18:30",
                    "Sabores del sur boliviano con café de las laderas tarijeñas.",
                    "sabor@tostadelsur.bo", R.color.verde_claro, 'S', -21.535, -64.729)));

    // ------------------------------------------------------------------
    // 6. PUNTOS DE VENTA (distribución urbana por marca)
    // ------------------------------------------------------------------
    private static final List<PuntoVenta> PUNTOS_DE_VENTA = new ArrayList<>(Arrays.asList(
            new PuntoVenta("TYPICA", "Achumani", "Sur",
                    "Av. Costanera, Achumani", -16.535, -68.070),
            new PuntoVenta("TYPICA", "San Miguel", "Sur",
                    "Av. Ballivián, San Miguel", -16.521, -68.083),
            new PuntoVenta("TYPICA", "Miraflores", "Miraflores",
                    "Av. Busch, Miraflores", -16.501, -68.113),

            new PuntoVenta("Café Yungas", "Camacho", "Centro",
                    "Av. Camacho esq. Colón", -16.497, -68.137),
            new PuntoVenta("Café Yungas", "Sopocachi", "Sopocachi",
                    "Av. 6 de Agosto", -16.512, -68.124),
            new PuntoVenta("Café Yungas", "Obrajes", "Sur",
                    "Av. Hernando Siles, Obrajes", -16.520, -68.096),
            new PuntoVenta("Café Yungas", "Villa Fátima", "Villa Fátima",
                    "Av. La Paz, Villa Fátima", -16.472, -68.105),
            new PuntoVenta("Café Yungas", "San Pedro", "San Pedro",
                    "Plaza San Pedro", -16.493, -68.144),

            new PuntoVenta("Café de la Finca", "Centro", "Centro",
                    "Calle Mercado", -16.495, -68.135),
            new PuntoVenta("Café de la Finca", "Calacoto", "Sur",
                    "Av. Costanera, Calacoto", -16.538, -68.076)));

    // Cada método "obtener" siguiente es público y lo usa el RepositorioDatos.
    static List<CafeOrigen> obtenerCafesDeOrigen() {
        return new ArrayList<>(TODO_LOS_CAFES);
    }

    static List<MarcaCafe> obtenerMarcas() {
        return new ArrayList<>(MARCAS);
    }

    static List<Cafeteria> obtenerCafeterias() {
        return new ArrayList<>(CAFETERIAS);
    }

    static List<Productor> obtenerProductores() {
        return new ArrayList<>(PRODUCTORES);
    }

    static List<Tostaderia> obtenerTostaderias() {
        return new ArrayList<>(TOSTADERIAS);
    }

    static List<PuntoVenta> obtenerPuntosDeVenta() {
        return new ArrayList<>(PUNTOS_DE_VENTA);
    }
}