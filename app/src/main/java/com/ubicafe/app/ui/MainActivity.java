package com.ubicafe.app.ui;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.ubicafe.app.R;
import com.ubicafe.app.ui.explorar.ExplorarFragment;
import com.ubicafe.app.ui.inicio.InicioFragment;
import com.ubicafe.app.ui.mapa.MapaFragment;

/**
 * Pantalla principal.
 * Contiene las tres pestañas (Inicio, Mapa, Explorar)
 * y se encarga única y exclusivamente de cambiar de fragmento.
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView navegacion = findViewById(R.id.nav_inferior);

        // Al abrir, mostramos la pestaña de Inicio.
        if (savedInstanceState == null) {
            navegacion.setSelectedItemId(R.id.menu_inicio);
        }

        navegacion.setOnItemSelectedListener(menuItem -> {
            Fragment destino = seleccionarFragmento(menuItem.getItemId());
            if (destino != null) {
                cambiarFragmento(destino);
            }
            return true;
        });
    }

    /** Devuelve el fragmento que corresponde a cada pestaña del menú. */
    private Fragment seleccionarFragmento(int idOpcion) {
        if (idOpcion == R.id.menu_mapa) {
            return new MapaFragment();
        }
        if (idOpcion == R.id.menu_explorar) {
            return new ExplorarFragment();
        }
        return new InicioFragment(); // idOpcion == menu_inicio
    }

    private void cambiarFragmento(@NonNull Fragment fragmento) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.contenedor_fragmentos, fragmento)
                .commit();
    }
}