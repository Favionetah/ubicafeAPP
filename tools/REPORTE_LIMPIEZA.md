# Reporte de limpieza del censo

Generado por `tools/generar_censo.py`. No editar a mano: el JSON se
reproduce con `python3 tools/generar_censo.py`.

## Resultado de la validación

Todos los conteos coinciden con la hoja RESUMEN del censo y no
queda ninguna fila de detalle sin Entidad a la que pertenecer.


## Conteos

| Qué | Cuántos |
|---|---|
| Entidades en el mapa | 213 |
| Con ficha de cafetería | 188 |
| Con ficha de tostaduría | 13 |
| Con ficha de productor | 12 |
| Con ficha de marca | 78 |
| Marcas en el catálogo | 40 |
| Puntos de venta | 78 |
| Cafés de origen | 66 |
| Entidades de una sola ficha | 116 |
| Entidades con dos o más fichas | 79 |
| Direcciones ausentes | 3 |
| Ubicaciones con aviso del censo | 33 |
| Puntos de venta sin entidad | 0 |

## Cómo se cruzaron las hojas

Las hojas del censo no comparten identificador, así que el cruce es
por nombre normalizado y, cuando el nombre se repite porque hay
sucursales de una misma cadena, se desempata con la distancia entre
coordenadas, de modo que cada sucursal conserva su propia entidad.

| Hoja de detalle | Cruzadas |
|---|---|
| detalleCafeteria | 188 de 188 |
| detalleTostaderia | 13 de 13 |
| detalleProductor | 12 de 12 |
| punto de venta | 78 de 78 |

## Cambios aplicados

### campo sin dato real (26)

- `0` → `(oculto)`  — Almara
- `no sabe` → `(oculto)`  — Andean Hills
- `None` → `(oculto)`  — Alexander Coffee Multicine
- `No sabe` → `(oculto)`  — Café chulumani Genaro Ramos
- `0.0` → `(oculto)`  — Sin Fronteras

### cruce por coordenadas (18)

- `AMANITA CAFE MARKET` → `Amanita`  — detalleCafeteria
- `Veirut` → `Beirut`  — detalleCafeteria
- `Biofilia` → `Biofilia SRL`  — detalleCafeteria
- `Bistro Café la Baguetteria` → `Bistro`  — detalleCafeteria
- `Bolivian Coffee` → `Bolivian Cofee`  — detalleCafeteria
- `Cafe Vida La Paz` → `Cafe vida`  — detalleCafeteria
- `Café Chulumani` → `Café chulumani Genaro Ramos`  — detalleCafeteria
- `Crazzy Market` → `Crazy Market`  — detalleCafeteria
- `Uta Ideas` → `Cultoras`  — detalleCafeteria
- `Caferia fahrenheit` → `Fahrenheit`  — detalleCafeteria
- `Juan Valdez Cafe` → `JUAN VALDEZ`  — detalleCafeteria
- `Luna` → `Lunas`  — detalleCafeteria
- _…y 6 casos más_

### errata del censo (7)

- `Coffe arábica` → `Coffea arabica`  — AGROECOLOGIA NAKHAKI
- `Catuai rojo` → `Catuaí rojo`  — Coccos

### dirección que era un enlace (3)

- `https://maps.app.goo.gl/zbLijKYS3xFW47Ci7?g_st=aw` → `(se conserva como mapaUrl)`  — Fortaleza Coffe
- `https://maps.app.goo.gl/yLA7ikPPvkyeSzhw6` → `(se conserva como mapaUrl)`  — AGROECOLOGIA NAKHAKI
- `https://maps.app.goo.gl/fqgaqQSG8hFhNwLaA?g_st=aw` → `(se conserva como mapaUrl)`  — Barbecue - café restaurant


## Avisos de ubicación que dejó el propio censo

El censo marcó 33 ubicaciones para revisión. La app las dibuja en el
mapa igual que las demás, pero conserva el motivo en `notaUbicacion`.

- `altitud 0 (GPS débil)` (9)
- `signo corregido; altitud 0 (GPS débil)` (8)
- `altitud 0 (GPS débil); precisión 2000m` (6)
- `signo corregido` (2)
- `precisión 2000m` (2)
- `signo corregido; precisión 510m` (1)
- `precisión 510m` (1)
- `precisión 508m` (1)
- `altitud 0 (GPS débil); precisión 135m` (1)
- `precisión 3250m` (1)
- `altitud 0 (GPS débil); precisión 200m` (1)

## Decisiones que conviene revisar

1. **No hay precios en el censo.** Ninguna de las dos hojas tiene una
   columna de precio, así que el JSON no lleva ninguno y la app no
   muestra bloque de precio. `meta.sinPrecio` lo deja explícito.
2. **La columna de altitud del Excel no se usó.** Es la altitud del GPS,
   o sea la altura de La Paz donde está el local, no la del cafetal.
3. **Los nombres se guardan tal cual los escribió el negocio.** No se
   corrigieron mayúsculas ni minúsculas para no inventar cómo se
   escribe cada marca. La búsqueda ignora mayúsculas y tildes.
4. **Casi todas las fichas de marca están vacías.** De los 40 campos
   del catálogo, solo `esNacional` está completo en las 40 marcas; el
   canal de comercialización y la cobertura, en 4. La ficha de marca
   tiene que poder mostrarse casi vacía sin romperse.
5. **La ficha de cafetería solo tiene datos duros en 66 de 188.** Las
   188 tienen `tipoApp`, pero año de apertura, mesas, capacidad y
   baristas solo están en 66. La hoja RESUMEN dice 69; la diferencia
   son tres encuestas marcadas como completadas con los campos en blanco.
