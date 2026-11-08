package com.desierto.search.evaluation;

import com.desierto.domain.pieces.Queen;

final class QueenPositionEvaluator {
  int evaluate(Queen queen, PositionContext context) {
    return context.base(queen);
  }
}
