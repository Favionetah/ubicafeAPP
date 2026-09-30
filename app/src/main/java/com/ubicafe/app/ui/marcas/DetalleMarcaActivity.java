package com.ubicafe.app.ui.marcas;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.ubicafe.app.R;
import com.ubicafe.app.datos.RepositorioDatos;
import com.ubicafe.app.databinding.ActivityDetalleMarcaBinding;
import com.ubicafe.app.modelo.Marca;
import com.ubicafe.app.modelo.Rol;
import com.ubicafe.app.ui.comun.CreditosActivity;
import com.ubicafe.app.ui.comun.ComoLlegar;
import com.ubicafe.app.modelo.Sucursal;
import com.ubicafe.app.ui.cafeterias.DetalleCafeteriaActivity;
import com.ubicafe.app.ui.comun.FichaTecnica;
import com.ubicafe.app.ui.comun.ListaSucursales;
import com.ubicafe.app.ui.mapa.MapaActivity;

import java.util.List;

/**
 * FICHA DE UNA MARCA (P7)
 * ---------------------------------------------------------------
 * Una marca es una empresa, no un lugar, así que su ficha tiene dos
 * mitades: los datos que registró la empresa y los locales que el censo
 * encontró abiertos. De las 40 marcas censadas, varias no
 * registraron ni una sucursal: se dice con todas las letras en vez de
 * mostrar una lista vacía.
 */
public class DetalleMarcaActivity extends AppCompatActivity {

    /** Nombre de la marca a mostrar. */
    public static final String EXTRA_NOMBRE_MARCA = "nombre_marca";

    /** Alias con el nombre que usaban las pantallas viejas. */
    public static final String EXTRA_MARCA = EXTRA_NOMBRE_MARCA;

    private ActivityDetalleMarcaBinding ui;

    public static void abrir(Context origen, Marca marca) {
        Intent intento = new Intent(origen, DetalleMarcaActivity.class);
        intento.putExtra(EXTRA_NOMBRE_MARCA, marca.nombre);
        origen.startActivity(intento);
    }

    @Override
    protected void onCreate(@Nullable Bundle estado) {
        super.onCreate(estado);
        ui = ActivityDetalleMarcaBinding.inflate(getLayoutInflater());
        setContentView(ui.getRoot());

        ui.barra.btnVolver.setOnClickListener(v -> finish());

        // Los créditos tienen que quedar a mano en cada ficha: las
        // licencias de las fotos obligan a poder consultarlos.
        ui.enlaceCreditos.setOnClickListener(v -> CreditosActivity.abrir(this));

        String nombre = getIntent().getStringExtra(EXTRA_NOMBRE_MARCA);
        Marca marca = nombre == null ? null : RepositorioDatos.obtenerMarca(nombre);
        if (marca == null) {
            finish();
            return;
        }

        pintarEncabezado(marca);
        pintarDatos(marca);
        pintarSucursales(marca);

        int anio = RepositorioDatos.obtenerAnioCenso();
        if (anio > 0) {
            ui.textoRegistro.setText(getString(R.string.detalle_registrada_en, anio));
        }
    }

    private void pintarEncabezado(Marca marca) {
        ui.barra.textoTitulo.setText(R.string.marcas_titulo);
        ui.barra.textoSubtitulo.setText(getString(R.string.marcas_subtitulo));
        ui.textoInicial.setText(String.valueOf(marca.inicial()));
        ui.textoInicial.setBackgroundTintList(ContextCompat.getColorStateList(
                this, Rol.MARCA.color()));
        ui.textoNombre.setText(marca.nombre);
        ComoLlegar.pintarHeroMarca(ui.fotoHero, this, marca.foto);
    }

    private void pintarDatos(Marca marca) {
        if (marca.detalle == null || marca.detalle.estaVacia()) {
            FichaTecnica.mostrarAviso(ui.contenedorDatos,
                    getString(R.string.detalle_marca_vacio));
            return;
        }
        FichaTecnica.agregar(ui.contenedorDatos, marca.detalle.pares());

        if (!marca.nota.isEmpty()) {
            ui.textoNota.setVisibility(View.VISIBLE);
            ui.textoNota.setText(marca.nota);
        }
    }

    /**
     * Los locales de la cadena. Es el bloque más útil de la ficha:
     * sin él, "Typica" es solo un nombre.
     */
    private void pintarSucursales(Marca marca) {
        List<Sucursal> sucursales = marca.sucursales;

        if (sucursales.isEmpty()) {
            ui.textoTituloSucursales.setText(R.string.detalle_sin_sucursales);
            ui.btnVerMapa.setVisibility(View.GONE);
            return;
        }

        int total = sucursales.size();
        ui.textoTituloSucursales.setText(getResources().getQuantityString(
                R.plurals.detalle_sucursales, total, total));
        ui.contenedorSucursales.addView(
                ListaSucursales.encabezado(ui.contenedorSucursales, sucursales.size()));
        ListaSucursales.agregar(ui.contenedorSucursales, sucursales, sucursal ->
                DetalleCafeteriaActivity.abrir(this,
                        RepositorioDatos.obtenerEntidad(sucursal.entidadId)));

        ui.btnVerMapa.setVisibility(View.VISIBLE);
        ui.btnVerMapa.setOnClickListener(v -> {
            Intent intento = new Intent(this, MapaActivity.class);
            intento.putExtra(MapaActivity.EXTRA_FILTRO_MARCA, marca.nombre);
            startActivity(intento);
        });
    }
}
