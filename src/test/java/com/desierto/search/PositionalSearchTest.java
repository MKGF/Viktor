package com.desierto.search;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.desierto.domain.Board;
import com.desierto.domain.BoardFactory;
import com.desierto.domain.Cell;
import com.desierto.domain.Color;
import com.desierto.domain.Move;
import com.desierto.domain.MoveGenerator;
import com.desierto.domain.MoveNotation;
import com.desierto.domain.pieces.King;
import com.desierto.domain.pieces.Pawn;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PositionalSearchTest {
  @Test
  void openingDevelopsACentralKnightForBothSides() {
    Board board = Board.standard();
    assertReasonableMove(board, Set.of("b1c3", "g1f3"));

    Move firstMove = MoveNotation.find("e2e4",
        new MoveGenerator().generateLegalMoves(board, Color.WHITE)).orElseThrow();
    board.makeMove(firstMove);
    assertReasonableMove(board, Set.of("b8c6", "g8f6"));
  }

  @Test
  void endgameBringsTheKingTowardsTheCenterForBothSides() {
    Board white = BoardFactory.position().castlingRights(false, false, false, false)
        .place(new King(Color.WHITE, Cell.A1), Cell.A1)
        .place(new Pawn(Color.WHITE, Cell.H2), Cell.H2)
        .place(new King(Color.BLACK, Cell.H8), Cell.H8)
        .build();
    Board black = BoardFactory.position().sideToMove(Color.BLACK)
        .castlingRights(false, false, false, false)
        .place(new King(Color.BLACK, Cell.A8), Cell.A8)
        .place(new Pawn(Color.BLACK, Cell.H7), Cell.H7)
        .place(new King(Color.WHITE, Cell.H1), Cell.H1)
        .build();
    assertReasonableMove(white, Set.of("a1a2", "a1b1", "a1b2"));
    assertReasonableMove(black, Set.of("a8a7", "a8b8", "a8b7"));
  }

  private void assertReasonableMove(Board board, Set<String> reasonableMoves) {
    SearchResult result = new FixedDepthAlphaBetaEngine().findBestMove(board, new SearchLimits(1));
    Move move = result.bestMove().orElseThrow();
    String notation = move.from().toAlgebraic().toLowerCase()
        + move.to().toAlgebraic().toLowerCase();
    assertTrue(reasonableMoves.contains(notation), notation + " score=" + result.score());
  }
}
