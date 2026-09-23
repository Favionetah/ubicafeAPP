package com.ubicafe.app.ui.productores;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ubicafe.app.R;
import com.ubicafe.app.modelo.Productor;

import java.util.List;

/**
 * Adaptador de la lista de productores (fincas).
 */
public class AdaptadorProductor extends RecyclerView.Adapter<AdaptadorProductor.Vista> {

    private final List<Productor> productores;

    public AdaptadorProductor(List<Productor> productores) {
        this.productores = productores;
    }

    @NonNull
    @Override
    public Vista onCreateViewHolder(@NonNull ViewGroup padre, int tipoDeVista) {
        View vista = LayoutInflater.from(padre.getContext())
                .inflate(R.layout.item_productor, padre, false);
        return new Vista(vista);
    }

    @Override
    public void onBindViewHolder(@NonNull Vista soporte, int posicion) {
        Productor productor = productores.get(posicion);

        soporte.textoNombre.setText(productor.nombreFinca);
        soporte.textoSubtitulo.setText(productor.nombreFamilia + " · " + productor.origen);
        soporte.textoInicial.setText(String.valueOf(productor.inicial));
        pintarCuadro(soporte.boxInicial, productor.colorMarca);

        soporte.itemView.setOnClickListener(v -> {
            Intent intento = new Intent(v.getContext(), DetalleProductorActivity.class);
            intento.putExtra(DetalleProductorActivity.EXTRA_NOMBRE_FINCA, productor.nombreFinca);
            v.getContext().startActivity(intento);
        });
    }

    private void pintarCuadro(LinearLayout cuadro, int colorRes) {
        GradientDrawable fondo = new GradientDrawable();
        fondo.setShape(GradientDrawable.RECTANGLE);
        fondo.setCornerRadius(12f);
        fondo.setColor(cuadro.getContext().getColor(colorRes));
        cuadro.setBackground(fondo);
    }

    @Override
    public int getItemCount() {
        return productores.size();
    }

    static class Vista extends RecyclerView.ViewHolder {
        final LinearLayout boxInicial;
        final TextView textoInicial, textoNombre, textoSubtitulo;

        Vista(View itemView) {
            super(itemView);
            boxInicial = itemView.findViewById(R.id.box_inicial);
            textoInicial = itemView.findViewById(R.id.texto_inicial);
            textoNombre = itemView.findViewById(R.id.texto_nombre);
            textoSubtitulo = itemView.findViewById(R.id.texto_subtitulo);
        }
    }
}