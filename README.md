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
- `claim`: claim a draw after 50 moves by each player without a pawn move or capture.
  At 99 halfmoves, use `claim b1b2` to announce a legal move that would reach 100; the move
  is not played when the claim succeeds. Invalid claims are rejected; game clocks and arbiter
  penalties are not modelled.
- `help`: show the input instructions.
- `quit`: end the game; end of input also ends the game.

White pieces appear in uppercase and Black pieces in lowercase. The letters are `K` king,
`Q` queen, `R` rook, `B` bishop, `N` knight and `P` pawn; `.` denotes an empty square.
Viktor displays its moves in the same notation.

In the console, the game ends automatically on checkmate, stalemate or recognized dead positions: kings alone,
one bishop or knight against a lone king, or positions where the only other pieces are bishops
all on the same square color. These are not all possible dead positions. The game also ends
automatically after 75 moves by each player without a pawn move or capture; checkmate on the
last move takes precedence. Threefold repetition also ends the game automatically, without
a claim. This is a deliberate simplification of the official rules, under which threefold
repetition is claimable and fivefold repetition is automatic. Viktor can claim the 50-move
draw when it is its best option.

For compatibility, the previous Spanish options (`--negras`, `--profundidad`, `--ayuda`) and
commands (`ayuda`, `salir`) remain available.

## Connecting a graphical chess interface

Viktor also provides a separate UCI engine for graphical chess applications. On Windows,
build a self-contained executable from the project root using PowerShell and a JDK 21 or later
that includes `javac`, `jar` and `jpackage`:

```powershell
.\build-uci.ps1
```

The executable is `target\ViktorUCI\ViktorUCI.exe`. Keep the entire `ViktorUCI` directory
together: it includes the Java runtime needed to launch the engine. The script does not
overwrite an existing `target\ViktorUCI` directory; move or remove an old build before
rebuilding. The console mode described above remains available separately.

For example, [Lucas Chess](https://lucaschess.pythonanywhere.com/downloads) supports external
UCI engines:

1. Install or run Lucas Chess for Windows.
2. Open **Engines → External engines → New** and select `ViktorUCI.exe`.
3. Accept the engine details, then start a game against an engine and select Viktor from
   **External engines**. Choose your color and a fixed thinking time or depth.

UCI sends Viktor the current position and requests a search; Viktor returns its selected move.
The GUI controls the board, clock and game result. Viktor supports the UCI handshake, readiness,
new games, `position startpos` and six-field `position fen` with subsequent moves, `go depth`,
`go movetime`, clock and increment limits, `go infinite`, `stop` and `quit`. Depth is limited to
64. Search information includes the completed depth, score, nodes and root move. Pondering,
`searchmoves`, node limits and other UCI options are not implemented; do not select those
settings in the GUI.

UCI has no standard command for an engine to claim a draw. If Viktor detects an automatic
draw or would claim the fifty-move rule while legal moves remain, it reports the situation in
`info string` and returns a legal move; the GUI must decide the result. Viktor's search still
treats threefold repetition as automatic, which differs from official claim rules. Repetition
history starts at the supplied position: a standalone FEN cannot describe moves played before it,
so those earlier occurrences cannot be detected unless the GUI supplies them as moves from an
earlier position.
