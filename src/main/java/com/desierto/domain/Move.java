package com.desierto.domain;

import java.util.Objects;

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
    if (promotionPiece != null && type != MoveType.PROMOTION && type != MoveType.CAPTURE_PROMOTION) {
      throw new IllegalArgumentException("Promotion piece is only valid for promotion moves");
    }
    if (capturedPiece != null
        && type != MoveType.CAPTURE
        && type != MoveType.CAPTURE_PROMOTION
        && type != MoveType.EN_PASSANT) {
      throw new IllegalArgumentException("Captured piece is only valid for capture moves");
    }
    if (piece.getColor() != color) {
      throw new IllegalArgumentException("Move color must match piece color");
    }
  }

  public boolean isCapture() {
    return capturedPiece != null;
  }

  public boolean isPromotion() {
    return promotionPiece != null;
  }
}
