package com.desierto.domain;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.desierto.domain.pieces.Bishop;
import com.desierto.domain.pieces.King;
import com.desierto.domain.pieces.Knight;
import com.desierto.domain.pieces.Pawn;
import com.desierto.domain.pieces.Queen;
import com.desierto.domain.pieces.Rook;
import java.util.List;
import org.junit.jupiter.api.Test;

class AttackGeometryTest {
  private final MoveGenerator generator = new MoveGenerator();

  @Test
  void attackDetectionMatchesPieceListTraversalBeforeDuringAndAfterTemporaryMoves() {
    AttackDetector detector = new AttackDetector();
    Board special = BoardFactory.position().enPassantTarget(Cell.D6)
        .place(new King(Color.WHITE, Cell.E1), Cell.E1)
        .place(new Rook(Color.WHITE, Cell.A1), Cell.A1)
        .place(new Rook(Color.WHITE, Cell.H1), Cell.H1)
        .place(new Pawn(Color.WHITE, Cell.E5), Cell.E5)
        .place(new Pawn(Color.BLACK, Cell.D5), Cell.D5)
        .place(new King(Color.BLACK, Cell.E8), Cell.E8).build();
    Board promotion = BoardFactory.position().castlingRights(false, false, false, false)
        .place(new King(Color.WHITE, Cell.E1), Cell.E1)
        .place(new King(Color.BLACK, Cell.E8), Cell.E8)
        .place(new Pawn(Color.WHITE, Cell.A7), Cell.A7)
        .place(new Rook(Color.BLACK, Cell.B8), Cell.B8).build();
    List<Move> specialMoves = generator.generateLegalMoves(special, Color.WHITE);
    assertTrue(specialMoves.stream().anyMatch(move -> move.type() == MoveType.EN_PASSANT));
    assertTrue(specialMoves.stream().anyMatch(move -> move.type() == MoveType.CASTLE_KINGSIDE));
    assertTrue(generator.generateLegalMoves(promotion, Color.WHITE).stream().anyMatch(Move::isPromotion));
    for (Board board : List.of(Board.standard(), special, promotion)) {
      String initial = board.toString();
      assertMatchesPieceListTraversal(board, detector);
      for (Move move : generator.generateLegalMoves(board, board.getSideToMove())) {
        Board.UndoState undo = board.makeTemporaryMove(move);
        try {
          assertMatchesPieceListTraversal(board, detector);
        } finally {
          board.restoreTemporaryMove(undo);
        }
        assertEquals(initial, board.toString());
        assertMatchesPieceListTraversal(board, detector);
      }
    }
  }

  private static void assertMatchesPieceListTraversal(Board board, AttackDetector detector) {
    for (Color color : Color.values()) {
      for (int row = 0; row < 8; row++) {
        for (int column = 0; column < 8; column++) {
          Cell target = Cell.of(row, column);
          boolean expected = board.getPieces(color).stream()
              .anyMatch(piece -> detector.attacks(board, piece, target));
          assertEquals(expected, detector.isSquareAttacked(board, target, color),
              color + " attacks " + target);
        }
      }
      Cell king = board.getPieces(color).stream().filter(King.class::isInstance)
          .map(Piece::getCell).findFirst().orElse(null);
      boolean expectedCheck = king != null && board.getPieces(color.opposite()).stream()
          .anyMatch(piece -> detector.attacks(board, piece, king));
      assertEquals(expectedCheck, detector.isKingInCheck(board, color));
    }
  }

  @Test
  void slidingAttacksStopAtTheFirstPieceAndIncludeFriendlyPieces() {
    Bishop bishop = new Bishop(Color.WHITE, Cell.C1);
    Pawn blocker = new Pawn(Color.WHITE, Cell.E3);
    Rook rook = new Rook(Color.BLACK, Cell.A4);
    Queen queen = new Queen(Color.BLACK, Cell.H8);
    Board board = BoardFactory.position().castlingRights(false, false, false, false)
        .place(bishop, Cell.C1).place(blocker, Cell.E3)
        .place(rook, Cell.A4).place(queen, Cell.H8).build();
    assertTrue(generator.attacksSquare(board, bishop, Cell.E3));
    assertFalse(generator.attacksSquare(board, bishop, Cell.F4));
    assertFalse(generator.attacksSquare(board, bishop, Cell.C1));
    assertFalse(generator.attacksSquare(board, bishop, Cell.C4));
    assertTrue(generator.attacksSquare(board, rook, Cell.D4));
    assertFalse(generator.attacksSquare(board, rook, Cell.B5));
    assertTrue(generator.attacksSquare(board, queen, Cell.H5));
    assertTrue(generator.attacksSquare(board, queen, Cell.F6));
    assertFalse(generator.attacksSquare(board, queen, Cell.F5));
  }

  @Test
  void pinnedPiecesStillHaveGeometricAttacks() {
    Knight knight = new Knight(Color.WHITE, Cell.E2);
    Board board = BoardFactory.position().castlingRights(false, false, false, false)
        .place(new King(Color.WHITE, Cell.E1), Cell.E1)
        .place(knight, Cell.E2)
        .place(new Rook(Color.BLACK, Cell.E8), Cell.E8)
        .place(new King(Color.BLACK, Cell.A8), Cell.A8).build();
    assertTrue(generator.attacksSquare(board, knight, Cell.F4));
    assertTrue(generator.generateLegalMoves(board, Cell.E2).isEmpty());
    assertThrows(IllegalArgumentException.class,
        () -> generator.attacksSquare(board, new Knight(Color.WHITE, Cell.E2), Cell.F4));
  }
}
