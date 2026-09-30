package com.ubicafe.app.ui.comun;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.ubicafe.app.R;
import com.ubicafe.app.modelo.Marca;
import com.ubicafe.app.modelo.Rol;
import com.ubicafe.app.util.CargadorFotos;
import com.ubicafe.app.util.Texto;

import java.util.ArrayList;
import java.util.List;

/**
 * TARJETA DE UNA MARCA
 * ---------------------------------------------------------------
 * Una marca no es un lugar: es una empresa que puede tener 11 locales
 * (Alexander Coffee) o ninguno. Por eso la tarjeta no pide dirección,
 * sino el número de sucursales y las zonas donde opera, que es lo que
 * de verdad le interesa a quien la busca.
 *
 * La marca se representa con su foto genérica, y si no tiene con su
 * inicial sobre el color de su papel. La inicial queda debajo como
 * respaldo, para que nunca se vea un hueco mientras carga. Al no venir
 * logotipos en el censo, no se inventa ninguno: la inicial es la letra
 * del nombre y nada más.
 */
public class AdaptadorMarca extends RecyclerView.Adapter<AdaptadorMarca.Vista> {

    public interface AlTocar {
        void onMarcaTocada(Marca marca);
    }

    private final List<Marca> marcas = new ArrayList<>();
    private final AlTocar alTocar;

    public AdaptadorMarca(List<Marca> marcas, AlTocar alTocar) {
        this.alTocar = alTocar;
        setItems(marcas);
    }

    public void setItems(List<Marca> nuevas) {
        DiffUtil.DiffResult diferencia = DiffUtil.calculateDiff(
                new Comparador(marcas, nuevas));
        marcas.clear();
        marcas.addAll(nuevas);
        diferencia.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public Vista onCreateViewHolder(@NonNull ViewGroup padre, int tipo) {
        View vista = LayoutInflater.from(padre.getContext())
                .inflate(R.layout.item_marca, padre, false);
        return new Vista(vista);
    }

    @Override
    public void onBindViewHolder(@NonNull Vista soporte, int posicion) {
        Marca marca = marcas.get(posicion);
        Context contexto = soporte.itemView.getContext();

        // La inicial se pinta siempre y la foto encima. Si la imagen no
        // existe, CargadorFotos deja el ícono del papel y la inicial
        // sigue leyéndose detrás.
        soporte.inicial.setText(String.valueOf(marca.inicial()));
        soporte.inicial.setBackgroundTintList(ContextCompat.getColorStateList(
                contexto, Rol.MARCA.color()));
        CargadorFotos.pintarRecortada(soporte.fotoMarca, contexto, marca.foto,
                R.drawable.ic_marca);

        soporte.textoNombre.setText(marca.nombre);
        soporte.textoEtiquetas.setText(resumen(marca));

        soporte.itemView.setOnClickListener(v -> alTocar.onMarcaTocada(marca));
    }

    /**
     * El subtítulo de la tarjeta: cuántas sucursales tiene y dónde están.
     * Si no tiene ninguna, se dice con todas sus letras en vez de
     * mostrar "0 sucursales", que suena a error.
     */
    private String resumen(Marca marca) {
        if (!marca.tieneSucursales()) {
            return "Sin sucursales registradas";
        }
        int cantidad = marca.sucursales.size();
        String zonas = Texto.unir(" · ", marca.zonasDondeOpera());
        String texto = cantidad == 1
                ? "1 sucursal"
                : cantidad + " sucursales";
        return zonas.isEmpty() ? texto : texto + " · " + zonas;
    }

    @Override
    public int getItemCount() {
        return marcas.size();
    }

    static class Vista extends RecyclerView.ViewHolder {
        final ImageView fotoMarca;
        final TextView inicial, textoNombre, textoEtiquetas;

        Vista(View itemView) {
            super(itemView);
            fotoMarca = itemView.findViewById(R.id.foto_marca);
            inicial = itemView.findViewById(R.id.texto_inicial);
            textoNombre = itemView.findViewById(R.id.texto_nombre);
            textoEtiquetas = itemView.findViewById(R.id.texto_etiquetas);
        }
    }

    private static class Comparador extends DiffUtil.Callback {
        private final List<Marca> antes;
        private final List<Marca> despues;

        Comparador(List<Marca> antes, List<Marca> despues) {
            this.antes = new ArrayList<>(antes);
            this.despues = new ArrayList<>(despues);
        }

        @Override public int getOldListSize() { return antes.size(); }
        @Override public int getNewListSize() { return despues.size(); }

        @Override public boolean areItemsTheSame(int i, int j) {
            return antes.get(i).nombre.equals(despues.get(j).nombre);
        }

        @Override public boolean areContentsTheSame(int i, int j) {
            Marca uno = antes.get(i);
            Marca otro = despues.get(j);
            return uno.sucursales.size() == otro.sucursales.size()
                    && uno.nota.equals(otro.nota)
                    && uno.foto.equals(otro.foto);
        }
    }
}
