package com.desierto.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.desierto.domain.pieces.Bishop;
import com.desierto.domain.pieces.King;
import com.desierto.domain.pieces.Knight;
import com.desierto.domain.pieces.Pawn;
import com.desierto.domain.pieces.Queen;
import com.desierto.domain.pieces.Rook;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class SearchPositionTest {

  private final MoveGenerator generator = new MoveGenerator();

  @Test
  void shouldRestoreAllStateAfterEveryGeneratedMoveIncludingSpecialMoves() {
    for (Board board : positions()) {
      Snapshot before = snapshot(board);
      long version = board.positionVersion();
      List<Piece> originalPieces = board.getPieces();
      List<LegalMove> moves = generator.generateLegalMoveApplications(board, board.getSideToMove());
      assertEquals(before, snapshot(board));
      assertEquals(version, board.positionVersion());
      for (LegalMove move : moves) {
        Snapshot copiedResult = snapshot(move.resultingPosition());
        Board.UndoState undo = board.makeTemporaryMove(move.move());
        assertEquals(copiedResult, snapshot(board));
        board.restoreTemporaryMove(undo);
        assertEquals(before, snapshot(board));
        assertEquals(version, board.positionVersion());
        for (int i = 0; i < originalPieces.size(); i++) {
          assertSame(originalPieces.get(i), board.getPieces().get(i));
        }
      }
    }
  }

  @Test
  void shouldRestoreTheBoardWhenApplyingAMoveThrows() {
    Piece piece = new ExplodingPiece(Cell.A1);
    Board board = BoardFactory.withPieces(BoardFactory.placement(piece, Cell.A1));
    Snapshot before = snapshot(board);
    long version = board.positionVersion();
    Move move = new Move(Color.WHITE, piece, Cell.A1, Cell.A2, null, MoveType.NORMAL, null);

    assertThrows(IllegalStateException.class, () -> board.makeTemporaryMove(move));

    assertEquals(before, snapshot(board));
    assertSame(piece, board.getPieceAt(Cell.A1).orElseThrow());
    assertEquals(version, board.positionVersion());
  }

  @Test
  void shouldRestoreNestedMovesAndInvalidateMovesFromAbandonedBranches() {
    Board source = Board.standard();
    SearchPosition position = new SearchPosition(source);
    Board working = position.board();
    Snapshot initial = snapshot(working);
    LegalMove root = find(position, Cell.E2, Cell.E4);
    LegalMove abandoned;
    try (SearchPosition.MoveScope rootScope = position.apply(root)) {
      Snapshot child = snapshot(working);
      abandoned = find(position, Cell.E7, Cell.E5);
      try (SearchPosition.MoveScope childScope = position.apply(abandoned)) {
        assertNotEquals(child, snapshot(working));
      }
      assertEquals(child, snapshot(working));
    }
    assertEquals(initial, snapshot(working));
    assertEquals(initial, snapshot(source));
    assertThrows(IllegalStateException.class, abandoned::resultingPosition);

    try (SearchPosition.MoveScope ignored = position.apply(root)) {
      assertThrows(IllegalStateException.class, abandoned::resultingPosition);
      assertThrows(IllegalStateException.class, () -> position.apply(abandoned));
    }
    assertEquals(initial, snapshot(working));
  }

  @Test
  void shouldEnforceLifoScopesAndRestoreAfterExceptions() {
    SearchPosition position = new SearchPosition(Board.standard());
    Snapshot initial = snapshot(position.board());
    SearchPosition.MoveScope first = position.apply(find(position, Cell.E2, Cell.E4));
    SearchPosition.MoveScope second = position.apply(find(position, Cell.E7, Cell.E5));
    assertThrows(IllegalStateException.class, first::close);
    second.close();
    first.close();
    assertThrows(IllegalStateException.class, first::close);
    assertEquals(initial, snapshot(position.board()));

    assertThrows(IllegalArgumentException.class, () -> {
      try (SearchPosition.MoveScope ignored = position.apply(find(position, Cell.D2, Cell.D4))) {
        throw new IllegalArgumentException("simulated failure");
      }
    });
    assertEquals(initial, snapshot(position.board()));
  }

  @Test
  void shouldLeaveExistingLegalMovesValidAfterGenerationAndCheckDetection() {
    Board board = Board.standard();
    LegalMove first = generator.generateLegalMoveApplications(board, Color.WHITE).getFirst();
    long version = board.positionVersion();
    Snapshot before = snapshot(board);

    generator.generateLegalMoves(board, Color.WHITE);
    first.givesCheck();

    assertEquals(version, board.positionVersion());
    assertEquals(before, snapshot(board));
    first.resultingPosition();
  }

  @Test
  void shouldReturnMovesBoundToTheOriginalPosition() {
    List<Board> positions = positions();
    for (int i : new int[] {3, 4, 5, 6}) {
      Board source = positions.get(i);
      SearchPosition position = new SearchPosition(source);
      MoveType type = i < 5 ? MoveType.EN_PASSANT : MoveType.CAPTURE_PROMOTION;
      LegalMove candidate = generator.generateLegalMoveApplications(position.board(), source.getSideToMove())
          .stream().filter(move -> move.move().type() == type).findFirst().orElseThrow();

      Move move = position.originalMove(candidate.move());

      assertSame(source.getPieceAt(move.from()).orElseThrow(), move.piece());
      Cell captureCell = type == MoveType.EN_PASSANT
          ? Cell.of(move.from().row(), move.to().column()) : move.to();
      assertSame(source.getPieceAt(captureCell).orElseThrow(), move.capturedPiece());
      source.makeMove(move);
      source.undoMove();
    }
  }

  @Test
  void shouldPreserveThePublicMoveHistoryDuringGeneration() {
    Board board = positions().getLast();
    Snapshot before = snapshot(board);
    LegalMove legalMove = generator.generateLegalMoveApplications(board, board.getSideToMove()).getFirst();
    legalMove.givesCheck();
    assertEquals(before, snapshot(board));

    board.undoMove();

    assertEquals(Color.WHITE, board.getSideToMove());
    assertTrue(board.getPieceAt(Cell.E2).orElseThrow() instanceof Pawn);
    assertTrue(board.getPieceAt(Cell.E4).isEmpty());
  }

  @Test
  void shouldMatchInitialPositionPerftThroughDepthFour() {
    SearchPosition position = new SearchPosition(Board.standard());
    Snapshot before = snapshot(position.board());

    assertEquals(20, perft(position, 1));
    assertEquals(400, perft(position, 2));
    assertEquals(8902, perft(position, 3));
    assertEquals(197281, perft(position, 4));
    assertEquals(before, snapshot(position.board()));
  }

  @Test
  void shouldMatchKiwipetePerftWithCastlingAndCaptures() {
    Board board = BoardFactory.position()
        .place(new Rook(Color.BLACK, Cell.A8), Cell.A8)
        .place(new King(Color.BLACK, Cell.E8), Cell.E8)
        .place(new Rook(Color.BLACK, Cell.H8), Cell.H8)
        .place(new Pawn(Color.BLACK, Cell.A7), Cell.A7)
        .place(new Pawn(Color.BLACK, Cell.C7), Cell.C7)
        .place(new Pawn(Color.BLACK, Cell.D7), Cell.D7)
        .place(new Queen(Color.BLACK, Cell.E7), Cell.E7)
        .place(new Pawn(Color.BLACK, Cell.F7), Cell.F7)
        .place(new Bishop(Color.BLACK, Cell.G7), Cell.G7)
        .place(new Bishop(Color.BLACK, Cell.A6), Cell.A6)
        .place(new Knight(Color.BLACK, Cell.B6), Cell.B6)
        .place(new Pawn(Color.BLACK, Cell.E6), Cell.E6)
        .place(new Knight(Color.BLACK, Cell.F6), Cell.F6)
        .place(new Pawn(Color.BLACK, Cell.G6), Cell.G6)
        .place(new Pawn(Color.WHITE, Cell.D5), Cell.D5)
        .place(new Knight(Color.WHITE, Cell.E5), Cell.E5)
        .place(new Pawn(Color.BLACK, Cell.B4), Cell.B4)
        .place(new Pawn(Color.WHITE, Cell.E4), Cell.E4)
        .place(new Knight(Color.WHITE, Cell.C3), Cell.C3)
        .place(new Queen(Color.WHITE, Cell.F3), Cell.F3)
        .place(new Pawn(Color.BLACK, Cell.H3), Cell.H3)
        .place(new Pawn(Color.WHITE, Cell.A2), Cell.A2)
        .place(new Pawn(Color.WHITE, Cell.B2), Cell.B2)
        .place(new Pawn(Color.WHITE, Cell.C2), Cell.C2)
        .place(new Bishop(Color.WHITE, Cell.D2), Cell.D2)
        .place(new Bishop(Color.WHITE, Cell.E2), Cell.E2)
        .place(new Pawn(Color.WHITE, Cell.F2), Cell.F2)
        .place(new Pawn(Color.WHITE, Cell.G2), Cell.G2)
        .place(new Pawn(Color.WHITE, Cell.H2), Cell.H2)
        .place(new Rook(Color.WHITE, Cell.A1), Cell.A1)
        .place(new King(Color.WHITE, Cell.E1), Cell.E1)
        .place(new Rook(Color.WHITE, Cell.H1), Cell.H1)
        .build();
    SearchPosition position = new SearchPosition(board);
    Snapshot before = snapshot(position.board());

    assertEquals(48, perft(position, 1));
    assertEquals(2039, perft(position, 2));
    assertEquals(97862, perft(position, 3));
    assertEquals(before, snapshot(position.board()));
  }

  private long perft(SearchPosition position, int depth) {
    if (depth == 0) {
      return 1;
    }
    long nodes = 0;
    Board board = position.board();
    for (LegalMove move : generator.generateLegalMoveApplications(board, board.getSideToMove())) {
      try (SearchPosition.MoveScope ignored = position.apply(move)) {
        nodes += perft(position, depth - 1);
      }
    }
    return nodes;
  }

  private LegalMove find(SearchPosition position, Cell from, Cell to) {
    Board board = position.board();
    return generator.generateLegalMoveApplications(board, board.getSideToMove()).stream()
        .filter(move -> move.move().from().equals(from) && move.move().to().equals(to))
        .findFirst().orElseThrow();
  }

  private List<Board> positions() {
    List<Board> boards = new ArrayList<>();
    boards.add(Board.standard());
    for (Color side : Color.values()) {
      boards.add(BoardFactory.position().sideToMove(side)
          .halfmoveClock(23).fullmoveNumber(17)
          .place(new King(Color.WHITE, Cell.E1), Cell.E1)
          .place(new Rook(Color.WHITE, Cell.A1), Cell.A1)
          .place(new Rook(Color.WHITE, Cell.H1), Cell.H1)
          .place(new King(Color.BLACK, Cell.E8), Cell.E8)
          .place(new Rook(Color.BLACK, Cell.A8), Cell.A8)
          .place(new Rook(Color.BLACK, Cell.H8), Cell.H8)
          .build());
    }
    boards.add(BoardFactory.position().castlingRights(false, false, false, false)
        .enPassantTarget(Cell.D6).halfmoveClock(5).fullmoveNumber(9)
        .place(new King(Color.WHITE, Cell.E1), Cell.E1)
        .place(new King(Color.BLACK, Cell.E8), Cell.E8)
        .place(new Pawn(Color.WHITE, Cell.E5), Cell.E5)
        .place(new Pawn(Color.BLACK, Cell.D5), Cell.D5).build());
    boards.add(BoardFactory.position().sideToMove(Color.BLACK)
        .castlingRights(false, false, false, false).enPassantTarget(Cell.D3)
        .place(new King(Color.WHITE, Cell.E1), Cell.E1)
        .place(new King(Color.BLACK, Cell.E8), Cell.E8)
        .place(new Pawn(Color.WHITE, Cell.D4), Cell.D4)
        .place(new Pawn(Color.BLACK, Cell.E4), Cell.E4).build());
    boards.add(BoardFactory.position().castlingRights(false, false, false, false)
        .place(new King(Color.WHITE, Cell.E1), Cell.E1)
        .place(new King(Color.BLACK, Cell.H8), Cell.H8)
        .place(new Pawn(Color.WHITE, Cell.A7), Cell.A7)
        .place(new Rook(Color.BLACK, Cell.B8), Cell.B8).build());
    boards.add(BoardFactory.position().sideToMove(Color.BLACK)
        .castlingRights(false, false, false, false)
        .place(new King(Color.WHITE, Cell.H1), Cell.H1)
        .place(new King(Color.BLACK, Cell.E8), Cell.E8)
        .place(new Pawn(Color.BLACK, Cell.A2), Cell.A2)
        .place(new Rook(Color.WHITE, Cell.B1), Cell.B1).build());
    Board withHistory = Board.standard();
    withHistory.makeMove(generator.generateMoves(withHistory, Cell.E2).stream()
        .filter(move -> move.to().equals(Cell.E4)).findFirst().orElseThrow());
    boards.add(withHistory);
    return boards;
  }

  private Snapshot snapshot(Board board) {
    List<PieceState> pieces = board.getPieces().stream().map(this::pieceState).toList();
    Move last = board.getLastMove().orElse(null);
    MoveState lastMove = last == null ? null : new MoveState(last.color(), last.from(), last.to(),
        last.type(), pieceState(last.piece()), pieceState(last.promotionPiece()),
        pieceState(last.capturedPiece()));
    return new Snapshot(pieces, lastMove, board.getSideToMove(), board.canWhiteCastleKingside(),
        board.canWhiteCastleQueenside(), board.canBlackCastleKingside(),
        board.canBlackCastleQueenside(), board.getEnPassantTarget().orElse(null),
        board.getHalfmoveClock(), board.getFullmoveNumber());
  }

  private PieceState pieceState(Piece piece) {
    return piece == null ? null
        : new PieceState(piece.getClass(), piece.getColor(), piece.getCell(), piece.hasMoved());
  }

  private record PieceState(Class<?> type, Color color, Cell cell, boolean moved) {
  }

  private record MoveState(Color color, Cell from, Cell to, MoveType type, PieceState piece,
                           PieceState promotion, PieceState captured) {
  }

  private record Snapshot(List<PieceState> pieces, MoveState lastMove, Color side, boolean wk,
                          boolean wq, boolean bk, boolean bq, Cell enPassant,
                          int halfmove, int fullmove) {
  }

  private record ExplodingPiece(Cell getCell) implements Piece {
    @Override public Color getColor() { return Color.WHITE; }
    @Override public boolean hasMoved() { return false; }
    @Override public Piece movedTo(Cell cell) { throw new IllegalStateException("simulated failure"); }
    @Override public Piece copy() { return this; }
  }
}
