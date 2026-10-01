package com.desierto.search.evaluation;

import com.desierto.domain.Board;
import com.desierto.domain.Color;

public interface PositionEvaluator {

  int evaluate(Board board, Color perspective);

  default int evaluate(Board board, Color perspective, int plyFromRoot) {
    if (plyFromRoot < 0) {
      throw new IllegalArgumentException("plyFromRoot must not be negative");
    }
    return evaluate(board, perspective);
  }
}
