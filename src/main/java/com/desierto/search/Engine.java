package com.desierto.search;

import com.desierto.domain.Board;
import com.desierto.book.OpeningBook;
import com.desierto.tablebase.SyzygyTablebase;
import java.time.Duration;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

/**
 * Thinks about a position without applying the resulting decision to the caller's board.
 * The caller is responsible for interpreting the result.
 */
public final class Engine {

  private final BookSearch search;

  public Engine() {
    this.search = new BookSearch(new IterativeDeepeningSearch(), null);
  }

  public Engine(OpeningBook book) {
    this.search = new BookSearch(new IterativeDeepeningSearch(), book);
  }

  public Engine(OpeningBook book, SyzygyTablebase tablebase) {
    this.search = new BookSearch(new IterativeDeepeningSearch(), book, tablebase);
  }

  public Engine(boolean quiescenceEnabled) {
    this.search = new BookSearch(new IterativeDeepeningSearch(quiescenceEnabled), null);
  }

  public Engine(boolean quiescenceEnabled, boolean transpositionsEnabled) {
    this.search = new BookSearch(new IterativeDeepeningSearch(quiescenceEnabled, transpositionsEnabled), null);
  }

  Engine(AlphaBetaSearch search, LongSupplier nanoTime) {
    this.search = new BookSearch(new IterativeDeepeningSearch(search, nanoTime), null);
  }

  public SearchResult think(Board board, SearchLimits limits) {
    return search.findBestMove(board, limits);
  }

  /**
   * Reports each completed iteration and returns the last one if cancelled.
   *
   * @throws SearchTimeoutException if cancelled before an iteration completes
   */
  public SearchResult think(Board board, SearchLimits limits, BooleanSupplier cancelled,
      Consumer<SearchResult> onIteration) {
    return search.findBestMove(board, limits, cancelled, onIteration);
  }

  /**
   * Uses a soft, position-dependent budget in addition to the caller's hard cancellation.
   * The caller must enforce the hard deadline through {@code cancelled}.
   */
  public SearchResult thinkManaged(Board board, SearchLimits limits, BooleanSupplier cancelled,
      Consumer<SearchResult> onIteration, Duration budget) {
    long nanos;
    try {
      nanos = budget.toNanos();
    } catch (ArithmeticException ex) {
      throw new IllegalArgumentException("budget is too large", ex);
    }
    return search.findBestMoveManaged(board, limits, cancelled, onIteration, nanos);
  }

  public SearchResult thinkManaged(Board board, SearchLimits limits, Duration budget) {
    long nanos;
    try {
      nanos = budget.toNanos();
    } catch (ArithmeticException ex) {
      throw new IllegalArgumentException("budget is too large", ex);
    }
    if (nanos <= 0) throw new IllegalArgumentException("budget must be positive");
    long start = System.nanoTime();
    return search.findBestMoveManaged(board, limits, () -> System.nanoTime() - start >= nanos,
        result -> {}, nanos);
  }

  /**
   * Returns the last completed iteration. Nodes include any interrupted iteration.
   * The deadline is checked between search operations, not during an operation.
   *
   * @throws SearchTimeoutException if no iteration completes within the limit
   */
  public SearchResult think(Board board, SearchLimits limits, Duration timeLimit) {
    return search.findBestMove(board, limits, timeLimit);
  }
}
