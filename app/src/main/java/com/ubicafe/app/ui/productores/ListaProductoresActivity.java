package com.ubicafe.app.ui.productores;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ubicafe.app.R;
import com.ubicafe.app.datos.RepositorioDatos;
import com.ubicafe.app.modelo.Productor;

import java.util.List;

/**
 * LISTA DE PRODUCTORES.
 * Muestra todas las fincas cafetaleras registradas.
 */
public class ListaProductoresActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lista_productores);

        ((TextView) findViewById(R.id.texto_titulo)).setText(getString(R.string.productores_titulo));

        findViewById(R.id.btn_volver).setOnClickListener(v -> finish());

        List<Productor> productores = RepositorioDatos.obtenerProductores();
        RecyclerView lista = findViewById(R.id.lista);
        lista.setLayoutManager(new LinearLayoutManager(this));
        lista.setAdapter(new AdaptadorProductor(productores));
    }
}