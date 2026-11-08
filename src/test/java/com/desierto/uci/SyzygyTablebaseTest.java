package com.desierto.uci;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.desierto.domain.Board;
import com.desierto.domain.LegalMove;
import com.desierto.domain.MoveGenerator;
import com.desierto.domain.MoveNotation;
import com.desierto.search.Engine;
import com.desierto.search.SearchDiagnostics;
import com.desierto.search.SearchLimits;
import com.desierto.search.SearchResult;
import com.desierto.search.SearchTimeoutException;
import com.desierto.tablebase.SyzygyTablebase;
import com.desierto.tablebase.TablebaseOutcome.Wdl;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SyzygyTablebaseTest {
  private static SyzygyTablebase realTablebase;

  private static synchronized SyzygyTablebase realTablebase() throws IOException {
    String path = System.getProperty("viktor.syzygy.testPath");
    assumeTrue(path != null && !path.isBlank(), "Set viktor.syzygy.testPath for real-table tests");
    if (realTablebase == null) {
      realTablebase = new SyzygyTablebase(Path.of(path), message -> {
        throw new AssertionError(message);
      });
    }
    return realTablebase;
  }

  @Test
  void rejectsMissingOrInvalidTables(@TempDir Path directory) throws IOException {
    IOException missing = assertThrows(IOException.class, () -> new SyzygyTablebase(directory, s -> {}));
    assertTrue(missing.getMessage().contains("Missing tablebase file"));
    Files.write(directory.resolve("KQvK.rtbw"), new byte[80]);
    IOException corrupt = assertThrows(IOException.class, () -> new SyzygyTablebase(directory, s -> {}));
    assertTrue(corrupt.getMessage().contains("Invalid tablebase header"));
  }

  @Test
  void probesRealWinsLossesAndPromotions() throws IOException {
    SyzygyTablebase tablebase = realTablebase();
    String[] fens = {
        "4k3/8/8/8/8/8/8/3QK3 w - - 0 1",
        "4k3/8/8/8/8/8/8/3QK3 b - - 0 1",
        "8/P7/2K5/8/8/8/8/7k w - - 0 1",
        "8/8/8/8/8/2k5/2p5/K7 b - - 0 1",
        "8/8/8/8/3k4/8/5K2/2Q1q3 w - - 0 1",
        "8/p3K3/8/8/8/6P1/6k1/8 b - - 0 1"
    };
    Wdl[] expected = {Wdl.WIN, Wdl.LOSS, Wdl.WIN, Wdl.WIN, Wdl.WIN, Wdl.DRAW};
    int[] dtz = {15, 16, 1, 1, 1, 0};
    for (int i = 0; i < fens.length; i++) {
      Board board = Fen.parse(fens[i]);
      String original = board.toString();
      var decision = tablebase.probe(board).orElseThrow();
      assertEquals(expected[i], decision.outcome().wdl(), fens[i]);
      assertEquals(dtz[i], decision.outcome().dtz(), fens[i]);
      assertTrue(MoveNotation.find(MoveNotation.format(decision.move()),
          new MoveGenerator().generateLegalMoves(board, board.getSideToMove())).isPresent(), fens[i]);
      if (i == 2 || i == 3) assertTrue(decision.move().isPromotion(), fens[i]);
      if (i == 4) assertTrue(decision.move().isCapture(), fens[i]);
      assertEquals(original, board.toString());
    }
  }

  @Test
  void skipsPositionsWithHistoryClaimsOrUnsupportedRules() throws IOException {
    SyzygyTablebase tablebase = realTablebase();
    Board history = Fen.parse("4k3/8/8/8/8/8/8/3QK3 w - - 0 1");
    var quiet = MoveNotation.find("e1e2",
        new MoveGenerator().generateLegalMoves(history, history.getSideToMove())).orElseThrow();
    new LegalMove(history, quiet).play();
    assertTrue(history.hasRepetitionHistory());
    assertTrue(tablebase.probe(history).isEmpty());
    for (String fen : new String[] {
        "4k3/8/8/8/8/8/8/3QK3 w - - 1 1",
        "4k3/8/8/8/8/8/8/3QK3 w - - 99 1",
        "4k3/8/8/8/8/8/8/4K2R w K - 0 1",
        "4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1",
        "4k3/8/8/8/8/8/8/4K3 w - - 0 1",
        "4k3/8/8/8/8/8/8/1BRQK3 w - - 0 1"
    }) {
      assertTrue(tablebase.probe(Fen.parse(fen)).isEmpty(), fen);
    }
  }

  @Test
  void engineReportsTablebaseWithoutInventingSearchScore() throws IOException {
    Board board = Fen.parse("4k3/8/8/8/8/8/8/3QK3 w - - 0 1");
    Engine engine = new Engine(null, realTablebase());
    SearchResult result = engine.think(board, new SearchLimits(3));
    assertEquals(Wdl.WIN, result.tablebaseOutcome().orElseThrow().wdl());
    assertEquals(0, result.nodes());
    assertFalse(result.fromBook());
    String log = SearchDiagnostics.decision("tablebase", MoveNotation.format(result.bestMove().orElseThrow()),
        result, "", board);
    assertTrue(log.contains("reason=tablebase"));
    assertTrue(log.contains("wdl=win"));
    assertTrue(log.contains("dtz=15"));
    assertTrue(log.contains("score=unavailable"));
    assertFalse(log.contains("static_material_delta"));
    assertThrows(SearchTimeoutException.class,
        () -> engine.think(board, new SearchLimits(3), () -> true, ignored -> {}));
    Board claim = Fen.parse("5q1k/8/8/8/8/8/8/KR6 w - - 100 1");
    SearchResult fallback = engine.think(claim, new SearchLimits(1));
    assertTrue(fallback.tablebaseOutcome().isEmpty());
    assertTrue(fallback.drawClaim().isPresent());
  }
}
