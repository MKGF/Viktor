package com.desierto.search;

import com.desierto.domain.Move;
import com.desierto.domain.Color;
import java.util.Objects;
import java.util.Optional;

public record SearchResult(Optional<Move> bestMove, Color perspective, int score, int depth, long nodes,
                           Optional<DrawClaim> drawClaim) {

  public SearchResult(Optional<Move> bestMove, Color perspective, int score, int depth, long nodes) {
    this(bestMove, perspective, score, depth, nodes, Optional.empty());
  }

  public SearchResult {
    Objects.requireNonNull(bestMove, "bestMove must not be null");
    Objects.requireNonNull(perspective, "perspective must not be null");
    Objects.requireNonNull(drawClaim, "drawClaim must not be null");
    if (drawClaim.isPresent() && (bestMove.isPresent() || score != 0)) {
      throw new IllegalArgumentException("A draw claim must have no best move and a zero score");
    }
    if (depth < 0) {
      throw new IllegalArgumentException("depth must not be negative");
    }
    if (nodes < 0) {
      throw new IllegalArgumentException("nodes must not be negative");
    }
  }
}
