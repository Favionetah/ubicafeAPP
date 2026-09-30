package com.ubicafe.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.ubicafe.app.R;
import com.ubicafe.app.ui.explorar.ExplorarFragment;
import com.ubicafe.app.ui.inicio.InicioFragment;
import com.ubicafe.app.ui.mapa.MapaFragment;

import java.util.ArrayList;
import java.util.List;

/**
 * Pantalla principal.
 * Contiene las tres pestañas (Inicio, Mapa, Explorar)
 * y se encarga única y exclusivamente de cambiar de fragmento.
 *
 * Easter egg: si se toca el patrón Inicio·Inicio·Inicio·Mapa·Mapa·Mapa·
 * Explorar·Explorar·Explorar·Inicio·Mapa·Explorar en la barra inferior,
 * se abre la pantalla secreta GhostActivity.
 */
public class MainActivity extends AppCompatActivity {

    /** Patrón oculto que desbloquea la ghost page. */
    private static final int[] PATRON_OCULTO = {
            R.id.menu_inicio, R.id.menu_inicio, R.id.menu_inicio,
            R.id.menu_mapa, R.id.menu_mapa, R.id.menu_mapa,
            R.id.menu_explorar, R.id.menu_explorar, R.id.menu_explorar,
            R.id.menu_inicio, R.id.menu_mapa, R.id.menu_explorar,
    };

    /** Clave con la que se guarda la pestaña activa al girar la pantalla. */
    private static final String CLAVE_PESTANA = "pestana_activa";

    /** Últimos toques de la barra inferior (ventana del tamaño del patrón). */
    private final List<Integer> ultimosToques = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView navegacion = findViewById(R.id.nav_inferior);

        // El listener va PRIMERO: setSelectedItemId dispara la selección a
        // través de él, así que registrado después se pierde la primera
        // llamada y el contenedor queda en blanco hasta que se toque la barra.
        navegacion.setOnItemSelectedListener(this::alSeleccionarPestana);

        // Arranque en frío: no hay ningún fragmento restaurado, así que lo
        // cargamos explícitamente. No nos apoyamos en setSelectedItemId para
        // esto porque la barra ya viene con Inicio marcado y volver a marcar
        // la opción actual no vuelve a invocar al listener.
        if (savedInstanceState == null) {
            if (navegacion.getSelectedItemId() != R.id.menu_inicio) {
                navegacion.setSelectedItemId(R.id.menu_inicio);
            }
            cambiarFragmento(seleccionarFragmento(R.id.menu_inicio));
            return;
        }

        // Tras una rotación el FragmentManager ya restauró el fragmento, pero
        // la barra vuelve a empezar en Inicio. Marcamos la opción correcta
        // silenciando el listener un momento: si no, la sincronización
        // reemplazaría el fragmento restaurado por uno nuevo y se perdería su
        // estado (el mapa es lo más caro de reconstruir).
        int pestana = savedInstanceState.getInt(CLAVE_PESTANA, R.id.menu_inicio);
        if (navegacion.getSelectedItemId() != pestana) {
            navegacion.setOnItemSelectedListener(null);
            navegacion.setSelectedItemId(pestana);
            navegacion.setOnItemSelectedListener(this::alSeleccionarPestana);
        }
    }

    private boolean alSeleccionarPestana(MenuItem menuItem) {
        registrarToque(menuItem.getItemId());

        Fragment destino = seleccionarFragmento(menuItem.getItemId());
        if (destino != null) {
            cambiarFragmento(destino);
        }
        return true;
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        BottomNavigationView navegacion = findViewById(R.id.nav_inferior);
        outState.putInt(CLAVE_PESTANA, navegacion.getSelectedItemId());
    }

    /** Acumula el toque actual y comprueba si completa el patrón secreto. */
    private void registrarToque(int idOpcion) {
        ultimosToques.add(idOpcion);
        if (ultimosToques.size() > PATRON_OCULTO.length) {
            ultimosToques.remove(0);
        }
        if (esPatronOculto()) {
            ultimosToques.clear();
            startActivity(new Intent(this, GhostActivity.class));
        }
    }

    private boolean esPatronOculto() {
        if (ultimosToques.size() != PATRON_OCULTO.length) {
            return false;
        }
        for (int i = 0; i < PATRON_OCULTO.length; i++) {
            if (ultimosToques.get(i) != PATRON_OCULTO[i]) {
                return false;
            }
        }
        return true;
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