package com.desierto.search.evaluation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.desierto.domain.Board;
import com.desierto.domain.BoardFactory;
import com.desierto.domain.Cell;
import com.desierto.domain.Color;
import com.desierto.domain.pieces.King;
import com.desierto.domain.pieces.Queen;
import org.junit.jupiter.api.Test;

class StandardPositionEvaluatorTest {

  private final StandardPositionEvaluator evaluator = new StandardPositionEvaluator();

  @Test
  void shouldScoreCheckmateFromTheRequestedPerspective() {
    Board board = BoardFactory.withPieces(
        BoardFactory.placement(new King(Color.WHITE, Cell.H1), Cell.H1),
        BoardFactory.placement(new Queen(Color.BLACK, Cell.G2), Cell.G2),
        BoardFactory.placement(new King(Color.BLACK, Cell.F3), Cell.F3));

    assertEquals(-StandardPositionEvaluator.CHECKMATE_SCORE, evaluator.evaluate(board, Color.WHITE));
    assertEquals(StandardPositionEvaluator.CHECKMATE_SCORE, evaluator.evaluate(board, Color.BLACK));
  }

  @Test
  void shouldReduceTheMateScoreByTheDistanceFromTheRoot() {
    Board board = BoardFactory.withPieces(
        BoardFactory.placement(new King(Color.WHITE, Cell.H1), Cell.H1),
        BoardFactory.placement(new Queen(Color.BLACK, Cell.G2), Cell.G2),
        BoardFactory.placement(new King(Color.BLACK, Cell.F3), Cell.F3));

    assertEquals(-99_997, evaluator.evaluate(board, Color.WHITE, 3));
    assertEquals(99_997, evaluator.evaluate(board, Color.BLACK, 3));
  }

  @Test
  void shouldScoreStalemateAsNeutral() {
    Board board = BoardFactory.withPieces(
        BoardFactory.placement(new King(Color.WHITE, Cell.A1), Cell.A1),
        BoardFactory.placement(new Queen(Color.BLACK, Cell.C2), Cell.C2),
        BoardFactory.placement(new King(Color.BLACK, Cell.C3), Cell.C3));

    assertEquals(0, evaluator.evaluate(board, Color.WHITE));
    assertEquals(0, evaluator.evaluate(board, Color.BLACK));
  }
}
