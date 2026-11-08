package com.desierto.search.evaluation;

import com.desierto.domain.Cell;
import com.desierto.domain.Color;
import com.desierto.domain.Piece;
import com.desierto.domain.pieces.King;
import com.desierto.domain.pieces.Pawn;

final class KingPositionEvaluator {
  private static final int KING_SHIELD_BONUS = 8;

  int evaluate(King king, PositionContext context) {
    int middlegame = PositionalEvaluator.tableValue(king) + shield(king, context);
    int endgame = PositionalEvaluator.kingEndgameValue(king);
    return (middlegame * context.phase()
        + endgame * (PositionalEvaluator.MAX_PHASE - context.phase())) * PositionalEvaluator.TABLE_SCALE;
  }

  private int shield(King king, PositionContext context) {
    Cell cell = king.getCell();
    int row = cell.row() + (king.getColor() == Color.WHITE ? 1 : -1);
    if (row < 0 || row > 7) return 0;
    int bonus = 0;
    for (int file = Math.max(0, cell.column() - 1); file <= Math.min(7, cell.column() + 1); file++) {
      Piece piece = context.board().getPieceAt(Cell.of(row, file)).orElse(null);
      if (piece instanceof Pawn && piece.getColor() == king.getColor()) bonus += KING_SHIELD_BONUS;
    }
    return bonus;
  }
}
