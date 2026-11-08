package com.desierto.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.desierto.domain.Board;
import com.desierto.domain.BoardFactory;
import com.desierto.domain.Cell;
import com.desierto.domain.Color;
import com.desierto.domain.Move;
import com.desierto.domain.pieces.King;
import com.desierto.domain.pieces.Queen;
import com.desierto.domain.pieces.Rook;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AlphaBetaSearchTest {

  @Test
  void shouldMatchMinimaxWhileExploringFewerNodes() {
    Board board = Board.standard();
    SearchLimits limits = new SearchLimits(2);

    SearchResult minimaxResult = new MinimaxSearch().findBestMove(board, limits);
    SearchResult alphaBetaResult = new AlphaBetaSearch().findBestMove(board, limits);

    assertEquals(minimaxResult.bestMove(), alphaBetaResult.bestMove());
    assertEquals(minimaxResult.score(), alphaBetaResult.score());
    assertEquals(minimaxResult.depth(), alphaBetaResult.depth());
    assertTrue(alphaBetaResult.nodes() < minimaxResult.nodes());
  }

  @Test
  void shouldReduceNodesComparedWithUnorderedAlphaBeta() {
    Board board = BoardFactory.position()
        .castlingRights(false, false, false, false)
        .place(new King(Color.WHITE, Cell.E1), Cell.E1)
        .place(new Queen(Color.WHITE, Cell.A1), Cell.A1)
        .place(new Rook(Color.BLACK, Cell.A8), Cell.A8)
        .place(new King(Color.BLACK, Cell.E8), Cell.E8)
        .build();
    SearchLimits limits = new SearchLimits(2);
    MoveOrderingStrategy unorderedStrategy = (position, moves, preferredMove) -> List.copyOf(moves);

    SearchResult unorderedResult = new AlphaBetaSearch(
        new com.desierto.search.evaluation.StandardPositionEvaluator(),
        new com.desierto.domain.MoveGenerator(), unorderedStrategy, false).findBestMove(board, limits);
    SearchResult orderedResult = new AlphaBetaSearch(false).findBestMove(board, limits);

    assertEquals(MoveKey.from(unorderedResult.bestMove().orElseThrow()),
        MoveKey.from(orderedResult.bestMove().orElseThrow()));
    assertEquals(unorderedResult.score(), orderedResult.score());
    assertTrue(orderedResult.nodes() < unorderedResult.nodes());
  }

  @Test
  void shouldUseAnEarlierBestMoveOnlyAtTheRoot() {
    Board board = Board.standard();
    AlphaBetaSearch firstEngine = new AlphaBetaSearch();
    MoveKey earlierBestMove = MoveKey.from(firstEngine.findBestMove(board, new SearchLimits(1))
        .bestMove().orElseThrow());
    List<Optional<MoveKey>> receivedPreferences = new ArrayList<>();
    List<MoveKey> firstOrderedMoves = new ArrayList<>();
    TacticalMoveOrderingStrategy tacticalStrategy = new TacticalMoveOrderingStrategy();
    MoveOrderingStrategy recordingStrategy = (position, moves, preferredMove) -> {
      receivedPreferences.add(preferredMove);
      var orderedMoves = tacticalStrategy.order(position, moves, preferredMove);
      firstOrderedMoves.add(MoveKey.from(orderedMoves.getFirst().move()));
      return orderedMoves;
    };
    AlphaBetaSearch engine = new AlphaBetaSearch(
        new com.desierto.search.evaluation.StandardPositionEvaluator(),
        new com.desierto.domain.MoveGenerator(), recordingStrategy);

    SearchResult result = engine.findBestMove(board, new SearchLimits(2),
        Optional.of(earlierBestMove));
    SearchResult baseline = firstEngine.findBestMove(board, new SearchLimits(2));

    assertEquals(Optional.of(earlierBestMove), receivedPreferences.getFirst());
    assertEquals(earlierBestMove, firstOrderedMoves.getFirst());
    assertTrue(receivedPreferences.size() > 1);
    assertTrue(receivedPreferences.subList(1, receivedPreferences.size()).stream()
        .allMatch(Optional::isEmpty));
    assertEquals(baseline.score(), result.score());
    assertEquals(baseline.depth(), result.depth());
  }

  @Test
  void shouldIgnoreAnEarlierMoveThatIsNotLegalInTheCurrentPosition() {
    Board board = Board.standard();
    MoveKey staleMove = new MoveKey(Color.WHITE, Cell.A1, Cell.A8,
        com.desierto.domain.MoveType.NORMAL, null);
    AlphaBetaSearch engine = new AlphaBetaSearch();

    SearchResult result = engine.findBestMove(board, new SearchLimits(1), Optional.of(staleMove));
    SearchResult baseline = engine.findBestMove(board, new SearchLimits(1));

    assertEquals(baseline, result);
  }

  @Test
  void shouldRejectANullPreferredMoveOptional() {
    AlphaBetaSearch engine = new AlphaBetaSearch();

    assertThrows(NullPointerException.class,
        () -> engine.findBestMove(Board.standard(), new SearchLimits(1), null));
  }
}
