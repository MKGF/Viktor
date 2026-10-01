package com.desierto.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.desierto.domain.Cell;
import com.desierto.domain.Color;
import com.desierto.domain.Move;
import com.desierto.domain.MoveType;
import com.desierto.domain.pieces.Pawn;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SearchResultTest {

  @Test
  void shouldRepresentACompletedSearch() {
    Move move = new Move(Color.WHITE, new Pawn(Color.WHITE, Cell.E2), Cell.E2, Cell.E4, null,
        MoveType.NORMAL, null);
    SearchResult result = new SearchResult(Optional.of(move), Color.WHITE, 100, 3, 1_248);

    assertEquals(move, result.bestMove().orElseThrow());
    assertEquals(Color.WHITE, result.perspective());
    assertEquals(100, result.score());
    assertEquals(3, result.depth());
    assertEquals(1_248, result.nodes());
  }

  @Test
  void shouldRepresentATerminalPositionWithoutAMove() {
    SearchResult result = new SearchResult(Optional.empty(), Color.BLACK, -100_000, 0, 1);

    assertEquals(Optional.empty(), result.bestMove());
    assertEquals(Color.BLACK, result.perspective());
    assertEquals(-100_000, result.score());
  }

  @Test
  void shouldRejectInvalidSearchMetrics() {
    assertThrows(NullPointerException.class, () -> new SearchResult(null, Color.WHITE, 0, 0, 0));
    assertThrows(NullPointerException.class,
        () -> new SearchResult(Optional.empty(), null, 0, 0, 0));
    assertThrows(IllegalArgumentException.class,
        () -> new SearchResult(Optional.empty(), Color.WHITE, 0, -1, 0));
    assertThrows(IllegalArgumentException.class,
        () -> new SearchResult(Optional.empty(), Color.WHITE, 0, 0, -1));
  }
}
