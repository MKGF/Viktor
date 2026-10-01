package com.desierto.search;

import com.desierto.domain.Move;
import com.desierto.domain.Color;
import java.util.Objects;
import java.util.Optional;

public record SearchResult(Optional<Move> bestMove, Color perspective, int score, int depth, long nodes) {

  public SearchResult {
    Objects.requireNonNull(bestMove, "bestMove must not be null");
    Objects.requireNonNull(perspective, "perspective must not be null");
    if (depth < 0) {
      throw new IllegalArgumentException("depth must not be negative");
    }
    if (nodes < 0) {
      throw new IllegalArgumentException("nodes must not be negative");
    }
  }
}
