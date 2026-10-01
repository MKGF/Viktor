package com.desierto.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.desierto.domain.Board;
import com.desierto.domain.BoardFactory;
import com.desierto.domain.Cell;
import com.desierto.domain.Color;
import com.desierto.domain.Move;
import com.desierto.domain.pieces.King;
import com.desierto.domain.pieces.Queen;
import com.desierto.domain.pieces.Rook;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class FixedDepthAlphaBetaEngineTest {

  @Test
  void shouldMatchMinimaxWhileExploringFewerNodes() {
    Board board = Board.standard();
    SearchLimits limits = new SearchLimits(2);

    SearchResult minimaxResult = new FixedDepthMinimaxEngine().findBestMove(board, limits);
    SearchResult alphaBetaResult = new FixedDepthAlphaBetaEngine().findBestMove(board, limits);

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

    SearchResult unorderedResult = new FixedDepthAlphaBetaEngine(
        new com.desierto.search.evaluation.StandardPositionEvaluator(),
        new com.desierto.domain.MoveGenerator(), unorderedStrategy).findBestMove(board, limits);
    SearchResult orderedResult = new FixedDepthAlphaBetaEngine().findBestMove(board, limits);

    assertEquals(MoveKey.from(unorderedResult.bestMove().orElseThrow()),
        MoveKey.from(orderedResult.bestMove().orElseThrow()));
    assertEquals(unorderedResult.score(), orderedResult.score());
    assertTrue(orderedResult.nodes() < unorderedResult.nodes());
  }
}
