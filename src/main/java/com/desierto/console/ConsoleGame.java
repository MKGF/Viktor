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
    output.println("Viktor: juegas con " + colorName(humanColor) + ". Escribe ayuda para ver los comandos.");
    output.println(board);
    while (true) {
      Color side = board.getSideToMove();
      List<Move> legalMoves = generator.generateLegalMoves(board, side);
      boolean inCheck = generator.isKingInCheck(board, side);
      if (legalMoves.isEmpty()) {
        if (inCheck) {
          output.println("Jaque mate. Ganan las " + colorName(side.opposite()) + ".");
        } else {
          output.println("Tablas por ahogado.");
        }
        return;
      }
      if (inCheck) {
        output.println("Jaque a las " + colorName(side) + ".");
      }
      if (side == humanColor) {
        output.print("Tu jugada: ");
        output.flush();
        String line = input.readLine();
        if (line == null || line.trim().equalsIgnoreCase("salir")) {
          output.println("Partida terminada.");
          return;
        }
        String command = line.trim().toLowerCase(Locale.ROOT);
        if (command.equals("ayuda")) {
          output.println("Escribe origen y destino, por ejemplo e2e4. Promocion: e7e8q (q, r, b o n).");
          output.println("Enroque: e1g1 o e1c1 para blancas; e8g8 o e8c8 para negras. Salir: salir.");
          continue;
        }
        if (!MoveNotation.hasValidFormat(command)) {
          output.println("Formato incorrecto. Ejemplo: e2e4; promocion: e7e8q.");
          continue;
        }
        var move = MoveNotation.find(command, legalMoves);
        if (move.isEmpty()) {
          output.println("Jugada ilegal. Comprueba el turno y, si hay promocion, indica q, r, b o n.");
          continue;
        }
        board.makeMove(move.orElseThrow());
      } else {
        output.println("Viktor esta pensando...");
        Move move = engine.findBestMove(board, limits).bestMove()
            .orElseThrow(() -> new IllegalStateException("El motor no devolvio una jugada en una posicion no terminal"));
        board.makeMove(move);
        output.println("Viktor juega: " + MoveNotation.format(move));
      }
      output.println(board);
    }
  }

  private static String colorName(Color color) {
    return color == Color.WHITE ? "blancas" : "negras";
  }
}
