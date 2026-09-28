package com.ubicafe.app.ui.comun;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.ubicafe.app.R;
import com.ubicafe.app.modelo.Sucursal;
import com.ubicafe.app.ui.mapa.Geolocalizador;

import java.util.ArrayList;
import java.util.List;

/**
 * TARJETA DE UN PUNTO DE VENTA
 * ---------------------------------------------------------------
 * Una sucursal es un local concreto: tiene su propia dirección y sus
 * propias coordenadas, aunque pertenezca a una cadena con otras 10.
 * La tarjeta lleva el nombre de la cadena arriba y el del local abajo,
 * porque para quien busca un lugar cercano lo que importa es la calle.
 */
public class AdaptadorSucursal extends RecyclerView.Adapter<AdaptadorSucursal.Vista> {

    public interface AlTocar {
        void onSucursalTocada(Sucursal sucursal);
    }

    private final List<Sucursal> sucursales = new ArrayList<>();
    private final AlTocar alTocar;

    public AdaptadorSucursal(List<Sucursal> sucursales, AlTocar alTocar) {
        this.alTocar = alTocar;
        setItems(sucursales);
    }

    public void setItems(List<Sucursal> nuevas) {
        DiffUtil.DiffResult diferencia = DiffUtil.calculateDiff(
                new Comparador(sucursales, nuevas));
        sucursales.clear();
        sucursales.addAll(nuevas);
        diferencia.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public Vista onCreateViewHolder(@NonNull ViewGroup padre, int tipo) {
        View vista = LayoutInflater.from(padre.getContext())
                .inflate(R.layout.item_sucursal, padre, false);
        return new Vista(vista);
    }

    @Override
    public void onBindViewHolder(@NonNull Vista soporte, int posicion) {
        Sucursal sucursal = sucursales.get(posicion);

        soporte.textoEntidad.setText(sucursal.marca);
        soporte.textoSucursal.setText(sucursal.nombre);

        if (sucursal.direccion.isEmpty()) {
            soporte.textoDireccion.setVisibility(View.GONE);
        } else {
            soporte.textoDireccion.setVisibility(View.VISIBLE);
            soporte.textoDireccion.setText(sucursal.direccion);
        }

        pintarDistancia(soporte, sucursal);
        soporte.itemView.setOnClickListener(v -> alTocar.onSucursalTocada(sucursal));
    }

    private void pintarDistancia(Vista soporte, Sucursal sucursal) {
        if (!sucursal.tieneCoordenadas()) {
            soporte.filaPie.setVisibility(View.GONE);
            return;
        }
        soporte.filaPie.setVisibility(View.VISIBLE);

        double kilometros = Geolocalizador.kilometrosA(
                soporte.itemView.getContext(), sucursal.lat, sucursal.lng);
        soporte.textoDistancia.setText(kilometros < 0
                ? Geolocalizador.SIN_UBICACION
                : Geolocalizador.formatear(kilometros));
    }

    @Override
    public int getItemCount() {
        return sucursales.size();
    }

    static class Vista extends RecyclerView.ViewHolder {
        final TextView textoEntidad, textoSucursal, textoDireccion, textoDistancia;
        final View filaPie;

        Vista(View itemView) {
            super(itemView);
            textoEntidad = itemView.findViewById(R.id.texto_entidad);
            textoSucursal = itemView.findViewById(R.id.texto_sucursal);
            textoDireccion = itemView.findViewById(R.id.texto_direccion);
            textoDistancia = itemView.findViewById(R.id.texto_distancia);
            filaPie = itemView.findViewById(R.id.fila_pie);
        }
    }

    private static class Comparador extends DiffUtil.Callback {
        private final List<Sucursal> antes;
        private final List<Sucursal> despues;

        Comparador(List<Sucursal> antes, List<Sucursal> despues) {
            this.antes = new ArrayList<>(antes);
            this.despues = new ArrayList<>(despues);
        }

        @Override public int getOldListSize() { return antes.size(); }
        @Override public int getNewListSize() { return despues.size(); }

        @Override public boolean areItemsTheSame(int i, int j) {
            return antes.get(i).entidadId.equals(despues.get(j).entidadId);
        }

        @Override public boolean areContentsTheSame(int i, int j) {
            Sucursal uno = antes.get(i);
            Sucursal otro = despues.get(j);
            return uno.nombre.equals(otro.nombre)
                    && uno.direccion.equals(otro.direccion);
        }
    }
}
