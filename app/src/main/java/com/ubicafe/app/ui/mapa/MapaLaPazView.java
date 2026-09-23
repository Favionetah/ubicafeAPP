package com.ubicafe.app.ui.mapa;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * MAPA ESTÁTICO DE LA PAZ.
 * ---------------------------------------------------------------
 * Dibuja un plano estilizado de la ciudad (zonas, calles y cerros)
 * y coloca marcadores usando las coordenadas reales lat/lng.
 *
 * CUANDO SE ACTIVE GOOGLE MAPS, este lienzo se reemplaza por un
 * SupportMapFragment pegado a los mismos datos: ver MapaActivity,
 * método mostrarConGoogleMaps(). Mientras tanto, esta vista hace
 * que la app se vea "como en el diseño" sin necesitar internet.
 * ---------------------------------------------------------------
 */
public class MapaLaPazView extends View {

    /** Notifica cuando el usuario toca un marcador. */
    public interface AlSeleccionarMarcador {
        void alSeleccionar(MarcadorMapa marcador);
    }

    // Recuadro geográfico que abarca La Paz (para proyectar lat/lng al lienzo).
    private static final double LAT_NORTE = -16.465;
    private static final double LAT_SUR = -16.555;
    private static final double LNG_OESTE = -68.155;
    private static final double LNG_ESTE = -68.045;

    private final List<MarcadorMapa> marcadores = new ArrayList<>();
    private AlSeleccionarMarcador listener;

    // Pinceles de dibujo (se preparan una sola vez).
    private final Paint pincelFondo = new Paint();
    private final Paint pincelCerro = new Paint();
    private final Paint pincelCalle = new Paint();
    private final Paint pincelZona = new Paint();
    private final Paint pincelMarcador = new Paint();
    private final Paint pincelMarcadorCentro = new Paint();
    private final Paint pincelTextoZona = new Paint();
    private final Paint pincelTextoFino = new Paint();

    public MapaLaPazView(Context context) {
        this(context, null);
    }

    public MapaLaPazView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        prepararPinceles();
    }

    private void prepararPinceles() {
        // Fondo: verde tierra de los Yungas/altiplano
        pincelFondo.setColor(Color.rgb(0xE8, 0xEF, 0xE4));
        pincelFondo.setStyle(Paint.Style.FILL);

        // Cerro/colina detrás de la ciudad (detalle decorativo)
        pincelCerro.setColor(Color.rgb(0xCB, 0xDE, 0xC2));
        pincelCerro.setStyle(Paint.Style.FILL);

        // Calles principales
        pincelCalle.setColor(Color.rgb(0xF4, 0xF1, 0xE8));
        pincelCalle.setStrokeWidth(6f);
        pincelCalle.setStrokeCap(Paint.Cap.ROUND);
        pincelCalle.setStyle(Paint.Style.STROKE);

        // Mancha de la "zona cercana" (efecto suave)
        pincelZona.setColor(Color.rgb(0xB9, 0xD0, 0xBF));
        pincelZona.setStyle(Paint.Style.FILL);

        // Marcador: círculo relleno con borde
        pincelMarcador.setStyle(Paint.Style.FILL);
        pincelMarcadorCentro.setColor(Color.WHITE);
        pincelMarcadorCentro.setStyle(Paint.Style.FILL);

        // Textos
        pincelTextoZona.setColor(Color.rgb(0x59, 0x6A, 0x5E));
        pincelTextoZona.setTextSize(34f);
        pincelTextoZona.setTextAlign(Paint.Align.CENTER);
        pincelTextoZona.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);

        pincelTextoFino.setColor(Color.rgb(0x3A, 0x4A, 0x40));
        pincelTextoFino.setTextSize(30f);
    }

    /** Establece la lista de marcadores y redibuja. */
    public void setMarcadores(List<MarcadorMapa> lista) {
        marcadores.clear();
        marcadores.addAll(lista);
        invalidate();
    }

    /** El Activity se suscribe aquí para saber qué marcador se tocó. */
    public void setListenerAlSeleccionar(AlSeleccionarMarcador listener) {
        this.listener = listener;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Fondo general
        canvas.drawRect(0, 0, getWidth(), getHeight(), pincelFondo);

        // Cerro decorativo
        canvas.drawCircle(getWidth() * 0.72f, getHeight() * 0.18f,
                getWidth() * 0.32f, pincelCerro);

        // Calles principales (trazos diagonales)
        canvas.drawLine(0, getHeight() * 0.72f, getWidth(), getHeight() * 0.82f, pincelCalle);
        canvas.drawLine(getWidth() * 0.2f, 0, getWidth() * 0.75f, getHeight(), pincelCalle);
        canvas.drawLine(0, getHeight() * 0.35f, getWidth() * 0.6f, getHeight() * 0.9f, pincelCalle);

        // Mancha suave "cercana" al centro
        canvas.drawOval(new RectF(getWidth() * 0.28f, getHeight() * 0.30f,
                getWidth() * 0.72f, getHeight() * 0.62f), pincelZona);

        // Etiqueta del título del mapa
        canvas.drawText("LA PAZ · BOLIVIA", getWidth() / 2f, getHeight() * 0.13f, pincelTextoZona);

        // Zonas (nombres fijos como en el diseño)
        String[] zonas = {"SAN MIGUEL", "ACHUMANI", "MIRAFLORES", "SOPOCACHI", "CENTRO"};
        float[] coordenadasZonas = {
                0.72f, 0.46f,
                0.86f, 0.72f,
                0.62f, 0.52f,
                0.42f, 0.44f,
                0.30f, 0.60f
        };
        for (int i = 0; i < zonas.length; i++) {
            canvas.drawText(zonas[i],
                    getWidth() * coordenadasZonas[i * 2],
                    getHeight() * coordenadasZonas[i * 2 + 1],
                    pincelTextoFino);
        }

        // Marcadores: cada uno es un pin con su letra inicial
        for (MarcadorMapa marcador : marcadores) {
            float x = aX(marcador.lng);
            float y = aY(marcador.lat);
            pincelMarcador.setColor(marcador.color);
            canvas.drawCircle(x, y, 22f, pincelMarcador);
            canvas.drawCircle(x, y, 12f, pincelMarcadorCentro);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP && listener != null) {
            float x = event.getX();
            float y = event.getY();

            // Buscar el marcador más cercano (radio de 60 px de tolerancia)
            MarcadorMapa elegido = null;
            float distMin = Float.MAX_VALUE;
            for (MarcadorMapa marcador : marcadores) {
                float mx = aX(marcador.lng);
                float my = aY(marcador.lat);
                float dist = (float) Math.hypot(mx - x, my - y);
                if (dist < 60f && dist < distMin) {
                    distMin = dist;
                    elegido = marcador;
                }
            }
            if (elegido != null) {
                listener.alSeleccionar(elegido);
                return true;
            }
        }
        return super.onTouchEvent(event);
    }

    // ---------- Proyección de coordenadas reales al lienzo ----------

    private float aX(double lng) {
        float proporcion = (float) ((lng - LNG_OESTE) / (LNG_ESTE - LNG_OESTE));
        return getWidth() * proporcion;
    }

    private float aY(double lat) {
        float proporcion = (float) ((LAT_NORTE - lat) / (LAT_SUR - LAT_NORTE));
        return getHeight() * proporcion;
    }
}