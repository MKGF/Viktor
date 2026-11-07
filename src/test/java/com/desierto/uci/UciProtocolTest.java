package com.desierto.uci;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.desierto.EngineInfo;
import com.desierto.domain.Board;
import com.desierto.domain.MoveGenerator;
import com.desierto.domain.MoveNotation;
import com.desierto.search.SearchDiagnostics;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class UciProtocolTest {
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
      BufferedReader input = new BufferedReader(new PipedReader(commands));
      PrintStream output = new PrintStream(bytes, true, StandardCharsets.UTF_8);
      PrintStream errors = new PrintStream(errorBytes, true, StandardCharsets.UTF_8);
      protocol = new Thread(() -> {
        try {
          new UciProtocol(output, errors).run(input);
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
