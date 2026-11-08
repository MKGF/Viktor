package com.desierto.search;

import static com.desierto.search.SearchTrace.Bound.EXACT;
import static com.desierto.search.SearchTrace.Bound.UPPER;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.desierto.domain.Board;
import com.desierto.domain.BoardFactory;
import com.desierto.domain.Cell;
import com.desierto.domain.Color;
import com.desierto.domain.pieces.King;
import com.desierto.domain.pieces.Pawn;
import com.desierto.domain.pieces.Queen;
import com.desierto.domain.pieces.Rook;
import java.util.List;
import org.junit.jupiter.api.Test;

class AlphaBetaTraceCharacterizationTest {

  @Test
  void preservesPrincipalVariationsAndRankedAlternativesAcrossSearchOptions() {
    SearchTrace initial = new SearchTrace(List.of("b1c3", "b8c6"), List.of(
        alternative("b1c3", 0, EXACT), alternative("g1f3", 0, UPPER),
        alternative("a2a3", 0, UPPER)));
    SearchTrace tactical = new SearchTrace(List.of("d1f3", "e6e5"), List.of(
        alternative("d1f3", 286, EXACT), alternative("d1g4", 286, UPPER),
        alternative("d1c2", 286, UPPER)));
    SearchTrace tacticalWithQuiescence = new SearchTrace(List.of("d1f3", "e8e7"), List.of(
        alternative("d1f3", 289, EXACT), alternative("d1b1", 288, UPPER),
        alternative("d1c1", 286, UPPER)));
    SearchTrace endgame = new SearchTrace(List.of("a5a6", "h4h3"), List.of(
        alternative("a5a6", 8, EXACT), alternative("e4d4", 2, EXACT),
        alternative("e4e3", -6, UPPER)));

    for (boolean quiescence : new boolean[] {false, true}) {
      for (boolean transpositions : new boolean[] {false, true}) {
        assertTrace(Board.standard(), quiescence, transpositions, 2, initial);
        assertTrace(tacticalPosition(), quiescence, transpositions, 2,
            quiescence ? tacticalWithQuiescence : tactical);
        assertTrace(endgamePosition(), quiescence, transpositions, 2, endgame);
      }
    }
  }

  @Test
  void preservesDeeperPrincipalVariationWithAndWithoutTranspositions() {
    SearchTrace expected = new SearchTrace(List.of("b1c3", "b8c6", "g1f3"), List.of(
        alternative("b1c3", 44, EXACT), alternative("g1f3", 44, UPPER),
        alternative("a2a3", 44, UPPER)));
    for (boolean transpositions : new boolean[] {false, true}) {
      assertTrace(Board.standard(), false, transpositions, 3, expected);
    }
  }

  @Test
  void preservesTranspositionCutoffsAndTheirTrace() {
    SearchTrace expected = new SearchTrace(List.of("b1c3", "b8c6", "g1f3", "g8f6"), List.of(
        alternative("b1c3", 0, EXACT), alternative("g1f3", 0, UPPER),
        alternative("a2a3", 0, UPPER)));
    for (boolean transpositions : new boolean[] {false, true}) {
      SearchResult result = new AlphaBetaSearch(false, transpositions)
          .findBestMove(Board.standard(), new SearchLimits(4));
      assertEquals(0, result.score());
      assertEquals(transpositions ? 9999 : 10043, result.nodes());
      assertEquals(expected, result.trace());
    }
  }

  private static void assertTrace(Board board, boolean quiescence, boolean transpositions,
      int depth, SearchTrace expected) {
    SearchResult result = new AlphaBetaSearch(quiescence, transpositions)
        .findBestMove(board, new SearchLimits(depth));
    assertEquals(expected, result.trace());
  }

  private static SearchTrace.Alternative alternative(String move, int score, SearchTrace.Bound bound) {
    return new SearchTrace.Alternative(move, score, bound);
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
