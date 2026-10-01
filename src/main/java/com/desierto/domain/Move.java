package com.desierto.domain;

import java.util.Objects;
import com.desierto.domain.pieces.Bishop;
import com.desierto.domain.pieces.Knight;
import com.desierto.domain.pieces.Queen;
import com.desierto.domain.pieces.Rook;

public record Move(
    Color color,
    Piece piece,
    Cell from,
    Cell to,
    Piece promotionPiece,
    MoveType type,
    Piece capturedPiece
) {

  public Move {
    Objects.requireNonNull(color, "color must not be null");
    Objects.requireNonNull(piece, "piece must not be null");
    Objects.requireNonNull(from, "from must not be null");
    Objects.requireNonNull(to, "to must not be null");
    Objects.requireNonNull(type, "type must not be null");
    boolean promotion = type == MoveType.PROMOTION || type == MoveType.CAPTURE_PROMOTION;
    boolean capture = type == MoveType.CAPTURE
        || type == MoveType.CAPTURE_PROMOTION
        || type == MoveType.EN_PASSANT;
    if (promotion != (promotionPiece != null)) {
      throw new IllegalArgumentException("Promotion moves must include a promotion piece");
    }
    if (capture != (capturedPiece != null)) {
      throw new IllegalArgumentException("Capture moves must include a captured piece");
    }
    if (piece.getColor() != color) {
      throw new IllegalArgumentException("Move color must match piece color");
    }
    if (promotionPiece != null
        && (!(promotionPiece instanceof Queen)
        && !(promotionPiece instanceof Rook)
        && !(promotionPiece instanceof Bishop)
        && !(promotionPiece instanceof Knight))) {
      throw new IllegalArgumentException("Promotion piece must be a queen, rook, bishop, or knight");
    }
    if (promotionPiece != null && promotionPiece.getColor() != color) {
      throw new IllegalArgumentException("Promotion piece color must match move color");
    }
    if (capturedPiece != null && capturedPiece.getColor() == color) {
      throw new IllegalArgumentException("Captured piece must have the opposite color");
    }
  }

  public boolean isCapture() {
    return capturedPiece != null;
  }

  public boolean isPromotion() {
    return promotionPiece != null;
  }
}
