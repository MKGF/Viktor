package com.desierto.domain;

import com.desierto.domain.pieces.Bishop;
import com.desierto.domain.pieces.King;
import com.desierto.domain.pieces.Knight;
import com.desierto.domain.pieces.Pawn;
import com.desierto.domain.pieces.Queen;
import com.desierto.domain.pieces.Rook;
import java.util.Objects;

final class AttackDetector {

  private static final int[][] BISHOP_DIRECTIONS = {
      {1, 1}, {1, -1}, {-1, 1}, {-1, -1}
  };
  private static final int[][] ROOK_DIRECTIONS = {
      {1, 0}, {-1, 0}, {0, 1}, {0, -1}
  };
  private static final int[][] QUEEN_DIRECTIONS = {
      {1, 1}, {1, -1}, {-1, 1}, {-1, -1},
      {1, 0}, {-1, 0}, {0, 1}, {0, -1}
  };

  boolean isKingInCheck(Board board, Color color) {
    Objects.requireNonNull(board, "board must not be null");
    Objects.requireNonNull(color, "color must not be null");

    Cell kingCell = findKing(board, color);
    return kingCell != null && isSquareAttacked(board, kingCell, color.opposite());
  }

  boolean isSquareAttacked(Board board, Cell square, Color attackerColor) {
    for (Piece piece : board.getPieces(attackerColor)) {
      if (attacks(board, piece, square)) {
        return true;
      }
    }
    return false;
  }

  private boolean attacks(Board board, Piece piece, Cell target) {
    if (piece instanceof Pawn pawn) {
      int direction = pawn.getColor() == Color.WHITE ? 1 : -1;
      return target.row() == pawn.getCell().row() + direction
          && Math.abs(target.column() - pawn.getCell().column()) == 1;
    }
    if (piece instanceof Knight knight) {
      int rowDifference = Math.abs(target.row() - knight.getCell().row());
      int columnDifference = Math.abs(target.column() - knight.getCell().column());
      return (rowDifference == 2 && columnDifference == 1)
          || (rowDifference == 1 && columnDifference == 2);
    }
    if (piece instanceof Bishop bishop) {
      return attacksAlongRay(board, bishop.getCell(), target, BISHOP_DIRECTIONS);
    }
    if (piece instanceof Rook rook) {
      return attacksAlongRay(board, rook.getCell(), target, ROOK_DIRECTIONS);
    }
    if (piece instanceof Queen queen) {
      return attacksAlongRay(board, queen.getCell(), target, QUEEN_DIRECTIONS);
    }
    if (piece instanceof King king) {
      int rowDifference = Math.abs(target.row() - king.getCell().row());
      int columnDifference = Math.abs(target.column() - king.getCell().column());
      return rowDifference <= 1 && columnDifference <= 1;
    }
    throw new IllegalArgumentException("Unknown piece type");
  }

  private boolean attacksAlongRay(Board board, Cell from, Cell target, int[][] directions) {
    for (int[] direction : directions) {
      int row = from.row() + direction[0];
      int column = from.column() + direction[1];
      while (isInside(row, column)) {
        Cell current = Cell.of(row, column);
        if (current.equals(target)) {
          return true;
        }
        if (board.isOccupied(current)) {
          break;
        }
        row += direction[0];
        column += direction[1];
      }
    }
    return false;
  }

  private Cell findKing(Board board, Color color) {
    for (Piece piece : board.getPieces(color)) {
      if (piece instanceof King) {
        return piece.getCell();
      }
    }
    return null;
  }

  private boolean isInside(int row, int column) {
    return row >= 0 && row < 8 && column >= 0 && column < 8;
  }
}
