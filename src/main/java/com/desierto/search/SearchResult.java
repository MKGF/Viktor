package com.desierto.search;

import com.desierto.domain.Move;
import com.desierto.domain.Color;
import com.desierto.tablebase.TablebaseOutcome;
import java.util.Objects;
import java.util.Optional;

/**
 * Search decision. When {@code fromBook} is true, the move has not been evaluated:
 * {@code score}, {@code depth} and {@code nodes} are zero placeholders. The same applies
 * when {@code tablebaseOutcome} is present: WDL/DTZ is not a centipawn or mate score.
 */
public record SearchResult(Optional<Move> bestMove, Color perspective, int score, int depth, long nodes,
                           Optional<DrawClaim> drawClaim, SearchTrace trace, boolean fromBook,
                           Optional<TablebaseOutcome> tablebaseOutcome) {

  public SearchResult(Optional<Move> bestMove, Color perspective, int score, int depth, long nodes,
      Optional<DrawClaim> drawClaim, SearchTrace trace, boolean fromBook) {
    this(bestMove, perspective, score, depth, nodes, drawClaim, trace, fromBook, Optional.empty());
  }

  public SearchResult(Optional<Move> bestMove, Color perspective, int score, int depth, long nodes,
      Optional<DrawClaim> drawClaim, SearchTrace trace) {
    this(bestMove, perspective, score, depth, nodes, drawClaim, trace, false);
  }

  public SearchResult(Optional<Move> bestMove, Color perspective, int score, int depth, long nodes) {
    this(bestMove, perspective, score, depth, nodes, Optional.empty(), SearchTrace.EMPTY);
  }

  public SearchResult(Optional<Move> bestMove, Color perspective, int score, int depth, long nodes,
      Optional<DrawClaim> drawClaim) {
    this(bestMove, perspective, score, depth, nodes, drawClaim, SearchTrace.EMPTY);
  }

  public SearchResult {
    Objects.requireNonNull(bestMove, "bestMove must not be null");
    Objects.requireNonNull(perspective, "perspective must not be null");
    Objects.requireNonNull(drawClaim, "drawClaim must not be null");
    Objects.requireNonNull(trace, "trace must not be null");
    Objects.requireNonNull(tablebaseOutcome, "tablebaseOutcome must not be null");
    if (drawClaim.isPresent() && (bestMove.isPresent() || score != 0)) {
      throw new IllegalArgumentException("A draw claim must have no best move and a zero score");
    }
    if (fromBook && (bestMove.isEmpty() || drawClaim.isPresent() || score != 0 || depth != 0 || nodes != 0)) {
      throw new IllegalArgumentException("A book result must contain an unevaluated move");
    }
    if (tablebaseOutcome.isPresent() && (fromBook || bestMove.isEmpty() || drawClaim.isPresent()
        || score != 0 || depth != 0 || nodes != 0)) {
      throw new IllegalArgumentException("A tablebase result must contain an unevaluated move");
    }
    if (depth < 0) {
      throw new IllegalArgumentException("depth must not be negative");
    }
    if (nodes < 0) {
      throw new IllegalArgumentException("nodes must not be negative");
    }
  }
}
