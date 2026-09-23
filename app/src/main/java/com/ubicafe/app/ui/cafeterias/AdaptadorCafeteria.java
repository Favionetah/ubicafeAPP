package com.ubicafe.app.ui.cafeterias;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ubicafe.app.R;
import com.ubicafe.app.modelo.Cafeteria;

import java.util.List;

/**
 * Adaptador de la lista de cafeterías (tarjetas con foto del P6).
 * Convierte cada objeto Cafeteria en una tarjeta visible.
 */
public class AdaptadorCafeteria extends RecyclerView.Adapter<AdaptadorCafeteria.Vista> {

    private final List<Cafeteria> cafeterias;

    public AdaptadorCafeteria(List<Cafeteria> cafeterias) {
        this.cafeterias = cafeterias;
    }

    /** Actualiza la lista completa del adaptador (tras filtrar o buscar). */
    public void actualizar(List<Cafeteria> nuevas) {
        cafeterias.clear();
        cafeterias.addAll(nuevas);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Vista onCreateViewHolder(@NonNull ViewGroup padre, int tipoDeVista) {
        View vista = LayoutInflater.from(padre.getContext())
                .inflate(R.layout.item_cafeteria, padre, false);
        return new Vista(vista);
    }

    @Override
    public void onBindViewHolder(@NonNull Vista soporte, int posicion) {
        Cafeteria cafeteria = cafeterias.get(posicion);

        soporte.textoNombre.setText(
                cafeteria.marca == null ? cafeteria.nombre : cafeteria.marca);
        soporte.textoTipo.setText(cafeteria.tipo);
        soporte.textoZona.setText(cafeteria.zona);
        soporte.textoDistancia.setText("A 450 m de ti");
        soporte.textoRating.setText("4.9 (240)");

        soporte.itemView.setOnClickListener(v -> abrirDetalle(soporte.itemView, cafeteria));
    }

    private void abrirDetalle(View vista, Cafeteria cafeteria) {
        Intent intento = new Intent(vista.getContext(), DetalleCafeteriaActivity.class);
        intento.putExtra(DetalleCafeteriaActivity.EXTRA_NOMBRE_CAFETERIA, cafeteria.nombre);
        vista.getContext().startActivity(intento);
    }

    @Override
    public int getItemCount() {
        return cafeterias.size();
    }

    /** Referencias a las vistas de una tarjeta (evita buscarlas en cada vuelta). */
    static class Vista extends RecyclerView.ViewHolder {
        final TextView textoNombre, textoTipo, textoZona, textoDistancia, textoRating;

        Vista(View itemView) {
            super(itemView);
            textoNombre = itemView.findViewById(R.id.texto_nombre);
            textoTipo = itemView.findViewById(R.id.texto_tipo);
            textoZona = itemView.findViewById(R.id.texto_zona);
            textoDistancia = itemView.findViewById(R.id.texto_distancia);
            textoRating = itemView.findViewById(R.id.texto_rating);
        }
    }
}