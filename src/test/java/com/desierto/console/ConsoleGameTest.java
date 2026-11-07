package com.desierto.console;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.desierto.domain.Board;
import com.desierto.domain.BoardFactory;
import com.desierto.domain.Cell;
import com.desierto.domain.Color;
import com.desierto.domain.Move;
import com.desierto.domain.MoveGenerator;
import com.desierto.domain.pieces.King;
import com.desierto.domain.pieces.Knight;
import com.desierto.domain.pieces.Pawn;
import com.desierto.domain.pieces.Queen;
import com.desierto.domain.pieces.Rook;
import com.desierto.console.ConsoleGame.TimedSearch;
import com.desierto.search.SearchLimits;
import com.desierto.search.SearchResult;
import com.desierto.search.SearchTimeoutException;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ConsoleGameTest {

  private final MoveGenerator generator = new MoveGenerator();

  @Test
  void shouldRejectInvalidInputAndPlayAnExchange() throws IOException {
    Board board = Board.standard();
    String output = play(board, Color.WHITE, moves("e7e5"),
        "help\nz9z9\ne2e5\nE2E4\nquit\n");

    assertTrue(output.contains("Invalid format"));
    assertTrue(output.contains("Illegal move"));
    assertTrue(output.contains("e7e8q"));
    assertTrue(output.contains("Viktor plays: e7e5"));
    assertTrue(output.contains("Game ended."));
    assertTrue(board.getPieceAt(Cell.E4).orElseThrow() instanceof Pawn);
    assertEquals(Color.WHITE, board.getPieceAt(Cell.E4).orElseThrow().getColor());
    assertEquals(Color.BLACK, board.getPieceAt(Cell.E5).orElseThrow().getColor());
  }

  @Test
  void shouldLetTheEngineOpenWhenTheHumanPlaysBlackAndStopOnEof() throws IOException {
    Board board = Board.standard();
    String output = play(board, Color.BLACK, moves("e2e4"), "");

    assertTrue(output.contains("you play Black"));
    assertTrue(output.contains("Viktor plays: e2e4"));
    assertTrue(output.contains("Game ended."));
    assertEquals(Color.BLACK, board.getSideToMove());
  }

  @Test
  void shouldReportCheckmateAfterAnEngineMove() throws IOException {
    Board board = Board.standard();
    String output = play(board, Color.WHITE, moves("e7e5", "d8h4"), "f2f3\ng2g4\n");

    assertTrue(output.contains("Viktor plays: d8h4"));
    assertTrue(output.contains("Checkmate. Black wins."));
    assertFalse(output.contains("Game ended."));
  }

  @Test
  void shouldReportStalemateBeforeRequestingInput() throws IOException {
    Board board = BoardFactory.position().sideToMove(Color.BLACK)
        .castlingRights(false, false, false, false)
        .place(new King(Color.BLACK, Cell.H8), Cell.H8)
        .place(new King(Color.WHITE, Cell.F7), Cell.F7)
        .place(new Queen(Color.WHITE, Cell.G6), Cell.G6)
        .build();

    String output = play(board, Color.BLACK, (position, limits, timeLimit) -> {
      throw new AssertionError("Engine should not be called");
    }, "");

    assertTrue(output.contains("Draw by stalemate."));
    assertFalse(output.contains("Your move:"));
  }

  @Test
  void shouldRequirePromotionPieceAndSupportUnderpromotion() throws IOException {
    Board board = BoardFactory.position().castlingRights(false, false, false, false)
        .place(new King(Color.WHITE, Cell.E1), Cell.E1)
        .place(new King(Color.BLACK, Cell.H8), Cell.H8)
        .place(new Pawn(Color.WHITE, Cell.A7), Cell.A7)
        .place(new Rook(Color.BLACK, Cell.B8), Cell.B8).build();
    String output = play(board, Color.WHITE, moves("h8g7"), "a7b8\na7b8n\nquit\n");

    assertTrue(output.contains("Illegal move"));
    assertTrue(board.getPieceAt(Cell.B8).orElseThrow() instanceof Knight);
  }

  @Test
  void shouldContinueAcceptingSpanishCommandAliases() throws IOException {
    String output = play(Board.standard(), Color.WHITE, moves(), "ayuda\nsalir\n");

    assertTrue(output.contains("Castling:"));
    assertTrue(output.contains("Game ended."));
  }

  @Test
  void shouldRepresentCastlingEnPassantAndPromotionByCoordinates() {
    Board castling = BoardFactory.position()
        .place(new King(Color.WHITE, Cell.E1), Cell.E1)
        .place(new Rook(Color.WHITE, Cell.H1), Cell.H1)
        .place(new King(Color.BLACK, Cell.E8), Cell.E8).build();
    Board enPassant = BoardFactory.position().castlingRights(false, false, false, false)
        .enPassantTarget(Cell.D6)
        .place(new King(Color.WHITE, Cell.E1), Cell.E1)
        .place(new King(Color.BLACK, Cell.E8), Cell.E8)
        .place(new Pawn(Color.WHITE, Cell.E5), Cell.E5)
        .place(new Pawn(Color.BLACK, Cell.D5), Cell.D5).build();
    applyNotation(castling, "e1g1");
    applyNotation(enPassant, "e5d6");
    assertTrue(castling.getPieceAt(Cell.F1).orElseThrow() instanceof Rook);
    assertTrue(enPassant.getPieceAt(Cell.D5).isEmpty());
  }

  @Test
  void shouldPropagateInputErrorsAndRejectMissingEngineMoves() {
    Reader broken = new Reader() {
      @Override public int read(char[] chars, int offset, int length) throws IOException {
        throw new IOException("input failed");
      }
      @Override public void close() {
      }
    };
    ConsoleGame humanGame = new ConsoleGame(Board.standard(), Color.WHITE, moves(), new SearchLimits(1),
        Duration.ofSeconds(1));
    PrintStream output = new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8);
    assertThrows(IOException.class, () -> humanGame.run(new BufferedReader(broken), output));

    ConsoleGame engineGame = new ConsoleGame(Board.standard(), Color.BLACK,
        (board, limits, timeLimit) -> new SearchResult(Optional.empty(), Color.WHITE, 0, 0, 0),
        new SearchLimits(1), Duration.ofSeconds(1));
    assertThrows(IllegalStateException.class,
        () -> engineGame.run(new BufferedReader(new StringReader("")), output));
  }

  @Test
  void shouldPassDepthAndTimeLimitToTheEngine() throws IOException {
    Board board = Board.standard();
    Duration timeLimit = Duration.ofSeconds(7);
    TimedSearch engine = (position, limits, allowedTime) -> {
      assertEquals(15, limits.maxDepth());
      assertEquals(timeLimit, allowedTime);
      Move move = MoveNotation.find("e2e4",
          generator.generateLegalMoves(position, position.getSideToMove())).orElseThrow();
      return new SearchResult(Optional.of(move), position.getSideToMove(), 0, 4, 100);
    };
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    new ConsoleGame(board, Color.BLACK, engine, new SearchLimits(15), timeLimit)
        .run(new BufferedReader(new StringReader("quit\n")),
            new PrintStream(bytes, true, StandardCharsets.UTF_8));

    assertTrue(bytes.toString(StandardCharsets.UTF_8).contains("Viktor plays: e2e4 (depth 4)"));
  }

  @Test
  void shouldInformTheUserAndEndTheGameWhenNoIterationCompletes() throws IOException {
    Board board = Board.standard();
    String initialPosition = board.toString();
    String output = play(board, Color.BLACK, (position, limits, timeLimit) -> {
      throw new SearchTimeoutException();
    }, "");

    assertTrue(output.contains("Search timed out before completing the first depth"));
    assertTrue(output.contains("Game ended."));
    assertFalse(output.contains("Viktor plays:"));
    assertEquals(initialPosition, board.toString());
    assertEquals(Color.WHITE, board.getSideToMove());
  }

  @Test
  void shouldRejectInvalidTimeLimits() {
    assertThrows(NullPointerException.class, () -> new ConsoleGame(Color.WHITE, 3, null));
    assertThrows(IllegalArgumentException.class, () -> new ConsoleGame(Color.WHITE, 3, Duration.ZERO));
    assertThrows(IllegalArgumentException.class, () -> new ConsoleGame(Color.WHITE, 3, Duration.ofSeconds(-1)));
    assertThrows(IllegalArgumentException.class,
        () -> new ConsoleGame(Color.WHITE, 3, Duration.ofSeconds(Long.MAX_VALUE)));
  }

  private String play(Board board, Color humanColor, TimedSearch engine, String commands)
      throws IOException {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    PrintStream output = new PrintStream(bytes, true, StandardCharsets.UTF_8);
    new ConsoleGame(board, humanColor, engine, new SearchLimits(1), Duration.ofSeconds(1))
        .run(new BufferedReader(new StringReader(commands)), output);
    return bytes.toString(StandardCharsets.UTF_8);
  }

  private void applyNotation(Board board, String notation) {
    Move move = MoveNotation.find(notation, generator.generateLegalMoves(board, Color.WHITE)).orElseThrow();
    assertEquals(notation, MoveNotation.format(move));
    board.makeMove(move);
  }

  private TimedSearch moves(String... moves) {
    int[] index = {0};
    return (board, limits, timeLimit) -> {
      String notation = moves[index[0]++];
      Move move = MoveNotation.find(notation,
          generator.generateLegalMoves(board, board.getSideToMove())).orElseThrow();
      return new SearchResult(Optional.of(move), board.getSideToMove(), 0, 1, 1);
    };
  }
}
