package com.desierto.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.desierto.domain.pieces.Pawn;
import com.desierto.domain.pieces.Queen;
import org.junit.jupiter.api.Test;

class MoveTest {

  @Test
  void shouldCreateNormalMove() {
    Pawn pawn = new Pawn(Color.WHITE, Cell.E2);

    Move move = new Move(Color.WHITE, pawn, Cell.E2, Cell.E4, null, MoveType.NORMAL, null);

    assertEquals(Color.WHITE, move.color());
    assertEquals(pawn, move.piece());
    assertEquals(Cell.E2, move.from());
    assertEquals(Cell.E4, move.to());
    assertEquals(MoveType.NORMAL, move.type());
    assertFalse(move.isCapture());
    assertFalse(move.isPromotion());
  }

  @Test
  void shouldCreateCaptureAndPromotionMoves() {
    Pawn pawn = new Pawn(Color.WHITE, Cell.E7);
    Pawn captured = new Pawn(Color.BLACK, Cell.E8);
    Queen promotion = new Queen(Color.WHITE, Cell.E8);

    Move capture = new Move(Color.WHITE, pawn, Cell.E7, Cell.E8, null, MoveType.CAPTURE, captured);
    Move promotionMove = new Move(Color.WHITE, pawn, Cell.E7, Cell.E8, promotion, MoveType.PROMOTION, null);
    Move capturePromotion = new Move(Color.WHITE, pawn, Cell.E7, Cell.E8, promotion, MoveType.CAPTURE_PROMOTION, captured);

    assertTrue(capture.isCapture());
    assertEquals(captured, capture.capturedPiece());
    assertEquals(MoveType.CAPTURE, capture.type());

    assertTrue(promotionMove.isPromotion());
    assertEquals(promotion, promotionMove.promotionPiece());
    assertEquals(MoveType.PROMOTION, promotionMove.type());

    assertTrue(capturePromotion.isCapture());
    assertTrue(capturePromotion.isPromotion());
    assertEquals(MoveType.CAPTURE_PROMOTION, capturePromotion.type());
  }

  @Test
  void shouldRejectInvalidMoveConfiguration() {
    Pawn pawn = new Pawn(Color.WHITE, Cell.E2);
    Queen queen = new Queen(Color.WHITE, Cell.E8);
    Pawn captured = new Pawn(Color.BLACK, Cell.E8);

    assertThrows(IllegalArgumentException.class,
        () -> new Move(Color.BLACK, pawn, Cell.E2, Cell.E4, null, MoveType.NORMAL, null));
    assertThrows(IllegalArgumentException.class,
        () -> new Move(Color.WHITE, pawn, Cell.E2, Cell.E4, queen, MoveType.NORMAL, null));
    assertThrows(IllegalArgumentException.class,
        () -> new Move(Color.WHITE, pawn, Cell.E2, Cell.E4, null, MoveType.NORMAL, captured));
    assertThrows(IllegalArgumentException.class,
        () -> new Move(Color.WHITE, pawn, Cell.E7, Cell.E8, null, MoveType.PROMOTION, null));
    assertThrows(IllegalArgumentException.class,
        () -> new Move(Color.WHITE, pawn, Cell.E2, Cell.E4, null, MoveType.CAPTURE, null));
    assertThrows(IllegalArgumentException.class,
        () -> new Move(Color.WHITE, pawn, Cell.E7, Cell.E8, new Pawn(Color.WHITE, Cell.E8),
            MoveType.PROMOTION, null));
  }
}
