package com.ubicafe.app.ui.mapa;

import android.content.Context;
import android.content.SharedPreferences;

import org.osmdroid.config.Configuration;

/**
 * CONFIGURACIÓN DEL MAPA (OpenStreetMap / osmdroid).
 * ---------------------------------------------------------------
 * Política de teselas: se usan las teselas públicas de openstreetmap.org,
 * que NO requieren token. Si algún día se cuenta con un proveedor propio
 * (p. ej. MapTiler, Mapbox) basta con completar TOKEN_PROVEEDOR y cambiar
 * la URL de teselas en el futuro; el resto de la app no cambia.
 * ---------------------------------------------------------------
 */
public final class ConfiguracionMapa {

    /** Token opcional de un proveedor de teselas (vacío = teas públicas). */
    public static final String TOKEN_PROVEEDOR = "";

    /** Centro de la ciudad de La Paz para centrar el mapa por defecto. */
    public static final double LAT_LA_PAZ = -16.500;
    public static final double LNG_LA_PAZ = -68.120;

    private ConfiguracionMapa() {
        // Prevent instanciación.
    }

    /** Aplica el user-agent y las preferencias que osmdroid necesita. */
    public static void inicializar(Context contexto) {
        SharedPreferences preferencias = contexto.getSharedPreferences("osmdroid",
                Context.MODE_PRIVATE);
        Configuration.getInstance().load(contexto, preferencias);
        Configuration.getInstance().setUserAgentValue(
                "UbiCafe/1.0 (" + contexto.getPackageName() + ")");
    }
}