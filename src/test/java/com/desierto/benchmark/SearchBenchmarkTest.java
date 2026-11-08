package com.desierto.benchmark;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.desierto.domain.Board;
import com.desierto.domain.Cell;
import com.desierto.domain.MoveGenerator;
import com.desierto.search.AlphaBetaSearch;
import com.desierto.search.SearchLimits;
import com.desierto.search.SearchTimeoutException;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class SearchBenchmarkTest {

  @Test
  void shouldCalculateMedianWithoutChangingTheInput() {
    double[] samples = {7, 1, 3, 5};

    assertEquals(4, SearchBenchmark.median(samples));
    assertEquals(7, samples[0]);
    assertEquals(3, SearchBenchmark.median(new double[] {7, 3, 1}));
    assertThrows(IllegalArgumentException.class, () -> SearchBenchmark.median(new double[0]));
  }

  @Test
  void shouldValidateConfiguration() {
    assertEquals(new SearchBenchmark.Config(2, 5, 2, 8, 250),
        SearchBenchmark.Config.fromArgs(new String[0]));
    assertEquals(new SearchBenchmark.Config(0, 1, 1, 3, 10),
        SearchBenchmark.Config.fromArgs(new String[] {"0", "1", "1", "3", "10"}));
    assertThrows(IllegalArgumentException.class, () -> SearchBenchmark.Config.fromArgs(new String[] {"1"}));
    assertThrows(IllegalArgumentException.class, () -> new SearchBenchmark.Config(-1, 1, 1, 1, 1));
    assertThrows(IllegalArgumentException.class, () -> new SearchBenchmark.Config(0, 0, 1, 1, 1));
    assertThrows(IllegalArgumentException.class, () -> new SearchBenchmark.Config(0, 1, 0, 1, 1));
    assertThrows(IllegalArgumentException.class, () -> new SearchBenchmark.Config(0, 1, 1, 0, 1));
    assertThrows(IllegalArgumentException.class, () -> new SearchBenchmark.Config(0, 1, 1, 1, 0));
    assertThrows(IllegalArgumentException.class,
        () -> new SearchBenchmark.Config(0, 1, 1, 1, Long.MAX_VALUE));
  }

  @Test
  void shouldUsePlayablePositionsAndKnownTacticalCase() {
    MoveGenerator generator = new MoveGenerator();
    assertEquals(3, SearchBenchmark.positions().size());
    for (SearchBenchmark.Position position : SearchBenchmark.positions()) {
      Board board = position.board().get();
      assertFalse(generator.generateLegalMoveApplications(board, board.getSideToMove()).isEmpty());
    }

    Board tactical = SearchBenchmark.positions().get(1).board().get();
    assertEquals(Cell.D5, new AlphaBetaSearch(false)
        .findBestMove(tactical, new SearchLimits(1)).bestMove().orElseThrow().to());
    assertFalse(new AlphaBetaSearch()
        .findBestMove(tactical, new SearchLimits(1)).bestMove().orElseThrow().to().equals(Cell.D5));
  }

  @Test
  void shouldReportEveryScenarioWithoutTimingAssertions() {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try (PrintStream output = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
      SearchBenchmark.run(new SearchBenchmark.Config(0, 1, 1, 1, 100), output);
    }
    String output = bytes.toString(StandardCharsets.UTF_8);

    assertTrue(output.contains("warmups=0 samples=1 fixed_depth=1 iterative_max_depth=1 time_limit_ms=100"));
    for (String position : new String[] {"initial", "tactical", "endgame"}) {
      assertTrue(output.contains("position=" + position + " mode=generation sample=1"));
      for (boolean quiescence : new boolean[] {false, true}) {
        for (boolean transpositions : new boolean[] {false, true}) {
          String settings = " quiescence=" + quiescence + " transpositions=" + transpositions;
          assertTrue(output.contains("position=" + position + " mode=fixed"
              + settings + " sample=1 status=result"));
          assertTrue(output.contains("position=" + position + " mode=timed"
              + settings + " sample=1 status="));
        }
      }
    }
  }

  @Test
  void shouldReportTimeoutsWithoutInventingAResult() {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try (PrintStream output = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
      SearchBenchmark.measureSearch(SearchBenchmark.positions().getFirst(),
          new SearchBenchmark.Config(0, 1, 1, 1, 1), output, "timed", true, true,
          board -> { throw new SearchTimeoutException(); });
    }
    String output = bytes.toString(StandardCharsets.UTF_8);

    assertTrue(output.contains("status=timeout"));
    assertTrue(output.contains("results=0 timeouts=1"));
    assertFalse(output.contains("score_cp="));
    assertFalse(output.contains("median_nps="));
  }
}
