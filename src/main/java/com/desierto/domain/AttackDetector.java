package com.desierto.domain;

import com.desierto.domain.pieces.Bishop;
import com.desierto.domain.pieces.King;
import com.desierto.domain.pieces.Knight;
import com.desierto.domain.pieces.Pawn;
import com.desierto.domain.pieces.Queen;
import com.desierto.domain.pieces.Rook;
import java.util.Objects;

final class AttackDetector {

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

  boolean attacks(Board board, Piece piece, Cell target) {
    if (piece.getCell().equals(target)) {
      return false;
    }
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
      return attacksAlongRay(board, bishop.getCell(), target, true, false);
    }
    if (piece instanceof Rook rook) {
      return attacksAlongRay(board, rook.getCell(), target, false, true);
    }
    if (piece instanceof Queen queen) {
      return attacksAlongRay(board, queen.getCell(), target, true, true);
    }
    if (piece instanceof King king) {
      int rowDifference = Math.abs(target.row() - king.getCell().row());
      int columnDifference = Math.abs(target.column() - king.getCell().column());
      return rowDifference <= 1 && columnDifference <= 1;
    }
    throw new IllegalArgumentException("Unknown piece type");
  }

  private boolean attacksAlongRay(Board board, Cell from, Cell target,
      boolean diagonal, boolean straight) {
    int rows = target.row() - from.row();
    int columns = target.column() - from.column();
    if (!(diagonal && Math.abs(rows) == Math.abs(columns))
        && !(straight && (rows == 0 || columns == 0))) {
      return false;
    }
    int rowStep = Integer.signum(rows);
    int columnStep = Integer.signum(columns);
    int row = from.row() + rowStep;
    int column = from.column() + columnStep;
    while (row != target.row() || column != target.column()) {
      if (board.isOccupied(Cell.of(row, column))) {
        return false;
      }
      row += rowStep;
      column += columnStep;
    }
    return true;
  }

  private Cell findKing(Board board, Color color) {
    for (Piece piece : board.getPieces(color)) {
      if (piece instanceof King) {
        return piece.getCell();
      }
    }
    return null;
  }

}
