package com.desierto.console;

import com.desierto.domain.Board;
import com.desierto.domain.Color;
import com.desierto.domain.Move;
import com.desierto.domain.MoveGenerator;
import com.desierto.search.FixedDepthAlphaBetaEngine;
import com.desierto.search.SearchEngine;
import com.desierto.search.SearchLimits;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class ConsoleGame {

  private final Board board;
  private final Color humanColor;
  private final SearchEngine engine;
  private final SearchLimits limits;
  private final MoveGenerator generator = new MoveGenerator();

  public ConsoleGame(Color humanColor, int depth) {
    this(Board.standard(), humanColor, new FixedDepthAlphaBetaEngine(), new SearchLimits(depth));
  }

  ConsoleGame(Board board, Color humanColor, SearchEngine engine, SearchLimits limits) {
    this.board = Objects.requireNonNull(board, "board must not be null");
    this.humanColor = Objects.requireNonNull(humanColor, "humanColor must not be null");
    this.engine = Objects.requireNonNull(engine, "engine must not be null");
    this.limits = Objects.requireNonNull(limits, "limits must not be null");
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
        Move move = engine.findBestMove(board, limits).bestMove()
            .orElseThrow(() -> new IllegalStateException("The engine returned no move in a non-terminal position"));
        board.makeMove(move);
        output.println("Viktor plays: " + MoveNotation.format(move));
      }
      output.println(board);
    }
  }

  private static String colorName(Color color) {
    return color == Color.WHITE ? "White" : "Black";
  }
}
