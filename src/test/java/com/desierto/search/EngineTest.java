package com.desierto.search;

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
import com.desierto.domain.MoveNotation;
import com.desierto.domain.pieces.King;
import com.desierto.domain.pieces.Queen;
import com.desierto.domain.pieces.Rook;
import com.desierto.search.evaluation.StandardPositionEvaluator;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class EngineTest {

  @Test
  void thinkingMatchesTheExistingSearchAndDoesNotPlayTheDecision() {
    Board board = Board.standard();
    board.makeMove(MoveNotation.find("e2e4",
        new MoveGenerator().generateLegalMoves(board, board.getSideToMove())).orElseThrow());
    BoardState before = BoardState.of(board);
    SearchLimits limits = new SearchLimits(2);

    SearchResult expected = new IterativeDeepeningSearch().findBestMove(board, limits);
    SearchResult decision = new Engine().think(board, limits);

    assertEquals(expected, decision);
    assertTrue(decision.bestMove().isPresent());
    assertEquals(before, BoardState.of(board));
    board.undoMove();
    assertEquals(Board.standard().toString(), board.toString());
  }

  @Test
  void thinkingReportsIterationsAndReturnsTheLastCompletedOneOnCancellation() {
    Board board = Board.standard();
    BoardState before = BoardState.of(board);
    AtomicBoolean cancelled = new AtomicBoolean();
    List<SearchResult> iterations = new ArrayList<>();

    SearchResult decision = new Engine().think(board, new SearchLimits(3), cancelled::get, result -> {
      iterations.add(result);
      cancelled.set(true);
    });

    assertEquals(1, iterations.size());
    assertEquals(iterations.getFirst(), decision);
    assertEquals(1, decision.depth());
    assertEquals(before, BoardState.of(board));
  }

  @Test
  void cancellationBeforeTheFirstIterationDoesNotChangeTheBoard() {
    Board board = Board.standard();
    BoardState before = BoardState.of(board);

    assertThrows(SearchTimeoutException.class,
        () -> new Engine().think(board, new SearchLimits(2), () -> true, result -> {}));
    assertEquals(before, BoardState.of(board));
  }

  @Test
  void timedThinkingAndItsValidationUseTheExistingContract() {
    Board board = Board.standard();
    Engine engine = new Engine(new AlphaBetaSearch(), () -> 0);
    SearchLimits limits = new SearchLimits(1);

    assertEquals(engine.think(board, limits), engine.think(board, limits, Duration.ofSeconds(1)));
    assertThrows(IllegalArgumentException.class, () -> engine.think(board, limits, Duration.ZERO));
    assertThrows(NullPointerException.class, () -> engine.think(board, limits, (Duration) null));
  }

  @Test
  void thinkingCanReturnADrawClaimInsteadOfAMove() {
    Board board = BoardFactory.position().halfmoveClock(100)
        .castlingRights(false, false, false, false)
        .place(new King(Color.WHITE, Cell.A1), Cell.A1)
        .place(new Rook(Color.WHITE, Cell.B1), Cell.B1)
        .place(new King(Color.BLACK, Cell.H8), Cell.H8)
        .place(new Rook(Color.BLACK, Cell.B8), Cell.B8)
        .build();
    BoardState before = BoardState.of(board);
    AlphaBetaSearch search = new AlphaBetaSearch(
        (position, perspective) -> -1000, new MoveGenerator(), new TacticalMoveOrderingStrategy(),
        false, false);

    SearchResult decision = new Engine(search, () -> 0).think(board, new SearchLimits(1));

    assertTrue(decision.drawClaim().isPresent());
    assertFalse(decision.bestMove().isPresent());
    assertEquals(0, decision.score());
    assertEquals(before, BoardState.of(board));
  }

  @Test
  void thinkingReturnsATerminalPositionWithoutPlaying() {
    Board board = BoardFactory.position().sideToMove(Color.BLACK)
        .castlingRights(false, false, false, false)
        .place(new King(Color.BLACK, Cell.H8), Cell.H8)
        .place(new Queen(Color.WHITE, Cell.G7), Cell.G7)
        .place(new King(Color.WHITE, Cell.F6), Cell.F6)
        .build();
    BoardState before = BoardState.of(board);

    SearchResult decision = new Engine().think(board, new SearchLimits(2));

    assertTrue(decision.bestMove().isEmpty());
    assertTrue(decision.drawClaim().isEmpty());
    assertEquals(-StandardPositionEvaluator.CHECKMATE_SCORE, decision.score());
    assertEquals(0, decision.depth());
    assertEquals(before, BoardState.of(board));
  }

  private record BoardState(String squares, Color side, boolean whiteKingside, boolean whiteQueenside,
                            boolean blackKingside, boolean blackQueenside, Optional<Cell> enPassant,
                            int halfmoves, int fullmoves, Optional<Move> lastMove, long repetition) {
    private static BoardState of(Board board) {
      return new BoardState(board.toString(), board.getSideToMove(), board.canWhiteCastleKingside(),
          board.canWhiteCastleQueenside(), board.canBlackCastleKingside(),
          board.canBlackCastleQueenside(), board.getEnPassantTarget(), board.getHalfmoveClock(),
          board.getFullmoveNumber(), board.getLastMove(), board.repetitionHistoryFingerprint());
    }
  }
}
