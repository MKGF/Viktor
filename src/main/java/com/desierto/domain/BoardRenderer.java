package com.desierto.domain;

import com.desierto.domain.pieces.Bishop;
import com.desierto.domain.pieces.King;
import com.desierto.domain.pieces.Knight;
import com.desierto.domain.pieces.Pawn;
import com.desierto.domain.pieces.Queen;
import com.desierto.domain.pieces.Rook;

final class BoardRenderer {

  private BoardRenderer() {
  }

  static String render(Board board) {
    StringBuilder builder = new StringBuilder();
    for (int row = 7; row >= 0; row--) {
      builder.append(row + 1).append(' ');
      for (int column = 0; column < 8; column++) {
        Piece piece = board.getPieceAt(Cell.of(row, column)).orElse(null);
        builder.append(symbol(piece));
        if (column < 7) {
          builder.append(' ');
        }
      }
      builder.append(System.lineSeparator());
    }
    builder.append("  A B C D E F G H");
    return builder.toString();
  }

  private static char symbol(Piece piece) {
    if (piece == null) {
      return '.';
    }
    char symbol;
    if (piece instanceof King) {
      symbol = 'K';
    } else if (piece instanceof Queen) {
      symbol = 'Q';
    } else if (piece instanceof Rook) {
      symbol = 'R';
    } else if (piece instanceof Bishop) {
      symbol = 'B';
    } else if (piece instanceof Knight) {
      symbol = 'N';
    } else if (piece instanceof Pawn) {
      symbol = 'P';
    } else {
      throw new IllegalArgumentException("Unknown piece type");
    }
    return piece.getColor() == Color.WHITE ? symbol : Character.toLowerCase(symbol);
  }
}
