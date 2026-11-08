package com.desierto.search;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.desierto.domain.Board;
import com.desierto.domain.BoardFactory;
import com.desierto.domain.Cell;
import com.desierto.domain.Color;
import com.desierto.domain.MoveNotation;
import com.desierto.domain.pieces.King;
import com.desierto.domain.pieces.Pawn;
import com.desierto.domain.pieces.Queen;
import com.desierto.domain.pieces.Rook;
import org.junit.jupiter.api.Test;

class SearchBehaviorCharacterizationTest {

  @Test
  void fixedDepthSearchPreservesBaselineAcrossPositionsAndOptions() {
    for (boolean quiescence : new boolean[] {false, true}) {
      for (boolean transpositions : new boolean[] {false, true}) {
        assertSearch(Board.standard(), quiescence, transpositions, "b1c3", 0,
            quiescence ? 155 : 140);
        assertSearch(tacticalPosition(), quiescence, transpositions, "d1f3",
            quiescence ? 289 : 286, quiescence ? 269 : 96);
        assertSearch(endgamePosition(), quiescence, transpositions, "a5a6", 8, 30);
      }
    }
  }

  private static void assertSearch(Board board, boolean quiescence, boolean transpositions,
      String expectedMove, int expectedScore, long expectedNodes) {
    String before = board.toString();
    Color sideToMove = board.getSideToMove();
    int halfmoveClock = board.getHalfmoveClock();
    int fullmoveNumber = board.getFullmoveNumber();

    SearchResult result = new AlphaBetaSearch(quiescence, transpositions)
        .findBestMove(board, new SearchLimits(2));

    assertEquals(expectedMove, MoveNotation.format(result.bestMove().orElseThrow()));
    assertEquals(expectedScore, result.score());
    assertEquals(expectedNodes, result.nodes());
    assertEquals(2, result.depth());
    assertEquals(before, board.toString());
    assertEquals(sideToMove, board.getSideToMove());
    assertEquals(halfmoveClock, board.getHalfmoveClock());
    assertEquals(fullmoveNumber, board.getFullmoveNumber());
  }

  private static Board tacticalPosition() {
    return BoardFactory.position()
        .castlingRights(false, false, false, false)
        .place(new King(Color.WHITE, Cell.E1), Cell.E1)
        .place(new Queen(Color.WHITE, Cell.D1), Cell.D1)
        .place(new Rook(Color.BLACK, Cell.D5), Cell.D5)
        .place(new Pawn(Color.BLACK, Cell.E6), Cell.E6)
        .place(new King(Color.BLACK, Cell.E8), Cell.E8)
        .build();
  }

  private static Board endgamePosition() {
    return BoardFactory.position()
        .castlingRights(false, false, false, false)
        .place(new King(Color.WHITE, Cell.E4), Cell.E4)
        .place(new Pawn(Color.WHITE, Cell.A5), Cell.A5)
        .place(new King(Color.BLACK, Cell.E6), Cell.E6)
        .place(new Pawn(Color.BLACK, Cell.H4), Cell.H4)
        .build();
  }
}
