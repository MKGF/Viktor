package com.desierto.search;

import com.desierto.domain.Board;
import com.desierto.domain.Color;
import com.desierto.domain.LegalMove;
import com.desierto.domain.Move;
import com.desierto.domain.MoveGenerator;
import com.desierto.search.evaluation.PositionEvaluator;
import com.desierto.search.evaluation.StandardPositionEvaluator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class FixedDepthAlphaBetaEngine implements SearchEngine {

  private final PositionEvaluator evaluator;
  private final MoveGenerator moveGenerator;
  private final MoveOrderingStrategy moveOrderingStrategy;

  public FixedDepthAlphaBetaEngine() {
    this(new StandardPositionEvaluator(), new MoveGenerator(), new TacticalMoveOrderingStrategy());
  }

  FixedDepthAlphaBetaEngine(PositionEvaluator evaluator, MoveGenerator moveGenerator) {
    this(evaluator, moveGenerator, new TacticalMoveOrderingStrategy());
  }

  FixedDepthAlphaBetaEngine(PositionEvaluator evaluator, MoveGenerator moveGenerator,
      MoveOrderingStrategy moveOrderingStrategy) {
    this.evaluator = Objects.requireNonNull(evaluator, "evaluator must not be null");
    this.moveGenerator = Objects.requireNonNull(moveGenerator, "moveGenerator must not be null");
    this.moveOrderingStrategy = Objects.requireNonNull(moveOrderingStrategy,
        "moveOrderingStrategy must not be null");
  }

  @Override
  public SearchResult findBestMove(Board board, SearchLimits limits) {
    Objects.requireNonNull(board, "board must not be null");
    Objects.requireNonNull(limits, "limits must not be null");

    Color perspective = board.getSideToMove();
    SearchContext context = new SearchContext();
    context.visitPosition();
    List<LegalMove> legalMoves = moveGenerator.generateLegalMoveApplications(board, perspective);
    if (legalMoves.isEmpty()) {
      return new SearchResult(Optional.empty(), perspective, evaluator.evaluate(board, perspective, 0), 0,
          context.nodes());
    }

    Move bestMove = null;
    int bestScore = Integer.MIN_VALUE;
    int alpha = Integer.MIN_VALUE;
    int beta = Integer.MAX_VALUE;
    for (LegalMove legalMove : orderMoves(board, legalMoves)) {
      Move move = legalMove.move();
      Board child = legalMove.resultingPosition();
      int score = alphaBeta(child, limits.maxDepth() - 1, perspective, 1, alpha, beta, context);
      if (score > bestScore) {
        bestScore = score;
        bestMove = move;
      }
      alpha = Math.max(alpha, bestScore);
    }
    return new SearchResult(Optional.of(bestMove), perspective, bestScore, limits.maxDepth(),
        context.nodes());
  }

  private int alphaBeta(Board board, int remainingDepth, Color perspective, int plyFromRoot, int alpha,
      int beta, SearchContext context) {
    context.visitPosition();
    if (remainingDepth == 0) {
      return evaluator.evaluate(board, perspective, plyFromRoot);
    }

    List<LegalMove> legalMoves = moveGenerator.generateLegalMoveApplications(board,
        board.getSideToMove());
    if (legalMoves.isEmpty()) {
      return evaluator.evaluate(board, perspective, plyFromRoot);
    }

    if (board.getSideToMove() == perspective) {
      int bestScore = Integer.MIN_VALUE;
      for (LegalMove legalMove : orderMoves(board, legalMoves)) {
        Board child = legalMove.resultingPosition();
        int score = alphaBeta(child, remainingDepth - 1, perspective, plyFromRoot + 1, alpha, beta,
            context);
        bestScore = Math.max(bestScore, score);
        alpha = Math.max(alpha, bestScore);
        if (alpha >= beta) {
          break;
        }
      }
      return bestScore;
    }

    int bestScore = Integer.MAX_VALUE;
    for (LegalMove legalMove : orderMoves(board, legalMoves)) {
      Board child = legalMove.resultingPosition();
      int score = alphaBeta(child, remainingDepth - 1, perspective, plyFromRoot + 1, alpha, beta,
          context);
      bestScore = Math.min(bestScore, score);
      beta = Math.min(beta, bestScore);
      if (alpha >= beta) {
        break;
      }
    }
    return bestScore;
  }

  private List<LegalMove> orderMoves(Board board, List<LegalMove> legalMoves) {
    return moveOrderingStrategy.order(board, legalMoves, Optional.empty());
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
