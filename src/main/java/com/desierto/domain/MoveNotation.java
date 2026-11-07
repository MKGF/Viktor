package com.desierto.domain;

import com.desierto.domain.pieces.Bishop;
import com.desierto.domain.pieces.Knight;
import com.desierto.domain.pieces.Queen;
import com.desierto.domain.pieces.Rook;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class MoveNotation {

  private MoveNotation() {
  }

  public static boolean hasValidFormat(String input) {
    return input.matches("[a-h][1-8][a-h][1-8][qrbn]?");
  }

  public static Optional<Move> find(String input, List<Move> legalMoves) {
    Cell from = Cell.fromAlgebraic(input.substring(0, 2));
    Cell to = Cell.fromAlgebraic(input.substring(2, 4));
    char promotion = input.length() == 5 ? input.charAt(4) : '\0';
    return legalMoves.stream()
        .filter(move -> move.from().equals(from) && move.to().equals(to))
        .filter(move -> promotionSymbol(move.promotionPiece()) == promotion)
        .findFirst();
  }

  public static String format(Move move) {
    String result = (move.from().toAlgebraic() + move.to().toAlgebraic()).toLowerCase(Locale.ROOT);
    char promotion = promotionSymbol(move.promotionPiece());
    return promotion == '\0' ? result : result + promotion;
  }

  private static char promotionSymbol(Piece piece) {
    if (piece == null) {
      return '\0';
    }
    if (piece instanceof Queen) {
      return 'q';
    }
    if (piece instanceof Rook) {
      return 'r';
    }
    if (piece instanceof Bishop) {
      return 'b';
    }
    if (piece instanceof Knight) {
      return 'n';
    }
    throw new IllegalArgumentException("Unknown promotion piece");
  }
}
