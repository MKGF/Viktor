package com.desierto.search;

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
import java.util.Objects;
import java.util.SplittableRandom;

final class PositionHash {

  private static final long[][][][] PIECES = new long[2][6][64][2];
  private static final long[] CASTLING = new long[4];
  private static final long[] EN_PASSANT = new long[64];
  private static final long[] HALF_MOVE_CLOCK = new long[151];
  private static final long BLACK_TO_MOVE;

  static {
    SplittableRandom random = new SplittableRandom(0x5629c75ba3d14f83L);
    for (long[][][] color : PIECES) {
      for (long[][] type : color) {
        for (long[] square : type) {
          for (int moved = 0; moved < square.length; moved++) {
            square[moved] = random.nextLong();
          }
        }
      }
    }
    for (int i = 0; i < CASTLING.length; i++) {
      CASTLING[i] = random.nextLong();
    }
    for (int i = 0; i < EN_PASSANT.length; i++) {
      EN_PASSANT[i] = random.nextLong();
    }
    for (int i = 0; i < HALF_MOVE_CLOCK.length; i++) {
      HALF_MOVE_CLOCK[i] = random.nextLong();
    }
    BLACK_TO_MOVE = random.nextLong();
  }

  private PositionHash() {
  }

  static long of(Board board) {
    Objects.requireNonNull(board, "board must not be null");
    long hash = 0;
    for (Piece piece : board.getPieces()) {
      int color = piece.getColor() == Color.WHITE ? 0 : 1;
      hash ^= PIECES[color][pieceType(piece)][square(piece.getCell())][piece.hasMoved() ? 1 : 0];
    }
    if (board.getSideToMove() == Color.BLACK) {
      hash ^= BLACK_TO_MOVE;
    }
    if (board.canWhiteCastleKingside()) {
      hash ^= CASTLING[0];
    }
    if (board.canWhiteCastleQueenside()) {
      hash ^= CASTLING[1];
    }
    if (board.canBlackCastleKingside()) {
      hash ^= CASTLING[2];
    }
    if (board.canBlackCastleQueenside()) {
      hash ^= CASTLING[3];
    }
    if (board.getEnPassantTarget().isPresent()) {
      hash ^= EN_PASSANT[square(board.getEnPassantTarget().orElseThrow())];
    }
    hash ^= HALF_MOVE_CLOCK[Math.min(board.getHalfmoveClock(), 150)];
    hash ^= board.repetitionHistoryFingerprint();
    return hash;
  }

  private static int square(Cell cell) {
    return cell.row() * 8 + cell.column();
  }

  private static int pieceType(Piece piece) {
    if (piece instanceof Pawn) {
      return 0;
    }
    if (piece instanceof Knight) {
      return 1;
    }
    if (piece instanceof Bishop) {
      return 2;
    }
    if (piece instanceof Rook) {
      return 3;
    }
    if (piece instanceof Queen) {
      return 4;
    }
    if (piece instanceof King) {
      return 5;
    }
    throw new IllegalArgumentException("Unknown piece type");
  }
}
