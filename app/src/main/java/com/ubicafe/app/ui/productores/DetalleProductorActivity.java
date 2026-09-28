package com.ubicafe.app.ui.productores;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import com.ubicafe.app.databinding.ItemDatoBinding;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.ubicafe.app.R;
import com.ubicafe.app.datos.RepositorioDatos;
import com.ubicafe.app.databinding.ActivityDetalleEntidadBinding;
import com.ubicafe.app.modelo.Entidad;
import com.ubicafe.app.modelo.Rol;
import com.ubicafe.app.ui.comun.FichaTecnica;
import com.ubicafe.app.ui.mapa.Geolocalizador;
import com.ubicafe.app.ui.mapa.MapaActivity;
import com.ubicafe.app.util.UiUtils;

import java.util.List;

/**
 * FICHA DE UN PRODUCTOR (P7)
 * ---------------------------------------------------------------
 * Un productor no está en la ciudad: vive en su comunidad, en el
 * campo. Por eso su ficha da más peso al origen, las especies y las
 * variedades, y la distancia se calcula igual para no tratarlo como un
 * caso especial.
 */
public class DetalleProductorActivity extends AppCompatActivity {

    public static final String EXTRA_ID_ENTIDAD = "id_entidad";
    public static final String EXTRA_NOMBRE_FINCA = "nombre_finca";

    private ActivityDetalleEntidadBinding ui;

    public static void abrir(Context origen, Entidad lugar) {
        Intent intento = new Intent(origen, DetalleProductorActivity.class);
        intento.putExtra(EXTRA_ID_ENTIDAD, lugar.id);
        origen.startActivity(intento);
    }

    @Override
    protected void onCreate(@Nullable Bundle estado) {
        super.onCreate(estado);
        ui = ActivityDetalleEntidadBinding.inflate(getLayoutInflater());
        setContentView(ui.getRoot());

        ui.barra.btnVolver.setOnClickListener(v -> finish());

        Entidad lugar = buscarLugar();
        if (lugar == null) {
            finish();
            return;
        }

        ui.barra.textoTitulo.setText(Rol.PRODUCTOR.etiqueta());
        ui.barra.textoSubtitulo.setText(getString(R.string.productores_subtitulo));
        ui.iconoRol.setImageResource(Rol.PRODUCTOR.icono());
        ui.iconoRol.setBackgroundTintList(ContextCompat.getColorStateList(
                this, Rol.PRODUCTOR.color()));
        ui.textoNombre.setText(lugar.nombre);
        ui.textoRoles.setText(lugar.rolesComoTexto());

        pintarDatos(lugar);
        pintarFichas(lugar);
        pintarAcciones(lugar);

        int anio = RepositorioDatos.obtenerAnioCenso();
        if (anio > 0) {
            ui.textoRegistro.setText(getString(R.string.detalle_registrada_en, anio));
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
        String nombre = getIntent().getStringExtra(EXTRA_NOMBRE_FINCA);
        return nombre == null ? null : RepositorioDatos.obtenerEntidadPorNombre(nombre);
    }

    private void pintarDatos(Entidad lugar) {
        if (!lugar.macrodistrito.isEmpty()) {
            mostrarFila(ui.datoZona, getString(R.string.cafeteria_campo_zona),
                    lugar.macrodistrito);
        } else {
            FichaTecnica.ocultar(ui.datoZona);
        }

        String direccion = lugar.direccion.isEmpty() ? lugar.mapaUrl : lugar.direccion;
        if (direccion.isEmpty()) {
            FichaTecnica.ocultar(ui.datoDireccion);
        } else {
            mostrarFila(ui.datoDireccion, getString(R.string.detalle_campo_direccion), direccion);
        }

        if (lugar.tieneCoordenadas()) {
            double kilometros = Geolocalizador.kilometrosA(this, lugar.lat, lugar.lng);
            mostrarFila(ui.datoDistancia, getString(R.string.inicio_distancia_corto),
                    kilometros < 0
                            ? Geolocalizador.SIN_UBICACION
                            : Geolocalizador.formatear(kilometros));
        } else {
            FichaTecnica.ocultar(ui.datoDistancia);
        }

        FichaTecnica.ocultar(ui.datoTipo);
    }

    private void mostrarFila(ItemDatoBinding fila, String etiqueta, String valor) {
        FichaTecnica.pintar(fila, etiqueta, valor);
    }

    private void pintarFichas(Entidad lugar) {
        if (lugar.detalleProductor == null || lugar.detalleProductor.estaVacia()) {
            FichaTecnica.mostrarAviso(ui.contenedorFichas,
                    getString(R.string.detalle_productor_vacio));
            return;
        }

        ui.contenedorFichas.addView(
                FichaTecnica.encabezado(this, R.string.detalle_seccion_productor));

        List<com.ubicafe.app.modelo.ParDato> pares = lugar.detalleProductor.pares();
        FichaTecnica.agregar(ui.contenedorFichas, pares);

        List<String> notas = lugar.notasDeCata();
        if (!notas.isEmpty()) {
            FichaTecnica.mostrarAviso(ui.contenedorFichas,
                    getString(R.string.ficha_notas_sensoriales) + ": "
                            + com.ubicafe.app.util.Texto.unir(" · ", notas));
        }
    }

    private void pintarAcciones(Entidad lugar) {
        if (!lugar.tieneCoordenadas()) {
            ui.bloqueAcciones.setVisibility(android.view.View.GONE);
            return;
        }
        ui.btnVerMapa.setOnClickListener(v -> {
            Intent intento = new Intent(this, MapaActivity.class);
            intento.putExtra(MapaActivity.EXTRA_CENTRAR_EN, lugar.id);
            startActivity(intento);
        });
        ui.btnIrMarca.setVisibility(android.view.View.GONE);
    }
}
