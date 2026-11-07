package com.desierto.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.desierto.domain.Board;
import com.desierto.domain.BoardFactory;
import com.desierto.domain.Cell;
import com.desierto.domain.Color;
import com.desierto.domain.DrawDetector;
import com.desierto.domain.Move;
import com.desierto.domain.MoveGenerator;
import com.desierto.domain.pieces.King;
import com.desierto.domain.pieces.Pawn;
import com.desierto.domain.pieces.Queen;
import com.desierto.domain.pieces.Rook;
import com.desierto.search.evaluation.PositionEvaluator;
import com.desierto.search.evaluation.StandardPositionEvaluator;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class FiftyMoveRuleTest {
  private final MoveGenerator generator = new MoveGenerator();

  @Test
  void shouldApplyClaimAndAutomaticThresholdsWithoutConfusingPliesAndMoves() {
    assertFalse(DrawDetector.canClaimFiftyMoves(position(99)));
    assertTrue(DrawDetector.canClaimFiftyMoves(position(100)));
    assertFalse(DrawDetector.isSeventyFiveMoveDraw(position(149), generator));
    assertTrue(DrawDetector.isSeventyFiveMoveDraw(position(150), generator));
    assertFalse(DrawDetector.canClaimFiftyMovesAfter(position(98), find(position(98), Cell.B1, Cell.B2)));
    Board board = position(99);
    assertTrue(DrawDetector.canClaimFiftyMovesAfter(board, find(board, Cell.B1, Cell.B2)));
  }

  @Test
  void shouldNotAllowAClaimBasedOnAPawnMoveOrCapture() {
    Board board = BoardFactory.position().halfmoveClock(99)
        .castlingRights(false, false, false, false)
        .place(new King(Color.WHITE, Cell.A1), Cell.A1)
        .place(new Rook(Color.WHITE, Cell.B1), Cell.B1)
        .place(new Pawn(Color.WHITE, Cell.C2), Cell.C2)
        .place(new King(Color.BLACK, Cell.H8), Cell.H8)
        .place(new Rook(Color.BLACK, Cell.B8), Cell.B8).build();
    assertFalse(DrawDetector.canClaimFiftyMovesAfter(board, find(board, Cell.C2, Cell.C3)));
    assertFalse(DrawDetector.canClaimFiftyMovesAfter(board, find(board, Cell.B1, Cell.B8)));
    board.makeMove(find(board, Cell.C2, Cell.C3));
    assertEquals(0, board.getHalfmoveClock());
    board.undoMove();
    assertEquals(99, board.getHalfmoveClock());
  }

  @Test
  void checkmateTakesPrecedenceOverTheAutomaticDraw() {
    Board mate = BoardFactory.position().halfmoveClock(150).castlingRights(false, false, false, false)
        .place(new King(Color.WHITE, Cell.H1), Cell.H1)
        .place(new Queen(Color.BLACK, Cell.G2), Cell.G2)
        .place(new King(Color.BLACK, Cell.F3), Cell.F3).build();
    assertFalse(DrawDetector.isSeventyFiveMoveDraw(mate, generator));
    assertEquals(-StandardPositionEvaluator.CHECKMATE_SCORE,
        new StandardPositionEvaluator().evaluate(mate, Color.WHITE));
    for (SearchEngine engine : engines()) {
      SearchResult result = engine.findBestMove(mate, new SearchLimits(2));
      assertTrue(result.bestMove().isEmpty());
      assertTrue(result.drawClaim().isEmpty());
      assertEquals(-StandardPositionEvaluator.CHECKMATE_SCORE, result.score());
    }
  }

  @Test
  void shouldStopOnAutomaticDrawWithoutRequiringAClaim() {
    Board board = position(150);
    assertEquals(0, new StandardPositionEvaluator().evaluate(board, Color.WHITE));
    for (SearchEngine engine : engines()) {
      SearchResult result = engine.findBestMove(board, new SearchLimits(2));
      assertTrue(result.bestMove().isEmpty());
      assertTrue(result.drawClaim().isEmpty());
      assertEquals(0, result.score());
      assertEquals(0, result.depth());
    }
  }

  @Test
  void shouldTreatClaimAsAnOptionAndContinueWhenWinning() {
    PositionEvaluator losing = (board, perspective) -> -1000;
    PositionEvaluator winning = (board, perspective) -> 1000;
    for (int halfmoves : new int[] {99, 100}) {
      for (boolean quiescence : new boolean[] {false, true}) {
        for (boolean transpositions : new boolean[] {false, true}) {
          Board board = position(halfmoves);
          SearchEngine engine = new FixedDepthAlphaBetaEngine(losing, generator,
              new TacticalMoveOrderingStrategy(), quiescence, transpositions);
          SearchResult result = engine.findBestMove(board, new SearchLimits(1));
          assertEquals(0, result.score());
          assertTrue(result.bestMove().isEmpty());
          assertEquals(halfmoves == 99, result.drawClaim().orElseThrow().intendedMove().isPresent());
        }
      }
      SearchResult minimax = new FixedDepthMinimaxEngine(losing, generator)
          .findBestMove(position(halfmoves), new SearchLimits(1));
      assertTrue(minimax.drawClaim().isPresent());
      Board winningBoard = BoardFactory.position().halfmoveClock(halfmoves)
          .castlingRights(false, false, false, false)
          .place(new King(Color.WHITE, Cell.A1), Cell.A1)
          .place(new Rook(Color.WHITE, Cell.B1), Cell.B1)
          .place(new Pawn(Color.WHITE, Cell.C2), Cell.C2)
          .place(new King(Color.BLACK, Cell.H8), Cell.H8).build();
      SearchResult better = new FixedDepthMinimaxEngine(winning, generator)
          .findBestMove(winningBoard, new SearchLimits(1));
      assertTrue(better.bestMove().isPresent());
      assertTrue(better.drawClaim().isEmpty());
      assertEquals(1000, better.score());
      for (boolean quiescence : new boolean[] {false, true}) {
        SearchResult alphaBeta = new FixedDepthAlphaBetaEngine(winning, generator,
            new TacticalMoveOrderingStrategy(), quiescence, true)
            .findBestMove(winningBoard, new SearchLimits(1));
        assertTrue(alphaBeta.bestMove().isPresent());
        assertEquals(1000, alphaBeta.score());
      }
    }
  }

  @Test
  void shouldContinueIteratingAfterAnOptionalClaim() {
    PositionEvaluator evaluator = new PositionEvaluator() {
      @Override public int evaluate(Board board, Color perspective) { return -100; }
      @Override public int evaluate(Board board, Color perspective, int ply) {
        return ply == 1 ? -100 : 100;
      }
    };
    Board board = BoardFactory.position().halfmoveClock(100)
        .castlingRights(false, false, false, false)
        .place(new King(Color.WHITE, Cell.A1), Cell.A1)
        .place(new Rook(Color.WHITE, Cell.B1), Cell.B1)
        .place(new Pawn(Color.WHITE, Cell.C2), Cell.C2)
        .place(new King(Color.BLACK, Cell.H8), Cell.H8).build();
    FixedDepthAlphaBetaEngine fixed = new FixedDepthAlphaBetaEngine(evaluator, generator,
        new TacticalMoveOrderingStrategy(), false, true);
    SearchResult result = new IterativeDeepeningEngine(fixed, () -> 0)
        .findBestMove(board, new SearchLimits(2));

    assertEquals(2, result.depth());
    assertTrue(result.bestMove().isPresent());
    assertTrue(result.bestMove().orElseThrow().piece() instanceof Pawn);
    assertEquals(100, result.score());
  }

  @Test
  void transpositionHashMustIncludeTheHalfmoveClock() {
    assertNotEquals(PositionHash.of(position(98)), PositionHash.of(position(99)));
    assertNotEquals(PositionHash.of(position(99)), PositionHash.of(position(100)));
    assertNotEquals(PositionHash.of(position(149)), PositionHash.of(position(150)));
    assertEquals(PositionHash.of(position(150)), PositionHash.of(position(151)));
  }

  @Test
  void shouldPreferMateOnTheHundredAndFiftiethHalfmove() {
    Board board = BoardFactory.position().sideToMove(Color.BLACK).halfmoveClock(149)
        .castlingRights(false, false, false, false)
        .place(new King(Color.WHITE, Cell.H1), Cell.H1)
        .place(new Queen(Color.BLACK, Cell.G3), Cell.G3)
        .place(new King(Color.BLACK, Cell.F3), Cell.F3).build();
    for (SearchEngine engine : engines()) {
      SearchResult result = engine.findBestMove(board, new SearchLimits(1));
      assertTrue(result.bestMove().isPresent());
      assertTrue(result.score() > 0);
      Board after = board.copy();
      after.makeMove(result.bestMove().orElseThrow());
      assertEquals(150, after.getHalfmoveClock());
      assertTrue(generator.isCheckmate(after, Color.WHITE));
    }
  }

  @Test
  void shouldPreserveTheCompletedClaimWhenAnIterationTimesOut() {
    AtomicLong clock = new AtomicLong();
    int[] rootOrders = {0};
    TacticalMoveOrderingStrategy tactical = new TacticalMoveOrderingStrategy();
    MoveOrderingStrategy strategy = (board, moves, preferred) -> {
      if (board.getSideToMove() == Color.WHITE && board.getHalfmoveClock() == 100
          && ++rootOrders[0] == 2) {
        clock.set(1);
      }
      return tactical.order(board, moves, preferred);
    };
    FixedDepthAlphaBetaEngine fixed = new FixedDepthAlphaBetaEngine(
        (board, perspective) -> -1000, generator, strategy, false, false);
    SearchResult result = new IterativeDeepeningEngine(fixed, clock::get)
        .findBestMove(position(100), new SearchLimits(3), Duration.ofNanos(1));

    assertEquals(1, result.depth());
    assertTrue(result.bestMove().isEmpty());
    assertTrue(result.drawClaim().isPresent());
  }

  private List<SearchEngine> engines() {
    return List.of(new FixedDepthMinimaxEngine(), new FixedDepthAlphaBetaEngine(false, false),
        new FixedDepthAlphaBetaEngine(true, true), new IterativeDeepeningEngine());
  }

  private Board position(int clock) {
    return BoardFactory.position().halfmoveClock(clock).castlingRights(false, false, false, false)
        .place(new King(Color.WHITE, Cell.A1), Cell.A1)
        .place(new Rook(Color.WHITE, Cell.B1), Cell.B1)
        .place(new King(Color.BLACK, Cell.H8), Cell.H8).build();
  }

  private Move find(Board board, Cell from, Cell to) {
    return generator.generateLegalMoves(board, board.getSideToMove()).stream()
        .filter(move -> move.from().equals(from) && move.to().equals(to)).findFirst().orElseThrow();
  }
}
