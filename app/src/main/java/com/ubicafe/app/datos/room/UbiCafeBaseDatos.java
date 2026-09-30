package com.ubicafe.app.datos.room;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

/**
 * LA BASE DE DATOS DEL CENSO
 * ---------------------------------------------------------------
 * La clase que Room genera a partir de esta. Room se encarga de abrir
 * SQLite, crear las tablas, guardarlas en el teléfono y traducir cada
 * @Query de los DAO a SQL que SQLite entiende.
 *
 * La base se llama "ubicafe.db" y vive en /data/data/com.ubicafe.app/,
 * el espacio privado de la app. Se puede ver y auditar desde Android
 * Studio con el inspector de bases de datos, o desde una terminal con
 * "adb shell run-as com.ubicafe.app cat databases/ubicafe.db".
 *
 * La interfaz no habla con Room nunca: lo hace RepositorioDatos, que
 * sigue siendo el único punto de acceso. Cambiar el esquema, las
 * consultas o el motor entero no obliga a tocar ninguna pantalla.
 */
@Database(
        entities = {
                FilaEntidad.class,
                FilaRol.class,
                FilaDetalleCafeteria.class,
                FilaDetalleMarcaEntidad.class,
                FilaDetalleTostaderia.class,
                FilaDetalleProductor.class,
                FilaMarca.class,
                FilaSucursal.class,
                FilaVariedad.class,
                FilaMeta.class,
        },
        version = 1,
        exportSchema = false)
public abstract class UbiCafeBaseDatos extends RoomDatabase {

    public static final String NOMBRE = "ubicafe.db";

    public abstract DaoEntidades entidades();

    public abstract DaoRoles roles();

    public abstract DaoDetalles detalles();

    public abstract DaoMarcas marcas();

    public abstract DaoVariedades variedades();

    public abstract DaoMeta meta();

    private static volatile UbiCafeBaseDatos instancia;

    /**
     * Abre (o crea) la base. La instancia se recuerda: Room se encarga
     * de que dos llamadas no abran dos conexiones a la vez.
     */
    public static UbiCafeBaseDatos obtener(Context contexto) {
        UbiCafeBaseDatos actual = instancia;
        if (actual != null) {
            return actual;
        }
        synchronized (UbiCafeBaseDatos.class) {
            if (instancia == null) {
                instancia = Room.databaseBuilder(
                                contexto.getApplicationContext(),
                                UbiCafeBaseDatos.class,
                                NOMBRE)
                        .allowMainThreadQueries()
                        .build();
            }
            return instancia;
        }
    }

    /**
     * allowMainThreadQueries() está puesto a propósito, pero con una
     * condición que hay que cumplir para que siga siendo aceptable: la
     * base solo se consulta para LEER el censo, nunca para escribir desde
     * una pantalla, y la única escritura (el llenado inicial) ocurre en
     * el hilo de fondo que arranca la app.
     *
     * Por qué se permite: el censo son 213 filas y las consultas son por
     * clave primaria o sobre una tabla entera de ese tamaño. SQLite
     * responde a eso en microsegundos, muy por debajo del fotograma de
     * 16 ms. Bloquear el hilo principal con las pantallas esperando, en
     * cambio, sí costaría fluidez.
     *
     * Si algún día la app creciera a decenas de miles de registros, o
     * apareciera escritura desde la interfaz, esto hay que quitarlo y
     * pasar las consultas a un hilo de fondo con callbacks.
     */
}
