package com.ubicafe.app.ui.comun;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.ImageView;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.ubicafe.app.R;
import com.ubicafe.app.modelo.Entidad;
import com.ubicafe.app.ui.mapa.Geolocalizador;
import com.ubicafe.app.ui.mapa.MapaActivity;
import com.ubicafe.app.util.CargadorFotos;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * EL BOTÓN "CÓMO LLEGAR" DE LAS FICHAS
 * ---------------------------------------------------------------
 * Las cuatro fichas de lugar (cafetería, tostaduría, productor y la
 * genérica) comparten estos botones, así que lo que hacen vive aquí y no
 * copiado cuatro veces. Lo que hace es:
 *
 *  1. Si ya hay permiso preciso, abrir el mapa con la ruta.
 *  2. Si no, pedirlo y abrir el mapa en cuanto se conceda.
 *
 * POR QUÉ PIDE EL PERMISO AL PINCHAR Y NO AL ABRIR LA FICHA
 * Un permiso pedido sin motivo se deniega por costumbre. Aquí la
 * persona ya ha tocado "Cómo llegar": sabe exactamente para qué quiere
 * su ubicación, y en ese momento concederlo es más probable. Además, si lo
 * deniega, se abre igualmente el mapa con el destino: pierde la ruta,
 * pero no la pantalla.
 */
public final class ComoLlegar {

    private ComoLlegar() {
        // Clase de utilidades: no se instancia.
    }

    /**
     * Deja listos los botones de "Cómo llegar" de una ficha.
     *
     * Acepta varios porque cada ficha tiene dos: el de arriba del
     * contenido y el fijo del pie. Se conectan juntos y no por separado
     * a propósito: el lanzador de permisos se registra UNA sola vez por
     * Activity, y llamar a conectar dos veces dejaría el segundo
     * registro sin permiso y la app cerrándose al pedir la ubicación.
     *
     * Hay que llamarlo desde onCreate, antes de que la Activity llegue
     * a onStart: registrar el lanzador más tarde lanza una excepción.
     */
    public static void conectar(ComponentActivity pantalla, Entidad lugar,
                                View... botones) {
        if (pantalla == null || lugar == null || botones == null) {
            return;
        }
        List<View> vivos = new ArrayList<>(botones.length);
        for (View boton : botones) {
            if (boton == null) {
                continue;
            }
            if (!lugar.tieneCoordenadas()) {
                // Sin destino no hay ruta. Se oculta el botón en vez de
                // dejar algo que no puede hacer nada, y con él el pie
                // entero: si solo se escondiera el botón, el marco del
                // pie se quedaría como una franja vacía al pie de la
                // pantalla.
                boton.setVisibility(View.GONE);
                android.view.ViewGroup marco = (android.view.ViewGroup) boton.getParent();
                if (marco != null
                        && marco.getId() == R.id.contenedor_boton_fijo) {
                    marco.setVisibility(View.GONE);
                }
                continue;
            }
            vivos.add(boton);
        }
        if (vivos.isEmpty()) {
            return;
        }

        final ActivityResultLauncher<String[]> pedir =
                pantalla.registerForActivityResult(
                        new ActivityResultContracts.RequestMultiplePermissions(),
                        resultado -> onPermisoRespondido(pantalla, resultado, lugar));

        View.OnClickListener alPulsar = v -> {
            if (Geolocalizador.tienePermisoPreciso(pantalla)) {
                abrirRuta(pantalla, lugar);
                return;
            }
            pedir.launch(Geolocalizador.permisosDeRuta());
        };
        for (View boton : vivos) {
            boton.setOnClickListener(alPulsar);
        }
    }

    /**
     * Sea cual sea la respuesta, el mapa se abre. Si se denegó, el panel
     * de la ruta lo explicará y mostrará el trayecto en línea recta
     * desde el último punto conocido, o pedirá activar la ubicación si
     * no hay ninguno.
     */
    private static void onPermisoRespondido(ComponentActivity pantalla,
                                            Map<String, Boolean> resultado, Entidad lugar) {
        boolean concedido = false;
        for (Boolean valor : resultado.values()) {
            concedido = concedido || Boolean.TRUE.equals(valor);
        }
        Geolocalizador.recordarDecision(pantalla, concedido);
        abrirRuta(pantalla, lugar);
    }

    private static void abrirRuta(Context contexto, Entidad lugar) {
        Intent intento = new Intent(contexto, MapaActivity.class);
        intento.putExtra(MapaActivity.EXTRA_RUTA_A, lugar.id);
        contexto.startActivity(intento);
    }

    /**
     * La foto grande de la cabecera. Si el lugar no tiene, la vista se
     * oculta para que el encabezado suba y no quede un rectángulo vacío
     * de 200 dp: es preferible no tener hero a tener un hueco.
     *
     * Cuando sí la hay, se enciende también el aviso de que es una
     * imagen genérica. Las fotos del censo no son de cada local, así
     * que poner la foto sin decirlo sería hacer pasar una imagen de
     * stock por la foto del negocio.
     */
    public static void pintarHero(ImageView hero, Context contexto, Entidad lugar) {
        pintar(hero, contexto, lugar.foto, lugar.rolPrincipal.icono());
    }

    /** Igual, pero para la ficha de una marca. */
    public static void pintarHeroMarca(ImageView hero, Context contexto, String foto) {
        pintar(hero, contexto, foto, R.drawable.ic_marca);
    }

    private static void pintar(ImageView hero, Context contexto, String foto, int respaldo) {
        if (hero == null) {
            return;
        }
        View aviso = avisoDe(hero);
        if (foto == null || foto.isEmpty()) {
            hero.setVisibility(View.GONE);
            if (aviso != null) {
                aviso.setVisibility(View.GONE);
            }
            return;
        }
        hero.setVisibility(View.VISIBLE);
        // El respaldo es el ícono del papel a tamaño grande: si la
        // imagen no llegara a decodificarse, al menos se ve de qué
        // lugar se trata en vez de un hueco.
        CargadorFotos.pintarRecortada(hero, contexto, foto, respaldo);
        if (aviso != null) {
            aviso.setVisibility(View.VISIBLE);
            // El aviso es pulsable: es justo el texto que alguien
            // encuentra leyendo cuando se pregunta "por qué la
            // foto no es de la cafetería". La respuesta está en los
            // créditos.
            aviso.setOnClickListener(v -> CreditosActivity.abrir(contexto));
        }
    }

    /**
     * El TextView del aviso, que va justo detrás del hero en el layout.
     * Se busca por id y no se guarda, porque el helper no guarda
     * referencias a vistas de una pantalla que puede destruirse.
     */
    private static View avisoDe(ImageView hero) {
        android.view.ViewGroup padre = (android.view.ViewGroup) hero.getParent();
        return padre == null ? null : padre.findViewById(R.id.texto_foto_credito);
    }
}
