package com.desierto.search.evaluation;

import com.desierto.domain.pieces.Rook;

final class RookPositionEvaluator {
  private static final int OPEN_FILE_BONUS = 8;
  private static final int SEMI_OPEN_FILE_BONUS = 4;

  int evaluate(Rook rook, PositionContext context) {
    int file = rook.getCell().column();
    int bonus = 0;
    if (!context.hasPawnOnFile(rook.getColor(), file)) {
      bonus = context.hasPawnOnFile(rook.getColor().opposite(), file)
          ? SEMI_OPEN_FILE_BONUS : OPEN_FILE_BONUS;
    }
    return context.base(rook) + bonus * PositionalEvaluator.SCORE_SCALE;
  }
}
