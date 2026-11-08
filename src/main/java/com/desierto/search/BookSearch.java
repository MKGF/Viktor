package com.desierto.search;

import com.desierto.book.OpeningBook;
import com.desierto.domain.Board;
import com.desierto.domain.DrawDetector;
import com.desierto.domain.Move;
import com.desierto.domain.MoveGenerator;
import com.desierto.tablebase.SyzygyTablebase;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** Consults optional tablebases and a book before falling back to iterative search. */
final class BookSearch {
  private final IterativeDeepeningSearch search;
  private final OpeningBook book;
  private final SyzygyTablebase tablebase;

  BookSearch(IterativeDeepeningSearch search, OpeningBook book) {
    this(search, book, null);
  }

  BookSearch(IterativeDeepeningSearch search, OpeningBook book, SyzygyTablebase tablebase) {
    this.search = Objects.requireNonNull(search, "search must not be null");
    this.book = book;
    this.tablebase = tablebase;
  }

  SearchResult findBestMove(Board board, SearchLimits limits) {
    Objects.requireNonNull(limits, "limits must not be null");
    return immediateResult(board).orElseGet(() -> search.findBestMove(board, limits));
  }

  SearchResult findBestMove(Board board, SearchLimits limits, BooleanSupplier cancelled,
      Consumer<SearchResult> onIteration) {
    Objects.requireNonNull(limits, "limits must not be null");
    Objects.requireNonNull(cancelled, "cancelled must not be null");
    Objects.requireNonNull(onIteration, "onIteration must not be null");
    if (cancelled.getAsBoolean()) throw new SearchTimeoutException();
    Optional<SearchResult> result = immediateResult(board);
    if (cancelled.getAsBoolean()) throw new SearchTimeoutException();
    return result.orElseGet(() -> search.findBestMove(board, limits, cancelled, onIteration));
  }

  SearchResult findBestMove(Board board, SearchLimits limits, Duration timeLimit) {
    Objects.requireNonNull(limits, "limits must not be null");
    Objects.requireNonNull(timeLimit, "timeLimit must not be null");
    long nanos;
    try {
      nanos = timeLimit.toNanos();
    } catch (ArithmeticException ex) {
      throw new IllegalArgumentException("timeLimit is too large", ex);
    }
    if (nanos <= 0) throw new IllegalArgumentException("timeLimit must be positive");
    if (book == null && tablebase == null) return search.findBestMove(board, limits, timeLimit);
    long start = System.nanoTime();
    Optional<SearchResult> result = immediateResult(board);
    long remaining = nanos - (System.nanoTime() - start);
    if (remaining <= 0) throw new SearchTimeoutException();
    return result.orElseGet(() -> search.findBestMove(board, limits, Duration.ofNanos(remaining)));
  }

  private Optional<SearchResult> immediateResult(Board board) {
    Objects.requireNonNull(board, "board must not be null");
    if (tablebase != null) {
      Optional<SyzygyTablebase.Decision> decision = tablebase.probe(board);
      if (decision.isPresent()) {
        SyzygyTablebase.Decision value = decision.orElseThrow();
        return Optional.of(new SearchResult(Optional.of(value.move()), board.getSideToMove(),
            0, 0, 0, Optional.empty(), SearchTrace.EMPTY, false, Optional.of(value.outcome())));
      }
    }
    return bookResult(board);
  }

  private Optional<SearchResult> bookResult(Board board) {
    Objects.requireNonNull(board, "board must not be null");
    if (book == null || board.getHalfmoveClock() >= 99
        || DrawDetector.isDeadByInsufficientMaterial(board)) return Optional.empty();
    MoveGenerator generator = new MoveGenerator();
    if (DrawDetector.isSeventyFiveMoveDraw(board, generator)
        || DrawDetector.isThreefoldRepetitionDraw(board, generator)) return Optional.empty();
    Optional<Move> move = book.findMove(board);
    return move.map(value -> new SearchResult(Optional.of(value), board.getSideToMove(), 0, 0, 0,
        Optional.empty(), SearchTrace.EMPTY, true));
  }
}
