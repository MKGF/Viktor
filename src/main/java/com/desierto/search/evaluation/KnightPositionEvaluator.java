package com.desierto.search.evaluation;

import com.desierto.domain.Cell;
import com.desierto.domain.Color;
import com.desierto.domain.Piece;
import com.desierto.domain.pieces.Knight;
import com.desierto.domain.pieces.Pawn;

final class KnightPositionEvaluator {
  private static final int OUTPOST_BONUS = 8;
  private static final int SUPPORTED_OUTPOST_BONUS = 4;
  private static final int NEARBY_PAWN_BONUS = 1;
  private static final int MAX_NEARBY_PAWNS = 4;

  int evaluate(Knight knight, PositionContext context) {
    Cell square = knight.getCell();
    int rank = knight.getColor() == Color.WHITE ? square.row() : 7 - square.row();
    boolean challenged = false;
    boolean supported = false;
    int nearbyPawns = 0;
    for (Piece piece : context.pieces()) {
      if (!(piece instanceof Pawn)) continue;
      if (piece.getColor() == knight.getColor()) {
        supported |= context.attacks(piece, square);
      } else {
        int fileDistance = Math.abs(piece.getCell().column() - square.column());
        int rankDistance = Math.abs(piece.getCell().row() - square.row());
        if (fileDistance == 1
            && (piece.getColor() == Color.WHITE ? piece.getCell().row() < square.row()
                : piece.getCell().row() > square.row())) {
          challenged = true;
        }
        if (fileDistance <= 2 && rankDistance <= 2
            && !context.attacks(knight, piece.getCell())) {
          nearbyPawns++;
        }
      }
    }
    int bonus = rank >= 3 && rank <= 5 && !challenged
        ? OUTPOST_BONUS + (supported ? SUPPORTED_OUTPOST_BONUS : 0)
            + Math.min(nearbyPawns, MAX_NEARBY_PAWNS) * NEARBY_PAWN_BONUS : 0;
    return context.base(knight) + context.undevelopedPenalty(knight)
        + bonus * PositionalEvaluator.SCORE_SCALE;
  }
}
