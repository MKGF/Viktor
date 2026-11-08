package com.desierto.search;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.desierto.domain.Board;
import com.desierto.domain.BoardFactory;
import com.desierto.domain.Cell;
import com.desierto.domain.Color;
import com.desierto.domain.Move;
import com.desierto.domain.MoveGenerator;
import com.desierto.domain.MoveNotation;
import com.desierto.domain.pieces.Bishop;
import com.desierto.domain.pieces.King;
import com.desierto.domain.pieces.Pawn;
import com.desierto.domain.pieces.Queen;
import com.desierto.domain.pieces.Rook;
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

  @Test
  void searchFindsTheOpenRookFileForBothSides() {
    for (Color color : Color.values()) {
      Color opponent = color.opposite();
      Cell king = mirror(Cell.C1, color);
      Cell rook = mirror(Cell.F1, color);
      Cell pawn = mirror(Cell.E4, color);
      Cell blockingPawn = mirror(Cell.F2, color);
      Cell bishop = mirror(Cell.G1, color);
      Cell enemyKing = mirror(Cell.H8, color);
      Cell enemyQueen = mirror(Cell.A8, color);
      Board board = BoardFactory.position().sideToMove(color)
          .castlingRights(false, false, false, false)
          .place(new King(color, king), king)
          .place(new Rook(color, rook), rook)
          .place(new Pawn(color, pawn), pawn)
          .place(new Pawn(color, blockingPawn), blockingPawn)
          .place(new Bishop(color, bishop), bishop)
          .place(new King(opponent, enemyKing), enemyKing)
          .place(new Queen(opponent, enemyQueen), enemyQueen)
          .build();
      SearchResult result = new AlphaBetaSearch(false, false)
          .findBestMove(board, new SearchLimits(1));
      String expected = color == Color.WHITE ? "f1d1" : "f8d8";
      assertTrue(result.bestMove().map(MoveNotation::format).orElse("").equals(expected),
          color + ": " + result.trace());
    }
  }

  private Cell mirror(Cell cell, Color color) {
    return color == Color.WHITE ? cell : Cell.of(7 - cell.row(), cell.column());
  }

  private void assertReasonableMove(Board board, Set<String> reasonableMoves) {
    SearchResult result = new AlphaBetaSearch().findBestMove(board, new SearchLimits(1));
    Move move = result.bestMove().orElseThrow();
    String notation = move.from().toAlgebraic().toLowerCase()
        + move.to().toAlgebraic().toLowerCase();
    assertTrue(reasonableMoves.contains(notation), notation + " score=" + result.score());
  }
}
