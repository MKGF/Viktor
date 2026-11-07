package com.desierto.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.desierto.domain.Board;
import com.desierto.domain.BoardFactory;
import com.desierto.domain.Cell;
import com.desierto.domain.Color;
import com.desierto.domain.DrawDetector;
import com.desierto.domain.MoveGenerator;
import com.desierto.domain.pieces.Bishop;
import com.desierto.domain.pieces.King;
import com.desierto.domain.pieces.Knight;
import com.desierto.search.evaluation.PositionEvaluator;
import com.desierto.search.evaluation.StandardPositionEvaluator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class InsufficientMaterialSearchTest {

  @Test
  void shouldTreatDeadPositionsAsTerminalInEveryEngine() {
    Board board = BoardFactory.position().castlingRights(false, false, false, false)
        .place(new King(Color.WHITE, Cell.A1), Cell.A1)
        .place(new Knight(Color.WHITE, Cell.B1), Cell.B1)
        .place(new King(Color.BLACK, Cell.H8), Cell.H8).build();
    assertEquals(0, new StandardPositionEvaluator().evaluate(board, Color.WHITE));
    assertEquals(0, new StandardPositionEvaluator().evaluate(board, Color.BLACK));

    for (SearchEngine engine : List.of(new FixedDepthMinimaxEngine(),
        new FixedDepthAlphaBetaEngine(false, false),
        new FixedDepthAlphaBetaEngine(true, true), new IterativeDeepeningEngine())) {
      SearchResult result = engine.findBestMove(board, new SearchLimits(3));
      assertTrue(result.bestMove().isEmpty());
      assertEquals(0, result.score());
      assertEquals(0, result.depth());
    }
  }

  @Test
  void shouldPreferAForcedDrawToALosingEvaluationWithinTheSearch() {
    PositionEvaluator losing = (board, perspective) -> -1000;
    for (SearchEngine engine : List.of(
        new FixedDepthMinimaxEngine(losing, new MoveGenerator()),
        new FixedDepthAlphaBetaEngine(losing, new MoveGenerator(), new TacticalMoveOrderingStrategy(), false, false),
        new FixedDepthAlphaBetaEngine(losing, new MoveGenerator(), new TacticalMoveOrderingStrategy(), true, true))) {
      Board board = BoardFactory.position().castlingRights(false, false, false, false)
          .place(new King(Color.WHITE, Cell.C3), Cell.C3)
          .place(new Knight(Color.WHITE, Cell.A1), Cell.A1)
          .place(new King(Color.BLACK, Cell.H8), Cell.H8)
          .place(new Bishop(Color.BLACK, Cell.D4), Cell.D4).build();

      SearchResult result = engine.findBestMove(board, new SearchLimits(2));

      assertEquals(0, result.score());
      assertEquals(Cell.D4, result.bestMove().orElseThrow().to());
      board.makeMove(result.bestMove().orElseThrow());
      assertTrue(DrawDetector.isDeadByInsufficientMaterial(board));
    }
  }

  @Test
  void shouldCheckTheDeadlineBeforeReturningADeadPosition() {
    Board board = BoardFactory.position().castlingRights(false, false, false, false)
        .place(new King(Color.WHITE, Cell.A1), Cell.A1)
        .place(new Knight(Color.WHITE, Cell.B1), Cell.B1)
        .place(new King(Color.BLACK, Cell.H8), Cell.H8).build();
    AtomicInteger checks = new AtomicInteger();
    var context = new FixedDepthAlphaBetaEngine.SearchContext(() -> checks.getAndIncrement() >= 2);

    assertThrows(FixedDepthAlphaBetaEngine.SearchDeadlineExceeded.class,
        () -> new FixedDepthAlphaBetaEngine().findBestMove(board, new SearchLimits(1),
            Optional.empty(), context));
  }
}
