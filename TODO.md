# TODO - Motor de ajedrez

Hoja de ruta del proyecto. La base de reglas está completada y el trabajo actual se centra en
convertirla en un motor de búsqueda.

## 1. Base del dominio

- [x] Definir un `Board` que represente el estado completo del tablero.
- [x] Rehacer `Move` para incluir origen, destino, pieza, captura, promoción, enroque y en passant.
- [x] Convertir `Cell` en un valor inmutable con `equals`, `hashCode` y validación de coordenadas.
- [x] Completar `Piece` con color y posición.
- [x] Mantener `Move` como objeto de valor y delegar la decisión de tipos de jugada a `MoveGenerator`.

## 2. Reglas básicas

- [x] Implementar movimiento legal de peones.
- [x] Implementar movimiento legal de caballos.
- [x] Implementar movimiento legal de alfiles, torres y dama.
- [x] Implementar movimiento legal del rey.
- [x] Implementar capturas.
- [x] Implementar enroque.
- [x] Implementar en passant.
- [x] Implementar promoción de peón.
- [x] Hacer que `MoveGenerator` construya los tipos especiales de movimiento.

## 3. Estado de partida

- [x] Inicializar la posición estándar de ajedrez.
- [x] Guardar turno actual.
- [x] Guardar derechos de enroque.
- [x] Guardar posibilidad de en passant.
- [x] Guardar contador de medio-movimientos y número de jugada si hace falta.

## 4. Legalidad de jugadas

- [x] Generar movimientos pseudo-legales.
- [x] Filtrar movimientos que dejen al rey en jaque.
- [x] Detectar jaque.
- [x] Detectar jaque mate.
- [x] Detectar ahogado.
- [x] Detectar posiciones muertas por material en los casos reconocidos: reyes solos, un alfil
  o caballo contra un rey, o solo alfiles en casillas del mismo color.
- [ ] Ampliar la detección de posiciones muertas más allá de esos casos de material.
- [x] Implementar la reclamación de tablas por 50 jugadas y las tablas automáticas por 75 jugadas,
  incluida la prioridad del mate y la reclamación con una jugada anunciada.
- [x] Detectar la triple repetición y terminar la partida automáticamente por decisión de diseño.
- [ ] Si se adoptan las reglas oficiales de repetición, permitir reclamar por triple repetición
  e implementar las tablas automáticas por quíntuple repetición.

## 5. Aplicación y deshacer

- [x] Implementar `makeMove`.
- [x] Implementar `undoMove`.
- [x] Asegurar que una jugada aplicada y deshecha restaura exactamente el estado anterior.

## 6. Motor de búsqueda

### Fundamentos

- [x] Implementar evaluación material básica en centipeones.
- [x] Evaluar mate y ahogado desde la perspectiva indicada.
- [x] Definir la API de búsqueda y su resultado: mejor jugada, puntuación, profundidad y nodos.

### Búsqueda y calidad de jugada

- [x] Implementar minimax a profundidad fija.
- [x] Puntuar la distancia al mate para preferir mates rápidos y retrasar derrotas inevitables.
- [x] Añadir alpha-beta pruning.
- [x] Añadir ordenación de movimientos: promociones, capturas, jaques y mejor jugada anterior.
  `FixedDepthAlphaBetaEngine` acepta opcionalmente la clave de una jugada anterior en la raíz;
  el llamador puede proporcionarla a partir de una búsqueda previa.
- [x] Añadir iterative deepening para profundizar por etapas y respetar un límite de tiempo.
  `IterativeDeepeningEngine.findBestMove(board, limits, timeLimit)` devuelve la última iteración
  completada, reutilizando su mejor jugada en la siguiente. Si no se completa ninguna, lanza
  `SearchTimeoutException`. Los nodos incluyen el trabajo de la iteración interrumpida.
  El límite se comprueba entre operaciones de búsqueda, no interrumpe una operación en curso.
- [x] Añadir quiescence search en las hojas para resolver capturas, promociones y jaques pendientes.
  Se exploran capturas y promociones; si el rey está en jaque, se exploran todas las respuestas
  legales. La extensión se limita a ocho medias jugadas y entonces se evalúa incluso en jaque.
  Está activada por defecto en alpha-beta y búsqueda iterativa; puede desactivarse con el
  constructor que recibe `false`.

## 7. Rendimiento (etapa actual completada)

- [x] Medir tiempos de generación y búsqueda, profundidad alcanzada y nodos por segundo.
  `com.desierto.benchmark.SearchBenchmark` mide las posiciones inicial, táctica y final, con y
  sin quiescence ni tabla de transposiciones, en todas las combinaciones, a profundidad fija
  y con límite de tiempo. Ejecutar:
  `mvn compile` y luego `java -cp target\classes com.desierto.benchmark.SearchBenchmark`.
  Opcionalmente acepta cinco argumentos: calentamientos, muestras, profundidad fija,
  profundidad máxima iterativa y límite en milisegundos (por defecto `2 5 2 8 250`).
  Se muestran cada muestra y las medianas de tiempo y nodos por segundo, además de la versión
  de Java y datos del equipo. Las muestras que agotan el tiempo antes de completar una etapa
  se señalan como `timeout`; los nodos de una búsqueda iterativa incluyen la etapa interrumpida.
  Las mediciones dependen de la máquina y no son umbrales de test.

- [x] Añadir transposition table con hashing Zobrist para reutilizar posiciones repetidas.
  La tabla tiene 65536 entradas y pertenece a una búsqueda, incluidas sus iteraciones. El hash
  cubre piezas, color, posición, indicador de movimiento, turno, enroque, casilla en passant y
  contador de medio-movimientos y una huella del historial de repetición.
  Solo se reutilizan puntuaciones a la misma profundidad restante y distancia desde la raíz;
  las cotas alpha-beta se aplican según su tipo. Quiescence no se almacena en la tabla.
  Se activa por defecto y puede desactivarse con el segundo parámetro booleano de los
  constructores de `FixedDepthAlphaBetaEngine` e `IterativeDeepeningEngine`.
  La huella incluye el recuento de las posiciones relevantes desde el último movimiento de
  peón o captura, por lo que las transposiciones con contextos diferentes no comparten
  puntuaciones.

- [x] Evitar listas intermedias al obtener las piezas de un color.

### Mejoras de exploración realizadas

- [x] Sustituir las copias completas de `Board` por `make/unmake` interno con un `UndoState`.
  La búsqueda trabaja sobre una copia aislada de la posición inicial y revierte cada jugada
  incluso si se interrumpe por una excepción o por el límite de tiempo.
- [x] Reutilizar `make/unmake` al filtrar movimientos que dejan al rey en jaque.
  `Board` es mutable y no es seguro para acceso concurrente; también debe sincronizarse durante
  la generación de jugadas y la comprobación temporal de jaques.

### Posibles mejoras futuras (no bloqueantes)

- Optimizar estructuras de datos si se necesita más rendimiento y las mediciones identifican
  un cuello de botella.
- Evaluar una representación de tablero más compacta si se necesita más rendimiento y las
  mediciones justifican el cambio.

## 8. Interfaz y uso

- [x] Crear entrada por consola para jugar contra Viktor.
- [x] Mostrar tablero en texto.
- [x] Leer movimientos por coordenadas, incluidas las promociones. El uso se explica en `README.md`.
- [x] Aplicar en consola un límite de tiempo por jugada configurable mediante búsqueda iterativa.
- [x] Añadir integración UCI básica para GUIs, con posiciones FEN, historial de jugadas,
  búsqueda interrumpible, profundidad y control de tiempo.
- [ ] Ampliar UCI con opciones adicionales si se necesitan: ponderación, `searchmoves` y
  límites de nodos, entre otras.

## 9. Tests

- [x] Test de posición inicial.
- [x] Test de movimientos de cada pieza.
- [x] Test de capturas.
- [x] Test de enroque.
- [x] Test de en passant.
- [x] Test de promoción.
- [x] Test de jaque, mate y ahogado.
- [x] Test de `makeMove` / `undoMove`.
- [x] Test de turno, legalidad y protección frente a metadatos de movimiento incorrectos.
- [x] Test de evaluación material, mate y ahogado.
- [x] Test de selección táctica del buscador: ganancia de material, defensa y mate.
- [x] Test de rendimiento y número de nodos al añadir optimizaciones.

## Orden de trabajo actual

1. Definir la API de búsqueda y su resultado.
2. Implementar minimax a profundidad fija.
3. Añadir puntuación por distancia al mate y pruebas tácticas.
4. Integrar alpha-beta pruning.
5. Mejorar la ordenación de movimientos.
6. Incorporar iterative deepening y límite de tiempo.
7. Añadir quiescence search.
8. Medir rendimiento.
9. Añadir hashing Zobrist y tabla de transposiciones.
10. Optimizar a partir de las mediciones (etapa actual completada; mejoras posteriores opcionales).
11. Crear la interfaz de consola e integración UCI básica (completadas).
