# Viktor

Viktor es un motor de ajedrez en Java. Incluye una interfaz de consola para jugar una partida
contra el motor.

## Requisitos

- JDK 21 o superior.
- Maven para compilar el proyecto.

## Iniciar una partida

Desde la raíz del proyecto, en Windows:

```powershell
mvn compile
java -cp target\classes com.desierto.Viktor
```

También puedes ejecutar `main` de `com.desierto.Viktor` desde IntelliJ IDEA; configura los
argumentos de programa en la configuración de ejecución si quieres cambiar las opciones.

Por defecto juegas con blancas y Viktor busca a profundidad 3. Para jugar con negras o cambiar
la profundidad:

```powershell
java -cp target\classes com.desierto.Viktor --negras --profundidad 4
```

`--negras` y `--profundidad N` son independientes. La profundidad debe ser un entero positivo:
una profundidad mayor puede tardar mucho más, ya que esta interfaz no establece un límite de
tiempo por jugada. Usa `--ayuda` para consultar las opciones de inicio.

## Durante la partida

Escribe la casilla de origen y la de destino, sin espacios; se admiten mayúsculas y minúsculas.

- `e2e4`: mover de e2 a e4.
- `e1g1` o `e1c1`: enroque blanco corto o largo; para negras, `e8g8` o `e8c8`.
- `e7e8q`: promoción; añade obligatoriamente `q` (dama), `r` (torre), `b` (alfil) o `n`
  (caballo), también cuando la promoción incluye captura.
- `ayuda`: mostrar las instrucciones de entrada.
- `salir`: terminar la partida; el fin de entrada también la termina.

En el tablero, las piezas blancas aparecen en mayúsculas y las negras en minúsculas.
Las letras son `K` rey, `Q` dama, `R` torre, `B` alfil, `N` caballo y `P` peón; `.` indica
una casilla vacía. Viktor muestra su jugada con la misma notación.

La partida termina automáticamente por jaque mate o ahogado. Aún no se detectan otras tablas,
como las de repetición, material insuficiente o las basadas en contadores de jugadas. La interfaz
es interactiva y no implementa el protocolo UCI.
