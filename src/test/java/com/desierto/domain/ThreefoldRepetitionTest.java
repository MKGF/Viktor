package com.desierto.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.desierto.domain.pieces.King;
import com.desierto.domain.pieces.Knight;
import com.desierto.domain.pieces.Pawn;
import com.desierto.domain.pieces.Rook;
import org.junit.jupiter.api.Test;

class ThreefoldRepetitionTest {
  private final MoveGenerator generator = new MoveGenerator();
  private static final String[] CYCLE = {"g1f3", "g8f6", "f3g1", "f6g8"};

  @Test
  void countsTheInitialPositionAndRestoresHistoryOnUndoAndCopy() {
    Board board = Board.standard();
    play(board, CYCLE);
    assertFalse(board.isThreefoldRepetition());
    Board copy = board.copy();
    play(board, CYCLE);
    assertTrue(DrawDetector.isThreefoldRepetitionDraw(board, generator));
    assertFalse(copy.isThreefoldRepetition());
    play(copy, CYCLE);
    assertTrue(copy.isThreefoldRepetition());
    board.undoMove();
    assertFalse(board.isThreefoldRepetition());
    play(board, "f6g8");
    assertTrue(board.isThreefoldRepetition());
  }

  @Test
  void tracksSearchBranchesAndGeneratedResultWithoutContaminatingTheSource() {
    Board source = Board.standard();
    play(source, CYCLE);
    play(source, "g1f3", "g8f6", "f3g1");
    SearchPosition position = new SearchPosition(source);
    LegalMove last = find(position.board(), "f6g8");
    long before = position.board().repetitionHistoryFingerprint();
    assertTrue(last.resultingPosition().isThreefoldRepetition());
    generator.generateLegalMoves(position.board(), Color.BLACK);
    last.givesCheck();
    assertEquals(before, position.board().repetitionHistoryFingerprint());
    assertThrows(IllegalStateException.class, () -> {
      try (SearchPosition.MoveScope ignored = position.exploreMove(last)) {
        assertTrue(position.board().isThreefoldRepetition());
        throw new IllegalStateException("interrupt");
      }
    });
    assertFalse(position.board().isThreefoldRepetition());
    assertEquals(before, position.board().repetitionHistoryFingerprint());
    assertFalse(source.isThreefoldRepetition());
    try (SearchPosition.MoveScope ignored = position.exploreMove(find(position.board(), "f6h5"))) {
      assertFalse(position.board().isThreefoldRepetition());
    }
    assertEquals(before, position.board().repetitionHistoryFingerprint());
  }

  @Test
  void irreversibleSearchMovesClearHistoryOnlyInsideTheirScope() {
    Board source = Board.standard();
    play(source, CYCLE);
    SearchPosition position = new SearchPosition(source);
    Board board = position.board();
    long before = board.repetitionHistoryFingerprint();
    LegalMove pawnMove = find(board, "e2e4");
    assertEquals(0, pawnMove.resultingPosition().repetitionHistoryFingerprint());
    try (SearchPosition.MoveScope ignored = position.exploreMove(pawnMove)) {
      assertEquals(0, board.repetitionHistoryFingerprint());
    }
    assertEquals(before, board.repetitionHistoryFingerprint());
    board.makeMove(find(board, "e2e4").move());
    assertEquals(0, board.repetitionHistoryFingerprint());
    board.undoMove();
    assertEquals(before, board.repetitionHistoryFingerprint());
  }

  @Test
  void identityIncludesTurnAndEffectiveCastlingRightsButNotClocksOrUnrelatedMovedFlags() {
    Board base = kingRookPosition(Color.WHITE, true, false, 0);
    Board otherClock = kingRookPosition(Color.WHITE, true, false, 70);
    Board movedKnight = kingRookPosition(Color.WHITE, true, true, 0);
    assertEquals(RepetitionPosition.of(base), RepetitionPosition.of(otherClock));
    assertEquals(RepetitionPosition.of(base), RepetitionPosition.of(movedKnight));
    assertNotEquals(RepetitionPosition.of(base),
        RepetitionPosition.of(kingRookPosition(Color.BLACK, true, false, 0)));
    assertNotEquals(RepetitionPosition.of(base),
        RepetitionPosition.of(kingRookPosition(Color.WHITE, false, false, 0)));

    Board returnedRook = BoardFactory.position().castlingRights(true, false, false, false)
        .place(new King(Color.WHITE, Cell.E1), Cell.E1)
        .place(new Rook(Color.WHITE, Cell.H1).movedTo(Cell.H1), Cell.H1)
        .place(new Knight(Color.WHITE, Cell.B1), Cell.B1)
        .place(new King(Color.BLACK, Cell.E8), Cell.E8).build();
    assertEquals(RepetitionPosition.of(kingRookPosition(Color.WHITE, false, false, 0)),
        RepetitionPosition.of(returnedRook));
  }

  @Test
  void enPassantOnlyDistinguishesPositionsWhenCaptureIsLegal() {
    Board legal = enPassantPosition(Cell.D6, false, true);
    Board legalWithoutTarget = enPassantPosition(null, false, true);
    assertNotEquals(RepetitionPosition.of(legal), RepetitionPosition.of(legalWithoutTarget));
    assertEquals(RepetitionPosition.of(enPassantPosition(Cell.D6, true, true)),
        RepetitionPosition.of(enPassantPosition(null, true, true)));
    assertEquals(RepetitionPosition.of(enPassantPosition(Cell.D6, false, false)),
        RepetitionPosition.of(enPassantPosition(null, false, false)));
  }

  @Test
  void initialFactoryPositionAndExpiredEnPassantAreCounted() {
    Board board = enPassantPosition(Cell.D6, false, false);
    play(board, "e1f1", "e8f8", "f1e1", "f8e8");
    assertFalse(board.isThreefoldRepetition());
    play(board, "e1f1", "e8f8", "f1e1", "f8e8");
    assertTrue(board.isThreefoldRepetition());
  }

  @Test
  void legalEnPassantInTheInitialPositionDelaysTheThirdOccurrence() {
    Board board = enPassantPosition(Cell.D6, false, true);
    String[] cycle = {"e1f1", "e8f8", "f1e1", "f8e8"};
    play(board, cycle);
    play(board, cycle);
    assertFalse(board.isThreefoldRepetition());
    play(board, cycle);
    assertTrue(board.isThreefoldRepetition());
  }

  @Test
  void losingCastlingRightsDistinguishesTheInitialPosition() {
    Board board = BoardFactory.position().castlingRights(true, false, false, false)
        .place(new King(Color.WHITE, Cell.E1), Cell.E1)
        .place(new Rook(Color.WHITE, Cell.H1), Cell.H1)
        .place(new King(Color.BLACK, Cell.E8), Cell.E8).build();
    String[] cycle = {"h1h2", "e8f8", "h2h1", "f8e8"};
    play(board, cycle);
    play(board, cycle);
    assertFalse(board.isThreefoldRepetition());
    play(board, cycle);
    assertTrue(board.isThreefoldRepetition());
  }

  private Board kingRookPosition(Color side, boolean right, boolean knightMoved, int clock) {
    Knight knight = new Knight(Color.WHITE, Cell.B1);
    return BoardFactory.position().sideToMove(side).halfmoveClock(clock)
        .castlingRights(right, false, false, false)
        .place(new King(Color.WHITE, Cell.E1), Cell.E1)
        .place(new Rook(Color.WHITE, Cell.H1), Cell.H1)
        .place(knightMoved ? knight.movedTo(Cell.B1) : knight, Cell.B1)
        .place(new King(Color.BLACK, Cell.E8), Cell.E8).build();
  }

  private Board enPassantPosition(Cell target, boolean pinned, boolean whitePawn) {
    BoardFactory.PositionBuilder builder = BoardFactory.position()
        .castlingRights(false, false, false, false).enPassantTarget(target)
        .place(new King(Color.WHITE, Cell.E1), Cell.E1)
        .place(new King(Color.BLACK, pinned ? Cell.A8 : Cell.E8), pinned ? Cell.A8 : Cell.E8)
        .place(new Pawn(Color.BLACK, Cell.D5), Cell.D5);
    if (whitePawn) {
      builder.place(new Pawn(Color.WHITE, Cell.E5), Cell.E5);
    }
    if (pinned) {
      builder.place(new Rook(Color.BLACK, Cell.E8), Cell.E8);
    }
    return builder.build();
  }

  private void play(Board board, String... moves) {
    for (String move : moves) {
      board.makeMove(find(board, move).move());
    }
  }

  private LegalMove find(Board board, String notation) {
    return generator.generateLegalMoveApplications(board, board.getSideToMove()).stream()
        .filter(move -> (move.move().from().toAlgebraic() + move.move().to().toAlgebraic())
            .equalsIgnoreCase(notation))
        .findFirst().orElseThrow();
  }
}
