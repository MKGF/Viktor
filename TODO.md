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
- [ ] Añadir ordenación de movimientos: promociones, capturas, jaques y mejor jugada anterior.
- [ ] Añadir iterative deepening para profundizar por etapas y respetar un límite de tiempo.
- [ ] Añadir quiescence search en las hojas para resolver capturas, promociones y jaques pendientes.

## 7. Rendimiento

- [ ] Medir tiempos de generación y búsqueda, profundidad alcanzada y nodos por segundo.
- [ ] Añadir transposition table con hashing Zobrist para reutilizar posiciones repetidas.
- [ ] Optimizar estructuras de datos solo cuando las mediciones identifiquen un cuello de botella.

### Mejoras futuras de exploración

- [ ] Sustituir las copias completas de `Board` por `make/unmake` interno con un `UndoState`.
- [ ] Reutilizar `make/unmake` al filtrar movimientos que dejan al rey en jaque.
- [ ] Evaluar una representación de tablero más compacta solo si las mediciones lo justifican.

## 8. Interfaz y uso

- [ ] Crear entrada por consola.
- [x] Mostrar tablero en texto.
- [ ] Leer movimientos en una notación definida.
- [ ] Definir formato UCI si se quiere integrar con GUIs externas.

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
- [ ] Test de rendimiento y número de nodos al añadir optimizaciones.

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
10. Optimizar solo a partir de las mediciones.
11. Crear la interfaz de consola y, si procede, UCI.
