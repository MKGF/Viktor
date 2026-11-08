package com.desierto.uci;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.desierto.EngineInfo;
import com.desierto.DefaultResources;
import com.desierto.domain.Board;
import com.desierto.domain.MoveGenerator;
import com.desierto.domain.MoveNotation;
import com.desierto.search.SearchDiagnostics;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class UciProtocolTest {
  @Test
  void usesDefaultBookAndCanDisableIt(@TempDir Path directory) throws Exception {
    DefaultResources defaults = DefaultResources.inDirectory(directory);
    Files.createDirectories(defaults.book().getParent());
    try (DataOutputStream out = new DataOutputStream(Files.newOutputStream(defaults.book()))) {
      out.writeLong(0x463b96181691fc9cL);
      out.writeShort((12 << 6) | 28);
      out.writeShort(1);
      out.writeInt(0);
    }
    try (Harness h = new Harness(defaults)) {
      h.send("uci\nisready\nposition startpos\ngo depth 1\n");
      String output = h.await("bestmove ");
      assertTrue(output.contains("option name OwnBook type check default true"));
      assertTrue(output.contains("option name BookFile type string default " + defaults.book()));
      assertTrue(output.contains("option name SyzygyPath type string default " + defaults.tablebases()));
      assertTrue(output.contains("source=book"));
      assertTrue(output.contains("bestmove e2e4"));
      assertTrue(output.contains("Warning: Cannot load tablebases"));
      h.send("setoption name OwnBook value false\nposition startpos\ngo depth 1\n");
      output = h.awaitCount("bestmove ", 2);
      assertTrue(output.substring(output.indexOf("bestmove e2e4") + 12).contains("source=search"));
    }
  }

  @Test
  void usesDefaultTablebaseAndCanDisableIt() throws Exception {
    String path = System.getProperty("viktor.syzygy.testPath");
    assumeTrue(path != null && !path.isBlank(), "Set viktor.syzygy.testPath for real-table tests");
    DefaultResources defaults = new DefaultResources(null, Path.of(path).toAbsolutePath());
    try (Harness h = new Harness(defaults)) {
      String position = "position fen 4k3/8/8/8/8/8/8/3QK3 w - - 0 1";
      h.send("uci\nisready\n" + position + "\ngo depth 1\n");
      String output = h.await("bestmove ");
      assertTrue(output.contains("option name SyzygyPath type string default " + defaults.tablebases()));
      assertTrue(output.contains("source=tablebase"));
      h.send("setoption name SyzygyPath value <empty>\n" + position + "\ngo depth 1\n");
      output = h.awaitCount("bestmove ", 2);
      assertTrue(output.substring(output.indexOf("bestmove ") + 9).contains("source=search"));
    }
  }

  @Test
  void respectsDisablingDefaultsBeforeReadiness(@TempDir Path directory) throws Exception {
    try (Harness h = new Harness(DefaultResources.inDirectory(directory))) {
      h.send("uci\nsetoption name OwnBook value false\nsetoption name SyzygyPath value <empty>\n"
          + "isready\nposition startpos\ngo depth 1\n");
      String output = h.await("bestmove ");
      assertTrue(output.contains("readyok"));
      assertFalse(output.contains("Warning:"));
      assertTrue(output.contains("source=search"));
    }
  }

  @Test
  void warnsAndSearchesWhenDefaultResourcesAreMissing(@TempDir Path directory) throws Exception {
    try (Harness h = new Harness(DefaultResources.inDirectory(directory))) {
      h.send("uci\nisready\nposition startpos\ngo depth 1\n");
      String output = h.await("bestmove ");
      assertTrue(output.contains("Warning: Cannot load opening book"));
      assertTrue(output.contains("Warning: Cannot load tablebases"));
      assertTrue(output.contains("source=search"));
    }
  }

  @Test
  void usesTablebaseAndWaitsForStopInInfiniteMode() throws Exception {
    String path = System.getProperty("viktor.syzygy.testPath");
    assumeTrue(path != null && !path.isBlank(), "Set viktor.syzygy.testPath for real-table tests");
    try (Harness h = new Harness()) {
      String position = "position fen 4k3/8/8/8/8/8/8/3QK3 w - - 0 1";
      h.send("uci\nsetoption name SyzygyPath value " + Path.of(path).toAbsolutePath()
          + "\n" + position + "\ngo depth 2\n");
      String output = h.await("bestmove ");
      assertTrue(output.contains("option name SyzygyPath type string default <empty>"));
      assertTrue(output.contains("source=tablebase"));
      assertTrue(output.contains("reason=tablebase"));
      assertTrue(output.contains("wdl=win"));
      assertTrue(output.contains("score=unavailable"));
      assertLegalBestMove(output, UciPosition.parse(position.split(" ")));
      h.send(position + "\ngo infinite\nisready\n");
      output = h.await("readyok");
      assertEquals(1, output.split("bestmove ", -1).length - 1);
      h.send("stop\n");
      output = h.awaitCount("bestmove ", 2);
      assertLegalBestMove(output, UciPosition.parse(position.split(" ")));
    }
  }

  @Test
  void warnsAboutMissingTablebasesAndFallsBack(@TempDir Path directory) throws Exception {
    try (Harness h = new Harness()) {
      h.send("setoption name SyzygyPath value " + directory + "\n"
          + "position fen 4k3/8/8/8/8/8/8/3QK3 w - - 0 1\ngo depth 1\n");
      String output = h.await("bestmove ");
      assertTrue(output.contains("Warning: Cannot load tablebases"));
      assertTrue(output.contains("source=search"));
    }
  }

  @Test
  void configuresBookAndRespectsInfiniteSearch(@TempDir Path directory) throws Exception {
    Path book = directory.resolve("opening book.bin");
    try (DataOutputStream out = new DataOutputStream(Files.newOutputStream(book))) {
      out.writeLong(0x463b96181691fc9cL);
      out.writeShort((12 << 6) | 28);
      out.writeShort(1);
      out.writeInt(0);
    }
    try (Harness h = new Harness()) {
      h.send("uci\nsetoption name BookFile value " + book
          + "\nsetoption name OwnBook value true\nposition startpos\ngo depth 1\n");
      String output = h.await("bestmove ");
      assertTrue(output.contains("option name OwnBook type check default false"));
      assertTrue(output.contains("option name BookFile type string default <empty>"));
      assertTrue(output.contains("source=book"));
      assertTrue(output.contains("reason=opening_book"));
      assertTrue(output.contains("score=unavailable"));
      assertTrue(output.contains("bestmove e2e4"));

      h.send("position startpos\ngo infinite\nisready\n");
      String ready = h.await("readyok");
      assertEquals(1, ready.split("bestmove ", -1).length - 1);
      h.send("stop\n");
      output = h.awaitCount("bestmove ", 2);
      assertLegalBestMove(output.substring(output.lastIndexOf("bestmove ")), Board.standard());

      h.send("setoption name OwnBook value false\nposition startpos\ngo depth 1\n");
      output = h.awaitCount("bestmove ", 3);
      assertTrue(output.substring(output.indexOf("bestmove e2e4") + 12).contains("source=search"));
    }
  }

  @Test
  void warnsAboutMissingBookAndFallsBack(@TempDir Path directory) throws Exception {
    try (Harness h = new Harness()) {
      h.send("setoption name BookFile value " + directory.resolve("missing.bin")
          + "\nsetoption name OwnBook value true\nposition startpos\ngo depth 1\n");
      String output = h.await("bestmove ");
      assertTrue(output.contains("Warning: Cannot load opening book"));
      assertTrue(output.contains("source=search"));
      assertLegalBestMove(output, Board.standard());
    }
  }

  @Test
  void writesReadableIndependentLogForMultipleGames(@TempDir Path directory) throws Exception {
    try (UciSessionLog log = UciSessionLog.open(directory, EngineInfo.name());
        UciSessionLog another = UciSessionLog.open(directory, EngineInfo.name());
        Harness h = new Harness(log)) {
      assertNotEquals(log.path(), another.path());
      assertTrue(log.path().toString().endsWith(".log"));
      h.send("ucinewgame\nposition startpos moves e2e4\ngo depth 2\n");
      String output = h.await("bestmove ");
      assertTrue(output.contains("info string decision:"));
      h.send("ucinewgame\nisready\n");
      h.await("readyok");
      String contents = Files.readString(log.path(), StandardCharsets.UTF_8).replace("\r\n", "\n");
      assertTrue(contents.contains("engine=" + EngineInfo.name()));
      assertEquals(2, contents.lines().filter(line -> line.equals("  event=new_game")).count());
      assertTrue(contents.contains("input=position startpos moves e2e4"));
      assertTrue(contents.contains("  board:\n"));
      assertTrue(contents.contains("input=go depth 2"));
      assertTrue(contents.contains("iteration:\n  depth=1"));
      assertTrue(contents.contains("iteration:\n  depth=2"));
      assertTrue(contents.contains("decision:\n  source=search"));
      assertTrue(contents.contains("  reason=best_searched_score"));
      assertTrue(contents.contains("  static_perspective=black"));
      assertTrue(contents.contains("  static_material_delta=cp:"));
      assertTrue(contents.contains("  static_positional_delta=cp:"));
      assertTrue(contents.contains("  alternatives:\n    - move="));
      assertTrue(contents.contains("  stage=projected_after_engine_move"));
      assertTrue(contents.contains("\n  side=black\n  fullmove=1\n"));
      assertFalse(contents.contains("info string"));
    }
  }

  @Test
  void reportsLogWriteFailureAndContinuesSearch(@TempDir Path directory) throws Exception {
    UciSessionLog log = UciSessionLog.open(directory, EngineInfo.name());
    log.close();
    try (Harness h = new Harness(log)) {
      h.send("position startpos\ngo depth 1\n");
      String output = h.await("bestmove ");
      assertTrue(output.contains("info string Warning: cannot write Viktor log:"));
      assertTrue(h.diagnostics().contains("Warning: cannot write Viktor log:"));
      assertLegalBestMove(output, Board.standard());
    }
  }

  @Test
  void reportsLogCreationFailure(@TempDir Path directory) throws Exception {
    Path file = Files.createFile(directory.resolve("not-a-directory"));
    assertThrows(IOException.class, () -> UciSessionLog.open(file, EngineInfo.name()));
  }

  @Test
  void handshakeDepthSearchAndNewGame() throws Exception {
    try (Harness h = new Harness()) {
      h.send("uci\nisready\nposition startpos moves e2e4\ngo depth 1\n");
      String output = h.await("bestmove ");
      assertTrue(output.lines().anyMatch(line -> line.equals("id name " + EngineInfo.name())));
      assertTrue(output.contains("uciok"));
      assertTrue(output.contains("readyok"));
      assertTrue(output.contains("info depth 1 score "));
      assertTrue(output.contains("info string decision:"));
      assertTrue(output.contains("info string   source=search"));
      assertTrue(output.contains("info string   selected_move="));
      assertTrue(output.contains("info string   score_perspective=black"));
      assertTrue(output.contains("info string   pv="));
      assertTrue(output.contains("info string   alternatives:"));
      assertTrue(output.contains("info string     - move="));
      assertTrue(output.contains("side=black fullmove=1 halfmove=0 in_check=false legal_moves=20"));
      assertTrue(output.contains("depth_limit=1 time_limit_ms=unlimited"));
      assertTrue(output.matches("(?s).*info string   score=(cp|mate):-?\\d+.*"));
      assertTrue(output.matches("(?s).*elapsed_ms=\\d+ uci_move=\\S+ depth=1 nodes=\\d+.*"));
      String diagnostic = output.lines().filter(line -> line.startsWith("info string "))
          .map(line -> line.substring("info string ".length()))
          .collect(java.util.stream.Collectors.joining("\n"));
      assertEquals(diagnostic, h.diagnostics().lines().collect(java.util.stream.Collectors.joining("\n")));
      Board board = UciPosition.parse("position startpos moves e2e4".split(" "));
      for (String line : SearchDiagnostics.position(board, "received").lines().toList()) {
        assertTrue(output.contains("info string " + line));
      }
      assertTrue(output.contains("info string   stage=projected_after_engine_move"));
      assertLegalBestMove(output, board);
      String best = output.substring(output.lastIndexOf("bestmove ") + "bestmove ".length()).split("\\s+")[0];
      board.makeMove(MoveNotation.find(best,
          new MoveGenerator().generateLegalMoves(board, board.getSideToMove())).orElseThrow());
      for (String line : SearchDiagnostics.position(board, "projected_after_engine_move").lines().toList()) {
        assertTrue(output.contains("info string " + line));
      }
      h.send("ucinewgame\nposition startpos\nisready\n");
      h.awaitCount("readyok", 2);
    }
  }

  @Test
  void staysResponsiveDuringInfiniteSearchAndStopsWithOneLegalMove() throws Exception {
    try (Harness h = new Harness()) {
      h.send("position startpos\ngo infinite\nisready\n");
      String before = h.await("readyok");
      assertFalse(before.contains("bestmove "));
      h.send("stop\n");
      String output = h.await("bestmove ");
      assertLegalBestMove(output, Board.standard());
      assertTrue(output.indexOf("bestmove ") == output.lastIndexOf("bestmove "));
    }
  }

  @Test
  void returnsFallbackWhenTimeExpiresBeforeFirstIteration() throws Exception {
    try (Harness h = new Harness()) {
      h.send("position startpos\ngo movetime 0\n");
      String output = h.await("bestmove ");
      assertTrue(output.contains("Search stopped before completing an iteration"));
      assertTrue(output.contains("source=no_completed_iteration"));
      assertTrue(output.contains("score=unavailable"));
      assertTrue(output.contains("depth=unavailable nodes=unavailable"));
      assertLegalBestMove(output, Board.standard());
    }
  }

  @Test
  void returnsLegalMoveForAutomaticDrawButNullMoveForCheckmate() throws Exception {
    try (Harness h = new Harness()) {
      String repeated = "position startpos moves g1f3 g8f6 f3g1 f6g8 g1f3 g8f6 f3g1 f6g8";
      h.send(repeated + "\ngo depth 1\n");
      String output = h.await("bestmove ");
      assertTrue(output.contains("Draw is handled by the GUI"));
      assertTrue(output.contains("source=automatic_draw"));
      assertLegalBestMove(output, UciPosition.parse(repeated.split(" ")));
      h.send("position fen 7k/6Q1/5K2/8/8/8/8/8 b - - 0 1\ngo depth 1\n");
      String mate = h.awaitCount("bestmove ", 2);
      assertTrue(mate.contains("source=terminal"));
      assertTrue(mate.contains("uci_move=0000"));
      assertTrue(mate.contains("score=mate:0"));
    }
  }

  @Test
  void returnsALegalMoveWhenTheEngineWouldClaimFiftyMoves() throws Exception {
    try (Harness h = new Harness()) {
      String position = "position fen 5q1k/8/8/8/8/8/8/KR6 w - - 100 1";
      h.send(position + "\ngo depth 1\n");
      String output = h.await("bestmove ");
      assertTrue(output.contains("Draw claim is handled by the GUI"));
      assertTrue(output.contains("source=draw_claim"));
      assertLegalBestMove(output, UciPosition.parse(position.split(" ")));
    }
  }

  @Test
  void reportsBadCommandsAndKeepsThePreviousPosition() throws Exception {
    try (Harness h = new Harness()) {
      h.send("position startpos moves e2e4\nposition startpos moves e2e5\n"
          + "go nodes 10\nisready\ngo depth 1\n");
      String output = h.awaitCount("bestmove ", 2);
      assertTrue(output.contains("Error: Illegal move"));
      assertTrue(output.contains("Unsupported go parameter: nodes"));
      assertTrue(output.contains("readyok"));
      assertLegalBestMove(output.substring(output.lastIndexOf("bestmove ")),
          UciPosition.parse("position startpos moves e2e4".split(" ")));
    }
  }

  private static void assertLegalBestMove(String output, Board board) {
    int start = output.lastIndexOf("bestmove ") + "bestmove ".length();
    String notation = output.substring(start).split("\\s+")[0];
    assertTrue(MoveNotation.hasValidFormat(notation), notation);
    assertTrue(MoveNotation.find(notation,
        new MoveGenerator().generateLegalMoves(board, board.getSideToMove())).isPresent(), notation);
  }

  private static final class Harness implements AutoCloseable {
    private final PipedWriter commands = new PipedWriter();
    private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errorBytes = new ByteArrayOutputStream();
    private final Thread protocol;
    private volatile Throwable failure;

    Harness() throws IOException {
      this(null, new DefaultResources(null, null));
    }

    Harness(UciSessionLog sessionLog) throws IOException {
      this(sessionLog, new DefaultResources(null, null));
    }

    Harness(DefaultResources defaults) throws IOException {
      this(null, defaults);
    }

    Harness(UciSessionLog sessionLog, DefaultResources defaults) throws IOException {
      BufferedReader input = new BufferedReader(new PipedReader(commands));
      PrintStream output = new PrintStream(bytes, true, StandardCharsets.UTF_8);
      PrintStream errors = new PrintStream(errorBytes, true, StandardCharsets.UTF_8);
      protocol = new Thread(() -> {
        try {
          new UciProtocol(output, errors, sessionLog, defaults).run(input);
        } catch (Throwable ex) {
          failure = ex;
        }
      });
      protocol.start();
    }

    void send(String value) throws IOException {
      commands.write(value);
      commands.flush();
    }

    String diagnostics() {
      return errorBytes.toString(StandardCharsets.UTF_8);
    }

    String await(String text) throws Exception {
      return awaitCount(text, 1);
    }

    String awaitCount(String text, int count) throws Exception {
      long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
      while (System.nanoTime() - deadline < 0) {
        String output = bytes.toString(StandardCharsets.UTF_8);
        if (output.split(java.util.regex.Pattern.quote(text), -1).length - 1 >= count) {
          return output;
        }
        if (failure != null) {
          throw new AssertionError("Protocol failed", failure);
        }
        Thread.sleep(5);
      }
      throw new AssertionError("Timed out waiting for " + text + ": " + bytes.toString(StandardCharsets.UTF_8));
    }

    @Override
    public void close() throws Exception {
      send("quit\n");
      protocol.join(10_000);
      commands.close();
      assertFalse(protocol.isAlive());
      if (failure != null) {
        throw new AssertionError("Protocol failed", failure);
      }
    }
  }
}
