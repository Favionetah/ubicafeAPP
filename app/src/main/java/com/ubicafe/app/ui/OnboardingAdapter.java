package com.ubicafe.app.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ubicafe.app.R;
import com.ubicafe.app.util.CargadorFotos;

/**
 * ADAPTADOR DEL CARRUSEL DE ONBOARDING (P2).
 * Cada diapositiva muestra una foto a sangre, el título y la
 * descripción. El texto llega como recursos; la foto como una ruta
 * dentro de assets/, y la carga el cargador de imágenes para no
 * descodificar en el hilo principal.
 */
public class OnboardingAdapter extends RecyclerView.Adapter<OnboardingAdapter.PaginaViewHolder> {

    /** Información de una diapositiva del carrusel. */
    public static class Pagina {
        public final String titulo;
        public final String descripcion;
        public final String foto;

        public Pagina(String titulo, String descripcion, String foto) {
            this.titulo = titulo;
            this.descripcion = descripcion;
            this.foto = foto;
        }
    }

    /**
     * Las rutas de las fotos del carrusel, en el mismo orden que las
     * diapositivas. Existe para que una prueba pueda comprobar que los
     * archivos existen de verdad: al estar escritas a mano, un
     * renombrado en assets/ las rompería sin avisar al compilador.
     */
    public static String[] rutasDeFotos() {
        return new String[]{
                "fotos/cafes/cafe_berna_linares_947.jpg",
                "fotos/cafes/cafe_con_coca_linares_906.jpg",
                "fotos/cafes/alexander_coffe_20_de_octubre.jpg"
        };
    }

    private final Pagina[] paginas;

    /**
     * Las tres fotos salen del mismo archivo que las fichas. No es
     * pereza: son imágenes con autor y licencia ya anotados en
     * imagenes.json, y duplicar los archivos en otra carpeta metería
     * fotos nuevas sin acreditar y desincronizaría el contador que
     * publica la pantalla de créditos. El recorte lo hace Glide.
     */
    public OnboardingAdapter(Context contexto) {
        String[] fotos = rutasDeFotos();
        paginas = new Pagina[]{
                new Pagina(contexto.getString(R.string.onboarding_hero),
                        contexto.getString(R.string.onboarding_descripcion),
                        fotos[0]),
                new Pagina(contexto.getString(R.string.onboarding_slide2_titulo),
                        contexto.getString(R.string.onboarding_slide2_descripcion),
                        fotos[1]),
                new Pagina(contexto.getString(R.string.onboarding_slide3_titulo),
                        contexto.getString(R.string.onboarding_slide3_descripcion),
                        fotos[2])
        };
    }

    public int obtenerCantidad() {
        return paginas.length;
    }

    /** Última posición del carrusel (para saber cuándo mostrar "Comenzar"). */
    public boolean esUltima(int posicion) {
        return posicion == paginas.length - 1;
    }

    @NonNull
    @Override
    public PaginaViewHolder onCreateViewHolder(@NonNull ViewGroup contenedor, int tipoVista) {
        View vista = LayoutInflater.from(contenedor.getContext())
                .inflate(R.layout.item_onboarding_pagina, contenedor, false);
        return new PaginaViewHolder(vista);
    }

    @Override
    public void onBindViewHolder(PaginaViewHolder portador, int posicion) {
        Pagina pagina = paginas[posicion];
        portador.titulo.setText(pagina.titulo);
        portador.descripcion.setText(pagina.descripcion);
        // La foto se pinta como la de cualquier local: mismo cargador,
        // mismo recorte al centro. El logo ya no se usa aquí, así que
        // si la foto no llegara, el respaldo es un marco vacío y no
        // un ícono que finja ser el centro de la página.
        CargadorFotos.pintarRecortada(portador.imagen, portador.itemView.getContext(),
                pagina.foto, 0);
    }

    @Override
    public int getItemCount() {
        return paginas.length;
    }

    static class PaginaViewHolder extends RecyclerView.ViewHolder {
        final TextView titulo;
        final TextView descripcion;
        final ImageView imagen;

        PaginaViewHolder(@NonNull View vista) {
            super(vista);
            titulo = vista.findViewById(R.id.texto_titulo);
            descripcion = vista.findViewById(R.id.texto_descripcion);
            imagen = vista.findViewById(R.id.imagen_portada);
        }
    }
}