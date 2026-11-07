package com.desierto.domain;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.desierto.domain.pieces.Bishop;
import com.desierto.domain.pieces.King;
import com.desierto.domain.pieces.Knight;
import com.desierto.domain.pieces.Pawn;
import com.desierto.domain.pieces.Queen;
import com.desierto.domain.pieces.Rook;
import org.junit.jupiter.api.Test;

class DrawDetectorTest {

  @Test
  void shouldRecognizeKingsAloneAndOneMinorPiece() {
    assertTrue(DrawDetector.isDeadByInsufficientMaterial(position()));
    assertTrue(DrawDetector.isDeadByInsufficientMaterial(position(new Bishop(Color.WHITE, Cell.C1))));
    assertTrue(DrawDetector.isDeadByInsufficientMaterial(position(new Bishop(Color.BLACK, Cell.C8))));
    assertTrue(DrawDetector.isDeadByInsufficientMaterial(position(new Knight(Color.WHITE, Cell.B1))));
    assertTrue(DrawDetector.isDeadByInsufficientMaterial(position(new Knight(Color.BLACK, Cell.B8))));
  }

  @Test
  void shouldRecognizeBishopsRestrictedToOneSquareColorIncludingPromotedBishops() {
    assertTrue(DrawDetector.isDeadByInsufficientMaterial(position(
        new Bishop(Color.WHITE, Cell.C1), new Bishop(Color.BLACK, Cell.F8))));
    assertTrue(DrawDetector.isDeadByInsufficientMaterial(position(
        new Bishop(Color.WHITE, Cell.C1), new Bishop(Color.WHITE, Cell.E3),
        new Bishop(Color.BLACK, Cell.F8))));
  }

  @Test
  void shouldNotDeclareDrawWhenCheckmateMayBePossible() {
    assertFalse(DrawDetector.isDeadByInsufficientMaterial(position(new Pawn(Color.WHITE, Cell.A2))));
    assertFalse(DrawDetector.isDeadByInsufficientMaterial(position(new Rook(Color.WHITE, Cell.A2))));
    assertFalse(DrawDetector.isDeadByInsufficientMaterial(position(new Queen(Color.WHITE, Cell.A2))));
    assertFalse(DrawDetector.isDeadByInsufficientMaterial(position(
        new Bishop(Color.WHITE, Cell.C1), new Bishop(Color.BLACK, Cell.C8))));
    assertFalse(DrawDetector.isDeadByInsufficientMaterial(position(
        new Bishop(Color.WHITE, Cell.C1), new Knight(Color.WHITE, Cell.B1))));
    assertFalse(DrawDetector.isDeadByInsufficientMaterial(position(
        new Knight(Color.WHITE, Cell.B1), new Knight(Color.WHITE, Cell.G1))));
    assertFalse(DrawDetector.isDeadByInsufficientMaterial(position(
        new Knight(Color.WHITE, Cell.B1), new Knight(Color.BLACK, Cell.B8))));
  }

  @Test
  void shouldNotTreatIncompleteOrInvalidKingSetsAsDead() {
    assertFalse(DrawDetector.isDeadByInsufficientMaterial(BoardFactory.empty()));
    assertFalse(DrawDetector.isDeadByInsufficientMaterial(BoardFactory.withPieces(
        BoardFactory.placement(new King(Color.WHITE, Cell.A1), Cell.A1))));
    assertThrows(NullPointerException.class, () -> DrawDetector.isDeadByInsufficientMaterial(null));
  }

  private Board position(Piece... pieces) {
    var builder = BoardFactory.position().castlingRights(false, false, false, false)
        .place(new King(Color.WHITE, Cell.A1), Cell.A1)
        .place(new King(Color.BLACK, Cell.H8), Cell.H8);
    for (Piece piece : pieces) {
      builder.place(piece, piece.getCell());
    }
    return builder.build();
  }
}
