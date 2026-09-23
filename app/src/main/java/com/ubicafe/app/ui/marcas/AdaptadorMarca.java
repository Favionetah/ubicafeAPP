package com.ubicafe.app.ui.marcas;

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
import com.ubicafe.app.modelo.MarcaCafe;

import java.util.List;

/**
 * Adaptador de la lista de marcas de café.
 */
public class AdaptadorMarca extends RecyclerView.Adapter<AdaptadorMarca.Vista> {

    private final List<MarcaCafe> marcas;

    public AdaptadorMarca(List<MarcaCafe> marcas) {
        this.marcas = marcas;
    }

    @NonNull
    @Override
    public Vista onCreateViewHolder(@NonNull ViewGroup padre, int tipoDeVista) {
        View vista = LayoutInflater.from(padre.getContext())
                .inflate(R.layout.item_marca, padre, false);
        return new Vista(vista);
    }

    @Override
    public void onBindViewHolder(@NonNull Vista soporte, int posicion) {
        MarcaCafe marca = marcas.get(posicion);

        soporte.textoNombre.setText(marca.nombre);
        soporte.textoEtiquetas.setText(android.text.TextUtils.join(", ", marca.etiquetas));
        soporte.textoInicial.setText(String.valueOf(marca.inicial));
        pintarCuadro(soporte.boxInicial, marca.colorMarca);

        soporte.itemView.setOnClickListener(v -> {
            Intent intento = new Intent(v.getContext(), DetalleMarcaActivity.class);
            intento.putExtra(DetalleMarcaActivity.EXTRA_NOMBRE_MARCA, marca.nombre);
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
        return marcas.size();
    }

    static class Vista extends RecyclerView.ViewHolder {
        final LinearLayout boxInicial;
        final TextView textoInicial, textoNombre, textoEtiquetas;

        Vista(View itemView) {
            super(itemView);
            boxInicial = itemView.findViewById(R.id.box_inicial);
            textoInicial = itemView.findViewById(R.id.texto_inicial);
            textoNombre = itemView.findViewById(R.id.texto_nombre);
            textoEtiquetas = itemView.findViewById(R.id.texto_etiquetas);
        }
    }
}