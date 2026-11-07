package com.desierto.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.desierto.domain.Board;
import com.desierto.domain.Color;
import com.desierto.domain.Move;
import com.desierto.domain.MoveGenerator;
import com.desierto.search.evaluation.PositionEvaluator;
import com.desierto.search.evaluation.StandardPositionEvaluator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class ThreefoldRepetitionSearchTest {
  private final MoveGenerator generator = new MoveGenerator();
  private final PositionEvaluator losing = (board, perspective) -> -1000;

  @Test
  void allEnginesEndImmediatelyOnTheThirdOccurrence() {
    Board board = almostRepeated();
    play(board, "f6g8");
    assertEquals(0, new StandardPositionEvaluator().evaluate(board, Color.WHITE));
    for (SearchEngine engine : List.of(new FixedDepthMinimaxEngine(),
        new FixedDepthAlphaBetaEngine(false, false), new FixedDepthAlphaBetaEngine(true, true),
        new IterativeDeepeningEngine())) {
      SearchResult result = engine.findBestMove(board, new SearchLimits(2));
      assertTrue(result.bestMove().isEmpty());
      assertTrue(result.drawClaim().isEmpty());
      assertEquals(0, result.score());
      assertEquals(0, result.depth());
    }
  }

  @Test
  void searchRecognizesThirdOccurrenceInAChildAsAnAutomaticDraw() {
    Board board = almostRepeated();
    for (boolean quiescence : new boolean[] {false, true}) {
      for (boolean cached : new boolean[] {false, true}) {
        SearchEngine engine = new FixedDepthAlphaBetaEngine(losing, generator,
            new TacticalMoveOrderingStrategy(), quiescence, cached);
        assertDrawMove(engine, board);
      }
    }
    assertDrawMove(new FixedDepthMinimaxEngine(losing, generator), board);
    FixedDepthAlphaBetaEngine fixed = new FixedDepthAlphaBetaEngine(losing, generator,
        new TacticalMoveOrderingStrategy(), true, true);
    assertDrawMove(new IterativeDeepeningEngine(fixed, () -> 0), board);
    assertFalse(board.isThreefoldRepetition());
  }

  @Test
  void transpositionKeyIncludesTheCompleteHistoryAndRestoresAfterUndo() {
    Board first = Board.standard();
    Board second = Board.standard();
    play(first, "g1f3", "g8f6", "f3g1", "f6g8");
    play(second, "g1h3", "g8f6", "h3g1", "f6g8");
    assertEquals(first.toString(), second.toString());
    assertEquals(first.getHalfmoveClock(), second.getHalfmoveClock());
    assertNotEquals(PositionHash.of(first), PositionHash.of(second));
    long hash = PositionHash.of(first);
    play(first, "g1f3");
    first.undoMove();
    assertEquals(hash, PositionHash.of(first));
  }

  @Test
  void deadlineUnwindsTheSearchHistoryWithoutChangingTheSource() {
    Board board = almostRepeated();
    long before = board.repetitionHistoryFingerprint();
    AtomicBoolean expired = new AtomicBoolean();
    Board[] working = {null};
    TacticalMoveOrderingStrategy tactical = new TacticalMoveOrderingStrategy();
    MoveOrderingStrategy strategy = (position, moves, preferred) -> {
      working[0] = position;
      if (position.getSideToMove() != board.getSideToMove()) {
        expired.set(true);
      }
      return tactical.order(position, moves, preferred);
    };
    FixedDepthAlphaBetaEngine engine = new FixedDepthAlphaBetaEngine(losing, generator, strategy,
        false, true);
    FixedDepthAlphaBetaEngine.SearchContext context =
        new FixedDepthAlphaBetaEngine.SearchContext(expired::get);
    assertThrows(FixedDepthAlphaBetaEngine.SearchDeadlineExceeded.class,
        () -> engine.findBestMove(board, new SearchLimits(2), java.util.Optional.empty(), context));
    assertEquals(before, board.repetitionHistoryFingerprint());
    assertEquals(before, working[0].repetitionHistoryFingerprint());
    assertEquals(board.toString(), working[0].toString());
    assertDrawMove(new FixedDepthMinimaxEngine(losing, generator), board);
  }

  private void assertDrawMove(SearchEngine engine, Board board) {
    SearchResult result = engine.findBestMove(board, new SearchLimits(1));
    Move move = result.bestMove().orElseThrow();
    assertEquals("F6G8", move.from().toAlgebraic() + move.to().toAlgebraic());
    assertEquals(0, result.score());
    assertTrue(result.drawClaim().isEmpty());
  }

  private Board almostRepeated() {
    Board board = Board.standard();
    play(board, "g1f3", "g8f6", "f3g1", "f6g8", "g1f3", "g8f6", "f3g1");
    return board;
  }

  private void play(Board board, String... moves) {
    for (String notation : moves) {
      Move move = generator.generateLegalMoves(board, board.getSideToMove()).stream()
          .filter(candidate -> (candidate.from().toAlgebraic() + candidate.to().toAlgebraic())
              .equalsIgnoreCase(notation)).findFirst().orElseThrow();
      board.makeMove(move);
    }
  }
}
