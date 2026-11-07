package com.desierto.uci;

import com.desierto.domain.Board;
import com.desierto.domain.Move;
import com.desierto.domain.MoveGenerator;
import com.desierto.domain.MoveNotation;
import java.util.Arrays;

final class UciPosition {
  private UciPosition() {
  }

  static Board parse(String[] tokens) {
    if (tokens.length < 2) {
      throw new IllegalArgumentException("Missing position");
    }
    Board board;
    int next;
    if (tokens[1].equals("startpos")) {
      board = Board.standard();
      next = 2;
    } else if (tokens[1].equals("fen") && tokens.length >= 8) {
      board = Fen.parse(String.join(" ", Arrays.copyOfRange(tokens, 2, 8)));
      next = 8;
    } else {
      throw new IllegalArgumentException("Expected startpos or a six-field FEN");
    }
    if (next < tokens.length) {
      if (!tokens[next++].equals("moves")) {
        throw new IllegalArgumentException("Expected moves after position");
      }
      MoveGenerator generator = new MoveGenerator();
      for (; next < tokens.length; next++) {
        String notation = tokens[next];
        if (!MoveNotation.hasValidFormat(notation)) {
          throw new IllegalArgumentException("Invalid move: " + notation);
        }
        Move move = MoveNotation.find(notation,
            generator.generateLegalMoves(board, board.getSideToMove()))
            .orElseThrow(() -> new IllegalArgumentException("Illegal move: " + notation));
        board.makeMove(move);
      }
    }
    return board;
  }
}
