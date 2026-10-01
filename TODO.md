# TODO - Motor de ajedrez

Documento de trabajo para iterar el proyecto desde el estado actual hasta un motor funcional.

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

- [ ] Implementar evaluación material básica.
- [ ] Implementar minimax.
- [ ] Añadir alpha-beta pruning.
- [ ] Añadir ordenación de movimientos.
- [ ] Añadir iterative deepening.
- [ ] Añadir quiescence search.

## 7. Rendimiento

- [ ] Medir tiempos de generación y búsqueda.
- [ ] Optimizar estructuras de datos si es necesario.
- [ ] Añadir transposition table con hashing Zobrist.

## 8. Interfaz y uso

- [ ] Crear entrada por consola.
- [ ] Mostrar tablero en texto.
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

## Orden recomendado

1. Base del dominio
2. Reglas básicas
3. Estado de partida
4. Legalidad de jugadas
5. Aplicación y deshacer
6. Tests
7. Motor de búsqueda
8. Rendimiento
9. Interfaz y uso
