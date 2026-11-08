package com.desierto.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.desierto.domain.Board;
import com.desierto.domain.BoardFactory;
import com.desierto.domain.Cell;
import com.desierto.domain.Color;
import com.desierto.domain.Move;
import com.desierto.domain.MoveGenerator;
import com.desierto.domain.MoveNotation;
import com.desierto.domain.pieces.King;
import com.desierto.domain.pieces.Pawn;
import com.desierto.domain.pieces.Rook;
import com.desierto.search.evaluation.StandardPositionEvaluator;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class ManagedSearchPolicyTest {
  @Test
  void forcedMoveStopsAfterACompletedIterationButFixedDepthDoesNot() {
    Board board = forcedPosition(0);
    assertEquals(1, new MoveGenerator().generateLegalMoves(board, Color.BLACK).size());
    SearchResult managed = new Engine().thinkManaged(board, new SearchLimits(3), Duration.ofSeconds(30));
    SearchResult fixed = new Engine().think(board, new SearchLimits(3));
    assertEquals(1, managed.depth());
    assertEquals(3, fixed.depth());
    assertEquals(fixed.bestMove(), managed.bestMove());
  }

  @Test
  void doesNotStopOnAForcedMoveWhenADrawClaimIsAvailable() {
    Board board = forcedPosition(100);
    Move move = new MoveGenerator().generateLegalMoves(board, Color.BLACK).getFirst();
    ManagedSearchPolicy policy = new ManagedSearchPolicy(board, 1000);
    assertFalse(policy.shouldStop(result(move, 1, 200, 0), 1000));
    SearchResult result = new Engine().thinkManaged(board, new SearchLimits(2), Duration.ofSeconds(30));
    assertEquals(2, result.depth());
    assertTrue(result.drawClaim().isPresent());
    Board intendedClaim = forcedPosition(99);
    ManagedSearchPolicy intendedPolicy = new ManagedSearchPolicy(intendedClaim, 1000);
    Move intended = new MoveGenerator().generateLegalMoves(intendedClaim, Color.BLACK).getFirst();
    assertFalse(intendedPolicy.shouldStop(result(intended, 1, 200, 0), 1000));
  }

  @Test
  void softDeadlineInterruptsTheNextIterationAndKeepsTheCompletedResult() {
    Board board = quietPosition(0);
    String initial = board.toString();
    AtomicLong clock = new AtomicLong();
    int[] rootIterations = {0};
    TacticalMoveOrderingStrategy tactical = new TacticalMoveOrderingStrategy();
    MoveOrderingStrategy ordering = (position, moves, preferred) -> {
      if (position.getSideToMove() == board.getSideToMove()
          && position.toString().equals(initial) && ++rootIterations[0] == 3) {
        clock.set(750);
      }
      return tactical.order(position, moves, preferred);
    };
    IterativeDeepeningSearch search = new IterativeDeepeningSearch(
        new AlphaBetaSearch(new StandardPositionEvaluator(), new MoveGenerator(), ordering),
        clock::get);
    List<SearchResult> iterations = new ArrayList<>();
    SearchResult result = search.findBestMoveManaged(board, new SearchLimits(5), () -> false,
        iterations::add, 1000);
    assertEquals(2, iterations.size());
    assertEquals(2, result.depth());
    assertEquals(iterations.getLast().bestMove(), result.bestMove());
    assertTrue(result.nodes() > iterations.getLast().nodes());
    assertEquals(initial, board.toString());
  }

  @Test
  void reducesSoftBudgetOnlyForQuietLowMobilityPositions() {
    Board quiet = quietPosition(0);
    Move move = new MoveGenerator().generateLegalMoves(quiet, Color.WHITE).getFirst();
    ManagedSearchPolicy policy = new ManagedSearchPolicy(quiet, 1000);
    assertFalse(policy.shouldStop(result(move, 1, 200, 0), 900));
    assertFalse(policy.shouldCancel(900));
    assertFalse(policy.shouldStop(result(move, 2, 200, 0), 749));
    assertFalse(policy.shouldCancel(749));
    assertTrue(policy.shouldCancel(750));
    assertTrue(policy.shouldStop(result(move, 3, 200, 0), 750));

    Board initial = Board.standard();
    Move initialMove = new MoveGenerator().generateLegalMoves(initial, Color.WHITE).getFirst();
    ManagedSearchPolicy busy = new ManagedSearchPolicy(initial, 1000);
    assertFalse(busy.shouldStop(result(initialMove, 2, 200, 0), 1000));
    assertFalse(busy.shouldCancel(1000));
  }

  @Test
  void stableClearLeadRequiresDepthTimeAndConsecutiveScores() {
    Board board = quietPosition(0);
    Move move = new MoveGenerator().generateLegalMoves(board, Color.WHITE).getFirst();
    ManagedSearchPolicy policy = new ManagedSearchPolicy(board, 1000);
    assertFalse(policy.shouldStop(result(move, 1, 200, 0), 100));
    assertFalse(policy.shouldStop(result(move, 2, 210, 0), 200));
    assertFalse(policy.shouldStop(result(move, 3, 205, 0), 500));
    assertFalse(policy.shouldStop(result(move, 4, 205, 150), 500));
    assertTrue(policy.shouldStop(result(move, 5, 205, 105), 500));

    ManagedSearchPolicy unstable = new ManagedSearchPolicy(board, 1000);
    assertFalse(unstable.shouldStop(result(move, 2, 200, 0), 400));
    assertFalse(unstable.shouldStop(result(move, 3, 250, 0), 500));
    assertFalse(unstable.shouldStop(result(move, 4, 250, 0), 500));
    assertTrue(unstable.shouldStop(result(move, 5, 250, 0), 500));
  }

  private static SearchResult result(Move move, int depth, int score, int runnerUp) {
    String notation = MoveNotation.format(move);
    SearchTrace trace = new SearchTrace(List.of(notation), List.of(
        new SearchTrace.Alternative(notation, score, SearchTrace.Bound.EXACT),
        new SearchTrace.Alternative("a1a2", runnerUp, SearchTrace.Bound.UPPER)));
    return new SearchResult(Optional.of(move), move.color(), score, depth, 1, Optional.empty(), trace);
  }

  private static Board forcedPosition(int halfmoveClock) {
    return BoardFactory.position().sideToMove(Color.BLACK)
        .castlingRights(false, false, false, false).halfmoveClock(halfmoveClock)
        .place(new King(Color.BLACK, Cell.H8), Cell.H8)
        .place(new King(Color.WHITE, Cell.F6), Cell.F6)
        .place(new Rook(Color.WHITE, Cell.H1), Cell.H1).build();
  }

  private static Board quietPosition(int halfmoveClock) {
    return BoardFactory.position().castlingRights(false, false, false, false)
        .halfmoveClock(halfmoveClock)
        .place(new King(Color.WHITE, Cell.E4), Cell.E4)
        .place(new Pawn(Color.WHITE, Cell.A2), Cell.A2)
        .place(new King(Color.BLACK, Cell.E8), Cell.E8)
        .place(new Pawn(Color.BLACK, Cell.H7), Cell.H7).build();
  }
}
