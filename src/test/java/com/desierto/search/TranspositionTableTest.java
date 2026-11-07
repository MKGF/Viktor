package com.desierto.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.desierto.domain.Board;
import com.desierto.domain.BoardFactory;
import com.desierto.domain.Cell;
import com.desierto.domain.Color;
import com.desierto.domain.MoveGenerator;
import com.desierto.domain.pieces.King;
import com.desierto.domain.pieces.Queen;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class TranspositionTableTest {

  @Test
  void shouldClassifyBoundsAndProtectAgainstSlotCollisions() {
    TranspositionTable table = new TranspositionTable(1);
    table.store(17, 3, 2, 5, 0, 10);
    assertEquals(TranspositionTable.Bound.EXACT, table.find(17, 3, 2).bound());
    assertTrue(table.find(17, 3, 2).canCutOff(0, 10));
    assertNull(table.find(17, 2, 2));
    assertNull(table.find(17, 3, 1));
    assertNull(table.find(18, 3, 2));

    table.store(18, 3, 2, 10, 0, 10);
    assertNull(table.find(17, 3, 2));
    assertEquals(TranspositionTable.Bound.LOWER, table.find(18, 3, 2).bound());
    assertTrue(table.find(18, 3, 2).canCutOff(0, 10));
    assertFalse(table.find(18, 3, 2).canCutOff(0, 11));
    table.store(19, 3, 2, 0, 0, 10);
    assertEquals(TranspositionTable.Bound.UPPER, table.find(19, 3, 2).bound());
    assertTrue(table.find(19, 3, 2).canCutOff(0, 10));
    assertFalse(table.find(19, 3, 2).canCutOff(-1, 10));
    assertThrows(IllegalArgumentException.class, () -> new TranspositionTable(0));
    assertThrows(IllegalArgumentException.class, () -> new TranspositionTable(3));
  }

  @Test
  void shouldPreserveScoresAcrossPositionsAndQuiescenceModes() {
    List<Board> positions = new ArrayList<>();
    Board board = Board.standard();
    Random random = new Random(631);
    MoveGenerator generator = new MoveGenerator();
    positions.add(board.copy());
    for (int i = 0; i < 6; i++) {
      var moves = generator.generateLegalMoves(board, board.getSideToMove());
      board.makeMove(moves.get(random.nextInt(moves.size())));
      positions.add(board.copy());
    }
    positions.add(BoardFactory.position()
        .castlingRights(false, false, false, false)
        .place(new King(Color.WHITE, Cell.F6), Cell.F6)
        .place(new Queen(Color.WHITE, Cell.G6), Cell.G6)
        .place(new King(Color.BLACK, Cell.H8), Cell.H8)
        .build());

    for (boolean quiescence : new boolean[] {false, true}) {
      for (Board position : positions) {
        SearchResult without = new FixedDepthAlphaBetaEngine(quiescence, false)
            .findBestMove(position, new SearchLimits(3));
        SearchResult with = new FixedDepthAlphaBetaEngine(quiescence, true)
            .findBestMove(position, new SearchLimits(3));
        assertEquals(without.score(), with.score());
        assertEquals(without.depth(), with.depth());
        assertTrue(generator.generateLegalMoves(position, position.getSideToMove()).stream()
            .anyMatch(move -> MoveKey.from(move).equals(MoveKey.from(with.bestMove().orElseThrow()))));
      }
    }
  }

  @Test
  void shouldReduceNodesForTranspositionsWithoutChangingScore() {
    Board board = Board.standard();
    SearchLimits limits = new SearchLimits(4);
    SearchResult without = new FixedDepthAlphaBetaEngine(false, false).findBestMove(board, limits);
    SearchResult with = new FixedDepthAlphaBetaEngine(false, true).findBestMove(board, limits);

    assertEquals(without.score(), with.score());
    assertTrue(with.nodes() < without.nodes(), "Transpositions should avoid exploring duplicate subtrees");
  }

  @Test
  void shouldPreserveQuiescenceScoresAtTranspositionDepth() {
    Board board = Board.standard();
    SearchLimits limits = new SearchLimits(4);
    SearchResult without = new FixedDepthAlphaBetaEngine(true, false).findBestMove(board, limits);
    SearchResult with = new FixedDepthAlphaBetaEngine(true, true).findBestMove(board, limits);

    assertEquals(without.score(), with.score());
    assertTrue(with.nodes() < without.nodes());
  }

  @Test
  void shouldPreserveIterativeSearchScores() {
    Board board = Board.standard();
    SearchResult without = new IterativeDeepeningEngine(false, false)
        .findBestMove(board, new SearchLimits(4));
    SearchResult with = new IterativeDeepeningEngine(false, true)
        .findBestMove(board, new SearchLimits(4));

    assertEquals(without.score(), with.score());
    assertEquals(without.depth(), with.depth());
    assertTrue(with.nodes() < without.nodes());
  }
}
