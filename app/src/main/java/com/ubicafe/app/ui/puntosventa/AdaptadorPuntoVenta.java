package com.ubicafe.app.ui.puntosventa;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ubicafe.app.R;
import com.ubicafe.app.modelo.PuntoVenta;

import java.util.List;

/**
 * Adaptador de la lista de puntos de venta.
 */
public class AdaptadorPuntoVenta extends RecyclerView.Adapter<AdaptadorPuntoVenta.Vista> {

    private final List<PuntoVenta> puntos;

    public AdaptadorPuntoVenta(List<PuntoVenta> puntos) {
        this.puntos = puntos;
    }

    @NonNull
    @Override
    public Vista onCreateViewHolder(@NonNull ViewGroup padre, int tipoDeVista) {
        View vista = LayoutInflater.from(padre.getContext())
                .inflate(R.layout.item_punto_venta, padre, false);
        return new Vista(vista);
    }

    @Override
    public void onBindViewHolder(@NonNull Vista soporte, int posicion) {
        PuntoVenta punto = puntos.get(posicion);

        // Nombre visible: "marca - local" (ej. TYPICA - Achumani)
        soporte.textoNombre.setText(punto.marca + " - " + punto.local);
        soporte.textoBarrio.setText(punto.direccion + " · " + punto.barrio);
        soporte.textoInicial.setText(punto.marca.substring(0, 1));

        GradientDrawable fondo = new GradientDrawable();
        fondo.setShape(GradientDrawable.RECTANGLE);
        fondo.setCornerRadius(12f);
        fondo.setColor(soporte.itemView.getContext().getColor(R.color.verde_oscuro));
        soporte.boxInicial.setBackground(fondo);
    }

    @Override
    public int getItemCount() {
        return puntos.size();
    }

    static class Vista extends RecyclerView.ViewHolder {
        final LinearLayout boxInicial;
        final TextView textoInicial, textoNombre, textoBarrio;

        Vista(View itemView) {
            super(itemView);
            boxInicial = itemView.findViewById(R.id.box_inicial);
            textoInicial = itemView.findViewById(R.id.texto_inicial);
            textoNombre = itemView.findViewById(R.id.texto_nombre);
            textoBarrio = itemView.findViewById(R.id.texto_barrio);
        }
    }
}