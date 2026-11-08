package com.desierto.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.desierto.domain.pieces.King;
import com.desierto.domain.pieces.Knight;
import com.desierto.domain.pieces.Pawn;
import com.desierto.domain.pieces.Rook;
import org.junit.jupiter.api.Test;

class LegalMoveConstructionTest {
  private final MoveGenerator generator = new MoveGenerator();

  @Test
  void validatesAndBindsAnIndividuallyRequestedMove() {
    Board board = Board.standard();
    Move requested = MoveNotation.find("e2e4",
        generator.generateLegalMoves(board, board.getSideToMove())).orElseThrow();
    String before = board.toString();
    long version = board.positionVersion();

    LegalMove legal = new LegalMove(board, requested);

    assertEquals(before, board.toString());
    assertEquals(version, board.positionVersion());
    assertEquals(requested, legal.move());
    legal.play();
    assertTrue(board.getPieceAt(Cell.E4).orElseThrow() instanceof Pawn);
    assertThrows(IllegalStateException.class, legal::play);
  }

  @Test
  void rejectsIllegalMovesAndWrongTurnsWithoutModifyingTheBoard() {
    Board board = Board.standard();
    String before = board.toString();
    long version = board.positionVersion();
    Move illegal = new Move(Color.WHITE, board.getPieceAt(Cell.E2).orElseThrow(),
        Cell.E2, Cell.E5, null, MoveType.NORMAL, null);
    Move wrongSide = new Move(Color.BLACK, board.getPieceAt(Cell.E7).orElseThrow(),
        Cell.E7, Cell.E5, null, MoveType.NORMAL, null);

    assertThrows(IllegalArgumentException.class, () -> new LegalMove(board, illegal));
    assertThrows(IllegalArgumentException.class, () -> new LegalMove(board, wrongSide));
    assertThrows(NullPointerException.class, () -> new LegalMove(null, illegal));
    assertThrows(NullPointerException.class, () -> new LegalMove(board, null));
    assertEquals(before, board.toString());
    assertEquals(version, board.positionVersion());
  }

  @Test
  void rejectsAValidLookingMoveThatExposesTheKing() {
    Board board = BoardFactory.position().castlingRights(false, false, false, false)
        .place(new King(Color.WHITE, Cell.E1), Cell.E1)
        .place(new Rook(Color.WHITE, Cell.E2), Cell.E2)
        .place(new King(Color.BLACK, Cell.A8), Cell.A8)
        .place(new Rook(Color.BLACK, Cell.E8), Cell.E8).build();
    Move pinned = new Move(Color.WHITE, board.getPieceAt(Cell.E2).orElseThrow(),
        Cell.E2, Cell.D2, null, MoveType.NORMAL, null);
    String before = board.toString();

    assertThrows(IllegalArgumentException.class, () -> new LegalMove(board, pinned));
    assertEquals(before, board.toString());
  }

  @Test
  void rejectsCastlingThroughAnAttackedSquare() {
    Board board = BoardFactory.position().castlingRights(true, false, false, false)
        .place(new King(Color.WHITE, Cell.E1), Cell.E1)
        .place(new Rook(Color.WHITE, Cell.H1), Cell.H1)
        .place(new King(Color.BLACK, Cell.A8), Cell.A8)
        .place(new Rook(Color.BLACK, Cell.F8), Cell.F8).build();
    Move castle = new Move(Color.WHITE, board.getPieceAt(Cell.E1).orElseThrow(),
        Cell.E1, Cell.G1, null, MoveType.CASTLE_KINGSIDE, null);

    assertThrows(IllegalArgumentException.class, () -> new LegalMove(board, castle));
    assertTrue(board.getPieceAt(Cell.E1).orElseThrow() instanceof King);
    assertTrue(board.canWhiteCastleKingside());
  }

  @Test
  void validatesTheChosenPromotionAndCaptureMetadata() {
    Board board = BoardFactory.position().castlingRights(false, false, false, false)
        .place(new King(Color.WHITE, Cell.E1), Cell.E1)
        .place(new King(Color.BLACK, Cell.H8), Cell.H8)
        .place(new Pawn(Color.WHITE, Cell.A7), Cell.A7)
        .place(new Rook(Color.BLACK, Cell.B8), Cell.B8).build();
    Move requested = MoveNotation.find("a7b8n",
        generator.generateLegalMoves(board, Color.WHITE)).orElseThrow();
    Move wrongCapturedPiece = new Move(Color.WHITE, requested.piece(), Cell.A7, Cell.B8,
        requested.promotionPiece(), MoveType.CAPTURE_PROMOTION, new Pawn(Color.BLACK, Cell.B8));

    assertThrows(IllegalArgumentException.class, () -> new LegalMove(board, wrongCapturedPiece));
    new LegalMove(board, requested).play();
    assertTrue(board.getPieceAt(Cell.B8).orElseThrow() instanceof Knight);
  }
}
