package com.desierto.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.desierto.domain.pieces.King;
import com.desierto.domain.pieces.Pawn;
import com.desierto.domain.pieces.Queen;
import com.desierto.domain.pieces.Rook;
import java.util.List;
import org.junit.jupiter.api.Test;

class LegalMoveBoundaryTest {
  private final MoveGenerator generator = new MoveGenerator();

  @Test
  void playsAValidatedMoveAndKeepsThePublicUndoHistory() {
    Board board = Board.standard();
    String before = board.toString();
    LegalMove move = find(board, "e2e4");
    Board expected = move.resultingPosition();

    move.play();

    assertEquals(expected.toString(), board.toString());
    assertEquals(expected.getSideToMove(), board.getSideToMove());
    assertEquals(expected.getEnPassantTarget(), board.getEnPassantTarget());
    assertEquals(expected.getHalfmoveClock(), board.getHalfmoveClock());
    assertEquals(expected.getFullmoveNumber(), board.getFullmoveNumber());
    assertEquals(expected.repetitionHistoryFingerprint(), board.repetitionHistoryFingerprint());
    assertThrows(IllegalStateException.class, move::play);
    board.undoMove();
    assertEquals(before, board.toString());
    assertThrows(IllegalStateException.class, move::play);
  }

  @Test
  void rejectsMovesBoundToAnotherBoardOrAnAbandonedSearchBranch() {
    Board source = Board.standard();
    SearchPosition position = new SearchPosition(source);
    Board board = position.board();
    LegalMove fromSource = find(source, "e2e4");
    String before = board.toString();

    assertThrows(IllegalStateException.class, () -> position.exploreMove(fromSource));
    assertThrows(IllegalStateException.class, () -> board.applyLegalMove(fromSource));
    assertEquals(before, board.toString());

    LegalMove abandoned;
    try (SearchPosition.MoveScope ignored = position.exploreMove(find(board, "e2e4"))) {
      abandoned = find(board, "e7e5");
    }
    assertThrows(IllegalStateException.class, () -> position.exploreMove(abandoned));
    assertThrows(IllegalStateException.class, abandoned::play);
    assertEquals(before, board.toString());
  }

  @Test
  void doesNotAccreditIllegalMovesOrIncorrectMetadata() {
    Board board = Board.standard();
    Piece pawn = board.getPieceAt(Cell.E2).orElseThrow();
    Move illegal = new Move(Color.WHITE, pawn, Cell.E2, Cell.E5, null, MoveType.NORMAL, null);
    String before = board.toString();

    assertThrows(IllegalArgumentException.class, () -> generator.requireLegalMove(board, illegal));
    assertFalse(generator.generateLegalMoveApplications(board, Color.WHITE).stream()
        .anyMatch(move -> move.move().to().equals(Cell.E5)));
    assertEquals(before, board.toString());

    Board captureBoard = BoardFactory.position().sideToMove(Color.BLACK)
        .place(new Rook(Color.WHITE, Cell.H1), Cell.H1)
        .place(new Rook(Color.BLACK, Cell.H8), Cell.H8).build();
    Move forged = new Move(Color.BLACK, captureBoard.getPieceAt(Cell.H8).orElseThrow(),
        Cell.H8, Cell.H1, null, MoveType.CAPTURE, new Pawn(Color.WHITE, Cell.H1));
    assertThrows(IllegalArgumentException.class, () -> generator.requireLegalMove(captureBoard, forged));
  }

  @Test
  void doesNotTrustAnOverriddenRawMoveGeneratorToAccreditMoves() {
    Board board = Board.standard();
    Move illegal = new Move(Color.WHITE, board.getPieceAt(Cell.E2).orElseThrow(),
        Cell.E2, Cell.E5, null, MoveType.NORMAL, null);
    MoveGenerator misleading = new MoveGenerator() {
      @Override
      public List<Move> generateLegalMoves(Board ignored, Color color) {
        return List.of(illegal);
      }
    };

    assertFalse(misleading.generateLegalMoveApplications(board, Color.WHITE).stream()
        .anyMatch(move -> move.move().to().equals(Cell.E5)));
    assertThrows(IllegalArgumentException.class, () -> misleading.requireLegalMove(board, illegal));
  }

  @Test
  void playsCastlingAndPreservesRepetitionHistory() {
    Board board = BoardFactory.position().castlingRights(true, false, false, false)
        .place(new King(Color.WHITE, Cell.E1), Cell.E1)
        .place(new Rook(Color.WHITE, Cell.H1), Cell.H1)
        .place(new King(Color.BLACK, Cell.E8), Cell.E8).build();
    find(board, "e1g1").play();
    assertTrue(board.getPieceAt(Cell.G1).orElseThrow() instanceof King);
    assertTrue(board.getPieceAt(Cell.F1).orElseThrow() instanceof Rook);
    assertFalse(board.canWhiteCastleKingside());
    board.undoMove();
    assertTrue(board.canWhiteCastleKingside());

    Board repetition = Board.standard();
    for (int i = 0; i < 2; i++) {
      for (String notation : List.of("g1f3", "g8f6", "f3g1", "f6g8")) {
        find(repetition, notation).play();
      }
    }
    assertTrue(repetition.isThreefoldRepetition());
    repetition.undoMove();
    assertFalse(repetition.isThreefoldRepetition());
  }

  @Test
  void playsAndUndoesEnPassantAndCapturePromotion() {
    Board enPassant = BoardFactory.position().castlingRights(false, false, false, false)
        .enPassantTarget(Cell.D6)
        .place(new King(Color.WHITE, Cell.E1), Cell.E1)
        .place(new King(Color.BLACK, Cell.E8), Cell.E8)
        .place(new Pawn(Color.WHITE, Cell.E5), Cell.E5)
        .place(new Pawn(Color.BLACK, Cell.D5), Cell.D5).build();
    String initial = enPassant.toString();
    find(enPassant, "e5d6").play();
    assertTrue(enPassant.getPieceAt(Cell.D6).orElseThrow() instanceof Pawn);
    assertTrue(enPassant.getPieceAt(Cell.D5).isEmpty());
    enPassant.undoMove();
    assertEquals(initial, enPassant.toString());

    Board promotion = BoardFactory.position().castlingRights(false, false, false, false)
        .place(new King(Color.WHITE, Cell.E1), Cell.E1)
        .place(new King(Color.BLACK, Cell.H8), Cell.H8)
        .place(new Pawn(Color.WHITE, Cell.A7), Cell.A7)
        .place(new Rook(Color.BLACK, Cell.B8), Cell.B8).build();
    initial = promotion.toString();
    find(promotion, "a7b8q").play();
    assertTrue(promotion.getPieceAt(Cell.B8).orElseThrow() instanceof Queen);
    promotion.undoMove();
    assertEquals(initial, promotion.toString());
  }

  private LegalMove find(Board board, String notation) {
    return MoveNotation.findLegalMove(notation,
        generator.generateLegalMoveApplications(board, board.getSideToMove())).orElseThrow();
  }
}
