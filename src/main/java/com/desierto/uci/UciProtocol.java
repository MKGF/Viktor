package com.desierto.uci;

import com.desierto.domain.Board;
import com.desierto.domain.Move;
import com.desierto.domain.MoveGenerator;
import com.desierto.domain.MoveNotation;
import com.desierto.search.IterativeDeepeningEngine;
import com.desierto.search.SearchLimits;
import com.desierto.search.SearchDiagnostics;
import com.desierto.search.SearchResult;
import com.desierto.search.SearchTimeoutException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

final class UciProtocol {
  private final PrintStream output;
  private final PrintStream errors;
  private Board board = Board.standard();
  private SearchTask active;

  UciProtocol(PrintStream output, PrintStream errors) {
    this.output = Objects.requireNonNull(output, "output must not be null");
    this.errors = Objects.requireNonNull(errors, "errors must not be null");
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
              output.println("id name Viktor");
              output.println("id author MKGF");
              output.println("uciok");
            }
            case "isready" -> output.println("readyok");
            case "ucinewgame" -> {
              stop(false);
              board = Board.standard();
            }
            case "position" -> {
              stop(false);
              board = UciPosition.parse(tokens);
            }
            case "go" -> {
              stop(false);
              GoOptions options = GoOptions.parse(tokens, board.getSideToMove());
              active = new SearchTask(board.copy(), options);
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
          if (tokens[0].equals("go")) {
            output.println("bestmove 0000");
          }
        }
      }
    } finally {
      stop(true);
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
    private final AtomicBoolean cancelled = new AtomicBoolean();
    private final AtomicBoolean suppressOutput = new AtomicBoolean();
    private final Thread thread;

    SearchTask(Board position, GoOptions options) {
      this.position = position;
      this.options = options;
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
        result = new IterativeDeepeningEngine().findBestMove(position,
            new SearchLimits(options.depth()),
            () -> cancelled.get() || (timeNanos >= 0 && System.nanoTime() - start >= timeNanos),
            iteration -> reportIteration(iteration, start));
        if (result.bestMove().isPresent()) {
          reason = "search";
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
        String decision = "decision reason=" + reason
            + " move=" + best.map(MoveNotation::format).orElse("0000")
            + " side=" + position.getSideToMove().name().toLowerCase(java.util.Locale.ROOT)
            + " fullmove=" + position.getFullmoveNumber()
            + " halfmove=" + position.getHalfmoveClock()
            + " in_check=" + inCheck + " legal_moves=" + legalCount
            + " depth_limit=" + options.depth()
            + " time_limit_ms=" + (options.timeMillis() < 0 ? "unlimited" : options.timeMillis())
            + " infinite=" + options.infinite() + " stop_requested=" + cancelled.get()
            + " elapsed_ms=" + (System.nanoTime() - start) / 1_000_000
            + (result == null ? " depth=unavailable nodes=unavailable score=unavailable"
                : " " + SearchDiagnostics.result(result));
        errors.println(decision);
        output.println("info string " + decision);
        output.println("bestmove " + best.map(MoveNotation::format).orElse("0000"));
      }
    }

    private void reportIteration(SearchResult result, long start) {
      if (suppressOutput.get()) {
        return;
      }
      String scoreText = SearchDiagnostics.score(result.score());
      long elapsed = (System.nanoTime() - start) / 1_000_000;
      String pv = result.bestMove().map(move -> " pv " + MoveNotation.format(move)).orElse("");
      output.println("info depth " + result.depth() + " score " + scoreText
          + " nodes " + result.nodes() + " time " + elapsed + pv);
    }
  }
}
