package com.desierto.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.desierto.domain.pieces.King;
import com.desierto.domain.pieces.Rook;
import java.util.List;
import org.junit.jupiter.api.Test;

class CastlingRightsBehaviorTest {

  private static final int WHITE_KINGSIDE = 1;
  private static final int WHITE_QUEENSIDE = 2;
  private static final int BLACK_KINGSIDE = 4;
  private static final int BLACK_QUEENSIDE = 8;
  private final MoveGenerator generator = new MoveGenerator();

  @Test
  void preservesAllRightsCombinationsWhenKingsAndRooksMoveOrAreCaptured() {
    List<Scenario> scenarios = List.of(
        new Scenario(Color.WHITE, Cell.E1, Cell.D1, WHITE_KINGSIDE | WHITE_QUEENSIDE),
        new Scenario(Color.BLACK, Cell.E8, Cell.D8, BLACK_KINGSIDE | BLACK_QUEENSIDE),
        new Scenario(Color.WHITE, Cell.A1, Cell.A2, WHITE_QUEENSIDE),
        new Scenario(Color.WHITE, Cell.H1, Cell.H2, WHITE_KINGSIDE),
        new Scenario(Color.BLACK, Cell.A8, Cell.A7, BLACK_QUEENSIDE),
        new Scenario(Color.BLACK, Cell.H8, Cell.H7, BLACK_KINGSIDE),
        new Scenario(Color.WHITE, Cell.A1, Cell.A8, WHITE_QUEENSIDE | BLACK_QUEENSIDE),
        new Scenario(Color.WHITE, Cell.H1, Cell.H8, WHITE_KINGSIDE | BLACK_KINGSIDE),
        new Scenario(Color.BLACK, Cell.A8, Cell.A1, WHITE_QUEENSIDE | BLACK_QUEENSIDE),
        new Scenario(Color.BLACK, Cell.H8, Cell.H1, WHITE_KINGSIDE | BLACK_KINGSIDE));

    for (int initialRights = 0; initialRights < 16; initialRights++) {
      for (Scenario scenario : scenarios) {
        Board board = position(initialRights, scenario.side());
        Board copy = board.copy();
        assertEquals(initialRights, rights(board));
        assertEquals(initialRights, rights(copy));

        Move move = findMove(board, scenario.from(), scenario.to());
        assertEquals(initialRights, rights(board));
        board.makeMove(move);
        assertEquals(initialRights & ~scenario.lostRights(), rights(board), scenario.toString());
        assertEquals(initialRights, rights(copy));

        board.undoMove();
        assertEquals(initialRights, rights(board), scenario.toString());
      }
    }
  }

  @Test
  void restoresRightsAfterNestedSearchMoves() {
    for (int initialRights = 0; initialRights < 16; initialRights++) {
      Board source = position(initialRights, Color.WHITE);
      SearchPosition position = new SearchPosition(source);
      Board board = position.board();
      LegalMove capture = findLegalMove(board, Cell.A1, Cell.A8);

      try (SearchPosition.MoveScope ignored = position.exploreMove(capture)) {
        int afterCapture = initialRights & ~(WHITE_QUEENSIDE | BLACK_QUEENSIDE);
        assertEquals(afterCapture, rights(board));
        LegalMove reply = findLegalMove(board, Cell.E8, Cell.E7);
        try (SearchPosition.MoveScope replyScope = position.exploreMove(reply)) {
          assertEquals(afterCapture & ~(BLACK_KINGSIDE | BLACK_QUEENSIDE), rights(board));
        }
        assertEquals(afterCapture, rights(board));
      }
      assertEquals(initialRights, rights(board));
      assertEquals(initialRights, rights(source));
    }
  }

  private Board position(int rights, Color side) {
    return BoardFactory.position().sideToMove(side)
        .castlingRights((rights & WHITE_KINGSIDE) != 0, (rights & WHITE_QUEENSIDE) != 0,
            (rights & BLACK_KINGSIDE) != 0, (rights & BLACK_QUEENSIDE) != 0)
        .place(new King(Color.WHITE, Cell.E1), Cell.E1)
        .place(new Rook(Color.WHITE, Cell.A1), Cell.A1)
        .place(new Rook(Color.WHITE, Cell.H1), Cell.H1)
        .place(new King(Color.BLACK, Cell.E8), Cell.E8)
        .place(new Rook(Color.BLACK, Cell.A8), Cell.A8)
        .place(new Rook(Color.BLACK, Cell.H8), Cell.H8)
        .build();
  }

  private Move findMove(Board board, Cell from, Cell to) {
    return generator.generateLegalMoves(board, board.getSideToMove()).stream()
        .filter(move -> move.from().equals(from) && move.to().equals(to))
        .findFirst().orElseThrow();
  }

  private LegalMove findLegalMove(Board board, Cell from, Cell to) {
    return generator.generateLegalMoveApplications(board, board.getSideToMove()).stream()
        .filter(move -> move.move().from().equals(from) && move.move().to().equals(to))
        .findFirst().orElseThrow();
  }

  private static int rights(Board board) {
    return (board.canWhiteCastleKingside() ? WHITE_KINGSIDE : 0)
        | (board.canWhiteCastleQueenside() ? WHITE_QUEENSIDE : 0)
        | (board.canBlackCastleKingside() ? BLACK_KINGSIDE : 0)
        | (board.canBlackCastleQueenside() ? BLACK_QUEENSIDE : 0);
  }

  private record Scenario(Color side, Cell from, Cell to, int lostRights) {
  }
}
