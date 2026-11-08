package com.desierto.search.evaluation;

import com.desierto.domain.Board;
import com.desierto.domain.Cell;
import com.desierto.domain.Color;
import com.desierto.domain.MoveGenerator;
import com.desierto.domain.Piece;
import com.desierto.domain.pieces.Bishop;
import com.desierto.domain.pieces.Knight;
import com.desierto.domain.pieces.Pawn;
import com.desierto.domain.pieces.Queen;
import com.desierto.domain.pieces.Rook;
import java.util.ArrayList;
import java.util.List;

final class PositionContext {
  private static final int UNDEVELOPED_MINOR_PENALTY = 12;
  private final Board board;
  private final List<Piece> pieces;
  private final List<Piece> white = new ArrayList<>();
  private final List<Piece> black = new ArrayList<>();
  private final int[] pawnFiles = new int[2];
  private final MoveGenerator attacks = new MoveGenerator();
  private final int phase;
  private final int tableWeight;

  PositionContext(Board board) {
    this.board = board;
    pieces = board.getPieces();
    int materialPhase = 0;
    int undeveloped = 0;
    for (Piece piece : pieces) {
      (piece.getColor() == Color.WHITE ? white : black).add(piece);
      if (piece instanceof Pawn) {
        pawnFiles[colorIndex(piece.getColor())] |= 1 << piece.getCell().column();
      } else if (piece instanceof Queen) {
        materialPhase += 4;
      } else if (piece instanceof Rook) {
        materialPhase += 2;
      } else if (piece instanceof Knight || piece instanceof Bishop) {
        materialPhase++;
        if (isUndevelopedMinor(piece)) {
          undeveloped++;
        }
      }
    }
    phase = Math.min(materialPhase, PositionalEvaluator.MAX_PHASE);
    // At most eight starting minor squares count, including after promotions.
    tableWeight = 8 + 3 * Math.min(undeveloped, 8);
  }

  List<Piece> pieces() {
    return pieces;
  }

  List<Piece> pieces(Color color) {
    return color == Color.WHITE ? white : black;
  }

  Board board() {
    return board;
  }

  int phase() {
    return phase;
  }

  int base(Piece piece) {
    return PositionalEvaluator.tableValue(piece) * tableWeight * PositionalEvaluator.MAX_PHASE;
  }

  int undevelopedPenalty(Piece piece) {
    return isUndevelopedMinor(piece)
        ? -UNDEVELOPED_MINOR_PENALTY * phase * PositionalEvaluator.TABLE_SCALE : 0;
  }

  boolean hasPawnOnFile(Color color, int file) {
    return (pawnFiles[colorIndex(color)] & (1 << file)) != 0;
  }

  boolean attacks(Piece piece, Cell square) {
    return attacks.attacksSquare(board, piece, square);
  }

  int pressurePenalty(Piece target) {
    for (Piece attacker : pieces(target.getColor().opposite())) {
      if (attacks(attacker, target.getCell())) {
        for (Piece defender : pieces(target.getColor())) {
          if (defender != target && attacks(defender, target.getCell())) return -2;
        }
        return -6;
      }
    }
    return 0;
  }

  private static boolean isUndevelopedMinor(Piece piece) {
    Cell cell = piece.getCell();
    int rank = piece.getColor() == Color.WHITE ? cell.row() : 7 - cell.row();
    int file = cell.column();
    return rank == 0 && (piece instanceof Knight && (file == 1 || file == 6)
        || piece instanceof Bishop && (file == 2 || file == 5));
  }

  private static int colorIndex(Color color) {
    return color == Color.WHITE ? 0 : 1;
  }
}
