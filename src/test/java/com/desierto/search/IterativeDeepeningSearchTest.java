package com.desierto.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.desierto.domain.Board;
import com.desierto.domain.BoardFactory;
import com.desierto.domain.Cell;
import com.desierto.domain.Color;
import com.desierto.domain.MoveGenerator;
import com.desierto.domain.pieces.King;
import com.desierto.domain.pieces.Queen;
import com.desierto.search.evaluation.PositionEvaluator;
import com.desierto.search.evaluation.StandardPositionEvaluator;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class IterativeDeepeningSearchTest {

  @Test
  void cancellationReturnsTheLastReportedIteration() {
    AtomicBoolean cancelled = new AtomicBoolean();
    List<SearchResult> iterations = new ArrayList<>();
    SearchResult result = new IterativeDeepeningSearch().findBestMove(Board.standard(),
        new SearchLimits(5), cancelled::get, completed -> {
          iterations.add(completed);
          cancelled.set(true);
        });
    assertEquals(1, iterations.size());
    assertEquals(iterations.getFirst().bestMove(), result.bestMove());
    assertEquals(1, result.depth());
  }

  @Test
  void shouldCompleteEveryDepthAndReuseThePreviousBestMove() {
    Board board = Board.standard();
    String initialBoard = board.toString();
    List<Optional<MoveKey>> rootPreferences = new ArrayList<>();
    TacticalMoveOrderingStrategy tactical = new TacticalMoveOrderingStrategy();
    MoveOrderingStrategy strategy = (position, moves, preferred) -> {
      if (position.getSideToMove() == board.getSideToMove()
          && position.toString().equals(initialBoard)) {
        rootPreferences.add(preferred);
      }
      return tactical.order(position, moves, preferred);
    };
    AlphaBetaSearch fixed = new AlphaBetaSearch(
        new StandardPositionEvaluator(), new MoveGenerator(), strategy);
    IterativeDeepeningSearch iterative = new IterativeDeepeningSearch(fixed, () -> 0);

    SearchResult depthOne = new AlphaBetaSearch()
        .findBestMove(board, new SearchLimits(1));
    SearchResult result = iterative.findBestMove(board, new SearchLimits(2), Duration.ofSeconds(1));
    SearchResult baseline = new AlphaBetaSearch()
        .findBestMove(board, new SearchLimits(2));

    assertEquals(List.of(Optional.empty(), depthOne.bestMove().map(MoveKey::from)), rootPreferences);
    assertEquals(baseline.score(), result.score());
    assertEquals(2, result.depth());
    assertTrue(result.nodes() > depthOne.nodes());
    assertEquals(initialBoard, board.toString());
    assertEquals(Color.WHITE, board.getSideToMove());
  }

  @Test
  void shouldReturnLastCompletedIterationWhenTimeExpires() {
    Board board = Board.standard();
    String initialBoard = board.toString();
    AtomicLong clock = new AtomicLong();
    TacticalMoveOrderingStrategy tactical = new TacticalMoveOrderingStrategy();
    int[] rootOrders = {0};
    MoveOrderingStrategy strategy = (position, moves, preferred) -> {
      var ordered = tactical.order(position, moves, preferred);
      if (position.getSideToMove() == board.getSideToMove()
          && position.toString().equals(initialBoard) && ++rootOrders[0] == 2) {
        clock.set(1);
      }
      return ordered;
    };
    IterativeDeepeningSearch engine = new IterativeDeepeningSearch(
        new AlphaBetaSearch(new StandardPositionEvaluator(), new MoveGenerator(), strategy),
        clock::get);
    SearchResult depthOne = new AlphaBetaSearch()
        .findBestMove(board, new SearchLimits(1));

    SearchResult result = engine.findBestMove(board, new SearchLimits(3), Duration.ofNanos(1));

    assertEquals(depthOne.bestMove(), result.bestMove());
    assertEquals(depthOne.score(), result.score());
    assertEquals(1, result.depth());
    assertEquals(depthOne.nodes() + 1, result.nodes());
  }

  @Test
  void shouldReportTimeoutBeforeAnyIterationCompletes() {
    AtomicLong clock = new AtomicLong();
    IterativeDeepeningSearch engine = new IterativeDeepeningSearch(
        new AlphaBetaSearch(), clock::getAndIncrement);

    assertThrows(SearchTimeoutException.class,
        () -> engine.findBestMove(Board.standard(), new SearchLimits(2), Duration.ofNanos(1)));
  }

  @Test
  void shouldRejectAnIterationThatFinishesEvaluatingAfterTheDeadline() {
    Board board = Board.standard();
    String initialBoard = board.toString();
    AtomicLong clock = new AtomicLong();
    AtomicReference<Board> workingBoard = new AtomicReference<>();
    PositionEvaluator evaluator = (position, perspective) -> {
      workingBoard.set(position);
      clock.set(1);
      return 0;
    };
    IterativeDeepeningSearch engine = new IterativeDeepeningSearch(
        new AlphaBetaSearch(evaluator, new MoveGenerator()), clock::get);

    assertThrows(SearchTimeoutException.class,
        () -> engine.findBestMove(board, new SearchLimits(1), Duration.ofNanos(1)));
    assertEquals(initialBoard, board.toString());
    assertEquals(Color.WHITE, board.getSideToMove());
    assertEquals(initialBoard, workingBoard.get().toString());
    assertEquals(Color.WHITE, workingBoard.get().getSideToMove());
  }

  @Test
  void shouldSupportSearchWithoutATimeLimit() {
    Board board = Board.standard();
    SearchLimits limits = new SearchLimits(2);

    SearchResult result = new IterativeDeepeningSearch().findBestMove(board, limits);
    SearchResult baseline = new AlphaBetaSearch().findBestMove(board, limits);

    assertEquals(2, result.depth());
    assertEquals(baseline.score(), result.score());
    assertTrue(result.nodes() > baseline.nodes());
  }

  @Test
  void shouldReturnTerminalPositionsWithoutAnotherIteration() {
    Board board = BoardFactory.position()
        .sideToMove(Color.BLACK)
        .castlingRights(false, false, false, false)
        .place(new King(Color.BLACK, Cell.H8), Cell.H8)
        .place(new Queen(Color.WHITE, Cell.G7), Cell.G7)
        .place(new King(Color.WHITE, Cell.F6), Cell.F6)
        .build();

    SearchResult result = new IterativeDeepeningSearch().findBestMove(board, new SearchLimits(3));

    assertTrue(result.bestMove().isEmpty());
    assertEquals(0, result.depth());
    assertEquals(1, result.nodes());
    assertEquals(-StandardPositionEvaluator.CHECKMATE_SCORE, result.score());
  }

  @Test
  void shouldValidateTheTimeLimit() {
    IterativeDeepeningSearch engine = new IterativeDeepeningSearch();
    Board board = Board.standard();
    SearchLimits limits = new SearchLimits(1);

    assertThrows(NullPointerException.class, () -> engine.findBestMove(board, limits, null));
    assertThrows(IllegalArgumentException.class,
        () -> engine.findBestMove(board, limits, Duration.ZERO));
    assertThrows(IllegalArgumentException.class,
        () -> engine.findBestMove(board, limits, Duration.ofNanos(-1)));
    assertThrows(IllegalArgumentException.class,
        () -> engine.findBestMove(board, limits, Duration.ofSeconds(Long.MAX_VALUE)));
  }
}
