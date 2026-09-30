package com.ubicafe.app.util;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.ubicafe.app.R;

/**
 * LAS FOTOS DE LOS LOCALES
 * ---------------------------------------------------------------
 * La ruta de la foto no es una dirección web sino algo como
 * "fotos/cafes/typica.jpg", que es un archivo dentro de los assets del
 * APK. Para que Glide lo encuentre hay que convertirla en una URI con el
 * prefijo file:///android_asset/, que es el único que su cargador de
 * assets acepta. Con asset:/// no hay ningún cargador que sepa abrirlo y
 * la carga falla entera, sin excepción visible: solo un aviso en el
 * logcat y el ícono de respaldo en pantalla.
 *
 * POR QUÉ GLIDE Y NO UN new ImageView().setImageURI()
 * Un setImageURI sobre un asset descodifica la imagen en el hilo
 * principal, y eso en una lista con recycling significa un parón al
 * desplazarse. Glide lee en un hilo aparte, cachea el resultado para no
 * volver a descodificar, cancela la carga si la fila se recicla antes de
 * terminar y hace un crossfade suave al aparecer. Es justo el problema
 * que Glide resuelve, y escribirlo a mano sería rehacerlo peor.
 *
 * DÓNDE ESTÁN LAS IMÁGENES
 * En app/src/main/assets/fotos/, repartidas en cafes, marcas y
 * tostadurias. Son imágenes genéricas de cafeterías y tostadoras, no fotos
 * reales de cada negocio. El aviso está en la propia ficha, debajo del
 * hero, y los créditos completos en la pantalla de créditos. Aun así,
 * conviene que la interfaz no finja más de lo que sabe: por eso el
 * respaldo no es un marco vacío sino el ícono del rol.
 */
public final class CargadorFotos {

    private CargadorFotos() {
        // Clase de utilidades: no se instancia.
    }

    /**
     * Convierte la ruta guardada en la base de datos ("fotos/cafes/x.jpg")
     * en la URI que Glide sabe abrir.
     *
     * El prefijo no es un capricho: AssetUriLoader, el único cargador de
     * assets que trae Glide, solo acepta el esquema "file" y exige que
     * el primer segmento de la ruta sea "android_asset". Cualquier otro
     * esquema, incluido "asset", hace que la carga devuelva null y Glide
     * salte al error() sin más aviso que una línea en el logcat.
     *
     * Va en un método propio y no repetido en las tres llamadas porque
     * este error es invisible: la app compila, la base está bien, el
     * archivo está en el APK y aun así no sale ninguna foto.
     */
    private static String uriDe(String rutaFoto) {
        return "file:///android_asset/" + rutaFoto;
    }

    /**
     * Pinta la foto de un lugar en su ImageView. Si el lugar no tiene
     * foto, deja el ícono del rol a la vista, que es lo que había antes
     * de que existieran las imágenes.
     *
     * Se pasa el contexto de la Activity y no el de la aplicación a
     * propósito: Glide lo usa para saber cuándo ha de soltar la vista
     * cuando la pantalla se destruye.
     */
    public static void pintar(ImageView vista, Context contexto, String rutaFoto,
                             int iconoRespaldo) {
        if (vista == null) {
            return;
        }
        // Antes de pedir nada se limpia la vista: una tarjeta reciclada
        // arrastraría la foto del local anterior hasta que la nueva
        // llegara, y en una lista rápida se vería el cruce de dos fotos.
        Glide.with(contexto).clear(vista);
        vista.setImageResource(iconoRespaldo);

        if (rutaFoto == null || rutaFoto.isEmpty()) {
            return;
        }

        Glide.with(contexto)
                .load(uriDe(rutaFoto))
                // Los datos no cambian nunca, así que no tiene sentido
                // guardar en disco algo que ya viene dentro del APK.
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .placeholder(iconoRespaldo)
                .error(iconoRespaldo)
                .transition(DrawableTransitionOptions.withCrossFade(150))
                .into(vista);
    }

    /**
     * Igual que pintar, pero con un recorte: las fotos del censo son
     * todas de 1024×768, y en una miniatura cuadrada interested el
     * centro. centerCrop no deforma, a diferencia de fitXY.
     */
    public static void pintarRecortada(ImageView vista, Context contexto,
                                       String rutaFoto, int iconoRespaldo) {
        if (vista == null) {
            return;
        }
        Glide.with(contexto).clear(vista);
        vista.setImageResource(iconoRespaldo);

        if (rutaFoto == null || rutaFoto.isEmpty()) {
            return;
        }

        Glide.with(contexto)
                .load(uriDe(rutaFoto))
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .centerCrop()
                .placeholder(iconoRespaldo)
                .error(iconoRespaldo)
                .transition(DrawableTransitionOptions.withCrossFade(150))
                .into(vista);
    }

    /**
     * Precalienta las fotos de una lista al tamaño con el que se van a
     * ver. No es obligatorio: sin esto, la primera vez que se abre la
     * lista las fotos aparecen con un par de milisegundos de retraso.
     * Con esto, llegan listas.
     *
     * EL TAMAÑO ES OBLIGATORIO Y POR QUÉ
     * La versión anterior llamaba a preload() a secas, y eso no fallaba
     * pero tampoco servía para nada: preload() sin argumentos precarga
     * la imagen a su tamaño original (1024×768, unos 3 MB de mapa de
     * bits), y la lista la pide a 56 dp. La caché de Glide no confunde
     * un tamaño con otro, así que la miniatura salía de la caché
     * igualmente y además se quedaba ocupando la memoria con las
     * versiones grandes que nadie iba a usar. Por eso se pide el
     * tamaño exacto: el precalentado solo sirve si es el mismo que el
     * de la carga real.
     *
     * Solo se precargan las primeras, las que están a la vista al
     * abrir. Precargar las 213 sería trabajo inútil y una presión
     * innecesaria sobre la memoria.
     */
    public static void precalentar(Context contexto, Iterable<String> rutas,
                                   int anchoPx, int altoPx) {
        if (contexto == null || rutas == null || anchoPx <= 0 || altoPx <= 0) {
            return;
        }
        RequestManager manager = Glide.with(contexto);
        for (String ruta : rutas) {
            if (ruta == null || ruta.isEmpty()) {
                continue;
            }
            manager.load(uriDe(ruta))
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .override(anchoPx, altoPx)
                    .centerCrop()
                    .preload(anchoPx, altoPx);
        }
    }

    /**
     * Libera la memoria que Glide retiene.
     *
     * clearMemory() tiene que ir en el hilo principal, así que se avisa
     * de ello en el nombre del método en vez de dejar la trampa: llamarlo
     * desde un hilo secundario parece funcionar y a veces no.
     */
    public static void limpiarMemoriaEnHiloPrincipal(Context contexto) {
        Glide.get(contexto.getApplicationContext()).clearMemory();
    }

    /**
     * Igual que pintar, pero en vez de fiarse de lo que queda pintado en
     * la vista, avisa por un callback qué imagen se ha descargado. La
     * usan las pruebas instrumentadas, y de paso el motivo de que exista
     * es que un fallo de carga de Glide no se ve: la vista se queda con
     * el ícono de respaldo y no hay ninguna excepción, así que sin una
     * llamada de vuelta no hay forma de distinguir "todavía no ha
     * llegado" de "no va a llegar nunca".
     *
     * @param alTerminar recibe la imagen cargada, o null si la carga falló.
     */
    public static void pintarConEscucha(ImageView vista, Context contexto,
                                        String rutaFoto, int iconoRespaldo,
                                        AlTerminarCarga alTerminar) {
        if (vista == null || contexto == null || alTerminar == null) {
            return;
        }
        Glide.with(contexto).clear(vista);
        vista.setImageResource(iconoRespaldo);

        if (rutaFoto == null || rutaFoto.isEmpty()) {
            alTerminar.terminada(null);
            return;
        }

        Glide.with(contexto)
                .load(uriDe(rutaFoto))
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(GlideException e, Object model,
                                                Target<Drawable> target, boolean isFirstResource) {
                        alTerminar.terminada(null);
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable recurso, Object model,
                                                   Target<Drawable> target, DataSource source,
                                                   boolean isFirstResource) {
                        alTerminar.terminada(recurso);
                        return false;
                    }
                })
                .into(vista);
    }

    /**
     * Callback de la carga de una foto.
     */
    public interface AlTerminarCarga {
        /**
         * @param recurso la imagen cargada, o null si la carga falló.
         */
        void terminada(Drawable recurso);
    }

    /**
     * El ícono que corresponde a un lugar sin foto. Cada papel tiene el
     * suyo, y el respaldo no es el mismo para todos: una tostaduría sin
     * foto no puede parecerse a una cafetería sin foto.
     */
    public static int respaldo(com.ubicafe.app.modelo.Rol rol) {
        if (rol == null) {
            return R.drawable.ic_cafeteria;
        }
        return rol.icono();
    }
}
