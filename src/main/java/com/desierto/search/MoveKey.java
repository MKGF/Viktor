package com.desierto.search;

import com.desierto.domain.Cell;
import com.desierto.domain.Color;
import com.desierto.domain.Move;
import com.desierto.domain.MoveType;
import com.desierto.domain.Piece;
import java.util.Objects;

public record MoveKey(Color color, Cell from, Cell to, MoveType type,
                      Class<? extends Piece> promotionType) {

  public MoveKey {
    Objects.requireNonNull(color, "color must not be null");
    Objects.requireNonNull(from, "from must not be null");
    Objects.requireNonNull(to, "to must not be null");
    Objects.requireNonNull(type, "type must not be null");
  }

  public static MoveKey from(Move move) {
    Objects.requireNonNull(move, "move must not be null");
    Class<? extends Piece> promotionType = move.promotionPiece() == null
        ? null
        : move.promotionPiece().getClass();
    return new MoveKey(move.color(), move.from(), move.to(), move.type(), promotionType);
  }
}
