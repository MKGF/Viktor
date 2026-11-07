package com.desierto.search;

import com.desierto.domain.Board;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

public final class IterativeDeepeningEngine implements SearchEngine {

  private final FixedDepthAlphaBetaEngine engine;
  private final LongSupplier nanoTime;

  public IterativeDeepeningEngine() {
    this(new FixedDepthAlphaBetaEngine(), System::nanoTime);
  }

  public IterativeDeepeningEngine(boolean quiescenceEnabled) {
    this(new FixedDepthAlphaBetaEngine(quiescenceEnabled), System::nanoTime);
  }

  public IterativeDeepeningEngine(boolean quiescenceEnabled, boolean transpositionsEnabled) {
    this(new FixedDepthAlphaBetaEngine(quiescenceEnabled, transpositionsEnabled), System::nanoTime);
  }

  IterativeDeepeningEngine(FixedDepthAlphaBetaEngine engine, LongSupplier nanoTime) {
    this.engine = Objects.requireNonNull(engine, "engine must not be null");
    this.nanoTime = Objects.requireNonNull(nanoTime, "nanoTime must not be null");
  }

  @Override
  public SearchResult findBestMove(Board board, SearchLimits limits) {
    return search(board, limits, new FixedDepthAlphaBetaEngine.SearchContext(), result -> {});
  }

  /**
   * Searches until the requested depth or cancellation and reports each completed iteration.
   * If cancelled before an iteration completes, throws {@link SearchTimeoutException}.
   */
  public SearchResult findBestMove(Board board, SearchLimits limits, BooleanSupplier cancelled,
      Consumer<SearchResult> onIteration) {
    Objects.requireNonNull(cancelled, "cancelled must not be null");
    Objects.requireNonNull(onIteration, "onIteration must not be null");
    return search(board, limits, new FixedDepthAlphaBetaEngine.SearchContext(cancelled), onIteration);
  }

  /**
   * Returns the last completed iteration. Nodes include any interrupted iteration.
   * The deadline is checked between search operations, not during an operation.
   *
   * @throws SearchTimeoutException if no iteration completes within the limit
   */
  public SearchResult findBestMove(Board board, SearchLimits limits, Duration timeLimit) {
    Objects.requireNonNull(board, "board must not be null");
    Objects.requireNonNull(limits, "limits must not be null");
    Objects.requireNonNull(timeLimit, "timeLimit must not be null");
    long limitNanos;
    try {
      limitNanos = timeLimit.toNanos();
    } catch (ArithmeticException ex) {
      throw new IllegalArgumentException("timeLimit is too large", ex);
    }
    if (limitNanos <= 0) {
      throw new IllegalArgumentException("timeLimit must be positive");
    }
    long start = nanoTime.getAsLong();
    return search(board, limits, new FixedDepthAlphaBetaEngine.SearchContext(
        () -> nanoTime.getAsLong() - start >= limitNanos), result -> {});
  }

  private SearchResult search(Board board, SearchLimits limits,
      FixedDepthAlphaBetaEngine.SearchContext context, Consumer<SearchResult> onIteration) {
    Objects.requireNonNull(board, "board must not be null");
    Objects.requireNonNull(limits, "limits must not be null");
    SearchResult completed = null;
    Optional<MoveKey> preferredMove = Optional.empty();
    for (int depth = 1; ; depth++) {
      try {
        completed = engine.findBestMove(board, new SearchLimits(depth), preferredMove, context);
      } catch (FixedDepthAlphaBetaEngine.SearchDeadlineExceeded ex) {
        if (completed == null) {
          throw new SearchTimeoutException();
        }
        return new SearchResult(completed.bestMove(), completed.perspective(), completed.score(),
            completed.depth(), context.nodes(), completed.drawClaim(), completed.trace());
      }
      onIteration.accept(completed);
      if ((completed.bestMove().isEmpty() && completed.drawClaim().isEmpty())
          || depth == limits.maxDepth()) {
        return completed;
      }
      preferredMove = completed.bestMove().map(MoveKey::from);
    }
  }
}
