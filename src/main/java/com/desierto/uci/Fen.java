package com.desierto.uci;

import com.desierto.domain.Board;
import com.desierto.domain.BoardFactory;
import com.desierto.domain.Cell;
import com.desierto.domain.Color;
import com.desierto.domain.Piece;
import com.desierto.domain.pieces.Bishop;
import com.desierto.domain.pieces.King;
import com.desierto.domain.pieces.Knight;
import com.desierto.domain.pieces.Pawn;
import com.desierto.domain.pieces.Queen;
import com.desierto.domain.pieces.Rook;

final class Fen {
  private Fen() {
  }

  static Board parse(String fen) {
    String[] fields = fen.trim().split("\\s+");
    if (fields.length != 6) {
      throw new IllegalArgumentException("FEN must contain six fields");
    }
    Color side = switch (fields[1]) {
      case "w" -> Color.WHITE;
      case "b" -> Color.BLACK;
      default -> throw new IllegalArgumentException("Invalid FEN side to move");
    };
    String rights = fields[2];
    if (!rights.equals("-") && !rights.matches("(?!.*(.).*\\1)[KQkq]+")) {
      throw new IllegalArgumentException("Invalid FEN castling rights");
    }
    Cell enPassant = fields[3].equals("-") ? null : Cell.fromAlgebraic(fields[3]);
    if (enPassant != null && (fields[3].charAt(0) < 'a' || fields[3].charAt(0) > 'h'
        || enPassant.row() != (side == Color.WHITE ? 5 : 2))) {
      throw new IllegalArgumentException("Invalid FEN en passant target");
    }
    int halfmove = number(fields[4], false);
    int fullmove = number(fields[5], true);
    BoardFactory.PositionBuilder builder = BoardFactory.position().sideToMove(side)
        .castlingRights(rights.contains("K"), rights.contains("Q"),
            rights.contains("k"), rights.contains("q"))
        .enPassantTarget(enPassant).halfmoveClock(halfmove).fullmoveNumber(fullmove);
    String[] ranks = fields[0].split("/", -1);
    if (ranks.length != 8) {
      throw new IllegalArgumentException("FEN must contain eight ranks");
    }
    int whiteKings = 0;
    int blackKings = 0;
    for (int rank = 0; rank < 8; rank++) {
      int column = 0;
      for (char symbol : ranks[rank].toCharArray()) {
        if (symbol >= '1' && symbol <= '8') {
          column += symbol - '0';
          continue;
        }
        if (column >= 8) {
          throw new IllegalArgumentException("Invalid FEN rank width");
        }
        Cell cell = Cell.of(7 - rank, column++);
        Color color = Character.isUpperCase(symbol) ? Color.WHITE : Color.BLACK;
        Piece piece = switch (Character.toLowerCase(symbol)) {
          case 'k' -> new King(color, cell);
          case 'q' -> new Queen(color, cell);
          case 'r' -> new Rook(color, cell);
          case 'b' -> new Bishop(color, cell);
          case 'n' -> new Knight(color, cell);
          case 'p' -> {
            if (cell.row() == 0 || cell.row() == 7) {
              throw new IllegalArgumentException("FEN pawn on a promotion rank");
            }
            yield new Pawn(color, cell);
          }
          default -> throw new IllegalArgumentException("Invalid FEN piece");
        };
        if (piece instanceof King) {
          if (color == Color.WHITE) {
            whiteKings++;
          } else {
            blackKings++;
          }
        }
        builder.place(piece, cell);
      }
      if (column != 8) {
        throw new IllegalArgumentException("Invalid FEN rank width");
      }
    }
    if (whiteKings != 1 || blackKings != 1) {
      throw new IllegalArgumentException("FEN must contain one king of each color");
    }
    Board board = builder.build();
    checkCastling(board, rights, 'K', Color.WHITE, Cell.E1, Cell.H1);
    checkCastling(board, rights, 'Q', Color.WHITE, Cell.E1, Cell.A1);
    checkCastling(board, rights, 'k', Color.BLACK, Cell.E8, Cell.H8);
    checkCastling(board, rights, 'q', Color.BLACK, Cell.E8, Cell.A8);
    if (enPassant != null) {
      int pawnRow = enPassant.row() + (side == Color.WHITE ? -1 : 1);
      int originRow = enPassant.row() + (side == Color.WHITE ? 1 : -1);
      Piece pawn = board.getPieceAt(Cell.of(pawnRow, enPassant.column())).orElse(null);
      if (board.isOccupied(enPassant) || board.isOccupied(Cell.of(originRow, enPassant.column()))
          || !(pawn instanceof Pawn) || pawn.getColor() == side) {
        throw new IllegalArgumentException("Invalid FEN en passant position");
      }
    }
    return board;
  }

  private static void checkCastling(Board board, String rights, char right, Color color,
      Cell kingCell, Cell rookCell) {
    if (!rights.contains(String.valueOf(right))) {
      return;
    }
    Piece king = board.getPieceAt(kingCell).orElse(null);
    Piece rook = board.getPieceAt(rookCell).orElse(null);
    if (!(king instanceof King) || king.getColor() != color
        || !(rook instanceof Rook) || rook.getColor() != color) {
      throw new IllegalArgumentException("FEN castling right has no matching king and rook");
    }
  }

  private static int number(String value, boolean positive) {
    if (!value.matches("[0-9]+")) {
      throw new IllegalArgumentException("Invalid FEN move counter");
    }
    try {
      int number = Integer.parseInt(value);
      if (positive && number == 0) {
        throw new IllegalArgumentException("FEN fullmove number must be positive");
      }
      return number;
    } catch (NumberFormatException ex) {
      throw new IllegalArgumentException("FEN move counter is too large", ex);
    }
  }
}
