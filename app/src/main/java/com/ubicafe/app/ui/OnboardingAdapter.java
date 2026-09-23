package com.ubicafe.app.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ubicafe.app.R;

/**
 * ADAPTADOR DEL CARRUSEL DE ONBOARDING (P2).
 * Cada diapositiva muestra hero + título + descripción.
 * El contenido de cada página se pasa como pares de recursos de texto.
 */
public class OnboardingAdapter extends RecyclerView.Adapter<OnboardingAdapter.PaginaViewHolder> {

    /** Información de una diapositiva del carrusel. */
    public static class Pagina {
        public final String titulo;
        public final String descripcion;

        public Pagina(String titulo, String descripcion) {
            this.titulo = titulo;
            this.descripcion = descripcion;
        }
    }

    private final Pagina[] paginas;

    public OnboardingAdapter(Context contexto) {
        paginas = new Pagina[]{
                new Pagina(contexto.getString(R.string.onboarding_hero),
                        contexto.getString(R.string.onboarding_descripcion)),
                new Pagina(contexto.getString(R.string.onboarding_slide2_titulo),
                        contexto.getString(R.string.onboarding_slide2_descripcion)),
                new Pagina(contexto.getString(R.string.onboarding_slide3_titulo),
                        contexto.getString(R.string.onboarding_slide3_descripcion))
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
    public void onBindViewHolder(@NonNull PaginaViewHolder portador, int posicion) {
        Pagina pagina = paginas[posicion];
        portador.titulo.setText(pagina.titulo);
        portador.descripcion.setText(pagina.descripcion);
    }

    @Override
    public int getItemCount() {
        return paginas.length;
    }

    static class PaginaViewHolder extends RecyclerView.ViewHolder {
        final TextView titulo;
        final TextView descripcion;

        PaginaViewHolder(@NonNull View vista) {
            super(vista);
            titulo = vista.findViewById(R.id.texto_titulo);
            descripcion = vista.findViewById(R.id.texto_descripcion);
        }
    }
}