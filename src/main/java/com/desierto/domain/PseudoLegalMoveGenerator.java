package com.desierto.domain;

import com.desierto.domain.pieces.Bishop;
import com.desierto.domain.pieces.King;
import com.desierto.domain.pieces.Knight;
import com.desierto.domain.pieces.Pawn;
import com.desierto.domain.pieces.Queen;
import com.desierto.domain.pieces.Rook;
import java.util.ArrayList;
import java.util.List;

final class PseudoLegalMoveGenerator {

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
  private static final int[][] KING_DIRECTIONS = {
      {1, 1}, {1, 0}, {1, -1}, {0, 1}, {0, -1}, {-1, 1}, {-1, 0}, {-1, -1}
  };
  private static final int[][] KNIGHT_OFFSETS = {
      {2, 1}, {2, -1}, {-2, 1}, {-2, -1},
      {1, 2}, {1, -2}, {-1, 2}, {-1, -2}
  };

  private final AttackDetector attackDetector;

  PseudoLegalMoveGenerator(AttackDetector attackDetector) {
    this.attackDetector = attackDetector;
  }

  List<Move> generate(Board board, Piece piece) {
    List<Move> moves = new ArrayList<>();
    if (piece instanceof Pawn pawn) {
      generatePawnMoves(board, pawn, moves);
    } else if (piece instanceof Knight knight) {
      addJumpMoves(board, knight, moves, KNIGHT_OFFSETS);
    } else if (piece instanceof Bishop bishop) {
      generateSlidingMoves(board, bishop, moves, BISHOP_DIRECTIONS);
    } else if (piece instanceof Rook rook) {
      generateSlidingMoves(board, rook, moves, ROOK_DIRECTIONS);
    } else if (piece instanceof Queen queen) {
      generateSlidingMoves(board, queen, moves, QUEEN_DIRECTIONS);
    } else if (piece instanceof King king) {
      addJumpMoves(board, king, moves, KING_DIRECTIONS);
      addCastlingMoves(board, king, moves);
    } else {
      throw new IllegalArgumentException("Unknown piece type");
    }
    return List.copyOf(moves);
  }

  private void generatePawnMoves(Board board, Pawn pawn, List<Move> moves) {
    Cell from = pawn.getCell();
    int direction = pawn.getColor() == Color.WHITE ? 1 : -1;
    int startRow = pawn.getColor() == Color.WHITE ? 1 : 6;
    int promotionRow = pawn.getColor() == Color.WHITE ? 7 : 0;

    addPawnAdvance(board, pawn, moves, from.row() + direction, from.column(), promotionRow);
    if (from.row() == startRow) {
      int oneStepRow = from.row() + direction;
      int twoStepRow = from.row() + direction * 2;
      if (isEmpty(board, oneStepRow, from.column()) && isEmpty(board, twoStepRow, from.column())) {
        moves.add(new Move(pawn.getColor(), pawn, from, Cell.of(twoStepRow, from.column()), null,
            MoveType.NORMAL, null));
      }
    }
    addPawnCapture(board, pawn, moves, from.row() + direction, from.column() - 1, promotionRow);
    addPawnCapture(board, pawn, moves, from.row() + direction, from.column() + 1, promotionRow);
    addEnPassant(board, pawn, moves, from.row() + direction, from.column() - 1);
    addEnPassant(board, pawn, moves, from.row() + direction, from.column() + 1);
  }

  private void addPawnAdvance(Board board, Pawn pawn, List<Move> moves, int row, int column,
      int promotionRow) {
    if (!isEmpty(board, row, column)) {
      return;
    }
    Cell to = Cell.of(row, column);
    if (row == promotionRow) {
      addPromotionMoves(pawn, to, null, MoveType.PROMOTION, moves);
    } else {
      moves.add(new Move(pawn.getColor(), pawn, pawn.getCell(), to, null, MoveType.NORMAL, null));
    }
  }

  private void addPawnCapture(Board board, Pawn pawn, List<Move> moves, int row, int column,
      int promotionRow) {
    if (!isInside(row, column)) {
      return;
    }
    Cell to = Cell.of(row, column);
    Piece target = board.getPieceAt(to).orElse(null);
    if (!isCapturable(target, pawn.getColor())) {
      return;
    }
    if (row == promotionRow) {
      addPromotionMoves(pawn, to, target, MoveType.CAPTURE_PROMOTION, moves);
    } else {
      moves.add(new Move(pawn.getColor(), pawn, pawn.getCell(), to, null, MoveType.CAPTURE, target));
    }
  }

  private void addPromotionMoves(Pawn pawn, Cell to, Piece capturedPiece, MoveType moveType,
      List<Move> moves) {
    moves.add(new Move(pawn.getColor(), pawn, pawn.getCell(), to, new Queen(pawn.getColor(), to),
        moveType, capturedPiece));
    moves.add(new Move(pawn.getColor(), pawn, pawn.getCell(), to, new Rook(pawn.getColor(), to),
        moveType, capturedPiece));
    moves.add(new Move(pawn.getColor(), pawn, pawn.getCell(), to, new Bishop(pawn.getColor(), to),
        moveType, capturedPiece));
    moves.add(new Move(pawn.getColor(), pawn, pawn.getCell(), to, new Knight(pawn.getColor(), to),
        moveType, capturedPiece));
  }

  private void addEnPassant(Board board, Pawn pawn, List<Move> moves, int row, int column) {
    if (!isInside(row, column)) {
      return;
    }
    Cell to = Cell.of(row, column);
    Move lastMove = board.getLastMove().orElse(null);
    if (board.isOccupied(to)
        || !to.equals(board.getEnPassantTarget().orElse(null))
        || lastMove == null
        || !(lastMove.piece() instanceof Pawn)
        || lastMove.piece().getColor() == pawn.getColor()
        || Math.abs(lastMove.from().row() - lastMove.to().row()) != 2
        || lastMove.to().row() != pawn.getCell().row()
        || lastMove.to().column() != column) {
      return;
    }
    Piece captured = board.getPieceAt(Cell.of(pawn.getCell().row(), column)).orElse(null);
    if (captured instanceof Pawn && captured.getColor() != pawn.getColor()) {
      moves.add(new Move(pawn.getColor(), pawn, pawn.getCell(), to, null, MoveType.EN_PASSANT,
          captured));
    }
  }

  private void addJumpMoves(Board board, Piece piece, List<Move> moves, int[][] offsets) {
    Cell from = piece.getCell();
    for (int[] offset : offsets) {
      int row = from.row() + offset[0];
      int column = from.column() + offset[1];
      if (!isInside(row, column)) {
        continue;
      }
      Cell to = Cell.of(row, column);
      Piece target = board.getPieceAt(to).orElse(null);
      if (target == null) {
        moves.add(new Move(piece.getColor(), piece, from, to, null, MoveType.NORMAL, null));
      } else if (isCapturable(target, piece.getColor())) {
        moves.add(new Move(piece.getColor(), piece, from, to, null, MoveType.CAPTURE, target));
      }
    }
  }

  private void generateSlidingMoves(Board board, Piece piece, List<Move> moves, int[][] directions) {
    Cell from = piece.getCell();
    for (int[] direction : directions) {
      int row = from.row() + direction[0];
      int column = from.column() + direction[1];
      while (isInside(row, column)) {
        Cell to = Cell.of(row, column);
        Piece target = board.getPieceAt(to).orElse(null);
        if (target == null) {
          moves.add(new Move(piece.getColor(), piece, from, to, null, MoveType.NORMAL, null));
        } else {
          if (isCapturable(target, piece.getColor())) {
            moves.add(new Move(piece.getColor(), piece, from, to, null, MoveType.CAPTURE, target));
          }
          break;
        }
        row += direction[0];
        column += direction[1];
      }
    }
  }

  private void addCastlingMoves(Board board, King king, List<Move> moves) {
    Cell from = king.getCell();
    if (king.hasMoved() || attackDetector.isSquareAttacked(board, from, king.getColor().opposite())) {
      return;
    }
    if (king.getColor() == Color.WHITE && from.equals(Cell.E1)) {
      if (board.canWhiteCastleKingside()) {
        addKingsideCastle(board, king, moves, Cell.H1, Cell.F1, Cell.G1);
      }
      if (board.canWhiteCastleQueenside()) {
        addQueensideCastle(board, king, moves, Cell.A1, Cell.D1, Cell.C1, Cell.B1);
      }
    } else if (king.getColor() == Color.BLACK && from.equals(Cell.E8)) {
      if (board.canBlackCastleKingside()) {
        addKingsideCastle(board, king, moves, Cell.H8, Cell.F8, Cell.G8);
      }
      if (board.canBlackCastleQueenside()) {
        addQueensideCastle(board, king, moves, Cell.A8, Cell.D8, Cell.C8, Cell.B8);
      }
    }
  }

  private void addKingsideCastle(Board board, King king, List<Move> moves, Cell rookCell, Cell through,
      Cell to) {
    Piece rook = board.getPieceAt(rookCell).orElse(null);
    if (!(rook instanceof Rook)
        || rook.getColor() != king.getColor()
        || rook.hasMoved()
        || board.isOccupied(through)
        || board.isOccupied(to)
        || attackDetector.isSquareAttacked(board, through, king.getColor().opposite())
        || attackDetector.isSquareAttacked(board, to, king.getColor().opposite())) {
      return;
    }
    moves.add(new Move(king.getColor(), king, king.getCell(), to, null, MoveType.CASTLE_KINGSIDE, null));
  }

  private void addQueensideCastle(Board board, King king, List<Move> moves, Cell rookCell, Cell through,
      Cell to, Cell extraEmpty) {
    Piece rook = board.getPieceAt(rookCell).orElse(null);
    if (!(rook instanceof Rook)
        || rook.getColor() != king.getColor()
        || rook.hasMoved()
        || board.isOccupied(through)
        || board.isOccupied(to)
        || board.isOccupied(extraEmpty)
        || attackDetector.isSquareAttacked(board, through, king.getColor().opposite())
        || attackDetector.isSquareAttacked(board, to, king.getColor().opposite())) {
      return;
    }
    moves.add(new Move(king.getColor(), king, king.getCell(), to, null,
        MoveType.CASTLE_QUEENSIDE, null));
  }

  private boolean isCapturable(Piece target, Color moverColor) {
    return target != null && target.getColor() != moverColor && !(target instanceof King);
  }

  private boolean isEmpty(Board board, int row, int column) {
    return isInside(row, column) && !board.isOccupied(Cell.of(row, column));
  }

  private boolean isInside(int row, int column) {
    return row >= 0 && row < 8 && column >= 0 && column < 8;
  }
}
