package com.desierto.domain;

import com.desierto.domain.pieces.Bishop;
import com.desierto.domain.pieces.King;
import com.desierto.domain.pieces.Knight;
import com.desierto.domain.pieces.Pawn;
import com.desierto.domain.pieces.Queen;
import com.desierto.domain.pieces.Rook;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MoveGenerator {

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

  private static final int[][] KING_ATTACK_DIRECTIONS = {
      {1, 1}, {1, 0}, {1, -1}, {0, 1}, {0, -1}, {-1, 1}, {-1, 0}, {-1, -1}
  };

  public List<Move> generateMoves(Board board, Cell from) {
    return generateLegalMoves(board, from);
  }

  public List<Move> generateLegalMoves(Board board, Cell from) {
    Objects.requireNonNull(board, "board must not be null");
    Objects.requireNonNull(from, "from must not be null");

    Piece piece = board.getPieceAt(from)
        .orElseThrow(() -> new IllegalStateException("No piece on square " + from.toAlgebraic()));

    List<Move> moves = new ArrayList<>();
    if (piece instanceof Pawn pawn) {
      generatePawnMoves(board, pawn, moves);
    } else if (piece instanceof Knight knight) {
      generateKnightMoves(board, knight, moves);
    } else if (piece instanceof Bishop bishop) {
      generateBishopMoves(board, bishop, moves);
    } else if (piece instanceof Rook rook) {
      generateRookMoves(board, rook, moves);
    } else if (piece instanceof Queen queen) {
      generateQueenMoves(board, queen, moves);
    } else if (piece instanceof King king) {
      generateKingMoves(board, king, moves);
    } else {
      throw new IllegalArgumentException("Unknown piece type");
    }
    return filterLegalMoves(board, moves, piece.getColor());
  }

  public List<Move> generateMoves(Board board, Color color) {
    return generateLegalMoves(board, color);
  }

  public List<Move> generateLegalMoves(Board board, Color color) {
    Objects.requireNonNull(board, "board must not be null");
    Objects.requireNonNull(color, "color must not be null");

    List<Move> moves = new ArrayList<>();
    for (Piece piece : board.getPieces(color)) {
      moves.addAll(generateLegalMoves(board, piece.getCell()));
    }
    return List.copyOf(moves);
  }

  public boolean isKingInCheck(Board board, Color color) {
    Objects.requireNonNull(board, "board must not be null");
    Objects.requireNonNull(color, "color must not be null");

    Cell kingCell = findKing(board, color);
    if (kingCell == null) {
      return false;
    }
    return isSquareAttacked(board, kingCell, opposite(color));
  }

  public boolean isCheckmate(Board board, Color color) {
    Objects.requireNonNull(board, "board must not be null");
    Objects.requireNonNull(color, "color must not be null");
    return isKingInCheck(board, color) && generateLegalMoves(board, color).isEmpty();
  }

  public boolean isStalemate(Board board, Color color) {
    Objects.requireNonNull(board, "board must not be null");
    Objects.requireNonNull(color, "color must not be null");
    return !isKingInCheck(board, color) && generateLegalMoves(board, color).isEmpty();
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
      if (isInside(oneStepRow, from.column())
          && isInside(twoStepRow, from.column())
          && board.getPieceAt(Cell.of(oneStepRow, from.column())).isEmpty()
          && board.getPieceAt(Cell.of(twoStepRow, from.column())).isEmpty()) {
        moves.add(new Move(pawn.getColor(), pawn, from, Cell.of(twoStepRow, from.column()), null, MoveType.NORMAL, null));
      }
    }

    addPawnCapture(board, pawn, moves, from.row() + direction, from.column() - 1, promotionRow);
    addPawnCapture(board, pawn, moves, from.row() + direction, from.column() + 1, promotionRow);
    addEnPassant(board, pawn, moves, from.row() + direction, from.column() - 1);
    addEnPassant(board, pawn, moves, from.row() + direction, from.column() + 1);
  }

  private void addPawnAdvance(Board board, Pawn pawn, List<Move> moves, int row, int column, int promotionRow) {
    if (!isInside(row, column)) {
      return;
    }
    Cell to = Cell.of(row, column);
    if (board.getPieceAt(to).isPresent()) {
      return;
    }
    if (row == promotionRow) {
      addPromotionMoves(pawn, pawn.getCell(), to, null, MoveType.PROMOTION, moves);
      return;
    }
    moves.add(new Move(pawn.getColor(), pawn, pawn.getCell(), to, null, MoveType.NORMAL, null));
  }

  private void addPawnCapture(Board board, Pawn pawn, List<Move> moves, int row, int column, int promotionRow) {
    if (!isInside(row, column)) {
      return;
    }
    Cell to = Cell.of(row, column);
    Piece target = board.getPieceAt(to).orElse(null);
    if (target == null || target.getColor() == pawn.getColor()) {
      return;
    }
    if (row == promotionRow) {
      addPromotionMoves(pawn, pawn.getCell(), to, target, MoveType.CAPTURE_PROMOTION, moves);
      return;
    }
    moves.add(new Move(pawn.getColor(), pawn, pawn.getCell(), to, null, MoveType.CAPTURE, target));
  }

  private void addPromotionMoves(Pawn pawn, Cell from, Cell to, Piece capturedPiece, MoveType moveType, List<Move> moves) {
    moves.add(new Move(pawn.getColor(), pawn, from, to, new Queen(pawn.getColor(), to), moveType, capturedPiece));
    moves.add(new Move(pawn.getColor(), pawn, from, to, new Rook(pawn.getColor(), to), moveType, capturedPiece));
    moves.add(new Move(pawn.getColor(), pawn, from, to, new Bishop(pawn.getColor(), to), moveType, capturedPiece));
    moves.add(new Move(pawn.getColor(), pawn, from, to, new Knight(pawn.getColor(), to), moveType, capturedPiece));
  }

  private void addEnPassant(Board board, Pawn pawn, List<Move> moves, int row, int column) {
    if (!isInside(row, column)) {
      return;
    }
    Cell to = Cell.of(row, column);
    if (board.getPieceAt(to).isPresent()) {
      return;
    }
    Cell enPassantTarget = board.getEnPassantTarget().orElse(null);
    if (enPassantTarget == null || !enPassantTarget.equals(to)) {
      return;
    }
    Move lastMove = board.getLastMove().orElse(null);
    if (lastMove == null || !(lastMove.piece() instanceof Pawn lastPawn)) {
      return;
    }
    if (lastPawn.getColor() == pawn.getColor()) {
      return;
    }
    if (Math.abs(lastMove.from().row() - lastMove.to().row()) != 2) {
      return;
    }
    if (lastMove.to().row() != pawn.getCell().row() || lastMove.to().column() != column) {
      return;
    }
    moves.add(new Move(pawn.getColor(), pawn, pawn.getCell(), to, null, MoveType.EN_PASSANT, lastPawn));
  }

  private void generateKnightMoves(Board board, Knight knight, List<Move> moves) {
    int[][] offsets = {
        {2, 1}, {2, -1}, {-2, 1}, {-2, -1},
        {1, 2}, {1, -2}, {-1, 2}, {-1, -2}
    };
    addJumpMoves(board, knight, moves, offsets);
  }

  private void generateBishopMoves(Board board, Bishop bishop, List<Move> moves) {
    generateSlidingMoves(board, bishop, moves, BISHOP_DIRECTIONS);
  }

  private void generateRookMoves(Board board, Rook rook, List<Move> moves) {
    generateSlidingMoves(board, rook, moves, ROOK_DIRECTIONS);
  }

  private void generateQueenMoves(Board board, Queen queen, List<Move> moves) {
    generateSlidingMoves(board, queen, moves, QUEEN_DIRECTIONS);
  }

  private void generateKingMoves(Board board, King king, List<Move> moves) {
    addJumpMoves(board, king, moves, KING_ATTACK_DIRECTIONS);
    addCastlingMoves(board, king, moves);
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
      } else if (target.getColor() != piece.getColor()) {
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
          if (target.getColor() != piece.getColor()) {
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
    if (king.hasMoved() || isSquareAttacked(board, from, opposite(king.getColor()))) {
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

  private void addKingsideCastle(Board board, King king, List<Move> moves, Cell rookCell, Cell through, Cell to) {
    Piece rook = board.getPieceAt(rookCell).orElse(null);
    if (!(rook instanceof Rook) || rook.getColor() != king.getColor() || rook.hasMoved()) {
      return;
    }
    if (board.getPieceAt(through).isPresent() || board.getPieceAt(to).isPresent()) {
      return;
    }
    if (isSquareAttacked(board, through, opposite(king.getColor()))
        || isSquareAttacked(board, to, opposite(king.getColor()))) {
      return;
    }
    moves.add(new Move(king.getColor(), king, king.getCell(), to, null, MoveType.CASTLE_KINGSIDE, null));
  }

  private void addQueensideCastle(Board board, King king, List<Move> moves, Cell rookCell, Cell through, Cell to, Cell extraEmpty) {
    Piece rook = board.getPieceAt(rookCell).orElse(null);
    if (!(rook instanceof Rook) || rook.getColor() != king.getColor() || rook.hasMoved()) {
      return;
    }
    if (board.getPieceAt(through).isPresent()
        || board.getPieceAt(to).isPresent()
        || board.getPieceAt(extraEmpty).isPresent()) {
      return;
    }
    if (isSquareAttacked(board, through, opposite(king.getColor()))
        || isSquareAttacked(board, to, opposite(king.getColor()))) {
      return;
    }
    moves.add(new Move(king.getColor(), king, king.getCell(), to, null, MoveType.CASTLE_QUEENSIDE, null));
  }

  private boolean isSquareAttacked(Board board, Cell square, Color attackerColor) {
    for (Piece piece : board.getPieces(attackerColor)) {
      if (pieceAttacksSquare(board, piece, square)) {
        return true;
      }
    }
    return false;
  }

  private boolean pieceAttacksSquare(Board board, Piece piece, Cell target) {
    if (piece instanceof Pawn pawn) {
      int direction = pawn.getColor() == Color.WHITE ? 1 : -1;
      return target.row() == pawn.getCell().row() + direction
          && (target.column() == pawn.getCell().column() - 1 || target.column() == pawn.getCell().column() + 1);
    }
    if (piece instanceof Knight knight) {
      int rowDiff = Math.abs(target.row() - knight.getCell().row());
      int columnDiff = Math.abs(target.column() - knight.getCell().column());
      return (rowDiff == 2 && columnDiff == 1) || (rowDiff == 1 && columnDiff == 2);
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
      int rowDiff = Math.abs(target.row() - king.getCell().row());
      int columnDiff = Math.abs(target.column() - king.getCell().column());
      return rowDiff <= 1 && columnDiff <= 1;
    }
    return false;
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
        if (board.getPieceAt(current).isPresent()) {
          break;
        }
        row += direction[0];
        column += direction[1];
      }
    }
    return false;
  }

  private Color opposite(Color color) {
    return color == Color.WHITE ? Color.BLACK : Color.WHITE;
  }

  private List<Move> filterLegalMoves(Board board, List<Move> pseudoLegalMoves, Color moverColor) {
    List<Move> legalMoves = new ArrayList<>();
    for (Move move : pseudoLegalMoves) {
      Board copy = board.copy();
      copy.makeMove(move);
      if (!isKingInCheck(copy, moverColor)) {
        legalMoves.add(move);
      }
    }
    return List.copyOf(legalMoves);
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
