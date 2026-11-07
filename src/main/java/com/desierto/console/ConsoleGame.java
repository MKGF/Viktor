package com.desierto.console;

import com.desierto.domain.Board;
import com.desierto.domain.Color;
import com.desierto.domain.Move;
import com.desierto.domain.MoveGenerator;
import com.desierto.search.IterativeDeepeningEngine;
import com.desierto.search.SearchLimits;
import com.desierto.search.SearchResult;
import com.desierto.search.SearchTimeoutException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class ConsoleGame {

  public static final Duration DEFAULT_TIME_LIMIT = Duration.ofSeconds(10);

  private final Board board;
  private final Color humanColor;
  private final TimedSearch engine;
  private final SearchLimits limits;
  private final Duration timeLimit;
  private final MoveGenerator generator = new MoveGenerator();

  public ConsoleGame(Color humanColor, int depth) {
    this(humanColor, depth, DEFAULT_TIME_LIMIT);
  }

  public ConsoleGame(Color humanColor, int depth, Duration timeLimit) {
    this(Board.standard(), humanColor, new IterativeDeepeningEngine()::findBestMove,
        new SearchLimits(depth), timeLimit);
  }

  ConsoleGame(Board board, Color humanColor, TimedSearch engine, SearchLimits limits, Duration timeLimit) {
    this.board = Objects.requireNonNull(board, "board must not be null");
    this.humanColor = Objects.requireNonNull(humanColor, "humanColor must not be null");
    this.engine = Objects.requireNonNull(engine, "engine must not be null");
    this.limits = Objects.requireNonNull(limits, "limits must not be null");
    this.timeLimit = Objects.requireNonNull(timeLimit, "timeLimit must not be null");
    try {
      if (timeLimit.toNanos() <= 0) {
        throw new IllegalArgumentException("timeLimit must be positive");
      }
    } catch (ArithmeticException ex) {
      throw new IllegalArgumentException("timeLimit is too large", ex);
    }
  }

  public void run(BufferedReader input, PrintStream output) throws IOException {
    Objects.requireNonNull(input, "input must not be null");
    Objects.requireNonNull(output, "output must not be null");
    output.println("Viktor: you play " + colorName(humanColor) + ". Type help to see the commands.");
    output.println(board);
    while (true) {
      Color side = board.getSideToMove();
      List<Move> legalMoves = generator.generateLegalMoves(board, side);
      boolean inCheck = generator.isKingInCheck(board, side);
      if (legalMoves.isEmpty()) {
        if (inCheck) {
          output.println("Checkmate. " + colorName(side.opposite()) + " wins.");
        } else {
          output.println("Draw by stalemate.");
        }
        return;
      }
      if (inCheck) {
        output.println(colorName(side) + " is in check.");
      }
      if (side == humanColor) {
        output.print("Your move: ");
        output.flush();
        String line = input.readLine();
        if (line == null || line.trim().equalsIgnoreCase("quit")
            || line.trim().equalsIgnoreCase("salir")) {
          output.println("Game ended.");
          return;
        }
        String command = line.trim().toLowerCase(Locale.ROOT);
        if (command.equals("help") || command.equals("ayuda")) {
          output.println("Enter the starting and destination squares, for example e2e4. Promotion: e7e8q (q, r, b or n).");
          output.println("Castling: e1g1 or e1c1 for White; e8g8 or e8c8 for Black. To exit: quit.");
          continue;
        }
        if (!MoveNotation.hasValidFormat(command)) {
          output.println("Invalid format. Example: e2e4; promotion: e7e8q.");
          continue;
        }
        var move = MoveNotation.find(command, legalMoves);
        if (move.isEmpty()) {
          output.println("Illegal move. Check the position and specify q, r, b or n for a promotion.");
          continue;
        }
        board.makeMove(move.orElseThrow());
      } else {
        output.println("Viktor is thinking...");
        SearchResult result;
        try {
          result = engine.findBestMove(board, limits, timeLimit);
        } catch (SearchTimeoutException ex) {
          output.println("Search timed out before completing the first depth. Increase --time and try again.");
          output.println("Game ended.");
          return;
        }
        Move move = result.bestMove()
            .orElseThrow(() -> new IllegalStateException("The engine returned no move in a non-terminal position"));
        board.makeMove(move);
        output.println("Viktor plays: " + MoveNotation.format(move) + " (depth " + result.depth() + ")");
      }
      output.println(board);
    }
  }

  private static String colorName(Color color) {
    return color == Color.WHITE ? "White" : "Black";
  }

  @FunctionalInterface
  interface TimedSearch {
    SearchResult findBestMove(Board board, SearchLimits limits, Duration timeLimit);
  }
}
