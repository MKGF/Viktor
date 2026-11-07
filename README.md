# Viktor

Viktor is a chess engine written in Java. It includes a console interface for playing against
the engine.

## Branch names

New branches on GitHub must use `type/version/short-description`, for example
`feat/1.0/add-castling` or `fix/1.0/search-timeout`. Use a change type such as `feat`,
`fix` or `chore`, the target version, and a short description. The existing default branch
is exempt. GitHub enforces the three-part structure when a branch is created, but does not
validate the meaning of each part.

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

The executable is `target\ViktorUCI-1.1.1\ViktorUCI-1.1.1.exe` for version 1.1.1. The
script takes the version from `pom.xml`, so different versions can be kept side by side.
Keep the entire versioned directory together: it includes the Java runtime needed to launch
the engine. The script does not overwrite an existing directory for the same version; move
or remove it before rebuilding. The engine also reports its version in its UCI name and
console greeting. Maven embeds the version during resource filtering. When running directly
from an IDE that does not filter resources, use the project root as the working directory so
the engine can read the version from `pom.xml`. The console mode described above remains
available separately.

For example, [Lucas Chess](https://lucaschess.pythonanywhere.com/downloads) supports external
UCI engines:

1. Install or run Lucas Chess for Windows.
2. Open **Engines → External engines → New** and select the versioned `ViktorUCI-1.1.1.exe`.
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

## Position evaluation

Viktor evaluates material and piece-square placement for both colors. It also favors developing
knights and bishops and uses the king's position and nearby friendly pawns as a simple safety
heuristic. These opening and middlegame terms gradually give way to a centralized king in the
endgame as non-pawn material disappears. This is a heuristic, not a complete assessment of pawn
structure, mobility or king attacks. Checkmate and recognized draws still take precedence over
the positional score.

## Search diagnostics

Each UCI engine process also creates its own readable UTF-8 `.log` file, independent of the
GUI's protocol log. On Windows the files are in `%USERPROFILE%\Viktor\logs`; on other systems
they are in `Viktor/logs` under Java's `user.home`. Open that folder with:

```powershell
explorer "$env:USERPROFILE\Viktor\logs"
```

Each launch uses a unique filename and may contain multiple games, separated by `new_game`
events when the GUI sends `ucinewgame`. Timestamped blocks include received positions and
boards, search requests, completed iterations, decisions and projected positions. The file is
flushed after each block and is retained until manually deleted. It contains the position
commands and move histories supplied by the GUI; avoid sharing it if those are private. The
GUI controls the actual game result, so the log does not infer a result when the process ends.
If the file cannot be created or written, Viktor reports a warning on standard error and as a
UCI `info string`, then continues without the file. Console mode retains its existing output
and does not create this UCI log.

After each search, the console prints a structured decision, for example:

```text
decision:
  source=search
  selected_move=b1c3
  score=cp:44
  score_perspective=white
  pv=b1c3 b8c6
  alternatives:
    - move=b1c3 score=cp:44 bound=exact
    - move=g1f3 score=cp:44 bound=upper
  side=white fullmove=1 halfmove=0 in_check=false legal_moves=20 requested_depth=2 time_limit_ms=1000 elapsed_ms=20 depth=2 nodes=100
```

The score and alternatives use the searching side's perspective; `cp:100` is one pawn in
that side's favor and `mate:N` reports mate in moves. Up to three root moves are listed by
their reported scores, including the selected move. `bound=upper` means pruning established
only an upper bound: the true score may be lower, so these moves are not necessarily the
best remaining alternatives. The principal variation (`pv`) contains moves found by the
search and can be shorter than the completed depth when the search reaches a terminal
position, a draw claim, or a cached transposition. It does not include quiescence moves.

The console also logs a `position` block initially and after each move. It shows the static
evaluation, material and positional terms from White's perspective, regardless of whose turn
it is. Checkmate and automatic draws override the combined evaluation, so in those cases
the heuristic terms need not add up to it. Static evaluation is distinct from the search
score and is not a probability.

The UCI engine writes the same structured blocks to standard error and emits each line as
an `info string` on standard output; whether the GUI displays or retains them depends on the
GUI. It logs each received position and, after a search move, the projected position if that
move is played. Standard UCI `info` lines report the score and principal variation for each
completed depth; `bestmove` remains the final move response.

`source=search` means the move came from a completed search; `draw_claim` and `automatic_draw`
indicate a draw decision for which the GUI must adjudicate. `terminal` means no legal move is
available. If no iteration completes, `source=no_completed_iteration` reports unavailable
score, depth and nodes. In UCI, `selected_move=none` distinguishes these cases from `uci_move`,
the legal fallback sent to the GUI when one exists. Depth counts halfmoves, quiescence can look
further, and nodes can include work from an interrupted iteration. UCI elapsed time includes
any wait for `stop` during infinite analysis.

To keep an existing Windows build while generating an updated executable in a separate folder:

```powershell
.\build-uci.ps1 -Destination target\uci-diagnostics
```

In that case select `target\uci-diagnostics\ViktorUCI-1.1.1\ViktorUCI-1.1.1.exe` in the GUI
and keep its entire versioned folder together.
