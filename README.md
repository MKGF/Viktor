# Viktor

Viktor is a chess engine written in Java. It includes a console interface for playing against
the engine.

## Requirements

- JDK 21 or later.
- Maven to build the project.

## Starting a game

From the project root on Windows:

```powershell
mvn compile
java -cp target\classes com.desierto.Viktor
```

You can also run the `main` method of `com.desierto.Viktor` from IntelliJ IDEA. To change the
options, set the program arguments in the run configuration.

By default, you play White and Viktor searches to a maximum depth of 3, with a limit of
10 seconds per move. To play Black or change the depth and time limit:

```powershell
java -cp target\classes com.desierto.Viktor --black --depth 15 --time 5
```

`--black`, `--depth N` and `--time SECONDS` are independent. Depth and time must be positive
whole numbers. The time limit may be at most 9223372036 seconds. Viktor searches progressively
deeper positions and plays the best move from the last completed depth when the time expires;
it may finish earlier if it reaches the maximum depth. The actual depth is displayed with its
move. The deadline is checked between search operations, so it is not a strict real-time limit.
If the first depth cannot be completed in time, Viktor reports the timeout and ends the game;
restart with a higher time limit. Use `--help` to see the startup options.

## Playing

Enter the starting and destination squares without spaces. Input is case-insensitive.

- `e2e4`: move from e2 to e4.
- `e1g1` or `e1c1`: castle kingside or queenside as White; use `e8g8` or `e8c8` as Black.
- `e7e8q`: promote a pawn. Append `q` (queen), `r` (rook), `b` (bishop) or `n` (knight),
  including when the promotion captures a piece.
- `help`: show the input instructions.
- `quit`: end the game; end of input also ends the game.

White pieces appear in uppercase and Black pieces in lowercase. The letters are `K` king,
`Q` queen, `R` rook, `B` bishop, `N` knight and `P` pawn; `.` denotes an empty square.
Viktor displays its moves in the same notation.

The game ends automatically on checkmate or stalemate. Other draws, such as repetition,
insufficient material or move-count draws, are not yet detected. The interface is interactive
and does not implement the UCI protocol.

For compatibility, the previous Spanish options (`--negras`, `--profundidad`, `--ayuda`) and
commands (`ayuda`, `salir`) remain available.
