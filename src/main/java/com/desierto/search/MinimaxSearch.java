package com.desierto.search;

import com.desierto.domain.Board;
import com.desierto.domain.Color;
import com.desierto.domain.DrawDetector;
import com.desierto.domain.LegalMove;
import com.desierto.domain.Move;
import com.desierto.domain.MoveGenerator;
import com.desierto.domain.SearchPosition;
import com.desierto.search.evaluation.PositionEvaluator;
import com.desierto.search.evaluation.StandardPositionEvaluator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class MinimaxSearch implements SearchAlgorithm {

  private final PositionEvaluator evaluator;
  private final MoveGenerator moveGenerator;

  public MinimaxSearch() {
    this(new StandardPositionEvaluator(), new MoveGenerator());
  }

  MinimaxSearch(PositionEvaluator evaluator, MoveGenerator moveGenerator) {
    this.evaluator = Objects.requireNonNull(evaluator, "evaluator must not be null");
    this.moveGenerator = Objects.requireNonNull(moveGenerator, "moveGenerator must not be null");
  }

  @Override
  public SearchResult findBestMove(Board board, SearchLimits limits) {
    Objects.requireNonNull(board, "board must not be null");
    Objects.requireNonNull(limits, "limits must not be null");

    SearchPosition position = new SearchPosition(board);
    Board workingBoard = position.board();
    Color perspective = workingBoard.getSideToMove();
    SearchContext context = new SearchContext();
    context.visitPosition();
    List<LegalMove> legalMoves = moveGenerator.generateLegalMoveApplications(workingBoard, perspective);
    if (legalMoves.isEmpty()) {
      return new SearchResult(Optional.empty(), perspective, evaluator.evaluate(workingBoard, perspective, 0), 0,
          context.nodes());
    }
    if (DrawDetector.isDeadByInsufficientMaterial(workingBoard)
        || DrawDetector.isSeventyFiveMoveDraw(workingBoard, moveGenerator)
        || DrawDetector.isThreefoldRepetitionDraw(workingBoard, moveGenerator)) {
      return new SearchResult(Optional.empty(), perspective, 0, 0, context.nodes());
    }

    Optional<DrawClaim> claim = SearchDrawRules.claim(workingBoard, legalMoves);
    Move bestMove = null;
    int bestScore = claim.isPresent() ? 0 : Integer.MIN_VALUE;
    for (LegalMove legalMove : legalMoves) {
      Move move = legalMove.move();
      int score;
      try (SearchPosition.MoveScope ignored = position.apply(legalMove)) {
        score = minimax(position, limits.maxDepth() - 1, perspective, 1, context);
      }
      if (score > bestScore) {
        bestScore = score;
        bestMove = move;
      }
    }
    if (bestMove == null && claim.isPresent()) {
      Optional<Move> intended = claim.orElseThrow().intendedMove().map(position::originalMove);
      return new SearchResult(Optional.empty(), perspective, 0, limits.maxDepth(), context.nodes(),
          Optional.of(new DrawClaim(intended)));
    }
    return new SearchResult(Optional.of(position.originalMove(bestMove)), perspective, bestScore, limits.maxDepth(),
        context.nodes());
  }

  private int minimax(SearchPosition position, int remainingDepth, Color perspective, int plyFromRoot,
      SearchContext context) {
    Board board = position.board();
    context.visitPosition();
    if (DrawDetector.isDeadByInsufficientMaterial(board)) {
      return 0;
    }
    if (DrawDetector.isSeventyFiveMoveDraw(board, moveGenerator)
        || DrawDetector.isThreefoldRepetitionDraw(board, moveGenerator)) {
      return 0;
    }
    if (remainingDepth == 0) {
      int score = evaluator.evaluate(board, perspective, plyFromRoot);
      return SearchDrawRules.canClaim(board, moveGenerator)
          ? SearchDrawRules.withClaimOption(score, board, perspective) : score;
    }

    List<LegalMove> legalMoves = moveGenerator.generateLegalMoveApplications(board,
        board.getSideToMove());
    if (legalMoves.isEmpty()) {
      return evaluator.evaluate(board, perspective, plyFromRoot);
    }

    boolean maximizesScore = board.getSideToMove() == perspective;
    int bestScore = SearchDrawRules.claim(board, legalMoves).isPresent()
        ? 0 : maximizesScore ? Integer.MIN_VALUE : Integer.MAX_VALUE;
    for (LegalMove legalMove : legalMoves) {
      int score;
      try (SearchPosition.MoveScope ignored = position.apply(legalMove)) {
        score = minimax(position, remainingDepth - 1, perspective, plyFromRoot + 1, context);
      }
      bestScore = maximizesScore ? Math.max(bestScore, score) : Math.min(bestScore, score);
    }
    return bestScore;
  }

  private static final class SearchContext {

    private long nodes;

    void visitPosition() {
      nodes++;
    }

    long nodes() {
      return nodes;
    }
  }
}
