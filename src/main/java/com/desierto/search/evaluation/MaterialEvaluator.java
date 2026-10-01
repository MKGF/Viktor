package com.desierto.search.evaluation;

import com.desierto.domain.Board;
import com.desierto.domain.Color;
import com.desierto.domain.Piece;
import com.desierto.domain.pieces.Bishop;
import com.desierto.domain.pieces.King;
import com.desierto.domain.pieces.Knight;
import com.desierto.domain.pieces.Pawn;
import com.desierto.domain.pieces.Queen;
import com.desierto.domain.pieces.Rook;
import java.util.Objects;

public final class MaterialEvaluator implements PositionEvaluator {

  static final int PAWN_VALUE = 100;
  static final int KNIGHT_VALUE = 320;
  static final int BISHOP_VALUE = 330;
  static final int ROOK_VALUE = 500;
  static final int QUEEN_VALUE = 900;

  @Override
  public int evaluate(Board board, Color perspective) {
    Objects.requireNonNull(board, "board must not be null");
    Objects.requireNonNull(perspective, "perspective must not be null");

    int score = 0;
    for (Piece piece : board.getPieces()) {
      int signedValue = piece.getColor() == perspective ? pieceValue(piece) : -pieceValue(piece);
      score += signedValue;
    }
    return score;
  }

  private int pieceValue(Piece piece) {
    if (piece instanceof Pawn) {
      return PAWN_VALUE;
    }
    if (piece instanceof Knight) {
      return KNIGHT_VALUE;
    }
    if (piece instanceof Bishop) {
      return BISHOP_VALUE;
    }
    if (piece instanceof Rook) {
      return ROOK_VALUE;
    }
    if (piece instanceof Queen) {
      return QUEEN_VALUE;
    }
    if (piece instanceof King) {
      return 0;
    }
    throw new IllegalArgumentException("Unknown piece type");
  }
}
