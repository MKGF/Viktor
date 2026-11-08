package com.desierto.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.desierto.domain.Board;
import com.desierto.domain.BoardFactory;
import com.desierto.domain.Cell;
import com.desierto.domain.Color;
import com.desierto.domain.MoveGenerator;
import com.desierto.domain.MoveNotation;
import com.desierto.domain.pieces.King;
import com.desierto.domain.pieces.Knight;
import com.desierto.domain.pieces.Pawn;
import com.desierto.domain.pieces.Queen;
import com.desierto.domain.pieces.Rook;
import com.desierto.search.evaluation.PositionEvaluator;
import com.desierto.search.evaluation.StandardPositionEvaluator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class QuiescenceSearchTest {

  private final MoveGenerator generator = new MoveGenerator();
  private final TacticalMoveOrderingStrategy ordering = new TacticalMoveOrderingStrategy();
  private final QuiescenceSearch search = new QuiescenceSearch(
      new StandardPositionEvaluator(), generator, ordering);

  @Test
  void shouldResolveARecapture() {
    Board board = BoardFactory.position()
        .sideToMove(Color.BLACK)
        .castlingRights(false, false, false, false)
        .place(new King(Color.WHITE, Cell.E1), Cell.E1)
        .place(new Queen(Color.WHITE, Cell.D5), Cell.D5)
        .place(new Pawn(Color.BLACK, Cell.E6), Cell.E6)
        .place(new King(Color.BLACK, Cell.E8), Cell.E8)
        .build();
    AlphaBetaSearch.SearchContext context = new AlphaBetaSearch.SearchContext();

    int score = search.search(board, Color.WHITE, 1, Integer.MIN_VALUE, Integer.MAX_VALUE, context);

    Board recaptured = board.copy();
    recaptured.makeMove(MoveNotation.find("e6d5",
        generator.generateLegalMoves(recaptured, Color.BLACK)).orElseThrow());
    assertEquals(new StandardPositionEvaluator().evaluate(recaptured, Color.WHITE, 2), score);
    assertTrue(context.nodes() > 1);
    assertTrue(board.getPieceAt(Cell.D5).orElseThrow() instanceof Queen);
    assertEquals(Color.BLACK, board.getSideToMove());
  }

  @Test
  void shouldExploreNonCapturePromotions() {
    Board board = BoardFactory.position()
        .castlingRights(false, false, false, false)
        .place(new King(Color.WHITE, Cell.E1), Cell.E1)
        .place(new Pawn(Color.WHITE, Cell.A7), Cell.A7)
        .place(new King(Color.BLACK, Cell.H8), Cell.H8)
        .build();

    int score = search.search(board, Color.WHITE, 0, Integer.MIN_VALUE, Integer.MAX_VALUE,
        new AlphaBetaSearch.SearchContext());

    assertTrue(score > 800 && score < 1000, "score=" + score);
  }

  @Test
  void shouldExploreCapturePromotionsAndEnPassant() {
    Board promotion = BoardFactory.position()
        .castlingRights(false, false, false, false)
        .place(new King(Color.WHITE, Cell.E1), Cell.E1)
        .place(new Pawn(Color.WHITE, Cell.G7), Cell.G7)
        .place(new Rook(Color.BLACK, Cell.H8), Cell.H8)
        .place(new King(Color.BLACK, Cell.A8), Cell.A8)
        .build();
    Board enPassant = BoardFactory.position()
        .castlingRights(false, false, false, false)
        .enPassantTarget(Cell.D6)
        .place(new King(Color.WHITE, Cell.E1), Cell.E1)
        .place(new Pawn(Color.WHITE, Cell.E5), Cell.E5)
        .place(new Pawn(Color.BLACK, Cell.D5), Cell.D5)
        .place(new King(Color.BLACK, Cell.E8), Cell.E8)
        .build();

    int promotionScore = search.search(promotion, Color.WHITE, 0,
        Integer.MIN_VALUE, Integer.MAX_VALUE, new AlphaBetaSearch.SearchContext());
    int enPassantScore = search.search(enPassant, Color.WHITE, 0,
        Integer.MIN_VALUE, Integer.MAX_VALUE, new AlphaBetaSearch.SearchContext());
    assertTrue(promotionScore > 800 && promotionScore < 1000, "score=" + promotionScore);
    assertTrue(enPassantScore > 50 && enPassantScore < 150, "score=" + enPassantScore);
  }

  @Test
  void shouldExploreQuietEvasionsInsteadOfStandingPatInCheck() {
    Board board = checkedPosition();
    PositionEvaluator evaluator = (position, perspective) ->
        generator.isKingInCheck(position, Color.WHITE) ? 1000 : -10;
    QuiescenceSearch searchWithCustomEvaluator = new QuiescenceSearch(evaluator, generator, ordering);
    AlphaBetaSearch.SearchContext context = new AlphaBetaSearch.SearchContext();

    int score = searchWithCustomEvaluator.search(board, Color.WHITE, 0,
        Integer.MIN_VALUE, Integer.MAX_VALUE, context);

    assertEquals(-10, score);
    assertTrue(context.nodes() > 1);
  }

  @Test
  void shouldEvaluateEvenInCheckAtTheDepthCap() {
    Board board = checkedPosition();
    QuiescenceSearch capped = new QuiescenceSearch((position, perspective) -> 123,
        generator, ordering, 0);
    AlphaBetaSearch.SearchContext context = new AlphaBetaSearch.SearchContext();

    int score = capped.search(board, Color.WHITE, 0, Integer.MIN_VALUE, Integer.MAX_VALUE, context);

    assertEquals(8, QuiescenceSearch.MAX_PLY);
    assertEquals(123, score);
    assertEquals(1, context.nodes());
  }

  @Test
  void shouldStopAfterEightQuiescencePlies() {
    Board board = BoardFactory.position()
        .castlingRights(false, false, false, false)
        .place(new King(Color.WHITE, Cell.H1), Cell.H1)
        .place(new King(Color.BLACK, Cell.H8), Cell.H8)
        .place(new Pawn(Color.WHITE, Cell.A7), Cell.A7)
        .place(new Pawn(Color.WHITE, Cell.B7), Cell.B7)
        .place(new Pawn(Color.WHITE, Cell.C7), Cell.C7)
        .place(new Pawn(Color.WHITE, Cell.D7), Cell.D7)
        .place(new Pawn(Color.BLACK, Cell.A2), Cell.A2)
        .place(new Pawn(Color.BLACK, Cell.B2), Cell.B2)
        .place(new Pawn(Color.BLACK, Cell.C2), Cell.C2)
        .place(new Pawn(Color.BLACK, Cell.D2), Cell.D2)
        .build();
    AtomicInteger deepestPly = new AtomicInteger();
    PositionEvaluator evaluator = new PositionEvaluator() {
      @Override
      public int evaluate(Board position, Color perspective) {
        return position.getSideToMove() == perspective ? -1000 : 1000;
      }

      @Override
      public int evaluate(Board position, Color perspective, int plyFromRoot) {
        deepestPly.accumulateAndGet(plyFromRoot, Math::max);
        return evaluate(position, perspective);
      }
    };
    MoveOrderingStrategy onePromotion = (position, moves, preferred) -> List.of(moves.stream()
        .filter(move -> move.move().promotionPiece() instanceof Knight)
        .findFirst().orElseThrow());
    QuiescenceSearch singleLine = new QuiescenceSearch(evaluator, generator, onePromotion);
    AlphaBetaSearch.SearchContext context = new AlphaBetaSearch.SearchContext();

    singleLine.search(board, Color.WHITE, 0, Integer.MIN_VALUE, Integer.MAX_VALUE, context);

    assertEquals(8, deepestPly.get());
    assertEquals(9, context.nodes());
  }

  @Test
  void shouldEvaluateMateAndStalemateBeforeStandingPat() {
    Board checkmate = BoardFactory.position()
        .sideToMove(Color.BLACK)
        .castlingRights(false, false, false, false)
        .place(new King(Color.BLACK, Cell.H8), Cell.H8)
        .place(new Queen(Color.WHITE, Cell.G7), Cell.G7)
        .place(new King(Color.WHITE, Cell.F6), Cell.F6)
        .build();
    Board stalemate = BoardFactory.position()
        .sideToMove(Color.BLACK)
        .castlingRights(false, false, false, false)
        .place(new King(Color.BLACK, Cell.H8), Cell.H8)
        .place(new Queen(Color.WHITE, Cell.G6), Cell.G6)
        .place(new King(Color.WHITE, Cell.F7), Cell.F7)
        .build();

    assertEquals(-StandardPositionEvaluator.CHECKMATE_SCORE + 3,
        search.search(checkmate, Color.BLACK, 3, Integer.MIN_VALUE, Integer.MAX_VALUE,
            new AlphaBetaSearch.SearchContext()));
    assertEquals(0, search.search(stalemate, Color.BLACK, 3, Integer.MIN_VALUE, Integer.MAX_VALUE,
        new AlphaBetaSearch.SearchContext()));
  }

  @Test
  void shouldAvoidRecaptureAtFixedDepthAndAllowDisablingTheExtension() {
    Board board = BoardFactory.position()
        .castlingRights(false, false, false, false)
        .place(new King(Color.WHITE, Cell.E1), Cell.E1)
        .place(new Queen(Color.WHITE, Cell.D1), Cell.D1)
        .place(new Rook(Color.BLACK, Cell.D5), Cell.D5)
        .place(new Pawn(Color.BLACK, Cell.E6), Cell.E6)
        .place(new King(Color.BLACK, Cell.E8), Cell.E8)
        .build();

    SearchResult extended = new AlphaBetaSearch().findBestMove(board, new SearchLimits(1));
    SearchResult original = new AlphaBetaSearch(false).findBestMove(board, new SearchLimits(1));

    assertFalse(extended.bestMove().orElseThrow().to().equals(Cell.D5));
    assertEquals(Cell.D5, original.bestMove().orElseThrow().to());
    assertTrue(extended.nodes() > original.nodes());
  }

  @Test
  void shouldRespectTheSearchDeadline() {
    AlphaBetaSearch.SearchContext context =
        new AlphaBetaSearch.SearchContext(() -> true);

    assertThrows(AlphaBetaSearch.SearchDeadlineExceeded.class,
        () -> search.search(Board.standard(), Color.WHITE, 0,
            Integer.MIN_VALUE, Integer.MAX_VALUE, context));
    assertEquals(0, context.nodes());
  }

  private Board checkedPosition() {
    return BoardFactory.position()
        .castlingRights(false, false, false, false)
        .place(new King(Color.WHITE, Cell.E1), Cell.E1)
        .place(new Rook(Color.BLACK, Cell.E8), Cell.E8)
        .place(new King(Color.BLACK, Cell.A8), Cell.A8)
        .build();
  }
}
