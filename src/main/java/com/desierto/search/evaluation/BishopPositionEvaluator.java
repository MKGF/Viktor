package com.desierto.search.evaluation;

import com.desierto.domain.Piece;
import com.desierto.domain.pieces.Bishop;
import com.desierto.domain.pieces.Pawn;

final class BishopPositionEvaluator {
  private static final int SAME_COLOR_PAWN_BONUS = 2;
  private static final int BLOCKING_PAWN_PENALTY = 3;

  int evaluate(Bishop bishop, PositionContext context) {
    int adjustment = 0;
    for (Piece piece : context.pieces()) {
      if (piece instanceof Pawn) {
        if (piece.getColor() == bishop.getColor() && context.attacks(bishop, piece.getCell())) {
          adjustment -= BLOCKING_PAWN_PENALTY;
        } else if (piece.getColor() != bishop.getColor()
            && (piece.getCell().row() + piece.getCell().column()) % 2
                == (bishop.getCell().row() + bishop.getCell().column()) % 2) {
          adjustment += SAME_COLOR_PAWN_BONUS;
        }
      }
    }
    return context.base(bishop) + context.undevelopedPenalty(bishop)
        + adjustment * PositionalEvaluator.SCORE_SCALE;
  }
}
