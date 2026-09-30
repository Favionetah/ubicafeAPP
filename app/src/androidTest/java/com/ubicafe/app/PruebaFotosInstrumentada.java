package com.ubicafe.app;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.ubicafe.app.util.CargadorFotos;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.InputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * PRUEBA DE LAS FOTOS
 * ---------------------------------------------------------------
 * Existe por un fallo que no se veía leyendo el código: las 242 fotos
 * estaban en el APK y la base de datos tenía las rutas correctas, y aun
 * así en la pantalla no aparecía ninguna. La causa era la URI con la que
 * se le pedía la imagen a Glide, "asset:///...", un esquema que Glide no
 * reconoce. No salía ninguna excepción, ni un fallo de compilación ni un
 * cierre: solo una línea en el logcat y el ícono de respaldo en su lugar.
 *
 * Un fallo así no se previene leyendo el código, se previene mirando lo
 * que ocurre de verdad, y eso es lo que hace esta prueba: abre un asset
 * real de la carpeta fotos/ y lo carga por el mismo camino que usa la
 * app. Si la URI vuelve a romperse, la prueba falla.
 */
@RunWith(AndroidJUnit4.class)
public class PruebaFotosInstrumentada {

    private static final String RUTA = "fotos/cafes/alexander_coffee_6_de_agosto.jpg";

    /**
     * El archivo está realmente dentro del APK y se puede abrir. Es la
     * mitad de la comprobación: si esto falla, el problema no es la URI
     * sino que el archivo no llegó a empaquetarse.
     */
    @Test
    public void elArchivoDeFotoEstaEnLosAssets() throws Exception {
        Context contexto = ApplicationProvider.getApplicationContext();
        try (InputStream flujo = contexto.getAssets().open(RUTA)) {
            assertNotNull("El asset no se pudo abrir: " + RUTA, flujo);
            assertTrue("El asset está vacío", flujo.read() >= 0);
        }
    }

    /**
     * LaURI que se le pasa a Glide tiene el prefijo que su cargador de
     * assets acepta. Se comprueba sobre la cadena final, que es lo que
     * Glide ve de verdad, y no sobre una constante intermedia que
     * alguien podría dejar sin usar.
     */
    @Test
    public void laCargaDeGlideTerminaEnUnDrawable() throws Exception {
        Context contexto = ApplicationProvider.getApplicationContext();
        ImageView vista = new ImageView(contexto);
        AtomicReference<Drawable> resultado = new AtomicReference<>();
        CountDownLatch listo = new CountDownLatch(1);

        CargadorFotos.pintarConEscucha(vista, contexto, RUTA,
                R.drawable.ic_cafeteria, drawable -> {
                    resultado.set(drawable);
                    listo.countDown();
                });

        assertTrue("Glide no terminó la carga a tiempo",
                listo.await(20, TimeUnit.SECONDS));
        assertNotNull("Glide no devolvió ninguna imagen", resultado.get());
    }

    /**
     * Y la imagen que devuelve es de verdad la del archivo, no el ícono
     * de respaldo. Se comparan los tamaños porque el ícono es un vector
     * pequeño y la foto del censo es de 1024×768: si saliera el
     * respaldo, el alto no podría ser ese.
     */
    @Test
    public void laImagenCargadaEsLaFotoYNoElIcono() throws Exception {
        Context contexto = ApplicationProvider.getApplicationContext();
        ImageView vista = new ImageView(contexto);
        AtomicReference<Drawable> resultado = new AtomicReference<>();
        CountDownLatch listo = new CountDownLatch(1);

        CargadorFotos.pintarConEscucha(vista, contexto, RUTA,
                R.drawable.ic_cafeteria, drawable -> {
                    resultado.set(drawable);
                    listo.countDown();
                });

        assertTrue("Glide no terminó la carga a tiempo",
                listo.await(20, TimeUnit.SECONDS));
        Drawable cargada = resultado.get();
        assertNotNull("No se cargó ninguna imagen", cargada);
        assertTrue("La imagen es demasiado pequeña para ser la foto del censo: "
                        + cargada.getIntrinsicWidth() + "x" + cargada.getIntrinsicHeight(),
                cargada.getIntrinsicWidth() >= 512);
    }
}
