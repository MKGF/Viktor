package com.desierto.search.evaluation;

import com.desierto.domain.Board;
import com.desierto.domain.Cell;
import com.desierto.domain.Color;
import com.desierto.domain.Piece;
import com.desierto.domain.pieces.Bishop;
import com.desierto.domain.pieces.King;
import com.desierto.domain.pieces.Knight;
import com.desierto.domain.pieces.Pawn;
import com.desierto.domain.pieces.Queen;
import com.desierto.domain.pieces.Rook;
import java.util.List;
import java.util.Objects;

public final class PositionalEvaluator implements PositionEvaluator {
  private static final int MAX_PHASE = 24;
  private static final int UNDEVELOPED_MINOR_PENALTY = 12;
  private static final int KING_SHIELD_BONUS = 8;

  // Rows run from White's first rank to its eighth; Black uses the mirrored rank.
  private static final int[][] PAWN = {
      {  0,  0,  0,  0,  0,  0,  0,  0 },
      {  0,  0,  0, -4, -4,  0,  0,  0 },
      {  2,  4,  5,  6,  6,  5,  4,  2 },
      {  4,  6, 10, 18, 18, 10,  6,  4 },
      {  8, 10, 14, 22, 22, 14, 10,  8 },
      { 14, 16, 20, 24, 24, 20, 16, 14 },
      { 24, 26, 28, 30, 30, 28, 26, 24 },
      {  0,  0,  0,  0,  0,  0,  0,  0 }
  };
  private static final int[][] KNIGHT = {
      { -30, -20, -14, -12, -12, -14, -20, -30 },
      { -20, -10,  -2,   2,   2,  -2, -10, -20 },
      { -14,  -2,  12,  16,  16,  12,  -2, -14 },
      { -12,   2,  16,  22,  22,  16,   2, -12 },
      { -12,   2,  16,  22,  22,  16,   2, -12 },
      { -14,  -2,  12,  16,  16,  12,  -2, -14 },
      { -20, -10,  -2,   2,   2,  -2, -10, -20 },
      { -30, -20, -14, -12, -12, -14, -20, -30 }
  };
  private static final int[][] BISHOP = {
      { -14, -8, -8, -8, -8, -8, -8, -14 },
      {  -8,  2,  0,  0,  0,  0,  2,  -8 },
      {  -8,  4,  8, 10, 10,  8,  4,  -8 },
      {  -8,  2, 10, 14, 14, 10,  2,  -8 },
      {  -8,  2, 10, 14, 14, 10,  2,  -8 },
      {  -8,  4,  8, 10, 10,  8,  4,  -8 },
      {  -8,  2,  0,  0,  0,  0,  2,  -8 },
      { -14, -8, -8, -8, -8, -8, -8, -14 }
  };
  private static final int[][] ROOK = {
      {  0,  0,  2,  4,  4,  2,  0,  0 },
      { -2,  0,  0,  0,  0,  0,  0, -2 },
      { -2,  0,  0,  0,  0,  0,  0, -2 },
      { -2,  0,  0,  0,  0,  0,  0, -2 },
      { -2,  0,  0,  0,  0,  0,  0, -2 },
      {  0,  2,  2,  2,  2,  2,  2,  0 },
      {  6,  8,  8,  8,  8,  8,  8,  6 },
      {  0,  2,  2,  4,  4,  2,  2,  0 }
  };
  private static final int[][] QUEEN = {
      { -10, -6, -4, -2, -2, -4, -6, -10 },
      {  -6, -2,  0,  0,  0,  0, -2,  -6 },
      {  -4,  0,  4,  6,  6,  4,  0,  -4 },
      {  -2,  0,  6,  8,  8,  6,  0,  -2 },
      {  -2,  0,  6,  8,  8,  6,  0,  -2 },
      {  -4,  0,  4,  6,  6,  4,  0,  -4 },
      {  -6, -2,  0,  0,  0,  0, -2,  -6 },
      { -10, -6, -4, -2, -2, -4, -6, -10 }
  };
  private static final int[][] KING_MIDDLEGAME = {
      {  8, 18, 24, -10, -10,  4, 24,  8 },
      {  0,  4,  0, -12, -12,  0,  4,  0 },
      { -8,-12,-16, -24, -24,-16,-12, -8 },
      {-16,-20,-24, -32, -32,-24,-20,-16 },
      {-20,-24,-28, -36, -36,-28,-24,-20 },
      {-24,-28,-32, -40, -40,-32,-28,-24 },
      {-28,-32,-36, -44, -44,-36,-32,-28 },
      {-32,-36,-40, -48, -48,-40,-36,-32 }
  };
  private static final int[][] KING_ENDGAME = {
      { -24,-16,-12, -8, -8,-12,-16,-24 },
      { -16, -8, -2,  2,  2, -2, -8,-16 },
      { -12, -2,  8, 12, 12,  8, -2,-12 },
      {  -8,  2, 12, 20, 20, 12,  2, -8 },
      {  -8,  2, 12, 20, 20, 12,  2, -8 },
      { -12, -2,  8, 12, 12,  8, -2,-12 },
      { -16, -8, -2,  2,  2, -2, -8,-16 },
      { -24,-16,-12, -8, -8,-12,-16,-24 }
  };

  @Override
  public int evaluate(Board board, Color perspective) {
    Objects.requireNonNull(board, "board must not be null");
    Objects.requireNonNull(perspective, "perspective must not be null");
    List<Piece> pieces = board.getPieces();
    int phase = 0;
    for (Piece piece : pieces) {
      phase += phaseWeight(piece);
    }
    phase = Math.min(phase, MAX_PHASE);

    int weightedScore = 0;
    for (Piece piece : pieces) {
      Cell cell = piece.getCell();
      int rank = piece.getColor() == Color.WHITE ? cell.row() : 7 - cell.row();
      int file = cell.column();
      int middlegame = table(piece)[rank][file];
      int endgame = piece instanceof King ? KING_ENDGAME[rank][file] : middlegame;
      if ((piece instanceof Knight && (file == 1 || file == 6)
          || piece instanceof Bishop && (file == 2 || file == 5)) && rank == 0) {
        middlegame -= UNDEVELOPED_MINOR_PENALTY;
      }
      if (piece instanceof King) {
        middlegame += kingShield(board, piece);
      }
      int value = middlegame * phase + endgame * (MAX_PHASE - phase);
      weightedScore += piece.getColor() == Color.WHITE ? value : -value;
    }
    int whiteScore = weightedScore / MAX_PHASE;
    return perspective == Color.WHITE ? whiteScore : -whiteScore;
  }

  private static int phaseWeight(Piece piece) {
    if (piece instanceof Queen) {
      return 4;
    }
    if (piece instanceof Rook) {
      return 2;
    }
    if (piece instanceof Knight || piece instanceof Bishop) {
      return 1;
    }
    return 0;
  }

  private static int[][] table(Piece piece) {
    if (piece instanceof Pawn) {
      return PAWN;
    }
    if (piece instanceof Knight) {
      return KNIGHT;
    }
    if (piece instanceof Bishop) {
      return BISHOP;
    }
    if (piece instanceof Rook) {
      return ROOK;
    }
    if (piece instanceof Queen) {
      return QUEEN;
    }
    if (piece instanceof King) {
      return KING_MIDDLEGAME;
    }
    throw new IllegalArgumentException("Unknown piece type");
  }

  private static int kingShield(Board board, Piece king) {
    Cell cell = king.getCell();
    int row = cell.row() + (king.getColor() == Color.WHITE ? 1 : -1);
    if (row < 0 || row > 7) {
      return 0;
    }
    int shield = 0;
    for (int file = Math.max(0, cell.column() - 1); file <= Math.min(7, cell.column() + 1); file++) {
      Piece piece = board.getPieceAt(Cell.of(row, file)).orElse(null);
      if (piece instanceof Pawn && piece.getColor() == king.getColor()) {
        shield += KING_SHIELD_BONUS;
      }
    }
    return shield;
  }
}
