package com.desierto.uci;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.desierto.domain.Board;
import com.desierto.domain.Cell;
import com.desierto.domain.Color;
import com.desierto.domain.MoveGenerator;
import com.desierto.domain.MoveType;
import com.desierto.domain.pieces.Knight;
import org.junit.jupiter.api.Test;

class FenTest {
  @Test
  void loadsEveryFieldIncludingCastlingAndCounters() {
    Board board = Fen.parse("r3k2r/8/8/8/8/8/8/R3K2R b KQkq - 42 17");
    assertEquals(Color.BLACK, board.getSideToMove());
    assertEquals(42, board.getHalfmoveClock());
    assertEquals(17, board.getFullmoveNumber());
    assertTrue(board.canWhiteCastleKingside());
    assertTrue(board.canWhiteCastleQueenside());
    assertTrue(board.canBlackCastleKingside());
    assertTrue(board.canBlackCastleQueenside());
    assertEquals(2, new MoveGenerator().generateLegalMoves(board, Color.BLACK).stream()
        .filter(move -> move.type() == MoveType.CASTLE_KINGSIDE
            || move.type() == MoveType.CASTLE_QUEENSIDE).count());
  }

  @Test
  void loadsEnPassantAndAppliesPromotionAndCastlingMoves() {
    Board board = UciPosition.parse(("position fen 4k3/8/8/3pP3/8/8/8/4K3"
        + " w - d6 0 1 moves e5d6").split(" "));
    assertTrue(board.getPieceAt(Cell.D5).isEmpty());
    assertTrue(board.getPieceAt(Cell.D6).isPresent());
    Board promotion = UciPosition.parse(("position fen 7k/P7/8/8/8/8/8/4K3"
        + " w - - 0 1 moves a7a8n").split(" "));
    assertTrue(promotion.getPieceAt(Cell.A8).orElseThrow() instanceof Knight);
    Board castle = UciPosition.parse(("position fen 4k3/8/8/8/8/8/8/4K2R"
        + " w K - 0 1 moves e1g1").split(" "));
    assertTrue(castle.getPieceAt(Cell.F1).isPresent());
    assertFalse(castle.canWhiteCastleKingside());
  }

  @Test
  void replaysHistoryFromStartPositionForRepetition() {
    Board board = UciPosition.parse(("position startpos moves g1f3 g8f6 f3g1 f6g8"
        + " g1f3 g8f6 f3g1 f6g8").split(" "));
    assertTrue(board.isThreefoldRepetition());
    assertFalse(Fen.parse("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 8 5")
        .isThreefoldRepetition());
  }

  @Test
  void rejectsMalformedPositionsAndMoves() {
    for (String invalid : new String[] {
        "8/8/8/8/8/8/8/8 w - - 0 1",
        "4k3/8/8/8/8/8/8/4K3 w K - 0 1",
        "4k3/8/8/8/8/8/8/4K3 w - d6 0 1",
        "4k3/8/8/8/8/8/8/4K3 w - - -1 1",
        "4k3/8/8/8/8/8/8/4K3 w - - 0 0",
        "4k3/8/8/8/8/8/8/4K3 w KK - 0 1",
        "4k3/8/8/8/8/8/8/4K3 w - - 0",
        "4k3/8/8/8/8/8/8/P3K3 w - - 0 1"
    }) {
      assertThrows(IllegalArgumentException.class, () -> Fen.parse(invalid), invalid);
    }
    assertThrows(IllegalArgumentException.class,
        () -> UciPosition.parse("position startpos moves e2e5".split(" ")));
    assertThrows(IllegalArgumentException.class,
        () -> UciPosition.parse("position startpos moves e2e4 e7e9".split(" ")));
  }
}
