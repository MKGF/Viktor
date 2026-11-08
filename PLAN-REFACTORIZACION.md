# Plan de refactorización de Viktor

## Propósito

Hacer que el código de Viktor sea más fácil de seguir para una persona sin cambiar lo que
hace el motor. Este documento recoge el norte del refactor, los acuerdos y las decisiones que
debemos validar antes de implementar cada etapa.

La dificultad no está solo en el formato: para entender una búsqueda hay que seguir a la vez
las reglas del ajedrez, el estado del tablero, los algoritmos y las optimizaciones. Queremos
que el flujo principal exprese la intención, sin ocultar la complejidad que realmente existe.

## Modelo mental acordado

Cuando le toca jugar, Viktor **piensa y comunica una decisión**. Utiliza sus herramientas para
analizar la posición y escoger la mejor opción; no necesita saber quién ha solicitado esa
decisión ni cómo se presenta o ejecuta.

```text
Posición + límites
        |
        v
Motor: pensar con sus herramientas
        |
        v
Decisión + información de la búsqueda
        |
        +--> Consola: interpretar la decisión y, si procede, aplicar la jugada.
        |
        +--> UCI: comunicar la respuesta; la interfaz gráfica controla la partida.
```

La decisión no siempre es una jugada: puede incluir una reclamación de tablas o reflejar que
la partida ha terminado. Una búsqueda también puede detenerse o agotar el tiempo. Estos casos
deben conservar su comportamiento actual, no convertirse silenciosamente en una jugada.

**Habrá una fachada `Engine` con un método `think()`** como punto de entrada para este proceso.
Sus parámetros y el detalle de su contrato se definirán al revisar los usos actuales. La
fachada debe hacer comprensible cómo se llega a una decisión, no limitarse a añadir un nombre
nuevo sin aclarar responsabilidades.

Como punto de partida, reutilizaremos `SearchResult`: ya contiene la jugada, una posible
reclamación de tablas y los datos de búsqueda. Solo añadiremos otra representación si el diseño
o sus contratos demuestran que es necesaria. Mantendremos su comportamiento y los contratos
existentes, salvo los renombres de API acordados explícitamente.

### Responsabilidades

- **Motor:** analizar la posición, utilizar las herramientas de búsqueda y devolver la decisión
  y la información necesaria para interpretarla. No depende de consola ni de UCI.
- **Tablero y reglas:** representar y validar el estado y las jugadas. La búsqueda puede
  modificar temporalmente su copia de trabajo, pero no debe modificar permanentemente la
  posición de la partida que recibe.
- **Consola:** interpretar la decisión; si corresponde jugar, aplicar la jugada y comunicar el
  resultado. `ConsoleGame` ya aplica los movimientos sobre `Board`; conservará esa
  responsabilidad, así como la gestión actual de reclamaciones y errores.
- **UCI:** traducir la decisión al protocolo. La interfaz gráfica controla el tablero y el
  resultado; el motor no aplica su jugada a la partida de la interfaz.

En particular, no se propone un `makeMove()` en el motor: **decidir y aplicar una jugada son
responsabilidades distintas**.

### El motor y sus herramientas

Queremos un único motor conceptual, apoyado en las herramientas que necesita para decidir. Las
clases de búsqueda son algoritmos al servicio de ese motor, no distintas personalidades de
Viktor. Sus antiguos nombres `*Engine` se han sustituido por nombres que describen su función.

La organización concreta se diseñará durante el refactor. Como mapa inicial:

- `IterativeDeepeningSearch` repite la búsqueda a profundidades crecientes y conserva la última
  iteración completada; se apoya en `AlphaBetaSearch`.
- `AlphaBetaSearch` explora y poda el árbol a una profundidad determinada. Se apoya, entre otras
  cosas, en la generación y ordenación de jugadas, la evaluación, quiescence y la tabla de
  transposiciones.
- **Minimax** es la idea de elegir una jugada considerando que el rival también intentará
  responder de la mejor manera; alpha-beta la utiliza con poda. La clase independiente
  `MinimaxSearch` no se usa actualmente en el código de producción, aunque sí en
  tests, incluidos tests que contrastan resultados de búsqueda. Debe conservarse como
  implementación alternativa y referencia, sin incorporarla a cada decisión ni cambiar la
  estrategia por defecto.

Una dependencia puede formar parte del motor de manera **directa o a través de otra
herramienta**. La composición debe permitir entender de qué se beneficia Viktor, pero no
obliga a convertir cada detalle interno en un campo público de `Engine` ni a crear una tabla
de transposiciones con una vida distinta de la actual. El objetivo es que las relaciones sean
visibles sin concentrar toda la complejidad en una sola clase.

## Principios de diseño

- Los conceptos técnicos que son propios del problema —como minimax, alpha-beta pruning,
  quiescence o las tablas de transposiciones— conservan nombres técnicos reconocibles. La
  metáfora del pensamiento ayuda a organizar el flujo, no pretende disfrazar los algoritmos.
- Las herramientas deben tener responsabilidades comprensibles y participar en un proceso
  reconocible. Evitar tanto mezclar todo en una sola clase como crear capas y tipos que no
  aporten claridad.
- Los métodos principales deberían mostrar los pasos relevantes de la operación. Extraer
  detalles con nombres que expresen su intención solo cuando eso facilite seguir el código.
- Aclarar los contratos e invariantes importantes, especialmente la validez de una jugada, la
  propiedad del estado, la restauración de movimientos temporales y la separación entre la
  posición original y la copia de búsqueda.
- Mantener los nombres, comentarios y documentación coherentes con el código existente.
  Explicar lo que no resulte evidente; no añadir comentarios que repitan el código.
- Preservar el comportamiento durante el refactor. Una mejora de legibilidad no justifica
  alterar silenciosamente reglas, decisiones, mensajes o rendimiento. Los renombres acordados
  de las APIs de búsqueda son una excepción explícita a la compatibilidad de nombres.

## Seguridad del refactor: los tests son la fuente de verdad

Esta es una condición fundamental:

1. **Mantener los tests actuales como fuente de verdad.** Se permite ajustar sus referencias
   y nombres cuando una clase o interfaz se renombra. No se alteran sus escenarios, aserciones
   ni lógica para que pasen; si uno falla, investigaremos y corregiremos la regresión.
2. Ejecutar los tests existentes antes de empezar y registrar el resultado como línea base.
   Si hay fallos previos, identificarlos explícitamente antes de continuar.
3. Añadir tests nuevos donde falte cobertura para proteger el
   comportamiento antes de refactorizar. Los tests actuales son indispensables, pero no
   garantizan por sí solos que todos los casos estén cubiertos.
4. Trabajar en cambios pequeños y ejecutar los tests pertinentes después de cada etapa; volver
   a ejecutar la suite completa en los puntos de integración y antes de dar el trabajo por
   terminado.
5. Salvo los renombres expresamente acordados, mantener los contratos que ejercitan los tests.
   Cualquier cambio intencional de comportamiento necesita una decisión separada.

### Comportamientos que debemos proteger

- La jugada elegida, puntuación, profundidad, nodos y trazas en posiciones y condiciones
  deterministas, incluidas las alternativas y sus cotas cuando correspondan.
- El tablero original, sus derechos, contadores e historial después de pensar, también ante
  cancelaciones, límites de tiempo y errores; la restauración correcta de movimientos
  temporales y especiales.
- Mate, ahogado, posiciones muertas reconocidas, repetición y las reglas de 50 y 75 jugadas,
  incluida la prioridad del mate y la reclamación con jugada anunciada.
- La última iteración completada y el comportamiento cuando no se completa ninguna; la
  notificación de iteraciones, la cancelación y la parada de UCI.
- Los contratos de consola y UCI: salidas, diagnósticos y registros, así como sus diferencias
  deliberadas al tratar tablas y tiempos de espera.

En particular, UCI no dispone de un comando estándar para reclamar tablas: actualmente informa
de la situación y devuelve una jugada legal cuando la hay, dejando el resultado a la interfaz
gráfica. Si se detiene antes de completar una iteración, también usa una jugada legal alternativa
cuando está disponible. En consola, un tiempo agotado antes de la primera iteración se comunica
y termina la partida. Estas diferencias no deben desaparecer por unificar el modelo.

### Rendimiento y comparación

Antes de cambiar la búsqueda, registrar resultados y mediciones representativas, y compararlos
después en las mismas condiciones. Usar posiciones iniciales, tácticas y finales, con las
opciones existentes de quiescence y tabla de transposiciones cuando sean relevantes.

La búsqueda limitada por tiempo depende de la máquina y del momento de interrupción: comparar
decisiones y conteos exactos principalmente con profundidad fija y condiciones deterministas,
y evaluar por separado el comportamiento con tiempo y cancelación. Las mediciones de tiempo y
nodos por segundo no son umbrales de test universales; investigar degradaciones relevantes
antes de aceptarlas.

El proyecto ya dispone de `com.desierto.benchmark.SearchBenchmark`; su uso y sus parámetros
están documentados en `TODO.md`.

## Plan de trabajo

1. **Preparar una rama nueva para el refactor.** Seguir la convención
   `type/version/short-description` documentada en `README.md`. No empezar los cambios de
   código en la rama actual.
2. **Establecer la línea base.** Ejecutar la suite existente, identificar sus contratos y
   registrar resultados de búsqueda y rendimiento representativos. Conservar los tests actuales,
   salvo los ajustes de nombres acordados.
3. **Caracterizar los huecos.** Añadir pruebas independientes para los comportamientos
   importantes que aún no estén suficientemente protegidos, especialmente los límites entre
   motor, tablero, consola y UCI.
4. **Definir el flujo y sus contratos.** Revisar `SearchAlgorithm`, `SearchResult`,
   `IterativeDeepeningSearch`, `SearchPosition` y sus consumidores. Diseñar la fachada `Engine`
   y su `think()`, la composición de sus herramientas y la comunicación de resultados,
   conservando el comportamiento y documentando los renombres de API acordados.
5. **Hacer visible el proceso de pensamiento.** Reorganizar gradualmente la búsqueda para que
   sus pasos y herramientas sean fáciles de localizar, sin modificar los algoritmos ni el
   orden de sus operaciones por el mero hecho de mejorar la lectura.
6. **Aclarar los límites del estado y de las interfaces.** Mejorar la legibilidad de la
   aplicación y restauración de movimientos y de la interpretación de decisiones en consola
   y UCI, respetando sus responsabilidades y comportamientos propios.
7. **Verificar e integrar.** Ejecutar la suite completa, comparar las observaciones y
   mediciones con la línea base, revisar que los tests originales solo cambien por renombres y actualizar
   la documentación directamente afectada.

Las etapas pueden dividirse en cambios más pequeños. Si una extracción o abstracción dificulta
seguir el comportamiento, no se hará solo por encajar en el esquema.

## Primer avance

Se ha introducido `Engine.think()`, que utiliza `IterativeDeepeningSearch` y `AlphaBetaSearch`.
Consola y UCI lo utilizan; la consola sigue aplicando las jugadas y UCI sigue comunicándolas.
Las clases e interfaz de búsqueda anteriores se han renombrado, y los tests existentes solo
se han ajustado para reflejar esos nombres. Se han añadido pruebas independientes.

En alpha-beta se han separado la exploración de la raíz, las comprobaciones de tablas
automáticas, la evaluación de hojas y la búsqueda que maximiza o minimiza la puntuación.
Las pruebas nuevas fijan además variantes principales, alternativas y cotas, incluidas
posiciones con transposiciones.

Estos avances no concluyen la mejora de legibilidad interna del tablero y de la búsqueda.
Queda por revisar esas áreas gradualmente, con las mismas garantías, antes de considerar
terminado el plan.

## Decisiones aún abiertas

- Los parámetros y contratos concretos de `Engine.think()`, incluida la comunicación de
  iteraciones, la cancelación y los límites.
- Si `SearchResult` necesita una representación complementaria para expresar alguna decisión,
  manteniendo la compatibilidad existente.
- La composición concreta de las herramientas y los límites entre dependencias directas e
  indirectas, preservando la estrategia y el ciclo de vida actuales.
- Qué abstracciones del estado del tablero, como los derechos de enroque, simplifican realmente
  el código sin comprometer contratos o rendimiento.

Estas decisiones se tomarán al revisar el código y la cobertura, no se dan por resueltas en
este documento.

## Criterios para no perder el norte

El refactor habrá cumplido su objetivo cuando se pueda seguir con claridad cómo Viktor piensa
y comunica una decisión, las interfaces conserven sus responsabilidades, los tests originales
conserven sus escenarios y aserciones y sigan pasando, y las comprobaciones adicionales no muestren cambios de
comportamiento ni degradaciones de rendimiento sin explicar.
