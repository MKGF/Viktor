package com.desierto.book;

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

final class PolyglotKey {
  private PolyglotKey() {}

  static long of(Board board) {
    long key = 0;
    for (Piece piece : board.getPieces()) {
      int type;
      if (piece instanceof Pawn) type = 0;
      else if (piece instanceof Knight) type = 1;
      else if (piece instanceof Bishop) type = 2;
      else if (piece instanceof Rook) type = 3;
      else if (piece instanceof Queen) type = 4;
      else if (piece instanceof King) type = 5;
      else throw new IllegalArgumentException("Unknown piece: " + piece.getClass().getName());
      int square = piece.getCell().row() * 8 + piece.getCell().column();
      int color = piece.getColor() == Color.WHITE ? 1 : 0;
      key ^= PolyglotRandom.VALUES[(type * 2 + color) * 64 + square];
    }
    if (board.canWhiteCastleKingside()) key ^= PolyglotRandom.VALUES[768];
    if (board.canWhiteCastleQueenside()) key ^= PolyglotRandom.VALUES[769];
    if (board.canBlackCastleKingside()) key ^= PolyglotRandom.VALUES[770];
    if (board.canBlackCastleQueenside()) key ^= PolyglotRandom.VALUES[771];
    Cell target = board.getEnPassantTarget().orElse(null);
    if (target != null) {
      int row = target.row() + (board.getSideToMove() == Color.WHITE ? -1 : 1);
      for (int column : new int[] {target.column() - 1, target.column() + 1}) {
        if (row >= 0 && row < 8 && column >= 0 && column < 8) {
          Piece piece = board.getPieceAt(Cell.of(row, column)).orElse(null);
          if (piece instanceof Pawn && piece.getColor() == board.getSideToMove()) {
            key ^= PolyglotRandom.VALUES[772 + target.column()];
            break;
          }
        }
      }
    }
    if (board.getSideToMove() == Color.WHITE) key ^= PolyglotRandom.VALUES[780];
    return key;
  }
}
