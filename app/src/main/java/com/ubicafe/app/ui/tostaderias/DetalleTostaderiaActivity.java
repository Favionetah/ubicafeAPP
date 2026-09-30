package com.ubicafe.app.ui.tostaderias;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.ubicafe.app.R;
import com.ubicafe.app.datos.RepositorioDatos;
import com.ubicafe.app.databinding.ActivityDetalleEntidadBinding;
import com.ubicafe.app.modelo.Entidad;
import com.ubicafe.app.modelo.Rol;
import com.ubicafe.app.ui.comun.CreditosActivity;
import com.ubicafe.app.ui.comun.ComoLlegar;
import com.ubicafe.app.modelo.Sucursal;
import com.ubicafe.app.ui.cafeterias.DetalleCafeteriaActivity;
import com.ubicafe.app.ui.comun.FichaTecnica;
import com.ubicafe.app.ui.comun.ListaSucursales;
import com.ubicafe.app.util.UiUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * FICHA DE UNA TOSTADURÍA (P7)
 * ---------------------------------------------------------------
 * Reutiliza la plantilla de ficha del censo y solo añade la lista de
 * sucursales, que es el dato que la distingue: de dónde compra el
 * grano y dónde se puede encontrar lo que tuesta.
 */
public class DetalleTostaderiaActivity extends AppCompatActivity {

    public static final String EXTRA_ID_ENTIDAD = "id_entidad";
    public static final String EXTRA_NOMBRE_TOSTADURIA = "nombre_tostaderia";

    private ActivityDetalleEntidadBinding ui;

    public static void abrir(Context origen, Entidad lugar) {
        Intent intento = new Intent(origen, DetalleTostaderiaActivity.class);
        intento.putExtra(EXTRA_ID_ENTIDAD, lugar.id);
        origen.startActivity(intento);
    }

    @Override
    protected void onCreate(@Nullable Bundle estado) {
        super.onCreate(estado);
        ui = ActivityDetalleEntidadBinding.inflate(getLayoutInflater());
        setContentView(ui.getRoot());

        ui.barra.btnVolver.setOnClickListener(v -> finish());

        // Los créditos tienen que quedar a mano en cada ficha: las
        // licencias de las fotos obligan a poder consultarlos.
        ui.enlaceCreditos.setOnClickListener(v -> CreditosActivity.abrir(this));

        Entidad lugar = buscarLugar();
        if (lugar == null) {
            finish();
            return;
        }

        ui.barra.textoTitulo.setText(Rol.TOSTADURIA.etiqueta());
        ui.barra.textoSubtitulo.setText(getString(R.string.tostaderias_subtitulo));
        ui.iconoRol.setImageResource(Rol.TOSTADURIA.icono());
        ui.iconoRol.setBackgroundTintList(ContextCompat.getColorStateList(
                this, Rol.TOSTADURIA.color()));
        ui.textoNombre.setText(lugar.nombre);
        ui.textoRoles.setText(lugar.rolesComoTexto());

        ComoLlegar.pintarHero(ui.fotoHero, this, lugar);
        ComoLlegar.conectar(this, lugar, ui.btnComoLlegar, ui.btnComoLlegarFijo);

        if (lugar.detalleTostaderia != null && !lugar.detalleTostaderia.estaVacia()) {
            agregarSeccion(lugar.detalleTostaderia.pares());
        } else {
            FichaTecnica.mostrarAviso(ui.contenedorFichas,
                    getString(R.string.detalle_tostaderia_vacio));
        }

        List<Sucursal> sucursales = RepositorioDatos.obtenerSucursalesDeEntidad(lugar.id);
        agregarSucursales(sucursales);

        if (!lugar.tieneCoordenadas()) {
            ui.bloqueAcciones.setVisibility(View.GONE);
        } else {
            ui.btnVerMapa.setOnClickListener(v -> {
                Intent intento = new Intent(this, com.ubicafe.app.ui.mapa.MapaActivity.class);
                intento.putExtra(com.ubicafe.app.ui.mapa.MapaActivity.EXTRA_CENTRAR_EN, lugar.id);
                startActivity(intento);
            });
        }
    }

    private Entidad buscarLugar() {
        String id = getIntent().getStringExtra(EXTRA_ID_ENTIDAD);
        if (id != null && !id.isEmpty()) {
            Entidad porId = RepositorioDatos.obtenerEntidad(id);
            if (porId != null) {
                return porId;
            }
        }
        String nombre = getIntent().getStringExtra(EXTRA_NOMBRE_TOSTADURIA);
        return nombre == null ? null : RepositorioDatos.obtenerEntidadPorNombre(nombre);
    }

    private void agregarSeccion(List<com.ubicafe.app.modelo.ParDato> pares) {
        ui.contenedorFichas.addView(
                FichaTecnica.encabezado(this, R.string.detalle_seccion_tostaderia));
        FichaTecnica.agregar(ui.contenedorFichas, pares);
    }

    /**
     * Los locales de esta tostaduría, si registró alguno. Se listan
     * porque saber dónde está el local es la pregunta más frecuente.
     */
    private void agregarSucursales(List<Sucursal> sucursales) {
        if (sucursales.isEmpty()) {
            FichaTecnica.mostrarAviso(ui.contenedorFichas,
                    getString(R.string.detalle_sin_sucursales));
            return;
        }

        ui.contenedorFichas.addView(
                FichaTecnica.encabezado(this, R.string.detalle_seccion_sucursales));
        ListaSucursales.agregar(ui.contenedorFichas, sucursales, sucursal ->
                DetalleCafeteriaActivity.abrir(this,
                        RepositorioDatos.obtenerEntidad(sucursal.entidadId)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        ui.textoRegistro.setText(getString(R.string.detalle_registrada_en,
                RepositorioDatos.obtenerAnioCenso()));
    }
}
