# Viktor: detailed guide

Viktor is a chess engine written in Java. It includes a console interface for playing against
the engine.

## How Viktor makes a decision

`com.desierto.search.Engine` thinks about a board with `think()` and returns a `SearchResult`.
It uses `IterativeDeepeningSearch` and `AlphaBetaSearch`, with move generation, evaluation,
move ordering, quiescence and transposition caching as appropriate. Thinking does not play
the decision on the caller's board. The result can instead contain a draw claim or indicate
that there is no move; time limits and cancellation retain their documented behavior.

The console interprets the decision and applies a move to its board. UCI communicates a
response to the GUI, which controls the game. `MinimaxSearch` is an alternative used by
tests, not by the production search path.

An external Polyglot opening book, enabled by default, can select a legal move before searching. A book
decision has `SearchResult.fromBook() == true`; its score, depth and nodes are zero placeholders,
not a search evaluation. When no book move is available, Viktor searches normally.

Local Syzygy tables, enabled by default, can select a move before the book or search. A tablebase decision
has a `tablebaseOutcome()` with WDL and DTZ; its score, depth and nodes are placeholders rather
than a search evaluation.

The search classes and interface were renamed: callers of `FixedDepthAlphaBetaEngine`,
`FixedDepthMinimaxEngine`, `IterativeDeepeningEngine` and `SearchEngine` must use
`AlphaBetaSearch`, `MinimaxSearch`, `IterativeDeepeningSearch` and `SearchAlgorithm`,
respectively. The former names are no longer available.

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

Place the external resources in the project root before starting Viktor:

```text
Viktor\
  books\openings.bin
  tablebases\KPvK.rtbw
  tablebases\KPvK.rtbz
  ... (the remaining three- and four-piece Syzygy files)
```

They are not included in Git or downloaded by the build. See the sections below for their
sources and requirements. With JDK 21 or later and Maven installed, build and start the console
from the project root on Windows:

```powershell
mvn compile dependency:copy-dependencies "-DincludeScope=runtime"
java -cp "target\classes;target\dependency\*" com.desierto.Viktor
```

Both resources are loaded automatically from those paths; no extra command-line options are
needed. Missing or invalid resources produce warnings and Viktor continues with normal search.
When running Java directly, the paths are relative to the **working directory**, not the class
files; run from the project root. You can also run the `main` method of `com.desierto.Viktor`
from IntelliJ IDEA with the project root as its working directory. To change the options, set
the program arguments in the run configuration.

By default, you play White and Viktor searches to a maximum depth of 3, with a limit of
10 seconds per move. To play Black or change the depth and time limit:

```powershell
java -cp "target\classes;target\dependency\*" com.desierto.Viktor --black --depth 15 --time 5
```

`--black`, `--depth N` and `--time SECONDS` are independent. Depth and time must be positive
whole numbers. The time limit may be at most 9223372036 seconds. Viktor searches progressively
deeper positions and plays the best move from the last completed depth when the time expires;
it may finish earlier if it reaches the maximum depth. The actual depth is displayed with its
move. Timed console searches also use the conservative managed-time policy described below.
The deadline is checked between search operations, so it is not a strict real-time limit.
If the first depth cannot be completed in time, Viktor reports the timeout and ends the game;
restart with a higher time limit. Use `--help` to see the startup options.

To select another external, sorted Polyglot `.bin` opening book in the console:

```powershell
java -cp "target\classes;target\dependency\*" com.desierto.Viktor --black --book "C:\chess\book.bin"
```

Use `--no-book` to disable the book or `--no-tablebases` to disable the tablebases. `--book PATH`
and `--tablebases PATH` override the default paths; when contradictory options for the same
resource are supplied, the last one takes precedence.

The book is not distributed with Viktor. Moves are chosen randomly in proportion
to their positive Polyglot weights, after checking legality. Viktor skips the book when a draw
claim might be available or the position is an automatic draw. The book is validated on load;
an inaccessible or invalid file produces a warning and normal search remains available.

For local use, `books\openings.bin` can be obtained by downloading `gm2001.bin` from the
[Donna opening books collection](https://github.com/michaeldv/donna_opening_books) and renaming it.
The collection describes it as Oliver Deville's book of grandmaster games from 2001–2013. This file is ignored
by Git and is not distributed with Viktor; check its terms before redistributing it. Placed at
`books\openings.bin` under the project root, it is used automatically by the console.

## Local endgame tables

Viktor can consult locally installed Syzygy WDL (`.rtbw`) and DTZ (`.rtbz`) files for
three- and four-piece endings. The directory must contain the complete set of both types
for those endings: 70 files, approximately 4.35 MB. They can be obtained from the
[Lichess tablebase archive](https://tablebase.lichess.ovh/tables/standard/), in the
`3-4-5-wdl` and `3-4-5-dtz` directories. Select only names with at most four pieces
(count the piece letters on both sides of `v`); the archive provides file sizes in
`bytes.tsv` and checksums in `sha256`. Verify downloaded files before use. The data is
external, is not distributed with Viktor, and `tablebases\` is ignored by Git.

To select a different local set from the project root:

```powershell
java -cp "target\classes;target\dependency\*" com.desierto.Viktor --tablebases "C:\chess\tablebases"
```

The default directory is `tablebases\` under the working directory in the console. Missing or
unreadable files, invalid headers, or probe failures are reported as warnings and
Viktor searches normally instead. Piazzolla maps table files for the lifetime of the probe;
on Windows, restart the engine before replacing files that remain in use.

For correctness with Viktor's repetition and draw-claim rules, the tablebase is currently
used only when the halfmove clock is zero, no reversible position history is present, and
there are no castling rights or en-passant target. Terminal and recognized dead positions are
handled by the existing rules. Other positions use normal search, even if a table covers them.
UCI FEN positions do not contain earlier game history. The `wdl` and `dtz` diagnostic values
are from the side to move; DTZ is a distance to a zeroing move, in plies, and can be off by
one, not a mate distance or centipawn evaluation.

The ordinary test suite does not require table files. To include integration tests against
an installed set, run `mvn "-Dviktor.syzygy.testPath=tablebases" test`.

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
build a self-contained executable from the project root using PowerShell, Maven and a JDK 21 or later
that includes `javac`, `jar` and `jpackage`:

```powershell
.\build-uci.ps1
```

The script copies `books\openings.bin` and the available three- and four-piece Syzygy files from
the project root into the new application directory. It warns if the book is missing or the
tablebase set is incomplete; the resulting engine still starts and falls back to search.
It does not download the resources. Copying them into an application directory does **not**
grant redistribution rights: check their terms before sharing that directory.

The executable is `target\ViktorUCI-1.3.0\ViktorUCI-1.3.0.exe` for version 1.3.0. The
script takes the version from `pom.xml`, so different versions can be kept side by side.
Keep the entire versioned directory together: it includes the Java runtime needed to launch
the engine. The script does not overwrite an existing directory for the same version; move
or remove it before rebuilding. The engine also reports its version in its UCI name and
console greeting. Maven embeds the version during resource filtering. When running directly
from an IDE that does not filter resources, use the project root as the working directory so
the engine can read the version from `pom.xml`. The console mode described above remains
available separately.

The packaged engine looks next to the executable, regardless of the GUI's working directory:

```text
ViktorUCI-1.3.0\
  ViktorUCI-1.3.0.exe
  books\openings.bin
  tablebases\KPvK.rtbw
  tablebases\KPvK.rtbz
  ... (the remaining three- and four-piece Syzygy files)
```

Keep the application and its `books` and `tablebases` directories together when moving it.
If the files were unavailable during the build, place them in those locations later and restart
the engine. Both resources are enabled by default; the UCI handshake reports `OwnBook=true` and
the default absolute `BookFile` and `SyzygyPath`. A GUI may override those settings.

In a UCI GUI, use `OwnBook=false` to disable the book, or set `SyzygyPath` to `<empty>` to
disable tablebases. `BookFile` and `SyzygyPath` can also be set to different paths, preferably
absolute paths. For example:

```text
setoption name OwnBook value false
setoption name SyzygyPath value <empty>
```

Changing either book option reloads the enabled book; changing `SyzygyPath` reloads the tables.
Viktor reports loading or reading failures
through `info string` and its log, then uses normal search. A book move is reported as
`source=book` with no score; `go infinite` still waits for `stop`.

For an illustrated example, see the [Lucas Chess setup guide](lucas-chess.md).

UCI sends Viktor the current position and requests a search; Viktor returns its selected move.
The GUI controls the board, clock and game result. Viktor supports the UCI handshake, readiness,
new games, `position startpos` and six-field `position fen` with subsequent moves, `go depth`,
`go movetime`, clock and increment limits, `go infinite`, `stop` and `quit`. Depth is limited to
64. Search information includes the completed depth, score, nodes and root move. Pondering,
`searchmoves`, node limits and other UCI options are not implemented; do not select those
settings in the GUI.

For UCI clock-based searches and timed console searches, Viktor can stop after a completed
iteration when there is only one legal move. With at most 12 legal moves, no check, captures or
promotions, it uses a soft deadline of 75% of the allotted time after at least two completed
depths, returning the last completed depth if it interrupts the next iteration. In those quiet
positions it may also stop after at least half the time and depth four
if the same move has remained best for three consecutive iterations, adjacent scores differ by
at most 25 centipawns, and it leads the next alternative by at least 100 centipawns. These are
heuristics and can reduce playing strength. They are disabled when a fifty-move draw claim is
available. The original time limit remains the hard search deadline; a running search operation
can overrun it. UCI `movetime`, depth-only and infinite searches do not use these heuristics.

UCI has no standard command for an engine to claim a draw. If Viktor detects an automatic
draw or would claim the fifty-move rule while legal moves remain, it reports the situation in
`info string` and returns a legal move; the GUI must decide the result. Viktor's search still
treats threefold repetition as automatic, which differs from official claim rules. Repetition
history starts at the supplied position: a standalone FEN cannot describe moves played before it,
so those earlier occurrences cannot be detected unless the GUI supplies them as moves from an
earlier position.

## Position evaluation

Viktor evaluates material and piece-square placement for both colors. Each piece type has its
own positional evaluator. Rooks gain a bonus for open or semi-open files, bishops consider
opposing pawns on their square color and friendly pawns blocking their diagonals, and knights
favor advanced outposts that opposing pawns cannot challenge, especially when supported by a
friendly pawn and near opposing pawns. A small general pressure penalty distinguishes attacked
pieces from defended ones. These are geometric attacks, not a guarantee of a legal or profitable
capture.

The influence of the non-king piece-square tables is estimated from the number of knights
and bishops still on their starting squares: it ranges from full weight with eight undeveloped
minors to one quarter with none. This is a position-based approximation, not a move counter;
it can increase again if pieces return home. The contextual bonuses do not fade with that
weight. King safety and the development penalty still fade with non-pawn material, and the
king uses a separate endgame table. These heuristics do not fully assess pawn structure,
mobility, threats or king attacks. Checkmate and recognized draws take precedence.

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
  reason=best_searched_score
  static_perspective=white
  static_material_delta=cp:0
  static_positional_delta=cp:20
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

`reason=opening_book` identifies a book move, without attributing it to a search or a score.
`reason=best_searched_score` means the move was selected by search. For such moves,
`static_material_delta` and `static_positional_delta` show the change immediately after the
move, in centipawns from the moving side's perspective; a positive value favors that side.
These are static measurements, not proof that material or position caused the selection.
The search also considers later replies, tactics, and terminal results, and its score can differ
from these immediate changes. Draw and fallback decisions instead report their existing source
as the reason.

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

In that case select `target\uci-diagnostics\ViktorUCI-1.3.0\ViktorUCI-1.3.0.exe` in the GUI
and keep its entire versioned folder together.
