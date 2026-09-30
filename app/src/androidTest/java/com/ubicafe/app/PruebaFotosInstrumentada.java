package com.ubicafe.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.widget.ImageView;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.ubicafe.app.ui.OnboardingAdapter;
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
     * La URI que se le pasa a Glide tiene el prefijo que su cargador de
     * assets acepta. Se comprueba sobre la cadena final, que es lo que
     * Glide ve de verdad, y no sobre una constante intermedia que
     * alguien podría dejar sin usar.
     *
     * Dos detalles hacen que esto no sea un await() normal:
     *
     *  1. La carga se pide desde el hilo principal porque Glide lo
     *     exige: entrar en un ImageView toca la vista, y las vistas
     *     solo se tocan desde donde se pintan.
     *  2. Glide devuelve el resultado en el hilo principal, así que
     *     quedarse con await() a secas no sirve: el hilo de la prueba
     *     miraría una puerta que abre otro hilo, y ese otro hilo
     *     necesitaría que lo dejaran trabajar. Por eso se bombea el
     *     looper con waitForIdleSync() y se reintenta, en vez de
     *     esperar los veinte segundos de una sentada.
     */
    @Test
    public void laCargaDeGlideTerminaEnUnDrawable() throws Exception {
        Context contexto = ApplicationProvider.getApplicationContext();
        AtomicReference<Drawable> resultado = new AtomicReference<>();
        CountDownLatch listo = new CountDownLatch(1);

        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            ImageView vista = new ImageView(contexto);
            vista.layout(0, 0, 512, 512);
            CargadorFotos.pintarConEscucha(vista, contexto, RUTA,
                    R.drawable.ic_cafeteria, recurso -> {
                        resultado.set(recurso);
                        listo.countDown();
                    });
        });

        esperarListo(listo);
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
        AtomicReference<Drawable> resultado = new AtomicReference<>();
        CountDownLatch listo = new CountDownLatch(1);

        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            ImageView vista = new ImageView(contexto);
            vista.layout(0, 0, 512, 512);
            CargadorFotos.pintarConEscucha(vista, contexto, RUTA,
                    R.drawable.ic_cafeteria, recurso -> {
                        resultado.set(recurso);
                        listo.countDown();
                    });
        });

        esperarListo(listo);
        Drawable cargada = resultado.get();
        assertNotNull("No se cargó ninguna imagen", cargada);
        assertTrue("La imagen es demasiado pequeña para ser la foto del censo: "
                        + cargada.getIntrinsicWidth() + "x" + cargada.getIntrinsicHeight(),
                cargada.getIntrinsicWidth() >= 512);
    }

    /**
     * Espera a que la carga termine sin dejar el looper principal parado.
     * Sale cuando la cuenta llega a cero o cuando se agota el plazo, y es
     * la prueba quien decide si eso es un fallo.
     */
    private static void esperarListo(CountDownLatch listo) throws InterruptedException {
        long limite = System.currentTimeMillis() + 20_000L;
        while (listo.getCount() > 0 && System.currentTimeMillis() < limite) {
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            if (listo.await(200, TimeUnit.MILLISECONDS)) {
                return;
            }
        }
        assertTrue("Glide no terminó la carga a tiempo", listo.getCount() == 0);
    }

    /**
     * Las portadas del onboarding llaman a pintarRecortada con un
     * respaldo de 0, que significa "sin respaldo". Ese camino tuvo un
     * NullPointerException que tumbaba la app al arrancar: al escribir
     * el respaldo como "cond ? null : icono", Java unifica el tipo del
     * ternario como Integer, y placeholder() pide un int, así que al
     * desempaquetar el null petaba. El compilador no lo ve y la
     * revisión menos.
     *
     * La prueba carga una foto por ese mismo camino y exige que llegue
     * a la vista. Si el método peta, la app cae y la prueba falla sin
     * más explicación.
     */
    @Test
    public void pintarRecortadaSinRespaldoPoneLaFoto() throws Exception {
        Context contexto = ApplicationProvider.getApplicationContext();
        AtomicReference<Drawable> puesta = new AtomicReference<>();
        AtomicReference<ImageView> caja = new AtomicReference<>();

        // waitForIdleSync() no se puede llamar desde el hilo principal,
        // así que la carga se pide ahí y la espera se hace desde aquí.
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            ImageView vista = new ImageView(contexto);
            vista.layout(0, 0, 512, 512);
            caja.set(vista);
            // 0 = sin respaldo. Es lo que usan las portadas.
            CargadorFotos.pintarRecortada(vista, contexto, RUTA, 0);
        });

        long limite = System.currentTimeMillis() + 20_000L;
        String visto = "nunca hubo drawable";
        while (System.currentTimeMillis() < limite) {
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            Drawable d = caja.get() == null ? null : caja.get().getDrawable();
            if (d != null) {
                visto = d.getClass().getSimpleName() + " "
                        + d.getIntrinsicWidth() + "x" + d.getIntrinsicHeight();
                if (contieneBitmapDeLaFoto(d)) {
                    puesta.set(d);
                    break;
                }
            }
            Thread.sleep(100);
        }

        assertNotNull("La vista se quedó sin la foto al pedirla con respaldo 0. "
                + "Lo último que se vio fue: " + visto, puesta.get());
    }

    /**
     * Si el drawable esconde un bitmap, y si ese bitmap es del tamaño de
     * la foto del censo.
     *
     * No se puede mirar solo el drawable de fuera porque con centerCrop
     * y el fundido, lo que queda puesto es un TransitionDrawable: un
     * BitmapDrawable de la foto se ve ahí dentro. Un ícono de respaldo
     * sería un VectorDrawable, que no tiene bitmap, así que esta
     * comprobación también distingue el uno del otro.
     */
    private static boolean contieneBitmapDeLaFoto(Drawable d) {
        if (d instanceof BitmapDrawable) {
            Bitmap b = ((BitmapDrawable) d).getBitmap();
            return b != null && b.getWidth() >= 512;
        }
        // TransitionDrawable, que es lo que deja centerCrop con el
        // fundido, es un LayerDrawable: la foto es una de sus capas.
        if (d instanceof LayerDrawable) {
            LayerDrawable capas = (LayerDrawable) d;
            for (int i = 0; i < capas.getNumberOfLayers(); i++) {
                if (contieneBitmapDeLaFoto(capas.getDrawable(i))) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Las tres fotos del carrusel de onboarding.
     *
     * Van escritas a mano dentro de OnboardingAdapter, así que un
     * renombrado en assets/ las deja rotas sin que se entere el
     * compilador: la ruta es un texto y el compilador no comprueba que
     * exista. Esta prueba ata la lista de la clase con lo que hay
     * realmente en el APK.
     */
    @Test
    public void lasFotosDelOnboardingEstanEnLosAssets() throws Exception {
        Context contexto = ApplicationProvider.getApplicationContext();
        String[] rutas = OnboardingAdapter.rutasDeFotos();
        assertEquals("El carrusel debería tener tres diapositivas con foto",
                3, rutas.length);
        for (String ruta : rutas) {
            try (InputStream flujo = contexto.getAssets().open(ruta)) {
                assertNotNull("La foto del onboarding no existe: " + ruta, flujo);
            }
        }
    }
}
