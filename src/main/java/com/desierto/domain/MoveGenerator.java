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
  private static final int[][] KING_DIRECTIONS = {
      {1, 1}, {1, 0}, {1, -1}, {0, 1}, {0, -1}, {-1, 1}, {-1, 0}, {-1, -1}
  };
  private static final int[][] KNIGHT_OFFSETS = {
      {2, 1}, {2, -1}, {-2, 1}, {-2, -1},
      {1, 2}, {1, -2}, {-1, 2}, {-1, -2}
  };

  private final AttackDetector attackDetector;

  public MoveGenerator() {
    attackDetector = new AttackDetector();
  }

  public List<Move> generateMoves(Board board, Cell from) {
    return generateLegalMoves(board, from);
  }

  public List<Move> generateLegalMoves(Board board, Cell from) {
    Objects.requireNonNull(board, "board must not be null");
    Objects.requireNonNull(from, "from must not be null");

    Piece piece = board.getPieceAt(from)
        .orElseThrow(() -> new IllegalStateException("No piece on square " + from.toAlgebraic()));
    return filterMovesThatLeaveKingInCheck(board, generateCandidates(board, piece),
        piece.getColor());
  }

  public List<Move> generateMoves(Board board, Color color) {
    return generateLegalMoves(board, color);
  }

  public List<LegalMove> generateLegalMoveApplications(Board board, Color color) {
    return generateVerifiedMoveApplications(board, color);
  }

  private List<LegalMove> generateVerifiedMoveApplications(Board board, Color color) {
    Objects.requireNonNull(board, "board must not be null");
    Objects.requireNonNull(color, "color must not be null");
    if (color != board.getSideToMove()) {
      throw new IllegalArgumentException("Legal moves can only be generated for the side to move");
    }
    VerifiedMoves verified = new VerifiedMoves(board, generateLegalMovesForColor(board, color));
    List<LegalMove> legalMoves = new ArrayList<>();
    for (int i = 0; i < verified.moves.size(); i++) {
      legalMoves.add(new LegalMove(verified, i));
    }
    return List.copyOf(legalMoves);
  }

  public LegalMove requireLegalMove(Board board, Move requested) {
    return new LegalMove(board, requested);
  }

  VerifiedMoves verifyMove(Board board, Move requested) {
    Objects.requireNonNull(board, "board must not be null");
    Objects.requireNonNull(requested, "move must not be null");
    if (requested.color() != board.getSideToMove()) {
      throw new IllegalArgumentException("Move color must match the side to move");
    }
    Move verified = generateLegalMovesForColor(board, board.getSideToMove()).stream()
        .filter(candidate -> representsSameMove(requested, candidate))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Move is not legal in the current position"));
    return new VerifiedMoves(board, List.of(verified));
  }

  public List<Move> generateLegalMoves(Board board, Color color) {
    Objects.requireNonNull(board, "board must not be null");
    Objects.requireNonNull(color, "color must not be null");
    return generateLegalMovesForColor(board, color);
  }

  private List<Move> generateLegalMovesForColor(Board board, Color color) {
    List<Move> moves = new ArrayList<>();
    for (Piece piece : board.getPieces(color)) {
      moves.addAll(filterMovesThatLeaveKingInCheck(board, generateCandidates(board, piece),
          color));
    }
    return List.copyOf(moves);
  }

  private static boolean representsSameMove(Move requested, Move candidate) {
    return requested.color() == candidate.color()
        && requested.from().equals(candidate.from())
        && requested.to().equals(candidate.to())
        && requested.type() == candidate.type()
        && representsSamePiece(requested.piece(), candidate.piece())
        && representsSamePiece(requested.promotionPiece(), candidate.promotionPiece())
        && representsSamePiece(requested.capturedPiece(), candidate.capturedPiece());
  }

  private static boolean representsSamePiece(Piece requested, Piece candidate) {
    if (requested == null || candidate == null) {
      return requested == candidate;
    }
    return requested.getClass() == candidate.getClass()
        && requested.getColor() == candidate.getColor();
  }

  // Only this generator can create the proof used to construct LegalMove instances.
  static final class VerifiedMoves {
    private final Board board;
    private final List<Move> moves;

    private VerifiedMoves(Board board, List<Move> moves) {
      this.board = board;
      this.moves = moves;
    }

    Board board() {
      return board;
    }

    Move moveAt(int index) {
      return moves.get(index);
    }
  }

  public boolean isKingInCheck(Board board, Color color) {
    return attackDetector.isKingInCheck(board, color);
  }

  /** Tests geometric attacks, including attacks by pinned pieces and on friendly pieces. */
  public boolean attacksSquare(Board board, Piece piece, Cell square) {
    Objects.requireNonNull(board, "board must not be null");
    Objects.requireNonNull(piece, "piece must not be null");
    Objects.requireNonNull(square, "square must not be null");
    if (board.getPieceAt(piece.getCell()).orElse(null) != piece) {
      throw new IllegalArgumentException("Piece must be on the board");
    }
    return attackDetector.attacks(board, piece, square);
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

  private List<Move> filterMovesThatLeaveKingInCheck(Board board, List<Move> candidateMoves,
      Color moverColor) {
    List<Move> legalMoves = new ArrayList<>();
    for (Move move : candidateMoves) {
      Board.UndoState state = board.makeTemporaryMove(move);
      try {
        if (!attackDetector.isKingInCheck(board, moverColor)) {
          legalMoves.add(move);
        }
      } finally {
        board.restoreTemporaryMove(state);
      }
    }
    return List.copyOf(legalMoves);
  }

  private List<Move> generateCandidates(Board board, Piece piece) {
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
    if (board.isOccupied(to)
        || !to.equals(board.getEnPassantTarget().orElse(null))) {
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
