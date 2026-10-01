# TODO - Motor de ajedrez

Documento de trabajo para iterar el proyecto desde el estado actual hasta un motor funcional.

## 1. Base del dominio

- [x] Definir un `Board` que represente el estado completo del tablero.
- [ ] Rehacer `Move` para incluir origen, destino, pieza, captura, promoción, enroque y en passant.
- [x] Convertir `Cell` en un valor inmutable con `equals`, `hashCode` y validación de coordenadas.
- [x] Completar `Piece` con color y posición.
- [ ] Añadir una abstracción común para piezas deslizantes y piezas saltadoras si aporta claridad.

## 2. Reglas básicas

- [ ] Implementar movimiento legal de peones.
- [ ] Implementar movimiento legal de caballos.
- [ ] Implementar movimiento legal de alfiles, torres y dama.
- [ ] Implementar movimiento legal del rey.
- [ ] Implementar capturas.
- [ ] Implementar enroque.
- [ ] Implementar en passant.
- [ ] Implementar promoción de peón.

## 3. Estado de partida

- [ ] Inicializar la posición estándar de ajedrez.
- [ ] Guardar turno actual.
- [ ] Guardar derechos de enroque.
- [ ] Guardar posibilidad de en passant.
- [ ] Guardar contador de medio-movimientos y número de jugada si hace falta.

## 4. Legalidad de jugadas

- [ ] Generar movimientos pseudo-legales.
- [ ] Filtrar movimientos que dejen al rey en jaque.
- [ ] Detectar jaque.
- [ ] Detectar jaque mate.
- [ ] Detectar ahogado.

## 5. Aplicación y deshacer

- [ ] Implementar `makeMove`.
- [ ] Implementar `undoMove`.
- [ ] Asegurar que una jugada aplicada y deshecha restaura exactamente el estado anterior.

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
- [ ] Test de movimientos de cada pieza.
- [x] Test de capturas.
- [ ] Test de enroque.
- [ ] Test de en passant.
- [ ] Test de promoción.
- [ ] Test de jaque, mate y ahogado.
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
