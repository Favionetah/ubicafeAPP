package com.ubicafe.app.ui.comun;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.ubicafe.app.R;
import com.ubicafe.app.modelo.Entidad;
import com.ubicafe.app.modelo.Rol;
import com.ubicafe.app.ui.mapa.Geolocalizador;

import java.util.ArrayList;
import java.util.List;

/**
 * TARJETA DE UN LUGAR DEL CENSO
 * ---------------------------------------------------------------
 * La misma tarjeta sirve para cafeterías, tostadurías, productores y
 * otros: lo que cambia es el ícono y el color, que salen del papel del
 * lugar. Así el usuario ve de un vistazo que un sitio es cafetería y
 * tostaduría a la vez, sin repetirlo en dos listas.
 *
 * No muestra fotos porque el censo no tiene ninguna, y no muestra
 * estrellas porque no hay reseñas: inventar un "4.9 (240)" sería
 * mentira. En su lugar dice la distancia real, o pide activar la
 * ubicación.
 */
public class AdaptadorEntidad extends RecyclerView.Adapter<AdaptadorEntidad.Vista> {

    /** A dónde abrir al tocar la tarjeta. */
    public interface AlTocar {
        void onEntidadTocada(Entidad entidad);
    }

    private final List<Entidad> lugares = new ArrayList<>();
    private final AlTocar alTocar;

    public AdaptadorEntidad(List<Entidad> lugares, AlTocar alTocar) {
        this.alTocar = alTocar;
        setItems(lugares);
    }

    /** Reemplaza la lista animando solo lo que cambió. */
    public void setItems(List<Entidad> nuevos) {
        DiffUtil.DiffResult diferencia = DiffUtil.calculateDiff(
                new Comparador(lugares, nuevos));
        lugares.clear();
        lugares.addAll(nuevos);
        diferencia.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public Vista onCreateViewHolder(@NonNull ViewGroup padre, int tipo) {
        View vista = LayoutInflater.from(padre.getContext())
                .inflate(R.layout.item_entidad, padre, false);
        return new Vista(vista);
    }

    @Override
    public void onBindViewHolder(@NonNull Vista soporte, int posicion) {
        Entidad lugar = lugares.get(posicion);
        Context contexto = soporte.itemView.getContext();

        soporte.iconoRol.setImageResource(lugar.rolPrincipal.icono());
        soporte.iconoRol.setBackgroundTintList(
                androidx.core.content.ContextCompat.getColorStateList(
                        contexto, lugar.rolPrincipal.color()));
        soporte.textoNombre.setText(lugar.nombre);
        soporte.textoRoles.setText(lugar.rolesComoTexto());

        pintarZona(soporte, lugar);
        pintarDireccion(soporte, lugar);
        pintarPie(soporte, contexto, lugar);

        soporte.itemView.setOnClickListener(v -> alTocar.onEntidadTocada(lugar));
    }

    private void pintarZona(Vista soporte, Entidad lugar) {
        if (lugar.macrodistrito.isEmpty()) {
            soporte.textoZona.setVisibility(View.GONE);
        } else {
            soporte.textoZona.setVisibility(View.VISIBLE);
            soporte.textoZona.setText(lugar.macrodistrito);
        }
    }

    private void pintarDireccion(Vista soporte, Entidad lugar) {
        if (lugar.direccion.isEmpty()) {
            soporte.textoDireccion.setVisibility(View.GONE);
        } else {
            soporte.textoDireccion.setVisibility(View.VISIBLE);
            soporte.textoDireccion.setText(lugar.direccion);
        }
    }

    /**
     * El pie de la tarjeta. Si hay ubicación, muestra la distancia real;
     * si no, lo dice y no inventa una cifra.
     */
    private void pintarPie(Vista soporte, Context contexto, Entidad lugar) {
        if (!lugar.tieneCoordenadas()) {
            soporte.filaPie.setVisibility(View.GONE);
            return;
        }
        soporte.filaPie.setVisibility(View.VISIBLE);

        double kilometros = Geolocalizador.kilometrosA(contexto, lugar.lat, lugar.lng);
        if (kilometros < 0) {
            soporte.textoPie.setText(Geolocalizador.SIN_UBICACION);
        } else {
            soporte.textoPie.setText(Geolocalizador.formatear(kilometros));
        }
    }

    @Override
    public int getItemCount() {
        return lugares.size();
    }

    /** Vistas de una tarjeta, resueltas una sola vez. */
    static class Vista extends RecyclerView.ViewHolder {
        final ImageView iconoRol;
        final TextView textoNombre, textoRoles, textoZona, textoDireccion, textoPie;
        final View filaPie;

        Vista(View itemView) {
            super(itemView);
            iconoRol = itemView.findViewById(R.id.icono_rol);
            textoNombre = itemView.findViewById(R.id.texto_nombre);
            textoRoles = itemView.findViewById(R.id.texto_roles);
            textoZona = itemView.findViewById(R.id.texto_zona);
            textoDireccion = itemView.findViewById(R.id.texto_direccion);
            textoPie = itemView.findViewById(R.id.texto_pie);
            filaPie = itemView.findViewById(R.id.fila_pie);
        }
    }

    /**
     * Compara por identificador y por contenido. Sin esto, cambiar un
     * filtro de zona repintaría las 213 tarjetas y la lista daría saltos
     * al desplazarse.
     */
    private static class Comparador extends DiffUtil.Callback {
        private final List<Entidad> antes;
        private final List<Entidad> despues;

        Comparador(List<Entidad> antes, List<Entidad> despues) {
            this.antes = new ArrayList<>(antes);
            this.despues = new ArrayList<>(despues);
        }

        @Override public int getOldListSize() { return antes.size(); }
        @Override public int getNewListSize() { return despues.size(); }

        @Override public boolean areItemsTheSame(int i, int j) {
            return antes.get(i).id.equals(despues.get(j).id);
        }

        @Override public boolean areContentsTheSame(int i, int j) {
            Entidad uno = antes.get(i);
            Entidad otro = despues.get(j);
            return uno.nombre.equals(otro.nombre)
                    && uno.rolesComoTexto().equals(otro.rolesComoTexto())
                    && uno.macrodistrito.equals(otro.macrodistrito)
                    && uno.direccion.equals(otro.direccion);
        }
    }
}
