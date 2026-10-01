package com.desierto.search.evaluation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.desierto.domain.Board;
import com.desierto.domain.BoardFactory;
import com.desierto.domain.Cell;
import com.desierto.domain.Color;
import com.desierto.domain.pieces.Bishop;
import com.desierto.domain.pieces.Knight;
import com.desierto.domain.pieces.Pawn;
import com.desierto.domain.pieces.Queen;
import com.desierto.domain.pieces.Rook;
import org.junit.jupiter.api.Test;

class MaterialEvaluatorTest {

  private final MaterialEvaluator evaluator = new MaterialEvaluator();

  @Test
  void shouldEvaluateMaterialFromTheRequestedPerspective() {
    Board board = BoardFactory.withPieces(
        BoardFactory.placement(new Queen(Color.WHITE, Cell.D1), Cell.D1),
        BoardFactory.placement(new Rook(Color.WHITE, Cell.A1), Cell.A1),
        BoardFactory.placement(new Bishop(Color.WHITE, Cell.C1), Cell.C1),
        BoardFactory.placement(new Knight(Color.BLACK, Cell.B8), Cell.B8),
        BoardFactory.placement(new Pawn(Color.BLACK, Cell.A7), Cell.A7));

    assertEquals(1_310, evaluator.evaluate(board, Color.WHITE));
    assertEquals(-1_310, evaluator.evaluate(board, Color.BLACK));
  }

  @Test
  void shouldEvaluateEqualMaterialAsNeutral() {
    Board board = BoardFactory.withPieces(
        BoardFactory.placement(new Pawn(Color.WHITE, Cell.E2), Cell.E2),
        BoardFactory.placement(new Pawn(Color.BLACK, Cell.E7), Cell.E7));

    assertEquals(0, evaluator.evaluate(board, Color.WHITE));
    assertEquals(0, evaluator.evaluate(board, Color.BLACK));
  }
}
