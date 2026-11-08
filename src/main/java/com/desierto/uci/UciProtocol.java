package com.desierto.uci;

import com.desierto.EngineInfo;
import com.desierto.DefaultResources;
import com.desierto.book.OpeningBook;
import com.desierto.domain.Board;
import com.desierto.domain.LegalMove;
import com.desierto.domain.Move;
import com.desierto.domain.MoveGenerator;
import com.desierto.domain.MoveNotation;
import com.desierto.search.Engine;
import com.desierto.search.SearchLimits;
import com.desierto.search.SearchDiagnostics;
import com.desierto.search.SearchResult;
import com.desierto.search.SearchTimeoutException;
import com.desierto.tablebase.SyzygyTablebase;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

final class UciProtocol {
  private final PrintStream output;
  private final PrintStream errors;
  private UciSessionLog sessionLog;
  private Board board = Board.standard();
  private SearchTask active;
  private final String defaultBookFile;
  private final String defaultSyzygyPath;
  private boolean ownBook;
  private String bookFile;
  private String syzygyPath;
  private boolean bookInitialized;
  private boolean tablebaseInitialized;
  private OpeningBook openingBook;
  private SyzygyTablebase tablebase;

  UciProtocol(PrintStream output, PrintStream errors) {
    this(output, errors, null);
  }

  UciProtocol(PrintStream output, PrintStream errors, UciSessionLog sessionLog) {
    this(output, errors, sessionLog, DefaultResources.locate());
  }

  UciProtocol(PrintStream output, PrintStream errors, UciSessionLog sessionLog,
      DefaultResources defaults) {
    this.output = Objects.requireNonNull(output, "output must not be null");
    this.errors = Objects.requireNonNull(errors, "errors must not be null");
    this.sessionLog = sessionLog;
    Objects.requireNonNull(defaults, "defaults must not be null");
    defaultBookFile = defaults.book() == null ? "" : defaults.book().toString();
    defaultSyzygyPath = defaults.tablebases() == null ? "" : defaults.tablebases().toString();
    ownBook = !defaultBookFile.isEmpty();
    bookFile = defaultBookFile;
    syzygyPath = defaultSyzygyPath;
  }

  void run(BufferedReader input) throws IOException {
    Objects.requireNonNull(input, "input must not be null");
    try {
      String line;
      while ((line = input.readLine()) != null) {
        line = line.trim();
        if (line.isEmpty()) {
          continue;
        }
        String[] tokens = line.split("\\s+");
        try {
          switch (tokens[0]) {
            case "uci" -> {
              output.println("id name " + EngineInfo.name());
              output.println("id author MKGF");
              output.println("option name OwnBook type check default " + !defaultBookFile.isEmpty());
              output.println("option name BookFile type string default " + optionValue(defaultBookFile));
              output.println("option name SyzygyPath type string default " + optionValue(defaultSyzygyPath));
              output.println("uciok");
            }
            case "isready" -> {
              initializeResources();
              output.println("readyok");
            }
            case "setoption" -> {
              stop(false);
              setOption(line);
            }
            case "ucinewgame" -> {
              stop(false);
              board = Board.standard();
              recordLog("game:\n  event=new_game");
            }
            case "position" -> {
              stop(false);
              Board next = UciPosition.parse(tokens);
              String diagnostic = SearchDiagnostics.position(next, "received");
              board = next;
              logDiagnostic(diagnostic, "\n  input=" + line + "\n  board:\n"
                  + next.toString().lines().map(row -> "    " + row)
                      .collect(java.util.stream.Collectors.joining("\n")));
            }
            case "go" -> {
              stop(false);
              GoOptions options = GoOptions.parse(tokens, board.getSideToMove());
              initializeResources();
              recordLog("search:\n  input=" + line);
              active = new SearchTask(board.copy(), options, ownBook ? openingBook : null, tablebase);
              active.thread.start();
            }

            case "stop" -> stop(false);
            case "quit" -> {
              stop(true);
              return;
            }
            default -> output.println("info string Unsupported command: " + tokens[0]);
          }
        } catch (IllegalArgumentException ex) {
          output.println("info string Error: " + ex.getMessage());
          recordLog("error:\n  input=" + line + "\n  message=" + ex.getMessage());
          if (tokens[0].equals("go")) {
            output.println("bestmove 0000");
          }
        }
      }
    } finally {
      try {
        stop(true);
      } finally {
        recordLog("session:\n  event=end");
      }
    }
  }

  private void setOption(String line) {
    String prefix = "setoption name ";
    if (!line.startsWith(prefix)) throw new IllegalArgumentException("Expected setoption name");
    String rest = line.substring(prefix.length());
    int separator = rest.indexOf(" value ");
    if (separator < 0) throw new IllegalArgumentException("Expected setoption value");
    String name = rest.substring(0, separator).trim();
    String value = rest.substring(separator + " value ".length()).trim();
    if (name.equalsIgnoreCase("SyzygyPath")) {
      syzygyPath = value.equals("<empty>") ? "" : value;
      loadTablebase();
      return;
    } else if (name.equalsIgnoreCase("OwnBook")) {
      if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) {
        throw new IllegalArgumentException("OwnBook must be true or false");
      }
      ownBook = Boolean.parseBoolean(value);
    } else if (name.equalsIgnoreCase("BookFile")) {
      bookFile = value.equals("<empty>") ? "" : value;
    } else {
      throw new IllegalArgumentException("Unknown option: " + name);
    }
    loadBook();
  }

  private static String optionValue(String value) {
    return value.isEmpty() ? "<empty>" : value;
  }

  private void initializeResources() {
    if (!bookInitialized) loadBook();
    if (!tablebaseInitialized) loadTablebase();
  }

  private void loadTablebase() {
    tablebaseInitialized = true;
    tablebase = null;
    if (!syzygyPath.isEmpty()) {
      try {
        tablebase = new SyzygyTablebase(Path.of(syzygyPath), this::warning);
      } catch (IOException | SecurityException | InvalidPathException ex) {
        warning("Cannot load tablebases " + syzygyPath + ": " + ex.getMessage());
      }
    }
  }

  private void loadBook() {
    bookInitialized = true;
    openingBook = null;
    if (ownBook) {
      if (bookFile.isEmpty()) {
        warning("Opening book is enabled but BookFile is empty");
        return;
      }
      try {
        openingBook = new OpeningBook(Path.of(bookFile), this::warning);
      } catch (IOException | SecurityException | InvalidPathException ex) {
        warning("Cannot load opening book " + bookFile + ": " + ex.getMessage());
      }
    }
  }

  private synchronized void warning(String message) {
    String warning = "Warning: " + message;
    errors.println(warning);
    output.println("info string " + warning);
    recordLog("warning:\n  message=" + warning);
  }

  private synchronized void logDiagnostic(String diagnostic) {
    logDiagnostic(diagnostic, "");
  }

  private synchronized void logDiagnostic(String diagnostic, String logOnlyDetails) {
    errors.println(diagnostic);
    diagnostic.lines().forEach(line -> output.println("info string " + line));
    String formatted = diagnostic.lines()
        .flatMap(line -> line.startsWith("  side=")
            ? java.util.Arrays.stream(line.trim().split(" ")).map(field -> "  " + field)
            : java.util.stream.Stream.of(line))
        .collect(java.util.stream.Collectors.joining("\n"));
    recordLog(formatted + logOnlyDetails);
  }

  private synchronized void recordLog(String event) {
    if (sessionLog == null) {
      return;
    }
    try {
      sessionLog.record(event);
    } catch (IOException | SecurityException ex) {
      UciSessionLog failedLog = sessionLog;
      sessionLog = null;
      try {
        failedLog.close();
      } catch (IOException closeFailure) {
        ex.addSuppressed(closeFailure);
      }
      String warning = "Warning: cannot write Viktor log: " + ex;
      errors.println(warning);
      output.println("info string " + warning);
    }
  }

  private void stop(boolean suppressOutput) throws IOException {
    if (active == null) {
      return;
    }
    if (suppressOutput) {
      active.suppressOutput.set(true);
    }
    active.cancelled.set(true);
    try {
      active.thread.join();
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new IOException("Interrupted while stopping UCI search", ex);
    }
    active = null;
  }

  private final class SearchTask implements Runnable {
    private final Board position;
    private final GoOptions options;
    private final OpeningBook book;
    private final SyzygyTablebase tablebase;
    private final AtomicBoolean cancelled = new AtomicBoolean();
    private final AtomicBoolean suppressOutput = new AtomicBoolean();
    private final Thread thread;

    SearchTask(Board position, GoOptions options, OpeningBook book, SyzygyTablebase tablebase) {
      this.position = position;
      this.options = options;
      this.book = book;
      this.tablebase = tablebase;
      thread = new Thread(this, "viktor-uci-search");
    }

    @Override
    public void run() {
      long start = System.nanoTime();
      long timeNanos = options.timeMillis() < 0 ? -1
          : Math.min(options.timeMillis(), Long.MAX_VALUE / 1_000_000) * 1_000_000;
      Optional<Move> best;
      Optional<Move> fallback = Optional.empty();
      SearchResult result = null;
      int legalCount = 0;
      boolean inCheck = false;
      String reason = "no_completed_iteration";
      try {
        MoveGenerator generator = new MoveGenerator();
        List<Move> legalMoves = generator.generateLegalMoves(position, position.getSideToMove());
        legalCount = legalMoves.size();
        inCheck = generator.isKingInCheck(position, position.getSideToMove());
        fallback = legalMoves.stream().findFirst();
        best = fallback;
        Engine engine = new Engine(book, tablebase);
        java.util.function.BooleanSupplier deadline =
            () -> cancelled.get() || (timeNanos >= 0 && System.nanoTime() - start >= timeNanos);
        long remaining = timeNanos < 0 ? -1 : timeNanos - (System.nanoTime() - start);
        if (options.managedTime() && remaining > 0) {
          result = engine.thinkManaged(position, new SearchLimits(options.depth()), deadline,
              iteration -> reportIteration(iteration, start), Duration.ofNanos(remaining));
        } else {
          result = engine.think(position, new SearchLimits(options.depth()), deadline,
              iteration -> reportIteration(iteration, start));
        }
        if (result.bestMove().isPresent()) {
          reason = result.fromBook() ? "book" : result.tablebaseOutcome().isPresent() ? "tablebase" : "search";
          best = result.bestMove();
        } else if (result.drawClaim().isPresent()) {
          reason = "draw_claim";
          Optional<Move> intended = result.drawClaim().orElseThrow().intendedMove();
          best = intended.isPresent() ? intended : fallback;
          if (!suppressOutput.get()) {
            output.println("info string Draw claim is handled by the GUI; returning a legal move.");
          }
        } else {
          reason = fallback.isPresent() ? "automatic_draw" : "terminal";
          if (fallback.isPresent() && !suppressOutput.get()) {
            output.println("info string Draw is handled by the GUI; returning a legal move.");
          }
        }
      } catch (SearchTimeoutException ex) {
        if (!suppressOutput.get()) {
          output.println("info string Search stopped before completing an iteration.");
        }
        best = fallback;
      } catch (RuntimeException ex) {
        errors.println("UCI search failed:");
        ex.printStackTrace(errors);
        recordLog("error:\n  event=search_failed\n  message=" + ex);
        if (!suppressOutput.get()) {
          output.println("info string Search failed: " + ex.getClass().getSimpleName());
          output.println("bestmove 0000");
        }
        return;
      }
      if (options.infinite()) {
        while (!cancelled.get()) {
          try {
            Thread.sleep(10);
          } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            errors.println("UCI search thread interrupted.");
            return;
          }
        }
      }
      if (!suppressOutput.get()) {
        String decision = SearchDiagnostics.decision(reason,
            result != null ? result.bestMove().map(MoveNotation::format).orElse("none") : "none",
            result, "side=" + position.getSideToMove().name().toLowerCase(java.util.Locale.ROOT)
            + " fullmove=" + position.getFullmoveNumber()
            + " halfmove=" + position.getHalfmoveClock()
            + " in_check=" + inCheck + " legal_moves=" + legalCount
            + " depth_limit=" + options.depth()
            + " time_limit_ms=" + (options.timeMillis() < 0 ? "unlimited" : options.timeMillis())
            + " infinite=" + options.infinite() + " stop_requested=" + cancelled.get()
            + " elapsed_ms=" + (System.nanoTime() - start) / 1_000_000
            + " uci_move=" + best.map(MoveNotation::format).orElse("0000"), position);
        logDiagnostic(decision);
        if (result != null && result.bestMove().isPresent()) {
          Board after = new LegalMove(position, result.bestMove().orElseThrow()).resultingPosition();
          logDiagnostic(SearchDiagnostics.position(after, "projected_after_engine_move"));
        }
        output.println("bestmove " + best.map(MoveNotation::format).orElse("0000"));
      }
    }

    private void reportIteration(SearchResult result, long start) {
      if (suppressOutput.get()) {
        return;
      }
      String scoreText = SearchDiagnostics.score(result.score());
      long elapsed = (System.nanoTime() - start) / 1_000_000;
      String pv = result.trace().principalVariation().isEmpty() ? ""
          : " pv " + String.join(" ", result.trace().principalVariation());
      output.println("info depth " + result.depth() + " score " + scoreText
          + " nodes " + result.nodes() + " time " + elapsed + pv);
      recordLog("iteration:\n  depth=" + result.depth()
          + "\n  score=" + scoreText.replace(' ', ':')
          + "\n  score_perspective=" + result.perspective().name().toLowerCase(java.util.Locale.ROOT)
          + "\n  nodes=" + result.nodes() + "\n  elapsed_ms=" + elapsed
          + "\n  pv=" + (result.trace().principalVariation().isEmpty()
              ? "unavailable" : String.join(" ", result.trace().principalVariation())));
    }
  }
}
