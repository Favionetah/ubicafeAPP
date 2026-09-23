package com.ubicafe.app.ui.marcas;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ubicafe.app.R;
import com.ubicafe.app.datos.RepositorioDatos;
import com.ubicafe.app.modelo.MarcaCafe;

import java.util.List;

/**
 * LISTA DE MARCAS DE CAFÉ.
 * Muestra todas las marcas nacionales registradas.
 */
public class ListaMarcasActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lista_marcas);

        ((TextView) findViewById(R.id.texto_titulo)).setText(getString(R.string.categoria_marcas));

        findViewById(R.id.btn_volver).setOnClickListener(v -> finish());

        List<MarcaCafe> marcas = RepositorioDatos.obtenerMarcas();
        RecyclerView lista = findViewById(R.id.lista);
        lista.setLayoutManager(new LinearLayoutManager(this));
        lista.setAdapter(new AdaptadorMarca(marcas));
    }
}