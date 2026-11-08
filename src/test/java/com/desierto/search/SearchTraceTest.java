package com.desierto.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.desierto.domain.Board;
import com.desierto.domain.Move;
import com.desierto.domain.MoveGenerator;
import com.desierto.domain.MoveNotation;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class SearchTraceTest {
  private final MoveGenerator generator = new MoveGenerator();

  @Test
  void principalVariationContainsLegalMovesStartingWithTheSelectedMove() {
    for (boolean transpositions : new boolean[] {false, true}) {
      Board board = Board.standard();
      String initial = board.toString();
      SearchResult result = new AlphaBetaSearch(false, transpositions)
          .findBestMove(board, new SearchLimits(3));

      List<String> pv = result.trace().principalVariation();
      assertEquals(MoveNotation.format(result.bestMove().orElseThrow()), pv.getFirst());
      assertEquals(3, pv.size());
      for (String notation : pv) {
        Move move = MoveNotation.find(notation,
            generator.generateLegalMoves(board, board.getSideToMove())).orElseThrow();
        board.makeMove(move);
      }
      assertFalse(initial.equals(board.toString()));
    }
  }

  @Test
  void alternativeScoresAreExactOrValidUpperBounds() {
    Board board = Board.standard();
    for (boolean transpositions : new boolean[] {false, true}) {
      SearchResult result = new AlphaBetaSearch(false, transpositions)
          .findBestMove(board, new SearchLimits(2));
      List<SearchTrace.Alternative> alternatives = result.trace().alternatives();
      assertEquals(3, alternatives.size());
      assertEquals(MoveNotation.format(result.bestMove().orElseThrow()), alternatives.getFirst().move());
      assertEquals(result.score(), alternatives.getFirst().score());
      assertEquals(SearchTrace.Bound.EXACT, alternatives.getFirst().bound());
      for (SearchTrace.Alternative candidate : alternatives) {
        Board child = board.copy();
        Move move = MoveNotation.find(candidate.move(),
            generator.generateLegalMoves(child, child.getSideToMove())).orElseThrow();
        child.makeMove(move);
        int exact = -new MinimaxSearch().findBestMove(child, new SearchLimits(1)).score();
        if (candidate.bound() == SearchTrace.Bound.EXACT) {
          assertEquals(exact, candidate.score(), candidate.move());
        } else {
          assertTrue(candidate.score() >= exact, candidate.move() + ": " + candidate.score() + " < " + exact);
        }
      }
    }
  }

  @Test
  void cancelledIterationRetainsTheLastCompletedTrace() {
    AtomicBoolean cancelled = new AtomicBoolean();
    List<SearchResult> completed = new ArrayList<>();
    SearchResult result = new IterativeDeepeningSearch().findBestMove(Board.standard(),
        new SearchLimits(4), cancelled::get, iteration -> {
          completed.add(iteration);
          cancelled.set(true);
        });
    assertEquals(1, completed.size());
    assertEquals(completed.getFirst().trace(), result.trace());
    assertFalse(result.trace().principalVariation().isEmpty());
    assertEquals(3, result.trace().alternatives().size());
  }
}
