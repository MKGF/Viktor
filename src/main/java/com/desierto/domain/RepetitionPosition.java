package com.desierto.domain;

import com.desierto.domain.pieces.Bishop;
import com.desierto.domain.pieces.King;
import com.desierto.domain.pieces.Knight;
import com.desierto.domain.pieces.Pawn;
import com.desierto.domain.pieces.Queen;
import com.desierto.domain.pieces.Rook;

record RepetitionPosition(String value) {

  static RepetitionPosition of(Board board) {
    char[] squares = new char[64];
    java.util.Arrays.fill(squares, '.');
    for (Piece piece : board.getPieces()) {
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
      Cell cell = piece.getCell();
      squares[cell.row() * 8 + cell.column()] =
          piece.getColor() == Color.WHITE ? symbol : Character.toLowerCase(symbol);
    }
    StringBuilder key = new StringBuilder(70).append(squares);
    key.append(board.getSideToMove() == Color.WHITE ? 'w' : 'b');
    key.append(castlingAvailable(board, Color.WHITE, Cell.H1, board.canWhiteCastleKingside()) ? 'K' : '-');
    key.append(castlingAvailable(board, Color.WHITE, Cell.A1, board.canWhiteCastleQueenside()) ? 'Q' : '-');
    key.append(castlingAvailable(board, Color.BLACK, Cell.H8, board.canBlackCastleKingside()) ? 'k' : '-');
    key.append(castlingAvailable(board, Color.BLACK, Cell.A8, board.canBlackCastleQueenside()) ? 'q' : '-');
    Cell enPassant = legalEnPassantTarget(board);
    key.append(enPassant == null ? "-" : enPassant.toAlgebraic());
    return new RepetitionPosition(key.toString());
  }

  private static boolean castlingAvailable(Board board, Color color, Cell rookCell, boolean right) {
    Cell kingCell = color == Color.WHITE ? Cell.E1 : Cell.E8;
    Piece king = board.getPieceAt(kingCell).orElse(null);
    Piece rook = board.getPieceAt(rookCell).orElse(null);
    return right && king instanceof King && king.getColor() == color && !king.hasMoved()
        && rook instanceof Rook && rook.getColor() == color && !rook.hasMoved();
  }

  private static Cell legalEnPassantTarget(Board board) {
    Cell target = board.getEnPassantTarget().orElse(null);
    if (target == null) {
      return null;
    }
    int fromRow = target.row() + (board.getSideToMove() == Color.WHITE ? -1 : 1);
    if (fromRow < 0 || fromRow >= 8) {
      return null;
    }
    MoveGenerator generator = new MoveGenerator();
    for (int offset : new int[] {-1, 1}) {
      int column = target.column() + offset;
      if (column < 0 || column >= 8) {
        continue;
      }
      Cell from = Cell.of(fromRow, column);
      Piece piece = board.getPieceAt(from).orElse(null);
      if (piece instanceof Pawn && piece.getColor() == board.getSideToMove()
          && generator.generateLegalMoves(board, from).stream()
          .anyMatch(move -> move.type() == MoveType.EN_PASSANT && move.to().equals(target))) {
        return target;
      }
    }
    return null;
  }

  long fingerprint() {
    long hash = 0xcbf29ce484222325L;
    for (int i = 0; i < value.length(); i++) {
      hash = (hash ^ value.charAt(i)) * 0x100000001b3L;
    }
    return hash;
  }
}
