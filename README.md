# Viktor

Viktor is a Java chess engine with a console interface and a UCI interface for
graphical chess applications. It uses iterative deepening and alpha-beta search,
with optional Polyglot opening books and Syzygy endgame tables.

## Requirements

- JDK 21 or later.
- Maven.
- Windows PowerShell and a JDK with `jpackage` to build the standalone UCI engine.

## Play in the console

From the project root on Windows:

```powershell
mvn compile dependency:copy-dependencies "-DincludeScope=runtime"
java -cp "target\classes;target\dependency\*" com.desierto.Viktor
```

By default, you play White and Viktor searches up to depth 3 or 10 seconds per
move. Use `--help` for all options; for example:

```powershell
java -cp "target\classes;target\dependency\*" com.desierto.Viktor --black --depth 15 --time 5
```

Enter moves as `e2e4`, promotions as `e7e8q`, and castling as `e1g1` or
`e1c1`. Use `help`, `claim` or `quit` for instructions, a 50-move draw claim
or to end the game. Threefold repetition ends the game automatically, a
deliberate simplification of the official rules.

## Use a graphical interface

Build the standalone Windows UCI engine:

```powershell
.\build-uci.ps1
```

Select `target\ViktorUCI-<version>\ViktorUCI-<version>.exe` in a UCI-compatible
GUI such as [Lucas Chess](https://lucaschess.pythonanywhere.com/downloads).
The version comes from `pom.xml`; keep the entire generated directory together.
The script does not overwrite an existing directory for the same version.

See the [Lucas Chess setup guide](docs/lucas-chess.md) for illustrated steps.

The GUI controls the board, clock and game result. Viktor supports depth,
movetime, clock and infinite searches, but not pondering, `searchmoves` or node
limits. A GUI must adjudicate draw claims; a standalone FEN does not include
earlier repetition history.

## Optional chess data

Opening books and endgame tables are enabled by default but are not included or
downloaded by the build. Missing or invalid data produces a warning and Viktor
continues with normal search.

- **Opening book:** place a sorted Polyglot book at `books\openings.bin`.
  The [Donna opening books collection](https://github.com/michaeldv/donna_opening_books)
  provides `gm2001.bin`, which can be renamed for local use.
- **Endgame tables:** place the complete set of 70 three- and four-piece Syzygy
  WDL and DTZ files in `tablebases\`. They are available from the
  [Lichess tablebase archive](https://tablebase.lichess.ovh/tables/standard/);
  verify the downloads before use.

These resources are external; check their terms before redistributing them.
In console mode, paths are relative to the working directory. Use `--book PATH`
or `--tablebases PATH` to override them, and `--no-book` or `--no-tablebases`
to disable them. The UCI build copies available files beside the executable;
the GUI can configure `OwnBook`, `BookFile` and `SyzygyPath`.

## More information

See the [detailed guide](docs/guide.md) for resource setup, console commands,
UCI options, draw rules, search behavior and diagnostics. UCI logs are written
to `%USERPROFILE%\Viktor\logs` on Windows and may contain game histories.

Run the tests with `mvn test`. To include Syzygy integration tests against a
local set, run `mvn "-Dviktor.syzygy.testPath=tablebases" test`.

New GitHub branches use `type/version/short-description`, for example
`feat/1.0/add-castling`; the default branch is exempt.
