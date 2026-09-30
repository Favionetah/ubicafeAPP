package com.ubicafe.app.ui.cafeterias;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.ubicafe.app.databinding.ItemDatoBinding;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.ubicafe.app.R;
import com.ubicafe.app.datos.RepositorioDatos;
import com.ubicafe.app.databinding.ActivityDetalleEntidadBinding;
import com.ubicafe.app.modelo.Entidad;
import com.ubicafe.app.modelo.FichaDetalle;
import com.ubicafe.app.modelo.Marca;
import com.ubicafe.app.ui.comun.ComoLlegar;
import com.ubicafe.app.ui.comun.FichaTecnica;
import com.ubicafe.app.ui.mapa.Geolocalizador;
import com.ubicafe.app.ui.mapa.MapaActivity;
import com.ubicafe.app.ui.marcas.DetalleMarcaActivity;

/**
 * FICHA DE UN LUGAR DEL CENSO (P7)
 * ---------------------------------------------------------------
 * Una sola pantalla para cafeterías, tostadurías y productores. La
 * diferencia entre ellas no está en el formato sino en qué secciones
 * respondió cada lugar: hay 188 fichas de cafetería y solo 13 de
 * tostaduría, así que el bloque que no se respondió se oculta y se dice
 * por qué.
 *
 * Se abre por identificador, no por nombre: "Alexander Coffe" y
 * "Alexander coffee" son dos sucursales distintas de la misma cadena y
 * el nombre no las distingue.
 */
public class DetalleCafeteriaActivity extends AppCompatActivity {

    /** Identificador del lugar a mostrar. */
    public static final String EXTRA_ID_ENTIDAD = "id_entidad";

    /** Nombre del lugar, para las pantallas que solo conocen el nombre. */
    public static final String EXTRA_NOMBRE_CAFETERIA = "nombre_cafeteria";

    private ActivityDetalleEntidadBinding ui;

    /** Abre la ficha de un lugar concreto. */
    public static void abrir(Context origen, Entidad lugar) {
        Intent intento = new Intent(origen, DetalleCafeteriaActivity.class);
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
            // El lugar ya no está en los datos: se cierra en vez de
            // mostrar una ficha vacía sin explicación.
            finish();
            return;
        }

        pintarEncabezado(lugar);
        pintarDatosBasicos(lugar);
        pintarAvisoUbicacion(lugar);
        pintarFichas(lugar);
        pintarAcciones(lugar);
    }

    /** Busca el lugar por id y, si no viene, por nombre. */
    private Entidad buscarLugar() {
        String id = getIntent().getStringExtra(EXTRA_ID_ENTIDAD);
        if (id != null && !id.isEmpty()) {
            Entidad porId = RepositorioDatos.obtenerEntidad(id);
            if (porId != null) {
                return porId;
            }
        }
        String nombre = getIntent().getStringExtra(EXTRA_NOMBRE_CAFETERIA);
        return nombre == null ? null : RepositorioDatos.obtenerEntidadPorNombre(nombre);
    }

    private void pintarEncabezado(Entidad lugar) {
        ui.barra.textoTitulo.setText(lugar.rolPrincipal.etiqueta());
        ui.barra.textoSubtitulo.setText(lugar.macrodistrito.isEmpty()
                ? getString(R.string.app_ubicacion)
                : lugar.macrodistrito + " · La Paz");

        ui.iconoRol.setImageResource(lugar.rolPrincipal.icono());
        ui.iconoRol.setBackgroundTintList(ContextCompat.getColorStateList(
                this, lugar.rolPrincipal.color()));
        ui.textoNombre.setText(lugar.nombre);
        ui.textoRoles.setText(lugar.rolesComoTexto());

        ComoLlegar.pintarHero(ui.fotoHero, this, lugar);
        ComoLlegar.conectar(this, lugar, ui.btnComoLlegar, ui.btnComoLlegarFijo);
    }

    /**
     * Las cuatro filas de datos clave. Cada una se oculta si el censo no
     * la respondió, y la distancia solo si la persona aktivó la
     * ubicación: sin permiso se dice cómo activarla, no se estima nada.
     */
    private void pintarDatosBasicos(Entidad lugar) {
        mostrarFila(ui.datoZona, getString(R.string.cafeteria_campo_zona), lugar.macrodistrito);

        String direccion = lugar.direccion.isEmpty()
                ? lugar.mapaUrl
                : lugar.direccion;
        mostrarFila(ui.datoDireccion, getString(R.string.detalle_campo_direccion), direccion);

        if (!lugar.tieneCoordenadas()) {
            FichaTecnica.ocultar(ui.datoDistancia);
        } else {
            double kilometros = Geolocalizador.kilometrosA(this, lugar.lat, lugar.lng);
            mostrarFila(ui.datoDistancia, getString(R.string.inicio_distancia_corto),
                    kilometros < 0
                            ? Geolocalizador.SIN_UBICACION
                            : Geolocalizador.formatear(kilometros));
        }

        mostrarFila(ui.datoTipo, getString(R.string.detalle_campo_tipo),
                tipoDe(lugar));
    }

    /** Rellena una fila de la ficha, o la oculta si no hay valor. */
    private void mostrarFila(ItemDatoBinding fila, String etiqueta, String valor) {
        FichaTecnica.pintar(fila, etiqueta, valor);
    }

    /**
     * El tipo de establecimiento que respondió la encuesta: "cafetería",
     * "tostaduría con sala", "cafetería y tienda". Si no vino, se oculta
     * la fila en vez de poner algo parecido.
     */
    private String tipoDe(Entidad lugar) {
        if (lugar.detalleCafeteria == null
                || lugar.detalleCafeteria.tipoEstablecimiento.isEmpty()) {
            return "";
        }
        return lugar.detalleCafeteria.tipoEstablecimiento;
    }

    /**
     * El aviso del censo sobre la posición. Hay 33 lugares cuya
     * coordenada fue aproximada o no corresponde a su dirección, y
     * dejarlo callado haría parecer que el dato es exacto.
     */
    private void pintarAvisoUbicacion(Entidad lugar) {
        if (!lugar.notaUbicacion.isEmpty()) {
            ui.textoAvisoUbicacion.setVisibility(View.VISIBLE);
            ui.textoAvisoUbicacion.setText(getString(
                    R.string.detalle_nota_ubicacion, lugar.notaUbicacion));
            return;
        }
        if (!lugar.tieneCoordenadas()) {
            ui.textoAvisoUbicacion.setVisibility(View.VISIBLE);
            ui.textoAvisoUbicacion.setText(R.string.detalle_ubicacion_no_respondida);
        }
    }

    /**
     * Las secciones que el lugar respondió. Cada una es un bloque con
     * su título y sus filas; las que no respondió se saltan, porque 138
     * de los 213 lugares no llenaron ninguna y una pantalla llena de
     * avisos parecería un error.
     */
    private void pintarFichas(Entidad lugar) {
        int bloques = 0;

        bloques += agregarFicha(lugar.detalleCafeteria, R.string.detalle_seccion_cafeteria);
        bloques += agregarFicha(lugar.detalleTostaderia, R.string.detalle_seccion_tostaderia);
        bloques += agregarFicha(lugar.detalleProductor, R.string.detalle_seccion_productor);
        bloques += agregarFicha(lugar.detalleMarca, R.string.detalle_seccion_marca);

        if (bloques == 0) {
            FichaTecnica.mostrarAviso(ui.contenedorFichas,
                    getString(R.string.detalle_sin_secciones));
        }

        int anio = RepositorioDatos.obtenerAnioCenso();
        if (anio > 0) {
            ui.textoRegistro.setText(getString(R.string.detalle_registrada_en, anio));
        } else {
            ui.textoRegistro.setVisibility(View.GONE);
        }
    }

    /**
     * Un bloque de sección. Devuelve 1 si se pintó y 0 si se saltó, para
     * saber si el lugar tiene alguna ficha que mostrar.
     */
    private int agregarFicha(FichaDetalle detalle, int titulo) {
        if (detalle == null || detalle.estaVacia()) {
            return 0;
        }
        ui.contenedorFichas.addView(FichaTecnica.encabezado(this, titulo));
        FichaTecnica.agregar(ui.contenedorFichas, detalle.pares());
        return 1;
    }

    /** "Ver en el mapa" y, si pertenece a una cadena, la ficha de la cadena. */
    private void pintarAcciones(Entidad lugar) {
        if (!lugar.tieneCoordenadas()) {
            ui.bloqueAcciones.setVisibility(View.GONE);
            return;
        }
        ui.btnVerMapa.setOnClickListener(v -> {
            Intent intento = new Intent(this, MapaActivity.class);
            intento.putExtra(MapaActivity.EXTRA_CENTRAR_EN, lugar.id);
            startActivity(intento);
        });

        Marca marca = RepositorioDatos.obtenerMarcaDe(lugar.id);
        if (marca == null) {
            ui.btnIrMarca.setVisibility(View.GONE);
            return;
        }
        ui.btnIrMarca.setVisibility(View.VISIBLE);
        ui.btnIrMarca.setText(getString(R.string.detalle_ir_marca, marca.nombre));
        ui.btnIrMarca.setOnClickListener(v -> {
            Intent intento = new Intent(this, DetalleMarcaActivity.class);
            intento.putExtra(DetalleMarcaActivity.EXTRA_NOMBRE_MARCA, marca.nombre);
            startActivity(intento);
        });
    }
}
