package com.desierto.domain;

final class BoardRenderer {

  private BoardRenderer() {
  }

  static String render(Board board) {
    StringBuilder builder = new StringBuilder();
    for (int row = 7; row >= 0; row--) {
      builder.append(row + 1).append(' ');
      for (int column = 0; column < 8; column++) {
        Piece piece = board.getPieceAt(Cell.of(row, column)).orElse(null);
        builder.append(piece == null ? "." : piece.getClass().getSimpleName().charAt(0));
        if (column < 7) {
          builder.append(' ');
        }
      }
      builder.append(System.lineSeparator());
    }
    builder.append("  A B C D E F G H");
    return builder.toString();
  }
}
