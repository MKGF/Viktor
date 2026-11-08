package com.desierto.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.desierto.domain.pieces.King;
import com.desierto.domain.pieces.Pawn;
import com.desierto.domain.pieces.Rook;
import java.util.List;
import org.junit.jupiter.api.Test;

class MoveTransactionTest {
  private final MoveGenerator generator = new MoveGenerator();

  @Test
  void rollsBackPositionAndRepetitionAfterFailureWithoutLosingGameHistory() {
    Board board = Board.standard();
    find(board, "e2e4").play();
    find(board, "g8f6").play();
    Move move = find(board, "g1f3").move();
    RepetitionPosition previous = board.repetitionPosition();
    String before = board.toString();
    long version = board.positionVersion();
    long fingerprint = board.repetitionHistoryFingerprint();
    Move lastMove = board.getLastMove().orElseThrow();
    List<Piece> pieces = board.getPieces();
    boolean whiteKingside = board.canWhiteCastleKingside();
    boolean blackKingside = board.canBlackCastleKingside();
    int halfmoveClock = board.getHalfmoveClock();
    int fullmoveNumber = board.getFullmoveNumber();

    assertThrows(IllegalStateException.class, () -> {
      try (Board.MoveTransaction ignored = board.beginMove(move)) {
        board.addRepetitionPosition(previous, move);
        assertNotEquals(before, board.toString());
        assertNotEquals(fingerprint, board.repetitionHistoryFingerprint());
        throw new IllegalStateException("simulated failure after applying the move");
      }
    });

    assertEquals(before, board.toString());
    assertEquals(version, board.positionVersion());
    assertEquals(fingerprint, board.repetitionHistoryFingerprint());
    assertSame(lastMove, board.getLastMove().orElseThrow());
    assertEquals(pieces, board.getPieces());
    assertEquals(whiteKingside, board.canWhiteCastleKingside());
    assertEquals(blackKingside, board.canBlackCastleKingside());
    assertEquals(halfmoveClock, board.getHalfmoveClock());
    assertEquals(fullmoveNumber, board.getFullmoveNumber());
    board.undoMove();
    assertTrue(board.getPieceAt(Cell.G8).isPresent());
    assertTrue(board.getPieceAt(Cell.F6).isEmpty());
  }

  @Test
  void rollsBackWhenRepetitionUpdateThrowsAndWhenAnErrorEscapes() {
    Board board = Board.standard();
    Move move = find(board, "g1f3").move();
    String before = board.toString();
    long version = board.positionVersion();
    long fingerprint = board.repetitionHistoryFingerprint();

    assertThrows(NullPointerException.class, () -> {
      try (Board.MoveTransaction ignored = board.beginMove(move)) {
        board.addRepetitionPosition(null, move);
      }
    });
    assertEquals(before, board.toString());
    assertEquals(version, board.positionVersion());
    assertEquals(fingerprint, board.repetitionHistoryFingerprint());

    assertThrows(AssertionError.class, () -> {
      try (Board.MoveTransaction ignored = board.beginMove(move)) {
        throw new AssertionError("simulated error");
      }
    });
    assertEquals(before, board.toString());
    assertEquals(version, board.positionVersion());
    assertEquals(fingerprint, board.repetitionHistoryFingerprint());
  }

  @Test
  void commitsOnlyWhenExplicitlyRequested() {
    Board board = Board.standard();
    Move move = find(board, "g1f3").move();
    Board.MoveTransaction transaction = board.beginMove(move);
    transaction.commit();
    transaction.close();
    transaction.close();

    assertTrue(board.getPieceAt(Cell.G1).isEmpty());
    assertTrue(board.getPieceAt(Cell.F3).isPresent());
    assertThrows(IllegalStateException.class, transaction::commit);
  }

  @Test
  void rollsBackSpecialMovesAndCastlingRights() {
    Board castling = BoardFactory.position().castlingRights(true, false, false, false)
        .place(new King(Color.WHITE, Cell.E1), Cell.E1)
        .place(new Rook(Color.WHITE, Cell.H1), Cell.H1)
        .place(new King(Color.BLACK, Cell.E8), Cell.E8).build();
    assertRollback(castling, "e1g1");
    assertTrue(castling.canWhiteCastleKingside());

    Board enPassant = BoardFactory.position().castlingRights(false, false, false, false)
        .enPassantTarget(Cell.D6)
        .place(new King(Color.WHITE, Cell.E1), Cell.E1)
        .place(new King(Color.BLACK, Cell.E8), Cell.E8)
        .place(new Pawn(Color.WHITE, Cell.E5), Cell.E5)
        .place(new Pawn(Color.BLACK, Cell.D5), Cell.D5).build();
    assertRollback(enPassant, "e5d6");
    assertEquals(Cell.D6, enPassant.getEnPassantTarget().orElseThrow());

    Board promotion = BoardFactory.position().castlingRights(false, false, false, false)
        .place(new King(Color.WHITE, Cell.E1), Cell.E1)
        .place(new King(Color.BLACK, Cell.H8), Cell.H8)
        .place(new Pawn(Color.WHITE, Cell.A7), Cell.A7)
        .place(new Rook(Color.BLACK, Cell.B8), Cell.B8).build();
    assertRollback(promotion, "a7b8q");
  }

  private void assertRollback(Board board, String notation) {
    Move move = find(board, notation).move();
    String before = board.toString();
    List<Piece> pieces = board.getPieces();
    long version = board.positionVersion();
    assertThrows(IllegalStateException.class, () -> {
      try (Board.MoveTransaction ignored = board.beginMove(move)) {
        throw new IllegalStateException("simulated failure");
      }
    });
    assertEquals(before, board.toString());
    assertEquals(pieces, board.getPieces());
    assertEquals(version, board.positionVersion());
  }

  private LegalMove find(Board board, String notation) {
    return MoveNotation.findLegalMove(notation,
        generator.generateLegalMoveApplications(board, board.getSideToMove())).orElseThrow();
  }
}
