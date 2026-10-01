package com.desierto.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.desierto.domain.Board;
import com.desierto.domain.BoardFactory;
import com.desierto.domain.Cell;
import com.desierto.domain.Color;
import com.desierto.domain.pieces.King;
import com.desierto.domain.pieces.Pawn;
import com.desierto.domain.pieces.Queen;
import com.desierto.domain.pieces.Rook;
import com.desierto.search.evaluation.StandardPositionEvaluator;
import org.junit.jupiter.api.Test;

class FixedDepthMinimaxEngineTest {

  private final SearchEngine engine = new FixedDepthMinimaxEngine();

  @Test
  void shouldChooseTheHighestValueCaptureAtDepthOne() {
    Board board = BoardFactory.position()
        .castlingRights(false, false, false, false)
        .place(new King(Color.WHITE, Cell.E1), Cell.E1)
        .place(new Queen(Color.WHITE, Cell.A1), Cell.A1)
        .place(new Rook(Color.BLACK, Cell.A8), Cell.A8)
        .place(new King(Color.BLACK, Cell.E8), Cell.E8)
        .build();

    SearchResult result = engine.findBestMove(board, new SearchLimits(1));

    assertEquals(Cell.A1, result.bestMove().orElseThrow().from());
    assertEquals(Cell.A8, result.bestMove().orElseThrow().to());
    assertEquals(900, result.score());
    assertEquals(Color.WHITE, result.perspective());
    assertEquals(1, result.depth());
    assertTrue(result.nodes() > 1);
  }

  @Test
  void shouldAvoidACaptureThatCanBeRecapturedAtDepthTwo() {
    Board board = BoardFactory.position()
        .castlingRights(false, false, false, false)
        .place(new King(Color.WHITE, Cell.E1), Cell.E1)
        .place(new Queen(Color.WHITE, Cell.D1), Cell.D1)
        .place(new Rook(Color.BLACK, Cell.D5), Cell.D5)
        .place(new Pawn(Color.BLACK, Cell.E6), Cell.E6)
        .place(new King(Color.BLACK, Cell.E8), Cell.E8)
        .build();

    SearchResult result = engine.findBestMove(board, new SearchLimits(2));

    assertFalse(result.bestMove().orElseThrow().to().equals(Cell.D5));
    assertEquals(300, result.score());
    assertEquals(2, result.depth());
  }

  @Test
  void shouldReturnNoMoveForATerminalPosition() {
    Board board = BoardFactory.position()
        .sideToMove(Color.BLACK)
        .castlingRights(false, false, false, false)
        .place(new King(Color.BLACK, Cell.H8), Cell.H8)
        .place(new Queen(Color.WHITE, Cell.G7), Cell.G7)
        .place(new King(Color.WHITE, Cell.F6), Cell.F6)
        .build();

    SearchResult result = engine.findBestMove(board, new SearchLimits(3));

    assertTrue(result.bestMove().isEmpty());
    assertEquals(Color.BLACK, result.perspective());
    assertEquals(-StandardPositionEvaluator.CHECKMATE_SCORE, result.score());
    assertEquals(0, result.depth());
    assertEquals(1, result.nodes());
  }

  @Test
  void shouldChooseAMateInOneWithItsDistanceAdjustedScore() {
    Board board = BoardFactory.position()
        .castlingRights(false, false, false, false)
        .place(new King(Color.WHITE, Cell.F6), Cell.F6)
        .place(new Queen(Color.WHITE, Cell.G6), Cell.G6)
        .place(new King(Color.BLACK, Cell.H8), Cell.H8)
        .build();

    SearchResult result = engine.findBestMove(board, new SearchLimits(1));

    assertEquals(Cell.G6, result.bestMove().orElseThrow().from());
    assertEquals(Cell.G7, result.bestMove().orElseThrow().to());
    assertEquals(StandardPositionEvaluator.CHECKMATE_SCORE - 1, result.score());
  }

  @Test
  void shouldNotMutateThePositionItSearches() {
    Board board = BoardFactory.position()
        .castlingRights(false, false, false, false)
        .place(new King(Color.WHITE, Cell.E1), Cell.E1)
        .place(new Queen(Color.WHITE, Cell.A1), Cell.A1)
        .place(new Rook(Color.BLACK, Cell.A8), Cell.A8)
        .place(new King(Color.BLACK, Cell.E8), Cell.E8)
        .build();
    String boardBeforeSearch = board.toString();

    engine.findBestMove(board, new SearchLimits(2));

    assertEquals(boardBeforeSearch, board.toString());
    assertEquals(Color.WHITE, board.getSideToMove());
    assertTrue(board.getPieceAt(Cell.A1).orElseThrow() instanceof Queen);
    assertTrue(board.getPieceAt(Cell.A8).orElseThrow() instanceof Rook);
  }
}
