package com.desierto.search;

import com.desierto.domain.Board;
import java.time.Duration;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

/**
 * Thinks about a position without applying the resulting decision to the caller's board.
 * The caller is responsible for interpreting the result.
 */
public final class Engine {

  private final IterativeDeepeningSearch search;

  public Engine() {
    this.search = new IterativeDeepeningSearch();
  }

  public Engine(boolean quiescenceEnabled) {
    this.search = new IterativeDeepeningSearch(quiescenceEnabled);
  }

  public Engine(boolean quiescenceEnabled, boolean transpositionsEnabled) {
    this.search = new IterativeDeepeningSearch(quiescenceEnabled, transpositionsEnabled);
  }

  Engine(AlphaBetaSearch search, LongSupplier nanoTime) {
    this.search = new IterativeDeepeningSearch(search, nanoTime);
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
   * Returns the last completed iteration. Nodes include any interrupted iteration.
   * The deadline is checked between search operations, not during an operation.
   *
   * @throws SearchTimeoutException if no iteration completes within the limit
   */
  public SearchResult think(Board board, SearchLimits limits, Duration timeLimit) {
    return search.findBestMove(board, limits, timeLimit);
  }
}
