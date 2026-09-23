package com.ubicafe.app.ui.tostaderias;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ubicafe.app.R;
import com.ubicafe.app.modelo.Tostaderia;

import java.util.ArrayList;
import java.util.List;

/**
 * Adaptador de la lista de tostadurías.
 */
public class AdaptadorTostaderia extends RecyclerView.Adapter<AdaptadorTostaderia.Vista> {

    private final List<Tostaderia> tostadurias;

    public AdaptadorTostaderia(List<Tostaderia> tostadurias) {
        this.tostadurias = tostadurias;
    }

    public void actualizar(List<Tostaderia> nuevas) {
        tostadurias.clear();
        tostadurias.addAll(nuevas);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Vista onCreateViewHolder(@NonNull ViewGroup padre, int tipoDeVista) {
        View vista = LayoutInflater.from(padre.getContext())
                .inflate(R.layout.item_tostaderia, padre, false);
        return new Vista(vista);
    }

    @Override
    public void onBindViewHolder(@NonNull Vista soporte, int posicion) {
        Tostaderia tostaduría = tostadurias.get(posicion);

        soporte.textoNombre.setText(tostaduría.nombre);
        soporte.textoRegion.setText(tostaduría.region);

        soporte.itemView.setOnClickListener(v -> {
            Intent intento = new Intent(v.getContext(), DetalleTostaderiaActivity.class);
            intento.putExtra(DetalleTostaderiaActivity.EXTRA_NOMBRE_TOSTADERIA, tostaduría.nombre);
            v.getContext().startActivity(intento);
        });
    }

    @Override
    public int getItemCount() {
        return tostadurias.size();
    }

    static class Vista extends RecyclerView.ViewHolder {
        final TextView textoNombre, textoRegion;

        Vista(View itemView) {
            super(itemView);
            textoNombre = itemView.findViewById(R.id.texto_nombre);
            textoRegion = itemView.findViewById(R.id.texto_region);
        }
    }
}