package com.desierto.search.evaluation;

import com.desierto.domain.pieces.Pawn;

final class PawnPositionEvaluator {
  int evaluate(Pawn pawn, PositionContext context) {
    return context.base(pawn);
  }
}
